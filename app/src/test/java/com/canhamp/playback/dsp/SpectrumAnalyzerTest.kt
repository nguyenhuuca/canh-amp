package com.canhamp.playback.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpectrumAnalyzerTest {
    private val fs = 44100

    private class Stamped(val t: Long, val frame: SpectrumFrame)

    private fun sine(hz: Double, count: Int, offset: Int = 0) =
        FloatArray(count) { sin(2 * PI * hz * (it + offset) / fs).toFloat() }

    private fun TestScope.analyzer(ring: RingBuffer) = SpectrumAnalyzer(
        ring = ring,
        sampleRateProvider = { fs },
        clock = { testScheduler.currentTime },
        dispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun TestScope.collect(a: SpectrumAnalyzer): MutableList<Stamped> {
        val list = mutableListOf<Stamped>()
        backgroundScope.launch { a.frames.collect { list += Stamped(testScheduler.currentTime, it) } }
        return list
    }

    /** Keeps feeding the ring like a live producer (about 33 ms of audio per tick). */
    private fun TestScope.producer(ring: RingBuffer, gen: (Int) -> FloatArray) {
        backgroundScope.launch {
            var pos = 0
            while (true) {
                val chunk = gen(pos)
                ring.write(chunk, 0, chunk.size)
                pos += chunk.size
                delay(33)
            }
        }
    }

    @Test
    fun sineLightsTheBandContainingIt() = runTest {
        val ring = RingBuffer(4096)
        producer(ring) { sine(1000.0, 1470, it) }
        val frames = collect(analyzer(ring))
        advanceTimeBy(500)
        runCurrent()
        val bands = frames.last().frame.bands
        val edges = bandEdges(fs)
        val bin = (1000.0 * 1024 / fs).roundToInt()
        val expected = (0 until 16).first { bin >= edges[it] && bin < edges[it + 1] }
        assertEquals(expected, bands.indices.maxByOrNull { bands[it] })
        assertTrue("band ${bands[expected]}", bands[expected] > 0.9f)
        assertEquals(256, frames.last().frame.wave.size)
        assertTrue(frames.last().frame.wave.any { abs(it) > 0.5f })
    }

    @Test
    fun silenceStaysAtFloor() = runTest {
        val ring = RingBuffer(4096)
        producer(ring) { FloatArray(1470) }
        val frames = collect(analyzer(ring))
        advanceTimeBy(500)
        runCurrent()
        assertTrue(frames.isNotEmpty())
        for (f in frames) {
            assertTrue(f.frame.bands.all { it == 0f })
            assertTrue(f.frame.peaks.all { it == 0f })
        }
    }

    @Test
    fun bandsDecayAt18DbPerSecond() = runTest {
        val ring = RingBuffer(4096)
        var silent = false
        producer(ring) { if (silent) FloatArray(1470) else sine(1000.0, 1470, it) }
        val frames = collect(analyzer(ring))
        advanceTimeBy(500)
        runCurrent()
        val bin = (1000.0 * 1024 / fs).roundToInt()
        val b = bandEdges(fs).let { e -> (0 until 16).first { bin >= e[it] && bin < e[it + 1] } }
        silent = true
        advanceTimeBy(100) // let the window fill with zeros
        runCurrent()
        val a = frames.last()
        advanceTimeBy(1000)
        runCurrent()
        val z = frames.last()
        val rate = (a.frame.bands[b] - z.frame.bands[b]) / ((z.t - a.t) / 1000f)
        assertEquals(18f / 60f, rate, 0.03f)
        // peak-hold falls at 12 dB/s
        val peakRate = (a.frame.peaks[b] - z.frame.peaks[b]) / ((z.t - a.t) / 1000f)
        assertEquals(12f / 60f, peakRate, 0.02f)
    }

    @Test
    fun pausedBarsReachFloorWithin500Ms() = runTest {
        val ring = RingBuffer(4096)
        ring.write(sine(1000.0, 4096), 0, 4096)
        val frames = collect(analyzer(ring))
        advanceTimeBy(66)
        runCurrent()
        val lit = frames.last().frame.bands.max()
        assertTrue(lit > 0.9f) // fresh data shows before the pause is detected
        // no writes: paused after 100 ms, then 120 dB/s
        advanceTimeBy(100 + 500 + 40)
        runCurrent()
        assertTrue(frames.last().frame.bands.all { it == 0f })
    }

    @Test
    fun noRingReadsWithoutCollector() = runTest {
        var reads = 0
        val ring = object : RingBuffer(4096) {
            override fun readLatest(dest: FloatArray, count: Int): Boolean {
                reads++
                return super.readLatest(dest, count)
            }
        }
        val a = analyzer(ring)
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(0, reads)
        val frames = collect(a)
        advanceTimeBy(200)
        runCurrent()
        assertTrue(reads > 0)
        assertTrue(frames.isNotEmpty())
    }

    @Test
    fun frameRateAtMost30Fps() = runTest {
        val ring = RingBuffer(4096)
        val frames = collect(analyzer(ring))
        advanceTimeBy(1000)
        runCurrent()
        assertTrue("${frames.size}", frames.size <= 31)
    }

    @Test
    fun bandEdgesAreMonotonicAndCover50To16k() {
        for (rate in listOf(8000, 22050, 44100, 48000, 96000)) {
            val e = bandEdges(rate)
            assertEquals(17, e.size)
            for (i in 1 until e.size) assertTrue("fs=$rate i=$i", e[i] > e[i - 1])
            assertTrue(e[0] >= 1)
            assertTrue(e[16] <= 512)
        }
        val e = bandEdges(44100)
        assertEquals((50.0 * 1024 / 44100).roundToInt(), e[0])
        assertEquals((16000.0 * 1024 / 44100).roundToInt(), e[16])
    }
}
