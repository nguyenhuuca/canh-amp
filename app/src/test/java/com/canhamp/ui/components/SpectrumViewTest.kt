package com.canhamp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SpectrumViewTest {
    @Test fun animatedShowsLevelAndPeak() {
        assertEquals(litSegments(0.5f) to litSegments(0.8f), shownSegments(0.5f, 0.8f, animated = true))
    }

    @Test fun notAnimatedShowsNothingEvenWithPeaks() {
        // Regression: the peak dots kept following the music with animation off.
        assertEquals(0 to 0, shownSegments(0.5f, 0.8f, animated = false))
    }
}
