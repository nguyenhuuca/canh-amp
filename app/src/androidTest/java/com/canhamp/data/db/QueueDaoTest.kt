package com.canhamp.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QueueDaoTest {
    private lateinit var db: CanhAmpDatabase

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, CanhAmpDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    private fun item(pos: Int) = QueueItemEntity(pos, "u$pos", "t$pos", "a", 1000)

    @Test fun replaceAllReplacesPreviousRows() = runBlocking {
        val dao = db.queueDao()
        dao.replaceAll(listOf(item(0), item(1), item(2)))
        dao.replaceAll(listOf(item(0), item(1)))
        assertEquals(listOf("u0", "u1"), dao.getAll().map { it.uri })
    }
}
