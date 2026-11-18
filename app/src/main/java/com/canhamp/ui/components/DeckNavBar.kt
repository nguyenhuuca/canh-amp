package com.canhamp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.ui.DeckDestination
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.ui.theme.glowShadow

/** Bottom 5-tab bar. The active tab is recessed with phosphor text. */
@Composable
fun DeckNavBar(
    current: DeckDestination,
    onSelect: (DeckDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.surfaceContainerLow)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(Dimens.navHeight),
    ) {
        DeckDestination.entries.forEach { dest ->
            val active = dest == current
            val tint = if (active) c.primaryContainer else c.onSurfaceVariant
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (active) c.surfaceContainerHighest else c.surfaceContainerLow)
                    .semantics { selected = active }
                    .clickable(role = Role.Tab) { onSelect(dest) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(dest.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                Text(
                    stringResource(dest.labelRes).uppercase(),
                    style = Deck.type.labelSm.let { if (active) it.copy(shadow = glowShadow(c.glow)) else it },
                    color = tint,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun DeckNavBarPreview() = CanhAmpTheme {
    DeckNavBar(DeckDestination.MAIN, {})
}
