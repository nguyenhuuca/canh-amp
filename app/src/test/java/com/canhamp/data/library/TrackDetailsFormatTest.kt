package com.canhamp.data.library

import com.canhamp.data.db.TrackEntity
import com.canhamp.util.NoopLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class TrackDetailsFormatTest {

    @Test
    fun capTagTrimsCapsAndStripsControlChars() {
        assertEquals("abc", TrackDetailsFormat.capTag("  ab\u0000c \n"))
        assertEquals(256, TrackDetailsFormat.capTag("x".repeat(1000))?.length)
        assertNull(TrackDetailsFormat.capTag("   "))
        assertNull(TrackDetailsFormat.capTag(null))
    }

    @Test
    fun unknownValuesShowDash() {
        assertEquals("—", TrackDetailsFormat.text(null))
        assertEquals("—", TrackDetailsFormat.bitrate(null))
        assertEquals("—", TrackDetailsFormat.sampleRate(0))
        assertEquals("—", TrackDetailsFormat.size(0))
        assertEquals("—", TrackDetailsFormat.modified(0))
    }

    @Test
    fun formatsNumbers() {
        assertEquals("320 kbps", TrackDetailsFormat.bitrate(320))
        assertEquals("44.1 kHz", TrackDetailsFormat.sampleRate(44_100))
        assertEquals("512 B", TrackDetailsFormat.size(512))
        assertEquals("1.5 KB", TrackDetailsFormat.size(1536))
        assertEquals("4.0 MB", TrackDetailsFormat.size(4L * 1024 * 1024))
        assertEquals("1.00 GB", TrackDetailsFormat.size(1024L * 1024 * 1024))
        assertEquals("1970-01-02 00:00", TrackDetailsFormat.modified(86_400, TimeZone.getTimeZone("UTC")))
    }

    private fun row(sampleRate: Int?) = TrackEntity(
        id = 7, uri = "content://x/7", source = "MEDIASTORE", folderPath = "/m", fileName = "a.mp3",
        title = "T", artist = "A", album = "B", format = "mp3", durationMs = 1000, sampleRateHz = sampleRate,
        sizeBytes = 10, dateAddedSec = 1, dateModifiedSec = 2, scanGeneration = 1,
    )

    private class Lookup(var row: TrackEntity?) : TrackLookup {
        val writes = mutableListOf<Pair<Long, Int>>()
        override suspend fun findById(id: Long) = row?.takeIf { it.id == id }
        override suspend fun updateSampleRate(id: Long, hz: Int) {
            writes += id to hz
        }
    }

    private fun tags(t: TrackTags?) = object : TagSource {
        override suspend fun read(uri: String) = t
    }

    private fun repo(lookup: TrackLookup, tags: TagSource?) = LibraryRepository(
        source = object : AudioSource {
            override suspend fun scan(onChunk: suspend (List<ScannedRow>) -> Unit) = Unit
        },
        tracks = object : TrackStore {
            override suspend fun upsert(tracks: List<TrackEntity>) = 0
            override suspend fun deleteStale(source: com.canhamp.data.db.TrackSource, generation: Long) = 0
            override suspend fun count() = 0
        },
        store = object : ScanGenerationStore {
            override suspend fun scanGeneration() = 0L
            override suspend fun setScanGeneration(generation: Long) = Unit
            override suspend fun lastScanAtMs() = 0L
            override suspend fun setLastScanAtMs(ms: Long) = Unit
        },
        hasPermission = { true },
        logger = NoopLogger,
        lookup = lookup,
        tags = tags,
    )

    @Test
    fun detailsMergeTagsAndWriteSampleRateBack() = runTest {
        val lookup = Lookup(row(null))
        val tags = tags(TrackTags(year = "1999", sampleRateHz = 48_000, bitrateKbps = 192))
        val r = repo(lookup, tags).details(7) as DetailsResult.Found
        assertEquals("1999", r.details.year)
        assertEquals(48_000, r.details.sampleRateHz)
        assertEquals(listOf(7L to 48_000), lookup.writes)
    }

    @Test
    fun knownSampleRateIsNotRewritten() = runTest {
        val lookup = Lookup(row(44_100))
        repo(lookup, tags(TrackTags(sampleRateHz = 48_000))).details(7)
        assertTrue(lookup.writes.isEmpty())
    }

    @Test
    fun missingRowAndUnreadableFile() = runTest {
        assertEquals(DetailsResult.NotFound, repo(Lookup(null), tags(null)).details(7))
        val r = repo(Lookup(row(null)), tags(null)).details(7)
        assertTrue(r is DetailsResult.Unreadable)
        assertEquals("T", (r as DetailsResult.Unreadable).details.title)
    }
}
