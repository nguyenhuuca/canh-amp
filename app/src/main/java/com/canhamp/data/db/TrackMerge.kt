package com.canhamp.data.db

/** How a fresh scan or tag pass row replaces a stored one without losing what playback learned. */
object TrackMerge {
    /**
     * [incoming] wins, except for values only playback learns (B8: the sample rate, written by
     * `SampleRateRecorder`): a scan or tag pass that does not know them keeps the stored ones.
     */
    fun keepLearned(current: TrackEntity, incoming: TrackEntity): TrackEntity = incoming.copy(
        id = current.id,
        sampleRateHz = incoming.sampleRateHz ?: current.sampleRateHz,
    )
}
