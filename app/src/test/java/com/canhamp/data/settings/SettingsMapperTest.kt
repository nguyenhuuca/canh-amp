package com.canhamp.data.settings

import com.canhamp.ui.theme.ThemeId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SettingsMapperTest {
    @Test fun emptyMapGivesDefaults() {
        assertEquals(Settings(), SettingsMapper.fromPreferences(emptyMap()))
    }

    @Test fun preserveEqColorsDefaultsOffAndIsRead() {
        assertEquals(false, SettingsMapper.fromPreferences(emptyMap()).preserveEqColors)
        assertEquals(true, SettingsMapper.fromPreferences(mapOf("preserveEqColors" to true)).preserveEqColors)
        assertEquals(false, SettingsMapper.fromPreferences(mapOf("preserveEqColors" to "yes")).preserveEqColors)
    }

    @Test fun parseGainsValid() {
        assertArrayEquals(intArrayOf(1, -2, 3, 4, 5, 6, 7, 8, 9, 10), SettingsMapper.parseGains("1,-2,3,4,5,6,7,8,9,10"))
    }

    @Test fun parseGainsClamps() {
        assertArrayEquals(intArrayOf(12, -12, 0, 0, 0, 0, 0, 0, 0, 0), SettingsMapper.parseGains("99,-99,0,0,0,0,0,0,0,0"))
    }

    @Test fun parseGainsInvalidGivesZeros() {
        val zeros = IntArray(10)
        assertArrayEquals(zeros, SettingsMapper.parseGains(null))
        assertArrayEquals(zeros, SettingsMapper.parseGains(""))
        assertArrayEquals(zeros, SettingsMapper.parseGains("1,2,3"))
        assertArrayEquals(zeros, SettingsMapper.parseGains("1,2,3,4,5,6,7,8,9,x"))
        assertArrayEquals(zeros, SettingsMapper.parseGains("1,2,3,4,5,6,7,8,9,10,11"))
    }

    @Test fun formatRoundTrip() {
        val g = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, -9)
        assertEquals("0,1,2,3,4,5,6,7,8,-9", SettingsMapper.formatGains(g))
        assertArrayEquals(g, SettingsMapper.parseGains(SettingsMapper.formatGains(g)))
    }

    @Test fun badEnumStringsFallBackToDefaults() {
        val s = SettingsMapper.fromPreferences(
            mapOf("theme" to "NOPE", "repeat" to "x", "timeMode" to "", "visMode" to 5, "mainMode" to "BOGUS"),
        )
        assertEquals(ThemeId.CLASSIC, s.theme)
        assertEquals(RepeatSetting.OFF, s.repeat)
        assertEquals(TimeMode.ELAPSED, s.timeMode)
        assertEquals(VisMode.BAR, s.visMode)
        assertEquals(MainMode.DECK, s.mainMode)
    }

    @Test fun valuesAreReadAndClamped() {
        val s = SettingsMapper.fromPreferences(
            mapOf(
                "theme" to "CYBER_NEON", "repeat" to "ALL", "volume" to 3f, "balance" to -4f,
                "bassDb" to 50, "queueIndex" to -3, "queuePositionMs" to 42L, "shuffle" to true,
            ),
        )
        assertEquals(ThemeId.CYBER_NEON, s.theme)
        assertEquals(RepeatSetting.ALL, s.repeat)
        assertEquals(1f, s.volume)
        assertEquals(-1f, s.balance)
        assertEquals(12, s.bassDb)
        assertEquals(0, s.queueIndex)
        assertEquals(42L, s.queuePositionMs)
        assertEquals(true, s.shuffle)
    }

    @Test fun mainModeIsRead() {
        assertEquals(MainMode.MINIMAL, SettingsMapper.fromPreferences(mapOf("mainMode" to "MINIMAL")).mainMode)
        assertEquals(MainMode.DECK, SettingsMapper.fromPreferences(emptyMap()).mainMode)
    }

    @Test fun mainModeTakesPartInEquality() {
        val a = Settings(mainMode = MainMode.DECK)
        val b = Settings(mainMode = MainMode.MINIMAL)
        assertNotEquals(a, b)
        assertEquals(b, Settings(mainMode = MainMode.MINIMAL))
        assertEquals(b.hashCode(), Settings(mainMode = MainMode.MINIMAL).hashCode())
    }
}
