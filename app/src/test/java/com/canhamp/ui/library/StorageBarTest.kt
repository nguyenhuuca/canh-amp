package com.canhamp.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageBarTest {
    private val gb = 1024L * 1024 * 1024

    @Test
    fun splitsAudioOtherAndFree() {
        val f = storageFractions(StorageUsage(totalBytes = 100 * gb, freeBytes = 40 * gb, audioBytes = 10 * gb))
        assertEquals(0.10f, f.audio, 1e-6f)
        assertEquals(0.50f, f.other, 1e-6f)
        assertEquals(0.40f, f.free, 1e-6f)
    }

    @Test
    fun audioLargerThanUsedIsClamped() {
        val f = storageFractions(StorageUsage(100, 90, 50))
        assertEquals(0.10f, f.audio, 1e-6f)
        assertEquals(0f, f.other, 1e-6f)
        assertEquals(0.90f, f.free, 1e-6f)
    }

    @Test
    fun unreadableTotalIsAllFree() {
        assertEquals(StorageFractions(0f, 0f, 1f), storageFractions(StorageUsage(0, 0, 5)))
    }

    @Test
    fun freeAboveTotalIsClamped() {
        val f = storageFractions(StorageUsage(100, 500, 0))
        assertEquals(1f, f.free, 1e-6f)
        assertEquals(0f, f.other, 1e-6f)
    }

    @Test
    fun formatsGigabytesWithOneDecimal() {
        assertEquals("1.5 GB", formatGb(gb + gb / 2))
        assertEquals("0.0 GB", formatGb(-5))
    }
}
