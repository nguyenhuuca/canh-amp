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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhamp.CanhAmpApp
import com.canhamp.R
import com.canhamp.data.settings.RepeatSetting
import com.canhamp.playback.dsp.DspStatus
import com.canhamp.playback.dsp.SpectrumFrame
import com.canhamp.playback.NowPlaying
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.HFader
import com.canhamp.ui.components.LedToggle
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.library.TrackDetailsSheet
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.util.formatDuration

/** The main deck: modules 1 to 8 of the design spec, top to bottom. */
@Composable
fun MainScreen(
    onOpen: () -> Unit,
    onOpenEq: () -> Unit,
    vm: MainViewModel = viewModel(factory = MainViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showDetails by remember { mutableStateOf(false) }
    val currentTrackId = state.now.trackId
    val engine = (LocalContext.current.applicationContext as CanhAmpApp).container.dspEngine
    // Cold flow: the analyser only runs while this screen is resumed (B26).
    val frames = remember(engine) { engine.spectrumFrames() }
    val frame by frames.collectAsStateWithLifecycle(
        initialValue = SpectrumFrame.EMPTY,
        minActiveState = Lifecycle.State.RESUMED,
    )

    MainScreenContent(
        state = state,
        frame = frame,
        onOpen = onOpen,
        onOpenEq = onOpenEq,
        actions = DeckActions(
            toggleTime = vm::toggleTimeMode,
            cycleVis = vm::cycleVisMode,
            seek = vm::seek,
            previous = vm::previous,
            play = vm::play,
            pause = vm::pause,
            stop = vm::stop,
            next = vm::next,
            toggleShuffle = vm::toggleShuffle,
            cycleRepeat = vm::cycleRepeat,
        ),
        onShowDetails = { showDetails = true },
    )
    if (showDetails && currentTrackId != null) {
        TrackDetailsSheet(trackId = currentTrackId, onDismiss = { showDetails = false })
    }
}

/** Everything the deck can ask of the player, so the content composable needs no view model. */
private class DeckActions(
    val toggleTime: () -> Unit = {},
    val cycleVis: () -> Unit = {},
    val seek: (Long) -> Unit = {},
    val previous: () -> Unit = {},
    val play: () -> Unit = {},
    val pause: () -> Unit = {},
    val stop: () -> Unit = {},
    val next: () -> Unit = {},
    val toggleShuffle: () -> Unit = {},
    val cycleRepeat: () -> Unit = {},
)

/** The deck's modules for one [state]. */
@Composable
private fun MainScreenContent(
    state: MainUiState,
    frame: SpectrumFrame,
    onOpen: () -> Unit,
    onOpenEq: () -> Unit,
    actions: DeckActions,
    onShowDetails: () -> Unit,
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(vertical = Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        ChassisPlate(state.dsp)
        DeckDisplay(state, frame, actions.toggleTime, actions.cycleVis)
        SeekModule(state, actions.seek)
        TransportDeck(
            state,
            onPrev = actions.previous,
            onPlay = actions.play,
            onPause = actions.pause,
            onStop = actions.stop,
            onNext = actions.next,
            onOpen = onOpen,
        )
        TogglesRow(state, actions.toggleShuffle, actions.cycleRepeat, onShowDetails, onOpenEq, infoEnabled = state.now.trackId != null)
        NowPlayingCard(state.now)
        FooterPlate()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417, fontScale = 1.3f, widthDp = 360, heightDp = 1400)
@Composable
private fun MainScreenFontScalePreview() = CanhAmpTheme {
    MainScreenContent(
        state = MainUiState(
            now = NowPlaying(
                trackId = 1L,
                title = "A Very Long Track Title That Will Not Fit",
                artist = "An Artist With A Long Name",
                album = "Long Album Name",
                durationMs = 245_000,
                positionMs = 83_000,
                queueSize = 12,
                index = 3,
                bitrateKbps = 320,
                sampleRateHz = 44_100,
                channelCount = 2,
                isPlaying = true,
            ),
        ),
        frame = SpectrumFrame.EMPTY,
        onOpen = {},
        onOpenEq = {},
        actions = DeckActions(),
        onShowDetails = {},
    )
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
                Text(stringResource(R.string.deck_seek), style = Deck.type.labelMd, color = c.onSurface, maxLines = 1)
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
private fun TogglesRow(
    state: MainUiState,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onInfo: () -> Unit,
    onEq: () -> Unit,
    infoEnabled: Boolean,
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
                stringResource(R.string.btn_info), onInfo, icon = Icons.Filled.Info, enabled = infoEnabled,
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
