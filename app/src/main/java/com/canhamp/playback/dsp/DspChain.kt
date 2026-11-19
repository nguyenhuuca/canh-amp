package com.canhamp.playback.dsp

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ToInt16PcmAudioProcessor
import androidx.media3.common.util.UnstableApi

/**
 * The DSP processors in signal order: a 16-bit converter, equalizer, balance, limiter (ADR-0001),
 * then the spectrum [tap] last when given, so the visualizer sees what is heard (ADR-0002).
 * The converter lets 24/32-bit integer PCM (B42) reach the 16-bit DSP; it is inactive for 16-bit input.
 */
@UnstableApi
fun buildDspChain(engine: DspEngine, tap: SpectrumTapProcessor? = null): Array<AudioProcessor> {
    val chain = mutableListOf<AudioProcessor>(
        ToInt16PcmAudioProcessor(),
        EqualizerProcessor(engine),
        BalanceProcessor(engine),
        LimiterProcessor(),
    )
    if (tap != null) {
        engine.spectrumSampleRate = { tap.sampleRate }
        chain += tap
    }
    return chain.toTypedArray()
}
