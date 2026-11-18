package com.canhamp.data.queue

import com.canhamp.data.db.QueueDao
import com.canhamp.data.db.QueueItemEntity
import com.canhamp.data.settings.SettingsRepository
import com.canhamp.playback.QueueEntry
import com.canhamp.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The slice of [QueueDao] the mirror needs. */
interface QueueStore {
    suspend fun replaceAll(items: List<QueueItemEntity>)
    suspend fun getAll(): List<QueueItemEntity>
}

class QueueDaoStore(private val dao: QueueDao) : QueueStore {
    override suspend fun replaceAll(items: List<QueueItemEntity>) = dao.replaceAll(items)
    override suspend fun getAll(): List<QueueItemEntity> = dao.getAll()
}

/** Where the playing index and position live (DataStore). */
interface QueuePositionStore {
    suspend fun save(index: Int, positionMs: Long)
    suspend fun load(): Pair<Int, Long>
}

class SettingsPositionStore(private val settings: SettingsRepository) : QueuePositionStore {
    override suspend fun save(index: Int, positionMs: Long) = settings.setQueuePosition(index, positionMs)
    override suspend fun load(): Pair<Int, Long> = settings.settings.first().let { it.queueIndex to it.queuePositionMs }
}

data class QueueSnapshot(val items: List<QueueEntry>, val index: Int, val positionMs: Long)

/** B55 to B57: mirrors the player queue to Room and the position to DataStore; restores both on start. */
class QueueRepository(
    private val store: QueueStore,
    private val position: QueuePositionStore,
    private val logger: Logger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val debounceMs: Long = DEBOUNCE_MS,
) {
    private var pending: Job? = null

    /**
     * Schedules a write of [items] 500 ms from now; a newer call replaces an unwritten one.
     * Returns false when the list is over [MAX_ITEMS]: only the first 5,000 are kept.
     */
    fun mirror(items: List<QueueEntry>): Boolean {
        val kept = items.take(MAX_ITEMS)
        val job = scope.launch {
            delay(debounceMs)
            write(kept)
        }
        pending?.cancel()
        pending = job
        return items.size <= MAX_ITEMS
    }

    /** Writes [items] now, dropping any scheduled write (used when the service is going away). */
    suspend fun flush(items: List<QueueEntry>) {
        pending?.cancel()
        pending = null
        write(items.take(MAX_ITEMS))
    }

    suspend fun savePosition(index: Int, positionMs: Long) {
        try {
            position.save(index, positionMs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(TAG, "position save failed: ${e.javaClass.simpleName}")
        }
    }

    /** Rows in order plus the saved index (clamped into range) and position. */
    suspend fun load(): QueueSnapshot {
        val rows = store.getAll().sortedBy { it.position }
        val items = rows.map {
            QueueEntry(
                trackId = it.trackId ?: -1L,
                uri = it.uri,
                title = it.title,
                artist = it.artist,
                album = "",
                durationMs = it.durationMs,
                bitrateKbps = it.bitrateKbps,
            )
        }
        val (index, ms) = position.load()
        return QueueSnapshot(items, if (items.isEmpty()) 0 else index.coerceIn(0, items.lastIndex), ms.coerceAtLeast(0L))
    }

    private suspend fun write(items: List<QueueEntry>) {
        val rows = items.mapIndexed { i, e ->
            QueueItemEntity(
                position = i,
                uri = e.uri,
                title = e.title,
                artist = e.artist,
                durationMs = e.durationMs,
                bitrateKbps = e.bitrateKbps,
                trackId = e.trackId,
            )
        }
        try {
            store.replaceAll(rows)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(TAG, "queue mirror failed: ${e.javaClass.simpleName}")
        }
    }

    companion object {
        private const val TAG = "QueueRepository"
        const val MAX_ITEMS = 5_000
        const val DEBOUNCE_MS = 500L

        /** How many of [incoming] fit when [existing] items are already queued. */
        fun room(existing: Int, incoming: Int): Int = (MAX_ITEMS - existing).coerceIn(0, incoming)
    }
}
