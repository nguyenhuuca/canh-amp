package com.canhamp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ARC_START = 135f
private const val ARC_SWEEP = 270f
private val DragPerStep = 8.dp

/**
 * Rotary knob for a 0..[max] whole-dB value, drawn as a ring arc. Vertical drag turns it
 * (up raises), double tap resets to 0.
 */
@Composable
fun Knob(
    value: Int,
    onValueChange: (Int) -> Unit,
    contentDescription: String,
    stateDescription: String,
    modifier: Modifier = Modifier,
    max: Int = 12,
    size: Dp = 56.dp,
    accent: Color = Deck.colors.primaryContainer,
) {
    val c = Deck.colors
    val haptic = LocalHapticFeedback.current
    val current = value.coerceIn(0, max)
    val latest by rememberUpdatedState(current)
    val onChange by rememberUpdatedState(onValueChange)
    val carry = remember { mutableFloatStateOf(0f) }

    fun commit(next: Int) {
        val clamped = next.coerceIn(0, max)
        if (clamped == latest) return
        haptic.performHapticFeedback(
            if (clamped == 0) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
        )
        onChange(clamped)
    }

    Box(
        modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                this.stateDescription = stateDescription
                progressBarRangeInfo = ProgressBarRangeInfo(current.toFloat(), 0f..max.toFloat(), max - 1)
                setProgress { target ->
                    commit(target.roundToInt())
                    true
                }
            }
            .pointerInput(max) { detectTapGestures(onDoubleTap = { commit(0) }) }
            .pointerInput(max) {
                val stepPx = DragPerStep.toPx()
                detectVerticalDragGestures(onDragStart = { carry.floatValue = 0f }) { change, dy ->
                    change.consume()
                    carry.floatValue -= dy
                    val steps = (carry.floatValue / stepPx).toInt()
                    if (steps != 0) {
                        carry.floatValue -= steps * stepPx
                        commit(latest + steps)
                    }
                }
            },
    ) {
        Canvas(Modifier.size(size)) {
            val ring = 6.dp.toPx()
            val arcSize = Size(this.size.width - ring, this.size.height - ring)
            val topLeft = Offset(ring / 2, ring / 2)
            drawArc(c.surfaceContainerLowest, ARC_START, ARC_SWEEP, false, topLeft, arcSize, style = Stroke(ring, cap = StrokeCap.Round))
            val sweep = ARC_SWEEP * current / max
            if (sweep > 0f) {
                drawArc(accent, ARC_START, sweep, false, topLeft, arcSize, style = Stroke(ring, cap = StrokeCap.Round))
            }
            // Cap with a pointer dot at the current angle.
            val capR = (this.size.minDimension - 2 * ring - 4.dp.toPx()) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            drawCircle(Brush.verticalGradient(listOf(c.buttonTop, c.buttonBottom)), capR, center)
            val angle = Math.toRadians((ARC_START + sweep).toDouble())
            val dot = Offset(
                center.x + (capR - 6.dp.toPx()) * cos(angle).toFloat(),
                center.y + (capR - 6.dp.toPx()) * sin(angle).toFloat(),
            )
            drawCircle(accent, 2.5.dp.toPx(), dot)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun KnobPreview() = CanhAmpTheme {
    Knob(value = 6, onValueChange = {}, contentDescription = "Bass", stateDescription = "6 decibels")
}
