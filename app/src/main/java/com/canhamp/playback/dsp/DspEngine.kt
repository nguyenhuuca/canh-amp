package com.canhamp.playback.dsp

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class DspStatus { ACTIVE, BYPASSED, UNSUPPORTED_FORMAT }

/**
 * Single source of DSP settings. The UI writes through [update] and observes [config];
 * the audio thread reads [snapshot] or [current] without locks.
 */
class DspEngine {
    /** A config together with its monotonically increasing version. */
    class Snapshot(val config: DspConfig, val version: Long)

    private val ref = AtomicReference(Snapshot(DspConfig.FLAT, 0L))
    private val _config = MutableStateFlow(DspConfig.FLAT)
    private val _status = MutableStateFlow(DspStatus.ACTIVE)

    val config: StateFlow<DspConfig> = _config
    val status: StateFlow<DspStatus> = _status

    val version: Long get() = ref.get().version

    /** Lock-free read for the audio thread. */
    fun current(): DspConfig = ref.get().config

    /** Lock-free read of config and version as one consistent pair. */
    fun snapshot(): Snapshot = ref.get()

    @Synchronized
    fun update(transform: (DspConfig) -> DspConfig) {
        val old = ref.get()
        val next = transform(old.config).clamped()
        if (next == old.config) return
        ref.set(Snapshot(next, old.version + 1))
        _config.value = next
    }

    /** Called by processors; safe from the audio thread. */
    fun reportStatus(status: DspStatus) {
        if (_status.value != status) _status.value = status
    }
}
