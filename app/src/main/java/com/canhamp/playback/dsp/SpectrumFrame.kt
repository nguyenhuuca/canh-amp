package com.canhamp.playback.dsp

/** One visualizer frame: 16 band levels and peak-holds in 0..1, 256 mono samples in -1..1. */
class SpectrumFrame(
    val bands: FloatArray,
    val peaks: FloatArray,
    val wave: FloatArray,
) {
    companion object {
        const val BANDS = 16
        const val WAVE_SIZE = 256

        val EMPTY = SpectrumFrame(FloatArray(BANDS), FloatArray(BANDS), FloatArray(WAVE_SIZE))
    }
}
