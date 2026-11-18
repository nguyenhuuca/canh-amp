package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@UnstableApi
class ResponseCurveTest {

    private fun onlyBand(index: Int, gain: Int) =
        DspConfig(gainsDb = List(10) { if (it == index) gain else 0 })

    @Test
    fun flatConfigIsZeroEverywhere() {
        for ((_, db) in ResponseCurve.curve(DspConfig.FLAT)) assertEquals(0.0, db, 1e-9)
        assertEquals(0.0, ResponseCurve.peakBoostDb(DspConfig.FLAT, 48000.0), 1e-9)
    }

    @Test
    fun curveIsLogSpacedFrom20HzTo20kHz() {
        val c = ResponseCurve.curve(DspConfig.FLAT)
        assertEquals(64, c.size)
        assertEquals(20.0, c.first().first, 1e-6)
        assertEquals(20000.0, c.last().first, 1e-3)
        val r0 = c[1].first / c[0].first
        for (i in 1 until c.size) assertEquals(r0, c[i].first / c[i - 1].first, 1e-9)
    }

    @Test
    fun singleBandPeaksAtItsCentre() {
        val cfg = onlyBand(4, 6) // 1 kHz
        assertEquals(6.0, ResponseCurve.magnitudeDb(cfg, 48000.0, 1000.0), 0.3)
        val peak = ResponseCurve.curve(cfg, points = 256).maxByOrNull { it.second }!!
        assertEquals(6.0, peak.second, 0.3)
        assertEquals(1000.0, peak.first, 120.0)
    }

    @Test
    fun preampAddsToTheCurveButNotToPeakBoost() {
        val cfg = DspConfig(preampDb = -4, gainsDb = List(10) { if (it == 4) 6 else 0 })
        assertEquals(2.0, ResponseCurve.magnitudeDb(cfg, 48000.0, 1000.0), 0.3)
        assertEquals(6.0, ResponseCurve.peakBoostDb(cfg, 48000.0), 0.3)
        assertEquals(0.0, ResponseCurve.peakBoostDb(DspConfig(gainsDb = List(10) { -6 }), 48000.0), 1e-9)
    }

    @Test
    fun bypassedCurveIsFlat() {
        val cfg = DspConfig(eqEnabled = false, gainsDb = List(10) { 12 }, preampDb = 12, bassDb = 12)
        for ((_, db) in ResponseCurve.curve(cfg)) assertEquals(0.0, db, 1e-9)
        assertEquals(0.0, ResponseCurve.peakBoostDb(cfg, 48000.0), 1e-9)
    }

    @Test
    fun skippedBandsAt32kHz() {
        // 14 kHz and 16 kHz are at or above 0.45 * 32000 = 14.4 kHz (14 kHz is below, 16 kHz is not)
        val withSkipped = onlyBand(9, 12)
        for (f in listOf(1000.0, 8000.0, 14000.0)) {
            assertEquals(0.0, ResponseCurve.magnitudeDb(withSkipped, 32000.0, f), 1e-9)
        }
        assertEquals(0.0, ResponseCurve.peakBoostDb(withSkipped, 32000.0), 1e-9)
        assertTrue(ResponseCurve.peakBoostDb(withSkipped, 48000.0) > 11.0)
        // treble shelf at 10 kHz is active at 44.1 kHz and skipped at 20 kHz
        assertEquals(0.0, ResponseCurve.peakBoostDb(DspConfig(trebleDb = 12), 20000.0), 1e-9)
        assertTrue(ResponseCurve.peakBoostDb(DspConfig(trebleDb = 12), 44100.0) > 10.0)
    }

    @Test
    fun peakBoostMatchesMeasuredProcessorResponse() {
        val fs = 48000
        val cfg = DspConfig(
            gainsDb = listOf(3, 6, 9, 4, -3, 2, 7, 5, 0, -2),
            bassDb = 4,
            trebleDb = 3,
        )
        val engine = DspEngine()
        engine.update { cfg }
        var measuredMax = Double.NEGATIVE_INFINITY
        for (hz in listOf(60.0, 100.0, 170.0, 310.0, 600.0, 3000.0, 6000.0, 12000.0)) {
            val g = measureDb(engine, hz, fs)
            measuredMax = maxOf(measuredMax, g)
            assertEquals("at $hz", ResponseCurve.magnitudeDb(cfg, fs.toDouble(), hz), g, 0.5)
        }
        val peak = ResponseCurve.peakBoostDb(cfg, fs.toDouble())
        assertTrue("peak $peak measured $measuredMax", peak >= measuredMax - 0.5)
        assertEquals(peak, measuredMax, 3.0)
    }

    private fun measureDb(engine: DspEngine, hz: Double, fs: Int): Double {
        val proc = EqualizerProcessor(engine, autoHeadroom = false)
        proc.configure(AudioFormat(fs, 1, C.ENCODING_PCM_16BIT))
        proc.flush()
        val frames = fs
        val buf = ByteBuffer.allocateDirect(frames * 2).order(ByteOrder.nativeOrder())
        val input = ShortArray(frames) { (0.1 * 32767 * sin(2 * PI * hz * it / fs)).toInt().toShort() }
        for (s in input) buf.putShort(s)
        buf.flip()
        proc.queueInput(buf)
        val o = proc.output
        var inSum = 0.0
        var outSum = 0.0
        for (i in 0 until frames) {
            val y = o.short.toDouble()
            if (i >= 8192) {
                outSum += y * y
                inSum += input[i].toDouble() * input[i]
            }
        }
        return 20 * log10(sqrt(outSum) / sqrt(inSum))
    }

    @Test
    fun magnitudeIsFiniteAcrossTheGrid() {
        val cfg = DspConfig(gainsDb = List(10) { 12 }, preampDb = 12, bassDb = 12, trebleDb = 12)
        for ((_, db) in ResponseCurve.curve(cfg, 44100.0, 64)) assertTrue(abs(db) < 200)
    }
}
