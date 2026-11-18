package com.canhamp.playback.dsp

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiquadDesignTest {
    private val fs = 44100.0

    @Test
    fun peakingHitsGainAtCentreForEveryBand() {
        for (hz in DspConfig.BANDS_HZ) {
            if (hz >= 0.45 * fs) continue
            for (g in listOf(-12.0, -6.0, 6.0, 12.0)) {
                val c = BiquadDesign.peaking(fs, hz.toDouble(), DspConfig.Q, g)
                assertEquals("band $hz gain $g", g, BiquadDesign.magnitudeDb(c, fs, hz.toDouble()), 0.1)
            }
        }
    }

    @Test
    fun peakingIsFlatFarAway() {
        val c = BiquadDesign.peaking(fs, 1000.0, DspConfig.Q, 12.0)
        assertTrue(abs(BiquadDesign.magnitudeDb(c, fs, 20.0)) < 0.3)
        assertTrue(abs(BiquadDesign.magnitudeDb(c, fs, 20000.0)) < 0.5)
    }

    @Test
    fun zeroGainPeakingIsIdentity() {
        val c = BiquadDesign.peaking(fs, 1000.0, DspConfig.Q, 0.0)
        assertEquals(0.0, BiquadDesign.magnitudeDb(c, fs, 1000.0), 1e-9)
        assertEquals(0.0, BiquadDesign.magnitudeDb(c, fs, 5000.0), 1e-9)
    }

    @Test
    fun lowShelfReachesGainAtLowFrequencies() {
        val c = BiquadDesign.lowShelf(fs, 100.0, 1.0, 9.0)
        assertEquals(9.0, BiquadDesign.magnitudeDb(c, fs, 10.0), 0.2)
        assertEquals(4.5, BiquadDesign.magnitudeDb(c, fs, 100.0), 0.2) // half gain at f0
        assertEquals(0.0, BiquadDesign.magnitudeDb(c, fs, 10000.0), 0.2)
    }

    @Test
    fun highShelfReachesGainAtHighFrequencies() {
        val c = BiquadDesign.highShelf(fs, 10000.0, 1.0, 9.0)
        assertEquals(9.0, BiquadDesign.magnitudeDb(c, fs, 20000.0), 0.8)
        assertEquals(4.5, BiquadDesign.magnitudeDb(c, fs, 10000.0), 0.2)
        assertEquals(0.0, BiquadDesign.magnitudeDb(c, fs, 100.0), 0.2)
    }

    @Test
    fun biquadMatchesAnalyticResponse() {
        val c = BiquadDesign.peaking(fs, 1000.0, DspConfig.Q, 6.0)
        val bq = Biquad()
        bq.setCoeffs(c)
        var peak = 0.0
        for (n in 0 until 20000) {
            val y = bq.process(kotlin.math.sin(2 * Math.PI * 1000.0 * n / fs).toFloat())
            if (n > 10000) peak = maxOf(peak, abs(y.toDouble()))
        }
        assertEquals(6.0, 20 * kotlin.math.log10(peak), 0.1)
    }

    @Test
    fun resetClearsState() {
        val bq = Biquad()
        bq.setCoeffs(BiquadDesign.peaking(fs, 1000.0, DspConfig.Q, 6.0))
        val first = (0 until 50).map { bq.process(if (it == 0) 1f else 0f) }
        bq.reset()
        val second = (0 until 50).map { bq.process(if (it == 0) 1f else 0f) }
        assertEquals(first, second)
    }
}
