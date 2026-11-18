package com.canhamp.ui.library

import com.canhamp.playback.QueueEntry
import com.canhamp.playback.windowAround

fun TrackRow.toQueueEntry(): QueueEntry = QueueEntry(
    trackId = id,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    bitrateKbps = bitrateKbps,
)

/**
 * B11 play now: up to 500 entries of [snapshot] around [row], and the start index of [row] in them.
 * A row missing from the snapshot (list changed under the tap) plays alone.
 */
fun playQueueFor(row: TrackRow, snapshot: List<TrackRow>): Pair<List<QueueEntry>, Int> {
    val at = snapshot.indexOfFirst { it.id == row.id }
    if (at < 0) return listOf(row.toQueueEntry()) to 0
    val (window, start) = windowAround(snapshot, at)
    return window.map(TrackRow::toQueueEntry) to start
}
