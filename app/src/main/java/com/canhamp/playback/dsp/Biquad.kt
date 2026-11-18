package com.canhamp.playback.dsp

/**
 * One channel's biquad in Direct Form I with double state. Not thread safe.
 *
 * Direct Form I keeps input and output history rather than filter-internal sums, so changing the
 * coefficients between samples (gain smoothing) disturbs the output far less than Transposed Form II
 * does for low-frequency material.
 */
class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    /** Replaces coefficients; history is kept so parameter moves do not click. */
    fun setCoeffs(c: BiquadCoeffs) {
        b0 = c.b0
        b1 = c.b1
        b2 = c.b2
        a1 = c.a1
        a2 = c.a2
    }

    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }

    fun process(x: Float): Float {
        val xd = x.toDouble()
        val y = b0 * xd + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = xd
        y2 = y1
        y1 = y
        return y.toFloat()
    }
}
