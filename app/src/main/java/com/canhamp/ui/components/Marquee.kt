package com.canhamp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck

private val MarqueeVelocity = 30.dp

/** Single-line scrolling text; static and ellipsized when animations are off. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Marquee(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Deck.type.titleMd,
    color: Color = Deck.colors.onSurface,
) {
    val reduced = rememberReducedMotion()
    Text(
        text = text,
        color = color,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .semantics { contentDescription = text }
            .then(
                if (reduced) Modifier
                else Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = MarqueeVelocity),
            ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF101417, widthDp = 160)
@Composable
private fun MarqueePreview() = CanhAmpTheme {
    Marquee("A very long track title that does not fit", Modifier.padding(8.dp))
}
