package com.canhamp.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.glowShadow

/** Phosphor text with a soft glow, like a vacuum-fluorescent display. */
@Composable
fun VfdText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Deck.type.displayLgMobile,
    color: Color = Deck.colors.primaryContainer,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style.copy(shadow = glowShadow(Deck.colors.glow)),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun VfdTextPreview() = CanhAmpTheme {
    VfdText("03:45", Modifier.padding(8.dp))
}
