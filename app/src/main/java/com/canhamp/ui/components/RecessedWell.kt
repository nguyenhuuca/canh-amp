package com.canhamp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Recessed display well with a simulated inner shadow and optional acrylic glare. */
@Composable
fun RecessedWell(
    modifier: Modifier = Modifier,
    vfd: Boolean = false,
    glare: Boolean = false,
    padding: PaddingValues = PaddingValues(Dimens.spaceMd),
    content: @Composable () -> Unit,
) {
    val c = Deck.colors
    Box(
        modifier
            .clip(Deck.shapes.medium)
            .background(if (vfd) c.vfdWell else c.surfaceContainerLowest)
            .drawWithContent {
                drawContent()
                val shadowH = minOf(4.dp.toPx(), size.height)
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent),
                        startY = 0f,
                        endY = shadowH,
                    ),
                    size = size.copy(height = shadowH),
                )
                if (glare) {
                    val glareH = size.height / 3
                    drawRect(
                        Brush.verticalGradient(
                            listOf(c.primaryContainer.copy(alpha = 0.10f), Color.Transparent),
                            startY = 0f,
                            endY = glareH,
                        ),
                        size = size.copy(height = glareH),
                    )
                }
            }
            .padding(padding),
    ) { content() }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun RecessedWellPreview() = CanhAmpTheme {
    RecessedWell(Modifier.padding(8.dp), vfd = true, glare = true) {
        VfdText("00:42")
    }
}
