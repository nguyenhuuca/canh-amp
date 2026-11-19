package com.canhamp.ui.settings

import androidx.annotation.StringRes
import com.canhamp.R

/** In-app language choices. [tag] is a BCP-47 tag; empty means follow the system. */
enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.settings_lang_system),
    EN("en", R.string.settings_lang_english),
    VI("vi", R.string.settings_lang_vietnamese),
    ;

    companion object {
        /** Maps the stored locale tags (e.g. "vi" or "vi-VN") to a choice; anything unknown is [SYSTEM]. */
        fun fromTags(tags: String?): AppLanguage {
            val primary = tags?.substringBefore(',')?.trim()?.substringBefore('-')?.lowercase().orEmpty()
            return entries.firstOrNull { it.tag.isNotEmpty() && it.tag == primary } ?: SYSTEM
        }
    }
}
