package com.canhamp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

val LocalDeckColors = staticCompositionLocalOf { deckColorsFor(ThemeId.CLASSIC) }
val LocalDeckType = staticCompositionLocalOf { DeckType() }
val LocalDeckShapes = staticCompositionLocalOf { DeckShapes() }

object Deck {
    val colors: DeckColors
        @Composable @ReadOnlyComposable get() = LocalDeckColors.current
    val type: DeckType
        @Composable @ReadOnlyComposable get() = LocalDeckType.current
    val shapes: DeckShapes
        @Composable @ReadOnlyComposable get() = LocalDeckShapes.current
}

/** Soft glow for VFD-style text: pass as `TextStyle.shadow`. */
fun glowShadow(color: Color, blur: Float = 8f): Shadow =
    Shadow(color = color.copy(alpha = 0.6f), offset = Offset.Zero, blurRadius = blur)

@Composable
fun CanhAmpTheme(
    themeId: ThemeId = ThemeId.CLASSIC,
    content: @Composable () -> Unit,
) {
    val colors = remember(themeId) { deckColorsFor(themeId) }
    val scheme = remember(colors) {
        darkColorScheme(
            primary = colors.primaryContainer,
            onPrimary = colors.onPhosphor,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPhosphor,
            secondary = colors.secondaryFixedDim,
            onSecondary = colors.surfaceContainerLowest,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.onSurface,
            tertiary = colors.tertiary,
            onTertiary = colors.surfaceContainerLowest,
            background = colors.background,
            onBackground = colors.onSurface,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            surfaceTint = colors.primaryContainer,
            error = colors.error,
            onError = colors.surfaceContainerLowest,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.error,
            outline = colors.outline,
            outlineVariant = colors.outlineVariant,
            surfaceDim = colors.surfaceDim,
            surfaceBright = colors.surfaceBright,
            surfaceContainerLowest = colors.surfaceContainerLowest,
            surfaceContainerLow = colors.surfaceContainerLow,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainerHigh,
            surfaceContainerHighest = colors.surfaceContainerHighest,
        )
    }
    CompositionLocalProvider(
        LocalDeckColors provides colors,
        LocalDeckType provides remember { DeckType() },
        LocalDeckShapes provides remember { DeckShapes() },
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
