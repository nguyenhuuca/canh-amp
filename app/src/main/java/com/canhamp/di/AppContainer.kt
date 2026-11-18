package com.canhamp.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.canhamp.data.db.CanhAmpDatabase
import com.canhamp.data.settings.SettingsRepository

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Manual dependency container; services are added as features land. */
class AppContainer(val context: Context) {

    private val appContext: Context = context.applicationContext

    val database: CanhAmpDatabase by lazy {
        Room.databaseBuilder(appContext, CanhAmpDatabase::class.java, CanhAmpDatabase.NAME).build()
    }

    val trackDao by lazy { database.trackDao() }
    val safFolderDao by lazy { database.safFolderDao() }
    val queueDao by lazy { database.queueDao() }
    val eqPresetDao by lazy { database.eqPresetDao() }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.settingsStore) }
}
