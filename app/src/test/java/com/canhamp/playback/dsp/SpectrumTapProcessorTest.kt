package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@UnstableApi
class SpectrumTapProcessorTest {

    private fun process(proc: SpectrumTapProcessor, input: ShortArray): ShortArray {
        val buf = ByteBuffer.allocateDirect(input.size * 2).order(ByteOrder.nativeOrder())
        for (s in input) buf.putShort(s)
        buf.flip()
        proc.flush()
        proc.queueInput(buf)
        val out = proc.output
        val result = ShortArray(out.remaining() / 2)
        out.order(ByteOrder.nativeOrder()).asShortBuffer().get(result)
        return result
    }

    @Test
    fun passThroughIsBitExactAndRingGetsStereoDownmix() {
        val ring = RingBuffer(64)
        val proc = SpectrumTapProcessor(ring)
        proc.configure(AudioFormat(44100, 2, C.ENCODING_PCM_16BIT))
        val input = shortArrayOf(1000, 3000, -2000, 2000, 32767, -32768, 0, 0)
        val out = process(proc, input)
        assertArrayEquals(input, out)

        val dest = FloatArray(4)
        assertTrue(ring.readLatest(dest, 4))
        assertEquals(2000f / 32768f, dest[0], 1e-6f)
        assertEquals(0f, dest[1], 1e-6f)
        assertEquals(-0.5f / 32768f, dest[2], 1e-6f)
        assertEquals(0f, dest[3], 1e-6f)
        assertEquals(44100, proc.sampleRate)
    }

    @Test
    fun monoSamplesGoStraightToRing() {
        val ring = RingBuffer(64)
        val proc = SpectrumTapProcessor(ring)
        proc.configure(AudioFormat(48000, 1, C.ENCODING_PCM_16BIT))
        val input = shortArrayOf(16384, -16384, 8192)
        assertArrayEquals(input, process(proc, input))
        val dest = FloatArray(3)
        assertTrue(ring.readLatest(dest, 3))
        assertArrayEquals(floatArrayOf(0.5f, -0.5f, 0.25f), dest, 1e-6f)
        assertEquals(48000, proc.sampleRate)
    }

    @Test
    fun nonSixteenBitIsNotSet() {
        val proc = SpectrumTapProcessor(RingBuffer(64))
        val fmt = proc.configure(AudioFormat(44100, 2, C.ENCODING_PCM_FLOAT))
        assertEquals(AudioFormat.NOT_SET, fmt)
        assertFalse(proc.isActive)
    }
}
