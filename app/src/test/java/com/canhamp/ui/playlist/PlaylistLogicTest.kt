package com.canhamp.ui.playlist

import com.canhamp.playback.QueueEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaylistLogicTest {
    private fun entry(
        title: String,
        artist: String = "",
        durationMs: Long = 1_000L,
        id: Long = title.hashCode().toLong().coerceAtLeast(0L),
        uri: String = "content://t/$title",
    ) = QueueEntry(id, uri, title, artist, "", durationMs, 128)

    private val queue = listOf(
        entry("charlie", "Zed", 300_000),
        entry("Alpha", "mid", 100_000),
        entry("bravo", "Abe", 200_000),
    )

    @Test
    fun statsFormatTotalAndSelected() {
        val q = listOf(entry("a", durationMs = 3_723_000), entry("b", durationMs = 61_000))
        val s = queueStats(q, 1)
        assertEquals(2, s.count)
        assertEquals("1:03:04", s.totalText)
        assertEquals("01:01", s.selectedText)
    }

    @Test
    fun statsWithNoSelectionOrEmptyQueue() {
        assertEquals("--:--", queueStats(queue, null).selectedText)
        assertEquals("--:--", queueStats(queue, 9).selectedText)
        val empty = queueStats(emptyList(), null)
        assertEquals(0, empty.count)
        assertEquals("0:00:00", empty.totalText)
    }

    @Test
    fun sortByTitleIsCaseInsensitive() {
        assertEquals(listOf(1, 2, 0), sortOrder(queue, SortMode.TITLE))
    }

    @Test
    fun sortByArtistThenTitle() {
        val q = listOf(entry("b", "X"), entry("a", "x"), entry("c", "A"))
        assertEquals(listOf(2, 1, 0), sortOrder(q, SortMode.ARTIST))
    }

    @Test
    fun sortByDurationIsAscendingAndStable() {
        val q = listOf(entry("a", durationMs = 5), entry("b", durationMs = 1), entry("c", durationMs = 5))
        assertEquals(listOf(1, 0, 2), sortOrder(q, SortMode.DURATION))
    }

    @Test
    fun everySortKeepsThePlayingItemTracked() {
        for (mode in SortMode.entries) {
            for (playing in queue.indices) {
                val order = sortOrder(queue, mode)
                val moved = queue.reordered(order)
                val at = newPositionOf(order, playing)!!
                assertEquals("$mode $playing", queue[playing], moved[at])
            }
        }
    }

    @Test
    fun sortModeCycles() {
        assertEquals(SortMode.TITLE, SortMode.after(null))
        assertEquals(SortMode.ARTIST, SortMode.after(SortMode.TITLE))
        assertEquals(SortMode.DURATION, SortMode.after(SortMode.ARTIST))
        assertEquals(SortMode.TITLE, SortMode.after(SortMode.DURATION))
    }

    @Test
    fun shuffleIsAPermutationAndTracksThePlayingItem() {
        val big = List(50) { entry("t$it") }
        repeat(20) { seed ->
            val order = shuffleOrder(big.size, Random(seed))
            assertEquals(big.indices.toList(), order.sorted())
            val playing = seed % big.size
            assertEquals(big[playing], big.reordered(order)[newPositionOf(order, playing)!!])
        }
    }

    @Test
    fun shuffleOfTinyQueues() {
        assertEquals(emptyList<Int>(), shuffleOrder(0, Random(1)))
        assertEquals(listOf(0), shuffleOrder(1, Random(1)))
    }

    @Test
    fun newPositionOfNullAndUnknown() {
        assertNull(newPositionOf(listOf(1, 0), null))
        assertNull(newPositionOf(listOf(1, 0), 5))
    }

    @Test
    fun filterMatchesTitleOrArtistIgnoringCase() {
        assertEquals(listOf(1), filterIndices(queue, "ALP"))
        assertEquals(listOf(2), filterIndices(queue, "abe"))
        assertEquals(listOf(0, 1, 2), filterIndices(queue, "  "))
        assertEquals(emptyList<Int>(), filterIndices(queue, "zzz"))
    }

    @Test
    fun filteredRowsKeepTheirQueuePosition() {
        val rows = buildRows(queue, playingIndex = 2, selected = 0, query = "r")
        assertEquals(listOf(0, 2), rows.map { it.index })
        assertTrue(rows[1].isPlaying)
        assertTrue(rows[0].isSelected)
        assertFalse(canReorder("r"))
        assertTrue(canReorder(""))
    }

    @Test
    fun playingRowIsNotAlsoSelected() {
        val rows = buildRows(queue, playingIndex = 1, selected = 1, query = "")
        assertTrue(rows[1].isPlaying)
        assertFalse(rows[1].isSelected)
    }

    @Test
    fun rowWithoutLibraryTrackIsMissing() {
        val rows = buildRows(listOf(entry("a", id = -1), entry("b", id = 4)), null, null, "")
        assertTrue(rows[0].missing)
        assertFalse(rows[1].missing)
    }

    @Test
    fun keysAreUniqueForTheSameTrackQueuedTwice() {
        val same = entry("a", uri = "content://x/1")
        val keys = rowKeys(listOf(same, entry("b"), same))
        assertEquals(3, keys.toSet().size)
    }

    @Test
    fun selectionFollowsAMove() {
        assertEquals(3, selectionAfterMove(1, 1, 3))
        assertEquals(1, selectionAfterMove(2, 1, 3))
        assertEquals(2, selectionAfterMove(1, 3, 1))
        assertEquals(0, selectionAfterMove(0, 1, 3))
        assertNull(selectionAfterMove(null, 0, 1))
    }

    @Test
    fun selectionAfterRemove() {
        assertNull(selectionAfterRemove(2, 2))
        assertEquals(1, selectionAfterRemove(2, 0))
        assertEquals(1, selectionAfterRemove(1, 2))
        assertNull(selectionAfterRemove(null, 0))
    }

    @Test
    fun undoRemembersEntryAndIndex() {
        val removed = removedRow(queue, 1)!!
        assertEquals(RemovedRow(1, queue[1]), removed)
        assertNull(removedRow(queue, 3))
        assertNull(removedRow(queue, -1))
    }

    @Test
    fun undoRestoresAtOldIndexOrTheEnd() {
        val removed = RemovedRow(4, queue[0])
        assertEquals(4, restoreIndex(removed, 10))
        assertEquals(2, restoreIndex(removed, 2))
        assertEquals(0, restoreIndex(RemovedRow(0, queue[0]), 0))
    }
}
