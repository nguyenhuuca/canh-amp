package com.canhamp.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.canhamp.CanhAmpApp
import com.canhamp.data.playlist.LoadResult
import com.canhamp.data.playlist.PlaylistFileRepository
import com.canhamp.data.playlist.SaveResult
import com.canhamp.data.settings.RepeatSetting
import com.canhamp.playback.PlayerConnection
import com.canhamp.playback.QueueEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val MAX_FILTER_CHARS = 200
private const val STOP_TIMEOUT_MS = 5_000L

/** What the screen draws. [rows] are the filtered view; [total] counts the whole queue. */
data class PlaylistState(
    val rows: List<QueueRow> = emptyList(),
    val total: Int = 0,
    val stats: QueueStats = QueueStats(0, 0L, null),
    val filter: String = "",
    val selectedIndex: Int? = null,
    val selectedTrackId: Long? = null,
    val sortMode: SortMode? = null,
    val repeat: RepeatSetting = RepeatSetting.OFF,
    val canReorder: Boolean = true,
    val playingTrackId: Long? = null,
)

/** One-shot notices from save and load. */
sealed interface PlaylistEvent {
    data class Loaded(val count: Int, val missing: Int) : PlaylistEvent
    data object Invalid : PlaylistEvent
    data object TooLarge : PlaylistEvent
    data class Saved(val count: Int) : PlaylistEvent
    data object SaveFailed : PlaylistEvent
}

private data class Playhead(val index: Int, val repeat: RepeatSetting)

class PlaylistViewModel(
    private val player: PlayerConnection,
    private val files: PlaylistFileRepository,
    private val folderOf: suspend (Long) -> String?,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val filter = MutableStateFlow("")
    private val selected = MutableStateFlow<Int?>(null)
    private val sortMode = MutableStateFlow<SortMode?>(null)

    private val _undo = MutableStateFlow<RemovedRow?>(null)

    /** The last removed row while its UNDO is on offer. */
    val undo: StateFlow<RemovedRow?> = _undo.asStateFlow()

    private val _events = MutableSharedFlow<PlaylistEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<PlaylistEvent> = _events.asSharedFlow()

    private var undoJob: Job? = null

    private val playhead = player.nowPlaying
        .map { Playhead(if (it.queueSize > 0) it.index else -1, it.repeat) }
        .distinctUntilChanged()

    val state: StateFlow<PlaylistState> = combine(
        player.queue,
        playhead,
        filter,
        selected,
        sortMode,
    ) { queue, head, query, sel, sort ->
        val selection = sel?.takeIf { it in queue.indices }
        val playing = head.index.takeIf { it in queue.indices }
        PlaylistState(
            rows = buildRows(queue, playing, selection, query),
            total = queue.size,
            stats = queueStats(queue, selection),
            filter = query,
            selectedIndex = selection,
            selectedTrackId = selection?.let { queue[it].trackId },
            sortMode = sort,
            repeat = head.repeat,
            canReorder = canReorder(query),
            playingTrackId = playing?.let { queue[it].trackId },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlaylistState())

    /** Folder of the playing track, for the path pill. */
    val playingFolder: StateFlow<String?> = state.map { it.playingTrackId }.distinctUntilChanged()
        .map { id -> if (id != null && id >= 0) folderOf(id) else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private fun queue(): List<QueueEntry> = player.queue.value

    fun setFilter(text: String) {
        filter.value = text.take(MAX_FILTER_CHARS)
    }

    fun clearFilter() {
        filter.value = ""
    }

    fun select(index: Int) {
        if (index in queue().indices) selected.value = index
    }

    fun play(index: Int) {
        if (index !in queue().indices) return
        selected.value = index
        player.playAt(index)
    }

    fun move(from: Int, to: Int) {
        val size = queue().size
        if (from !in 0 until size || to !in 0 until size || from == to) return
        selected.value = selectionAfterMove(selected.value, from, to)
        player.move(from, to)
    }

    fun remove(index: Int) {
        val removed = removedRow(queue(), index) ?: return
        selected.value = selectionAfterRemove(selected.value, index)
        player.remove(index)
        _undo.value = removed
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(UNDO_MS)
            _undo.value = null
        }
    }

    fun removeSelected() {
        selected.value?.let(::remove)
    }

    fun undoRemove() {
        val removed = _undo.value ?: return
        undoJob?.cancel()
        _undo.value = null
        val at = restoreIndex(removed, queue().size)
        player.insert(at, removed.entry)
        selected.value = at
    }

    /** One SORT tap: advances the mode and reorders once (B62). */
    fun sort() {
        val mode = SortMode.after(sortMode.value)
        sortMode.value = mode
        reorder(sortOrder(queue(), mode))
    }

    /** RND: one shuffle of the order, not the shuffle mode. */
    fun randomize() {
        val q = queue()
        if (q.size > 1) reorder(shuffleOrder(q.size, random))
    }

    private fun reorder(order: List<Int>) {
        val q = queue()
        val playing = player.nowPlaying.value.takeIf { it.queueSize == q.size }?.index ?: return
        val newPlaying = newPositionOf(order, playing) ?: return
        selected.value = newPositionOf(order, selected.value)
        player.replaceQueueOrder(q.reordered(order), newPlaying)
    }

    fun cycleRepeat() = player.cycleRepeat()

    fun clearQueue() {
        undoJob?.cancel()
        _undo.value = null
        selected.value = null
        player.clearQueue()
    }

    /** B64. [uri] is the document the user created. */
    fun save(uri: String) {
        val items = queue()
        viewModelScope.launch {
            when (val r = files.save(uri, items)) {
                is SaveResult.Ok -> _events.tryEmit(PlaylistEvent.Saved(r.count))
                SaveResult.Failed -> _events.tryEmit(PlaylistEvent.SaveFailed)
            }
        }
    }

    /** B65. The caller has already confirmed the replace when the queue was not empty. */
    fun load(uri: String, documentId: String?) {
        viewModelScope.launch {
            when (val r = files.load(uri, documentId)) {
                is LoadResult.Ok -> {
                    if (r.entries.isNotEmpty()) {
                        selected.value = null
                        sortMode.value = null
                        player.setQueue(r.entries, 0)
                    }
                    _events.tryEmit(PlaylistEvent.Loaded(r.entries.size, r.missing))
                }
                is LoadResult.Error -> _events.tryEmit(
                    if (r.reason == LoadResult.Reason.TooLarge) PlaylistEvent.TooLarge else PlaylistEvent.Invalid,
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as CanhAmpApp).container
                PlaylistViewModel(
                    player = container.playerConnection,
                    files = container.playlistFileRepository,
                    folderOf = { id -> container.trackDao.findById(id)?.folderPath },
                )
            }
        }
    }
}
