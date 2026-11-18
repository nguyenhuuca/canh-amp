package com.canhamp.data.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uCodecTest {
    private val base = "/storage/emulated/0/Music"

    private fun entries(bytes: ByteArray): List<M3uEntry> =
        (M3uCodec.decode(bytes) as DecodeResult.Ok).entries

    private fun entries(text: String) = entries(text.toByteArray())

    private fun error(bytes: ByteArray) = (M3uCodec.decode(bytes) as DecodeResult.Error).reason

    @Test
    fun roundTrip() {
        val text = M3uCodec.encode(
            listOf(
                EncodeItem("/sdcard/a.mp3", 215, "Artist", "Song"),
                EncodeItem("content://x/1", 7, "B", "Ü"),
            ),
        )
        assertTrue(text.startsWith("#EXTM3U\n#EXTINF:215,Artist - Song\n/sdcard/a.mp3\n"))
        assertEquals(
            listOf(
                M3uEntry("/sdcard/a.mp3", 215, "Artist - Song"),
                M3uEntry("content://x/1", 7, "B - Ü"),
            ),
            entries(text.toByteArray(Charsets.UTF_8)),
        )
    }

    @Test
    fun encodeStripsNewlines() {
        val text = M3uCodec.encode(listOf(EncodeItem("/a\r\n#evil.mp3", 1, "A\nB", "T\r\nU")))
        assertEquals("#EXTM3U\n#EXTINF:1,AB - TU\n/a#evil.mp3\n", text)
    }

    @Test
    fun encodeEmptyIsHeaderOnly() {
        assertEquals("#EXTM3U\n", M3uCodec.encode(emptyList()))
    }

    @Test
    fun extinfParsed() {
        val e = entries("#EXTM3U\n#EXTINF:123,Foo - Bar\n/a.mp3\n")
        assertEquals(M3uEntry("/a.mp3", 123, "Foo - Bar"), e.single())
    }

    @Test
    fun negativeDurationIsNull() {
        val e = entries("#EXTINF:-1,Title\n/a.mp3\n").single()
        assertNull(e.durationSec)
        assertEquals("Title", e.displayTitle)
    }

    @Test
    fun garbageDurationIsNull() {
        val e = entries("#EXTINF:abc,Title\n/a.mp3\n").single()
        assertNull(e.durationSec)
        assertEquals("Title", e.displayTitle)
    }

    @Test
    fun extinfAppliesOnlyToNextLocation() {
        val e = entries("#EXTINF:5,T\n/a.mp3\n/b.mp3\n")
        assertEquals(5, e[0].durationSec)
        assertNull(e[1].durationSec)
        assertNull(e[1].displayTitle)
    }

    @Test
    fun commentsAndBlanksSkipped() {
        val e = entries("#EXTM3U\n# hi\n\n   \n/a.mp3\n")
        assertEquals(listOf("/a.mp3"), e.map { it.location })
    }

    @Test
    fun crlfAccepted() {
        val e = entries("#EXTM3U\r\n#EXTINF:3,T\r\n/a.mp3\r\n/b.mp3\r\n")
        assertEquals(listOf("/a.mp3", "/b.mp3"), e.map { it.location })
        assertEquals("T", e[0].displayTitle)
    }

    @Test
    fun bomStripped() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "#EXTM3U\n/a.mp3\n".toByteArray()
        assertEquals(listOf("/a.mp3"), entries(bytes).map { it.location })
    }

    @Test
    fun latin1Fallback() {
        val bytes = byteArrayOf('/'.code.toByte(), 'c'.code.toByte(), 'a'.code.toByte(), 'f'.code.toByte(), 0xE9.toByte())
        assertEquals("/café", entries(bytes).single().location)
    }

    @Test
    fun emptyAndCommentOnlyAreOkEmpty() {
        assertTrue(entries(ByteArray(0)).isEmpty())
        assertTrue(entries("#EXTM3U\n").isEmpty())
    }

    @Test
    fun sizeLimit() {
        val at = ByteArray(M3uCodec.MAX_BYTES) { '\n'.code.toByte() }
        assertTrue(M3uCodec.decode(at) is DecodeResult.Ok || error(at) != DecodeResult.Reason.TooLarge)
        val over = ByteArray(M3uCodec.MAX_BYTES + 1)
        assertEquals(DecodeResult.Reason.TooLarge, error(over))
    }

    @Test
    fun lineCountLimit() {
        val ok = "/a\n".repeat(M3uCodec.MAX_LINES)
        assertEquals(M3uCodec.MAX_LINES, entries(ok).size)
        val over = "/a\n".repeat(M3uCodec.MAX_LINES + 1)
        assertEquals(DecodeResult.Reason.NotAPlaylist, error(over.toByteArray()))
    }

    @Test
    fun lineLengthLimit() {
        val ok = "/" + "a".repeat(M3uCodec.MAX_LINE_CHARS - 1)
        assertEquals(1, entries(ok).size)
        val over = "/" + "a".repeat(M3uCodec.MAX_LINE_CHARS)
        assertEquals(DecodeResult.Reason.NotAPlaylist, error(over.toByteArray()))
    }

    @Test
    fun classifyContent() {
        assertEquals(M3uLocation.Content("content://media/1"), M3uCodec.classify("content://media/1", base))
    }

    @Test
    fun classifyAbsolute() {
        assertEquals(M3uLocation.AbsolutePath("/sdcard/a.mp3"), M3uCodec.classify("/sdcard/a.mp3", null))
    }

    @Test
    fun classifyRelativeWithBase() {
        assertEquals(
            M3uLocation.AbsolutePath("$base/sub/a.mp3"),
            M3uCodec.classify("sub/./a.mp3", base),
        )
    }

    @Test
    fun classifyParentNormalised() {
        assertEquals(
            M3uLocation.AbsolutePath("/storage/emulated/0/x.mp3"),
            M3uCodec.classify("../x.mp3", base),
        )
    }

    @Test
    fun classifyNeverEscapesRoot() {
        assertEquals(
            M3uLocation.AbsolutePath("/etc"),
            M3uCodec.classify("../../../../../../etc", base),
        )
        assertEquals(M3uLocation.AbsolutePath("/etc/passwd"), M3uCodec.classify("/../../etc/passwd", null))
    }

    @Test
    fun classifyFileUriDecoded() {
        assertEquals(
            M3uLocation.AbsolutePath("/storage/emulated/0/Music/a b.mp3"),
            M3uCodec.classify("file:///storage/emulated/0/Music/a%20b.mp3", null),
        )
    }

    @Test
    fun classifyFileUriBadEscapeUnresolvable() {
        assertEquals(M3uLocation.Unresolvable, M3uCodec.classify("file:///a%zz.mp3", null))
        assertEquals(M3uLocation.Unresolvable, M3uCodec.classify("file:///a%2", null))
    }

    @Test
    fun classifyFileUriKeepsPlusAndTraversal() {
        assertEquals(
            M3uLocation.AbsolutePath("/etc/a+b"),
            M3uCodec.classify("file:///x/%2e%2e/etc/a+b", null),
        )
    }

    @Test
    fun classifyRemote() {
        assertEquals(M3uLocation.Remote("http://x"), M3uCodec.classify("http://x", base))
        assertEquals(M3uLocation.Remote("https://x/y.mp3"), M3uCodec.classify("https://x/y.mp3", null))
        assertTrue(M3uCodec.classify("rtsp://x", null) is M3uLocation.Remote)
    }

    @Test
    fun classifyRelativeWithoutBase() {
        assertEquals(M3uLocation.Unresolvable, M3uCodec.classify("a.mp3", null))
        assertEquals(M3uLocation.Unresolvable, M3uCodec.classify("", base))
    }

    @Test
    fun classifyBackslashes() {
        assertEquals(M3uLocation.AbsolutePath("$base/sub/a.mp3"), M3uCodec.classify("sub\\a.mp3", base))
        assertEquals(M3uLocation.AbsolutePath("/sdcard/m/a.mp3"), M3uCodec.classify("\\sdcard\\m\\a.mp3", null))
    }

    @Test
    fun classifyDriveLetterUnresolvable() {
        assertFalse(M3uCodec.classify("C:\\Music\\a.mp3", base) is M3uLocation.AbsolutePath)
    }
}
