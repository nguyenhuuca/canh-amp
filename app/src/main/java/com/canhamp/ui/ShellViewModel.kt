package com.canhamp.ui

import androidx.lifecycle.ViewModel
import com.canhamp.ui.components.MiniPlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Shell state. The mini player stays hidden (null) until playback is wired in. */
class ShellViewModel : ViewModel() {
    private val _miniPlayer = MutableStateFlow<MiniPlayerState?>(null)
    val miniPlayer: StateFlow<MiniPlayerState?> = _miniPlayer
}
