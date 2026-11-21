package com.canhamp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.canhamp.CanhAmpApp
import com.canhamp.data.settings.MainMode
import com.canhamp.data.settings.SettingsRepository
import com.canhamp.data.settings.TimeMode
import com.canhamp.data.settings.VisMode
import com.canhamp.playback.NowPlaying
import com.canhamp.playback.PlayerConnection
import com.canhamp.playback.dsp.DspEngine
import com.canhamp.playback.dsp.DspStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val now: NowPlaying = NowPlaying.EMPTY,
    val timeMode: TimeMode = TimeMode.ELAPSED,
    val visMode: VisMode = VisMode.BAR,
    val visAnimated: Boolean = true,
    val dsp: DspStatus = DspStatus.ACTIVE,
    /** True for 3 s after a playback failure (B27). */
    val readError: Boolean = false,
    val mainMode: MainMode = MainMode.DECK,
    /** False until settings have loaded, so a saved MINIMAL layout does not flash the deck first. */
    val loaded: Boolean = false,
) {
    val hasQueue: Boolean get() = now.queueSize > 0
}

class MainViewModel(
    private val player: PlayerConnection,
    private val settings: SettingsRepository,
    private val dsp: DspEngine,
) : ViewModel() {

    private val readError = MutableStateFlow(false)
    private var errorJob: Job? = null

    val state: StateFlow<MainUiState> = combine(
        player.nowPlaying,
        settings.settings,
        dsp.status,
        readError,
    ) { now, s, status, err ->
        MainUiState(now, s.timeMode, s.visMode, s.visAnimated, status, err, s.mainMode, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MainUiState())

    init {
        viewModelScope.launch { player.errors.collect { showError() } }
    }

    private fun showError() {
        errorJob?.cancel()
        errorJob = viewModelScope.launch {
            readError.value = true
            delay(ERROR_MS)
            readError.value = false
        }
    }

    fun toggleTimeMode() {
        val next = if (state.value.timeMode == TimeMode.ELAPSED) TimeMode.REMAINING else TimeMode.ELAPSED
        viewModelScope.launch { settings.setTimeMode(next) }
    }

    fun cycleVisMode() {
        val next = if (state.value.visMode == VisMode.BAR) VisMode.WAVE else VisMode.BAR
        viewModelScope.launch { settings.setVisMode(next) }
    }

    fun setMainMode(mode: MainMode) {
        viewModelScope.launch { settings.setMainMode(mode) }
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun stop() = player.stop()
    fun previous() = player.previous()
    fun next() = player.next()
    fun seek(positionMs: Long) = player.seekTo(positionMs)
    fun toggleShuffle() = player.setShuffle(!state.value.now.shuffle)
    fun cycleRepeat() = player.cycleRepeat()

    companion object {
        const val ERROR_MS = 3_000L
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as CanhAmpApp).container
                MainViewModel(c.playerConnection, c.settingsRepository, c.dspEngine)
            }
        }
    }
}
