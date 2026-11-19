package com.canhamp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.canhamp.ui.skins.SkinsScreen
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

@Composable
fun SettingsScreen() {
    var tab by rememberSaveable { mutableStateOf(SettingsTab.SKIN) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = Dimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            for (t in SettingsTab.entries) {
                TabChip(stringResource(t.labelRes), selected = t == tab, onSelect = { tab = t })
            }
        }
        when (tab) {
            SettingsTab.SKIN -> SkinsScreen()
            SettingsTab.RELEASE_NOTES -> ReleaseNotesTab()
            SettingsTab.LANGUAGE -> LanguageTab()
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onSelect: () -> Unit) {
    val c = Deck.colors
    val shape = Deck.shapes.medium
    Row(
        Modifier
            .heightIn(min = Dimens.minTouch)
            .clip(shape)
            .background(if (selected) c.surfaceContainerLowest else c.surfaceContainerHigh)
            .border(1.dp, if (selected) c.bevelShadow else c.bevelHighlight, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) c.primaryContainer else c.surfaceVariant),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label.uppercase(),
            style = Deck.type.labelSm,
            color = if (selected) c.primaryContainer else c.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
