package com.canhamp.data.settings

import com.canhamp.ui.theme.ThemeId
import kotlinx.coroutines.flow.Flow

/** The settings the Skins screen reads and writes (B67, B68, B26). */
interface SkinPreferences {
    val settings: Flow<Settings>
    suspend fun setTheme(theme: ThemeId)
    suspend fun setPreserveEqColors(on: Boolean)
    suspend fun setVisAnimated(on: Boolean)
}
