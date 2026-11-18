package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Last stage of the DSP chain (ADR-0002): passes audio through bit-exact and copies a mono
 * downmix into [sink] for the spectrum analyser.
 */
@UnstableApi
class SpectrumTapProcessor(private val sink: RingBuffer) : BaseAudioProcessor() {

    /** Sample rate of the audio currently flowing, 0 until configured. */
    @Volatile
    var sampleRate: Int = 0
        private set

    private val scratch = FloatArray(CHUNK)

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..2) {
            return AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytesPerFrame = inputAudioFormat.bytesPerFrame
        val frames = inputBuffer.remaining() / bytesPerFrame
        if (frames == 0) return
        val bytes = frames * bytesPerFrame
        val channels = inputAudioFormat.channelCount

        val view = inputBuffer.duplicate().order(ByteOrder.nativeOrder())
        view.limit(view.position() + bytes)
        var done = 0
        while (done < frames) {
            val n = minOf(CHUNK, frames - done)
            for (i in 0 until n) {
                var sum = PcmConvert.readFloat(view)
                if (channels == 2) sum = (sum + PcmConvert.readFloat(view)) * 0.5f
                scratch[i] = sum
            }
            sink.write(scratch, 0, n)
            done += n
        }

        val out = replaceOutputBuffer(bytes)
        val oldLimit = inputBuffer.limit()
        inputBuffer.limit(inputBuffer.position() + bytes)
        out.put(inputBuffer)
        inputBuffer.limit(oldLimit)
        out.flip()
    }

    private companion object {
        const val CHUNK = 1024
    }
}
