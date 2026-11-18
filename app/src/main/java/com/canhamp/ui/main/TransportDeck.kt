package com.canhamp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Module 5: six tactile keys. PLAY is latched while playing, PAUSE while paused with a queue (B22). */
@Composable
fun TransportDeck(
    state: MainUiState,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    val playing = state.now.isPlaying
    val hasQueue = state.hasQueue
    DeckModule(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.deck_transport),
                    style = Deck.type.labelSm,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (playing) {
                    Spacer(Modifier.size(8.dp).clip(CircleShape).background(c.primaryContainer))
                    Spacer(Modifier.width(Dimens.spaceSm))
                    Text(stringResource(R.string.deck_motor), style = Deck.type.labelSm, color = c.primaryContainer, maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                val keyModifier = Modifier.weight(1f)
                TactileButton(stringResource(R.string.btn_prev), onPrev, keyModifier, Icons.Filled.SkipPrevious, enabled = hasQueue)
                TactileButton(
                    stringResource(R.string.btn_play), onPlay, keyModifier, Icons.Filled.PlayArrow,
                    active = playing, enabled = hasQueue,
                )
                TactileButton(
                    stringResource(R.string.btn_pause), onPause, keyModifier, Icons.Filled.Pause,
                    active = !playing && hasQueue && state.now.positionMs > 0L, enabled = hasQueue,
                )
                TactileButton(stringResource(R.string.btn_stop), onStop, keyModifier, Icons.Filled.Stop, enabled = hasQueue)
                TactileButton(stringResource(R.string.btn_next), onNext, keyModifier, Icons.Filled.SkipNext, enabled = hasQueue)
                TactileButton(stringResource(R.string.btn_open), onOpen, keyModifier, Icons.Filled.Eject)
            }
        }
    }
}
