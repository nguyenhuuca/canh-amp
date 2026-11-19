package com.canhamp.ui.settings

import androidx.annotation.StringRes
import com.canhamp.R

/** Tabs of the settings screen, in display order. Add an entry to add a tab. */
enum class SettingsTab(@StringRes val labelRes: Int) {
    SKIN(R.string.settings_tab_skin),
    RELEASE_NOTES(R.string.settings_tab_release_notes),
    LANGUAGE(R.string.settings_tab_language),
}
