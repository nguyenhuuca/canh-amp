package com.canhamp.util

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_HOUR = 3600L

/** "mm:ss" below one hour, "h:mm:ss" from one hour up. Negative input clamps to zero. */
fun formatDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / MS_PER_SECOND
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/** Time left as "-mm:ss"; position past the end clamps to "-00:00". */
fun formatRemaining(positionMs: Long, durationMs: Long): String =
    "-" + formatDuration((durationMs - positionMs).coerceAtLeast(0L))
