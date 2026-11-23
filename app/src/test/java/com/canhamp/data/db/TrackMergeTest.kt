package com.canhamp.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackMergeTest {
    private fun track(id: Long = 0, title: String = "t", hz: Int? = null) = TrackEntity(
        id = id, uri = "content://1", source = "MEDIASTORE", folderPath = "/Music", fileName = "t.mp3",
        title = title, artist = "a", album = "b", format = "MP3", durationMs = 1000, sampleRateHz = hz,
        sizeBytes = 10, dateAddedSec = 1, dateModifiedSec = 1, scanGeneration = 1,
    )

    @Test fun scanWithoutSampleRateKeepsTheLearnedOne() {
        val merged = TrackMerge.keepLearned(track(id = 7, title = "old", hz = 48_000), track(title = "new"))
        assertEquals(48_000, merged.sampleRateHz)
        assertEquals("new", merged.title)
        assertEquals(7L, merged.id)
    }

    @Test fun incomingSampleRateWins() {
        val merged = TrackMerge.keepLearned(track(id = 7, hz = 44_100), track(hz = 48_000))
        assertEquals(48_000, merged.sampleRateHz)
    }

    @Test fun nothingLearnedStaysNull() {
        assertNull(TrackMerge.keepLearned(track(id = 7), track()).sampleRateHz)
    }
}
