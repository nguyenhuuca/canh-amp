package com.canhamp.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test fun mapsTags() {
        assertEquals(AppLanguage.VI, AppLanguage.fromTags("vi"))
        assertEquals(AppLanguage.VI, AppLanguage.fromTags("vi-VN"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTags("en-US,vi"))
    }

    @Test fun unknownOrEmptyIsSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("fr"))
    }

    @Test fun tagsRoundTrip() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTags(it.tag)) }
    }
}
