package com.canhamp.playback.dsp

import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

/** Analytic magnitude response of a [DspConfig] (spec B45) and the peak boost used for headroom (B49). */
object ResponseCurve {
    const val MIN_HZ = 20.0
    const val MAX_HZ = 20000.0
    private const val NYQUIST_FRACTION = 0.45
    private const val GRID_POINTS = 256

    /** Bands, bass and treble only (no preamp); sections the processor would skip are skipped here too. */
    private fun sections(config: DspConfig, fs: Double): List<BiquadCoeffs> {
        val limit = NYQUIST_FRACTION * fs
        val out = ArrayList<BiquadCoeffs>(DspConfig.BAND_COUNT + 2)
        for (band in 0 until DspConfig.BAND_COUNT) {
            val hz = DspConfig.BANDS_HZ[band]
            val g = config.gainsDb[band]
            if (g != 0 && hz < limit) {
                out += BiquadDesign.peaking(fs, hz.toDouble(), DspConfig.Q, g.toDouble())
            }
        }
        if (config.bassDb > 0) {
            out += BiquadDesign.lowShelf(fs, DspConfig.BASS_HZ.toDouble(), 1.0, config.bassDb.toDouble())
        }
        if (config.trebleDb > 0 && DspConfig.TREBLE_HZ < limit) {
            out += BiquadDesign.highShelf(fs, DspConfig.TREBLE_HZ.toDouble(), 1.0, config.trebleDb.toDouble())
        }
        return out
    }

    private fun sumDb(sections: List<BiquadCoeffs>, fs: Double, f: Double): Double {
        var db = 0.0
        for (c in sections) db += BiquadDesign.magnitudeDb(c, fs, f)
        return db
    }

    /** Response at [f] in dB: every active section plus the preamp. Bypassed configs are flat (0 dB). */
    fun magnitudeDb(config: DspConfig, fs: Double, f: Double): Double {
        if (!config.eqEnabled) return 0.0
        return sumDb(sections(config, fs), fs, f) + config.preampDb
    }

    /** [points] (frequency, dB) pairs, log-spaced from 20 Hz to 20 kHz. */
    fun curve(config: DspConfig, fs: Double = 48000.0, points: Int = 64): List<Pair<Double, Double>> {
        require(points >= 2) { "points must be >= 2" }
        val secs = if (config.eqEnabled) sections(config, fs) else emptyList()
        val pre = if (config.eqEnabled) config.preampDb.toDouble() else 0.0
        return logGrid(MIN_HZ, MAX_HZ, points).map { f -> f to sumDb(secs, fs, f) + pre }
    }

    /** Peak of bands + shelves (preamp excluded) over the audible grid, never below 0 dB. */
    fun peakBoostDb(config: DspConfig, fs: Double): Double {
        if (!config.eqEnabled) return 0.0
        val secs = sections(config, fs)
        if (secs.isEmpty()) return 0.0
        var peak = 0.0
        for (f in logGrid(MIN_HZ, min(MAX_HZ, NYQUIST_FRACTION * fs), GRID_POINTS)) {
            peak = maxOf(peak, sumDb(secs, fs, f))
        }
        return peak
    }

    private fun logGrid(lo: Double, hi: Double, n: Int): List<Double> {
        val l0 = log10(lo)
        val step = (log10(hi) - l0) / (n - 1)
        return List(n) { 10.0.pow(l0 + step * it) }
    }
}
