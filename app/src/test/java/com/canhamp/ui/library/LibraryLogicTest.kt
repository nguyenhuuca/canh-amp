package com.canhamp.ui.library

import com.canhamp.data.db.GroupRow
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryLogicTest {
    private val row = TrackRow(1, "Around", "Daft Punk", "Homework", "mp3", 320, 44100, 9_856_000, 1, 1_700_000_000, "u")

    @Test fun allAndRecentIgnoreSelection() {
        assertEquals(QueryKind.All("%x%"), sourceFor(LibraryTab.ALL, "s", "%x%"))
        assertEquals(QueryKind.Recent(""), sourceFor(LibraryTab.RECENT, null, ""))
    }

    @Test fun groupTabsShowGroupsUntilSelected() {
        assertEquals(QueryKind.Groups(LibraryTab.FOLDERS), sourceFor(LibraryTab.FOLDERS, null, ""))
        assertEquals(QueryKind.Groups(LibraryTab.ARTISTS), sourceFor(LibraryTab.ARTISTS, null, "%a%"))
        assertEquals(QueryKind.Folder("/m", ""), sourceFor(LibraryTab.FOLDERS, "/m", ""))
        assertEquals(QueryKind.Artist("A", "%a%"), sourceFor(LibraryTab.ARTISTS, "A", "%a%"))
        assertEquals(QueryKind.Album("B", ""), sourceFor(LibraryTab.ALBUMS, "B", ""))
    }

    @Test fun filterGroupsMatchesIgnoringCase() {
        val g = listOf(GroupRow("Daft Punk", 3), GroupRow("Air", 1))
        assertEquals(g, filterGroups(g, " "))
        assertEquals(listOf(GroupRow("Daft Punk", 3)), filterGroups(g, "punk"))
        assertEquals(emptyList<GroupRow>(), filterGroups(g, "zzz"))
    }

    @Test fun permissionAfterRequest() {
        assertEquals(PermissionState.Granted, permissionAfterRequest(true, false))
        assertEquals(PermissionState.Needed, permissionAfterRequest(false, true))
        assertEquals(PermissionState.PermanentlyDenied, permissionAfterRequest(false, false))
    }

    @Test fun sizes() {
        assertEquals("0 B", formatSize(-5))
        assertEquals("512 B", formatSize(512))
        assertEquals("1.5 KB", formatSize(1536))
        assertEquals("9.4 MB", formatSize(9_856_000))
        assertEquals("2.0 GB", formatSize(2L * 1024 * 1024 * 1024))
    }

    @Test fun khz() {
        assertEquals("44.1", formatKhz(44_100))
        assertEquals("48", formatKhz(48_000))
        assertEquals("22.1", formatKhz(22_050))
        assertNull(formatKhz(null))
        assertNull(formatKhz(0))
    }

    @Test fun dates() {
        assertEquals("2023-11-14", formatDate(1_700_000_000, ZoneOffset.UTC))
        assertNull(formatDate(0, ZoneOffset.UTC))
    }

    @Test fun badges() {
        assertEquals(".MP3 · 320 KBPS · 44.1 KHZ · 9.4 MB", trackBadges(row))
        assertEquals(".FLAC · 9.4 MB", trackBadges(row.copy(format = "flac", bitrateKbps = null, sampleRateHz = null)))
    }

    @Test fun shortPathStripsStorageRoot() {
        assertEquals("/0/Music/Retro", shortPath("/storage/emulated/0/Music/Retro"))
        assertEquals("/sdcard2/x", shortPath("/sdcard2/x"))
    }

    private fun mode(
        permission: PermissionState = PermissionState.Granted,
        scanning: Boolean = false,
        total: Int = 10,
        ever: Boolean = true,
        query: Boolean = false,
        loaded: Boolean = true,
        visible: Int = 10,
    ) = listMode(permission, scanning, total, ever, query, loaded, visible)

    @Test fun listModes() {
        assertEquals(ListMode.Hidden, mode(permission = PermissionState.Needed))
        assertEquals(ListMode.Hidden, mode(permission = PermissionState.PermanentlyDenied))
        assertEquals(ListMode.Skeleton, mode(permission = PermissionState.Unknown))
        assertEquals(ListMode.Skeleton, mode(total = 0, scanning = true))
        assertEquals(ListMode.Skeleton, mode(total = 0, ever = false))
        assertEquals(ListMode.Empty, mode(total = 0))
        assertEquals(ListMode.NoMatch, mode(visible = 0, query = true))
        assertEquals(ListMode.Empty, mode(visible = 0))
        assertEquals(ListMode.Populated, mode(visible = 0, loaded = false))
        assertEquals(ListMode.Populated, mode())
        assertEquals(ListMode.Populated, mode(scanning = true))
    }
}
