package com.canhamp.data.eq

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetNameValidatorTest {

    private fun valid(raw: String) = PresetNameValidator.check(raw) is NameCheck.Valid

    @Test
    fun lengthBoundaries() {
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check(""))
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check("   "))
        assertEquals(NameCheck.Valid("a"), PresetNameValidator.check("a"))
        assertEquals(NameCheck.Valid("a".repeat(24)), PresetNameValidator.check("a".repeat(24)))
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check("a".repeat(25)))
    }

    @Test
    fun trimsBeforeCounting() {
        assertEquals(NameCheck.Valid("My Mix"), PresetNameValidator.check("  My Mix  "))
        assertEquals(NameCheck.Valid("a".repeat(24)), PresetNameValidator.check(" " + "a".repeat(24) + " "))
    }

    @Test
    fun allowedCharacters() {
        assertTrue(valid("Night-Mode_2"))
        assertTrue(valid("Nhạc Việt"))
        assertTrue(valid("Điệu Bolero"))
    }

    @Test
    fun decomposedVietnameseIsNormalised() {
        val decomposed = "Việt"
        assertEquals(NameCheck.Valid("Việt"), PresetNameValidator.check(decomposed))
    }

    @Test
    fun rejectsOtherCharacters() {
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check("Rock!"))
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check("a/b"))
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check("a,b"))
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check("tab\there"))
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check("loud 🔊"))
    }

    @Test
    fun emojiCountsAsOneCodePointButIsRejected() {
        assertEquals(NameCheck.InvalidName, PresetNameValidator.check("😀"))
    }

    @Test
    fun builtInNamesCollideIgnoringCase() {
        assertEquals(NameCheck.BuiltInName, PresetNameValidator.check("rock"))
        assertEquals(NameCheck.BuiltInName, PresetNameValidator.check("FULL BASS"))
        assertEquals(NameCheck.BuiltInName, PresetNameValidator.check("  Flat "))
        assertTrue(valid("Rock 2"))
    }

    private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
}
