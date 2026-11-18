package com.canhamp.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.canhamp.data.settings.RepeatSetting
import com.canhamp.data.settings.SettingsRepository
import com.canhamp.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * UI-side wrapper over a [MediaController] bound to [CanhAmpPlaybackService]. Same process, so the
 * controller lives as long as the app. Commands issued before the connection is up wait for it.
 */
class PlayerConnection(
    private val context: Context,
    private val settings: SettingsRepository,
    private val logger: Logger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val _nowPlaying = MutableStateFlow(NowPlaying.EMPTY)
    private val _connected = MutableStateFlow(false)
    private var controller: MediaController? = null
    private var connecting: Job? = null
    private var ticker: Job? = null

    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying
    val connected: StateFlow<Boolean> = _connected

    /** Idempotent. Retries with a growing delay until the session answers. */
    fun connect() {
        if (controller != null || connecting?.isActive == true) return
        connecting = scope.launch {
            var wait = RETRY_START_MS
            while (controller == null) {
                val c = buildController()
                if (c != null) {
                    attach(c)
                } else {
                    delay(wait)
                    wait = (wait * 2).coerceAtMost(RETRY_MAX_MS)
                }
            }
        }
    }

    private suspend fun buildController(): MediaController? {
        val token = SessionToken(context, ComponentName(context, CanhAmpPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        return suspendCancellableCoroutine { cont ->
            future.addListener(
                {
                    val result = try {
                        future.get()
                    } catch (e: Exception) {
                        logger.w(TAG, "controller connect failed: ${e.javaClass.simpleName}")
                        null
                    }
                    if (cont.isActive) cont.resume(result)
                },
                ContextCompat.getMainExecutor(context),
            )
            cont.invokeOnCancellation { future.cancel(false) }
        }
    }

    private fun attach(c: MediaController) {
        controller = c
        c.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    refresh(c)
                    updateTicker(c)
                }
            },
        )
        _connected.value = true
        refresh(c)
        updateTicker(c)
    }

    private fun updateTicker(c: MediaController) {
        if (c.isPlaying) {
            if (ticker?.isActive == true) return
            ticker = scope.launch {
                while (true) {
                    delay(TICK_MS)
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    _nowPlaying.update { it.copy(positionMs = pos) }
                }
            }
        } else {
            ticker?.cancel()
            ticker = null
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun refresh(c: MediaController) {
        val item = c.currentMediaItem
        val meta = item?.mediaMetadata
        val extras = meta?.extras
        val audio = c.currentTracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.let { g -> (0 until g.length).firstOrNull { g.isTrackSelected(it) }?.let(g::getTrackFormat) }
        val extraKbps = extras?.getInt(EXTRA_BITRATE_KBPS, 0)?.takeIf { it > 0 }
        val extraDuration = extras?.getLong(EXTRA_DURATION_MS, 0L) ?: 0L
        _nowPlaying.value = NowPlaying(
            trackId = extras?.takeIf { it.containsKey(EXTRA_TRACK_ID) }?.getLong(EXTRA_TRACK_ID),
            uri = item?.localConfiguration?.uri?.toString(),
            title = meta?.title?.toString().orEmpty(),
            artist = meta?.artist?.toString().orEmpty(),
            album = meta?.albumTitle?.toString().orEmpty(),
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: extraDuration,
            bitrateKbps = audio?.let { NowPlayingMapper.bitrateKbps(it.bitrate) } ?: extraKbps,
            sampleRateHz = audio?.let { NowPlayingMapper.sampleRateHz(it.sampleRate) },
            channelCount = audio?.let { NowPlayingMapper.channelCount(it.channelCount) },
            index = c.currentMediaItemIndex,
            queueSize = c.mediaItemCount,
            shuffle = c.shuffleModeEnabled,
            repeat = NowPlayingMapper.repeatFromPlayer(c.repeatMode),
            volume = c.volume,
        )
    }

    /** Runs [block] now, or as soon as the controller is connected. */
    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            block(c)
            return
        }
        connect()
        scope.launch {
            _connected.first { it }
            controller?.let(block)
        }
    }

    fun play() = withController { it.play() }

    fun pause() = withController { it.pause() }

    fun togglePlayPause() = withController { if (it.isPlaying) it.pause() else it.play() }

    /** B22: stop = pause + seek to 0, queue kept. */
    fun stop() = withController {
        it.pause()
        it.seekTo(0)
    }

    fun next() = withController { it.seekToNextMediaItem() }

    /** B22: restart after 3 s, otherwise the previous item (or a restart on the first item). */
    fun previous() = withController {
        val restart = NowPlayingMapper.previousAction(it.currentPosition) == NowPlayingMapper.PreviousAction.RESTART
        if (restart || !it.hasPreviousMediaItem()) it.seekTo(0) else it.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }

    fun setQueue(items: List<QueueEntry>, startIndex: Int) = withController { c ->
        if (items.isEmpty()) return@withController
        c.setMediaItems(items.map(QueueEntry::toMediaItem), startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun addToQueue(items: List<QueueEntry>) = withController { c ->
        if (items.isEmpty()) return@withController
        val wasEmpty = c.mediaItemCount == 0
        c.addMediaItems(items.map(QueueEntry::toMediaItem))
        if (wasEmpty || c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    /** B11: inserts at currentIndex + 1. */
    fun playNext(items: List<QueueEntry>) = withController { c ->
        if (items.isEmpty()) return@withController
        val wasEmpty = c.mediaItemCount == 0
        val at = if (wasEmpty) 0 else c.currentMediaItemIndex + 1
        c.addMediaItems(at, items.map(QueueEntry::toMediaItem))
        if (wasEmpty || c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    fun move(from: Int, to: Int) = withController { c ->
        val last = c.mediaItemCount - 1
        if (from in 0..last && to in 0..last && from != to) c.moveMediaItem(from, to)
    }

    fun remove(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun setShuffle(on: Boolean) {
        withController { it.shuffleModeEnabled = on }
        scope.launch { settings.setShuffle(on) }
    }

    fun cycleRepeat() = setRepeat(NowPlayingMapper.nextRepeat(_nowPlaying.value.repeat))

    fun setRepeat(mode: RepeatSetting) {
        withController { it.repeatMode = NowPlayingMapper.repeatToPlayer(mode) }
        scope.launch { settings.setRepeat(mode) }
    }

    fun setVolume(volume: Float) {
        val v = if (volume.isNaN()) 0f else volume.coerceIn(0f, 1f)
        withController { it.volume = v }
        scope.launch { settings.setVolume(v) }
    }

    private companion object {
        const val TAG = "PlayerConnection"
        const val TICK_MS = 250L
        const val RETRY_START_MS = 500L
        const val RETRY_MAX_MS = 8_000L
    }
}
