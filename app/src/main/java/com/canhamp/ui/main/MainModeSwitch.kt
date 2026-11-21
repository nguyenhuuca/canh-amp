package com.canhamp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.data.settings.MainMode
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Two-segment switch between the full deck and the Pure Minimal player. The choice is persisted. */
@Composable
fun MainModeSwitch(mode: MainMode, onSelect: (MainMode) -> Unit, modifier: Modifier = Modifier) {
    val c = Deck.colors
    val shape = Deck.shapes.medium
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceContainerLowest)
            .border(1.dp, c.surfaceVariant.copy(alpha = 0.3f), shape)
            .padding(Dimens.spaceSm)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Segment(stringResource(R.string.main_mode_deck), mode == MainMode.DECK, deckIcon = true, Modifier.weight(1f)) {
            onSelect(MainMode.DECK)
        }
        Segment(stringResource(R.string.main_mode_minimal), mode == MainMode.MINIMAL, deckIcon = false, Modifier.weight(1f)) {
            onSelect(MainMode.MINIMAL)
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, deckIcon: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Deck.colors
    val shape = Deck.shapes.small
    val tint = if (selected) c.primaryContainer else c.onSurfaceVariant
    Row(
        modifier
            .heightIn(min = Dimens.minTouch)
            .clip(shape)
            .background(if (selected) c.surfaceContainerHighest else c.surfaceContainer)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (deckIcon) {
            Icon(Icons.Filled.Dashboard, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        } else {
            Spacer(Modifier.size(6.dp).clip(CircleShape).background(if (selected) c.primaryContainer else c.surfaceVariant))
        }
        Spacer(Modifier.width(Dimens.spaceMd))
        Text(label, style = Deck.type.labelSm, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
