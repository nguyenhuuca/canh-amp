package com.canhamp.data.settings

import com.canhamp.ui.theme.ThemeId

enum class RepeatSetting { OFF, ALL, ONE }
enum class TimeMode { ELAPSED, REMAINING }
enum class VisMode { BAR, WAVE }

data class Settings(
    val theme: ThemeId = ThemeId.CLASSIC,
    val shuffle: Boolean = false,
    val repeat: RepeatSetting = RepeatSetting.OFF,
    val volume: Float = 0.8f,
    val balance: Float = 0f,
    val eqEnabled: Boolean = true,
    val eqGains: IntArray = IntArray(SettingsMapper.BANDS),
    val eqPreamp: Int = 0,
    val eqPresetName: String = "Flat",
    val bassDb: Int = 0,
    val trebleDb: Int = 0,
    val timeMode: TimeMode = TimeMode.ELAPSED,
    val visMode: VisMode = VisMode.BAR,
    val visAnimated: Boolean = true,
    val queueIndex: Int = 0,
    val queuePositionMs: Long = 0L,
    val scanGeneration: Long = 0L,
) {
    override fun equals(other: Any?): Boolean =
        other is Settings && theme == other.theme && shuffle == other.shuffle && repeat == other.repeat &&
            volume == other.volume && balance == other.balance && eqEnabled == other.eqEnabled &&
            eqGains.contentEquals(other.eqGains) && eqPreamp == other.eqPreamp &&
            eqPresetName == other.eqPresetName && bassDb == other.bassDb && trebleDb == other.trebleDb &&
            timeMode == other.timeMode && visMode == other.visMode && visAnimated == other.visAnimated &&
            queueIndex == other.queueIndex && queuePositionMs == other.queuePositionMs &&
            scanGeneration == other.scanGeneration

    override fun hashCode(): Int = listOf(
        theme, shuffle, repeat, volume, balance, eqEnabled, eqGains.contentHashCode(), eqPreamp,
        eqPresetName, bassDb, trebleDb, timeMode, visMode, visAnimated, queueIndex, queuePositionMs,
        scanGeneration,
    ).hashCode()
}
