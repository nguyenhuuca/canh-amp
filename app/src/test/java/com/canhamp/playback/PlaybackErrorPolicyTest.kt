package com.canhamp.playback

import com.canhamp.playback.PlaybackErrorPolicy.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorPolicyTest {

    @Test
    fun firstFourFailuresSkipAndFifthStops() {
        val p = PlaybackErrorPolicy()
        repeat(4) { assertEquals(Action.SKIP, p.onError(hasNext = true)) }
        assertEquals(Action.STOP, p.onError(hasNext = true))
    }

    @Test
    fun noNextItemStopsImmediately() {
        assertEquals(Action.STOP, PlaybackErrorPolicy().onError(hasNext = false))
    }

    @Test
    fun playingResetsTheRun() {
        val p = PlaybackErrorPolicy()
        repeat(4) { p.onError(hasNext = true) }
        p.onPlaying()
        repeat(4) { assertEquals(Action.SKIP, p.onError(hasNext = true)) }
        assertEquals(Action.STOP, p.onError(hasNext = true))
    }

    @Test
    fun stopResetsSoALaterRetryGetsAFreshRun() {
        val p = PlaybackErrorPolicy(maxConsecutive = 2)
        assertEquals(Action.SKIP, p.onError(true))
        assertEquals(Action.STOP, p.onError(true))
        assertEquals(0, p.failures)
        assertEquals(Action.SKIP, p.onError(true))
    }
}
