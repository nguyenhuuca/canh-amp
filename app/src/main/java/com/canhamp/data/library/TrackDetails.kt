package com.canhamp.data.library

import com.canhamp.data.db.TrackEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Tags and stream facts read from the file itself; every string is already capped (B76). */
data class TrackTags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val year: String? = null,
    val genre: String? = null,
    val trackNumber: String? = null,
    val mimeType: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val durationMs: Long? = null,
)

/** Row fields merged with [TrackTags]. */
data class TrackDetails(
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val year: String?,
    val genre: String?,
    val trackNumber: String?,
    val format: String,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val durationMs: Long,
    val sizeBytes: Long,
    val filePath: String?,
    val dateModifiedSec: Long,
)

sealed interface DetailsResult {
    data class Found(val details: TrackDetails) : DetailsResult

    /** The file could not be opened; [details] holds what the library row knows. */
    data class Unreadable(val details: TrackDetails) : DetailsResult
    data object NotFound : DetailsResult
}

/** Reads tags for a content URI; null when the file cannot be read. */
interface TagSource {
    suspend fun read(uri: String): TrackTags?
}

/** The slice of the track table the details flow needs. */
interface TrackLookup {
    suspend fun findById(id: Long): TrackEntity?
    suspend fun updateSampleRate(id: Long, hz: Int)
}

internal fun TrackEntity.toDetails(tags: TrackTags?) = TrackDetails(
    trackId = id,
    title = title,
    artist = artist,
    album = album,
    year = tags?.year,
    genre = tags?.genre,
    trackNumber = tags?.trackNumber,
    format = format,
    bitrateKbps = bitrateKbps ?: tags?.bitrateKbps,
    sampleRateHz = sampleRateHz ?: tags?.sampleRateHz,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    filePath = filePath,
    dateModifiedSec = dateModifiedSec,
)

/** Pure display helpers for the details sheet. */
object TrackDetailsFormat {
    const val NO_VALUE = "—"
    const val TAG_MAX = 256
    private const val KIB = 1024.0
    private const val MS_PER_SEC = 1000L

    /** Trims, drops control characters and caps at [TAG_MAX]; blank becomes null (B76). */
    fun capTag(raw: String?): String? =
        raw?.filterNot { it.isISOControl() }?.trim()?.take(TAG_MAX)?.trim()?.takeIf { it.isNotEmpty() }

    fun text(value: String?): String = capTag(value) ?: NO_VALUE

    fun bitrate(kbps: Int?): String = if (kbps != null && kbps > 0) "$kbps kbps" else NO_VALUE

    fun sampleRate(hz: Int?): String =
        if (hz != null && hz > 0) String.format(Locale.ROOT, "%.1f kHz", hz / 1000.0) else NO_VALUE

    fun size(bytes: Long): String = when {
        bytes <= 0 -> NO_VALUE
        bytes < KIB -> "$bytes B"
        bytes < KIB * KIB -> String.format(Locale.ROOT, "%.1f KB", bytes / KIB)
        bytes < KIB * KIB * KIB -> String.format(Locale.ROOT, "%.1f MB", bytes / (KIB * KIB))
        else -> String.format(Locale.ROOT, "%.2f GB", bytes / (KIB * KIB * KIB))
    }

    fun modified(sec: Long, zone: TimeZone = TimeZone.getDefault()): String {
        if (sec <= 0) return NO_VALUE
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply { timeZone = zone }.format(Date(sec * MS_PER_SEC))
    }
}
