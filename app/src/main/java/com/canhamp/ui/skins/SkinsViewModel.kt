package com.canhamp.ui.skins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.canhamp.CanhAmpApp
import com.canhamp.data.settings.SkinPreferences
import com.canhamp.playback.NowPlaying
import com.canhamp.ui.theme.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SkinsUiState(
    val filter: SkinCategory = SkinCategory.ALL,
    val applied: ThemeId = ThemeId.CLASSIC,
    val preserveEqColors: Boolean = false,
    val visAnimated: Boolean = true,
) {
    val visible: List<SkinInfo> get() = filterSkins(filter)
}

private const val STOP_TIMEOUT_MS = 5_000L
private const val MS_PER_SECOND = 1_000L

/** The preview shows whole seconds, so sub-second position updates collapse into one value. */
fun NowPlaying.toPreviewNow(): PreviewNow? =
    if (queueSize == 0) null else PreviewNow(positionMs / MS_PER_SECOND * MS_PER_SECOND, title)

class SkinsViewModel(
    private val prefs: SkinPreferences,
    nowPlaying: Flow<PreviewNow?>,
) : ViewModel() {

    private val filter = MutableStateFlow(SkinCategory.ALL)

    val state: StateFlow<SkinsUiState> = combine(filter, prefs.settings) { f, s ->
        SkinsUiState(f, s.theme, s.preserveEqColors, s.visAnimated)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SkinsUiState())

    val now: StateFlow<PreviewNow?> = nowPlaying.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun setFilter(category: SkinCategory) {
        filter.value = category
    }

    /** B67: writes the theme; `CanhAmpTheme` recomposes from the settings flow. */
    fun apply(theme: ThemeId) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun setPreserveEqColors(on: Boolean) {
        viewModelScope.launch { prefs.setPreserveEqColors(on) }
    }

    fun setVisAnimated(on: Boolean) {
        viewModelScope.launch { prefs.setVisAnimated(on) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as CanhAmpApp).container
                SkinsViewModel(
                    container.settingsRepository,
                    container.playerConnection.nowPlaying.map { it.toPreviewNow() },
                )
            }
        }
    }
}
