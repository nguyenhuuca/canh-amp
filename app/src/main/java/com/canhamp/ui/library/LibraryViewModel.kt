package com.canhamp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.paging.map
import com.canhamp.CanhAmpApp
import com.canhamp.data.db.GroupRow
import com.canhamp.data.db.TrackDao
import com.canhamp.data.db.TrackEntity
import com.canhamp.data.library.LibraryRepository
import com.canhamp.data.library.ScanGenerationStore
import com.canhamp.data.library.ScanOutcome
import com.canhamp.util.likePattern
import com.canhamp.util.normalizeQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val SEARCH_DEBOUNCE_MS = 150L
private const val PAGE_SIZE = 50
private const val STOP_TIMEOUT_MS = 5_000L
private const val MAX_FIELD_CHARS = 200

/** Search text as the list should see it: debounced, trimmed, capped and de-duplicated. */
@OptIn(FlowPreview::class)
fun Flow<String>.debouncedQuery(): Flow<String> =
    debounce(SEARCH_DEBOUNCE_MS).map(::normalizeQuery).distinctUntilChanged()

data class LibraryNav(val tab: LibraryTab = LibraryTab.ALL, val selection: String? = null)

data class TabCounts(val tracks: Int = 0, val artists: Int = 0, val albums: Int = 0)

/** One-shot messages shown on the deck. */
sealed interface LibraryEvent {
    data object ScanFailed : LibraryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val dao: TrackDao,
    private val repo: LibraryRepository,
    private val store: ScanGenerationStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _permission = MutableStateFlow(PermissionState.Unknown)
    val permission: StateFlow<PermissionState> = _permission.asStateFlow()

    private val _nav = MutableStateFlow(LibraryNav())
    val nav: StateFlow<LibraryNav> = _nav.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _everScanned = MutableStateFlow(false)
    val everScanned: StateFlow<Boolean> = _everScanned.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    /** Rows processed by the running scan, or null when idle. */
    val scanning: StateFlow<Int?> = repo.scanning

    private val debounced: StateFlow<String> =
        _query.debouncedQuery().stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /** True once the debounced search text is non-empty. */
    val queryActive: StateFlow<Boolean> =
        debounced.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val tracks: Flow<PagingData<TrackRow>> = combine(_nav, debounced) { nav, q ->
        sourceFor(nav.tab, nav.selection, likePattern(q))
    }.distinctUntilChanged().flatMapLatest { kind ->
        val factory = pagingFactory(kind)
        if (factory == null) {
            flowOf(PagingData.empty())
        } else {
            Pager(PagingConfig(pageSize = PAGE_SIZE), pagingSourceFactory = factory).flow
                .map { page -> page.map { it.toRow() } }
        }
    }.cachedIn(viewModelScope)

    val groups: StateFlow<List<GroupRow>> = _nav.map { it.tab }.distinctUntilChanged().flatMapLatest { tab ->
        when (tab) {
            LibraryTab.FOLDERS -> dao.folderGroups()
            LibraryTab.ARTISTS -> dao.artistGroups()
            LibraryTab.ALBUMS -> dao.albumGroups()
            else -> flowOf(emptyList())
        }
    }.combine(debounced) { list, q -> filterGroups(list, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val counts: StateFlow<TabCounts> = combine(dao.count(), dao.artistCount(), dao.albumCount(), ::TabCounts)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TabCounts())

    init {
        viewModelScope.launch { _everScanned.value = store.lastScanAtMs() > 0L }
    }

    private fun pagingFactory(kind: QueryKind): (() -> PagingSource<Int, TrackEntity>)? = when (kind) {
        is QueryKind.All -> ({ dao.pagingAll(kind.pattern) })
        is QueryKind.Recent -> ({ dao.pagingRecent(kind.pattern) })
        is QueryKind.Folder -> ({ dao.pagingByFolder(kind.name, kind.pattern) })
        is QueryKind.Artist -> ({ dao.pagingByArtist(kind.name, kind.pattern) })
        is QueryKind.Album -> ({ dao.pagingByAlbum(kind.name, kind.pattern) })
        is QueryKind.Groups -> null
    }

    fun setQuery(text: String) {
        _query.value = text.take(MAX_FIELD_CHARS)
    }

    fun clearQuery() {
        _query.value = ""
    }

    /** Switching tab keeps the search text and leaves any drill-down. */
    fun selectTab(tab: LibraryTab) = _nav.update { LibraryNav(tab, null) }

    fun drillInto(name: String) = _nav.update { it.copy(selection = name) }

    fun goUp() = _nav.update { it.copy(selection = null) }

    /** The system permission state seen at start or on resume. */
    fun onPermissionChecked(granted: Boolean) {
        val before = _permission.value
        when {
            granted -> if (before != PermissionState.Granted) {
                _permission.value = PermissionState.Granted
                val justGranted = before == PermissionState.Needed || before == PermissionState.PermanentlyDenied
                viewModelScope.launch {
                    if (LibraryRepository.shouldAutoScan(store.lastScanAtMs(), clock(), justGranted)) runScan()
                }
            }
            before == PermissionState.Unknown || before == PermissionState.Granted ->
                _permission.value = PermissionState.Needed
        }
    }

    /** The answer from the system dialog that GRANT ACCESS opened. */
    fun onPermissionRequestResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (granted) {
            _permission.value = PermissionState.Needed
            onPermissionChecked(true)
        } else {
            _permission.value = permissionAfterRequest(false, shouldShowRationale)
        }
    }

    fun rescan() {
        if (_permission.value == PermissionState.Granted) viewModelScope.launch { runScan() }
    }

    private suspend fun runScan() {
        when (repo.scan()) {
            is ScanOutcome.Success -> _everScanned.value = true
            is ScanOutcome.Failure -> _events.tryEmit(LibraryEvent.ScanFailed)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as CanhAmpApp).container
                LibraryViewModel(container.trackDao, container.libraryRepository, container.settingsRepository)
            }
        }
    }
}
