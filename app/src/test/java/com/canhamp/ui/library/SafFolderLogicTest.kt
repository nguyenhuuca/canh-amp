package com.canhamp.ui.library

import com.canhamp.data.db.SafFolderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafFolderLogicTest {
    private val music = SafFolderEntity(1, "content://t/music", "Music", 1)
    private val podcasts = SafFolderEntity(2, "content://t/pod", "Podcasts", 2)
    private val all = listOf(music, podcasts)

    @Test fun safGroupsAreRecognisedByPrefix() {
        assertTrue(isSafGroup("/SAF/Music"))
        assertTrue(isSafGroup("/SAF/Music/Rock"))
        assertFalse(isSafGroup("/storage/emulated/0/Music"))
        assertFalse(isSafGroup("/SAFE/Music"))
    }

    @Test fun rootAndSubfolderGroupsMapToTheirImport() {
        assertEquals(music, safFolderFor("/SAF/Music", all))
        assertEquals(music, safFolderFor("/SAF/Music/Rock/Live", all))
        assertEquals(podcasts, safFolderFor("/SAF/Podcasts", all))
    }

    @Test fun lookalikePrefixDoesNotMatch() {
        assertNull(safFolderFor("/SAF/Music2", all))
        assertNull(safFolderFor("/SAF/Mus", all))
    }

    @Test fun mediaStoreGroupsHaveNoFolder() {
        assertNull(safFolderFor("/Music/Rock", all))
        assertNull(safFolderFor("/SAF/Music", emptyList()))
    }
}
