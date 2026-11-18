package com.canhamp.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Per-track actions as a deck module in a bottom sheet (B11). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    row: TrackRow,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onDetails: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Deck.colors
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
                TactileButton(stringResource(R.string.lib_details), onDetails, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(Dimens.spaceXl))
    }
}
