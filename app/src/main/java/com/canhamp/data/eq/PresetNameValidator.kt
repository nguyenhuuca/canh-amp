package com.canhamp.data.eq

import java.text.Normalizer

/** Outcome of checking a user-typed preset name (B48). */
sealed interface NameCheck {
    /** [name] is trimmed and NFC-normalised, ready to store. */
    data class Valid(val name: String) : NameCheck
    data object InvalidName : NameCheck
    data object BuiltInName : NameCheck
}

object PresetNameValidator {
    const val MAX_LENGTH = 24

    fun check(raw: String): NameCheck {
        val name = Normalizer.normalize(raw, Normalizer.Form.NFC).trim()
        val length = name.codePointCount(0, name.length)
        if (length !in 1..MAX_LENGTH) return NameCheck.InvalidName
        if (!name.codePoints().allMatch(::allowed)) return NameCheck.InvalidName
        if (BuiltInPresets.isBuiltInName(name)) return NameCheck.BuiltInName
        return NameCheck.Valid(name)
    }

    private fun allowed(cp: Int): Boolean =
        Character.isLetter(cp) || Character.isDigit(cp) || cp == ' '.code || cp == '-'.code || cp == '_'.code
}
