package com.canhamp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.data.settings.VisMode
import com.canhamp.playback.dsp.SpectrumFrame
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.eq

private const val SEGMENTS = 16
private const val GREEN_SEGMENTS = 10
private const val AMBER_SEGMENTS = 4
private val SegmentHeight = 2.dp
private val SegmentGap = 1.dp
private val ColumnGap = 2.dp

/** Number of lit segments for a 0..1 level, clamped to 0..[SEGMENTS]. */
fun litSegments(level: Float): Int =
    if (level.isNaN()) 0 else Math.round(level.coerceIn(0f, 1f) * SEGMENTS)

/**
 * Decorative 16-band visualizer on one Canvas. BAR draws segmented columns with a peak dot; WAVE draws a
 * polyline. With [animated] off only the peak dots are drawn (B26).
 */
@Composable
fun SpectrumView(
    frame: SpectrumFrame,
    mode: VisMode,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    val eq = Deck.eq
    val off = c.surfaceVariant.copy(alpha = 0.35f)
    val height = (SegmentHeight + SegmentGap) * SEGMENTS
    Canvas(modifier.fillMaxWidth().height(height).clearAndSetSemantics { }) {
        if (mode == VisMode.WAVE) {
            drawWave(frame.wave, eq.green, animated)
        } else {
            drawBars(frame, animated, off, eq.green, eq.amber, eq.orange)
        }
    }
}

private fun segmentColor(i: Int, green: Color, amber: Color, orange: Color): Color = when {
    i < GREEN_SEGMENTS -> green
    i < GREEN_SEGMENTS + AMBER_SEGMENTS -> amber
    else -> orange
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBars(
    frame: SpectrumFrame,
    animated: Boolean,
    off: Color,
    green: Color,
    amber: Color,
    orange: Color,
) {
    val bands = SpectrumFrame.BANDS
    val gap = ColumnGap.toPx()
    val colW = (size.width - gap * (bands - 1)) / bands
    val segH = SegmentHeight.toPx()
    val step = segH + SegmentGap.toPx()
    for (b in 0 until bands) {
        val x = b * (colW + gap)
        val lit = if (animated) litSegments(frame.bands.getOrElse(b) { 0f }) else 0
        val peak = litSegments(frame.peaks.getOrElse(b) { 0f })
        for (i in 0 until SEGMENTS) {
            val y = size.height - i * step - segH
            val isPeak = peak > 0 && i == peak - 1
            val color = when {
                i < lit -> segmentColor(i, green, amber, orange)
                isPeak -> segmentColor(i, green, amber, orange)
                animated -> off
                else -> Color.Transparent
            }
            if (color != Color.Transparent) drawRect(color, Offset(x, y), Size(colW, segH))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(
    wave: FloatArray,
    color: Color,
    animated: Boolean,
) {
    val mid = size.height / 2
    if (!animated || wave.size < 2) {
        drawLine(color.copy(alpha = 0.4f), Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
        return
    }
    val path = Path()
    val dx = size.width / (wave.size - 1)
    for (i in wave.indices) {
        val y = mid - wave[i].coerceIn(-1f, 1f) * mid
        if (i == 0) path.moveTo(0f, y) else path.lineTo(i * dx, y)
    }
    drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun SpectrumViewPreview() = CanhAmpTheme {
    val bands = FloatArray(SpectrumFrame.BANDS) { (it % 8) / 8f }
    SpectrumView(SpectrumFrame(bands, bands, FloatArray(SpectrumFrame.WAVE_SIZE)), VisMode.BAR, animated = true)
}
