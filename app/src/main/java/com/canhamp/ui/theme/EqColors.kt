package com.canhamp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The three colours the spectrum and the EQ curve draw with: normal level, mid level, clip. */
@Immutable
data class EqColors(val green: Color, val amber: Color, val orange: Color)

/** B68: with [preserve] on, the CLASSIC colours are used whatever [themed] palette is active. */
fun eqColors(preserve: Boolean, themed: DeckColors): EqColors {
    val source = if (preserve) deckColorsFor(ThemeId.CLASSIC) else themed
    return EqColors(source.primaryContainer, source.secondaryContainer, source.tertiary)
}

val LocalPreserveEqColors = staticCompositionLocalOf { false }

/** The spectrum and EQ curve colours for the current theme and the "preserve EQ colours" setting. */
val Deck.eq: EqColors
    @Composable @ReadOnlyComposable get() = eqColors(LocalPreserveEqColors.current, LocalDeckColors.current)
