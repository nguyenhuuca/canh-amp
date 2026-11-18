package com.canhamp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.canhamp.CanhAmpApp
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

/** Shell state derived from playback: mini player, header stereo pill, and its transport buttons. */
class ShellViewModel(private val player: PlayerConnection) : ViewModel() {

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
                ShellViewModel((this[APPLICATION_KEY] as CanhAmpApp).container.playerConnection)
            }
        }
    }
}
