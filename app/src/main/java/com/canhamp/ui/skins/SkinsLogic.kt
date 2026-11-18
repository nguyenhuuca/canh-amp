package com.canhamp.ui.skins

import androidx.annotation.StringRes
import com.canhamp.R
import com.canhamp.playback.dsp.SpectrumFrame
import com.canhamp.ui.theme.ThemeId

/** Filter chips of the skin browser; each theme has exactly one category besides [ALL]. */
enum class SkinCategory(@StringRes val labelRes: Int) {
    ALL(R.string.skins_all),
    CLASSIC(R.string.skins_cat_classic),
    MODERN(R.string.skins_cat_modern),
    CYBER(R.string.skins_cat_cyber),
    DARK(R.string.skins_cat_dark),
}

/** One built-in theme card. */
data class SkinInfo(
    val id: ThemeId,
    val number: Int,
    val category: SkinCategory,
    @StringRes val nameRes: Int,
    @StringRes val tagRes: Int,
    @StringRes val descRes: Int,
)

/** The four built-in skins, in card order (B67). */
val SKINS: List<SkinInfo> = listOf(
    SkinInfo(ThemeId.CLASSIC, 1, SkinCategory.CLASSIC, R.string.skins_name_classic, R.string.skins_tag_classic, R.string.skins_desc_classic),
    SkinInfo(ThemeId.BENTO, 2, SkinCategory.MODERN, R.string.skins_name_bento, R.string.skins_tag_modern, R.string.skins_desc_bento),
    SkinInfo(ThemeId.TITANIUM_BLACK, 3, SkinCategory.DARK, R.string.skins_name_titanium, R.string.skins_tag_contrast, R.string.skins_desc_titanium),
    SkinInfo(ThemeId.CYBER_NEON, 4, SkinCategory.CYBER, R.string.skins_name_cyber, R.string.skins_tag_animated, R.string.skins_desc_cyber),
)

fun skinFor(id: ThemeId): SkinInfo = SKINS.first { it.id == id }

fun filterSkins(category: SkinCategory, skins: List<SkinInfo> = SKINS): List<SkinInfo> =
    if (category == SkinCategory.ALL) skins else skins.filter { it.category == category }

/** What the card previews show for the current track; null while the queue is empty. */
data class PreviewNow(val positionMs: Long, val title: String)

const val MINI_BARS = 6

/** Resting bars of the preview when the visualizer is not animated. */
val STATIC_MINI_BARS = floatArrayOf(0.35f, 0.6f, 0.85f, 0.5f, 0.7f, 0.4f)

/** Averages the 16 analyser bands into [MINI_BARS] preview bars. */
fun miniBars(frame: SpectrumFrame): FloatArray {
    val n = frame.bands.size
    if (n == 0) return FloatArray(MINI_BARS)
    return FloatArray(MINI_BARS) { i ->
        val from = i * n / MINI_BARS
        val to = maxOf(from + 1, (i + 1) * n / MINI_BARS).coerceAtMost(n)
        var sum = 0f
        for (b in from until to) sum += frame.bands[b].coerceIn(0f, 1f)
        sum / (to - from)
    }
}
