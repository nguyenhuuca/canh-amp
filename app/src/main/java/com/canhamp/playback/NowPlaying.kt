package com.canhamp.playback

import com.canhamp.data.settings.RepeatSetting

/** One playable track handed to the player. */
data class QueueEntry(
    val trackId: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val bitrateKbps: Int?,
)

/** What the UI shows about playback; [queueSize] 0 means nothing is loaded. */
data class NowPlaying(
    val trackId: Long? = null,
    val uri: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val index: Int = 0,
    val queueSize: Int = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatSetting = RepeatSetting.OFF,
    val volume: Float = 0.8f,
    /** Embedded cover bytes (B32); decoded by the UI. */
    val artworkData: ByteArray? = null,
    /** Sample MIME type of the selected audio track, for the format badge. */
    val mimeType: String? = null,
) {
    companion object {
        val EMPTY = NowPlaying()
    }
}
