package com.canhamp.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

internal const val EXTRA_TRACK_ID = "canhamp.trackId"
internal const val EXTRA_BITRATE_KBPS = "canhamp.bitrateKbps"
internal const val EXTRA_DURATION_MS = "canhamp.durationMs"

/** Builds the media item the session plays; ids and bitrate ride in the metadata extras. */
fun QueueEntry.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_TRACK_ID, trackId)
        putInt(EXTRA_BITRATE_KBPS, bitrateKbps ?: 0)
        putLong(EXTRA_DURATION_MS, durationMs)
    }
    return MediaItem.Builder()
        .setMediaId(trackId.toString())
        .setUri(Uri.parse(uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setExtras(extras)
                .build(),
        )
        .build()
}

/** Rebuilds the queue row for a player item (B55); null when the item has no uri. */
fun MediaItem.toQueueEntry(): QueueEntry? {
    val itemUri = localConfiguration?.uri?.toString() ?: return null
    val meta = mediaMetadata
    val extras = meta.extras
    val id = extras?.takeIf { it.containsKey(EXTRA_TRACK_ID) }?.getLong(EXTRA_TRACK_ID)
        ?: mediaId.toLongOrNull() ?: -1L
    return QueueEntry(
        trackId = id,
        uri = itemUri,
        title = meta.title?.toString().orEmpty(),
        artist = meta.artist?.toString().orEmpty(),
        album = meta.albumTitle?.toString().orEmpty(),
        durationMs = extras?.getLong(EXTRA_DURATION_MS, 0L) ?: 0L,
        bitrateKbps = extras?.getInt(EXTRA_BITRATE_KBPS, 0)?.takeIf { it > 0 },
    )
}
