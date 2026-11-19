package com.canhamp.data.playlist

import com.canhamp.data.db.TrackEntity
import com.canhamp.playback.QueueEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Library lookups the playlist files need; batch calls keep a 5,000-line list off a query-per-line path. */
interface PlaylistTrackIndex {
    suspend fun findByUris(uris: List<String>): List<TrackEntity>

    suspend fun findByFilePaths(paths: List<String>): List<TrackEntity>
}

/** Opens the document the user picked. Null when it cannot be opened. */
interface PlaylistStreams {
    fun openInput(uri: String): InputStream?

    /** Truncating write ("wt"). */
    fun openOutput(uri: String): OutputStream?
}

sealed interface SaveResult {
    data class Ok(val count: Int) : SaveResult
    data object Failed : SaveResult
}

sealed interface LoadResult {
    /** [entries] are the resolved tracks in file order; [missing] counts the lines that did not resolve. */
    data class Ok(val entries: List<QueueEntry>, val missing: Int) : LoadResult
    data class Error(val reason: Reason) : LoadResult

    enum class Reason { TooLarge, NotAPlaylist }
}

/** B64 to B66: M3U8 export and import. Playlist text is untrusted; entries resolve only through the library index. */
class PlaylistFileRepository(
    private val index: PlaylistTrackIndex,
    private val streams: PlaylistStreams,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun save(uri: String, items: List<QueueEntry>): SaveResult = withContext(ioDispatcher) {
        val known = index.findByUris(items.map { it.uri }.distinct()).associateBy { it.uri }
        val encoded = M3uCodec.encode(
            items.map { item ->
                EncodeItem(
                    path = known[item.uri]?.filePath?.takeIf { it.isNotBlank() } ?: item.uri,
                    durationSec = (item.durationMs / 1000L).toInt().coerceAtLeast(0),
                    artist = item.artist,
                    title = item.title,
                )
            },
        )
        try {
            val out = streams.openOutput(uri) ?: return@withContext SaveResult.Failed
            out.use { it.write(encoded.toByteArray(Charsets.UTF_8)) }
            SaveResult.Ok(items.size)
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            SaveResult.Failed
        } catch (_: RuntimeException) {
            // Providers also throw IllegalStateException, IllegalArgumentException and SecurityException.
            SaveResult.Failed
        }
    }

    /** [documentId] is the picker's document id, used only to find the folder that relative paths start from. */
    suspend fun load(uri: String, documentId: String?): LoadResult = withContext(ioDispatcher) {
        val bytes = try {
            val input = streams.openInput(uri)
                ?: return@withContext LoadResult.Error(LoadResult.Reason.NotAPlaylist)
            input.use { readCapped(it, M3uCodec.MAX_BYTES + 1) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            return@withContext LoadResult.Error(LoadResult.Reason.NotAPlaylist)
        } catch (_: RuntimeException) {
            return@withContext LoadResult.Error(LoadResult.Reason.NotAPlaylist)
        }
        val decoded = when (val r = M3uCodec.decode(bytes)) {
            is DecodeResult.Error -> return@withContext LoadResult.Error(
                if (r.reason == DecodeResult.Reason.TooLarge) LoadResult.Reason.TooLarge else LoadResult.Reason.NotAPlaylist,
            )
            is DecodeResult.Ok -> r.entries
        }
        val baseDir = baseDirFromDocumentId(documentId)
        val locations = decoded.map { M3uCodec.classify(it.location, baseDir) }
        val byUri = index.findByUris(locations.filterIsInstance<M3uLocation.Content>().map { it.uri }.distinct())
            .associateBy { it.uri }
        val byPath = HashMap<String, TrackEntity>()
        index.findByFilePaths(locations.filterIsInstance<M3uLocation.AbsolutePath>().map { it.path }.distinct())
            .forEach { t -> t.filePath?.let { byPath.putIfAbsent(it, t) } }

        val entries = ArrayList<QueueEntry>(locations.size)
        var missing = 0
        for (loc in locations) {
            val track = when (loc) {
                is M3uLocation.Content -> byUri[loc.uri]
                is M3uLocation.AbsolutePath -> byPath[loc.path]
                is M3uLocation.Remote, M3uLocation.Unresolvable -> null
            }
            if (track == null) missing++ else entries += track.toQueueEntry()
        }
        LoadResult.Ok(entries, missing)
    }

    private fun readCapped(input: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        while (out.size() < limit) {
            val n = input.read(buf, 0, minOf(buf.size, limit - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    companion object {
        private const val PRIMARY_ROOT = "/storage/emulated/0"
        private val nameFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

        /** B64 default file name. */
        fun defaultFileName(now: LocalDateTime): String = "canh-amp-${now.format(nameFormat)}.m3u8"

        /**
         * "primary:Music/x.m3u" gives "/storage/emulated/0/Music". Other volumes and other id shapes give null,
         * so relative paths in such files count as missing.
         */
        fun baseDirFromDocumentId(documentId: String?): String? {
            val id = documentId ?: return null
            val colon = id.indexOf(':')
            if (colon < 0 || id.substring(0, colon) != "primary") return null
            val rel = id.substring(colon + 1)
            val dir = rel.substringBeforeLast('/', "")
            return if (dir.isEmpty()) PRIMARY_ROOT else "$PRIMARY_ROOT/$dir"
        }
    }
}

private fun TrackEntity.toQueueEntry() = QueueEntry(
    trackId = id,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    bitrateKbps = bitrateKbps,
)
