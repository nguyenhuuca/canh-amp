package com.canhamp.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.canhamp.R
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.LedToggle
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.components.rememberReducedMotion
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.util.audioPermission
import kotlinx.coroutines.delay

private const val MESSAGE_MS = 3_000L
private const val SKELETON_ROWS = 6

/**
 * The library. Playback is hoisted: [onPlay] gets the tapped track and the loaded list around it,
 * [onMore] opens the track's action sheet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onPlay: (TrackRow, List<TrackRow>) -> Unit,
    onMore: (TrackRow) -> Unit,
    modifier: Modifier = Modifier,
    vm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val context = LocalContext.current
    val permission by vm.permission.collectAsState()
    val nav by vm.nav.collectAsState()
    val query by vm.query.collectAsState()
    val queryActive by vm.queryActive.collectAsState()
    val counts by vm.counts.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val everScanned by vm.everScanned.collectAsState()
    val groups by vm.groups.collectAsState()
    val items = vm.tracks.collectAsLazyPagingItems()

    // Re-check on every resume so a grant made in system settings is picked up.
    val owner = context.findActivity() as? LifecycleOwner
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(context, audioPermission()) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                vm.onPermissionChecked(granted)
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    var message by remember { mutableStateOf<String?>(null) }
    val failedText = stringResource(R.string.lib_scan_failed)
    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                LibraryEvent.ScanFailed -> {
                    message = failedText
                    delay(MESSAGE_MS)
                    message = null
                }
            }
        }
    }

    BackHandler(enabled = nav.selection != null) { vm.goUp() }

    val needsPermission = permission == PermissionState.Needed || permission == PermissionState.PermanentlyDenied
    val groupList = nav.selection == null && nav.tab in GROUP_TABS
    val mode = listMode(
        permission = permission,
        scanning = scanning != null,
        totalTracks = counts.tracks,
        everScanned = everScanned,
        queryActive = queryActive,
        listLoaded = groupList || items.loadState.refresh is LoadState.NotLoading,
        visibleCount = if (groupList) groups.size else items.itemCount,
    )

    Column(modifier.fillMaxSize()) {
        if (needsPermission) {
            Column(Modifier.weight(1f).padding(top = Dimens.spaceMd)) {
                PermissionPanel(
                    permanentlyDenied = permission == PermissionState.PermanentlyDenied,
                    onRequestResult = vm::onPermissionRequestResult,
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = Dimens.spaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            ) {
                item(key = "bay") {
                    StorageBay(
                        scanning = scanning,
                        indexed = counts.tracks,
                        message = message,
                        onRescan = vm::rescan,
                    )
                }
                stickyHeader(key = "controls") {
                    Column(
                        Modifier.fillMaxWidth().background(Deck.colors.surface),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                    ) {
                        SearchTerminal(query, onChange = vm::setQuery, onClear = vm::clearQuery)
                        TabRail(nav.tab, counts, onSelect = vm::selectTab)
                        nav.selection?.let { sel ->
                            Breadcrumb(
                                path = if (nav.tab == LibraryTab.FOLDERS) shortPath(sel) else sel,
                                fileCount = items.itemCount,
                                onUp = vm::goUp,
                            )
                        }
                    }
                }
                listBody(
                    mode = mode,
                    groupList = groupList,
                    tab = nav.tab,
                    groups = groups,
                    items = items,
                    query = query,
                    onDrill = vm::drillInto,
                    onPlay = onPlay,
                    onMore = onMore,
                )
            }
        }
        if (permission == PermissionState.Granted) {
            ImportBar(onClick = vm::rescan)
        }
    }
}

private val GROUP_TABS = setOf(LibraryTab.FOLDERS, LibraryTab.ARTISTS, LibraryTab.ALBUMS)

private fun LazyListScope.listBody(
    mode: ListMode,
    groupList: Boolean,
    tab: LibraryTab,
    groups: List<com.canhamp.data.db.GroupRow>,
    items: LazyPagingItems<TrackRow>,
    query: String,
    onDrill: (String) -> Unit,
    onPlay: (TrackRow, List<TrackRow>) -> Unit,
    onMore: (TrackRow) -> Unit,
) {
    when (mode) {
        ListMode.Hidden -> Unit
        ListMode.Skeleton -> items(SKELETON_ROWS, key = { "skeleton-$it" }) { SkeletonRow() }
        ListMode.Empty -> item(key = "empty") {
            EmptyNote(stringResource(R.string.lib_empty), stringResource(R.string.lib_empty_hint))
        }
        ListMode.NoMatch -> item(key = "nomatch") {
            EmptyNote(stringResource(R.string.lib_no_match, query.trim()), null)
        }
        ListMode.Populated -> if (groupList) {
            items(groups, key = { "g-${tab.name}-${it.name}" }) { group ->
                GroupCard(
                    group = group,
                    displayName = if (tab == LibraryTab.FOLDERS) shortPath(group.name) else group.name,
                    onClick = { onDrill(group.name) },
                )
            }
        } else {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                val row = items[index]
                if (row == null) {
                    SkeletonRow()
                } else {
                    TrackCard(
                        row = row,
                        onPlay = { onPlay(row, items.itemSnapshotList.items) },
                        onMore = { onMore(row) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageBay(scanning: Int?, indexed: Int, message: String?, onRescan: () -> Unit) {
    val c = Deck.colors
    val reduced = rememberReducedMotion()
    val isScanning = scanning != null
    val spin = if (isScanning && !reduced) {
        val t = rememberInfiniteTransition(label = "rescanSpin")
        val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1_200)), label = "rescanAngle")
        angle
    } else {
        0f
    }
    val status = if (scanning != null) {
        stringResource(R.string.lib_scanning, scanning)
    } else {
        stringResource(R.string.lib_indexed, indexed)
    }
    // Announced politely when a scan starts and ends only: the text changes with the flag, not the count.
    val announcement = if (isScanning) stringResource(R.string.lib_scan_started) else stringResource(R.string.lib_indexed, indexed)
    val rescan = stringResource(R.string.lib_rescan)

    DeckModule(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.lib_storage_root),
                    style = Deck.type.labelLg,
                    color = c.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    Modifier
                        .heightIn(min = Dimens.minTouch)
                        .clip(Deck.shapes.medium)
                        .clickable(enabled = !isScanning, role = Role.Button, onClick = onRescan)
                        .padding(horizontal = Dimens.spaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = null,
                        tint = if (isScanning) c.primaryContainer else c.onSurface,
                        modifier = Modifier.size(20.dp).rotate(spin),
                    )
                    Spacer(Modifier.width(Dimens.spaceSm))
                    Text(rescan, style = Deck.type.labelMd, color = c.onSurface, maxLines = 1)
                }
            }
            RecessedWell(Modifier.fillMaxWidth(), vfd = true, padding = PaddingValues(Dimens.spaceMd)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message ?: status,
                        style = Deck.type.labelLg,
                        color = if (message != null) c.error else c.primaryContainer,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.lib_db_ok), style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1)
                }
            }
            Box(
                Modifier.size(0.dp).semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = announcement
                },
            )
        }
    }
}

@Composable
private fun SearchTerminal(query: String, onChange: (String) -> Unit, onClear: () -> Unit) {
    val c = Deck.colors
    RecessedWell(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = Dimens.spaceMd)) {
        Row(Modifier.heightIn(min = Dimens.minTouch), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Terminal, contentDescription = null, tint = c.primaryContainer)
            Spacer(Modifier.width(Dimens.spaceMd))
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = Deck.type.labelLg.copy(color = c.primaryContainer),
                cursorBrush = SolidColor(c.primaryContainer),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.lib_search_hint),
                                style = Deck.type.labelLg,
                                color = c.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Box(
                    Modifier.size(Dimens.minTouch).clickable(role = Role.Button, onClick = onClear),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.lib_cd_clear),
                        tint = c.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabRail(current: LibraryTab, counts: TabCounts, onSelect: (LibraryTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        for (tab in LibraryTab.entries) {
            val label = when (tab) {
                LibraryTab.ALL -> stringResource(R.string.lib_tab_all, counts.tracks)
                LibraryTab.FOLDERS -> stringResource(R.string.lib_tab_folders)
                LibraryTab.ARTISTS -> stringResource(R.string.lib_tab_artists, counts.artists)
                LibraryTab.ALBUMS -> stringResource(R.string.lib_tab_albums, counts.albums)
                LibraryTab.RECENT -> stringResource(R.string.lib_tab_recent)
            }
            LedToggle(label = label, checked = tab == current, onCheckedChange = { onSelect(tab) })
        }
    }
}

@Composable
private fun EmptyNote(title: String, hint: String?) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Dimens.spaceXl * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        VfdText(title, style = Deck.type.titleMd)
        if (hint != null) {
            Text(hint, style = Deck.type.bodyMd, color = Deck.colors.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Placeholder card; the shimmer pulses unless animations are off. */
@Composable
private fun SkeletonRow() {
    val c = Deck.colors
    val reduced = rememberReducedMotion()
    val a = if (reduced) {
        0.5f
    } else {
        val t = rememberInfiniteTransition(label = "skeleton")
        val v by t.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "skeletonAlpha")
        v
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .alpha(a)
            .clip(Deck.shapes.medium)
            .background(c.surfaceContainerLow)
            .border(1.dp, c.outlineVariant, Deck.shapes.medium),
    )
}

@Composable
private fun ImportBar(onClick: () -> Unit) {
    val c = Deck.colors
    Box(Modifier.fillMaxWidth().padding(vertical = Dimens.spaceMd)) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.minTouch)
                .clip(Deck.shapes.medium)
                .background(c.primaryContainer)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(Dimens.spaceMd),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.lib_import),
                style = Deck.type.labelLg,
                color = c.onPrimaryContainer,
                maxLines = 1,
            )
        }
    }
}
