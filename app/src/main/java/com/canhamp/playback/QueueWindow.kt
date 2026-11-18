package com.canhamp.playback

/** A slice [from, toExclusive) of a list and where the tapped item sits inside it. */
data class QueueWindow(val from: Int, val toExclusive: Int, val startIndex: Int) {
    val size: Int get() = toExclusive - from
}

const val MAX_QUEUE_WINDOW = 500

/** At most [max] items around [index]; the window slides to stay inside [0, size). */
fun queueWindow(size: Int, index: Int, max: Int = MAX_QUEUE_WINDOW): QueueWindow {
    if (size <= 0 || max <= 0) return QueueWindow(0, 0, 0)
    val i = index.coerceIn(0, size - 1)
    if (size <= max) return QueueWindow(0, size, i)
    val from = (i - max / 2).coerceIn(0, size - max)
    return QueueWindow(from, from + max, i - from)
}

/** The window of [list] around [index] together with the start index inside it. */
fun <T> windowAround(list: List<T>, index: Int, max: Int = MAX_QUEUE_WINDOW): Pair<List<T>, Int> {
    val w = queueWindow(list.size, index, max)
    return list.subList(w.from, w.toExclusive) to w.startIndex
}
