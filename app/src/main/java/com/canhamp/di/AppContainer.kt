package com.canhamp.di

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.canhamp.data.db.CanhAmpDatabase
import com.canhamp.data.eq.EqRepository
import com.canhamp.data.library.AndroidDocumentTrees
import com.canhamp.data.library.ContentResolverUriPermissions
import com.canhamp.data.library.DaoSafStore
import com.canhamp.data.library.LibraryRepository
import com.canhamp.data.library.SafImportDeps
import com.canhamp.data.library.MediaStoreScanner
import com.canhamp.data.library.TrackDaoLookup
import com.canhamp.data.library.TrackDaoStore
import com.canhamp.data.library.TrackMetadataReader
import com.canhamp.data.playlist.ContentResolverStreams
import com.canhamp.data.playlist.PlaylistFileRepository
import com.canhamp.data.playlist.TrackDaoPlaylistIndex
import com.canhamp.data.queue.QueueDaoStore
import com.canhamp.data.queue.QueueRepository
import com.canhamp.data.queue.SettingsPositionStore
import com.canhamp.data.settings.SettingsRepository
import com.canhamp.playback.PlayerConnection
import com.canhamp.playback.SampleRateRecorder
import com.canhamp.playback.dsp.DspEngine
import com.canhamp.playback.dsp.SpectrumTapProcessor
import com.canhamp.util.AndroidLogger
import com.canhamp.util.Logger
import com.canhamp.util.audioPermission

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

    val eqRepository: EqRepository by lazy { EqRepository(eqPresetDao) }

    val logger: Logger by lazy { AndroidLogger() }

    /** One engine and one spectrum tap for the whole process (ADR-0001, ADR-0002). */
    val dspEngine: DspEngine by lazy { DspEngine() }
    @androidx.annotation.OptIn(UnstableApi::class)
    val spectrumTap: SpectrumTapProcessor by lazy { SpectrumTapProcessor(dspEngine.spectrumRing) }

    val sampleRateRecorder: SampleRateRecorder by lazy { SampleRateRecorder(TrackDaoLookup(trackDao), logger) }

    val queueRepository: QueueRepository by lazy {
        QueueRepository(QueueDaoStore(queueDao), SettingsPositionStore(settingsRepository), logger)
    }

    val playerConnection: PlayerConnection by lazy {
        PlayerConnection(appContext, settingsRepository, logger, recorder = sampleRateRecorder)
    }

    val playlistFileRepository: PlaylistFileRepository by lazy {
        PlaylistFileRepository(TrackDaoPlaylistIndex(trackDao), ContentResolverStreams(appContext.contentResolver))
    }

    val libraryRepository: LibraryRepository by lazy {
        val permission = audioPermission()
        LibraryRepository(
            source = MediaStoreScanner(appContext.contentResolver),
            tracks = TrackDaoStore(trackDao),
            store = settingsRepository,
            hasPermission = {
                ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
            },
            logger = logger,
            lookup = TrackDaoLookup(trackDao),
            tags = TrackMetadataReader(appContext),
            saf = SafImportDeps(
                store = DaoSafStore(safFolderDao, trackDao),
                trees = AndroidDocumentTrees(appContext.contentResolver),
                grants = ContentResolverUriPermissions(appContext.contentResolver),
            ),
        )
    }
}
