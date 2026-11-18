package com.canhamp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.canhamp.ui.AppNavGraph
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.ThemeId
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CanhAmpApp).container
        container.playerConnection.connect()
        setContent {
            // The first frame uses CLASSIC until DataStore answers; Apply then recomposes the whole app (B67).
            val settings = container.settingsRepository.settings
            val theme by remember(settings) { settings.map { it.theme }.distinctUntilChanged() }
                .collectAsState(initial = ThemeId.CLASSIC)
            val preserve by remember(settings) { settings.map { it.preserveEqColors }.distinctUntilChanged() }
                .collectAsState(initial = false)
            CanhAmpTheme(themeId = theme, preserveEqColors = preserve) { AppNavGraph() }
        }
    }
}
