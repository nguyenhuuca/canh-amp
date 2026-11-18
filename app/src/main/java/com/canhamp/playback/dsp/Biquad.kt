package com.canhamp.playback.dsp

/** One channel's biquad, Transposed Direct Form II with double state. Not thread safe. */
class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var s1 = 0.0
    private var s2 = 0.0

    /** Replaces coefficients; filter state is kept so parameter moves do not click. */
    fun setCoeffs(c: BiquadCoeffs) {
        b0 = c.b0
        b1 = c.b1
        b2 = c.b2
        a1 = c.a1
        a2 = c.a2
    }

    fun reset() {
        s1 = 0.0
        s2 = 0.0
    }

    fun process(x: Float): Float {
        val xd = x.toDouble()
        val y = b0 * xd + s1
        s1 = b1 * xd - a1 * y + s2
        s2 = b2 * xd - a2 * y
        return y.toFloat()
    }
}
