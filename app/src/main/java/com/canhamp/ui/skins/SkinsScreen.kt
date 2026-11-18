package com.canhamp.ui.skins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhamp.CanhAmpApp
import com.canhamp.R
import com.canhamp.playback.dsp.SpectrumFrame
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.LedToggle
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.ui.theme.ThemeId
import com.canhamp.ui.theme.deckColorsFor

/** The skin browser (B67, B68, B26). Applying a theme recomposes the whole app from the settings flow. */
@Composable
fun SkinsScreen(vm: SkinsViewModel = viewModel(factory = SkinsViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val engine = (LocalContext.current.applicationContext as CanhAmpApp).container.dspEngine
    // Cold flow: the analyser runs only while this screen is resumed and the visualizer is allowed (B26).
    val frames = remember(engine) { engine.spectrumFrames() }
    val bars = if (state.visAnimated) {
        val frame by frames.collectAsStateWithLifecycle(
            initialValue = SpectrumFrame.EMPTY,
            minActiveState = Lifecycle.State.RESUMED,
        )
        miniBars(frame)
    } else {
        STATIC_MINI_BARS
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(vertical = Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        Banner(state.applied)
        FilterChips(state.filter, vm::setFilter)
        for (skin in state.visible) {
            ThemeCard(skin, applied = skin.id == state.applied, now = now, bars = bars, onApply = { vm.apply(skin.id) })
        }
        Preferences(state, vm::setPreserveEqColors, vm::setVisAnimated)
        Text(
            stringResource(R.string.skins_footer, SKINS.size),
            style = Deck.type.labelSm,
            color = Deck.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Banner(applied: ThemeId) {
    val c = Deck.colors
    val skin = skinFor(applied)
    val palette = deckColorsFor(applied)
    DeckModule(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = c.primaryContainer)
                Spacer(Modifier.width(Dimens.spaceMd))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.skins_title), style = Deck.type.labelLg, color = c.onSurface, maxLines = 2)
                    Text(stringResource(R.string.skins_subtitle), style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1)
                }
                Text(
                    stringResource(R.string.skins_engine),
                    style = Deck.type.labelSm,
                    color = c.secondaryContainer,
                    maxLines = 1,
                    modifier = Modifier
                        .border(1.dp, c.secondaryContainer, Deck.shapes.small)
                        .padding(horizontal = Dimens.spaceSm, vertical = Dimens.spaceXs),
                )
            }
            Text(stringResource(R.string.skins_mounted), style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1)
            RecessedWell(Modifier.fillMaxWidth(), vfd = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(Deck.shapes.small)
                            .background(Brush.verticalGradient(listOf(palette.surfaceContainerHigh, palette.vfdWell)))
                            .border(1.dp, palette.primaryContainer, Deck.shapes.small),
                    )
                    Spacer(Modifier.width(Dimens.spaceMd))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(skin.nameRes), style = Deck.type.labelLg, color = c.primaryContainer, maxLines = 1)
                        Text(stringResource(skin.descRes), style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 2)
                    }
                    Text(stringResource(R.string.skins_active), style = Deck.type.labelSm, color = c.primaryContainer, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FilterChips(current: SkinCategory, onSelect: (SkinCategory) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        for (category in SkinCategory.entries) {
            val label = if (category == SkinCategory.ALL) {
                stringResource(category.labelRes, SKINS.size)
            } else {
                stringResource(category.labelRes)
            }
            LedToggle(label = label, checked = category == current, onCheckedChange = { onSelect(category) })
        }
    }
}

@Composable
private fun Preferences(state: SkinsUiState, onPreserve: (Boolean) -> Unit, onAnimated: (Boolean) -> Unit) {
    val c = Deck.colors
    DeckModule(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Text(stringResource(R.string.skins_prefs), style = Deck.type.labelLg, color = c.onSurface, maxLines = 1)
            PreferenceRow(stringResource(R.string.skins_pref_eq_colors), state.preserveEqColors, onPreserve)
            PreferenceRow(stringResource(R.string.skins_pref_anim_vis), state.visAnimated, onAnimated)
        }
    }
}

/** The whole row toggles; the state is spelled out as ENABLED or DISABLED, not by colour alone. */
@Composable
private fun PreferenceRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Deck.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.minTouch)
            .clip(Deck.shapes.medium)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Deck.type.labelMd, color = c.onSurface, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(8.dp)
                .clip(Deck.shapes.small)
                .background(if (checked) c.primaryContainer else c.surfaceVariant),
        )
        Spacer(Modifier.width(Dimens.spaceMd))
        Text(
            stringResource(if (checked) R.string.skins_enabled else R.string.skins_disabled),
            style = Deck.type.labelSm,
            color = if (checked) c.primaryContainer else c.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
