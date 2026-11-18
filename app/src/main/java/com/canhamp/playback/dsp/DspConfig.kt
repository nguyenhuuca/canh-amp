package com.canhamp.playback.dsp

/** Immutable DSP settings snapshot. Use [clamped] before applying untrusted values. */
data class DspConfig(
    val eqEnabled: Boolean = true,
    val gainsDb: List<Int> = List(BAND_COUNT) { 0 },
    val preampDb: Int = 0,
    val bassDb: Int = 0,
    val trebleDb: Int = 0,
    val balance: Float = 0f,
) {
    /** Returns a copy with every field forced into its legal range; [gainsDb] gets exactly 10 entries. */
    fun clamped(): DspConfig = copy(
        gainsDb = List(BAND_COUNT) { i -> gainsDb.getOrElse(i) { 0 }.coerceIn(-12, 12) },
        preampDb = preampDb.coerceIn(-12, 12),
        bassDb = bassDb.coerceIn(0, 12),
        trebleDb = trebleDb.coerceIn(0, 12),
        balance = if (balance.isNaN()) 0f else balance.coerceIn(-1f, 1f),
    )

    companion object {
        const val BAND_COUNT = 10
        val BANDS_HZ: List<Int> = listOf(60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000)
        const val Q = 1.41
        const val BASS_HZ = 100
        const val TREBLE_HZ = 10000
        val FLAT = DspConfig()
    }
}
