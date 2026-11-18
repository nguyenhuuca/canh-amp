package com.canhamp.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDaoTest {
    private lateinit var db: CanhAmpDatabase

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, CanhAmpDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    private fun track(uri: String, source: String = "MEDIASTORE", gen: Long = 1, title: String = "t") = TrackEntity(
        uri = uri, source = source, folderPath = "/Music", fileName = "$title.mp3", title = title,
        artist = "a", album = "b", format = "MP3", durationMs = 1000, sizeBytes = 10,
        dateAddedSec = 1, dateModifiedSec = 1, scanGeneration = gen,
    )

    @Test fun upsertByUriKeepsIdAndUpdatesFields() = runBlocking {
        val dao = db.trackDao()
        dao.upsertByUri(listOf(track("content://1", title = "old")))
        val id = dao.findByUri("content://1")!!.id
        dao.upsertByUri(listOf(track("content://1", title = "new", gen = 2), track("content://2")))
        val row = dao.findByUri("content://1")!!
        assertEquals(id, row.id)
        assertEquals("new", row.title)
        assertEquals(2L, row.scanGeneration)
        assertNotNull(dao.findByUri("content://2"))
    }

    @Test fun deleteStaleOnlyOlderGenerationOfGivenSource() = runBlocking {
        val dao = db.trackDao()
        dao.upsertByUri(
            listOf(
                track("m-old", "MEDIASTORE", 1), track("m-new", "MEDIASTORE", 2),
                track("s-old", "SAF", 1),
            ),
        )
        assertEquals(1, dao.deleteStale("MEDIASTORE", 2))
        assertNull(dao.findByUri("m-old"))
        assertNotNull(dao.findByUri("m-new"))
        assertNotNull(dao.findByUri("s-old"))
    }
}
