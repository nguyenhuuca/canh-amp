package com.canhamp.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.LedToggle
import com.canhamp.ui.theme.Dimens

@Composable
fun LanguageTab() {
    var current by remember { mutableStateOf(AppLanguage.fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())) }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(vertical = Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        DeckModule(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                for (lang in AppLanguage.entries) {
                    LedToggle(
                        label = stringResource(lang.labelRes),
                        checked = lang == current,
                        onCheckedChange = {
                            if (lang != current) {
                                current = lang
                                // Recreates the activity with the new resources; the choice is persisted by appcompat.
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
