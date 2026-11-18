package com.canhamp.playback.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * In-place radix-2 FFT for real input of size [n] (power of two). Not thread-safe:
 * it owns scratch arrays, so give each analyser its own instance.
 */
class Fft(val n: Int = 1024) {
    private val cosT = FloatArray(n / 2)
    private val sinT = FloatArray(n / 2)
    private val rev = IntArray(n)
    private val re = FloatArray(n)
    private val im = FloatArray(n)

    init {
        require(n >= 2 && n and (n - 1) == 0) { "n must be a power of two" }
        for (i in 0 until n / 2) {
            cosT[i] = cos(2.0 * PI * i / n).toFloat()
            sinT[i] = (-sin(2.0 * PI * i / n)).toFloat()
        }
        val bits = Integer.numberOfTrailingZeros(n)
        for (i in 0 until n) rev[i] = Integer.reverse(i) ushr (32 - bits)
    }

    /** Writes |X[k]| for k in 0 until n/2 into [out]; [input] is left untouched. Unnormalised. */
    fun magnitudes(input: FloatArray, out: FloatArray) {
        require(input.size >= n && out.size >= n / 2) { "buffers too small" }
        for (i in 0 until n) {
            re[rev[i]] = input[i]
            im[rev[i]] = 0f
        }
        var size = 2
        while (size <= n) {
            val half = size / 2
            val step = n / size
            var start = 0
            while (start < n) {
                var k = 0
                for (j in start until start + half) {
                    val wr = cosT[k]
                    val wi = sinT[k]
                    val tr = re[j + half] * wr - im[j + half] * wi
                    val ti = re[j + half] * wi + im[j + half] * wr
                    re[j + half] = re[j] - tr
                    im[j + half] = im[j] - ti
                    re[j] += tr
                    im[j] += ti
                    k += step
                }
                start += size
            }
            size *= 2
        }
        for (k in 0 until n / 2) out[k] = sqrt(re[k] * re[k] + im[k] * im[k])
    }

    companion object {
        /** Periodic Hann window of length [n]; its coherent gain is 0.5. */
        fun hann(n: Int): FloatArray =
            FloatArray(n) { (0.5 - 0.5 * cos(2.0 * PI * it / n)).toFloat() }
    }
}
