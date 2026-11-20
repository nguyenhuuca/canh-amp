package com.canhamp.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canhamp.R
import com.canhamp.playback.NowPlaying
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.util.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val VinylMax = 290.dp
private val LabelSize = 112.dp
private val PlaySize = 80.dp
private val SkipSize = 56.dp
private const val SPIN_MS = 18_000

/** The Pure Minimal player: a spinning record, the track, a slim seek bar and three keys. */
@Composable
internal fun MinimalPlayer(state: MainUiState, actions: DeckActions, modifier: Modifier = Modifier) {
    val now = state.now
    val c = Deck.colors
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceXl),
    ) {
        Vinyl(now, spinning = now.isPlaying && state.visAnimated, Modifier.widthIn(max = VinylMax).fillMaxWidth())
        TrackInfo(state)
        MinimalTimes(state)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundKey(Icons.Filled.SkipPrevious, stringResource(R.string.cd_previous), SkipSize, state.hasQueue, primary = false, actions.previous)
            RoundKey(
                if (now.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                stringResource(if (now.isPlaying) R.string.cd_pause else R.string.cd_play),
                PlaySize,
                state.hasQueue,
                primary = true,
                onClick = if (now.isPlaying) actions.pause else actions.play,
            )
            RoundKey(Icons.Filled.SkipNext, stringResource(R.string.cd_next), SkipSize, state.hasQueue, primary = false, actions.next)
        }
        if (state.readError) {
            Text(stringResource(R.string.err_cannot_read), style = Deck.type.labelMd, color = c.error, maxLines = 1)
        }
    }
}

@Composable
private fun Vinyl(now: NowPlaying, spinning: Boolean, modifier: Modifier) {
    val c = Deck.colors
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        while (true) {
            rotation.animateTo(rotation.value + 360f, tween(SPIN_MS, easing = LinearEasing))
            rotation.snapTo(rotation.value % 360f)
        }
    }
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        // Phosphor aura and the outer frame.
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(
                Brush.radialGradient(listOf(c.primaryContainer.copy(alpha = 0.16f), Color.Transparent), center, r * 1.05f),
                radius = r * 1.05f,
            )
            drawCircle(Brush.verticalGradient(listOf(c.surfaceContainerLow, c.surfaceContainerLowest)), radius = r * 0.98f)
            drawCircle(Color.White.copy(alpha = 0.05f), radius = r * 0.98f, style = Stroke(1.dp.toPx()))
        }
        // The disc itself spins: grooves, sheen and the centre label.
        Box(
            Modifier.fillMaxSize(0.9f).graphicsLayer { rotationZ = rotation.value },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(c.surface, radius = r)
                drawCircle(
                    Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), center, r),
                    radius = r,
                )
                val grooves = floatArrayOf(0.95f, 0.86f, 0.77f, 0.68f, 0.6f, 0.52f)
                grooves.forEachIndexed { i, f ->
                    drawCircle(
                        c.surfaceVariant.copy(alpha = if (i % 2 == 0) 0.3f else 0.18f),
                        radius = r * f,
                        style = Stroke(1.dp.toPx()),
                    )
                }
                drawCircle(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.05f), Color.Transparent, Color.White.copy(alpha = 0.04f)),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                    ),
                    radius = r,
                )
            }
            CenterLabel(now)
        }
    }
}

@Composable
private fun CenterLabel(now: NowPlaying) {
    val c = Deck.colors
    val targetPx = with(LocalDensity.current) { LabelSize.roundToPx() }
    val art by produceState<ImageBitmap?>(null, now.artworkData, targetPx) {
        val bytes = now.artworkData
        value = if (bytes == null) null else withContext(Dispatchers.Default) {
            decodeArtwork(bytes, targetPx)?.asImageBitmap()
        }
    }
    Box(
        Modifier
            .size(LabelSize)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(c.surfaceContainerHigh, c.surfaceContainerHighest, c.surfaceContainerLow)))
            .border(1.dp, c.primaryContainer.copy(alpha = 0.3f), CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(c.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        val image = art
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Canvas(Modifier.fillMaxSize().padding(2.dp)) {
                drawCircle(
                    c.primaryContainer.copy(alpha = 0.2f),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp)) {
                Text(
                    now.artist.ifBlank { "CANH AMP" }.uppercase(),
                    style = Deck.type.labelSm.copy(fontSize = 8.sp),
                    color = c.primaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = c.primaryFixedDim, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.minimal_side),
                    style = Deck.type.labelSm.copy(fontSize = 7.sp),
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                )
                Spindle(Modifier.padding(top = Dimens.spaceXs))
            }
        }
        if (image != null) Spindle()
    }
}

@Composable
private fun Spindle(modifier: Modifier = Modifier) {
    val c = Deck.colors
    Box(
        modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(c.surfaceContainerLowest)
            .border(1.dp, c.surfaceBright.copy(alpha = 0.5f), CircleShape),
    )
}

@Composable
private fun TrackInfo(state: MainUiState) {
    val c = Deck.colors
    val now = state.now
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        if (!state.hasQueue) {
            Text(stringResource(R.string.deck_no_track), style = Deck.type.titleMd, color = c.onSurfaceVariant, maxLines = 1)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.minimal_track, now.index + 1),
                style = Deck.type.labelMd,
                color = c.primaryContainer,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.surfaceContainerHighest.copy(alpha = 0.8f))
                    .border(1.dp, c.primaryContainer.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXs),
            )
            DeckFormat.formatBadge(now.mimeType)?.let {
                Text(it, style = Deck.type.labelMd, color = c.secondaryFixed, maxLines = 1)
            }
        }
        Text(
            now.title,
            style = Deck.type.headlineLg,
            color = c.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            listOf(now.artist, now.album).filter { it.isNotBlank() }.joinToString(" • "),
            style = Deck.type.bodyMd,
            color = c.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Elapsed time, format and duration on one line. */
@Composable
private fun MinimalTimes(state: MainUiState) {
    val c = Deck.colors
    val now = state.now
    val elapsed = if (state.hasQueue) formatDuration(now.positionMs) else DeckFormat.NO_VALUE
    val total = if (state.hasQueue) formatDuration(now.durationMs) else DeckFormat.NO_VALUE
    val khz = stringResource(R.string.deck_khz)
    Row(Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceMd), verticalAlignment = Alignment.CenterVertically) {
        Text(elapsed, style = Deck.type.labelMd, color = c.primaryContainer, maxLines = 1)
        Text(
            listOfNotNull(
                DeckFormat.formatBadge(now.mimeType),
                now.sampleRateHz?.let { "${DeckFormat.kHz(it)} $khz" },
            ).joinToString(" • "),
            style = Deck.type.labelSm,
            color = c.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = Dimens.spaceMd),
        )
        Text(total, style = Deck.type.labelMd, color = c.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun RoundKey(
    icon: ImageVector,
    description: String,
    size: Dp,
    enabled: Boolean,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val c = Deck.colors
    val glow = c.primaryContainer
    Box(
        Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .then(
                if (primary) {
                    Modifier.drawBehind {
                        drawCircle(
                            Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), center, this.size.minDimension * 0.8f),
                            radius = this.size.minDimension * 0.8f,
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(if (primary) c.primaryContainer else c.surfaceContainerHigh.copy(alpha = 0.8f))
            .border(1.dp, if (primary) Color.Transparent else c.surfaceVariant.copy(alpha = 0.3f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (primary) c.onPrimaryContainer else c.onSurface,
            modifier = Modifier.size(if (primary) 38.dp else 26.dp),
        )
    }
}
