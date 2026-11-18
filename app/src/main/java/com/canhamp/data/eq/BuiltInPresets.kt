package com.canhamp.data.eq

/**
 * One equalizer preset. [tag] is the drawer sub-label of a built-in (empty for user presets).
 * Built-ins carry negative ids so they never clash with database rows.
 */
data class EqPreset(
    val id: Long,
    val name: String,
    val tag: String,
    val gains: List<Int>,
    val preamp: Int,
    val user: Boolean,
)

/** Spec B46: the eight built-in presets, preamp 0. */
object BuiltInPresets {
    private fun builtIn(id: Long, name: String, tag: String, gains: List<Int>) =
        EqPreset(id, name, tag, gains, preamp = 0, user = false)

    val FLAT: EqPreset = builtIn(-4, "Flat", "0dB", List(10) { 0 })

    val all: List<EqPreset> = listOf(
        builtIn(-1, "Rock", "CLASSIC", listOf(5, 7, 3, -1, -2, 1, 4, 7, 8, 9)),
        builtIn(-2, "Pop", "CLASSIC", listOf(-1, 2, 4, 5, 4, 2, -1, -2, -2, -2)),
        builtIn(-3, "Techno", "BASS+", listOf(7, 6, 2, -3, -4, 0, 5, 8, 8, 7)),
        FLAT,
        builtIn(-5, "Full Bass", "SUB-BOOST", listOf(11, 10, 8, 4, 0, -2, -4, -6, -7, -8)),
        builtIn(-6, "Club", "DANCE", listOf(0, 2, 4, 3, 3, 3, 2, 0, 1, 2)),
        builtIn(-7, "Reggae", "WARM", listOf(0, 1, -1, -4, -1, 3, 4, 2, 0, 0)),
        builtIn(-8, "Vocal", "MID-RANGE", listOf(-3, -4, -2, 4, 7, 6, 3, 1, -1, -3)),
    )

    fun isBuiltInName(name: String): Boolean = all.any { it.name.equals(name, ignoreCase = true) }
}
