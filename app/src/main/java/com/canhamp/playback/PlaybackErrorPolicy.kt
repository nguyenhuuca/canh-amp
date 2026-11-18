package com.canhamp.playback

/**
 * Decides what to do after a playback error (B27): skip to the next item, or stop once
 * [maxConsecutive] failures happen in a row. Pure, so it is tested on the JVM.
 */
class PlaybackErrorPolicy(private val maxConsecutive: Int = MAX_CONSECUTIVE) {

    enum class Action { SKIP, STOP }

    var failures: Int = 0
        private set

    /** Records one failure. STOP also resets the count so a later manual retry starts fresh. */
    fun onError(hasNext: Boolean): Action {
        failures++
        if (!hasNext || failures >= maxConsecutive) {
            failures = 0
            return Action.STOP
        }
        return Action.SKIP
    }

    /** Call when audio actually plays; skipping past a bad item is not a success. */
    fun onPlaying() {
        failures = 0
    }

    companion object {
        const val MAX_CONSECUTIVE = 5
    }
}
