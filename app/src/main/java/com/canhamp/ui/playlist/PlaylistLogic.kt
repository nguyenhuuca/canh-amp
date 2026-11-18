package com.canhamp.ui.playlist

import com.canhamp.playback.QueueEntry
import com.canhamp.util.formatDuration
import java.util.Locale
import kotlin.random.Random

/** SORT cycles A-Z by title, then artist, then duration (B62). */
enum class SortMode {
    TITLE,
    ARTIST,
    DURATION,
    ;

    companion object {
        /** The mode one SORT tap applies after [current]; the first tap applies TITLE. */
        fun after(current: SortMode?): SortMode = when (current) {
            null -> TITLE
            TITLE -> ARTIST
            ARTIST -> DURATION
            DURATION -> TITLE
        }
    }
}

/** One visible row of the queue list. [index] is the position in the whole queue, not in the filtered view. */
data class QueueRow(
    val index: Int,
    val key: String,
    val entry: QueueEntry,
    val isPlaying: Boolean,
    val isSelected: Boolean,
    val missing: Boolean,
)

data class QueueStats(val count: Int, val totalMs: Long, val selectedMs: Long?) {
    /** "h:mm:ss", also under an hour. */
    val totalText: String get() = formatTotal(totalMs)

    /** "mm:ss" of the selected row, "--:--" when none. */
    val selectedText: String get() = selectedMs?.let(::formatDuration) ?: "--:--"
}

/** A row taken out of the queue, kept for the 4 s UNDO (B60). */
data class RemovedRow(val index: Int, val entry: QueueEntry)

const val UNDO_MS = 4_000L

fun formatTotal(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000L
    return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

fun queueStats(queue: List<QueueEntry>, selected: Int?): QueueStats =
    QueueStats(queue.size, queue.sumOf { it.durationMs }, selected?.let { queue.getOrNull(it)?.durationMs })

/** B57 as far as this screen knows it: a row without a library track. */
fun isMissing(entry: QueueEntry): Boolean = entry.trackId < 0

/** Stable keys for list rows: the uri plus its nth occurrence, so the same track queued twice stays distinct. */
fun rowKeys(queue: List<QueueEntry>): List<String> {
    val seen = HashMap<String, Int>()
    return queue.map { e ->
        val n = (seen[e.uri] ?: 0) + 1
        seen[e.uri] = n
        "${e.uri}#$n"
    }
}

/** B63: case-insensitive contains on title and artist; blank keeps everything. Returns queue positions. */
fun filterIndices(queue: List<QueueEntry>, query: String): List<Int> {
    val q = query.trim()
    if (q.isEmpty()) return queue.indices.toList()
    return queue.indices.filter { i ->
        val e = queue[i]
        e.title.contains(q, ignoreCase = true) || e.artist.contains(q, ignoreCase = true)
    }
}

fun buildRows(queue: List<QueueEntry>, playingIndex: Int?, selected: Int?, query: String): List<QueueRow> {
    val keys = rowKeys(queue)
    return filterIndices(queue, query).map { i ->
        QueueRow(
            index = i,
            key = keys[i],
            entry = queue[i],
            isPlaying = i == playingIndex,
            isSelected = i == selected && i != playingIndex,
            missing = isMissing(queue[i]),
        )
    }
}

/** Old queue positions in their new order for [mode]. Case-insensitive and stable, so equal rows keep their order. */
fun sortOrder(queue: List<QueueEntry>, mode: SortMode): List<Int> {
    val byText = String.CASE_INSENSITIVE_ORDER
    val cmp: Comparator<Int> = when (mode) {
        SortMode.TITLE -> Comparator { a, b -> byText.compare(queue[a].title, queue[b].title) }
        SortMode.ARTIST -> Comparator { a, b ->
            byText.compare(queue[a].artist, queue[b].artist).takeIf { it != 0 }
                ?: byText.compare(queue[a].title, queue[b].title)
        }
        SortMode.DURATION -> Comparator { a, b -> queue[a].durationMs.compareTo(queue[b].durationMs) }
    }
    return queue.indices.sortedWith(cmp)
}

/** B62 RND: a one-off permutation (Fisher-Yates). */
fun shuffleOrder(size: Int, random: Random): List<Int> {
    val order = MutableList(size) { it }
    for (i in size - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val t = order[i]
        order[i] = order[j]
        order[j] = t
    }
    return order
}

/** Where old position [old] ends up after [order] is applied; null when [old] is null or not in [order]. */
fun newPositionOf(order: List<Int>, old: Int?): Int? = old?.let { order.indexOf(it) }?.takeIf { it >= 0 }

fun <T> List<T>.reordered(order: List<Int>): List<T> = order.map { this[it] }

/** The selected position after one row moves from [from] to [to]. */
fun selectionAfterMove(selected: Int?, from: Int, to: Int): Int? = when {
    selected == null -> null
    selected == from -> to
    from < to && selected in (from + 1)..to -> selected - 1
    from > to && selected in to until from -> selected + 1
    else -> selected
}

/** The selected position after row [removed] is taken out; the selection is lost when it was that row. */
fun selectionAfterRemove(selected: Int?, removed: Int): Int? = when {
    selected == null || selected == removed -> null
    selected > removed -> selected - 1
    else -> selected
}

/** Undo bookkeeping: what to remember when [index] is removed, or null when there is no such row. */
fun removedRow(queue: List<QueueEntry>, index: Int): RemovedRow? =
    queue.getOrNull(index)?.let { RemovedRow(index, it) }

/** Where an undone row goes back: its old position, or the end when the queue got shorter meanwhile. */
fun restoreIndex(removed: RemovedRow, queueSize: Int): Int = removed.index.coerceIn(0, queueSize)

/** Filtered rows can only be dragged when no filter hides part of the queue. */
fun canReorder(query: String): Boolean = query.isBlank()
