package com.canhamp.ui.skins

import com.canhamp.data.settings.Settings
import com.canhamp.data.settings.SkinPreferences
import com.canhamp.playback.NowPlaying
import com.canhamp.playback.dsp.SpectrumFrame
import com.canhamp.ui.theme.ThemeId
import com.canhamp.ui.theme.deckColorsFor
import com.canhamp.ui.theme.eqColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkinsViewModelTest {

    private class FakePrefs(initial: Settings = Settings()) : SkinPreferences {
        val flow = MutableStateFlow(initial)
        override val settings = flow
        override suspend fun setTheme(theme: ThemeId) { flow.value = flow.value.copy(theme = theme) }
        override suspend fun setPreserveEqColors(on: Boolean) { flow.value = flow.value.copy(preserveEqColors = on) }
        override suspend fun setVisAnimated(on: Boolean) { flow.value = flow.value.copy(visAnimated = on) }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vmFor(prefs: FakePrefs) = SkinsViewModel(prefs, flowOf(null))

    @Test fun filtersByCategory() {
        assertEquals(4, filterSkins(SkinCategory.ALL).size)
        assertEquals(listOf(ThemeId.CLASSIC), filterSkins(SkinCategory.CLASSIC).map { it.id })
        assertEquals(listOf(ThemeId.BENTO), filterSkins(SkinCategory.MODERN).map { it.id })
        assertEquals(listOf(ThemeId.CYBER_NEON), filterSkins(SkinCategory.CYBER).map { it.id })
        assertEquals(listOf(ThemeId.TITANIUM_BLACK), filterSkins(SkinCategory.DARK).map { it.id })
    }

    @Test fun everyThemeHasExactlyOneCard() {
        assertEquals(ThemeId.entries.toSet(), SKINS.map { it.id }.toSet())
        assertEquals(SKINS.size, SKINS.map { it.number }.toSet().size)
    }

    @Test fun stateFollowsFilterAndSettings() = runTest {
        val prefs = FakePrefs(Settings(theme = ThemeId.BENTO, preserveEqColors = true, visAnimated = false))
        val vm = vmFor(prefs)
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        assertEquals(ThemeId.BENTO, vm.state.value.applied)
        assertTrue(vm.state.value.preserveEqColors)
        assertFalse(vm.state.value.visAnimated)
        assertEquals(4, vm.state.value.visible.size)
        vm.setFilter(SkinCategory.DARK)
        assertEquals(listOf(ThemeId.TITANIUM_BLACK), vm.state.value.visible.map { it.id })
        job.cancel()
    }

    @Test fun applyPersistsThemeAndCardBecomesApplied() = runTest {
        val prefs = FakePrefs()
        val vm = vmFor(prefs)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect { } }
        assertEquals(ThemeId.CLASSIC, vm.state.value.applied)
        vm.apply(ThemeId.CYBER_NEON)
        assertEquals(ThemeId.CYBER_NEON, prefs.flow.value.theme)
        assertEquals(ThemeId.CYBER_NEON, vm.state.value.applied)
    }

    @Test fun togglesPersistBothPreferences() = runTest {
        val prefs = FakePrefs()
        val vm = vmFor(prefs)
        vm.setPreserveEqColors(true)
        vm.setVisAnimated(false)
        assertTrue(prefs.flow.first().preserveEqColors)
        assertFalse(prefs.flow.first().visAnimated)
    }

    @Test fun previewNowIsNullWithoutQueueAndWholeSecondsOtherwise() {
        assertNull(NowPlaying.EMPTY.toPreviewNow())
        val np = NowPlaying(title = "Song", positionMs = 65_999, queueSize = 3)
        assertEquals(PreviewNow(65_000, "Song"), np.toPreviewNow())
    }

    @Test fun miniBarsAverageGroupsOfBands() {
        val bands = FloatArray(SpectrumFrame.BANDS) { 0f }
        for (i in 0 until 3) bands[i] = 0.6f
        val frame = SpectrumFrame(bands, FloatArray(SpectrumFrame.BANDS), FloatArray(SpectrumFrame.WAVE_SIZE))
        val bars = miniBars(frame)
        assertEquals(MINI_BARS, bars.size)
        assertTrue(bars[0] > 0.3f)
        assertEquals(0f, bars[5], 0f)
        assertArrayEquals(FloatArray(MINI_BARS), miniBars(SpectrumFrame.EMPTY), 0f)
    }

    @Test fun preserveEqColorsKeepsClassicWhateverTheTheme() {
        val classic = deckColorsFor(ThemeId.CLASSIC)
        for (id in ThemeId.entries) {
            val themed = deckColorsFor(id)
            val kept = eqColors(true, themed)
            assertEquals(classic.primaryContainer, kept.green)
            assertEquals(classic.secondaryContainer, kept.amber)
            assertEquals(classic.tertiary, kept.orange)
            val follow = eqColors(false, themed)
            assertEquals(themed.primaryContainer, follow.green)
            assertEquals(themed.secondaryContainer, follow.amber)
        }
    }
}
