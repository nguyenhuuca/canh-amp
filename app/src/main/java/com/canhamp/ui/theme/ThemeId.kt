package com.canhamp.ui.theme

enum class ThemeId(val key: String) {
    CLASSIC("classic"),
    BENTO("bento"),
    TITANIUM_BLACK("titanium_black"),
    CYBER_NEON("cyber_neon"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeId = entries.firstOrNull { it.key == key } ?: CLASSIC
    }
}
