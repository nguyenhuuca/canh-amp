package com.canhamp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import java.util.Locale

private const val GIB = 1024.0 * 1024.0 * 1024.0

/** Device storage in bytes: [audioBytes] of it is indexed music. */
data class StorageUsage(val totalBytes: Long, val freeBytes: Long, val audioBytes: Long)

/** Bar segment widths as fractions of the total; they never sum above 1. */
data class StorageFractions(val audio: Float, val other: Float, val free: Float)

/** Splits [usage] into audio / other used / free. An unreadable total (<= 0) is all free. */
fun storageFractions(usage: StorageUsage): StorageFractions {
    val total = usage.totalBytes
    if (total <= 0L) return StorageFractions(0f, 0f, 1f)
    val free = usage.freeBytes.coerceIn(0L, total)
    val used = total - free
    val audio = usage.audioBytes.coerceIn(0L, used)
    return StorageFractions(
        audio = (audio.toDouble() / total).toFloat(),
        other = ((used - audio).toDouble() / total).toFloat(),
        free = (free.toDouble() / total).toFloat(),
    )
}

/** Bytes as gibibytes with one decimal, for example "12.5 GB". Locale-independent digits. */
fun formatGb(bytes: Long): String = String.format(Locale.US, "%.1f GB", bytes.coerceAtLeast(0L) / GIB)

/** Capacity gauge for the Library bay (B12): audio, other files, free space. */
@Composable
fun StorageBar(usage: StorageUsage, modifier: Modifier = Modifier) {
    val c = Deck.colors
    val f = storageFractions(usage)
    val used = usage.totalBytes - usage.freeBytes
    val capacity = stringResource(R.string.lib_capacity, formatGb(used), formatGb(usage.totalBytes))
    val free = stringResource(R.string.lib_free, formatGb(usage.freeBytes))
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$capacity, $free" },
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Row(
            Modifier.fillMaxWidth().height(8.dp).clip(Deck.shapes.small).clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Segment(f.audio, c.primaryContainer)
            Segment(f.other, c.tertiary)
            Segment(f.free, c.surfaceVariant)
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            Text(capacity, style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
            Text(free, style = Deck.type.labelSm, color = c.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Segment(fraction: Float, color: androidx.compose.ui.graphics.Color) {
    if (fraction > 0f) Box(Modifier.weight(fraction).fillMaxHeight().background(color))
}
