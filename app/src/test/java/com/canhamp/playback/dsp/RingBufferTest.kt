package com.canhamp.playback.dsp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingBufferTest {
    @Test
    fun insufficientDataReturnsFalse() {
        val ring = RingBuffer(8)
        val dest = FloatArray(4)
        assertFalse(ring.readLatest(dest, 4))
        ring.write(floatArrayOf(1f, 2f, 3f), 0, 3)
        assertFalse(ring.readLatest(dest, 4))
        assertEquals(3L, ring.writeIndex)
    }

    @Test
    fun readLatestReturnsMostRecentInOrder() {
        val ring = RingBuffer(8)
        ring.write(floatArrayOf(1f, 2f, 3f, 4f, 5f), 0, 5)
        val dest = FloatArray(3)
        assertTrue(ring.readLatest(dest, 3))
        assertArrayEquals(floatArrayOf(3f, 4f, 5f), dest, 0f)
    }

    @Test
    fun wrapAroundKeepsOrder() {
        val ring = RingBuffer(8)
        ring.write(FloatArray(6) { it.toFloat() }, 0, 6)
        ring.write(FloatArray(6) { 10f + it }, 0, 6) // total 12 > capacity
        val dest = FloatArray(8)
        assertTrue(ring.readLatest(dest, 8))
        assertArrayEquals(floatArrayOf(4f, 5f, 10f, 11f, 12f, 13f, 14f, 15f), dest, 0f)
    }

    @Test
    fun writeHonoursOffset() {
        val ring = RingBuffer(8)
        ring.write(floatArrayOf(9f, 1f, 2f, 9f), 1, 2)
        val dest = FloatArray(2)
        assertTrue(ring.readLatest(dest, 2))
        assertArrayEquals(floatArrayOf(1f, 2f), dest, 0f)
    }

    @Test
    fun cannotReadMoreThanCapacity() {
        val ring = RingBuffer(4)
        ring.write(FloatArray(10), 0, 10)
        assertFalse(ring.readLatest(FloatArray(8), 8))
    }
}
