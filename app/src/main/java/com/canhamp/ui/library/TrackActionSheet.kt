package com.canhamp.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Per-track actions as a deck module in a bottom sheet (B11). Details is a placeholder until the real sheet lands. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    row: TrackRow,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Deck.colors
    var details by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        contentColor = c.onSurface,
    ) {
        DeckModule(Modifier.fillMaxWidth().padding(horizontal = Dimens.margin).navigationBarsPadding()) {
            Column {
                Text(
                    "${row.artist} - ${row.title}".uppercase(),
                    style = Deck.type.labelMd,
                    color = c.primaryContainer,
                    maxLines = 1,
                )
                Spacer(Modifier.height(Dimens.spaceMd))
                TactileButton(stringResource(R.string.lib_add_queue), onAddToQueue, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Dimens.spaceSm))
                TactileButton(stringResource(R.string.lib_play_next), onPlayNext, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Dimens.spaceSm))
                TactileButton(stringResource(R.string.lib_details), { details = true }, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(Dimens.spaceXl))
    }
    if (details) {
        AlertDialog(
            onDismissRequest = { details = false },
            confirmButton = {
                TextButton(onClick = { details = false }) { Text(stringResource(R.string.lib_sheet_close)) }
            },
            title = { Text(row.title) },
            text = { Text(stringResource(R.string.lib_details_placeholder)) },
        )
    }
}
