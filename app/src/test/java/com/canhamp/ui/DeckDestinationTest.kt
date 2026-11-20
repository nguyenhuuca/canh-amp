package com.canhamp.ui

import com.canhamp.data.settings.MainMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckDestinationTest {
    @Test fun hasFiveDestinationsInTabOrder() {
        assertEquals(
            listOf("MAIN", "EQ", "PLAYLIST", "LIBRARY", "SETTINGS"),
            DeckDestination.entries.map { it.name },
        )
    }

    @Test fun routesAreUnique() {
        val routes = DeckDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test fun mainIsStart() {
        assertSame(DeckDestination.MAIN, DeckDestination.start)
    }

    @Test fun fromRouteResolvesAndRejectsUnknown() {
        DeckDestination.entries.forEach { assertSame(it, DeckDestination.fromRoute(it.route)) }
        assertNull(DeckDestination.fromRoute("nope"))
        assertNull(DeckDestination.fromRoute(null))
    }

    @Test fun backFromNonMainGoesToMain() {
        DeckDestination.entries.filter { it != DeckDestination.MAIN }.forEach {
            assertSame(DeckDestination.MAIN, backTarget(it))
        }
    }

    @Test fun backOnMainExits() {
        assertNull(backTarget(DeckDestination.MAIN))
    }

    @Test fun miniPlayerHiddenOnlyOnMinimalMain() {
        assertFalse(showMiniPlayer(DeckDestination.MAIN, MainMode.MINIMAL))
        assertTrue(showMiniPlayer(DeckDestination.MAIN, MainMode.DECK))
        DeckDestination.entries.filter { it != DeckDestination.MAIN }.forEach {
            assertTrue(showMiniPlayer(it, MainMode.MINIMAL))
        }
    }
}
