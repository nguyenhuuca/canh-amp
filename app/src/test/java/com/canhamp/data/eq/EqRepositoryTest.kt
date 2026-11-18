package com.canhamp.data.eq

import com.canhamp.data.db.EqPresetDao
import com.canhamp.data.db.EqPresetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqRepositoryTest {

    private class FakeDao : EqPresetDao {
        val rows = MutableStateFlow<List<EqPresetEntity>>(emptyList())
        private var nextId = 1L

        override fun observe() = rows

        override suspend fun insert(preset: EqPresetEntity): Long {
            val id = if (preset.id == 0L) nextId++ else preset.id
            // REPLACE semantics: drop a row with the same id or the same name (NOCASE).
            rows.value = rows.value.filter { it.id != id && !it.name.equals(preset.name, ignoreCase = true) } +
                preset.copy(id = id)
            return id
        }

        override suspend fun findByName(name: String) = rows.value.firstOrNull { it.name.equals(name, ignoreCase = true) }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filter { it.id != id }
        }
    }

    private val gains = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    @Test
    fun saveStoresUserPresetAfterBuiltIns() = runTest {
        val dao = FakeDao()
        val repo = EqRepository(dao) { 100L }
        assertEquals(SaveResult.Saved("Mine"), repo.savePreset("  Mine ", gains, 2, overwrite = false))
        val list = repo.presets().first()
        assertEquals(BuiltInPresets.all.size + 1, list.size)
        val user = list.last()
        assertTrue(user.user)
        assertEquals("Mine", user.name)
        assertEquals(gains, user.gains)
        assertEquals(2, user.preamp)
        assertTrue(list.take(BuiltInPresets.all.size).none { it.user })
    }

    @Test
    fun duplicateNeedsOverwrite() = runTest {
        val dao = FakeDao()
        val repo = EqRepository(dao)
        repo.savePreset("Mine", gains, 0, overwrite = false)
        assertEquals(SaveResult.DuplicateName("Mine"), repo.savePreset("mine", List(10) { 0 }, 0, overwrite = false))
        // Unchanged after the refused save.
        assertEquals(gains, dao.rows.value.single().gainsCsv.split(",").map { it.toInt() })
    }

    @Test
    fun overwriteReplacesKeepingOneRow() = runTest {
        val dao = FakeDao()
        val repo = EqRepository(dao)
        repo.savePreset("Mine", gains, 0, overwrite = false)
        val id = dao.rows.value.single().id
        assertEquals(SaveResult.Saved("MINE"), repo.savePreset("MINE", List(10) { 0 }, -3, overwrite = true))
        val row = dao.rows.value.single()
        assertEquals(id, row.id)
        assertEquals(-3, row.preampDb)
        assertEquals("0,0,0,0,0,0,0,0,0,0", row.gainsCsv)
    }

    @Test
    fun invalidAndBuiltInNamesAreRejectedWithoutWriting() = runTest {
        val dao = FakeDao()
        val repo = EqRepository(dao)
        assertEquals(SaveResult.InvalidName, repo.savePreset("", gains, 0, overwrite = true))
        assertEquals(SaveResult.InvalidName, repo.savePreset("bad!", gains, 0, overwrite = false))
        assertEquals(SaveResult.BuiltInName, repo.savePreset("rock", gains, 0, overwrite = true))
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun deleteRemovesUserPresetOnly() = runTest {
        val dao = FakeDao()
        val repo = EqRepository(dao)
        repo.savePreset("Mine", gains, 0, overwrite = false)
        repo.deletePreset(BuiltInPresets.FLAT.id)
        assertEquals(1, dao.rows.value.size)
        repo.deletePreset(dao.rows.value.single().id)
        assertTrue(dao.rows.value.isEmpty())
        assertNull(dao.findByName("Mine"))
        assertEquals(BuiltInPresets.all, repo.presets().first())
    }

    @Test
    fun corruptStoredGainsFallBackToFlat() = runTest {
        val dao = FakeDao()
        dao.rows.value = listOf(EqPresetEntity(5, "Bad", "x,y", 99, 0))
        val p = EqRepository(dao).presets().first().last()
        assertEquals(List(10) { 0 }, p.gains)
        assertEquals(12, p.preamp)
    }
}
