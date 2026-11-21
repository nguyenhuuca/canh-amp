package com.canhamp.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalPlayerLogicTest {
    @Test fun bigKeyPausesWhilePlaying() {
        assertEquals(PlayKey.PAUSE, playKey(isPlaying = true))
        assertEquals(PlayKey.PLAY, playKey(isPlaying = false))
    }

    @Test fun vinylSpinsOnlyWhenPlayingAnimatedAndMotionAllowed() {
        assertTrue(vinylSpinning(isPlaying = true, visAnimated = true, reducedMotion = false))
        assertFalse(vinylSpinning(isPlaying = false, visAnimated = true, reducedMotion = false))
        assertFalse(vinylSpinning(isPlaying = true, visAnimated = false, reducedMotion = false))
        // Regression: with animation scale 0 the spin loop ran once per frame without visible motion.
        assertFalse(vinylSpinning(isPlaying = true, visAnimated = true, reducedMotion = true))
    }
}
