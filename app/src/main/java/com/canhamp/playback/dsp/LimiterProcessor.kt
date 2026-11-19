package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max

/**
 * Peak limiter (spec B50): ceiling -0.1 dBFS, 0.5 ms attack, 80 ms release, linked across channels.
 *
 * There is no lookahead, so the envelope alone can overshoot during the attack; the output is also
 * hard-clamped to the ceiling. While the envelope stays under the ceiling the gain is exactly 1 and
 * samples whose magnitude is at most the ceiling (32392 in 16-bit) pass bit for bit; louder samples are
 * clamped to it, which keeps the "no sample above the ceiling" guarantee.
 */
@UnstableApi
class LimiterProcessor : BaseAudioProcessor() {

    private var env = 0f
    private var attackCoeff = 0f
    private var releaseCoeff = 0f

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..2) {
            return AudioFormat.NOT_SET
        }
        val fs = inputAudioFormat.sampleRate.toDouble()
        attackCoeff = exp(-1.0 / (ATTACK_S * fs)).toFloat()
        releaseCoeff = exp(-1.0 / (RELEASE_S * fs)).toFloat()
        return inputAudioFormat
    }

    override fun onFlush() {
        env = 0f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val channels = inputAudioFormat.channelCount
        val frames = inputBuffer.remaining() / inputAudioFormat.bytesPerFrame
        if (frames == 0) return
        val out = replaceOutputBuffer(frames * inputAudioFormat.bytesPerFrame)
        val src = inputBuffer.order(ByteOrder.nativeOrder())
        val dst = out.order(ByteOrder.nativeOrder())
        var e = env
        for (i in 0 until frames) {
            val l = PcmConvert.toFloat(src.short)
            val r = if (channels == 2) PcmConvert.toFloat(src.short) else l
            val peak = max(abs(l), abs(r))
            val coeff = if (peak > e) attackCoeff else releaseCoeff
            e = peak + (e - peak) * coeff
            val g = if (e > CEILING) CEILING / e else 1f
            dst.putShort(limit(l, g))
            if (channels == 2) dst.putShort(limit(r, g))
        }
        env = e
        out.flip()
    }

    private fun limit(x: Float, g: Float): Short {
        val y = if (g == 1f) x else (x * g).coerceIn(-CEILING, CEILING)
        return PcmConvert.toShort(y).toInt().coerceIn(-MAX_OUT, MAX_OUT).toShort()
    }

    private companion object {
        const val CEILING = 0.98855f // -0.1 dBFS
        const val ATTACK_S = 0.0005
        const val RELEASE_S = 0.080
        val MAX_OUT = floor(CEILING * 32768f).toInt() // never above the ceiling after rounding
    }
}
