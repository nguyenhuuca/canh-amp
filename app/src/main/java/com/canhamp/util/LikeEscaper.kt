package com.canhamp.util

private const val MAX_QUERY = 100
private const val BACKSLASH = '\\'

/** Trims and caps a user search at 100 characters. */
fun normalizeQuery(query: String): String = query.trim().take(MAX_QUERY)

/**
 * Builds a SQL LIKE pattern (use with `ESCAPE '\'`) from user text: `\`, `%` and `_` match literally.
 * Returns "" for blank input, which the DAO queries treat as "no filter".
 */
fun likePattern(query: String): String {
    val q = normalizeQuery(query)
    if (q.isEmpty()) return ""
    val escaped = buildString(q.length + 2) {
        for (ch in q) {
            if (ch == BACKSLASH || ch == '%' || ch == '_') append(BACKSLASH)
            append(ch)
        }
    }
    return "%$escaped%"
}
