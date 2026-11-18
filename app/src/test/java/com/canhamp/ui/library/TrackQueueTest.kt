package com.canhamp.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackQueueTest {

    private fun row(id: Long) = TrackRow(
        id = id, title = "t$id", artist = "a", album = "b", format = "mp3", bitrateKbps = 192,
        sampleRateHz = null, sizeBytes = 1, durationMs = 1_000, dateModifiedSec = 0, uri = "content://x/$id",
    )

    @Test
    fun tappedRowStartsInsideTheSnapshotWindow() {
        val snapshot = (1L..800L).map(::row)
        val (entries, start) = playQueueFor(snapshot[700], snapshot)
        assertEquals(500, entries.size)
        assertEquals(701L, entries[start].trackId)
    }

    @Test
    fun rowMissingFromSnapshotPlaysAlone() {
        val (entries, start) = playQueueFor(row(9), listOf(row(1), row(2)))
        assertEquals(listOf(9L), entries.map { it.trackId })
        assertEquals(0, start)
    }

    @Test
    fun entryCarriesTrackFields() {
        val e = row(4).toQueueEntry()
        assertEquals("content://x/4", e.uri)
        assertEquals(192, e.bitrateKbps)
    }
}
