package com.canhamp.data.library

import com.canhamp.data.db.SafFolderEntity
import com.canhamp.data.db.TrackDao
import com.canhamp.data.db.TrackEntity
import com.canhamp.data.db.TrackSource
import com.canhamp.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

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

    /** Rows of one source; the scan uses it to tell an empty MediaStore answer from a real empty library. */
    suspend fun countBySource(source: TrackSource): Int = 0
}

class TrackDaoStore(private val dao: TrackDao) : TrackStore {
    override suspend fun upsert(tracks: List<TrackEntity>): Int = dao.upsertByUri(tracks)
    override suspend fun deleteStale(source: TrackSource, generation: Long): Int =
        dao.deleteStale(source.name, generation)

    override suspend fun count(): Int = dao.count().first()
    override suspend fun countBySource(source: TrackSource): Int = dao.countBySource(source.name)
}

class TrackDaoLookup(private val dao: TrackDao) : TrackLookup {
    override suspend fun findById(id: Long): TrackEntity? = dao.findById(id)
    override suspend fun updateSampleRate(id: Long, hz: Int) = dao.updateSampleRate(id, hz)
}

class LibraryRepository(
    private val source: AudioSource,
    private val tracks: TrackStore,
    private val store: ScanGenerationStore,
    private val hasPermission: () -> Boolean,
    private val logger: Logger,
    private val clock: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val lookup: TrackLookup? = null,
    private val tags: TagSource? = null,
    private val saf: SafImportDeps? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private var running: Deferred<ScanOutcome>? = null
    private val _scanning = MutableStateFlow<Int?>(null)

    /** Rows processed so far by the running scan, or null when idle (B14). */
    val scanning: StateFlow<Int?> = _scanning.asStateFlow()

    private val activeImports = AtomicInteger(0)
    private val _importing = MutableStateFlow<Int?>(null)

    /** Files found or read so far by a folder import, or null when none runs (B14). */
    val importing: StateFlow<Int?> = _importing.asStateFlow()

    /** Runs a MediaStore scan; a call made while one runs joins it (B6). */
    suspend fun scan(): ScanOutcome {
        val job = mutex.withLock {
            running?.takeIf { it.isActive } ?: scope.async { doScan() }.also { running = it }
        }
        return job.await()
    }

    /** Track details (B8): row fields plus file tags; a sample rate learned here is written back to the row. */
    suspend fun details(trackId: Long): DetailsResult {
        val row = lookup?.findById(trackId) ?: return DetailsResult.NotFound
        val read = tags?.read(row.uri) ?: return DetailsResult.Unreadable(row.toDetails(null))
        val hz = read.sampleRateHz
        if (row.sampleRateHz == null && hz != null && hz > 0) {
            try {
                lookup?.updateSampleRate(trackId, hz)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(TAG, "sample rate write-back failed: ${e.javaClass.simpleName}")
            }
        }
        return DetailsResult.Found(row.toDetails(read))
    }

    /**
     * Imports a SAF tree (B10). Rows are inserted with names-derived metadata and returned at once; tags,
     * duration and bitrate fill in on a background pass.
     */
    suspend fun importFolder(treeUri: String): ImportOutcome {
        val deps = saf ?: return ImportOutcome.Failed
        if (!treeUri.startsWith("content://") || treeUri.length > MAX_URI_CHARS) return ImportOutcome.Failed
        if (deps.store.findFolder(treeUri) != null) return ImportOutcome.AlreadyImported
        try {
            deps.grants.take(treeUri)
        } catch (e: SecurityException) {
            logger.w(TAG, "grant refused: ${e.javaClass.simpleName}")
            return ImportOutcome.NotPersistable
        }
        beginImport()
        var folderId = 0L
        try {
            val (name, files) = withContext(io) {
                val tree = deps.trees.open(treeUri)
                tree.displayName() to SafFolderScanner(tree).scan { _importing.value = it }
            }
            if (files.isEmpty()) {
                deps.grants.release(treeUri)
                return ImportOutcome.Empty
            }
            val displayName = SafTrackMapper.safeName(name)
            val nowSec = clock() / 1000
            mutex.withLock {
                folderId = deps.store.insertFolder(SafFolderEntity(treeUri = treeUri, displayName = displayName, addedAtSec = nowSec))
                if (folderId <= 0L) {
                    // A concurrent import of the same tree won; its grant stays.
                    folderId = 0L
                    return ImportOutcome.AlreadyImported
                }
                for (chunk in files.chunked(INSERT_CHUNK)) {
                    tracks.upsert(chunk.map { SafTrackMapper.map(it, folderId, displayName, nowSec) })
                }
            }
            val id = folderId
            beginImport()
            scope.launch { enrich(deps, id) }
            logger.i(TAG, "import done files=${files.size}")
            return ImportOutcome.Success(ImportResult(files.size))
        } catch (e: CancellationException) {
            rollbackImport(deps, treeUri, folderId)
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "import failed: ${e.javaClass.simpleName}")
            rollbackImport(deps, treeUri, folderId)
            return ImportOutcome.Failed
        } finally {
            endImport()
        }
    }

    private suspend fun rollbackImport(deps: SafImportDeps, treeUri: String, folderId: Long) {
        withContext(NonCancellable) {
            try {
                if (folderId > 0L) deps.store.removeFolder(folderId)
                deps.grants.release(treeUri)
            } catch (e: Exception) {
                logger.w(TAG, "import rollback failed: ${e.javaClass.simpleName}")
            }
        }
    }

    /** Removes an imported folder: releases its grant and deletes its tracks. */
    suspend fun removeFolder(folderId: Long) {
        val deps = saf ?: return
        val folder = deps.store.folders().firstOrNull { it.id == folderId } ?: return
        mutex.withLock { deps.store.removeFolder(folderId) }
        deps.grants.release(folder.treeUri)
    }

    /** On start: folders whose persisted grant was lost (revoked, app data restored) are removed. Returns how many. */
    suspend fun pruneLostGrants(): Int {
        val deps = saf ?: return 0
        val live = deps.grants.persisted()
        val lost = deps.store.folders().filter { it.treeUri !in live }
        for (folder in lost) mutex.withLock { deps.store.removeFolder(folder.id) }
        if (lost.isNotEmpty()) logger.i(TAG, "pruned lost grants count=${lost.size}")
        return lost.size
    }

    private fun beginImport() {
        if (activeImports.incrementAndGet() == 1) _importing.value = 0
    }

    private fun endImport() {
        if (activeImports.decrementAndGet() == 0) _importing.value = null
    }

    /** Reads tags for a folder's rows, [ENRICH_PARALLEL] at a time, and writes them in batches. */
    private suspend fun enrich(deps: SafImportDeps, folderId: Long) {
        try {
            val reader = tags ?: return
            val rows = deps.store.tracksOf(folderId)
            val gate = Semaphore(ENRICH_PARALLEL)
            val pending = ArrayList<TrackEntity>()
            var done = 0
            _importing.value = 0
            coroutineScope {
                for (row in rows) {
                    launch {
                        val updated = gate.withPermit { reader.read(row.uri)?.let { SafTrackMapper.enrich(row, it) } }
                        mutex.withLock {
                            done++
                            updated?.let(pending::add)
                            _importing.value = done
                            if (pending.size >= UPDATE_CHUNK || done == rows.size) {
                                if (pending.isNotEmpty()) deps.store.updateTracks(pending.toList())
                                pending.clear()
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(TAG, "tag pass failed: ${e.javaClass.simpleName}")
        } finally {
            endImport()
        }
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
            // An empty answer over a non-empty library is more likely a provider glitch than a wiped device.
            val existing = if (seen == 0) tracks.countBySource(TrackSource.MEDIASTORE) else 0
            if (seen == 0 && existing > 0) {
                logger.w(TAG, "scan saw 0 rows, keeping existing=$existing")
                val end = clock()
                store.setLastScanAtMs(end)
                return ScanOutcome.Success(ScanResult(0, 0, 0, tracks.count(), end - startedAt))
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
        const val ENRICH_PARALLEL = 4
        private const val INSERT_CHUNK = 500
        private const val UPDATE_CHUNK = 50
        private const val MAX_URI_CHARS = 2048
        const val RESCAN_INTERVAL_MS = 24L * 60 * 60 * 1000

        /** B6: scan on start when never scanned, last scan is 24 h old or more, or permission was just granted. */
        fun shouldAutoScan(lastScanAtMs: Long, nowMs: Long, permissionJustGranted: Boolean): Boolean =
            permissionJustGranted || lastScanAtMs <= 0L || nowMs < lastScanAtMs ||
                nowMs - lastScanAtMs >= RESCAN_INTERVAL_MS
    }
}
