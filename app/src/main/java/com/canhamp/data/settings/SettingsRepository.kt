package com.canhamp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.canhamp.ui.theme.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<Settings> = store.data.map { prefs ->
        SettingsMapper.fromPreferences(prefs.asMap().mapKeys { it.key.name })
    }

    suspend fun setTheme(theme: ThemeId) = put { it[THEME] = theme.name }
    suspend fun setShuffle(on: Boolean) = put { it[SHUFFLE] = on }
    suspend fun setRepeat(mode: RepeatSetting) = put { it[REPEAT] = mode.name }
    suspend fun setVolume(v: Float) = put { it[VOLUME] = if (v.isNaN()) 0f else v.coerceIn(0f, 1f) }
    suspend fun setBalance(b: Float) = put { it[BALANCE] = if (b.isNaN()) 0f else b.coerceIn(-1f, 1f) }

    suspend fun setEq(enabled: Boolean, gains: IntArray, preamp: Int, presetName: String) = put {
        it[EQ_ENABLED] = enabled
        it[EQ_GAINS] = SettingsMapper.formatGains(gains)
        it[EQ_PREAMP] = preamp.coerceIn(SettingsMapper.GAIN_MIN, SettingsMapper.GAIN_MAX)
        it[EQ_PRESET_NAME] = presetName
    }

    suspend fun setBassTreble(bassDb: Int, trebleDb: Int) = put {
        it[BASS] = bassDb.coerceIn(0, SettingsMapper.GAIN_MAX)
        it[TREBLE] = trebleDb.coerceIn(0, SettingsMapper.GAIN_MAX)
    }

    suspend fun setTimeMode(mode: TimeMode) = put { it[TIME_MODE] = mode.name }
    suspend fun setVisMode(mode: VisMode) = put { it[VIS_MODE] = mode.name }
    suspend fun setVisAnimated(on: Boolean) = put { it[VIS_ANIMATED] = on }

    suspend fun setQueuePosition(index: Int, ms: Long) = put {
        it[QUEUE_INDEX] = index.coerceAtLeast(0)
        it[QUEUE_POSITION_MS] = ms.coerceAtLeast(0L)
    }

    suspend fun setScanGeneration(generation: Long) = put { it[SCAN_GENERATION] = generation }

    private suspend fun put(block: (MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    private companion object {
        val THEME = stringPreferencesKey(SettingsMapper.THEME)
        val SHUFFLE = booleanPreferencesKey(SettingsMapper.SHUFFLE)
        val REPEAT = stringPreferencesKey(SettingsMapper.REPEAT)
        val VOLUME = floatPreferencesKey(SettingsMapper.VOLUME)
        val BALANCE = floatPreferencesKey(SettingsMapper.BALANCE)
        val EQ_ENABLED = booleanPreferencesKey(SettingsMapper.EQ_ENABLED)
        val EQ_GAINS = stringPreferencesKey(SettingsMapper.EQ_GAINS)
        val EQ_PREAMP = intPreferencesKey(SettingsMapper.EQ_PREAMP)
        val EQ_PRESET_NAME = stringPreferencesKey(SettingsMapper.EQ_PRESET_NAME)
        val BASS = intPreferencesKey(SettingsMapper.BASS_DB)
        val TREBLE = intPreferencesKey(SettingsMapper.TREBLE_DB)
        val TIME_MODE = stringPreferencesKey(SettingsMapper.TIME_MODE)
        val VIS_MODE = stringPreferencesKey(SettingsMapper.VIS_MODE)
        val VIS_ANIMATED = booleanPreferencesKey(SettingsMapper.VIS_ANIMATED)
        val QUEUE_INDEX = intPreferencesKey(SettingsMapper.QUEUE_INDEX)
        val QUEUE_POSITION_MS = longPreferencesKey(SettingsMapper.QUEUE_POSITION_MS)
        val SCAN_GENERATION = longPreferencesKey(SettingsMapper.SCAN_GENERATION)
    }
}
