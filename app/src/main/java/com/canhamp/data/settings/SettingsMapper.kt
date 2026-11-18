package com.canhamp.data.settings

import com.canhamp.ui.theme.ThemeId

/** Pure mapping between stored preference values and [Settings]; no Android dependencies. */
object SettingsMapper {
    const val BANDS = 10
    const val GAIN_MIN = -12
    const val GAIN_MAX = 12

    const val THEME = "theme"
    const val SHUFFLE = "shuffle"
    const val REPEAT = "repeat"
    const val VOLUME = "volume"
    const val BALANCE = "balance"
    const val EQ_ENABLED = "eqEnabled"
    const val EQ_GAINS = "eqGains"
    const val EQ_PREAMP = "eqPreamp"
    const val EQ_PRESET_NAME = "eqPresetName"
    const val BASS_DB = "bassDb"
    const val TREBLE_DB = "trebleDb"
    const val TIME_MODE = "timeMode"
    const val VIS_MODE = "visMode"
    const val VIS_ANIMATED = "visAnimated"
    const val QUEUE_INDEX = "queueIndex"
    const val QUEUE_POSITION_MS = "queuePositionMs"
    const val SCAN_GENERATION = "scanGeneration"
    const val LAST_SCAN_AT_MS = "lastScanAtMs"

    /** Ten integers clamped to -12..12; null, wrong length or any non-integer yields zeros. */
    fun parseGains(csv: String?): IntArray {
        val zeros = IntArray(BANDS)
        if (csv.isNullOrBlank()) return zeros
        val parts = csv.split(',')
        if (parts.size != BANDS) return zeros
        val out = IntArray(BANDS)
        for (i in parts.indices) {
            out[i] = (parts[i].trim().toIntOrNull() ?: return zeros).coerceIn(GAIN_MIN, GAIN_MAX)
        }
        return out
    }

    fun formatGains(gains: IntArray): String {
        val fixed = IntArray(BANDS) { gains.getOrElse(it) { 0 } }
        return fixed.joinToString(",") { it.coerceIn(GAIN_MIN, GAIN_MAX).toString() }
    }

    fun fromPreferences(map: Map<String, Any?>): Settings {
        val d = Settings()
        fun bool(k: String, def: Boolean) = map[k] as? Boolean ?: def
        fun int(k: String, def: Int) = map[k] as? Int ?: def
        fun float(k: String, def: Float, lo: Float, hi: Float): Float {
            val v = map[k] as? Float ?: return def
            return if (v.isFinite()) v.coerceIn(lo, hi) else def
        }
        return Settings(
            theme = (map[THEME] as? String)?.let { n -> ThemeId.entries.firstOrNull { it.name == n } } ?: d.theme,
            shuffle = bool(SHUFFLE, d.shuffle),
            repeat = enumOf<RepeatSetting>(map[REPEAT]) ?: d.repeat,
            volume = float(VOLUME, d.volume, 0f, 1f),
            balance = float(BALANCE, d.balance, -1f, 1f),
            eqEnabled = bool(EQ_ENABLED, d.eqEnabled),
            eqGains = parseGains(map[EQ_GAINS] as? String),
            eqPreamp = int(EQ_PREAMP, d.eqPreamp).coerceIn(GAIN_MIN, GAIN_MAX),
            eqPresetName = map[EQ_PRESET_NAME] as? String ?: d.eqPresetName,
            bassDb = int(BASS_DB, d.bassDb).coerceIn(0, GAIN_MAX),
            trebleDb = int(TREBLE_DB, d.trebleDb).coerceIn(0, GAIN_MAX),
            timeMode = enumOf<TimeMode>(map[TIME_MODE]) ?: d.timeMode,
            visMode = enumOf<VisMode>(map[VIS_MODE]) ?: d.visMode,
            visAnimated = bool(VIS_ANIMATED, d.visAnimated),
            queueIndex = int(QUEUE_INDEX, d.queueIndex).coerceAtLeast(0),
            queuePositionMs = (map[QUEUE_POSITION_MS] as? Long ?: d.queuePositionMs).coerceAtLeast(0L),
            scanGeneration = map[SCAN_GENERATION] as? Long ?: d.scanGeneration,
            lastScanAtMs = (map[LAST_SCAN_AT_MS] as? Long ?: d.lastScanAtMs).coerceAtLeast(0L),
        )
    }

    private inline fun <reified E : Enum<E>> enumOf(v: Any?): E? =
        (v as? String)?.let { n -> enumValues<E>().firstOrNull { it.name == n } }
}
