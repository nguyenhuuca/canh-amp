package com.canhamp.data.library

import com.canhamp.data.db.SafFolderEntity
import com.canhamp.data.db.TrackEntity
import com.canhamp.data.db.TrackSource
import com.canhamp.util.NoopLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositorySafTest {

    private class FakeTree(private val files: List<String>, private val name: String = "Music") : DocumentTree {
        override val rootDocumentId = "root"
        override fun displayName() = name
        override fun children(documentId: String): List<DocEntry> = when (documentId) {
            "root" -> files.filter { '/' !in it }.map { DocEntry(it, it, "audio/mpeg", 100, 7_000, false) } +
                files.filter { '/' in it }.map { it.substringBefore('/') }.distinct()
                    .map { DocEntry("dir:$it", it, "vnd.android.document/directory", 0, 0, true) }
            else -> files.filter { it.startsWith(documentId.removePrefix("dir:") + "/") }
                .map { DocEntry(it, it.substringAfter('/'), "audio/mpeg", 100, 7_000, false) }
        }

        override fun documentUri(documentId: String) = "content://tree/$documentId"
    }

    private class FakeTrees(var tree: DocumentTree, var failure: Exception? = null) : DocumentTrees {
        override fun open(treeUri: String): DocumentTree {
            failure?.let { throw it }
            return tree
        }
    }

    private class FakeGrants(var refuse: Boolean = false, val live: MutableSet<String> = linkedSetOf()) : UriPermissions {
        val released = mutableListOf<String>()
        override fun take(treeUri: String) {
            if (refuse) throw SecurityException("no")
            live.add(treeUri)
        }

        override fun release(treeUri: String) {
            released.add(treeUri)
            live.remove(treeUri)
        }

        override fun persisted(): Set<String> = live.toSet()
    }

    private class FakeSafStore : SafStore, TrackStore {
        val folderRows = mutableListOf<SafFolderEntity>()
        val rows = linkedMapOf<String, TrackEntity>()
        private var nextFolder = 1L
        private var nextTrack = 1L

        override suspend fun findFolder(treeUri: String) = folderRows.firstOrNull { it.treeUri == treeUri }
        override suspend fun insertFolder(folder: SafFolderEntity): Long {
            if (findFolder(folder.treeUri) != null) return -1
            val id = nextFolder++
            folderRows.add(folder.copy(id = id))
            return id
        }

        override suspend fun folders() = folderRows.toList()
        override suspend fun tracksOf(folderId: Long) = rows.values.filter { it.safFolderId == folderId }
        override suspend fun updateTracks(tracks: List<TrackEntity>) {
            tracks.forEach { t -> if (rows.containsKey(t.uri)) rows[t.uri] = t }
        }

        override suspend fun removeFolder(folderId: Long) {
            rows.values.removeAll { it.safFolderId == folderId }
            folderRows.removeAll { it.id == folderId }
        }

        override suspend fun upsert(tracks: List<TrackEntity>): Int {
            var inserted = 0
            for (t in tracks) {
                val existing = rows[t.uri]
                if (existing == null) inserted++
                rows[t.uri] = t.copy(id = existing?.id ?: nextTrack++)
            }
            return inserted
        }

        override suspend fun deleteStale(source: TrackSource, generation: Long) = 0
        override suspend fun count() = rows.size
    }

    private class FakeGen : ScanGenerationStore {
        override suspend fun scanGeneration() = 0L
        override suspend fun setScanGeneration(generation: Long) = Unit
        override suspend fun lastScanAtMs() = 0L
        override suspend fun setLastScanAtMs(ms: Long) = Unit
    }

    private class FakeTags(private val byUri: Map<String, TrackTags?> = emptyMap()) : TagSource {
        var reads = 0
        var inFlight = 0
        var maxInFlight = 0
        override suspend fun read(uri: String): TrackTags? {
            reads++
            inFlight++
            maxInFlight = maxOf(maxInFlight, inFlight)
            kotlinx.coroutines.yield()
            inFlight--
            return byUri[uri]
        }
    }

    private class Rig(
        val scope: TestScope,
        val store: FakeSafStore,
        val grants: FakeGrants,
        val trees: FakeTrees,
        val tags: FakeTags,
        val repo: LibraryRepository,
    )

    private fun TestScope.rig(
        files: List<String> = listOf("a.mp3", "b.flac"),
        tags: FakeTags = FakeTags(),
        grants: FakeGrants = FakeGrants(),
    ): Rig {
        val store = FakeSafStore()
        val trees = FakeTrees(FakeTree(files))
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repo = LibraryRepository(
            source = object : AudioSource {
                override suspend fun scan(onChunk: suspend (List<ScannedRow>) -> Unit) = Unit
            },
            tracks = store,
            store = FakeGen(),
            hasPermission = { true },
            logger = NoopLogger,
            clock = { 42_000L },
            scope = TestScope(dispatcher),
            tags = tags,
            saf = SafImportDeps(store, trees, grants),
            io = dispatcher,
        )
        return Rig(this, store, grants, trees, tags, repo)
    }

    private val tree = "content://tree/primary%3AMusic"

    @Test fun successInsertsRowsWithNameDerivedMetadata() = runTest {
        val r = rig(listOf("Song One.mp3", "Rock/Deep.flac"))
        val out = r.repo.importFolder(tree)
        assertEquals(ImportOutcome.Success(ImportResult(2)), out)
        assertEquals(1, r.store.folderRows.size)
        assertEquals("Music", r.store.folderRows.single().displayName)
        assertTrue(tree in r.grants.live)
        val byName = r.store.rows.values.associateBy { it.fileName }
        val top = byName.getValue("Song One.mp3")
        assertEquals("Song One", top.title)
        assertEquals("<unknown>", top.artist)
        assertEquals("<unknown>", top.album)
        assertEquals("/SAF/Music", top.folderPath)
        assertEquals(TrackSource.SAF.name, top.source)
        assertEquals(r.store.folderRows.single().id, top.safFolderId)
        assertEquals("MP3", top.format)
        assertEquals(7L, top.dateModifiedSec)
        assertEquals("/SAF/Music/Rock", byName.getValue("Deep.flac").folderPath)
    }

    @Test fun alreadyImportedKeepsGrantAndRows() = runTest {
        val r = rig()
        r.repo.importFolder(tree)
        advanceUntilIdle()
        val again = r.repo.importFolder(tree)
        assertEquals(ImportOutcome.AlreadyImported, again)
        assertEquals(2, r.store.rows.size)
        assertEquals(1, r.store.folderRows.size)
        assertTrue(r.grants.released.isEmpty())
        assertTrue(tree in r.grants.live)
    }

    @Test fun emptyFolderReleasesGrantAndInsertsNothing() = runTest {
        val r = rig(listOf("cover.jpg", "readme.txt"))
        assertEquals(ImportOutcome.Empty, r.repo.importFolder(tree))
        assertEquals(listOf(tree), r.grants.released)
        assertTrue(r.grants.live.isEmpty())
        assertTrue(r.store.folderRows.isEmpty())
        assertTrue(r.store.rows.isEmpty())
    }

    @Test fun refusedGrantIsNotPersistable() = runTest {
        val r = rig(grants = FakeGrants(refuse = true))
        assertEquals(ImportOutcome.NotPersistable, r.repo.importFolder(tree))
        assertTrue(r.store.folderRows.isEmpty())
    }

    @Test fun walkFailureReleasesGrantAndReportsFailed() = runTest {
        val r = rig()
        r.trees.failure = IllegalArgumentException("bad")
        assertEquals(ImportOutcome.Failed, r.repo.importFolder(tree))
        assertEquals(listOf(tree), r.grants.released)
        assertTrue(r.store.folderRows.isEmpty())
    }

    @Test fun rejectsNonContentUris() = runTest {
        val r = rig()
        assertEquals(ImportOutcome.Failed, r.repo.importFolder("file:///sdcard/Music"))
        assertEquals(ImportOutcome.Failed, r.repo.importFolder(""))
        assertTrue(r.grants.live.isEmpty())
    }

    @Test fun backgroundPassFillsTagsAndKeepsNameWhenUnreadable() = runTest {
        val tags = FakeTags(
            mapOf(
                "content://tree/a.mp3" to TrackTags(
                    title = "Real Title", artist = "Artist", album = "Album",
                    durationMs = 123_000, bitrateKbps = 320, sampleRateHz = 44_100,
                ),
                "content://tree/b.flac" to null,
            ),
        )
        val r = rig(tags = tags)
        r.repo.importFolder(tree)
        advanceUntilIdle()
        val a = r.store.rows.getValue("content://tree/a.mp3")
        assertEquals("Real Title", a.title)
        assertEquals("Artist", a.artist)
        assertEquals("Album", a.album)
        assertEquals(123_000L, a.durationMs)
        assertEquals(320, a.bitrateKbps)
        assertEquals(44_100, a.sampleRateHz)
        val b = r.store.rows.getValue("content://tree/b.flac")
        assertEquals("b", b.title)
        assertEquals(0L, b.durationMs)
        assertNull(r.repo.importing.value)
    }

    @Test fun backgroundPassReadsAtMostFourAtATime() = runTest {
        val files = (1..30).map { "t$it.mp3" }
        val tags = FakeTags()
        val r = rig(files, tags)
        r.repo.importFolder(tree)
        advanceUntilIdle()
        assertEquals(30, tags.reads)
        assertTrue("max in flight ${tags.maxInFlight}", tags.maxInFlight in 1..LibraryRepository.ENRICH_PARALLEL)
    }

    @Test fun removalDuringBackgroundPassDoesNotResurrectRows() = runTest {
        val tags = FakeTags(mapOf("content://tree/a.mp3" to TrackTags(title = "X")))
        val r = rig(tags = tags)
        r.repo.importFolder(tree)
        r.repo.removeFolder(r.store.folderRows.single().id)
        advanceUntilIdle()
        assertTrue(r.store.rows.isEmpty())
        assertTrue(r.store.folderRows.isEmpty())
    }

    @Test fun removeFolderReleasesGrantAndDeletesRows() = runTest {
        val r = rig()
        r.repo.importFolder(tree)
        advanceUntilIdle()
        r.repo.removeFolder(r.store.folderRows.single().id)
        assertTrue(r.store.rows.isEmpty())
        assertTrue(r.store.folderRows.isEmpty())
        assertEquals(listOf(tree), r.grants.released)
    }

    @Test fun removeUnknownFolderIsNoop() = runTest {
        val r = rig()
        r.repo.importFolder(tree)
        advanceUntilIdle()
        r.repo.removeFolder(999)
        assertEquals(2, r.store.rows.size)
        assertTrue(r.grants.released.isEmpty())
    }

    @Test fun pruneRemovesFoldersWithLostGrantsOnly() = runTest {
        val r = rig()
        val other = "content://tree/other"
        r.repo.importFolder(tree)
        r.trees.tree = FakeTree(listOf("z.mp3"), name = "Other")
        r.repo.importFolder(other)
        advanceUntilIdle()
        r.grants.live.remove(tree)
        val removed = r.repo.pruneLostGrants()
        assertEquals(1, removed)
        assertEquals(listOf(other), r.store.folderRows.map { it.treeUri })
        assertTrue(r.store.rows.values.all { it.safFolderId == r.store.folderRows.single().id })
        assertFalse(r.store.rows.isEmpty())
    }

    @Test fun pruneWithAllGrantsLiveKeepsEverything() = runTest {
        val r = rig()
        r.repo.importFolder(tree)
        advanceUntilIdle()
        assertEquals(0, r.repo.pruneLostGrants())
        assertEquals(2, r.store.rows.size)
    }
}
