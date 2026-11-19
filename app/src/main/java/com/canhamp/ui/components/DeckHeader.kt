package com.canhamp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.ui.theme.glowShadow

/** Top plate: app mark, title over the screen subtitle, and the STEREO/MONO LED. [stereo] null dims the LED. */
@Composable
fun DeckHeader(
    subtitle: String,
    stereo: Boolean?,
    modifier: Modifier = Modifier,
    subtitleError: Boolean = false,
) {
    val c = Deck.colors
    Box(
        modifier
            .fillMaxWidth()
            .background(c.surfaceContainer)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .height(Dimens.headerHeight)
                .padding(horizontal = Dimens.margin),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppMark(Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_tagline),
                    style = Deck.type.titleMd.copy(shadow = glowShadow(c.glow)),
                    color = c.primaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle.uppercase(),
                    style = Deck.type.labelSm,
                    color = if (subtitleError) c.error else c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { if (subtitleError) liveRegion = LiveRegionMode.Polite },
                )
            }
            StereoPill(stereo)
        }
    }
}

/** Decorative mark: a ring with three level bars, in phosphor. */
@Composable
private fun AppMark(modifier: Modifier = Modifier) {
    val color = Deck.colors.primaryContainer
    Canvas(modifier) {
        val stroke = 2.dp.toPx()
        drawCircle(color, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        val barW = 3.dp.toPx()
        val gap = 1.5.dp.toPx()
        listOf(0.25f, 0.45f, 0.32f).forEachIndexed { i, f ->
            val h = size.height * f
            val x = size.width * 0.5f + (i - 1) * (barW + gap) - barW / 2
            drawRect(color, Offset(x, size.height * 0.5f - h / 2), Size(barW, h))
        }
    }
}

@Composable
private fun StereoPill(stereo: Boolean?) {
    val c = Deck.colors
    val label = stringResource(if (stereo == false) R.string.led_mono else R.string.led_stereo)
    val lit = stereo != null
    Row(
        Modifier
            .alpha(if (lit) 1f else 0.4f)
            .clip(Deck.shapes.medium)
            .background(c.surfaceContainerLowest)
            .border(1.dp, c.bevelShadow, Deck.shapes.medium)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            Modifier.size(6.dp).clip(CircleShape).background(if (lit) c.primaryContainer else c.surfaceVariant),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun DeckHeaderPreview() = CanhAmpTheme {
    DeckHeader(subtitle = "Main player", stereo = true)
}
