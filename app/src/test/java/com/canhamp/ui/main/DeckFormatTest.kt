package com.canhamp.ui.main

import com.canhamp.data.settings.TimeMode
import com.canhamp.playback.NowPlaying
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeckFormatTest {

    private val idle = "*** IDLE ***"

    private val track = NowPlaying(
        title = "Song", artist = "Band", index = 2, queueSize = 5,
        positionMs = 65_000, durationMs = 241_000,
        bitrateKbps = 128, sampleRateHz = 44_100, channelCount = 2,
    )

    @Test fun timerElapsed() = assertEquals("01:05", DeckFormat.timerText(track, TimeMode.ELAPSED))

    @Test fun timerRemaining() = assertEquals("-02:56", DeckFormat.timerText(track, TimeMode.REMAINING))

    @Test fun timerHours() {
        val np = track.copy(positionMs = 3_725_000, durationMs = 7_200_000)
        assertEquals("1:02:05", DeckFormat.timerText(np, TimeMode.ELAPSED))
        assertEquals("-57:55", DeckFormat.timerText(np, TimeMode.REMAINING))
    }

    @Test fun timerEmptyQueue() {
        assertEquals("--:--", DeckFormat.timerText(NowPlaying.EMPTY, TimeMode.ELAPSED))
        assertEquals("--:--", DeckFormat.timerText(NowPlaying.EMPTY, TimeMode.REMAINING))
    }

    @Test fun marqueeNormal() =
        assertEquals("*** 3. Band - Song (04:01) [128kbps stereo 44.1kHz] ***", DeckFormat.marqueeText(track, idle))

    @Test fun marqueeIdle() = assertEquals(idle, DeckFormat.marqueeText(NowPlaying.EMPTY, idle))

    @Test fun marqueeMonoWithoutArtistOrBitrate() {
        val np = track.copy(artist = "", bitrateKbps = null, channelCount = 1)
        assertEquals("*** 3. Song (04:01) [mono 44.1kHz] ***", DeckFormat.marqueeText(np, idle))
    }

    @Test fun marqueeUnknownTechnicalInfo() {
        val np = track.copy(bitrateKbps = null, sampleRateHz = null, channelCount = null)
        assertEquals("*** 3. Band - Song (04:01) ***", DeckFormat.marqueeText(np, idle))
    }

    @Test fun kHz() {
        assertEquals("44.1", DeckFormat.kHz(44_100))
        assertEquals("48.0", DeckFormat.kHz(48_000))
        assertEquals("--", DeckFormat.kHz(null))
        assertEquals("--", DeckFormat.kHz(0))
    }

    @Test fun channelLabel() {
        assertEquals("STEREO", DeckFormat.channelLabel(2))
        assertEquals("STEREO", DeckFormat.channelLabel(6))
        assertEquals("MONO", DeckFormat.channelLabel(1))
        assertEquals("--", DeckFormat.channelLabel(null))
    }

    @Test fun trackNumber() {
        assertEquals("TRACK 01", DeckFormat.trackNumber(0, 3))
        assertEquals("TRACK 12", DeckFormat.trackNumber(11, 20))
        assertEquals("TRACK --", DeckFormat.trackNumber(0, 0))
    }

    @Test fun formatBadge() {
        assertEquals("MPEG-1 LAYER 3", DeckFormat.formatBadge("audio/mpeg"))
        assertEquals("FLAC", DeckFormat.formatBadge("audio/flac"))
        assertEquals("AAC", DeckFormat.formatBadge("audio/mp4a-latm"))
        assertEquals("AMR-NB", DeckFormat.formatBadge("audio/amr-nb"))
        assertNull(DeckFormat.formatBadge(null))
        assertNull(DeckFormat.formatBadge(""))
    }

    @Test fun balanceLabel() {
        assertEquals("CENTER", DeckFormat.balanceLabel(0f, "CENTER"))
        assertEquals("L 40", DeckFormat.balanceLabel(-0.4f, "CENTER"))
        assertEquals("R 40", DeckFormat.balanceLabel(0.4f, "CENTER"))
        assertEquals("R 100", DeckFormat.balanceLabel(1f, "CENTER"))
    }

    @Test fun balanceSnapsToCentreBelowOneStep() {
        assertEquals(0f, DeckFormat.snapBalance(0.049f), 0f)
        assertEquals(0f, DeckFormat.snapBalance(-0.049f), 0f)
        assertEquals(0.05f, DeckFormat.snapBalance(0.05f), 1e-6f)
    }

    @Test fun balanceStepsOfFivePercentAndClamps() {
        assertEquals(0.4f, DeckFormat.snapBalance(0.41f), 1e-6f)
        assertEquals(-0.35f, DeckFormat.snapBalance(-0.34f), 1e-6f)
        assertEquals(1f, DeckFormat.snapBalance(3f), 0f)
        assertEquals(-1f, DeckFormat.snapBalance(-3f), 0f)
        assertEquals(0f, DeckFormat.snapBalance(Float.NaN), 0f)
    }

    @Test fun balanceFaderMapping() {
        assertEquals(0f, DeckFormat.balanceFromFader(0.5f), 0f)
        assertEquals(-1f, DeckFormat.balanceFromFader(0f), 0f)
        assertEquals(1f, DeckFormat.balanceFromFader(1f), 0f)
        assertEquals(0.5f, DeckFormat.faderFromBalance(0f), 0f)
        assertEquals(0f, DeckFormat.faderFromBalance(-1f), 0f)
    }

    @Test fun artworkSampleSizeKeepsTargetResolution() {
        assertEquals(1, artworkSampleSize(100, 100, 64))
        assertEquals(8, artworkSampleSize(600, 600, 64))
        assertEquals(1, artworkSampleSize(0, 0, 64))
        assertEquals(8, artworkSampleSize(1000, 4000, 64))
    }
}
