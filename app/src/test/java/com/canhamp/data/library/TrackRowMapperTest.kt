package com.canhamp.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackRowMapperTest {
    private fun row(
        name: String = "song.mp3",
        title: String? = "T",
        artist: String? = "A",
        album: String? = "B",
        mime: String? = "audio/mpeg",
        dur: Long = 200_000,
        size: Long = 4_000_000,
        bitrate: Int? = null,
        rel: String? = null,
        data: String? = null,
    ) = MediaStoreRow(1, name, title, artist, album, mime, dur, size, bitrate, rel, data)

    private fun map(r: MediaStoreRow) = TrackRowMapper.map(r, "content://x/1", 7)

    @Test fun basicFields() {
        val t = map(row(data = "/storage/emulated/0/Music/song.mp3"))
        assertEquals("content://x/1", t.uri)
        assertEquals("MEDIASTORE", t.source)
        assertEquals(1L, t.mediaStoreId)
        assertEquals(7L, t.scanGeneration)
        assertEquals("song.mp3", t.fileName)
        assertEquals("/storage/emulated/0/Music/song.mp3", t.filePath)
    }

    @Test fun titleFallsBackToFileNameWithoutExtension() {
        assertEquals("song", map(row(title = "")).title)
        assertEquals("song", map(row(title = null)).title)
        assertEquals("song", map(row(title = "<unknown>")).title)
    }

    @Test fun artistAndAlbumFallback() {
        val t = map(row(artist = " ", album = null))
        assertEquals("<unknown>", t.artist)
        assertEquals("<unknown>", t.album)
        assertEquals("<unknown>", map(row(artist = "<unknown>")).artist)
    }

    @Test fun textCappedAt256() {
        val t = map(row(title = "x".repeat(300), artist = "y".repeat(300), album = "z".repeat(257)))
        assertEquals(256, t.title.length)
        assertEquals(256, t.artist.length)
        assertEquals(256, t.album.length)
    }

    @Test fun formatFromExtensionFirst() {
        assertEquals("FLAC", map(row(name = "a.FLAC", mime = "audio/mpeg")).format)
    }

    @Test fun formatFromMimeWhenExtensionUnsupported() {
        assertEquals("M4A", map(row(name = "a.mp4", mime = "audio/mp4")).format)
        assertEquals("WAV", map(row(name = "a", mime = "audio/x-wav")).format)
        assertEquals("OPUS", map(row(name = "a.bin", mime = "audio/opus")).format)
    }

    @Test fun formatFallsBackToExtensionOrAudio() {
        assertEquals("WMA", map(row(name = "a.wma", mime = "audio/x-ms-wma")).format)
        assertEquals("AUDIO", map(row(name = "a", mime = null)).format)
    }

    @Test fun bitrateFromColumn() {
        assertEquals(320, map(row(bitrate = 320_000)).bitrateKbps)
    }

    @Test fun bitrateComputedWhenColumnMissingOrZero() {
        // 4_000_000 bytes * 8 / 200_000 ms = 160 kbps
        assertEquals(160, map(row(bitrate = null)).bitrateKbps)
        assertEquals(160, map(row(bitrate = 0)).bitrateKbps)
    }

    @Test fun bitrateNullWithoutDuration() {
        assertNull(map(row(dur = 0)).bitrateKbps)
    }

    @Test fun folderFromRelativePath() {
        assertEquals("/Music/Retro", map(row(rel = "Music/Retro/", data = "/a/b/c.mp3")).folderPath)
    }

    @Test fun folderFromDataParent() {
        assertEquals("/storage/0/Music", map(row(data = "/storage/0/Music/song.mp3")).folderPath)
        assertEquals("/", map(row(data = "song.mp3")).folderPath)
        assertEquals("/", map(row()).folderPath)
    }
}
