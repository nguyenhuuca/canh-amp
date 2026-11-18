package com.canhamp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LikeEscaperTest {
    @Test fun emptyStaysEmpty() = assertEquals("", likePattern(""))
    @Test fun blankStaysEmpty() = assertEquals("", likePattern("   "))
    @Test fun wrapsPlainText() = assertEquals("%daft%", likePattern("daft"))
    @Test fun trims() = assertEquals("%daft punk%", likePattern("  daft punk "))
    @Test fun escapesPercent() = assertEquals("%100\\%%", likePattern("100%"))
    @Test fun escapesUnderscore() = assertEquals("%a\\_b%", likePattern("a_b"))
    @Test fun escapesBackslash() = assertEquals("%a\\\\\\%%", likePattern("a\\%"))
    @Test fun capsAtOneHundredChars() = assertEquals("%" + "a".repeat(100) + "%", likePattern("a".repeat(150)))
    @Test fun capCountsBeforeEscaping() = assertEquals("%" + "\\%".repeat(100) + "%", likePattern("%".repeat(150)))
    @Test fun normalizeTrimsAndCaps() = assertEquals("a".repeat(100), normalizeQuery(" " + "a".repeat(120)))
}
