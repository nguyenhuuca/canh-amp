package com.canhamp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.canhamp.CanhAmpApp
import com.canhamp.data.settings.MainMode
import com.canhamp.data.settings.SettingsRepository
import com.canhamp.playback.NowPlaying
import com.canhamp.playback.NowPlayingMapper
import com.canhamp.playback.PlayerConnection
import com.canhamp.ui.components.MiniPlayerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The mini player is hidden (null) while the queue is empty. */
fun NowPlaying.toMiniPlayerState(): MiniPlayerState? =
    if (queueSize == 0) null else MiniPlayerState(title, artist, positionMs, isPlaying)

/** False on MAIN in minimal mode: that layout already has its own big transport keys. */
fun showMiniPlayer(current: DeckDestination, mainMode: MainMode): Boolean =
    !(current == DeckDestination.MAIN && mainMode == MainMode.MINIMAL)

/** Shell state derived from playback: mini player, header stereo pill, and its transport buttons. */
class ShellViewModel(private val player: PlayerConnection, settings: SettingsRepository) : ViewModel() {

    /** Layout of the MAIN tab; the mini player hides on MAIN in minimal mode. */
    val mainMode: StateFlow<MainMode> = settings.settings
        .map { it.mainMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MainMode.DECK)

    val miniPlayer: StateFlow<MiniPlayerState?> = player.nowPlaying
        .map { it.toMiniPlayerState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** True stereo, false mono, null when no track format is known. */
    val stereo: StateFlow<Boolean?> = player.nowPlaying
        .map { NowPlayingMapper.isStereo(it.channelCount) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun previous() = player.previous()
    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as CanhAmpApp).container
                ShellViewModel(c.playerConnection, c.settingsRepository)
            }
        }
    }
}
