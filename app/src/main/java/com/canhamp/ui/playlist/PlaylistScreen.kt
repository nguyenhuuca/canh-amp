package com.canhamp.ui.playlist

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhamp.R
import com.canhamp.data.playlist.PlaylistFileRepository
import com.canhamp.data.settings.RepeatSetting
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.components.rememberReducedMotion
import com.canhamp.ui.library.TrackDetailsSheet
import com.canhamp.ui.library.shortPath
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

private const val MESSAGE_MS = 3_000L
private val PLAYLIST_MIME_TYPES = arrayOf(
    "audio/x-mpegurl",
    "audio/mpegurl",
    "application/vnd.apple.mpegurl",
    "text/plain",
)

/**
 * The queue editor. Starting a pick from the library is hoisted: [onPickFiles] and [onPickDir] open the Library
 * in pick mode, [onAddFromLibrary] opens it plainly.
 */
@Composable
fun PlaylistScreen(
    onAddFromLibrary: () -> Unit,
    onPickFiles: () -> Unit,
    onPickDir: () -> Unit,
    modifier: Modifier = Modifier,
    vm: PlaylistViewModel = viewModel(factory = PlaylistViewModel.Factory),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val undo by vm.undo.collectAsState()
    val folder by vm.playingFolder.collectAsState()

    var message by remember { mutableStateOf<String?>(null) }
    var pendingLoad by remember { mutableStateOf<Uri?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var detailsId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            message = when (event) {
                is PlaylistEvent.Loaded -> context.getString(R.string.pl_loaded, event.count, event.missing)
                PlaylistEvent.Invalid -> context.getString(R.string.pl_invalid)
                PlaylistEvent.TooLarge -> context.getString(R.string.pl_too_large)
                is PlaylistEvent.Saved -> context.getString(R.string.pl_saved, event.count)
                PlaylistEvent.SaveFailed -> context.getString(R.string.pl_save_failed)
            }
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(MESSAGE_MS)
            message = null
        }
    }

    fun loadNow(uri: Uri) {
        val id = if (DocumentsContract.isDocumentUri(context, uri)) {
            runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        } else {
            null
        }
        vm.load(uri.toString(), id)
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        if (uri != null) vm.save(uri.toString())
    }
    val loadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (state.total > 0) pendingLoad = uri else loadNow(uri)
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(vertical = Dimens.spaceMd), verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            HeaderWell(state)
            FilterDeck(
                query = state.filter,
                repeat = state.repeat,
                onQuery = vm::setFilter,
                onClear = vm::clearFilter,
                onRandom = vm::randomize,
                onLoop = vm::cycleRepeat,
            )
            val note = undo?.let { stringResource(R.string.pl_removed) } ?: message
            ListWell(
                state = state,
                vm = vm,
                note = note,
                noteAction = if (undo != null) stringResource(R.string.undo) else null,
                onAddFromLibrary = onAddFromLibrary,
                modifier = Modifier.weight(1f),
            )
            ActionGrid(
                sortMode = state.sortMode,
                hasSelection = state.selectedIndex != null,
                canShowDetails = (state.selectedTrackId ?: -1L) >= 0L,
                onAddFile = onPickFiles,
                onAddDir = onPickDir,
                onDelete = vm::removeSelected,
                onMisc = { detailsId = state.selectedTrackId?.takeIf { it >= 0L } },
                onSave = { saveLauncher.launch(PlaylistFileRepository.defaultFileName(LocalDateTime.now())) },
                onLoad = { loadLauncher.launch(PLAYLIST_MIME_TYPES) },
                onClear = { if (state.total > 0) confirmClear = true },
                onSort = vm::sort,
            )
            PathPill(folder)
            Text(
                stringResource(R.string.pl_export_banner),
                style = Deck.type.labelSm,
                color = Deck.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    pendingLoad?.let { uri ->
        ConfirmDialog(
            text = stringResource(R.string.pl_replace_confirm),
            onConfirm = {
                pendingLoad = null
                loadNow(uri)
            },
            onDismiss = { pendingLoad = null },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            text = stringResource(R.string.pl_clear_confirm),
            onConfirm = {
                confirmClear = false
                vm.clearQueue()
            },
            onDismiss = { confirmClear = false },
        )
    }
    detailsId?.let { id -> TrackDetailsSheet(trackId = id, onDismiss = { detailsId = null }) }
}

@Composable
private fun HeaderWell(state: PlaylistState) {
    val c = Deck.colors
    val reduced = rememberReducedMotion()
    val pulse = if (reduced) {
        1f
    } else {
        val t = rememberInfiniteTransition(label = "plPulse")
        val a by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "plPulseAlpha")
        a
    }
    RecessedWell(Modifier.fillMaxWidth(), vfd = true, glare = true) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).alpha(pulse).clip(CircleShape).background(c.primaryContainer))
                Spacer(Modifier.width(Dimens.spaceMd))
                Text(
                    stringResource(R.string.pl_title) + " " + stringResource(R.string.pl_version),
                    style = Deck.type.labelLg,
                    color = c.primaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Metric(stringResource(R.string.pl_tracks), state.stats.count.toString(), c.primaryContainer)
                Metric(stringResource(R.string.pl_time), state.stats.totalText, c.primaryContainer)
                Metric(stringResource(R.string.pl_selected), state.stats.selectedText, c.secondaryContainer)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
        Text(label, style = Deck.type.labelMd, color = Deck.colors.onSurfaceVariant, maxLines = 1)
        Text(value, style = Deck.type.labelMd, color = valueColor, maxLines = 1)
    }
}

@Composable
private fun FilterDeck(
    query: String,
    repeat: RepeatSetting,
    onQuery: (String) -> Unit,
    onClear: () -> Unit,
    onRandom: () -> Unit,
    onLoop: () -> Unit,
) {
    val c = Deck.colors
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd), verticalAlignment = Alignment.CenterVertically) {
        RecessedWell(Modifier.weight(1f), padding = PaddingValues(horizontal = Dimens.spaceMd)) {
            Row(Modifier.heightIn(min = Dimens.minTouch), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Terminal, contentDescription = null, tint = c.primaryContainer)
                Spacer(Modifier.width(Dimens.spaceMd))
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = Deck.type.labelLg.copy(color = c.primaryContainer),
                    cursorBrush = SolidColor(c.primaryContainer),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(R.string.pl_filter_hint),
                                    style = Deck.type.labelMd,
                                    color = c.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
        TactileButton(
            stringResource(R.string.pl_rnd),
            onRandom,
            contentDescription = stringResource(R.string.pl_cd_random),
        )
        TactileButton(
            label = "",
            onClick = onLoop,
            icon = if (repeat == RepeatSetting.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            active = repeat != RepeatSetting.OFF,
            contentDescription = stringResource(R.string.pl_cd_loop) + ": " + when (repeat) {
                RepeatSetting.OFF -> stringResource(R.string.toggle_repeat)
                RepeatSetting.ALL -> stringResource(R.string.toggle_repeat_all)
                RepeatSetting.ONE -> stringResource(R.string.toggle_repeat_one)
            },
        )
    }
}

@Composable
private fun ListWell(
    state: PlaylistState,
    vm: PlaylistViewModel,
    note: String?,
    noteAction: String?,
    onAddFromLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    RecessedWell(modifier.fillMaxWidth(), padding = PaddingValues(Dimens.spaceSm)) {
      Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = Dimens.spaceSm, bottom = Dimens.spaceSm)) {
                Text("#", style = Deck.type.labelSm, color = c.onSurfaceVariant, modifier = Modifier.width(32.dp))
                Text(
                    stringResource(R.string.pl_col_title),
                    style = Deck.type.labelSm,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.pl_col_rate),
                    style = Deck.type.labelSm,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.width(36.dp),
                )
                Text(
                    stringResource(R.string.pl_col_time),
                    style = Deck.type.labelSm,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(44.dp),
                )
                Spacer(Modifier.width(Dimens.minTouch))
            }
            when {
                state.total == 0 -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg, Alignment.CenterVertically),
                ) {
                    VfdText(stringResource(R.string.pl_empty), style = Deck.type.titleMd)
                    TactileButton(stringResource(R.string.pl_add_from_library), onAddFromLibrary)
                }
                state.rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    VfdText(stringResource(R.string.pl_no_match, state.filter.trim()), style = Deck.type.labelLg)
                }
                else -> QueueList(state, vm)
            }
        }
        if (note != null) {
            Toast(
                text = note,
                actionLabel = noteAction,
                onAction = vm::undoRemove,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Dimens.spaceMd),
            )
        }
      }
    }
}

@Composable
private fun QueueList(state: PlaylistState, vm: PlaylistViewModel) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val reorder = rememberReorderableLazyListState(listState) { from, to -> vm.move(from.index, to.index) }
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        items(state.rows, key = { it.key }) { row ->
            ReorderableItem(reorder, key = row.key, enabled = state.canReorder) { dragging ->
                QueueRowItem(
                    row = row,
                    total = state.total,
                    dragging = dragging,
                    canMove = state.canReorder,
                    onSelect = { vm.select(row.index) },
                    onPlay = { vm.play(row.index) },
                    onRemove = { vm.remove(row.index) },
                    onMoveUp = { vm.move(row.index, row.index - 1) },
                    onMoveDown = { vm.move(row.index, row.index + 1) },
                    modifier = Modifier.longPressDraggableHandle(
                        enabled = state.canReorder,
                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PathPill(folder: String?) {
    val c = Deck.colors
    RecessedWell(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                folder?.let(::shortPath).orEmpty().ifEmpty { "-" },
                style = Deck.type.labelMd,
                color = c.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Dimens.spaceMd))
            Text(
                stringResource(R.string.pl_read_only),
                style = Deck.type.labelSm,
                color = c.secondaryContainer,
                maxLines = 1,
            )
        }
    }
}

/** Deck toast with an optional action, announced politely. */
@Composable
private fun Toast(text: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    RecessedWell(modifier.semantics { liveRegion = LiveRegionMode.Polite }, vfd = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg)) {
            VfdText(text, style = Deck.type.labelMd)
            if (actionLabel != null) TactileButton(actionLabel, onAction)
        }
    }
}

@Composable
private fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = Deck.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceContainer,
        title = { Text(text, style = Deck.type.labelLg, color = c.onSurface) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.pl_yes), style = Deck.type.labelLg, color = c.primaryContainer)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pl_cancel), style = Deck.type.labelLg, color = c.onSurfaceVariant)
            }
        },
    )
}
