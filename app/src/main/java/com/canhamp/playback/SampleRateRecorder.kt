package com.canhamp.playback

import com.canhamp.data.library.TrackLookup
import com.canhamp.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** B8: writes the sample rate the player reports back to the track row, once per item. */
class SampleRateRecorder(
    private val lookup: TrackLookup,
    private val logger: Logger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private var lastTrackId = NONE
    private var lastHz = 0

    /** Call from the main thread; repeated calls for the same item and rate are ignored. */
    fun record(trackId: Long, hz: Int) {
        if (trackId < 0 || hz <= 0 || (trackId == lastTrackId && hz == lastHz)) return
        lastTrackId = trackId
        lastHz = hz
        scope.launch {
            try {
                val row = lookup.findById(trackId) ?: return@launch
                if (row.sampleRateHz != hz) lookup.updateSampleRate(trackId, hz)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.w(TAG, "sample rate write failed: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "SampleRateRecorder"
        const val NONE = Long.MIN_VALUE
    }
}
