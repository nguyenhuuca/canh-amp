package com.canhamp.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesParserTest {
    @Test fun parsesSectionsAndBullets() {
        val r = parseReleaseNotes("## 1.0 - d\n- a\n-  b \n\n## 0.9\n- c\n")
        assertEquals(listOf(ReleaseSection("1.0 - d", listOf("a", "b")), ReleaseSection("0.9", listOf("c"))), r)
    }

    @Test fun ignoresStrayLinesAndOrphanBullets() {
        val r = parseReleaseNotes("- orphan\nhello\n# h1\n## \n## T\ntext\n- x\r\n")
        assertEquals(listOf(ReleaseSection("T", listOf("x"))), r)
    }

    @Test fun emptyInput() {
        assertTrue(parseReleaseNotes("").isEmpty())
    }

    @Test fun capsSections() {
        val text = (1..300).joinToString("\n") { "## v$it\n- b" }
        assertEquals(200, parseReleaseNotes(text).size)
    }
}
