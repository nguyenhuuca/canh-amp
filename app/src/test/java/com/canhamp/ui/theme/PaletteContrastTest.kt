package com.canhamp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class PaletteContrastTest {

    private fun channel(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        return 0.2126 * channel((argb shr 16) and 0xFF) +
            0.7152 * channel((argb shr 8) and 0xFF) +
            0.0722 * channel(argb and 0xFF)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun readablePairsMeetAa() {
        for (id in ThemeId.entries) {
            val c = deckColorsFor(id)
            val pairs = mapOf(
                "onSurface/surface" to contrast(c.onSurface, c.surface),
                "onSurface/surfaceContainer" to contrast(c.onSurface, c.surfaceContainer),
                "onSurfaceVariant/surfaceContainer" to contrast(c.onSurfaceVariant, c.surfaceContainer),
                "primaryContainer/surfaceContainerLowest" to contrast(c.primaryContainer, c.surfaceContainerLowest),
                "onPhosphor/primaryContainer" to contrast(c.onPhosphor, c.primaryContainer),
                "secondaryFixedDim/surfaceContainer" to contrast(c.secondaryFixedDim, c.surfaceContainer),
            )
            println("$id min=" + "%.2f".format(pairs.values.min()))
            for ((name, ratio) in pairs) {
                assertTrue("$id $name contrast $ratio < 4.5", ratio >= 4.5)
            }
        }
    }

    @Test
    fun everyThemeHasPalette() {
        for (id in ThemeId.entries) assertNotNull(deckColorsFor(id))
    }

    @Test
    fun fromKeyDefaultsToClassic() {
        assertEquals(ThemeId.CLASSIC, ThemeId.fromKey(null))
        assertEquals(ThemeId.CLASSIC, ThemeId.fromKey("bogus"))
        for (id in ThemeId.entries) assertEquals(id, ThemeId.fromKey(id.key))
    }
}
