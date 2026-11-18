package com.canhamp.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canhamp.CanhAmpApp
import com.canhamp.R
import com.canhamp.data.library.DetailsResult
import com.canhamp.data.library.TrackDetails
import com.canhamp.data.library.TrackDetailsFormat as F
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.components.TactileButton
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.util.formatDuration

/** Read-only track details as a deck module in a bottom sheet (library design spec). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsSheet(trackId: Long, onDismiss: () -> Unit) {
    val library = (LocalContext.current.applicationContext as CanhAmpApp).container.libraryRepository
    var result by remember(trackId) { mutableStateOf<DetailsResult?>(null) }
    LaunchedEffect(trackId) { result = library.details(trackId) }
    val c = Deck.colors

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface, contentColor = c.onSurface) {
        DeckModule(Modifier.fillMaxWidth().padding(horizontal = Dimens.margin).navigationBarsPadding()) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (val r = result) {
                    null -> Notice(stringResource(R.string.det_loading))
                    DetailsResult.NotFound -> Notice(stringResource(R.string.det_not_found))
                    is DetailsResult.Found -> Rows(r.details)
                    is DetailsResult.Unreadable -> {
                        Notice(stringResource(R.string.det_unreadable))
                        Spacer(Modifier.height(Dimens.spaceMd))
                        Rows(r.details)
                    }
                }
                Spacer(Modifier.height(Dimens.spaceMd))
                TactileButton(stringResource(R.string.lib_sheet_close), onDismiss, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(Dimens.spaceXl))
    }
}

@Composable
private fun Notice(text: String) {
    Text(text, style = Deck.type.labelMd, color = Deck.colors.primaryContainer)
}

@Composable
private fun Rows(d: TrackDetails) {
    DetailRow(R.string.det_title, F.text(d.title))
    DetailRow(R.string.det_artist, F.text(d.artist))
    DetailRow(R.string.det_album, F.text(d.album))
    DetailRow(R.string.det_year, F.text(d.year))
    DetailRow(R.string.det_genre, F.text(d.genre))
    DetailRow(R.string.det_track, F.text(d.trackNumber))
    DetailRow(R.string.det_format, F.text(d.format.uppercase()))
    DetailRow(R.string.det_bitrate, F.bitrate(d.bitrateKbps))
    DetailRow(R.string.det_sample_rate, F.sampleRate(d.sampleRateHz))
    DetailRow(R.string.det_duration, if (d.durationMs > 0) formatDuration(d.durationMs) else F.NO_VALUE)
    DetailRow(R.string.det_size, F.size(d.sizeBytes))
    DetailRow(R.string.det_path, F.text(d.filePath), maxLines = 3)
    DetailRow(R.string.det_modified, F.modified(d.dateModifiedSec))
}

@Composable
private fun DetailRow(labelRes: Int, value: String, maxLines: Int = 2) {
    val c = Deck.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = Dimens.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Text(stringResource(labelRes), style = Deck.type.labelSm, color = c.onSurfaceVariant, modifier = Modifier.width(LABEL_WIDTH))
        Box(Modifier.weight(1f)) {
            Text(value, style = Deck.type.bodyMd, color = c.onSurface, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        }
    }
}

private val LABEL_WIDTH = 104.dp
