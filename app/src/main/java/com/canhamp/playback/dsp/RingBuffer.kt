package com.canhamp.playback.dsp

import java.util.concurrent.atomic.AtomicLong

/**
 * Single-producer / single-consumer float ring for the visualizer tap (ADR-0002).
 * Lock-free; the consumer may see a torn read while the producer overwrites old samples,
 * which is acceptable because the data only feeds a display.
 */
open class RingBuffer(val capacity: Int = 4096) {
    private val data = FloatArray(capacity)
    private val writeIdx = AtomicLong(0)

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    /** Total samples ever written; advances only when audio flows. */
    open val writeIndex: Long get() = writeIdx.get()

    /** Producer side: appends [count] samples of [samples] starting at [offset]. */
    open fun write(samples: FloatArray, offset: Int, count: Int) {
        require(offset >= 0 && count >= 0 && offset + count <= samples.size) { "range out of bounds" }
        var w = writeIdx.get()
        for (i in 0 until count) {
            data[(w % capacity).toInt()] = samples[offset + i]
            w++
        }
        writeIdx.set(w) // publishes the samples above
    }

    /** Copies the most recent [count] samples (oldest first) into [dest]; false if not enough yet. */
    open fun readLatest(dest: FloatArray, count: Int): Boolean {
        if (count < 0 || count > capacity || count > dest.size) return false
        val w = writeIdx.get()
        if (w < count) return false
        val start = w - count
        for (i in 0 until count) dest[i] = data[((start + i) % capacity).toInt()]
        return true
    }
}
