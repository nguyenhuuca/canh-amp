package com.canhamp.data.playlist

import com.canhamp.data.db.TrackEntity
import com.canhamp.data.db.TrackSource
import com.canhamp.playback.QueueEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime

class PlaylistFileRepositoryTest {
    private fun track(id: Long, title: String, path: String?, uri: String = "content://media/$id") = TrackEntity(
        id = id,
        uri = uri,
        source = TrackSource.MEDIASTORE.name,
        filePath = path,
        folderPath = path?.substringBeforeLast('/') ?: "",
        fileName = title,
        title = title,
        artist = "Artist",
        album = "Album",
        format = "MP3",
        durationMs = 215_000,
        bitrateKbps = 320,
        sizeBytes = 1,
        dateAddedSec = 0,
        dateModifiedSec = 0,
        scanGeneration = 1,
    )

    private class FakeIndex(private val tracks: List<TrackEntity>) : PlaylistTrackIndex {
        override suspend fun findByUris(uris: List<String>) = tracks.filter { it.uri in uris }
        override suspend fun findByFilePaths(paths: List<String>) = tracks.filter { it.filePath in paths }
    }

    private class FakeStreams(
        var input: ByteArray? = null,
        var failWrite: Boolean = false,
    ) : PlaylistStreams {
        val written = ByteArrayOutputStream()
        var opened: String? = null

        override fun openInput(uri: String): InputStream? = input?.let(::ByteArrayInputStream)

        override fun openOutput(uri: String): OutputStream? {
            opened = uri
            return if (failWrite) object : OutputStream() {
                override fun write(b: Int) { throw java.io.IOException("disk full") }
            } else written
        }
    }

    private val library = listOf(
        track(1, "One", "/storage/emulated/0/Music/one.mp3"),
        track(2, "Two", "/storage/emulated/0/Music/sub/two.mp3"),
        track(3, "Three", null, uri = "content://media/3"),
    )

    private fun repo(streams: FakeStreams) = PlaylistFileRepository(FakeIndex(library), streams)

    private fun queueEntry(t: TrackEntity) =
        QueueEntry(t.id, t.uri, t.title, t.artist, t.album, t.durationMs, t.bitrateKbps)

    @Test
    fun loadResolvesContentAbsoluteRelativeAndCountsTheRest() = runTest {
        val text = """
            #EXTM3U
            content://media/3
            /storage/emulated/0/Music/one.mp3
            sub/two.mp3
            file:///storage/emulated/0/Music/one.mp3
            http://example.com/stream.mp3
            https://example.com/b.mp3
            /storage/emulated/0/Music/gone.mp3
            C:\music\x.mp3
            content://media/999
        """.trimIndent()
        val streams = FakeStreams(input = text.toByteArray())
        val r = repo(streams).load("content://picker/1", "primary:Music/list.m3u") as LoadResult.Ok
        assertEquals(listOf(3L, 1L, 2L, 1L), r.entries.map { it.trackId })
        assertEquals(5, r.missing)
    }

    @Test
    fun relativePathsAreMissingWithoutABaseDir() = runTest {
        val streams = FakeStreams(input = "sub/two.mp3\n".toByteArray())
        val r = repo(streams).load("content://picker/1", "1234-ABCD:Music/list.m3u") as LoadResult.Ok
        assertTrue(r.entries.isEmpty())
        assertEquals(1, r.missing)
    }

    @Test
    fun parentSegmentsNeverEscapeTheRoot() = runTest {
        val streams = FakeStreams(input = "../../../../../storage/emulated/0/Music/one.mp3\n".toByteArray())
        val r = repo(streams).load("content://picker/1", "primary:Music/list.m3u") as LoadResult.Ok
        assertEquals(listOf(1L), r.entries.map { it.trackId })
    }

    @Test
    fun loadRejectsAFileOverTwoMiB() = runTest {
        val streams = FakeStreams(input = ByteArray(M3uCodec.MAX_BYTES + 5) { 'a'.code.toByte() })
        val r = repo(streams).load("content://picker/1", null)
        assertEquals(LoadResult.Error(LoadResult.Reason.TooLarge), r)
    }

    @Test
    fun loadOfExactlyTwoMiBIsNotTooLarge() = runTest {
        val line = "/x.mp3\n"
        val bytes = ByteArray(M3uCodec.MAX_BYTES) { line[it % line.length].code.toByte() }
        // 2 MiB of short lines exceeds the line limit, which is a different error than TooLarge.
        val r = repo(FakeStreams(input = bytes)).load("content://picker/1", null)
        assertEquals(LoadResult.Error(LoadResult.Reason.NotAPlaylist), r)
    }

    @Test
    fun loadOfAnUnopenableFileIsNotAPlaylist() = runTest {
        val r = repo(FakeStreams(input = null)).load("content://picker/1", null)
        assertEquals(LoadResult.Error(LoadResult.Reason.NotAPlaylist), r)
    }

    @Test
    fun saveWritesFilePathWhenKnownAndUriOtherwise() = runTest {
        val streams = FakeStreams()
        val items = library.map(::queueEntry) + queueEntry(track(9, "Unknown", "/never/indexed.mp3", "content://media/9"))
            .copy(title = "Ghost")
        val result = repo(streams).save("content://picker/out", items)
        assertEquals(SaveResult.Ok(4), result)
        val lines = streams.written.toString(Charsets.UTF_8).lines().filterNot { it.startsWith("#") || it.isEmpty() }
        assertEquals(
            listOf(
                "/storage/emulated/0/Music/one.mp3",
                "/storage/emulated/0/Music/sub/two.mp3",
                "content://media/3",
                "content://media/9",
            ),
            lines,
        )
        assertTrue(streams.written.toString(Charsets.UTF_8).startsWith("#EXTM3U\n#EXTINF:215,Artist - One\n"))
        assertEquals("content://picker/out", streams.opened)
    }

    @Test
    fun saveWithoutBomAndRoundTrips() = runTest {
        val streams = FakeStreams()
        val items = listOf(queueEntry(library[0]), queueEntry(library[2]))
        repo(streams).save("content://picker/out", items)
        val bytes = streams.written.toByteArray()
        assertTrue(bytes[0] == '#'.code.toByte())
        val back = repo(FakeStreams(input = bytes)).load("content://picker/out", null) as LoadResult.Ok
        assertEquals(listOf(1L, 3L), back.entries.map { it.trackId })
        assertEquals(0, back.missing)
    }

    @Test
    fun saveReportsAFailedWrite() = runTest {
        val result = repo(FakeStreams(failWrite = true)).save("content://picker/out", listOf(queueEntry(library[0])))
        assertEquals(SaveResult.Failed, result)
    }

    @Test
    fun saveReportsAnUnopenableTarget() = runTest {
        val streams = object : PlaylistStreams {
            override fun openInput(uri: String): InputStream? = null
            override fun openOutput(uri: String): OutputStream? = null
        }
        val result = PlaylistFileRepository(FakeIndex(library), streams).save("content://x", listOf(queueEntry(library[0])))
        assertEquals(SaveResult.Failed, result)
    }

    private class CountingDispatcher : kotlinx.coroutines.CoroutineDispatcher() {
        var dispatched = 0
        override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
            dispatched++
            block.run()
        }
    }

    private class ThrowingStreams : PlaylistStreams {
        override fun openInput(uri: String): InputStream? = throw IllegalStateException("provider died")
        override fun openOutput(uri: String): OutputStream? = throw IllegalArgumentException("bad uri")
    }

    @Test
    fun providerRuntimeExceptionsBecomeFailureResults() = runTest {
        val repo = PlaylistFileRepository(FakeIndex(library), ThrowingStreams())
        assertEquals(SaveResult.Failed, repo.save("content://x", listOf(queueEntry(library[0]))))
        assertEquals(LoadResult.Error(LoadResult.Reason.NotAPlaylist), repo.load("content://x", null))
    }

    @Test
    fun saveAndLoadRunOnTheInjectedDispatcher() = runTest {
        val d = CountingDispatcher()
        val streams = FakeStreams()
        val repo = PlaylistFileRepository(FakeIndex(library), streams, d)
        repo.save("content://x", listOf(queueEntry(library[0])))
        assertEquals(1, d.dispatched)
        repo.load("content://x", null)
        assertEquals(2, d.dispatched)
    }

    @Test
    fun baseDirFromDocumentId() {
        val f = PlaylistFileRepository.Companion::baseDirFromDocumentId
        assertEquals("/storage/emulated/0/Music", f("primary:Music/x.m3u"))
        assertEquals("/storage/emulated/0/Music/Rock", f("primary:Music/Rock/x.m3u"))
        assertEquals("/storage/emulated/0", f("primary:x.m3u"))
        assertNull(f("1234-ABCD:Music/x.m3u"))
        assertNull(f("msf:1234"))
        assertNull(f("raw:/storage/emulated/0/Download/x.m3u"))
        assertNull(f(null))
        assertNull(f(""))
    }

    @Test
    fun defaultFileName() {
        assertEquals(
            "canh-amp-20261118-0905.m3u8",
            PlaylistFileRepository.defaultFileName(LocalDateTime.of(2026, 11, 18, 9, 5, 33)),
        )
    }
}
