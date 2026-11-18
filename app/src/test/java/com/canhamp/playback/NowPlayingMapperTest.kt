package com.canhamp.playback

import com.canhamp.data.settings.RepeatSetting
import com.canhamp.playback.NowPlayingMapper.PreviousAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowPlayingMapperTest {

    @Test
    fun previousRestartsOnlyAfterThreeSeconds() {
        assertEquals(PreviousAction.PREVIOUS_ITEM, NowPlayingMapper.previousAction(0))
        assertEquals(PreviousAction.PREVIOUS_ITEM, NowPlayingMapper.previousAction(3_000))
        assertEquals(PreviousAction.RESTART, NowPlayingMapper.previousAction(3_001))
    }

    @Test
    fun bitrateRoundsToKbpsAndHidesUnknown() {
        assertEquals(320, NowPlayingMapper.bitrateKbps(320_000))
        assertEquals(128, NowPlayingMapper.bitrateKbps(127_800))
        assertNull(NowPlayingMapper.bitrateKbps(-1))
        assertNull(NowPlayingMapper.bitrateKbps(0))
    }

    @Test
    fun unknownSampleRateAndChannelsAreNull() {
        assertEquals(44_100, NowPlayingMapper.sampleRateHz(44_100))
        assertNull(NowPlayingMapper.sampleRateHz(-1))
        assertEquals(2, NowPlayingMapper.channelCount(2))
        assertNull(NowPlayingMapper.channelCount(-1))
    }

    @Test
    fun stereoPillStates() {
        assertEquals(true, NowPlayingMapper.isStereo(2))
        assertEquals(false, NowPlayingMapper.isStereo(1))
        assertNull(NowPlayingMapper.isStereo(null))
    }

    @Test
    fun repeatMapsBothWaysAndCycles() {
        RepeatSetting.entries.forEach {
            assertEquals(it, NowPlayingMapper.repeatFromPlayer(NowPlayingMapper.repeatToPlayer(it)))
        }
        assertEquals(RepeatSetting.ALL, NowPlayingMapper.nextRepeat(RepeatSetting.OFF))
        assertEquals(RepeatSetting.ONE, NowPlayingMapper.nextRepeat(RepeatSetting.ALL))
        assertEquals(RepeatSetting.OFF, NowPlayingMapper.nextRepeat(RepeatSetting.ONE))
    }
}
