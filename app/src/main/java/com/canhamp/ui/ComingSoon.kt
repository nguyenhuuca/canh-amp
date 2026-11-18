package com.canhamp.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.VfdText
import com.canhamp.ui.theme.Deck

/** Placeholder body shared by screens that are not built yet. */
@Composable
fun ComingSoon(@StringRes nameRes: Int) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
        DeckModule(Modifier.fillMaxWidth()) {
            Column {
                VfdText(stringResource(nameRes))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.coming_soon),
                    style = Deck.type.labelMd,
                    color = Deck.colors.onSurfaceVariant,
                )
            }
        }
    }
}
