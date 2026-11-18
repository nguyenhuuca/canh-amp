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
