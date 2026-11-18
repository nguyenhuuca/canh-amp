package com.canhamp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.data.settings.TimeMode
import com.canhamp.data.settings.VisMode
import com.canhamp.playback.NowPlaying
import com.canhamp.playback.dsp.SpectrumFrame
import com.canhamp.ui.components.Marquee
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.components.SegmentDigits
import com.canhamp.ui.components.SpectrumView
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

private val FreqLabels = listOf("60", "170", "310", "600", "1K", "3K", "6K", "12K", "16K")
private const val LEFT_WEIGHT = 5f
private const val RIGHT_WEIGHT = 7f

/** Module 2: the VFD display well (status row, timer, spectrum, marquee, badges). */
@Composable
fun DeckDisplay(
    state: MainUiState,
    frame: SpectrumFrame,
    onToggleTime: () -> Unit,
    onCycleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    val now = state.now
    val idle = stringResource(R.string.marquee_idle)
    val marquee = if (state.readError) stringResource(R.string.err_cannot_read) else DeckFormat.marqueeText(now, idle)

    RecessedWell(modifier.fillMaxWidth(), vfd = true, glare = true) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            StatusRow(state)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                TimerBlock(state, onToggleTime, Modifier.weight(LEFT_WEIGHT))
                SpectrumBlock(state, frame, onCycleMode, Modifier.weight(RIGHT_WEIGHT))
            }
            Marquee(
                marquee,
                Modifier.fillMaxWidth(),
                style = Deck.type.titleMd,
                color = if (state.readError) c.error else c.primaryContainer,
            )
            Badges(now)
        }
    }
}

@Composable
private fun StatusRow(state: MainUiState) {
    val c = Deck.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            VfdText(stringResource(R.string.deck_brand), style = Deck.type.labelMd)
            Text(stringResource(R.string.deck_pro), style = Deck.type.labelMd, color = c.secondaryContainer)
        }
        val status = when {
            state.now.buffering && state.hasQueue -> R.string.deck_buffering
            state.now.isPlaying -> R.string.deck_audio_on
            else -> null
        }
        if (status != null) VfdText(stringResource(status), style = Deck.type.labelMd)
    }
}

@Composable
private fun TimerBlock(state: MainUiState, onToggle: () -> Unit, modifier: Modifier) {
    val c = Deck.colors
    val now = state.now
    val modeLabel = stringResource(
        if (state.timeMode == TimeMode.ELAPSED) R.string.deck_time_elapsed else R.string.deck_time_remaining,
    )
    val toggleLabel = stringResource(R.string.deck_cd_timer)
    RecessedWell(
        modifier.clickable(onClickLabel = toggleLabel, role = Role.Button, onClick = onToggle),
        vfd = true,
        padding = PaddingValues(Dimens.spaceMd),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                DeckFormat.trackNumber(now.index, now.queueSize),
                style = Deck.type.labelMd,
                color = c.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            )
            // Polite live region on the label only: it changes on toggle, not every second.
            Text(
                modeLabel,
                style = Deck.type.labelMd,
                color = c.secondaryContainer,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            )
            SegmentDigits(DeckFormat.timerText(now, state.timeMode), Modifier.padding(vertical = Dimens.spaceSm))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LabelValue(stringResource(R.string.deck_kbps), DeckFormat.kbpsText(now.bitrateKbps))
                LabelValue(stringResource(R.string.deck_khz), DeckFormat.kHz(now.sampleRateHz))
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(label, style = Deck.type.labelSm, color = Deck.colors.secondaryContainer)
        VfdText(value, style = Deck.type.labelLg)
    }
}

@Composable
private fun SpectrumBlock(state: MainUiState, frame: SpectrumFrame, onCycleMode: () -> Unit, modifier: Modifier) {
    val c = Deck.colors
    val modeLabel = stringResource(if (state.visMode == VisMode.BAR) R.string.deck_mode_bar else R.string.deck_mode_wave)
    val modeDescription = stringResource(R.string.deck_cd_mode)
    RecessedWell(modifier, vfd = true) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.deck_spectrum), style = Deck.type.labelSm, color = c.primaryContainer, maxLines = 1)
                Box(
                    Modifier
                        .heightIn(min = Dimens.minTouch)
                        .clip(Deck.shapes.small)
                        .clickable(onClickLabel = modeDescription, role = Role.Button, onClick = onCycleMode)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modeLabel,
                        style = Deck.type.labelSm,
                        color = c.primaryContainer,
                        maxLines = 1,
                        modifier = Modifier.background(c.surfaceContainerHigh).padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
            SpectrumView(frame, state.visMode, state.visAnimated)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FreqLabels.forEach {
                    Text(it, style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun Badges(now: NowPlaying) {
    val stereo = (now.channelCount ?: 0) >= 2
    val mono = now.channelCount == 1
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
        Badge(
            stringResource(R.string.deck_bitrate),
            now.bitrateKbps?.let { "$it ${stringResource(R.string.deck_kbps)}" } ?: DeckFormat.NO_VALUE,
            lit = false,
            Modifier.weight(1f),
        )
        Badge(
            stringResource(R.string.deck_sampling),
            now.sampleRateHz?.let { "${DeckFormat.kHz(it)} ${stringResource(R.string.deck_khz)}" } ?: DeckFormat.NO_VALUE,
            lit = false,
            Modifier.weight(1f),
        )
        Badge(
            stringResource(R.string.deck_channels),
            stringResource(R.string.led_stereo),
            lit = stereo,
            Modifier.weight(1f),
        )
        Badge(
            stringResource(R.string.led_mono),
            if (mono) stringResource(R.string.led_on) else stringResource(R.string.deck_off),
            lit = mono,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun Badge(label: String, value: String, lit: Boolean, modifier: Modifier) {
    val c = Deck.colors
    Column(
        modifier
            .clip(Deck.shapes.small)
            .background(if (lit) c.surfaceVariant.copy(alpha = 0.6f) else c.surfaceContainerLow)
            .padding(vertical = Dimens.spaceMd, horizontal = Dimens.spaceSm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = Deck.type.labelSm,
            color = if (lit) c.primaryContainer else c.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            value,
            style = Deck.type.labelMd,
            color = if (lit) c.primaryContainer else c.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
