package com.canhamp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck

/** Large glowing timer digits in the monospace display face. */
@Composable
fun SegmentDigits(text: String, modifier: Modifier = Modifier) {
    VfdText(
        text = text,
        modifier = modifier,
        style = Deck.type.displayLgMobile.copy(fontSize = 34.sp, lineHeight = 38.sp, textAlign = TextAlign.Center),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun SegmentDigitsPreview() = CanhAmpTheme {
    SegmentDigits("00:05", Modifier.padding(8.dp))
}
