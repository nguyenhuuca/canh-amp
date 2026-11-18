package com.canhamp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FaderMathTest {
    @Test fun midpointContinuous() = assertEquals(0.5f, faderValueFromX(50f, 100f, 0), 1e-6f)
    @Test fun clampsLow() = assertEquals(0f, faderValueFromX(-20f, 100f, 0), 0f)
    @Test fun clampsHigh() = assertEquals(1f, faderValueFromX(250f, 100f, 0), 0f)
    @Test fun zeroWidthIsZero() = assertEquals(0f, faderValueFromX(10f, 0f, 0), 0f)
    @Test fun snapsToSteps() = assertEquals(0.25f, faderValueFromX(30f, 100f, 4), 1e-6f)
    @Test fun snapsUpPastHalfStep() = assertEquals(0.5f, faderValueFromX(40f, 100f, 4), 1e-6f)
    @Test fun stepsStayClamped() = assertEquals(1f, faderValueFromX(500f, 100f, 4), 0f)
}
