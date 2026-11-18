package com.canhamp.data.library

import com.canhamp.data.db.TrackEntity
import com.canhamp.data.db.TrackSource
import com.canhamp.util.NoopLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoryTest {

    private fun scanned(id: Long) = ScannedRow(
        MediaStoreRow(id, "t$id.mp3", "T$id", "A", "B", "audio/mpeg", 60_000, 1_000_000),
        "content://x/$id",
    )

    private class FakeSource(
        private val chunks: List<List<ScannedRow>>,
        private val failure: Exception? = null,
        private val pauseMs: Long = 0,
    ) : AudioSource {
        var calls = 0
        override suspend fun scan(onChunk: suspend (List<ScannedRow>) -> Unit) {
            calls++
            if (pauseMs > 0) delay(pauseMs)
            failure?.let { throw it }
            chunks.forEach { onChunk(it) }
        }
    }

    private class FakeStore : TrackStore {
        val uris = linkedSetOf<String>()
        var staleGen: Long? = null
        var staleRemoved = 0
        override suspend fun upsert(tracks: List<TrackEntity>): Int {
            var inserted = 0
            tracks.forEach { if (uris.add(it.uri)) inserted++ }
            return inserted
        }

        override suspend fun deleteStale(source: TrackSource, generation: Long): Int {
            assertEquals(TrackSource.MEDIASTORE, source)
            staleGen = generation
            return staleRemoved
        }

        override suspend fun count(): Int = uris.size
    }

    private class FakeGen(var gen: Long = 0, var last: Long = 0) : ScanGenerationStore {
        override suspend fun scanGeneration() = gen
        override suspend fun setScanGeneration(generation: Long) { gen = generation }
        override suspend fun lastScanAtMs() = last
        override suspend fun setLastScanAtMs(ms: Long) { last = ms }
    }

    private fun TestScope.repo(
        source: AudioSource,
        store: FakeStore,
        gen: FakeGen,
        permission: Boolean = true,
    ) = LibraryRepository(
        source, store, gen, { permission }, NoopLogger, { testScheduler.currentTime },
        scope = TestScope(StandardTestDispatcher(testScheduler)),
    )

    @Test fun scanIncrementsGenerationAndDeletesStale() = runTest {
        val store = FakeStore().apply { staleRemoved = 3; uris.add("content://x/1") }
        val gen = FakeGen(gen = 4)
        val src = FakeSource(listOf(listOf(scanned(1), scanned(2)), listOf(scanned(3))))
        val out = repo(src, store, gen).scan()
        val r = (out as ScanOutcome.Success).result
        assertEquals(2, r.added)
        assertEquals(1, r.updated)
        assertEquals(3, r.removed)
        assertEquals(3, r.total)
        assertEquals(5L, store.staleGen)
        assertEquals(5L, gen.gen)
    }

    @Test fun permissionMissingSkipsScanner() = runTest {
        val src = FakeSource(emptyList())
        val gen = FakeGen(gen = 2)
        val out = repo(src, FakeStore(), gen, permission = false).scan()
        assertEquals(ScanOutcome.Failure(ScanError.PermissionMissing), out)
        assertEquals(0, src.calls)
        assertEquals(2L, gen.gen)
    }

    @Test fun concurrentScansShareOneRun() = runTest {
        val src = FakeSource(listOf(listOf(scanned(1))), pauseMs = 100)
        val repo = repo(src, FakeStore(), FakeGen())
        val a = async { repo.scan() }
        val b = async { repo.scan() }
        val ra = a.await()
        val rb = b.await()
        assertEquals(1, src.calls)
        assertTrue(ra is ScanOutcome.Success)
        assertEquals(ra, rb)
    }

    @Test fun sequentialScansRunAgain() = runTest {
        val src = FakeSource(listOf(listOf(scanned(1))))
        val gen = FakeGen()
        val repo = repo(src, FakeStore(), gen)
        repo.scan()
        repo.scan()
        assertEquals(2, src.calls)
        assertEquals(2L, gen.gen)
    }

    @Test fun scanningIsNullWhenIdle() = runTest {
        val repo = repo(FakeSource(listOf(listOf(scanned(1)))), FakeStore(), FakeGen())
        assertNull(repo.scanning.value)
        repo.scan()
        assertNull(repo.scanning.value)
    }

    @Test fun securityExceptionMapsToPermissionMissing() = runTest {
        val src = FakeSource(emptyList(), failure = SecurityException("x"))
        val gen = FakeGen(gen = 1)
        val out = repo(src, FakeStore(), gen).scan()
        assertEquals(ScanOutcome.Failure(ScanError.PermissionMissing), out)
        assertEquals(1L, gen.gen)
    }

    @Test fun otherFailureMapsToStorageUnavailableAndKeepsStaleRows() = runTest {
        val store = FakeStore()
        val out = repo(FakeSource(emptyList(), failure = java.io.IOException("x")), store, FakeGen()).scan()
        assertEquals(ScanOutcome.Failure(ScanError.StorageUnavailable), out)
        assertNull(store.staleGen)
    }

    @Test fun successStoresLastScanTime() = runTest {
        val gen = FakeGen()
        repo(FakeSource(listOf(listOf(scanned(1)))), FakeStore(), gen).scan()
        assertEquals(testScheduler.currentTime, gen.last)
    }

    @Test fun shouldAutoScanRules() {
        val day = LibraryRepository.RESCAN_INTERVAL_MS
        assertTrue(LibraryRepository.shouldAutoScan(0, 1000, false))
        assertTrue(LibraryRepository.shouldAutoScan(1000, 1000 + day, false))
        assertFalse(LibraryRepository.shouldAutoScan(1000, 1000 + day - 1, false))
        assertTrue(LibraryRepository.shouldAutoScan(1000, 1001, true))
        assertTrue(LibraryRepository.shouldAutoScan(5000, 1000, false))
    }
}
