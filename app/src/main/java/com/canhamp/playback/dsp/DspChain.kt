package com.canhamp.playback.dsp

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi

/** The DSP processors in signal order: equalizer, balance, limiter. */
@UnstableApi
fun buildDspChain(engine: DspEngine): Array<AudioProcessor> =
    arrayOf(EqualizerProcessor(engine), BalanceProcessor(engine), LimiterProcessor())
