package com.canhamp.data.playlist

/** One playable line of a playlist file, with the optional #EXTINF data that preceded it. */
data class M3uEntry(
    val location: String,
    val durationSec: Int?,
    val displayTitle: String?,
)

/** Where a playlist location points. Produced by [M3uCodec.classify]. */
sealed interface M3uLocation {
    data class Content(val uri: String) : M3uLocation
    data class AbsolutePath(val path: String) : M3uLocation
    data class Remote(val url: String) : M3uLocation
    data object Unresolvable : M3uLocation
}

/** One track to write with [M3uCodec.encode]. */
data class EncodeItem(
    val path: String,
    val durationSec: Int,
    val artist: String,
    val title: String,
)

sealed interface DecodeResult {
    data class Ok(val entries: List<M3uEntry>) : DecodeResult
    data class Error(val reason: Reason) : DecodeResult

    enum class Reason { TooLarge, NotAPlaylist }
}
