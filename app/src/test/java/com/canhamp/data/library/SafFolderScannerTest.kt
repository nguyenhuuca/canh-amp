package com.canhamp.data.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafFolderScannerTest {

    /** A tree built from a map of directory id to children; ids double as paths. */
    private class FakeTree(private val dirs: Map<String, List<DocEntry>>) : DocumentTree {
        override val rootDocumentId = "root"
        var listCalls = 0
        override fun displayName() = "Music"
        override fun children(documentId: String): List<DocEntry> {
            listCalls++
            return dirs[documentId].orEmpty()
        }

        override fun documentUri(documentId: String) = "content://tree/document/$documentId"
    }

    private fun file(id: String, name: String) = DocEntry(id, name, "audio/mpeg", 1_000, 5_000, isDir = false)
    private fun dir(id: String, name: String) = DocEntry(id, name, "vnd.android.document/directory", 0, 0, isDir = true)

    @Test fun keepsOnlySupportedExtensionsCaseInsensitive() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(
                    file("1", "a.mp3"), file("2", "b.FLAC"), file("3", "c.ogg"), file("4", "d.opus"),
                    file("5", "e.m4a"), file("6", "f.aac"), file("7", "g.wav"),
                    file("8", "cover.jpg"), file("9", "notes.txt"), file("10", "noext"), file("11", "x.mp3.bak"),
                ),
            ),
        )
        val out = SafFolderScanner(tree).scan()
        assertEquals(listOf("a.mp3", "b.FLAC", "c.ogg", "d.opus", "e.m4a", "f.aac", "g.wav"), out.map { it.name })
        assertEquals("content://tree/document/1", out[0].uri)
        assertEquals(1_000L, out[0].size)
    }

    @Test fun walksNestedDirsWithRelativePaths() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(file("1", "top.mp3"), dir("d1", "Rock")),
                "d1" to listOf(file("2", "r.mp3"), dir("d2", "Live")),
                "d2" to listOf(file("3", "l.mp3")),
            ),
        )
        val out = SafFolderScanner(tree).scan().associate { it.name to it.relativeDir }
        assertEquals(mapOf("top.mp3" to "", "r.mp3" to "Rock", "l.mp3" to "Rock/Live"), out)
    }

    @Test fun visitsBreadthFirst() {
        val tree = FakeTree(
            mapOf(
                "root" to listOf(dir("a", "A"), file("1", "root.mp3"), dir("b", "B")),
                "a" to listOf(dir("a2", "A2"), file("2", "a.mp3")),
                "b" to listOf(file("3", "b.mp3")),
                "a2" to listOf(file("4", "deep.mp3")),
            ),
        )
        assertEquals(listOf("root.mp3", "a.mp3", "b.mp3", "deep.mp3"), SafFolderScanner(tree).scan().map { it.name })
    }

    @Test fun depthCapStopsBelowEightLevels() {
        // root is level 0; d1..d9 chain, each holding one file.
        val dirs = HashMap<String, List<DocEntry>>()
        dirs["root"] = listOf(file("f0", "f0.mp3"), dir("d1", "d1"))
        for (i in 1..9) {
            val children = mutableListOf(file("f$i", "f$i.mp3"))
            if (i < 9) children.add(dir("d${i + 1}", "d${i + 1}"))
            dirs["d$i"] = children
        }
        val names = SafFolderScanner(FakeTree(dirs)).scan().map { it.name }
        assertEquals((0..SafFolderScanner.MAX_DEPTH).map { "f$it.mp3" }, names)
        assertTrue("f9.mp3" !in names)
    }

    @Test fun fileCapStopsAtTenThousand() {
        val many = (1..SafFolderScanner.MAX_FILES + 500).map { file("$it", "t$it.mp3") }
        val out = SafFolderScanner(FakeTree(mapOf("root" to many))).scan()
        assertEquals(SafFolderScanner.MAX_FILES, out.size)
        assertEquals("t1.mp3", out.first().name)
    }

    @Test fun fileCapStopsFurtherDirectories() {
        val many = (1..SafFolderScanner.MAX_FILES).map { file("$it", "t$it.mp3") }
        val tree = FakeTree(mapOf("root" to many + dir("d", "D"), "d" to listOf(file("x", "late.mp3"))))
        val out = SafFolderScanner(tree).scan()
        assertEquals(SafFolderScanner.MAX_FILES, out.size)
        assertEquals(1, tree.listCalls)
    }

    @Test fun emptyTreeGivesNothing() {
        assertTrue(SafFolderScanner(FakeTree(emptyMap())).scan().isEmpty())
    }

    @Test fun reportsProgress() {
        val tree = FakeTree(mapOf("root" to listOf(file("1", "a.mp3"), dir("d", "D")), "d" to listOf(file("2", "b.mp3"))))
        val seen = ArrayList<Int>()
        SafFolderScanner(tree).scan { seen.add(it) }
        assertEquals(listOf(1, 2), seen)
    }

    @Test fun slashInDirNameIsNeutralised() {
        val tree = FakeTree(mapOf("root" to listOf(dir("d", "a/b")), "d" to listOf(file("1", "x.mp3"))))
        assertEquals("a_b", SafFolderScanner(tree).scan().single().relativeDir)
    }
}
