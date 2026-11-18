package com.canhamp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import kotlin.math.roundToInt

val VFaderHeight = 144.dp
private val ThumbWidth = 24.dp
private val ThumbHeight = 16.dp
private val GrooveWidth = 8.dp

/**
 * Maps a touch y (0 at the top) to a whole-dB value in [min]..[max]: the top edge is [max].
 * [travel] is the track height minus the thumb, so the thumb centre follows the finger.
 */
fun faderStepFromY(y: Float, travel: Float, min: Int, max: Int): Int {
    if (travel <= 0f) return min
    val fraction = (1f - (y / travel)).coerceIn(0f, 1f)
    return (min + fraction * (max - min)).roundToInt().coerceIn(min, max)
}

/**
 * Vertical fader in whole steps (1 dB on the equalizer). Drag to move, double tap to reset to 0.
 * The touch area is the full width the parent gives it and [VFaderHeight] tall.
 */
@Composable
fun VFader(
    value: Int,
    onValueChange: (Int) -> Unit,
    contentDescription: String,
    stateDescription: String,
    modifier: Modifier = Modifier,
    min: Int = -12,
    max: Int = 12,
    accent: Color = Deck.colors.primaryContainer,
) {
    val c = Deck.colors
    val haptic = LocalHapticFeedback.current
    val current = value.coerceIn(min, max)
    val latest by rememberUpdatedState(current)
    val onChange by rememberUpdatedState(onValueChange)

    fun commit(next: Int) {
        val clamped = next.coerceIn(min, max)
        if (clamped == latest) return
        haptic.performHapticFeedback(
            if (clamped == 0) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
        )
        onChange(clamped)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(VFaderHeight)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                this.stateDescription = stateDescription
                progressBarRangeInfo = ProgressBarRangeInfo(current.toFloat(), min.toFloat()..max.toFloat(), max - min - 1)
                setProgress { target ->
                    commit(target.roundToInt())
                    true
                }
            }
            .pointerInput(min, max) {
                detectTapGestures(onDoubleTap = { commit(0) })
            }
            .pointerInput(min, max) {
                val thumb = ThumbHeight.toPx()
                fun stepAt(y: Float) = faderStepFromY(y - thumb / 2, size.height - thumb, min, max)
                detectVerticalDragGestures(
                    onDragStart = { commit(stepAt(it.y)) },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        commit(stepAt(change.position.y))
                    },
                )
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        // Recessed groove with a centre mark and a fill from 0 towards the value.
        Box(
            Modifier
                .height(VFaderHeight)
                .width(GrooveWidth)
                .clip(Deck.shapes.small)
                .background(c.surfaceContainerLowest)
                .border(1.dp, c.bevelShadow, Deck.shapes.small)
                .drawBehind {
                    val thumb = ThumbHeight.toPx()
                    val travel = size.height - thumb
                    fun centerY(v: Int) = thumb / 2 + travel * (1f - (v - min).toFloat() / (max - min))
                    val zeroY = centerY(0)
                    val valueY = centerY(current)
                    val top = minOf(zeroY, valueY)
                    val inset = 2.dp.toPx()
                    if (current != 0) {
                        drawRoundRect(
                            accent,
                            Offset(inset, top),
                            Size(size.width - 2 * inset, kotlin.math.abs(valueY - zeroY)),
                            CornerRadius(1.dp.toPx()),
                        )
                    }
                    drawLine(c.outline, Offset(0f, zeroY), Offset(size.width, zeroY), 1.dp.toPx())
                },
        )
        // Machined thumb.
        val travelDp = VFaderHeight - ThumbHeight
        val y = travelDp * (1f - (current - min).toFloat() / (max - min))
        Box(
            Modifier
                .offset(y = y)
                .size(ThumbWidth, ThumbHeight)
                .clip(Deck.shapes.small)
                .background(Brush.verticalGradient(listOf(c.buttonTop, c.buttonBottom)))
                .border(1.dp, c.bevelHighlight, Deck.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(2.dp).background(accent))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun VFaderPreview() = CanhAmpTheme {
    Box(Modifier.width(32.dp).padding(4.dp)) {
        VFader(value = 5, onValueChange = {}, contentDescription = "60 hertz", stateDescription = "plus 5 decibels")
    }
}
