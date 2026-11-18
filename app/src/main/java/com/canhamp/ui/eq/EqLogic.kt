package com.canhamp.ui.eq

import com.canhamp.data.eq.BuiltInPresets
import com.canhamp.data.eq.EqPreset
import com.canhamp.playback.dsp.DspConfig
import com.canhamp.playback.dsp.ResponseCurve
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** The EQ state that persists: the DSP config plus the active preset name ("" means custom). */
data class EqModel(val config: DspConfig, val presetName: String)

/** Pure reducers and text helpers for the EQ screen; no Android types so JVM tests cover them. */
object EqLogic {
    const val CUSTOM = ""
    const val DEFAULT_SAMPLE_RATE = 48_000
    const val CURVE_POINTS = 64
    const val BAND_MIN = -12
    const val BAND_MAX = 12
    const val KNOB_MAX = 12
    private const val HZ_PER_KHZ = 1000

    /** B47: a manual band edit clears the preset name. */
    fun setBand(m: EqModel, index: Int, db: Int): EqModel {
        if (index !in 0 until DspConfig.BAND_COUNT) return m
        val gains = m.config.gainsDb.toMutableList()
        gains[index] = db.coerceIn(BAND_MIN, BAND_MAX)
        return m.copy(config = m.config.copy(gainsDb = gains), presetName = CUSTOM)
    }

    /** B47: a preamp change clears the preset name too. */
    fun setPreamp(m: EqModel, db: Int): EqModel =
        m.copy(config = m.config.copy(preampDb = db.coerceIn(BAND_MIN, BAND_MAX)), presetName = CUSTOM)

    fun setBass(m: EqModel, db: Int): EqModel = m.copy(config = m.config.copy(bassDb = db.coerceIn(0, KNOB_MAX)))

    fun setTreble(m: EqModel, db: Int): EqModel = m.copy(config = m.config.copy(trebleDb = db.coerceIn(0, KNOB_MAX)))

    fun setEnabled(m: EqModel, on: Boolean): EqModel = m.copy(config = m.config.copy(eqEnabled = on))

    /** B46: gains and preamp come from the preset, the name is recorded; bass, treble and balance stay. */
    fun applyPreset(m: EqModel, preset: EqPreset): EqModel =
        m.copy(config = m.config.copy(gainsDb = preset.gains, preampDb = preset.preamp), presetName = preset.name)

    /** B47: reset flat applies the Flat preset. */
    fun resetFlat(m: EqModel): EqModel = applyPreset(m, BuiltInPresets.FLAT)

    /** B49: effective pre-gain is the preamp minus any positive peak boost. */
    fun gainDb(preampDb: Int, peakBoostDb: Double): Double = preampDb - max(0.0, peakBoostDb)

    /** "+3.2 dB", "-1.0 dB", "0.0 dB". */
    fun gainValueText(db: Double): String {
        val tenths = (db * 10).roundToInt()
        val body = String.format(Locale.US, "%.1f", abs(tenths) / 10.0)
        val sign = when {
            tenths > 0 -> "+"
            tenths < 0 -> "-"
            else -> ""
        }
        return "$sign$body dB"
    }

    /** "[ROCK]" for a named preset, null for custom (the UI shows its localised label). */
    fun presetLabel(name: String): String? = name.takeIf { it.isNotBlank() }?.let { "[${it.uppercase()}]" }

    /** B45: 64 log points; flat when bypassed. A missing sample rate falls back to 48 kHz. */
    fun curve(config: DspConfig, sampleRateHz: Int?): List<Pair<Double, Double>> {
        val fs = (sampleRateHz?.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE).toDouble()
        return ResponseCurve.curve(config, fs, CURVE_POINTS)
    }

    fun peakBoostDb(config: DspConfig, sampleRateHz: Int?): Double {
        val fs = (sampleRateHz?.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE).toDouble()
        return ResponseCurve.peakBoostDb(config, fs)
    }

    /** "60", "170", "1K", "16K". */
    fun bandLabel(hz: Int): String = if (hz >= HZ_PER_KHZ) "${hz / HZ_PER_KHZ}K" else hz.toString()

    /** "+5", "-1", "0". */
    fun dbLabel(db: Int): String = if (db > 0) "+$db" else db.toString()
}
