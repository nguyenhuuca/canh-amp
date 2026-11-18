package com.canhamp.ui.main

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhamp.CanhAmpApp
import com.canhamp.R
import com.canhamp.data.settings.RepeatSetting
import com.canhamp.playback.dsp.DspStatus
import com.canhamp.playback.dsp.SpectrumFrame
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.HFader
import com.canhamp.ui.components.LedToggle
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.util.formatDuration

/** The main deck: modules 1 to 8 of the design spec, top to bottom. */
@Composable
fun MainScreen(
    onOpen: () -> Unit,
    onOpenEq: () -> Unit,
    onInfo: () -> Unit = {},
    vm: MainViewModel = viewModel(factory = MainViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val engine = (LocalContext.current.applicationContext as CanhAmpApp).container.dspEngine
    // Cold flow: the analyser only runs while this screen is resumed (B26).
    val frames = remember(engine) { engine.spectrumFrames() }
    val frame by frames.collectAsStateWithLifecycle(
        initialValue = SpectrumFrame.EMPTY,
        minActiveState = Lifecycle.State.RESUMED,
    )

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(vertical = Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        ChassisPlate(state.dsp)
        DeckDisplay(state, frame, vm::toggleTimeMode, vm::cycleVisMode)
        SeekModule(state, vm::seek)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg)) {
            VolumeModule(state, vm::setVolume, Modifier.weight(1f))
            BalanceModule(state, vm::setBalance, Modifier.weight(1f))
        }
        TransportDeck(
            state,
            onPrev = vm::previous,
            onPlay = vm::play,
            onPause = vm::pause,
            onStop = vm::stop,
            onNext = vm::next,
            onOpen = onOpen,
        )
        TogglesRow(state, vm::toggleShuffle, vm::cycleRepeat, onInfo, onOpenEq)
        NowPlayingCard(state.now)
        FooterPlate()
    }
}

@Composable
private fun Screw() {
    Box(Modifier.size(10.dp).clip(CircleShape).background(Deck.colors.surfaceContainerHighest))
}

@Composable
private fun ChassisPlate(dsp: DspStatus) {
    val c = Deck.colors
    val pill = stringResource(
        when (dsp) {
            DspStatus.ACTIVE -> R.string.deck_dsp_ready
            DspStatus.BYPASSED -> R.string.deck_dsp_bypass
            DspStatus.UNSUPPORTED_FORMAT -> R.string.deck_dsp_na
        },
    )
    DeckModule(Modifier.fillMaxWidth(), PaddingValues(horizontal = Dimens.spaceMd, vertical = Dimens.spaceMd)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            Screw()
            Text(stringResource(R.string.deck_chassis), style = Deck.type.labelMd, color = c.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
            RecessedWell(vfd = true, padding = PaddingValues(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm)) {
                VfdText(pill, style = Deck.type.labelMd)
            }
            Screw()
        }
    }
}

private class DragHolder {
    var value: Float? = null
}

@Composable
private fun SeekModule(state: MainUiState, onSeek: (Long) -> Unit) {
    val c = Deck.colors
    val now = state.now
    val duration = now.durationMs
    val holder = remember { DragHolder() }
    var drag by remember { mutableStateOf<Float?>(null) }
    val fraction = if (duration > 0) (now.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shownMs = drag?.let { (it * duration).toLong() } ?: now.positionMs
    val label = if (state.hasQueue) "${formatDuration(shownMs)} / ${formatDuration(duration)}" else DeckFormat.EMPTY_TIMER
    DeckModule(Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.deck_seek), style = Deck.type.labelMd, color = c.onSurface)
                VfdText(label, style = Deck.type.labelMd)
            }
            HFader(
                value = drag ?: fraction,
                onValueChange = {
                    holder.value = it
                    drag = it
                },
                contentDescription = stringResource(R.string.deck_cd_seek),
                onValueChangeFinished = {
                    // B28: one seek on release.
                    val v = holder.value
                    holder.value = null
                    drag = null
                    if (v != null && duration > 0) onSeek((v * duration).toLong())
                },
                stateDescription = label,
            )
        }
    }
}

@Composable
private fun VolumeModule(state: MainUiState, onVolume: (Float) -> Unit, modifier: Modifier) {
    val c = Deck.colors
    val volume = DeckFormat.volumeLabel(state.now.volume)
    DeckModule(modifier) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.deck_volume), style = Deck.type.labelMd, color = c.onSurface, maxLines = 1)
                VfdText(volume, style = Deck.type.labelMd)
            }
            HFader(
                value = state.now.volume,
                onValueChange = onVolume,
                contentDescription = stringResource(R.string.deck_cd_volume),
                stateDescription = volume,
            )
            ScaleRow(stringResource(R.string.deck_min), stringResource(R.string.deck_mid), stringResource(R.string.deck_max))
        }
    }
}

@Composable
private fun BalanceModule(state: MainUiState, onBalance: (Float) -> Unit, modifier: Modifier) {
    val c = Deck.colors
    val label = DeckFormat.balanceLabel(state.balance, stringResource(R.string.deck_center))
    DeckModule(modifier) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.deck_balance), style = Deck.type.labelMd, color = c.onSurface, maxLines = 1)
                Text(label, style = Deck.type.labelMd, color = c.secondaryContainer, maxLines = 1)
            }
            HFader(
                value = DeckFormat.faderFromBalance(state.balance),
                onValueChange = { onBalance(DeckFormat.balanceFromFader(it)) },
                contentDescription = stringResource(R.string.deck_cd_balance),
                filled = false,
                detent = true,
                stateDescription = label,
            )
            ScaleRow(stringResource(R.string.deck_left), stringResource(R.string.deck_centre_short), stringResource(R.string.deck_right))
        }
    }
}

@Composable
private fun ScaleRow(start: String, mid: String, end: String) {
    val style = Deck.type.labelSm
    val color = Deck.colors.onSurfaceVariant
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(start, style = style, color = color)
        Text(mid, style = style, color = color)
        Text(end, style = style, color = color)
    }
}

@Composable
private fun TogglesRow(
    state: MainUiState,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onInfo: () -> Unit,
    onEq: () -> Unit,
) {
    val repeat = state.now.repeat
    val repeatLabel = stringResource(
        when (repeat) {
            RepeatSetting.OFF -> R.string.toggle_repeat
            RepeatSetting.ALL -> R.string.toggle_repeat_all
            RepeatSetting.ONE -> R.string.toggle_repeat_one
        },
    )
    DeckModule(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter), verticalAlignment = Alignment.CenterVertically) {
            LedToggle(stringResource(R.string.toggle_shuffle), state.now.shuffle, { onShuffle() }, Modifier.weight(1f))
            LedToggle(repeatLabel, repeat != RepeatSetting.OFF, { onRepeat() }, Modifier.weight(1f))
            TactileButton(
                stringResource(R.string.btn_info), onInfo, icon = Icons.Filled.Info,
                contentDescription = stringResource(R.string.deck_cd_info),
            )
            TactileButton(stringResource(R.string.nav_eq), onEq, icon = Icons.Filled.Equalizer)
        }
    }
}

@Composable
private fun FooterPlate() {
    val c = Deck.colors
    DeckModule(Modifier.fillMaxWidth(), PaddingValues(Dimens.spaceMd)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            Screw()
            Text(stringResource(R.string.deck_serial), style = Deck.type.labelMd, color = c.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.deck_brand), style = Deck.type.labelMd, color = c.secondaryContainer, maxLines = 1)
            Screw()
        }
    }
    Spacer(Modifier.height(Dimens.spaceMd))
}
