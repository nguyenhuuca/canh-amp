package com.canhamp.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.canhamp.CanhAmpApp
import com.canhamp.MainActivity
import com.canhamp.data.settings.Settings
import com.canhamp.playback.dsp.DspConfig
import com.canhamp.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Owns the single ExoPlayer and its MediaSession; media buttons and the notification come from Media3. */
@androidx.annotation.OptIn(UnstableApi::class)
class CanhAmpPlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val errorPolicy = PlaybackErrorPolicy()

    override fun onCreate() {
        super.onCreate()
        val container = (application as CanhAmpApp).container
        val logger = container.logger
        val exo = PlayerFactory.create(this, container.dspEngine, container.spectrumTap)
        exo.addListener(PlayerEvents(exo, errorPolicy, logger))
        player = exo

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, exo).setSessionActivity(openApp).build()

        scope.launch {
            val settings = container.settingsRepository.settings.first()
            applySettings(exo, settings)
            container.dspEngine.update { applyDsp(settings) }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 || p.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
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
    private class PlayerEvents(
        private val exo: ExoPlayer,
        private val policy: PlaybackErrorPolicy,
        private val logger: Logger,
    ) : Player.Listener {

        override fun onPlayerError(error: PlaybackException) {
            logger.w(TAG, "playback error code=${error.errorCodeName}")
            when (policy.onError(exo.hasNextMediaItem())) {
                PlaybackErrorPolicy.Action.SKIP -> {
                    exo.seekToNextMediaItem()
                    exo.prepare()
                    exo.play()
                }
                PlaybackErrorPolicy.Action.STOP -> exo.stop()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) policy.onPlaying()
        }
    }

    private companion object {
        const val TAG = "CanhAmpPlayback"
    }
}
