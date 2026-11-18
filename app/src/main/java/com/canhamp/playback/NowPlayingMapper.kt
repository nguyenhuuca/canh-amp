package com.canhamp.playback

import androidx.media3.common.Player
import com.canhamp.data.settings.RepeatSetting

/** Pure mappings between player values and UI values. */
object NowPlayingMapper {

    /** B22: past this point Previous restarts the current item. */
    const val PREVIOUS_RESTART_MS = 3_000L

    enum class PreviousAction { RESTART, PREVIOUS_ITEM }

    fun previousAction(positionMs: Long): PreviousAction =
        if (positionMs > PREVIOUS_RESTART_MS) PreviousAction.RESTART else PreviousAction.PREVIOUS_ITEM

    /** Format.bitrate is bits per second, or -1 when unknown. */
    fun bitrateKbps(bitsPerSecond: Int): Int? =
        if (bitsPerSecond <= 0) null else (bitsPerSecond + 500) / 1000

    fun sampleRateHz(hz: Int): Int? = if (hz <= 0) null else hz

    fun channelCount(count: Int): Int? = if (count <= 0) null else count

    /** True for stereo or wider, false for mono, null when unknown. */
    fun isStereo(channelCount: Int?): Boolean? = channelCount?.let { it >= 2 }

    fun repeatFromPlayer(mode: Int): RepeatSetting = when (mode) {
        Player.REPEAT_MODE_ALL -> RepeatSetting.ALL
        Player.REPEAT_MODE_ONE -> RepeatSetting.ONE
        else -> RepeatSetting.OFF
    }

    fun repeatToPlayer(repeat: RepeatSetting): Int = when (repeat) {
        RepeatSetting.OFF -> Player.REPEAT_MODE_OFF
        RepeatSetting.ALL -> Player.REPEAT_MODE_ALL
        RepeatSetting.ONE -> Player.REPEAT_MODE_ONE
    }

    /** OFF -> ALL -> ONE -> OFF (B31). */
    fun nextRepeat(repeat: RepeatSetting): RepeatSetting = when (repeat) {
        RepeatSetting.OFF -> RepeatSetting.ALL
        RepeatSetting.ALL -> RepeatSetting.ONE
        RepeatSetting.ONE -> RepeatSetting.OFF
    }
}
