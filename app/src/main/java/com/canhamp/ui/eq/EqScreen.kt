package com.canhamp.ui.eq

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhamp.R
import com.canhamp.playback.dsp.DspConfig
import com.canhamp.playback.dsp.DspStatus
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.HFader
import com.canhamp.ui.components.Knob
import com.canhamp.ui.components.LedToggle
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.components.VFader
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.main.DeckFormat
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Graphic equalizer (B41 to B49, B52): curve, 10 band faders plus preamp, shelves, presets. */
@Composable
fun EqScreen(vm: EqViewModel = viewModel(factory = EqViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<SheetMode?>(null) }
    var showSave by remember { mutableStateOf(false) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(vertical = Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        EqMainUnit(state, vm, onOpenPresets = { sheet = SheetMode.ALL })
        ProcessorModule(state, vm)
        ActionRow(
            onReset = vm::resetFlat,
            onSave = { showSave = true },
            onMyPresets = { sheet = SheetMode.USER },
        )
        Footnote(state.sampleRateHz)
        Spacer(Modifier.height(Dimens.spaceMd))
    }

    sheet?.let { mode ->
        PresetSheet(
            presets = state.presets,
            activeName = state.presetName,
            mode = mode,
            onSelect = {
                vm.applyPreset(it)
                sheet = null
            },
            onDelete = vm::deletePreset,
            onDismiss = { sheet = null },
        )
    }
    if (showSave) {
        SavePresetDialog(
            onSave = { name, overwrite, onResult -> vm.savePreset(name, overwrite, onResult) },
            onDismiss = { showSave = false },
        )
    }
}

@Composable
private fun EqMainUnit(state: EqUiState, vm: EqViewModel, onOpenPresets: () -> Unit) {
    val c = Deck.colors
    DeckModule(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            // Header plate.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                Column(Modifier.weight(1f)) {
                    VfdText(stringResource(R.string.eq_title), style = Deck.type.titleMd, maxLines = 2)
                    Text(stringResource(R.string.eq_subtitle), style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 2)
                }
                LedToggle(
                    label = stringResource(if (state.enabled) R.string.eq_on else R.string.eq_bypass),
                    checked = state.enabled,
                    onCheckedChange = { vm.toggleEnabled() },
                )
                TactileButton(stringResource(R.string.eq_preset), onOpenPresets, icon = Icons.Filled.Tune)
            }
            if (state.dspStatus == DspStatus.UNSUPPORTED_FORMAT) {
                RecessedWell(vfd = true, padding = PaddingValues(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm)) {
                    VfdText(stringResource(R.string.eq_dsp_na), style = Deck.type.labelMd, color = c.secondaryContainer)
                }
            }
            CurveWell(state)
            FaderDeck(state, vm)
        }
    }
}

@Composable
private fun CurveWell(state: EqUiState) {
    val c = Deck.colors
    val label = state.presetLabel ?: stringResource(R.string.eq_custom)
    RecessedWell(Modifier.fillMaxWidth(), vfd = true) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }) {
                    Text("> ${stringResource(R.string.eq_curve)} ", style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1)
                    VfdText(label, style = Deck.type.labelSm)
                }
                Text(
                    "${stringResource(R.string.eq_gain)} ${state.gainText}",
                    style = Deck.type.labelSm,
                    color = c.secondaryContainer,
                    maxLines = 1,
                )
            }
            CurveGraph(state.curve, bypassed = !state.enabled)
        }
    }
}

@Composable
private fun FaderDeck(state: EqUiState, vm: EqViewModel) {
    val c = Deck.colors
    Column(
        Modifier.fillMaxWidth().background(c.surfaceContainerLow, Deck.shapes.medium).padding(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val style = Deck.type.labelSm
            Text(stringResource(R.string.eq_scale_top), style = style, color = c.secondaryContainer)
            Text(stringResource(R.string.eq_scale_mid), style = style, color = c.onSurfaceVariant)
            Text(stringResource(R.string.eq_scale_bottom), style = style, color = c.secondaryContainer)
        }
        Row(Modifier.fillMaxWidth()) {
            FaderColumn(
                header = stringResource(R.string.eq_pre),
                value = state.preamp,
                description = stringResource(R.string.eq_cd_preamp),
                accent = c.secondaryContainer,
                onChange = vm::setPreamp,
                modifier = Modifier.weight(1f),
            )
            DspConfig.BANDS_HZ.forEachIndexed { i, hz ->
                FaderColumn(
                    header = EqLogic.bandLabel(hz),
                    value = state.bands.getOrElse(i) { 0 },
                    description = if (hz >= 1000) stringResource(R.string.eq_cd_khz, hz / 1000) else stringResource(R.string.eq_cd_hz, hz),
                    accent = c.primaryContainer,
                    onChange = { vm.setBand(i, it) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FaderColumn(
    header: String,
    value: Int,
    description: String,
    accent: androidx.compose.ui.graphics.Color,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        Text(header, style = Deck.type.labelSm, color = Deck.colors.onSurface, maxLines = 1)
        VFader(
            value = value,
            onValueChange = onChange,
            contentDescription = description,
            stateDescription = dbSpoken(value),
            accent = accent,
        )
        Text(EqLogic.dbLabel(value), style = Deck.type.labelSm, color = Deck.colors.secondaryContainer, maxLines = 1)
    }
}

@Composable
private fun dbSpoken(db: Int): String = when {
    db > 0 -> stringResource(R.string.eq_cd_db_plus, db)
    db < 0 -> stringResource(R.string.eq_cd_db_minus, -db)
    else -> stringResource(R.string.eq_cd_db_zero)
}

@Composable
private fun ProcessorModule(state: EqUiState, vm: EqViewModel) {
    val c = Deck.colors
    DeckModule(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            Text(stringResource(R.string.eq_processor), style = Deck.type.labelLg, color = c.onSurface, maxLines = 2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                KnobCell(stringResource(R.string.eq_bass), "${DspConfig.BASS_HZ}Hz", state.bass, vm::setBass, Modifier.weight(1f))
                KnobCell(stringResource(R.string.eq_treble), "${DspConfig.TREBLE_HZ / 1000}KHz", state.treble, vm::setTreble, Modifier.weight(1f))
                BalanceCell(state.balance, vm::setBalance, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CellFrame(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.background(Deck.colors.surfaceContainerLow, Deck.shapes.medium).padding(Dimens.spaceMd),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun KnobCell(label: String, corner: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier) {
    val c = Deck.colors
    CellFrame(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Text(label, style = Deck.type.labelSm, color = c.onSurface, maxLines = 1)
            Knob(
                value = value,
                onValueChange = onChange,
                contentDescription = label,
                stateDescription = dbSpoken(value),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(corner, style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1)
                Text(EqLogic.dbLabel(value) + "dB", style = Deck.type.labelSm, color = c.primaryContainer, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BalanceCell(balance: Float, onChange: (Float) -> Unit, modifier: Modifier) {
    val c = Deck.colors
    val label = DeckFormat.balanceLabel(balance, stringResource(R.string.deck_center))
    CellFrame(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Text(stringResource(R.string.deck_balance), style = Deck.type.labelSm, color = c.onSurface, maxLines = 1)
            HFader(
                value = DeckFormat.faderFromBalance(balance),
                onValueChange = { onChange(DeckFormat.balanceFromFader(it)) },
                contentDescription = stringResource(R.string.deck_cd_balance),
                filled = false,
                detent = true,
                stateDescription = label,
            )
            Text(label, style = Deck.type.labelSm, color = c.secondaryContainer, maxLines = 1)
        }
    }
}

@Composable
private fun ActionRow(onReset: () -> Unit, onSave: () -> Unit, onMyPresets: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
        TactileButton(stringResource(R.string.eq_reset), onReset, Modifier.weight(1f), icon = Icons.Filled.Remove)
        TactileButton(stringResource(R.string.eq_save), onSave, Modifier.weight(1f), icon = Icons.Filled.Save)
        TactileButton(stringResource(R.string.eq_my_presets), onMyPresets, Modifier.weight(1f), icon = Icons.Filled.LibraryMusic)
    }
}

@Composable
private fun Footnote(sampleRateHz: Int?) {
    val c = Deck.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceMd), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.eq_footnote), style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 2, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.eq_status, DeckFormat.kHz(sampleRateHz)),
            style = Deck.type.labelSm,
            color = c.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
    }
}
