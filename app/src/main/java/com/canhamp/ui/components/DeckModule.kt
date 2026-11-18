package com.canhamp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Raised module: light bevel on top/left, dark bevel on bottom/right. */
@Composable
fun DeckModule(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Dimens.spaceMd),
    content: @Composable () -> Unit,
) {
    val c = Deck.colors
    Box(
        modifier
            .clip(Deck.shapes.medium)
            .background(c.surfaceContainer)
            .drawBehind {
                val w = 1.dp.toPx()
                val h = w / 2
                drawLine(c.bevelHighlight, Offset(0f, h), Offset(size.width, h), w)
                drawLine(c.bevelHighlight, Offset(h, 0f), Offset(h, size.height), w)
                drawLine(c.bevelShadow, Offset(0f, size.height - h), Offset(size.width, size.height - h), w)
                drawLine(c.bevelShadow, Offset(size.width - h, 0f), Offset(size.width - h, size.height), w)
            }
            .padding(padding),
    ) { content() }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun DeckModulePreview() = CanhAmpTheme {
    DeckModule(Modifier.padding(8.dp)) { Text("MODULE", color = Deck.colors.onSurface) }
}
