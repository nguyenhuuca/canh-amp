package com.canhamp.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.canhamp.CanhAmpApp
import com.canhamp.MainActivity
import com.canhamp.data.queue.QueueRepository
import com.canhamp.data.settings.Settings
import com.canhamp.playback.dsp.DspConfig
import com.canhamp.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** Owns the single ExoPlayer and its MediaSession; media buttons and the notification come from Media3. */
@androidx.annotation.OptIn(UnstableApi::class)
class CanhAmpPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val errorPolicy = PlaybackErrorPolicy()
    private var queueRepo: QueueRepository? = null
    private var positionJob: Job? = null

    /** Set when onTaskRemoved ended the service with a save, so onDestroy does not block the main thread again. */
    private var savedOnTaskRemoved = false

    /** Mirroring stays off until the saved queue is loaded, so an empty start cannot wipe it (B57). */
    private var restored = false

    override fun onCreate() {
        super.onCreate()
        val container = (application as CanhAmpApp).container
        val logger = container.logger
        val exo = PlayerFactory.create(this, container.dspEngine, container.spectrumTap)
        val queue = container.queueRepository
        queueRepo = queue
        exo.addListener(PlayerEvents(exo, errorPolicy, logger))
        player = exo

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, exo)
            .setSessionActivity(openApp)
            .setCallback(SessionCallback())
            .build()

        scope.launch {
            val settings = container.settingsRepository.settings.first()
            applySettings(exo, settings)
            container.dspEngine.update { applyDsp(settings) }
            restoreQueue(exo, queue)
        }
    }

    /** The session is exported, so other apps can reach it: keep them out and keep remote uris out of the queue. */
    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult =
            if (SessionPolicy.isAllowedController(controller.isTrusted, controller.packageName, packageName)) {
                MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
            } else {
                MediaSession.ConnectionResult.reject()
            }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val kept = mediaItems.mapNotNull { item ->
                val uri = item.localConfiguration?.uri ?: item.requestMetadata.mediaUri
                when {
                    uri == null || !SessionPolicy.isAllowedMediaUri(uri.toString()) -> null
                    item.localConfiguration == null -> item.buildUpon().setUri(uri).build()
                    else -> item
                }
            }.toMutableList()
            return Futures.immediateFuture(kept)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** B57: put the saved queue back, paused at the saved position, unless something already loaded one. */
    private suspend fun restoreQueue(exo: ExoPlayer, queue: QueueRepository) {
        try {
            if (exo.mediaItemCount == 0) {
                val snap = queue.load()
                if (snap.items.isNotEmpty() && exo.mediaItemCount == 0) {
                    exo.setMediaItems(snap.items.map(QueueEntry::toMediaItem), snap.index, snap.positionMs)
                    exo.playWhenReady = false
                    exo.prepare()
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            (application as CanhAmpApp).container.logger.w(TAG, "queue restore failed: ${e.javaClass.simpleName}")
        } finally {
            // A failed restore must not leave mirroring off for good; mirror what is loaded, restored or not.
            restored = true
            if (exo.mediaItemCount > 0) queue.mirror(currentEntries(exo))
        }
    }

    private fun currentEntries(exo: ExoPlayer): List<QueueEntry> =
        (0 until exo.mediaItemCount).mapNotNull { exo.getMediaItemAt(it).toQueueEntry() }

    private fun savePosition(exo: ExoPlayer) {
        if (!restored || exo.mediaItemCount == 0) return
        val index = exo.currentMediaItemIndex
        val pos = exo.currentPosition.coerceAtLeast(0L)
        scope.launch { queueRepo?.savePosition(index, pos) }
    }

    /** B56: runs on the main thread as the service goes away, so it waits briefly for the writes. */
    private fun saveBlocking() {
        val exo = player ?: return
        val repo = queueRepo ?: return
        if (!restored || exo.mediaItemCount == 0) return
        val entries = currentEntries(exo)
        val index = exo.currentMediaItemIndex
        val pos = exo.currentPosition.coerceAtLeast(0L)
        runBlocking {
            withTimeoutOrNull(SAVE_TIMEOUT_MS) {
                repo.flush(entries)
                repo.savePosition(index, pos)
            }
        }
    }

    private fun updatePositionJob(exo: ExoPlayer, playing: Boolean) {
        positionJob?.cancel()
        positionJob = if (playing) {
            scope.launch {
                while (true) {
                    delay(POSITION_SAVE_MS)
                    savePosition(exo)
                }
            }
        } else {
            null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveBlocking()
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 || p.playbackState == Player.STATE_ENDED) {
            savedOnTaskRemoved = true
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (!savedOnTaskRemoved) saveBlocking()
        scope.cancel()
        session?.release()
        session = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun applySettings(exo: ExoPlayer, s: Settings) {
        exo.shuffleModeEnabled = s.shuffle
        exo.repeatMode = NowPlayingMapper.repeatToPlayer(s.repeat)
        exo.volume = s.volume.coerceIn(0f, 1f)
    }

    private fun applyDsp(s: Settings): DspConfig = DspConfig(
        eqEnabled = s.eqEnabled,
        gainsDb = s.eqGains.toList(),
        preampDb = s.eqPreamp,
        bassDb = s.bassDb,
        trebleDb = s.trebleDb,
        balance = s.balance,
    )

    /** B27: skip a failing item, stop after five failures in a row. */
    private inner class PlayerEvents(
        private val exo: ExoPlayer,
        private val policy: PlaybackErrorPolicy,
        private val logger: Logger,
    ) : Player.Listener {

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (restored) queueRepo?.mirror(currentEntries(exo))
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = savePosition(exo)

        override fun onPlayerError(error: PlaybackException) {
            logger.w(TAG, "playback error code=${error.errorCodeName}")
            when (policy.onError(exo.hasNextMediaItem())) {
                PlaybackErrorPolicy.Action.SKIP -> {
                    // A restored queue starts paused (B57); skipping must not start it.
                    val resume = exo.playWhenReady
                    exo.seekToNextMediaItem()
                    exo.prepare()
                    if (resume) exo.play()
                }
                PlaybackErrorPolicy.Action.STOP -> exo.stop()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) policy.onPlaying() else savePosition(exo)
            updatePositionJob(exo, isPlaying)
        }
    }

    private companion object {
        const val TAG = "CanhAmpPlayback"
        const val POSITION_SAVE_MS = 10_000L
        const val SAVE_TIMEOUT_MS = 500L
    }
}
