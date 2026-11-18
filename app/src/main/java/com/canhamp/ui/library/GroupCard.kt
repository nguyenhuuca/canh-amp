package com.canhamp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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

/** A folder, artist or album: name, track count and a chevron. */
@Composable
fun GroupCard(
    group: GroupRow,
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    val count = stringResource(R.string.lib_tracks_count, group.count)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouch)
            .clip(Deck.shapes.medium)
            .background(c.surfaceContainerLow)
            .border(1.dp, c.outlineVariant, Deck.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$displayName, $count" }
            .padding(Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(displayName, style = Deck.type.titleMd, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(count, style = Deck.type.labelMd, color = c.primaryContainer, maxLines = 1)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.onSurfaceVariant)
    }
}
