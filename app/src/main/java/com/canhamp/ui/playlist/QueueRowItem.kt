package com.canhamp.ui.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.util.formatDuration
import java.util.Locale

private val IndexWidth = 40.dp
private val RateWidth = 36.dp
private val TimeWidth = 44.dp

/**
 * One queue row (B59). Playing is inverted phosphor, selected is amber, missing is greyed with a badge.
 * [modifier] carries the long-press drag handle.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueRowItem(
    row: QueueRow,
    total: Int,
    dragging: Boolean,
    canMove: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    val e = row.entry
    val shape = Deck.shapes.medium
    val bg = when {
        row.isPlaying -> c.primaryContainer
        row.isSelected || dragging -> c.surfaceContainerHighest
        else -> c.surfaceContainerLowest
    }
    val fg = when {
        row.isPlaying -> c.onPhosphor
        row.isSelected -> c.secondaryContainer
        row.missing -> c.onSurfaceVariant
        else -> c.onSurface
    }
    val sub = if (row.isPlaying) c.onPhosphor else c.onSurfaceVariant

    val seconds = e.durationMs.coerceAtLeast(0L) / 1000L
    val spoken = listOfNotNull(
        stringResource(R.string.pl_cd_row, row.index + 1, total),
        e.artist.takeIf { it.isNotBlank() },
        e.title,
        stringResource(R.string.pl_cd_time, seconds / 60, seconds % 60),
        e.bitrateKbps?.takeIf { it > 0 }?.let { stringResource(R.string.lib_cd_kilobits, it) },
        stringResource(R.string.pl_cd_playing).takeIf { row.isPlaying },
        stringResource(R.string.pl_cd_selected).takeIf { row.isSelected },
        stringResource(R.string.pl_cd_missing).takeIf { row.missing },
    ).joinToString(", ")

    val moveUp = stringResource(R.string.pl_cd_move_up)
    val moveDown = stringResource(R.string.pl_cd_move_down)
    val remove = stringResource(R.string.pl_cd_remove)
    val play = stringResource(R.string.pl_cd_play_row)
    val actions = buildList {
        add(CustomAccessibilityAction(play) { onPlay(); true })
        if (canMove && row.index > 0) add(CustomAccessibilityAction(moveUp) { onMoveUp(); true })
        if (canMove && row.index < total - 1) add(CustomAccessibilityAction(moveDown) { onMoveDown(); true })
        add(CustomAccessibilityAction(remove) { onRemove(); true })
    }

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouch)
            .then(if (dragging) Modifier.shadow(8.dp, shape) else Modifier)
            .clip(shape)
            .background(bg)
            .then(if (dragging) Modifier.border(1.dp, c.primaryContainer, shape) else Modifier)
            .combinedClickable(onClick = onSelect, onDoubleClick = onPlay)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                customActions = actions
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(IndexWidth)
                .heightIn(min = Dimens.minTouch)
                .clearAndSetSemantics {}
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            if (row.isPlaying) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    String.format(Locale.ROOT, "%03d", row.index + 1),
                    style = Deck.type.labelLg,
                    color = if (row.isSelected) fg else sub,
                    maxLines = 1,
                )
            }
        }
        Column(Modifier.weight(1f).padding(vertical = Dimens.spaceSm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                Text(
                    e.title,
                    style = Deck.type.labelLg,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).alpha(if (row.missing) 0.6f else 1f),
                )
                if (row.missing) {
                    Text(
                        stringResource(R.string.pl_missing),
                        style = Deck.type.labelSm,
                        color = c.error,
                        maxLines = 1,
                        modifier = Modifier.border(1.dp, c.error, Deck.shapes.small).padding(horizontal = 3.dp),
                    )
                }
            }
            if (e.artist.isNotBlank()) {
                Text(
                    e.artist,
                    style = Deck.type.labelSm,
                    color = sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            e.bitrateKbps?.takeIf { it > 0 }?.toString() ?: "--",
            style = Deck.type.labelSm,
            color = sub,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(RateWidth),
        )
        Text(
            formatDuration(e.durationMs),
            style = Deck.type.labelMd,
            color = fg,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(TimeWidth),
        )
        Box(
            Modifier.size(Dimens.minTouch).clearAndSetSemantics {}.clickable(role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = if (row.isPlaying) c.onPhosphor else c.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}
