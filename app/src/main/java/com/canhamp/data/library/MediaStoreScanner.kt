package com.canhamp.data.library

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** A source of audio rows, delivered in chunks so callers can write them in batches (B4). */
interface AudioSource {
    /** Calls [onChunk] with up to [MediaStoreScanner.CHUNK_SIZE] rows at a time. */
    suspend fun scan(onChunk: suspend (List<ScannedRow>) -> Unit)
}

data class ScannedRow(val row: MediaStoreRow, val contentUri: String)

/** One cursor pass over `MediaStore.Audio.Media` on `Dispatchers.IO` (B2). */
class MediaStoreScanner(private val resolver: ContentResolver) : AudioSource {

    @Suppress("DEPRECATION")
    override suspend fun scan(onChunk: suspend (List<ScannedRow>) -> Unit) {
        withContext(Dispatchers.IO) {
            val base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val q = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            val r = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            val cols = arrayListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
            )
            if (q) cols += MediaStore.Audio.Media.RELATIVE_PATH
            if (r) cols += MediaStore.Audio.Media.BITRATE
            // Music only: excludes voice recordings, podcasts and audiobooks (spec B2).
            val kind = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val selection = "$kind AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
                "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND ${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
                "${MediaStore.Audio.Media.DURATION} >= $MIN_DURATION_MS"
            val cursor = resolver.query(base, cols.toTypedArray(), selection, null, null)
                ?: throw java.io.IOException("MediaStore query returned no cursor")
            cursor.use {
                val idx = Indexes(it, q, r)
                var chunk = ArrayList<ScannedRow>(CHUNK_SIZE)
                while (it.moveToNext()) {
                    ensureActive()
                    val row = idx.read(it)
                    chunk.add(ScannedRow(row, ContentUris.withAppendedId(base, row.id).toString()))
                    if (chunk.size >= CHUNK_SIZE) {
                        onChunk(chunk)
                        chunk = ArrayList(CHUNK_SIZE)
                    }
                }
                if (chunk.isNotEmpty()) onChunk(chunk)
            }
        }
    }

    @Suppress("DEPRECATION")
    private class Indexes(cur: Cursor, q: Boolean, r: Boolean) {
        val id = cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val name = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val title = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artist = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val album = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val mime = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val dur = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val size = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val data = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val added = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val modified = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val rel = if (q) cur.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH) else -1
        val bitrate = if (r) cur.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE) else -1

        fun read(cur: Cursor) = MediaStoreRow(
            id = cur.getLong(id),
            displayName = cur.getString(name).orEmpty(),
            title = cur.getString(title),
            artist = cur.getString(artist),
            album = cur.getString(album),
            mimeType = cur.getString(mime),
            durationMs = cur.getLong(dur),
            sizeBytes = cur.getLong(size),
            bitrate = if (bitrate >= 0 && !cur.isNull(bitrate)) cur.getInt(bitrate) else null,
            relativePath = if (rel >= 0) cur.getString(rel) else null,
            data = cur.getString(data),
            dateAddedSec = cur.getLong(added),
            dateModifiedSec = cur.getLong(modified),
        )
    }

    companion object {
        const val CHUNK_SIZE = 500
        const val MIN_DURATION_MS = 10_000
    }
}
