package com.canhamp.data.library

import com.canhamp.data.db.TrackEntity
import com.canhamp.data.db.TrackSource

/** Pure MediaStore row to [TrackEntity] mapping (B3). */
object TrackRowMapper {
    const val UNKNOWN = "<unknown>"
    const val MAX_TEXT = 256

    private val SUPPORTED = setOf("MP3", "FLAC", "OGG", "OPUS", "M4A", "AAC", "WAV")
    private val MIME_FORMAT = mapOf(
        "audio/mpeg" to "MP3",
        "audio/flac" to "FLAC",
        "audio/ogg" to "OGG",
        "audio/opus" to "OPUS",
        "audio/mp4" to "M4A",
        "audio/x-m4a" to "M4A",
        "audio/aac" to "AAC",
        "audio/wav" to "WAV",
        "audio/x-wav" to "WAV",
    )

    fun map(row: MediaStoreRow, contentUri: String, generation: Long): TrackEntity {
        val baseName = row.displayName.substringBeforeLast('.', row.displayName)
        return TrackEntity(
            uri = contentUri,
            source = TrackSource.MEDIASTORE.name,
            mediaStoreId = row.id,
            filePath = row.data,
            folderPath = folderOf(row),
            fileName = row.displayName,
            title = textOr(row.title, baseName),
            artist = textOr(row.artist, UNKNOWN),
            album = textOr(row.album, UNKNOWN),
            format = formatOf(row),
            mimeType = row.mimeType,
            durationMs = row.durationMs,
            bitrateKbps = bitrateOf(row),
            sizeBytes = row.sizeBytes,
            dateAddedSec = row.dateAddedSec,
            dateModifiedSec = row.dateModifiedSec,
            scanGeneration = generation,
        )
    }

    private fun textOr(value: String?, fallback: String): String {
        val v = value?.trim().orEmpty()
        val chosen = if (v.isEmpty() || v == UNKNOWN) fallback else v
        return chosen.ifEmpty { UNKNOWN }.take(MAX_TEXT)
    }

    private fun formatOf(row: MediaStoreRow): String {
        val ext = row.displayName.substringAfterLast('.', "").uppercase()
        if (ext in SUPPORTED) return ext
        MIME_FORMAT[row.mimeType?.lowercase()]?.let { return it }
        return ext.ifEmpty { "AUDIO" }
    }

    private fun bitrateOf(row: MediaStoreRow): Int? {
        val fromColumn = (row.bitrate ?: 0) / 1000
        if (fromColumn > 0) return fromColumn
        if (row.durationMs <= 0L) return null
        return Math.round(row.sizeBytes * 8.0 / row.durationMs).toInt()
    }

    private fun folderOf(row: MediaStoreRow): String {
        row.relativePath?.let { rel ->
            val trimmed = rel.trim().trim('/')
            return if (trimmed.isEmpty()) "/" else "/$trimmed"
        }
        val data = row.data ?: return "/"
        val parent = data.substringBeforeLast('/', "")
        return parent.ifEmpty { "/" }
    }
}
