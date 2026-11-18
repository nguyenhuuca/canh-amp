package com.canhamp.ui.library

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueryDebounceTest {
    @Test fun emitsOnlyTheLastValueAfterTheQuietPeriod() = runTest {
        val source = MutableStateFlow("")
        val seen = mutableListOf<String>()
        val job = launch { source.debouncedQuery().collect { seen += it } }
        advanceTimeBy(SEARCH_DEBOUNCE_MS + 1)
        runCurrent()
        seen.clear()

        source.value = "d"
        advanceTimeBy(100)
        source.value = "da"
        advanceTimeBy(100)
        source.value = "  daft "
        advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)
        runCurrent()
        assertEquals(emptyList<String>(), seen)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf("daft"), seen)
        job.cancel()
    }
}
