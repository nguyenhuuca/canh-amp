package com.canhamp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationsTest {
    @Test fun zero() = assertEquals("00:00", formatDuration(0))
    @Test fun fiveSeconds() = assertEquals("00:05", formatDuration(5_000))
    @Test fun threeFortyFive() = assertEquals("03:45", formatDuration(225_000))
    @Test fun justUnderOneHour() = assertEquals("59:59", formatDuration(3_599_999))
    @Test fun exactlyOneHour() = assertEquals("1:00:00", formatDuration(3_600_000))
    @Test fun overOneHour() = assertEquals("1:08:42", formatDuration(4_122_000))
    @Test fun negativeClamps() = assertEquals("00:00", formatDuration(-5_000))

    @Test fun remaining() = assertEquals("-02:00", formatRemaining(105_000, 225_000))
    @Test fun remainingPastEndClamps() = assertEquals("-00:00", formatRemaining(300_000, 225_000))
    @Test fun remainingLong() = assertEquals("-1:00:00", formatRemaining(0, 3_600_000))
}
