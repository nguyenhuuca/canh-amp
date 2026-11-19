package com.canhamp.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.canhamp.data.queue.QueueRepository
import com.canhamp.data.settings.RepeatSetting
import com.canhamp.data.settings.SettingsRepository
import com.canhamp.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Notices from the player layer that the UI shows as a toast. */
enum class PlayerMessage { QUEUE_FULL }

/**
 * UI-side wrapper over a [MediaController] bound to [CanhAmpPlaybackService]. Same process, so the
 * controller lives as long as the app. Commands issued before the connection is up wait for it.
 */
class PlayerConnection(
    private val context: Context,
    private val settings: SettingsRepository,
    private val logger: Logger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val recorder: SampleRateRecorder? = null,
) {
    private val _nowPlaying = MutableStateFlow(NowPlaying.EMPTY)
    private val _connected = MutableStateFlow(false)
    private var controller: MediaController? = null
    private var connecting: Job? = null
    private var ticker: Job? = null

    private val _errors = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _messages = MutableSharedFlow<PlayerMessage>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())

    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying

    /** The player's playlist in play order; follows the timeline, and edits made here show at once. */
    val queue: StateFlow<List<QueueEntry>> = _queue

    /** One-off notices for the UI, such as the queue cap (B55). */
    val messages: SharedFlow<PlayerMessage> = _messages

    /** One event per playback failure (B27); the UI shows the error text for 3 s. */
    val errors: SharedFlow<Unit> = _errors
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
        val future = MediaController.Builder(context, token).setListener(disconnectListener).buildAsync()
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

    private val disconnectListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) = handleDisconnect(controller)
    }

    /** The session went away (service killed, session released): drop the dead controller and rebuild with backoff. */
    private fun handleDisconnect(dead: MediaController) {
        if (controller !== dead) return
        logger.w(TAG, "controller disconnected")
        ticker?.cancel()
        ticker = null
        controller = null
        _connected.value = false
        dead.release()
        connect()
    }

    private fun attach(c: MediaController) {
        controller = c
        c.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                        refreshQueue(c)
                    }
                    refresh(c)
                    updateTicker(c)
                }

                override fun onPlayerError(error: PlaybackException) {
                    _errors.tryEmit(Unit)
                }
            },
        )
        _connected.value = true
        refreshQueue(c)
        refresh(c)
        updateTicker(c)
    }

    private fun refreshQueue(c: MediaController) {
        _queue.value = List(c.mediaItemCount) { c.getMediaItemAt(it).toQueueEntry() ?: UNKNOWN_ENTRY }
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
        val trackId = extras?.takeIf { it.containsKey(EXTRA_TRACK_ID) }?.getLong(EXTRA_TRACK_ID)
        val sampleRate = audio?.let { NowPlayingMapper.sampleRateHz(it.sampleRate) }
        if (trackId != null && sampleRate != null) recorder?.record(trackId, sampleRate)
        _nowPlaying.value = NowPlaying(
            trackId = trackId,
            uri = item?.localConfiguration?.uri?.toString(),
            title = meta?.title?.toString().orEmpty(),
            artist = meta?.artist?.toString().orEmpty(),
            album = meta?.albumTitle?.toString().orEmpty(),
            isPlaying = c.isPlaying,
            buffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: extraDuration,
            bitrateKbps = audio?.let { NowPlayingMapper.bitrateKbps(it.bitrate) } ?: extraKbps,
            sampleRateHz = sampleRate,
            channelCount = audio?.let { NowPlayingMapper.channelCount(it.channelCount) },
            index = c.currentMediaItemIndex,
            queueSize = c.mediaItemCount,
            shuffle = c.shuffleModeEnabled,
            repeat = NowPlayingMapper.repeatFromPlayer(c.repeatMode),
            volume = c.volume,
            artworkData = c.mediaMetadata.artworkData,
            mimeType = audio?.sampleMimeType,
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
        val kept = items.take(QueueRepository.MAX_ITEMS)
        if (kept.size < items.size) _messages.tryEmit(PlayerMessage.QUEUE_FULL)
        c.setMediaItems(kept.map(QueueEntry::toMediaItem), startIndex.coerceIn(0, kept.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun addToQueue(items: List<QueueEntry>) = withController { c ->
        if (items.isEmpty()) return@withController
        val kept = admit(c, items) ?: return@withController
        val wasEmpty = c.mediaItemCount == 0
        c.addMediaItems(kept.map(QueueEntry::toMediaItem))
        if (wasEmpty || c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    /** B11: inserts at currentIndex + 1. */
    fun playNext(items: List<QueueEntry>) = withController { c ->
        if (items.isEmpty()) return@withController
        val kept = admit(c, items) ?: return@withController
        val wasEmpty = c.mediaItemCount == 0
        val at = if (wasEmpty) 0 else c.currentMediaItemIndex + 1
        c.addMediaItems(at, kept.map(QueueEntry::toMediaItem))
        if (wasEmpty || c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    /** Entries that fit under the 5,000 cap, or null when none do; emits QUEUE_FULL when any are dropped (B55). */
    private fun admit(c: MediaController, items: List<QueueEntry>): List<QueueEntry>? {
        val room = QueueRepository.room(c.mediaItemCount, items.size)
        if (room < items.size) _messages.tryEmit(PlayerMessage.QUEUE_FULL)
        return if (room == 0) null else items.take(room)
    }

    fun move(from: Int, to: Int) = withController { c ->
        val last = c.mediaItemCount - 1
        if (from in 0..last && to in 0..last && from != to) {
            c.moveMediaItem(from, to)
            _queue.update { list ->
                if (from in list.indices && to in list.indices) list.toMutableList().apply { add(to, removeAt(from)) } else list
            }
        }
    }

    fun remove(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.removeMediaItem(index)
            _queue.update { list -> if (index in list.indices) list.toMutableList().apply { removeAt(index) } else list }
        }
    }

    /** B60 undo: puts [entry] back at [index] (clamped to the end). */
    fun insert(index: Int, entry: QueueEntry) = withController { c ->
        if (c.mediaItemCount >= QueueRepository.MAX_ITEMS) return@withController
        val at = index.coerceIn(0, c.mediaItemCount)
        c.addMediaItem(at, entry.toMediaItem())
        _queue.update { list -> list.toMutableList().apply { add(at.coerceAtMost(size), entry) } }
    }

    /** Plays the item at [index] from its start. */
    fun playAt(index: Int) = withController { c ->
        if (index !in 0 until c.mediaItemCount) return@withController
        c.seekTo(index, 0L)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    fun clearQueue() = withController { c ->
        c.clearMediaItems()
        c.stop()
        _queue.value = emptyList()
    }

    /**
     * B62: puts the list in [newOrder] without interrupting the item that plays. [playingNewIndex] is where that
     * item sits in [newOrder]; the items around it are swapped out and the playing one is left alone.
     * Ignored when the list changed size since [newOrder] was made.
     */
    fun replaceQueueOrder(newOrder: List<QueueEntry>, playingNewIndex: Int) = withController { c ->
        val count = c.mediaItemCount
        if (count == 0 || newOrder.size != count || playingNewIndex !in 0 until count) return@withController
        val current = c.currentMediaItemIndex
        if (current < 0 || current >= count) return@withController
        if (current + 1 < count) c.removeMediaItems(current + 1, count)
        if (current > 0) c.removeMediaItems(0, current)
        if (playingNewIndex > 0) c.addMediaItems(0, newOrder.subList(0, playingNewIndex).map(QueueEntry::toMediaItem))
        if (playingNewIndex + 1 < count) {
            c.addMediaItems(newOrder.subList(playingNewIndex + 1, count).map(QueueEntry::toMediaItem))
        }
        _queue.value = newOrder
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
        val UNKNOWN_ENTRY = QueueEntry(trackId = -1L, uri = "", title = "", artist = "", album = "", durationMs = 0L, bitrateKbps = null)
    }
}
