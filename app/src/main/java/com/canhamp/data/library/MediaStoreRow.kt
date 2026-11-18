package com.canhamp.data.library

/** One audio row as read from the MediaStore cursor; nullable fields are absent on older API levels. */
data class MediaStoreRow(
    val id: Long,
    val displayName: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val mimeType: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val bitrate: Int? = null,
    val relativePath: String? = null,
    val data: String? = null,
    val dateAddedSec: Long = 0L,
    val dateModifiedSec: Long = 0L,
)
