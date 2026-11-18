package com.canhamp.playback.dsp

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FftTest {
    private val n = 1024

    @Test
    fun sineOnBinPeaksAtThatBinWithHalfNMagnitude() {
        val k = 37
        val x = FloatArray(n) { sin(2 * PI * k * it / n).toFloat() }
        val out = FloatArray(n / 2)
        Fft(n).magnitudes(x, out)
        val peak = out.indices.maxByOrNull { out[it] }
        assertEquals(k, peak)
        assertEquals(n / 2f, out[k], 0.5f)
        assertTrue(out[k + 5] < 1f)
    }

    @Test
    fun dcLandsInBinZero() {
        val out = FloatArray(n / 2)
        Fft(n).magnitudes(FloatArray(n) { 0.5f }, out)
        assertEquals(n * 0.5f, out[0], 0.01f)
        assertEquals(0f, out[1], 0.01f)
    }

    @Test
    fun hannWindowedSineHasQuarterNMagnitude() {
        val k = 100
        val w = Fft.hann(n)
        val x = FloatArray(n) { (sin(2 * PI * k * it / n)).toFloat() * w[it] }
        val out = FloatArray(n / 2)
        Fft(n).magnitudes(x, out)
        assertEquals(n / 4f, out[k], 0.5f)
    }

    @Test
    fun hannEndsAtZeroAndPeaksAtOne() {
        val w = Fft.hann(n)
        assertEquals(0f, w[0], 1e-6f)
        assertEquals(1f, w[n / 2], 1e-6f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPowerOfTwo() {
        Fft(1000)
    }
}
