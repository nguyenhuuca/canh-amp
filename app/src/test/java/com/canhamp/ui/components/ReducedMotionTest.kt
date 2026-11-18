package com.canhamp.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducedMotionTest {
    @Test fun zeroScaleIsReduced() = assertTrue(isReducedMotion(0f))
    @Test fun normalScaleIsNot() = assertFalse(isReducedMotion(1f))
    @Test fun halfScaleIsNot() = assertFalse(isReducedMotion(0.5f))
}
