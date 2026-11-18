package com.canhamp.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign

/**
 * Preamp, 10 peaking bands, bass low-shelf and treble high-shelf on 16-bit PCM (spec B40-B44).
 *
 * [autoHeadroom] subtracts [ResponseCurve.peakBoostDb] from the preamp (B49); it is recomputed
 * whenever the coefficients are.
 *
 * Gain smoothing: switching a filter on from zero state, or changing its gain in one jump, produces a
 * transient. Instead, each slot's gain moves towards its target by at most [STEP_DB] per block of
 * about 2 ms, and the slot's coefficients are redesigned for every block. The pre-gain moves at the
 * same rate, interpolated linearly inside the block. A 12 dB change settles in roughly 25 ms. The
 * biquads are Direct Form I (see [Biquad]) because coefficient changes in Transposed Form II still
 * click on low-frequency material. The first config after configure/flush is applied directly, since the stream starts from silence.
 */
@UnstableApi
class EqualizerProcessor(
    private val engine: DspEngine,
    private val autoHeadroom: Boolean = true,
) : BaseAudioProcessor() {

    private var filters: Array<Array<Biquad>> = emptyArray() // [channel][slot]
    private val current = DoubleArray(SLOTS) // gain in dB currently applied per slot
    private val target = DoubleArray(SLOTS)
    private var preCurrent = 1.0
    private var preTarget = 1.0
    private var settling = false
    private var snapNext = true
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
        clearState()
        engine.reportStatus(DspStatus.ACTIVE)
        return inputAudioFormat
    }

    override fun onFlush() {
        clearState()
    }

    override fun onReset() {
        filters = emptyArray()
        clearState()
    }

    private fun clearState() {
        for (ch in filters) for (f in ch) {
            f.reset()
            f.setCoeffs(BiquadCoeffs.IDENTITY)
        }
        current.fill(0.0)
        target.fill(0.0)
        preCurrent = 1.0
        preTarget = 1.0
        settling = false
        snapNext = true
        appliedVersion = -1L
        appliedRate = -1
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
            enterBypass()
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
        val blockFrames = max(MIN_BLOCK, inputAudioFormat.sampleRate / 500)
        var done = 0
        while (done < frames) {
            val n = minOf(blockFrames, frames - done)
            val gainFrom = preCurrent
            if (settling) stepTowardTarget()
            val gainTo = preCurrent
            for (i in 0 until n) {
                val gain = (gainFrom + (gainTo - gainFrom) * (i + 1) / n).toFloat()
                for (c in 0 until channels) {
                    var x = PcmConvert.readFloat(src) * gain
                    val chain = filters[c]
                    // every slot runs (identity when flat) so its history is current when a gain ramps in
                    for (s in 0 until SLOTS) x = chain[s].process(x)
                    PcmConvert.writeFloat(dst, x)
                }
            }
            done += n
        }
        out.flip()
    }

    /** Bypass copies samples untouched; the next active block ramps up from a flat, empty chain. */
    private fun enterBypass() {
        for (ch in filters) for (f in ch) {
            f.reset()
            f.setCoeffs(BiquadCoeffs.IDENTITY)
        }
        current.fill(0.0)
        target.fill(0.0)
        preCurrent = 1.0
        preTarget = 1.0
        settling = false
        snapNext = false
        appliedVersion = -1L
    }

    private fun applyConfig(cfg: DspConfig, rate: Int) {
        val fs = rate.toDouble()
        val limit = 0.45 * fs
        target.fill(0.0)
        for (band in 0 until DspConfig.BAND_COUNT) {
            if (DspConfig.BANDS_HZ[band] < limit) target[band] = cfg.gainsDb[band].toDouble()
        }
        target[LOW] = cfg.bassDb.toDouble()
        if (DspConfig.TREBLE_HZ < limit) target[HIGH] = cfg.trebleDb.toDouble()
        val headroom = if (autoHeadroom) ResponseCurve.peakBoostDb(cfg, fs) else 0.0
        preTarget = 10.0.pow((cfg.preampDb - headroom) / 20.0)
        if (snapNext) {
            for (s in 0 until SLOTS) setSlotGain(s, target[s])
            preCurrent = preTarget
            settling = false
            snapNext = false
        } else {
            settling = true
        }
    }

    /** One smoothing block: every slot and the pre-gain move at most one step towards their targets. */
    private fun stepTowardTarget() {
        var pending = false
        for (s in 0 until SLOTS) {
            val diff = target[s] - current[s]
            if (diff == 0.0) continue
            setSlotGain(s, if (abs(diff) <= STEP_DB) target[s] else current[s] + STEP_DB * sign(diff))
            if (current[s] != target[s]) pending = true
        }
        preCurrent = preTarget.coerceIn(preCurrent / STEP_RATIO, preCurrent * STEP_RATIO)
        if (preCurrent != preTarget) pending = true
        settling = pending
    }

    private fun setSlotGain(slot: Int, gainDb: Double) {
        val fs = inputAudioFormat.sampleRate.toDouble()
        current[slot] = gainDb
        val coeffs = when {
            gainDb == 0.0 -> BiquadCoeffs.IDENTITY
            slot == LOW -> BiquadDesign.lowShelf(fs, DspConfig.BASS_HZ.toDouble(), 1.0, gainDb)
            slot == HIGH -> BiquadDesign.highShelf(fs, DspConfig.TREBLE_HZ.toDouble(), 1.0, gainDb)
            else -> BiquadDesign.peaking(fs, DspConfig.BANDS_HZ[slot].toDouble(), DspConfig.Q, gainDb)
        }
        for (chain in filters) {
            chain[slot].setCoeffs(coeffs) // history kept while ramping: no click
        }
    }

    private companion object {
        const val LOW = DspConfig.BAND_COUNT
        const val HIGH = DspConfig.BAND_COUNT + 1
        const val SLOTS = DspConfig.BAND_COUNT + 2
        const val STEP_DB = 1.0
        val STEP_RATIO = 10.0.pow(STEP_DB / 20.0)
        const val MIN_BLOCK = 16
    }
}
