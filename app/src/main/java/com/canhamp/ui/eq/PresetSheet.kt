package com.canhamp.ui.eq

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.data.eq.EqPreset
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens

/** Which presets the drawer lists. */
enum class SheetMode { ALL, USER }

/** Preset drawer (B46, B48): 2-column grid, ACTIVE marker, USER tag, long press deletes a user preset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSheet(
    presets: List<EqPreset>,
    activeName: String,
    mode: SheetMode,
    onSelect: (EqPreset) -> Unit,
    onDelete: (EqPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Deck.colors
    var pendingDelete by remember { mutableStateOf<EqPreset?>(null) }
    val shown = if (mode == SheetMode.USER) presets.filter { it.user } else presets
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface, contentColor = c.onSurface) {
        DeckModule(Modifier.fillMaxWidth().padding(horizontal = Dimens.margin).navigationBarsPadding()) {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(if (mode == SheetMode.USER) R.string.eq_my_presets else R.string.eq_presets_title),
                    style = Deck.type.labelMd,
                    color = c.primaryContainer,
                )
                Spacer(Modifier.height(Dimens.spaceMd))
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.eq_no_user_presets), style = Deck.type.labelMd, color = c.onSurfaceVariant)
                }
                shown.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().padding(bottom = Dimens.gutter), horizontalArrangement = Arrangement.spacedBy(Dimens.gutter)) {
                        pair.forEach { preset ->
                            PresetCell(
                                preset = preset,
                                active = preset.name.equals(activeName, ignoreCase = true),
                                onClick = { onSelect(preset) },
                                onLongClick = if (preset.user) ({ pendingDelete = preset }) else null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(Dimens.spaceXl))
    }
    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = c.surfaceContainer,
            title = {
                Text(stringResource(R.string.eq_delete_confirm, preset.name.uppercase()), style = Deck.type.labelLg, color = c.onSurface)
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(preset)
                }) { Text(stringResource(R.string.pl_yes), style = Deck.type.labelLg, color = c.primaryContainer) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.pl_cancel), style = Deck.type.labelLg, color = c.onSurfaceVariant)
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetCell(
    preset: EqPreset,
    active: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    val shape = Deck.shapes.medium
    val deleteLabel = stringResource(R.string.eq_cd_delete)
    val tag = if (preset.user) stringResource(R.string.eq_user) else preset.tag
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.buttonTop, c.buttonBottom)))
            .border(1.dp, c.bevelHighlight, shape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = null,
                onLongClickLabel = if (onLongClick != null) deleteLabel else null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics { selected = active }
            .drawBehind {
                if (active) drawRect(c.primaryContainer, Offset.Zero, Size(3.dp.toPx(), size.height))
            }
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd)
            .heightIn(min = 32.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                preset.name.uppercase(),
                style = Deck.type.labelMd,
                color = if (active) c.primaryContainer else c.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (active) {
                Spacer(Modifier.width(Dimens.spaceSm))
                Text(stringResource(R.string.eq_active), style = Deck.type.labelSm, color = c.primaryContainer, maxLines = 1)
            }
        }
        Text(tag, style = Deck.type.labelSm, color = if (preset.user) c.secondaryContainer else c.onSurfaceVariant, maxLines = 1)
    }
}
