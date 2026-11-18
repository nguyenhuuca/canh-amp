package com.canhamp.playback.dsp

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi

/**
 * The DSP processors in signal order: equalizer, balance, limiter (ADR-0001), then the
 * spectrum [tap] last when given, so the visualizer sees what is heard (ADR-0002).
 */
@UnstableApi
fun buildDspChain(engine: DspEngine, tap: SpectrumTapProcessor? = null): Array<AudioProcessor> {
    val chain = mutableListOf<AudioProcessor>(
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
