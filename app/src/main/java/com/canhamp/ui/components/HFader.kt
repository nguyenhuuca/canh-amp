package com.canhamp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck

private val ThumbWidth = 24.dp
private val ThumbHeight = 20.dp

/**
 * Maps a touch x position to a fader value in 0..1, clamped. When [steps] > 0 the value
 * snaps to the nearest of [steps] equal intervals.
 */
fun faderValueFromX(x: Float, width: Float, steps: Int): Float {
    if (width <= 0f) return 0f
    val v = (x / width).coerceIn(0f, 1f)
    return if (steps > 0) (Math.round(v * steps).toFloat() / steps).coerceIn(0f, 1f) else v
}

/**
 * Horizontal fader. [value] is 0..1. Drag or tap to set. [detent] draws a centre mark (balance).
 */
@Composable
fun HFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    steps: Int = 0,
    filled: Boolean = true,
    detent: Boolean = false,
    stateDescription: String? = null,
) {
    val c = Deck.colors
    val current = value.coerceIn(0f, 1f)
    val onChange by rememberUpdatedState(onValueChange)
    val onFinished by rememberUpdatedState(onValueChangeFinished)

    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                progressBarRangeInfo =ProgressBarRangeInfo(current, 0f..1f, if (steps > 0) steps - 1 else 0)
                if (stateDescription != null) this.stateDescription = stateDescription
                setProgress { target ->
                    onChange(faderValueFromX(target.coerceIn(0f, 1f), 1f, steps))
                    onFinished()
                    true
                }
            }
            .pointerInput(steps) {
                // Usable travel is the track width minus the thumb, so the thumb centre follows the finger.
                val thumb = ThumbWidth.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val travel = size.width - thumb
                    fun valueAt(x: Float) = faderValueFromX(x - thumb / 2, travel, steps)
                    onChange(valueAt(down.position.x))
                    horizontalDrag(down.id) { change ->
                        change.consume()
                        onChange(valueAt(change.position.x))
                    }
                    onFinished()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Recessed groove.
        Box(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(Deck.shapes.medium)
                .background(c.surfaceContainerLowest)
                .border(1.dp, c.bevelShadow, Deck.shapes.medium)
                .drawBehind {
                    val trackH = 6.dp.toPx()
                    val inset = ThumbWidth.toPx() / 2
                    val top = (size.height - trackH) / 2
                    val trackW = size.width - 2 * inset
                    drawRoundRect(c.surfaceVariant, Offset(inset, top), Size(trackW, trackH), CornerRadius(trackH / 2))
                    if (filled) {
                        drawRoundRect(
                            Brush.horizontalGradient(listOf(c.primaryFixedDim, c.primaryContainer)),
                            Offset(inset, top),
                            Size(trackW * current, trackH),
                            CornerRadius(trackH / 2),
                        )
                    }
                    if (detent) {
                        val x = size.width / 2
                        drawLine(c.outline, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                    }
                },
        )
        // Machined thumb, positioned along the usable travel.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val x = (maxWidth - ThumbWidth) * current
            Box(
                Modifier
                    .padding(start = x)
                    .size(ThumbWidth, ThumbHeight)
                    .clip(Deck.shapes.small)
                    .background(Brush.verticalGradient(listOf(c.buttonTop, c.buttonBottom)))
                    .border(1.dp, c.bevelHighlight, Deck.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(1.dp).height(12.dp).background(c.bevelShadow))
                    Box(Modifier.width(2.dp).height(14.dp).background(c.secondaryContainer))
                    Box(Modifier.width(1.dp).height(12.dp).background(c.bevelShadow))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun HFaderPreview() = CanhAmpTheme {
    Box(Modifier.padding(8.dp)) { HFader(value = 0.5f, onValueChange = {}, contentDescription = "Balance", detent = true) }
}
