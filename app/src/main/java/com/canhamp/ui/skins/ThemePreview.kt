package com.canhamp.ui.skins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.DeckColors
import com.canhamp.util.formatDuration

private const val TRANSPORT_KEYS = 5
private const val MINI_SEGMENTS = 8

/**
 * A decorative mini deck drawn with [palette]: top bar, LCD with the current time and title (or the idle
 * text), six spectrum bars and a transport row. It does not use the active theme, so every card shows its own.
 */
@Composable
fun ThemePreview(
    palette: DeckColors,
    now: PreviewNow?,
    bars: FloatArray,
    modifier: Modifier = Modifier,
) {
    val shape = Deck.shapes.medium
    val idle = stringResource(R.string.skins_preview_idle)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.outlineVariant, shape)
            .padding(8.dp)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(Deck.shapes.small)
                .background(palette.surfaceContainerHigh)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { Box(Modifier.size(5.dp).clip(Deck.shapes.small).background(palette.outline)) }
            Text(
                stringResource(R.string.deck_brand),
                style = Deck.type.labelSm,
                color = palette.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().clip(Deck.shapes.small).background(palette.vfdWell).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (now != null) formatDuration(now.positionMs) else "--:--",
                    style = Deck.type.titleMd,
                    color = palette.primaryContainer,
                    maxLines = 1,
                )
                Text(
                    now?.title?.takeIf { it.isNotBlank() } ?: idle,
                    style = Deck.type.labelSm,
                    color = palette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MiniSpectrum(palette, bars, Modifier.weight(1f).height(28.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(TRANSPORT_KEYS) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(Deck.shapes.small)
                        .background(Brush.verticalGradient(listOf(palette.buttonTop, palette.buttonBottom)))
                        .border(1.dp, palette.bevelHighlight, Deck.shapes.small),
                )
            }
        }
    }
}

@Composable
private fun MiniSpectrum(palette: DeckColors, bars: FloatArray, modifier: Modifier) {
    Canvas(modifier) {
        val n = bars.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        val seg = size.height / MINI_SEGMENTS
        val segGap = 1.dp.toPx()
        for (b in 0 until n) {
            val lit = Math.round(bars.getOrElse(b) { 0f }.coerceIn(0f, 1f) * MINI_SEGMENTS)
            for (i in 0 until lit) {
                val color = when {
                    i >= MINI_SEGMENTS - 1 -> palette.tertiary
                    i >= MINI_SEGMENTS - 3 -> palette.secondaryContainer
                    else -> palette.primaryContainer
                }
                drawRoundRect(
                    color,
                    Offset(b * (w + gap), size.height - (i + 1) * seg + segGap),
                    Size(w, seg - segGap),
                    CornerRadius(1.dp.toPx()),
                )
            }
        }
    }
}
