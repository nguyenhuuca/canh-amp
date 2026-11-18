package com.canhamp.di

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.canhamp.data.db.CanhAmpDatabase
import com.canhamp.data.library.LibraryRepository
import com.canhamp.data.library.MediaStoreScanner
import com.canhamp.data.library.TrackDaoStore
import com.canhamp.data.settings.SettingsRepository
import com.canhamp.util.AndroidLogger
import com.canhamp.util.Logger

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

    val logger: Logger by lazy { AndroidLogger() }

    val libraryRepository: LibraryRepository by lazy {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        LibraryRepository(
            source = MediaStoreScanner(appContext.contentResolver),
            tracks = TrackDaoStore(trackDao),
            store = settingsRepository,
            hasPermission = {
                ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
            },
            logger = logger,
        )
    }
}
