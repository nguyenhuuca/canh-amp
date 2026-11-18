package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

/**
 * Stereo balance (spec B30): left x min(1, 1 - b), right x min(1, 1 + b), with a centre detent below 0.05.
 * The gain moves linearly across each buffer so changes do not zipper. Mono passes through.
 */
@UnstableApi
class BalanceProcessor(private val engine: DspEngine) : BaseAudioProcessor() {

    private var lastLeft = 1f
    private var lastRight = 1f

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..2) {
            return AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        val (l, r) = gains(engine.current().balance)
        lastLeft = l
        lastRight = r
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytesPerFrame = inputAudioFormat.bytesPerFrame
        val frames = inputBuffer.remaining() / bytesPerFrame
        if (frames == 0) return
        val bytes = frames * bytesPerFrame
        val out = replaceOutputBuffer(bytes)
        val (targetL, targetR) = gains(engine.current().balance)
        val src = inputBuffer.order(ByteOrder.nativeOrder())
        val dst = out.order(ByteOrder.nativeOrder())

        val unity = lastLeft == 1f && lastRight == 1f && targetL == 1f && targetR == 1f
        if (inputAudioFormat.channelCount == 1 || unity) {
            val oldLimit = src.limit()
            src.limit(src.position() + bytes)
            dst.put(src)
            src.limit(oldLimit)
        } else {
            for (i in 0 until frames) {
                val t = (i + 1).toFloat() / frames
                val gl = lastLeft + (targetL - lastLeft) * t
                val gr = lastRight + (targetR - lastRight) * t
                dst.putShort(scale(src.short, gl))
                dst.putShort(scale(src.short, gr))
            }
        }
        lastLeft = targetL
        lastRight = targetR
        out.flip()
    }

    private fun scale(s: Short, g: Float): Short =
        if (g == 1f) s else Math.round(s * g).coerceIn(-32768, 32767).toShort()

    private fun gains(balance: Float): Pair<Float, Float> {
        val b = if (abs(balance) < DETENT) 0f else balance
        return min(1f, 1f - b) to min(1f, 1f + b)
    }

    private companion object {
        const val DETENT = 0.05f
    }
}
