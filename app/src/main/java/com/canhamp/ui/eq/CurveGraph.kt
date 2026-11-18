package com.canhamp.ui.eq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import kotlin.math.log10

private const val SCALE_DB = 15f
private const val MIN_HZ = 20.0
private const val MAX_HZ = 20_000.0
private val GridHz = listOf(100.0, 1_000.0, 10_000.0)

/**
 * Decorative frequency-response curve (B45) on a ±15 dB scale: glow stroke plus gradient fill,
 * grey when [bypassed] (B44). [points] are (Hz, dB) pairs, log-spaced.
 */
@Composable
fun CurveGraph(points: List<Pair<Double, Double>>, bypassed: Boolean, modifier: Modifier = Modifier) {
    val c = Deck.colors
    val line = if (bypassed) c.outline else c.primaryContainer
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(Deck.shapes.small)
            .background(c.vfdWellDeep)
            .clearAndSetSemantics { },
    ) {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val w = size.width
            val h = size.height
            val grid = c.outlineVariant.copy(alpha = 0.6f)
            for (hz in GridHz) {
                val x = w * ((log10(hz) - log10(MIN_HZ)) / (log10(MAX_HZ) - log10(MIN_HZ))).toFloat()
                drawLine(grid, Offset(x, 0f), Offset(x, h), 1.dp.toPx())
            }
            for (i in 0..4) {
                val y = h * i / 4f
                drawLine(grid, Offset(0f, y), Offset(w, y), 1.dp.toPx())
            }
            // 0 dB line sits in the middle row.
            drawLine(c.outline.copy(alpha = 0.7f), Offset(0f, h / 2), Offset(w, h / 2), 1.dp.toPx())

            if (points.size < 2) return@Canvas
            fun yOf(db: Double) = h / 2 - (db.toFloat().coerceIn(-SCALE_DB, SCALE_DB) / SCALE_DB) * (h / 2)
            val stroke = Path()
            points.forEachIndexed { i, (_, db) ->
                val x = w * i / (points.size - 1)
                if (i == 0) stroke.moveTo(x, yOf(db)) else stroke.lineTo(x, yOf(db))
            }
            val fill = Path().apply {
                addPath(stroke)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.35f), Color.Transparent)))
            if (!bypassed) {
                drawPath(stroke, line.copy(alpha = 0.25f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            drawPath(stroke, line, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun CurveGraphPreview() = CanhAmpTheme {
    val pts = List(64) { i -> (20.0 + i) to 6.0 * kotlin.math.sin(i / 10.0) }
    Box(Modifier.padding(8.dp)) { CurveGraph(pts, bypassed = false) }
}
