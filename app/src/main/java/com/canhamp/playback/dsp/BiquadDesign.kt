package com.canhamp.playback.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Normalised biquad coefficients (a0 = 1). */
data class BiquadCoeffs(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
) {
    companion object {
        val IDENTITY = BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0)
    }
}

/** RBJ audio-EQ-cookbook designs plus the analytic magnitude response. */
object BiquadDesign {

    fun peaking(fs: Double, f0: Double, q: Double, gainDb: Double): BiquadCoeffs {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * f0 / fs
        val cosW = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        return normalise(
            1.0 + alpha * a, -2.0 * cosW, 1.0 - alpha * a,
            1.0 + alpha / a, -2.0 * cosW, 1.0 - alpha / a,
        )
    }

    fun lowShelf(fs: Double, f0: Double, s: Double = 1.0, gainDb: Double): BiquadCoeffs {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * f0 / fs
        val cosW = cos(w0)
        val k = 2.0 * sqrt(a) * shelfAlpha(w0, a, s)
        return normalise(
            a * ((a + 1) - (a - 1) * cosW + k),
            2 * a * ((a - 1) - (a + 1) * cosW),
            a * ((a + 1) - (a - 1) * cosW - k),
            (a + 1) + (a - 1) * cosW + k,
            -2 * ((a - 1) + (a + 1) * cosW),
            (a + 1) + (a - 1) * cosW - k,
        )
    }

    fun highShelf(fs: Double, f0: Double, s: Double = 1.0, gainDb: Double): BiquadCoeffs {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * f0 / fs
        val cosW = cos(w0)
        val k = 2.0 * sqrt(a) * shelfAlpha(w0, a, s)
        return normalise(
            a * ((a + 1) + (a - 1) * cosW + k),
            -2 * a * ((a - 1) + (a + 1) * cosW),
            a * ((a + 1) + (a - 1) * cosW - k),
            (a + 1) - (a - 1) * cosW + k,
            2 * ((a - 1) - (a + 1) * cosW),
            (a + 1) - (a - 1) * cosW - k,
        )
    }

    /** Magnitude response in dB of [c] at frequency [f] for sample rate [fs]. */
    fun magnitudeDb(c: BiquadCoeffs, fs: Double, f: Double): Double {
        val w = 2.0 * PI * f / fs
        val numRe = c.b0 + c.b1 * cos(w) + c.b2 * cos(2 * w)
        val numIm = -(c.b1 * sin(w) + c.b2 * sin(2 * w))
        val denRe = 1.0 + c.a1 * cos(w) + c.a2 * cos(2 * w)
        val denIm = -(c.a1 * sin(w) + c.a2 * sin(2 * w))
        val mag = hypot(numRe, numIm) / hypot(denRe, denIm)
        return 20.0 * log10(maxOf(mag, 1e-12))
    }

    private fun shelfAlpha(w0: Double, a: Double, s: Double): Double =
        sin(w0) / 2.0 * sqrt((a + 1.0 / a) * (1.0 / s - 1.0) + 2.0)

    private fun normalise(b0: Double, b1: Double, b2: Double, a0: Double, a1: Double, a2: Double) =
        BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
}
