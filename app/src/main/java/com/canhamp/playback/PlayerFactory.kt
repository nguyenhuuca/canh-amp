package com.canhamp.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.canhamp.playback.dsp.DspEngine
import com.canhamp.playback.dsp.SpectrumTapProcessor
import com.canhamp.playback.dsp.buildDspChain

/** Builds the single ExoPlayer with the DSP chain in its audio sink (ADR-0001, ADR-0002). */
@UnstableApi
object PlayerFactory {

    fun create(context: Context, engine: DspEngine, tap: SpectrumTapProcessor): ExoPlayer =
        ExoPlayer.Builder(context, DspRenderersFactory(context, engine, tap))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

    /** Float output and offload stay off so the processors always see 16-bit PCM. */
    private class DspRenderersFactory(
        context: Context,
        private val engine: DspEngine,
        private val tap: SpectrumTapProcessor,
    ) : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessorChain(DefaultAudioSink.DefaultAudioProcessorChain(*buildDspChain(engine, tap)))
            .build()
    }
}
