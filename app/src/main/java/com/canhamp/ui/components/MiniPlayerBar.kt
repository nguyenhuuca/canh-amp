package com.canhamp.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.util.formatDuration

@Immutable
data class MiniPlayerState(
    val title: String,
    val artist: String,
    val positionMs: Long,
    val isPlaying: Boolean,
)

/** Compact transport above the nav bar. Hidden when [state] is null (empty queue). */
@Composable
fun MiniPlayerBar(
    state: MiniPlayerState?,
    onOpen: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == null) return
    val c = Deck.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(Dimens.miniPlayerHeight)
            .background(c.surfaceContainerLow)
            .padding(horizontal = Dimens.margin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecessedWell(
            Modifier.weight(1f).height(32.dp).clickable(role = Role.Button, onClick = onOpen),
            padding = PaddingValues(horizontal = 8.dp),
        ) {
            Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                PlayPulse(state.isPlaying)
                Spacer(Modifier.width(8.dp))
                val line = buildAnnotatedString {
                    withStyle(SpanStyle(color = c.secondaryFixedDim)) {
                        append("[${formatDuration(state.positionMs)}] ")
                    }
                    append("${state.artist} - ${state.title}".uppercase())
                }
                Text(
                    line,
                    style = Deck.type.labelMd,
                    color = c.primaryFixed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        MiniButton(Icons.Filled.SkipPrevious, stringResource(R.string.cd_previous), onPrev)
        MiniButton(
            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play),
            onPlayPause,
            filled = true,
        )
        MiniButton(Icons.Filled.SkipNext, stringResource(R.string.cd_next), onNext)
    }
}

@Composable
private fun PlayPulse(playing: Boolean) {
    val reduced = rememberReducedMotion()
    val pulse = if (playing && !reduced) {
        val t = rememberInfiniteTransition(label = "miniPulse")
        val a by t.animateFloat(
            initialValue = 1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "miniPulseAlpha",
        )
        a
    } else {
        1f
    }
    Icon(
        Icons.Filled.PlayArrow,
        contentDescription = null,
        tint = Deck.colors.primaryContainer,
        modifier = Modifier.size(14.dp).alpha(if (playing) pulse else 0.4f),
    )
}

@Composable
private fun MiniButton(icon: ImageVector, description: String, onClick: () -> Unit, filled: Boolean = false) {
    val c = Deck.colors
    val view = LocalView.current
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClickLabel = description) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(Deck.shapes.medium)
                .background(
                    if (filled) {
                        Brush.verticalGradient(listOf(c.primaryContainer, c.primaryContainer))
                    } else {
                        Brush.verticalGradient(listOf(c.buttonTop, c.buttonBottom))
                    },
                )
                .border(1.dp, c.bevelHighlight, Deck.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (filled) c.onPhosphor else c.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun MiniPlayerBarPreview() = CanhAmpTheme {
    MiniPlayerBar(MiniPlayerState("Night Drive", "Neon Echo", 83_000, true), {}, {}, {}, {})
}
