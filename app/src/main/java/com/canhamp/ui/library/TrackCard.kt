package com.canhamp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import java.util.Locale

/** One track: a single focus node whose Play, Queue, Next and Details actions are also exposed to screen readers. */
@Composable
fun TrackCard(
    row: TrackRow,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    /** Non-null in pick mode: shows a checkbox with this state instead of the play and more buttons. */
    pickChecked: Boolean? = null,
) {
    val c = Deck.colors
    val parts = listOfNotNull(
        row.artist,
        row.title,
        row.format.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT),
        row.bitrateKbps?.takeIf { it > 0 }?.let { stringResource(R.string.lib_cd_kilobits, it) },
        formatSize(row.sizeBytes),
    )
    val spoken = parts.joinToString(", ")
    val actions = listOf(
        CustomAccessibilityAction(stringResource(R.string.cd_play)) { onPlay(); true },
        CustomAccessibilityAction(stringResource(R.string.lib_add_queue)) { onMore(); true },
        CustomAccessibilityAction(stringResource(R.string.lib_play_next)) { onMore(); true },
        CustomAccessibilityAction(stringResource(R.string.lib_details)) { onMore(); true },
    )
    val album = stringResource(R.string.lib_album_line, row.album)
    val modified = formatDate(row.dateModifiedSec)?.let { stringResource(R.string.lib_mod_line, it) }

    Row(
        modifier
            .fillMaxWidth()
            .clip(Deck.shapes.medium)
            .background(c.surfaceContainerLow)
            .border(1.dp, c.outlineVariant, Deck.shapes.medium)
            .clickable(onClick = onPlay)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                customActions = actions
                if (pickChecked != null) selected = pickChecked
            }
            .padding(start = Dimens.spaceLg, top = Dimens.spaceMd, bottom = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AudioFile, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(Dimens.spaceLg))
        Column(Modifier.weight(1f)) {
            Text(row.title, style = Deck.type.titleMd, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                trackBadges(row),
                style = Deck.type.labelMd,
                color = c.primaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                Text(
                    album,
                    style = Deck.type.labelSm,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (modified != null) {
                    Text(modified, style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        if (pickChecked != null) {
            Box(Modifier.size(Dimens.minTouch), contentAlignment = Alignment.Center) {
                Icon(
                    if (pickChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (pickChecked) c.primaryContainer else c.onSurfaceVariant,
                )
            }
        } else {
            CardButton(Icons.Filled.PlayArrow, onPlay, circle = true)
            CardButton(Icons.Filled.MoreVert, onMore, circle = false)
        }
    }
}

/** 32 dp visual, 48 dp hit; hidden from accessibility because the card exposes the same actions. */
@Composable
private fun CardButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, circle: Boolean) {
    val c = Deck.colors
    Box(
        Modifier.size(Dimens.minTouch).clearAndSetSemantics {}.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(if (circle) CircleShape else Deck.shapes.medium)
                .background(if (circle) c.surfaceContainerHighest else c.surfaceContainerLow)
                .border(1.dp, c.outlineVariant, if (circle) CircleShape else Deck.shapes.medium)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (circle) c.primaryContainer else c.onSurfaceVariant)
        }
    }
}
