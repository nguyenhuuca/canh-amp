package com.canhamp.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.data.db.GroupRow
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/**
 * A folder, artist or album: name, track count and a chevron. An imported SAF folder ([saf]) shows a badge
 * and offers [onLongClick] to remove it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCard(
    group: GroupRow,
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    saf: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val c = Deck.colors
    val count = stringResource(R.string.lib_tracks_count, group.count)
    val safHint = if (saf) stringResource(R.string.lib_cd_saf_hint) else null
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouch)
            .clip(Deck.shapes.medium)
            .background(c.surfaceContainerLow)
            .border(1.dp, c.outlineVariant, Deck.shapes.medium)
            .combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (safHint != null) "$displayName, $count. $safHint" else "$displayName, $count"
            }
            .padding(Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(displayName, style = Deck.type.titleMd, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(count, style = Deck.type.labelMd, color = c.primaryContainer, maxLines = 1)
                if (saf) {
                    Spacer(Modifier.width(Dimens.spaceMd))
                    Text(
                        stringResource(R.string.lib_saf_badge),
                        style = Deck.type.labelSm,
                        color = c.secondaryContainer,
                        maxLines = 1,
                        modifier = Modifier
                            .border(1.dp, c.secondaryContainer, Deck.shapes.small)
                            .padding(horizontal = Dimens.spaceSm),
                    )
                }
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.onSurfaceVariant)
    }
}
