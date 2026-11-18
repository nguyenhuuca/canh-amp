package com.canhamp.playback.dsp

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 1024-point Hann FFT mapped to 16 log bands (spec B51, B26). Cold: work happens only while
 * [frames] is collected, at most ~30 times per second. [clock] returns milliseconds.
 */
class SpectrumAnalyzer(
    private val ring: RingBuffer,
    private val sampleRateProvider: () -> Int,
    private val clock: () -> Long,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val frames: Flow<SpectrumFrame> = flow {
        val fft = Fft(N)
        val window = Fft.hann(N)
        val samples = FloatArray(N)
        val windowed = FloatArray(N)
        val mags = FloatArray(N / 2)
        val level = FloatArray(BANDS)
        val peaks = FloatArray(BANDS)
        val target = FloatArray(BANDS)
        var lastTime = clock()
        var lastWrite = ring.writeIndex
        var lastAdvance = lastTime
        while (true) {
            delay(FRAME_MS)
            val now = clock()
            val dt = max(0L, now - lastTime) / 1000f
            lastTime = now
            val w = ring.writeIndex
            if (w != lastWrite) {
                lastWrite = w
                lastAdvance = now
            }
            val paused = now - lastAdvance > PAUSE_MS
            val have = !paused && ring.readLatest(samples, N)
            if (have) {
                for (i in 0 until N) windowed[i] = samples[i] * window[i]
                fft.magnitudes(windowed, mags)
                val rate = sampleRateProvider().let { if (it > 0) it else DEFAULT_RATE }
                val edges = bandEdges(rate, N)
                for (b in 0 until BANDS) {
                    var m = 0f
                    for (k in edges[b] until edges[b + 1]) m = max(m, mags[k])
                    target[b] = normalise(m / (N / 4f)) // Hann: full-scale sine peaks at N/4
                }
            } else {
                target.fill(0f)
            }
            val fall = (if (paused) FAST_DECAY else DECAY) * dt
            val peakFall = PEAK_DECAY * dt
            for (b in 0 until BANDS) {
                level[b] = max(target[b], level[b] - fall)
                peaks[b] = max(level[b], peaks[b] - peakFall)
            }
            val wave = FloatArray(SpectrumFrame.WAVE_SIZE)
            if (have) System.arraycopy(samples, N - wave.size, wave, 0, wave.size)
            emit(SpectrumFrame(level.copyOf(), peaks.copyOf(), wave))
        }
    }.flowOn(dispatcher)

    companion object {
        const val N = 1024
        const val BANDS = SpectrumFrame.BANDS
        const val FRAME_MS = 33L
        const val PAUSE_MS = 100L
        const val MIN_HZ = 50.0
        const val MAX_HZ = 16000.0
        private const val DEFAULT_RATE = 44100
        private const val RANGE_DB = 60f
        private const val DECAY = 18f / RANGE_DB // normalised units per second
        private const val FAST_DECAY = 120f / RANGE_DB
        private const val PEAK_DECAY = 12f / RANGE_DB

        /** Linear magnitude (1.0 = full-scale sine) to 0..1 over -60..0 dBFS. */
        private fun normalise(mag: Float): Float {
            val db = 20f * log10(max(mag, 1e-9f))
            return ((db + RANGE_DB) / RANGE_DB).coerceIn(0f, 1f)
        }

        /**
         * Bin boundaries: band b covers bins edges[b] until edges[b + 1] (size BANDS + 1).
         * Log-spaced from [MIN_HZ] to [MAX_HZ]; strictly increasing so every band has at
         * least one bin; bin 0 (DC) is never used.
         */
        fun bandEdges(fs: Int, n: Int = N): IntArray {
            val half = n / 2
            val ratio = MAX_HZ / MIN_HZ
            val edges = IntArray(BANDS + 1)
            for (i in 0..BANDS) {
                val hz = MIN_HZ * ratio.pow(i.toDouble() / BANDS)
                edges[i] = (hz * n / fs).roundToInt()
            }
            edges[0] = max(edges[0], 1)
            for (i in 1..BANDS) edges[i] = max(edges[i], edges[i - 1] + 1)
            for (i in BANDS downTo 0) edges[i] = min(edges[i], half - (BANDS - i))
            return edges
        }
    }
}

/** Pure bin mapping, see [SpectrumAnalyzer.bandEdges]. */
fun bandEdges(fs: Int, n: Int = SpectrumAnalyzer.N): IntArray = SpectrumAnalyzer.bandEdges(fs, n)
