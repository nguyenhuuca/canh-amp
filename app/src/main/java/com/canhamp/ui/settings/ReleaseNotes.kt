package com.canhamp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.canhamp.BuildConfig
import com.canhamp.R
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.RecessedWell
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ReleaseSection(val title: String, val bullets: List<String>)

private const val MAX_SECTIONS = 200
private const val MAX_LINE = 500

/**
 * Parses the bundled markdown subset: "## " starts a section, "- " adds a bullet to the current
 * section. Everything else (blank lines, stray text, bullets before any header) is ignored.
 */
fun parseReleaseNotes(text: String): List<ReleaseSection> {
    val out = ArrayList<ReleaseSection>()
    var title: String? = null
    var bullets = ArrayList<String>()
    fun flush() {
        title?.let { out.add(ReleaseSection(it, bullets)) }
        bullets = ArrayList()
    }
    for (raw in text.lineSequence()) {
        val line = raw.trim().take(MAX_LINE)
        when {
            line.startsWith("## ") -> {
                val t = line.removePrefix("## ").trim()
                if (t.isEmpty()) continue
                flush()
                if (out.size >= MAX_SECTIONS) return out
                title = t
            }
            line.startsWith("- ") && title != null -> {
                val b = line.removePrefix("- ").trim()
                if (b.isNotEmpty()) bullets.add(b)
            }
        }
    }
    flush()
    return out
}

@Composable
fun ReleaseNotesTab() {
    val context = LocalContext.current
    val sections by produceState<List<ReleaseSection>>(emptyList(), context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.resources.openRawResource(R.raw.release_notes).bufferedReader().use { parseReleaseNotes(it.readText()) }
            }.getOrDefault(emptyList())
        }
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(vertical = Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        RecessedWell(Modifier.fillMaxWidth(), vfd = true) {
            VfdText(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = Deck.type.titleMd,
            )
        }
        for (section in sections) {
            DeckModule(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                    Text(section.title, style = Deck.type.titleMd, color = Deck.colors.primaryContainer)
                    for (bullet in section.bullets) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                            Text("•", style = Deck.type.bodyMd, color = Deck.colors.onSurfaceVariant)
                            Text(bullet, style = Deck.type.bodyMd, color = Deck.colors.onSurface)
                        }
                    }
                }
            }
        }
    }
}
