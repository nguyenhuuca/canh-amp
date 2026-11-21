package com.canhamp.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun mainModeRoundTrips() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmp.newFile("settings.preferences_pb").also { it.delete() }
        }
        val repo = SettingsRepository(store)
        assertEquals(MainMode.DECK, repo.settings.first().mainMode)
        repo.setMainMode(MainMode.MINIMAL)
        assertEquals(MainMode.MINIMAL, repo.settings.first().mainMode)
        repo.setMainMode(MainMode.DECK)
        assertEquals(MainMode.DECK, repo.settings.first().mainMode)
    }
}
