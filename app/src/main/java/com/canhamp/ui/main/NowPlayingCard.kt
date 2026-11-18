package com.canhamp.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.playback.NowPlaying
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ArtworkSize = 64.dp

/** Module 7: artwork or cassette placeholder, format badge, title and artist / album. */
@Composable
fun NowPlayingCard(now: NowPlaying, modifier: Modifier = Modifier) {
    val c = Deck.colors
    val targetPx = with(LocalDensity.current) { ArtworkSize.roundToPx() }
    val art by produceState<ImageBitmap?>(null, now.artworkData, targetPx) {
        val bytes = now.artworkData
        value = if (bytes == null) null else withContext(Dispatchers.Default) {
            decodeArtwork(bytes, targetPx)?.asImageBitmap()
        }
    }
    val empty = now.queueSize == 0
    DeckModule(modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg), verticalAlignment = Alignment.CenterVertically) {
            val shape = Deck.shapes.small
            val artModifier = Modifier.size(ArtworkSize).clip(shape)
            val image = art
            if (image != null) {
                Image(image, contentDescription = null, modifier = artModifier, contentScale = ContentScale.Crop)
            } else {
                Image(painterResource(R.drawable.cassette_placeholder), contentDescription = null, modifier = artModifier)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                if (empty) {
                    Text(stringResource(R.string.deck_no_track), style = Deck.type.titleMd, color = c.onSurfaceVariant)
                } else {
                    DeckFormat.formatBadge(now.mimeType)?.let {
                        Text(
                            it,
                            style = Deck.type.labelSm,
                            color = c.secondaryContainer,
                            maxLines = 1,
                            modifier = Modifier.background(c.surfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Text(now.title, style = Deck.type.titleMd, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(now.artist, now.album).filter { it.isNotBlank() }.joinToString(" • "),
                        style = Deck.type.bodyMd,
                        color = c.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
