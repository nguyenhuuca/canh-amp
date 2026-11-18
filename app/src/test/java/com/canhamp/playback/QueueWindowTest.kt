package com.canhamp.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueWindowTest {

    @Test
    fun emptyListGivesEmptyWindow() {
        assertEquals(QueueWindow(0, 0, 0), queueWindow(0, 5))
    }

    @Test
    fun shortListIsKeptWhole() {
        assertEquals(QueueWindow(0, 10, 7), queueWindow(10, 7))
        assertEquals(QueueWindow(0, 500, 499), queueWindow(500, 499))
    }

    @Test
    fun longListCentersOnTheTappedItem() {
        val w = queueWindow(2_000, 1_000)
        assertEquals(500, w.size)
        assertEquals(750, w.from)
        assertEquals(250, w.startIndex)
    }

    @Test
    fun windowSlidesAtBothEdges() {
        val head = queueWindow(2_000, 10)
        assertEquals(QueueWindow(0, 500, 10), head)
        val tail = queueWindow(2_000, 1_995)
        assertEquals(QueueWindow(1_500, 2_000, 495), tail)
    }

    @Test
    fun outOfRangeIndexIsClamped() {
        assertEquals(0, queueWindow(10, -3).startIndex)
        assertEquals(9, queueWindow(10, 99).startIndex)
    }

    @Test
    fun windowAroundReturnsTheTappedItemAtStart() {
        val list = (0 until 1_200).toList()
        val (window, start) = windowAround(list, 600)
        assertEquals(500, window.size)
        assertEquals(600, window[start])
    }
}
