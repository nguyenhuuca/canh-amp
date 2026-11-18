package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@UnstableApi
class LimiterProcessorTest {

    private val fs = 44100
    private val ceilingLsb = 0.98855 * 32768

    private fun sine(amp: Double, frames: Int = fs): ShortArray = ShortArray(frames * 2) {
        (amp * 32767 * sin(2 * PI * 1000.0 * (it / 2) / fs)).toInt().toShort()
    }

    private fun runChain(procs: List<AudioProcessor>, input: ShortArray): ShortArray {
        for (p in procs) {
            p.configure(AudioFormat(fs, 2, C.ENCODING_PCM_16BIT))
            p.flush()
        }
        val out = ShortArray(input.size)
        var pos = 0
        while (pos < input.size) {
            val n = minOf(2048, input.size - pos)
            var buf = ByteBuffer.allocateDirect(n * 2).order(ByteOrder.nativeOrder())
            for (i in 0 until n) buf.putShort(input[pos + i])
            buf.flip()
            for (p in procs) {
                p.queueInput(buf)
                buf = p.output
            }
            for (i in 0 until n) out[pos + i] = buf.short
            pos += n
        }
        return out
    }

    @Test
    fun allBandsBoostedStaysUnderCeiling() {
        val engine = DspEngine()
        engine.update { DspConfig(gainsDb = List(10) { 12 }, preampDb = 0) }
        val input = sine(1.0)
        val out = runChain(listOf(EqualizerProcessor(engine, false), LimiterProcessor()), input)
        var peak = 0
        for (s in out) {
            assertTrue("sample $s", s < 32767 && s > -32768)
            peak = maxOf(peak, abs(s.toInt()))
        }
        assertTrue("peak $peak", peak <= ceilingLsb)
        assertTrue("limiter should still pass signal, peak $peak", peak > 20000)
    }

    @Test
    fun transparentBelowCeiling() {
        val input = sine(0.5) // about -6 dBFS
        val out = runChain(listOf(LimiterProcessor()), input)
        assertArrayEquals(input, out)
    }

    @Test
    fun linkedChannelsKeepTheStereoImage() {
        val frames = fs / 4
        val input = ShortArray(frames * 2) { if (it % 2 == 0) 32767 else 8000 }
        val out = runChain(listOf(LimiterProcessor()), input)
        // once the envelope has settled both channels share one gain, so the ratio is preserved
        val i = (frames - 1) * 2
        assertTrue(out[i] <= ceilingLsb)
        val ratioIn = 8000.0 / 32767
        val ratioOut = out[i + 1].toDouble() / out[i]
        assertTrue("ratio $ratioOut", abs(ratioOut - ratioIn) < 0.01)
    }

    @Test
    fun releaseRestoresUnityAfterLoudPassage() {
        val loud = ShortArray(fs / 10 * 2) { 32767 }
        val quiet = ShortArray(fs * 2) { 1000 }
        val out = runChain(listOf(LimiterProcessor()), loud + quiet)
        assertTrue(out.last().toInt() == 1000)
    }

    @Test
    fun unsupportedEncodingIsInactive() {
        val p = LimiterProcessor()
        p.configure(AudioFormat(fs, 2, C.ENCODING_PCM_FLOAT))
        assertFalse(p.isActive)
    }

    @Test
    fun chainOrderIsEqualizerBalanceLimiter() {
        val chain = buildDspChain(DspEngine())
        assertTrue(chain[0] is EqualizerProcessor)
        assertTrue(chain[1] is BalanceProcessor)
        assertTrue(chain[2] is LimiterProcessor)
    }
}
