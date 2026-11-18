package com.canhamp.data.library

import com.canhamp.data.db.TrackDao
import com.canhamp.data.db.TrackEntity
import com.canhamp.data.db.TrackSource
import com.canhamp.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ScanResult(val added: Int, val updated: Int, val removed: Int, val total: Int, val durationMs: Long)

sealed interface ScanError {
    data object PermissionMissing : ScanError
    data object StorageUnavailable : ScanError
}

sealed interface ScanOutcome {
    data class Success(val result: ScanResult) : ScanOutcome
    data class Failure(val error: ScanError) : ScanOutcome
}

/** Scan bookkeeping kept in DataStore. */
interface ScanGenerationStore {
    suspend fun scanGeneration(): Long
    suspend fun setScanGeneration(generation: Long)
    suspend fun lastScanAtMs(): Long
    suspend fun setLastScanAtMs(ms: Long)
}

/** The slice of [TrackDao] a scan needs. */
interface TrackStore {
    /** Inserts or updates by uri; returns how many rows were newly inserted. */
    suspend fun upsert(tracks: List<TrackEntity>): Int
    suspend fun deleteStale(source: TrackSource, generation: Long): Int
    suspend fun count(): Int
}

class TrackDaoStore(private val dao: TrackDao) : TrackStore {
    override suspend fun upsert(tracks: List<TrackEntity>): Int = dao.upsertByUri(tracks)
    override suspend fun deleteStale(source: TrackSource, generation: Long): Int =
        dao.deleteStale(source.name, generation)

    override suspend fun count(): Int = dao.count().first()
}

class LibraryRepository(
    private val source: AudioSource,
    private val tracks: TrackStore,
    private val store: ScanGenerationStore,
    private val hasPermission: () -> Boolean,
    private val logger: Logger,
    private val clock: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mutex = Mutex()
    private var running: Deferred<ScanOutcome>? = null
    private val _scanning = MutableStateFlow<Int?>(null)

    /** Rows processed so far by the running scan, or null when idle (B14). */
    val scanning: StateFlow<Int?> = _scanning.asStateFlow()

    /** Runs a MediaStore scan; a call made while one runs joins it (B6). */
    suspend fun scan(): ScanOutcome {
        val job = mutex.withLock {
            running?.takeIf { it.isActive } ?: scope.async { doScan() }.also { running = it }
        }
        return job.await()
    }

    private suspend fun doScan(): ScanOutcome {
        if (!hasPermission()) return ScanOutcome.Failure(ScanError.PermissionMissing)
        val startedAt = clock()
        _scanning.value = 0
        try {
            val newGen = store.scanGeneration() + 1
            var added = 0
            var seen = 0
            source.scan { chunk ->
                val entities = chunk.map { TrackRowMapper.map(it.row, it.contentUri, newGen) }
                added += tracks.upsert(entities)
                seen += entities.size
                _scanning.value = seen
            }
            val removed = tracks.deleteStale(TrackSource.MEDIASTORE, newGen)
            store.setScanGeneration(newGen)
            val end = clock()
            store.setLastScanAtMs(end)
            val result = ScanResult(added, seen - added, removed, tracks.count(), end - startedAt)
            logger.i(
                TAG,
                "scan done added=$added updated=${result.updated} removed=$removed " +
                    "total=${result.total} ms=${result.durationMs}",
            )
            return ScanOutcome.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.w(TAG, "scan denied: ${e.javaClass.simpleName}")
            return ScanOutcome.Failure(ScanError.PermissionMissing)
        } catch (e: Exception) {
            logger.e(TAG, "scan failed: ${e.javaClass.simpleName}")
            return ScanOutcome.Failure(ScanError.StorageUnavailable)
        } finally {
            _scanning.value = null
        }
    }

    companion object {
        private const val TAG = "LibraryRepository"
        const val RESCAN_INTERVAL_MS = 24L * 60 * 60 * 1000

        /** B6: scan on start when never scanned, last scan is 24 h old or more, or permission was just granted. */
        fun shouldAutoScan(lastScanAtMs: Long, nowMs: Long, permissionJustGranted: Boolean): Boolean =
            permissionJustGranted || lastScanAtMs <= 0L || nowMs < lastScanAtMs ||
                nowMs - lastScanAtMs >= RESCAN_INTERVAL_MS
    }
}
