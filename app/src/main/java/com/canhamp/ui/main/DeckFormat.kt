package com.canhamp.ui.main

import com.canhamp.data.settings.TimeMode
import com.canhamp.playback.NowPlaying
import com.canhamp.util.formatDuration
import com.canhamp.util.formatRemaining
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pure text and value helpers for the main deck, kept free of Android types for JVM tests. */
object DeckFormat {

    const val NO_VALUE = "--"
    const val EMPTY_TIMER = "--:--"
    const val BALANCE_STEP = 0.05f
    private const val BALANCE_STEPS_PER_UNIT = 20f
    private const val PERCENT = 100f
    private const val HZ_PER_KHZ = 1000f

    /** B24: elapsed "mm:ss" or remaining "-mm:ss", "h:mm:ss" from one hour up, "--:--" with no queue. */
    fun timerText(np: NowPlaying, mode: TimeMode): String = when {
        np.queueSize == 0 -> EMPTY_TIMER
        mode == TimeMode.REMAINING -> formatRemaining(np.positionMs, np.durationMs)
        else -> formatDuration(np.positionMs)
    }

    /** B25 marquee line; [idle] is the localized empty-queue text. */
    fun marqueeText(np: NowPlaying, idle: String): String {
        if (np.queueSize == 0) return idle
        val who = if (np.artist.isBlank()) np.title else "${np.artist} - ${np.title}"
        val tech = listOfNotNull(
            np.bitrateKbps?.let { "${it}kbps" },
            channelLabel(np.channelCount).takeIf { np.channelCount != null }?.lowercase(),
            np.sampleRateHz?.let { "${kHz(it)}kHz" },
        ).joinToString(" ")
        val bracket = if (tech.isEmpty()) "" else " [$tech]"
        return "*** ${np.index + 1}. $who (${formatDuration(np.durationMs)})$bracket ***"
    }

    /** 44100 -> "44.1"; unknown -> "--". */
    fun kHz(hz: Int?): String =
        if (hz == null || hz <= 0) NO_VALUE else "%.1f".format(java.util.Locale.US, hz / HZ_PER_KHZ)

    /** STEREO for two or more channels, MONO for one, "--" when unknown. */
    fun channelLabel(count: Int?): String = when {
        count == null -> NO_VALUE
        count >= 2 -> "STEREO"
        else -> "MONO"
    }

    /** "TRACK 01" from the 0-based queue index; "TRACK --" with no queue. */
    fun trackNumber(index: Int, queueSize: Int): String =
        if (queueSize == 0) "TRACK $NO_VALUE" else "TRACK %02d".format(index + 1)

    fun kbpsText(kbps: Int?): String = kbps?.toString() ?: NO_VALUE

    /** Badge text for an audio sample MIME type, or null when nothing is known. */
    fun formatBadge(mimeType: String?): String? = when (mimeType?.lowercase()) {
        null, "" -> null
        "audio/mpeg" -> "MPEG-1 LAYER 3"
        "audio/mpeg-l2" -> "MPEG-1 LAYER 2"
        "audio/flac" -> "FLAC"
        "audio/mp4a-latm" -> "AAC"
        "audio/vorbis" -> "VORBIS"
        "audio/opus" -> "OPUS"
        "audio/raw" -> "PCM"
        "audio/alac" -> "ALAC"
        else -> mimeType.substringAfter('/').uppercase()
    }

    /** [center] when the balance is centred, otherwise "L 40" or "R 40". */
    fun balanceLabel(balance: Float, center: String): String {
        val pct = (abs(balance) * PERCENT).roundToInt()
        return when {
            pct == 0 -> center
            balance < 0f -> "L $pct"
            else -> "R $pct"
        }
    }

    /** B30: steps of 0.05 over -1..1, with a snap to 0 when |b| < 0.05. */
    fun snapBalance(raw: Float): Float {
        if (raw.isNaN()) return 0f
        val v = raw.coerceIn(-1f, 1f)
        if (abs(v) < BALANCE_STEP) return 0f
        return (v / BALANCE_STEP).roundToInt() / BALANCE_STEPS_PER_UNIT
    }

    /** Fader value 0..1 to balance -1..1, and back. */
    fun balanceFromFader(v: Float): Float = snapBalance(v * 2f - 1f)

    fun faderFromBalance(balance: Float): Float = (balance.coerceIn(-1f, 1f) + 1f) / 2f
}
