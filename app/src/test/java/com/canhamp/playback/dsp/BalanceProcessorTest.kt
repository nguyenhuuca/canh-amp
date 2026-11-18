package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@UnstableApi
class BalanceProcessorTest {

    private fun stereo(frames: Int) = ShortArray(frames * 2) { i ->
        (0.5 * 32767 * sin(2 * PI * (if (i % 2 == 0) 440.0 else 660.0) * (i / 2) / 44100)).toInt().toShort()
    }

    private fun process(balance: Float, input: ShortArray, channels: Int = 2): ShortArray {
        val engine = DspEngine()
        engine.update { DspConfig(balance = balance) }
        val proc = BalanceProcessor(engine)
        proc.configure(AudioFormat(44100, channels, C.ENCODING_PCM_16BIT))
        proc.flush()
        val buf = ByteBuffer.allocateDirect(input.size * 2).order(ByteOrder.nativeOrder())
        for (s in input) buf.putShort(s)
        buf.flip()
        proc.queueInput(buf)
        val o = proc.output
        return ShortArray(input.size) { o.short }
    }

    @Test
    fun fullLeftSilencesRight() {
        val input = stereo(2000)
        val out = process(-1f, input)
        for (i in input.indices) {
            if (i % 2 == 0) assertEquals(input[i], out[i]) else assertEquals(0, out[i].toInt())
        }
    }

    @Test
    fun fullRightSilencesLeft() {
        val input = stereo(2000)
        val out = process(1f, input)
        for (i in input.indices) {
            if (i % 2 == 1) assertEquals(input[i], out[i]) else assertEquals(0, out[i].toInt())
        }
    }

    @Test
    fun centreIsBitExact() {
        val input = stereo(2000)
        assertArrayEquals(input, process(0f, input))
    }

    @Test
    fun detentBelowFiveHundredthsIsBitExact() {
        val input = stereo(2000)
        assertArrayEquals(input, process(0.03f, input))
        assertArrayEquals(input, process(-0.04f, input))
    }

    @Test
    fun halfBalanceAttenuatesOneSide() {
        val input = ShortArray(2000) { 10000 }
        val out = process(0.5f, input)
        assertEquals(5000, out[out.size - 2].toInt()) // left x 0.5
        assertEquals(10000, out[out.size - 1].toInt())
    }

    @Test
    fun monoPassesThrough() {
        val input = ShortArray(1000) { (it * 7 % 20000).toShort() }
        assertArrayEquals(input, process(-1f, input, channels = 1))
    }

    @Test
    fun balanceChangeIsRampedAcrossTheBuffer() {
        val engine = DspEngine()
        val proc = BalanceProcessor(engine)
        proc.configure(AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        proc.flush()
        engine.update { DspConfig(balance = 1f) }
        val frames = 1000
        val buf = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.nativeOrder())
        repeat(frames * 2) { buf.putShort(10000) }
        buf.flip()
        proc.queueInput(buf)
        val o = proc.output
        val left = ShortArray(frames) { val l = o.short; o.short; l }
        assertEquals(10000.0, left.first().toDouble(), 20.0) // starts near unity
        assertEquals(0, left.last().toInt())
        for (i in 1 until frames) assertEquals(true, left[i] <= left[i - 1])
    }

    @Test
    fun unsupportedEncodingIsInactive() {
        val proc = BalanceProcessor(DspEngine())
        proc.configure(AudioFormat(44100, 2, C.ENCODING_PCM_FLOAT))
        assertFalse(proc.isActive)
    }
}
