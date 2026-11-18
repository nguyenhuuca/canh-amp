package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.pow

/**
 * Preamp, 10 peaking bands, bass low-shelf and treble high-shelf on 16-bit PCM (spec B40-B44).
 *
 * [autoHeadroom] subtracts the largest boost from the preamp (B49). For now the boost is
 * estimated as the largest positive band, bass or treble gain; the analytic peak replaces it later.
 */
@UnstableApi
class EqualizerProcessor(
    private val engine: DspEngine,
    private val autoHeadroom: Boolean = true,
) : BaseAudioProcessor() {

    private var filters: Array<Array<Biquad>> = emptyArray() // [channel][slot]
    private var enabled = BooleanArray(SLOTS)
    private var preGain = 1.0
    private var appliedVersion = -1L
    private var appliedRate = -1

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.channelCount !in 1..2 ||
            inputAudioFormat.sampleRate !in 8000..96000
        ) {
            engine.reportStatus(DspStatus.UNSUPPORTED_FORMAT)
            return AudioFormat.NOT_SET
        }
        filters = Array(inputAudioFormat.channelCount) { Array(SLOTS) { Biquad() } }
        enabled = BooleanArray(SLOTS)
        appliedVersion = -1L
        appliedRate = -1
        engine.reportStatus(DspStatus.ACTIVE)
        return inputAudioFormat
    }

    override fun onFlush() {
        for (ch in filters) for (f in ch) f.reset()
    }

    override fun onReset() {
        filters = emptyArray()
        enabled = BooleanArray(SLOTS)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytesPerFrame = inputAudioFormat.bytesPerFrame
        val frames = inputBuffer.remaining() / bytesPerFrame
        if (frames == 0) return
        val bytes = frames * bytesPerFrame
        val out = replaceOutputBuffer(bytes)
        val snap = engine.snapshot()
        val cfg = snap.config

        if (!cfg.eqEnabled) {
            engine.reportStatus(DspStatus.BYPASSED)
            val oldLimit = inputBuffer.limit()
            inputBuffer.limit(inputBuffer.position() + bytes)
            out.put(inputBuffer)
            inputBuffer.limit(oldLimit)
            out.flip()
            return
        }
        engine.reportStatus(DspStatus.ACTIVE)
        if (snap.version != appliedVersion || inputAudioFormat.sampleRate != appliedRate) {
            applyConfig(cfg, inputAudioFormat.sampleRate)
            appliedVersion = snap.version
            appliedRate = inputAudioFormat.sampleRate
        }

        val channels = inputAudioFormat.channelCount
        val order = ByteOrder.nativeOrder()
        val src = inputBuffer.order(order)
        val dst = out.order(order)
        val gain = preGain.toFloat()
        for (i in 0 until frames) {
            for (c in 0 until channels) {
                var x = PcmConvert.readFloat(src) * gain
                val chain = filters[c]
                for (s in 0 until SLOTS) if (enabled[s]) x = chain[s].process(x)
                PcmConvert.writeFloat(dst, x)
            }
        }
        out.flip()
    }

    private fun applyConfig(cfg: DspConfig, rate: Int) {
        val fs = rate.toDouble()
        val limit = 0.45 * fs
        val want = BooleanArray(SLOTS)
        val coeffs = arrayOfNulls<BiquadCoeffs>(SLOTS)
        var maxBoost = 0
        for (band in 0 until DspConfig.BAND_COUNT) {
            val hz = DspConfig.BANDS_HZ[band]
            val g = cfg.gainsDb[band]
            if (g != 0 && hz < limit) {
                want[band] = true
                coeffs[band] = BiquadDesign.peaking(fs, hz.toDouble(), DspConfig.Q, g.toDouble())
                maxBoost = max(maxBoost, g)
            }
        }
        if (cfg.bassDb > 0) {
            want[LOW] = true
            coeffs[LOW] = BiquadDesign.lowShelf(fs, DspConfig.BASS_HZ.toDouble(), 1.0, cfg.bassDb.toDouble())
            maxBoost = max(maxBoost, cfg.bassDb)
        }
        if (cfg.trebleDb > 0 && DspConfig.TREBLE_HZ < limit) {
            want[HIGH] = true
            coeffs[HIGH] = BiquadDesign.highShelf(fs, DspConfig.TREBLE_HZ.toDouble(), 1.0, cfg.trebleDb.toDouble())
            maxBoost = max(maxBoost, cfg.trebleDb)
        }
        for (chain in filters) {
            for (s in 0 until SLOTS) {
                val c = coeffs[s]
                if (want[s] && c != null) {
                    chain[s].setCoeffs(c) // state kept: no click on slider moves
                } else if (enabled[s]) {
                    chain[s].reset()
                    chain[s].setCoeffs(BiquadCoeffs.IDENTITY)
                }
            }
        }
        for (s in 0 until SLOTS) enabled[s] = want[s]
        val headroom = if (autoHeadroom) maxBoost else 0
        preGain = 10.0.pow((cfg.preampDb - headroom) / 20.0)
    }

    private companion object {
        const val LOW = DspConfig.BAND_COUNT
        const val HIGH = DspConfig.BAND_COUNT + 1
        const val SLOTS = DspConfig.BAND_COUNT + 2
    }
}
