package com.canhamp.data.eq

import com.canhamp.data.db.EqPresetDao
import com.canhamp.data.db.EqPresetEntity
import com.canhamp.data.settings.SettingsMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface SaveResult {
    data class Saved(val name: String) : SaveResult
    data object InvalidName : SaveResult
    data object BuiltInName : SaveResult

    /** A user preset with this name exists and overwrite was not requested. */
    data class DuplicateName(val name: String) : SaveResult
}

/** Built-in presets followed by the user presets stored in Room (B46, B48). */
class EqRepository(
    private val dao: EqPresetDao,
    private val nowSec: () -> Long = { System.currentTimeMillis() / MILLIS },
) {
    fun presets(): Flow<List<EqPreset>> = dao.observe().map { rows ->
        BuiltInPresets.all + rows.map { it.toPreset() }
    }

    suspend fun savePreset(name: String, gains: List<Int>, preamp: Int, overwrite: Boolean): SaveResult {
        val clean = when (val check = PresetNameValidator.check(name)) {
            NameCheck.InvalidName -> return SaveResult.InvalidName
            NameCheck.BuiltInName -> return SaveResult.BuiltInName
            is NameCheck.Valid -> check.name
        }
        val existing = dao.findByName(clean)
        if (existing != null && !overwrite) return SaveResult.DuplicateName(existing.name)
        dao.insert(
            EqPresetEntity(
                id = existing?.id ?: 0,
                name = clean,
                gainsCsv = SettingsMapper.formatGains(gains.toIntArray()),
                preampDb = preamp.coerceIn(SettingsMapper.GAIN_MIN, SettingsMapper.GAIN_MAX),
                createdAtSec = existing?.createdAtSec ?: nowSec(),
            ),
        )
        return SaveResult.Saved(clean)
    }

    suspend fun deletePreset(id: Long) {
        if (id > 0) dao.delete(id)
    }

    private fun EqPresetEntity.toPreset() = EqPreset(
        id = id,
        name = name,
        tag = "",
        gains = SettingsMapper.parseGains(gainsCsv).toList(),
        preamp = preampDb.coerceIn(SettingsMapper.GAIN_MIN, SettingsMapper.GAIN_MAX),
        user = true,
    )

    private companion object {
        const val MILLIS = 1000L
    }
}
