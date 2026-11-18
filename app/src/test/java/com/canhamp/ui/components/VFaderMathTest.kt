package com.canhamp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class VFaderMathTest {
    @Test
    fun topIsMaxBottomIsMin() {
        assertEquals(12, faderStepFromY(0f, 100f, -12, 12))
        assertEquals(-12, faderStepFromY(100f, 100f, -12, 12))
        assertEquals(0, faderStepFromY(50f, 100f, -12, 12))
    }

    @Test
    fun snapsToWholeSteps() {
        // 100 px over 24 steps: one step is ~4.17 px.
        assertEquals(11, faderStepFromY(4.2f, 100f, -12, 12))
        assertEquals(12, faderStepFromY(1.5f, 100f, -12, 12))
    }

    @Test
    fun clampsOutsideTheTrack() {
        assertEquals(12, faderStepFromY(-50f, 100f, -12, 12))
        assertEquals(-12, faderStepFromY(500f, 100f, -12, 12))
        assertEquals(-12, faderStepFromY(10f, 0f, -12, 12))
    }
}
