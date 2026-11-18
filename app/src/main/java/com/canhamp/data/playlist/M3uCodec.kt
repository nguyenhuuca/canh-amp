package com.canhamp.data.playlist

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Pure M3U/M3U8 reader and writer. Playlist content is untrusted (B76). */
object M3uCodec {
    const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_LINES = 10_000
    const val MAX_LINE_CHARS = 4_096

    private const val EXTINF = "#EXTINF:"
    private val schemeRegex = Regex("^([A-Za-z][A-Za-z0-9+.-]*)://")
    private val driveRegex = Regex("^[A-Za-z]:([/\\\\]|$)")

    fun encode(items: List<EncodeItem>): String {
        val sb = StringBuilder("#EXTM3U\n")
        for (item in items) {
            sb.append(EXTINF).append(item.durationSec)
                .append(',').append(oneLine(item.artist)).append(" - ").append(oneLine(item.title)).append('\n')
                .append(oneLine(item.path)).append('\n')
        }
        return sb.toString()
    }

    private fun oneLine(s: String) = s.replace("\r", "").replace("\n", "")

    fun decode(bytes: ByteArray): DecodeResult {
        if (bytes.size > MAX_BYTES) return DecodeResult.Error(DecodeResult.Reason.TooLarge)
        val hasBom = bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        val body = if (hasBom) bytes.copyOfRange(3, bytes.size) else bytes
        val text = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(body)).toString()
        } catch (_: CharacterCodingException) {
            String(body, StandardCharsets.ISO_8859_1)
        }
        val lines = text.split('\n').let { if (it.last().isEmpty()) it.dropLast(1) else it }
        if (lines.size > MAX_LINES) return DecodeResult.Error(DecodeResult.Reason.NotAPlaylist)

        val entries = ArrayList<M3uEntry>()
        var pendingDuration: Int? = null
        var pendingTitle: String? = null
        for (raw in lines) {
            val line = raw.removeSuffix("\r")
            if (line.length > MAX_LINE_CHARS) return DecodeResult.Error(DecodeResult.Reason.NotAPlaylist)
            val t = line.trim()
            when {
                t.isEmpty() -> Unit
                t.startsWith(EXTINF) -> {
                    val rest = t.substring(EXTINF.length)
                    val comma = rest.indexOf(',')
                    val durText = if (comma >= 0) rest.substring(0, comma) else rest
                    pendingDuration = durText.trim().toIntOrNull()?.takeIf { it >= 0 }
                    pendingTitle = if (comma >= 0) rest.substring(comma + 1).trim().ifEmpty { null } else null
                }
                t.startsWith("#") -> Unit
                else -> {
                    entries += M3uEntry(t, pendingDuration, pendingTitle)
                    pendingDuration = null
                    pendingTitle = null
                }
            }
        }
        return DecodeResult.Ok(entries)
    }

    fun classify(location: String, baseDir: String?): M3uLocation {
        val loc = location.trim()
        if (loc.isEmpty()) return M3uLocation.Unresolvable
        val scheme = schemeRegex.find(loc)?.groupValues?.get(1)?.lowercase()
        if (scheme != null) {
            return when (scheme) {
                "content" -> M3uLocation.Content(loc)
                "file" -> fileUri(loc)
                else -> M3uLocation.Remote(loc)
            }
        }
        if (driveRegex.containsMatchIn(loc)) return M3uLocation.Unresolvable
        val path = loc.replace('\\', '/')
        if (path.startsWith("/")) return M3uLocation.AbsolutePath(normalise(path))
        val base = baseDir?.replace('\\', '/')?.takeIf { it.startsWith("/") }
            ?: return M3uLocation.Unresolvable
        return M3uLocation.AbsolutePath(normalise("$base/$path"))
    }

    private fun fileUri(uri: String): M3uLocation {
        var rest = uri.substring("file://".length)
        if (!rest.startsWith("/")) {
            val slash = rest.indexOf('/')
            if (slash < 0) return M3uLocation.Unresolvable
            rest = rest.substring(slash) // drop host part
        }
        val decoded = percentDecode(rest) ?: return M3uLocation.Unresolvable
        return M3uLocation.AbsolutePath(normalise(decoded.replace('\\', '/')))
    }

    private fun percentDecode(s: String): String? {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%') {
                if (i + 3 > s.length) return null
                val b = s.substring(i + 1, i + 3).toIntOrNull(16) ?: return null
                out.write(b)
                i += 3
            } else {
                out.write(c.toString().toByteArray(StandardCharsets.UTF_8))
                i++
            }
        }
        return try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(out.toByteArray())).toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }

    /** Resolves `.` and `..`; `..` at the root is dropped so a path never escapes "/". */
    private fun normalise(path: String): String {
        val stack = ArrayList<String>()
        for (seg in path.split('/')) {
            when (seg) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                else -> stack += seg
            }
        }
        return "/" + stack.joinToString("/")
    }
}
