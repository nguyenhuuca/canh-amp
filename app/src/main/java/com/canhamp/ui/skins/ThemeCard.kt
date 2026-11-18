package com.canhamp.ui.skins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.components.DeckModule
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.Dimens
import com.canhamp.ui.theme.deckColorsFor

/** One theme: ID line and tag, live preview in its own palette, swatches, description and the apply button. */
@Composable
fun ThemeCard(
    skin: SkinInfo,
    applied: Boolean,
    now: PreviewNow?,
    bars: FloatArray,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deck.colors
    val palette = deckColorsFor(skin.id)
    val name = stringResource(skin.nameRes)
    val tag = stringResource(skin.tagRes)
    val state = stringResource(if (applied) R.string.skins_cd_applied else R.string.skins_cd_not_applied)
    val group = stringResource(R.string.skins_cd_card, name, tag.lowercase(), state)
    val applyDescription = stringResource(R.string.skins_cd_apply, name)

    DeckModule(modifier.fillMaxWidth().semantics { contentDescription = group }) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.skins_id_line, "%02d".format(skin.number), tag),
                    style = Deck.type.labelSm,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (applied) {
                    Text(
                        stringResource(R.string.skins_current),
                        style = Deck.type.labelSm,
                        color = c.primaryContainer,
                        maxLines = 1,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = Deck.type.titleMd, color = c.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
                Text(
                    tag,
                    style = Deck.type.labelSm,
                    color = c.secondaryContainer,
                    maxLines = 1,
                    modifier = Modifier
                        .border(1.dp, c.secondaryContainer, Deck.shapes.small)
                        .padding(horizontal = Dimens.spaceSm, vertical = Dimens.spaceXs),
                )
            }
            ThemePreview(palette, now, bars)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                val swatches = listOf(palette.surface, palette.primaryContainer, palette.secondaryContainer, palette.surfaceContainerHigh)
                for (swatch in swatches) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(Deck.shapes.small)
                            .background(swatch)
                            .border(1.dp, c.outline, Deck.shapes.small),
                    )
                }
            }
            Text(stringResource(skin.descRes), style = Deck.type.bodyMd, color = c.onSurfaceVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.minTouch)
                    .clip(Deck.shapes.medium)
                    .background(if (applied) c.surfaceContainerHigh else c.primaryContainer)
                    .clickable(enabled = !applied, role = Role.Button, onClick = onApply)
                    .semantics { if (!applied) contentDescription = applyDescription }
                    .padding(Dimens.spaceMd),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (applied) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = c.primaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Dimens.spaceSm))
                }
                Text(
                    stringResource(if (applied) R.string.skins_applied else R.string.skins_apply),
                    style = Deck.type.labelLg,
                    color = if (applied) c.primaryContainer else c.onPhosphor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
