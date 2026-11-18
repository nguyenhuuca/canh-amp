package com.canhamp.data.queue

import com.canhamp.data.db.QueueItemEntity
import com.canhamp.playback.QueueEntry
import com.canhamp.util.NoopLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueueRepositoryTest {

    private class FakeStore(var rows: List<QueueItemEntity> = emptyList()) : QueueStore {
        val writes = mutableListOf<List<QueueItemEntity>>()
        override suspend fun replaceAll(items: List<QueueItemEntity>) {
            writes += items
            rows = items
        }

        override suspend fun getAll() = rows
    }

    private class FakePosition(var saved: Pair<Int, Long> = 0 to 0L) : QueuePositionStore {
        override suspend fun save(index: Int, positionMs: Long) {
            saved = index to positionMs
        }

        override suspend fun load() = saved
    }

    private fun entry(id: Long) = QueueEntry(id, "content://x/$id", "T$id", "A", "B", 1000L, 128)

    private fun TestScope.repo(store: FakeStore, pos: FakePosition = FakePosition()) =
        QueueRepository(store, pos, NoopLogger, scope = this.backgroundScope)

    @Test
    fun rapidMirrorsCollapseIntoOneWriteOfTheLatest() = runTest {
        val store = FakeStore()
        val repo = repo(store)
        repo.mirror(listOf(entry(1)))
        advanceTimeBy(200)
        repo.mirror(listOf(entry(1), entry(2)))
        advanceTimeBy(200)
        repo.mirror(listOf(entry(3)))
        advanceTimeBy(499)
        runCurrent()
        assertTrue(store.writes.isEmpty())
        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, store.writes.size)
        assertEquals(listOf(3L), store.writes[0].map { it.trackId })
    }

    @Test
    fun capsAtFiveThousandAndReportsExcess() = runTest {
        val store = FakeStore()
        val repo = repo(store)
        val ok = repo.mirror((1L..5000L).map(::entry))
        advanceTimeBy(600)
        runCurrent()
        assertTrue(ok)
        assertEquals(5000, store.writes.single().size)

        val over = repo.mirror((1L..5001L).map(::entry))
        advanceTimeBy(600)
        runCurrent()
        assertFalse(over)
        assertEquals(5000, store.writes.last().size)
        assertEquals(4999, store.writes.last().last().position)
    }

    @Test
    fun roomLeftUnderTheCap() {
        assertEquals(10, QueueRepository.room(0, 10))
        assertEquals(3, QueueRepository.room(4997, 10))
        assertEquals(0, QueueRepository.room(5000, 10))
        assertEquals(0, QueueRepository.room(6000, 10))
    }

    @Test
    fun flushWritesNowAndDropsTheScheduledWrite() = runTest {
        val store = FakeStore()
        val repo = repo(store)
        repo.mirror(listOf(entry(1)))
        repo.flush(listOf(entry(2)))
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, store.writes.size)
        assertEquals(listOf(2L), store.writes[0].map { it.trackId })
    }

    @Test
    fun loadMapsRowsInOrderAndClampsIndex() = runTest {
        val rows = listOf(2, 0, 1).map {
            QueueItemEntity(it, "content://x/$it", "T$it", "A", 1000L, null, it.toLong())
        }
        val repo = repo(FakeStore(rows), FakePosition(9 to 4200L))
        val snap = repo.load()
        assertEquals(listOf(0L, 1L, 2L), snap.items.map { it.trackId })
        assertEquals(2, snap.index)
        assertEquals(4200L, snap.positionMs)
    }

    @Test
    fun loadOfEmptyQueueIsIndexZero() = runTest {
        val snap = repo(FakeStore(), FakePosition(5 to 10L)).load()
        assertTrue(snap.items.isEmpty())
        assertEquals(0, snap.index)
    }

    @Test
    fun savePositionDelegates() = runTest {
        val pos = FakePosition()
        repo(FakeStore(), pos).savePosition(3, 1234L)
        assertEquals(3 to 1234L, pos.saved)
    }
}
