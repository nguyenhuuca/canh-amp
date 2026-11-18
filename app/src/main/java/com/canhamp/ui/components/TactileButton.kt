package com.canhamp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck
import com.canhamp.ui.theme.glowShadow

/** Hardware-style key. [active] renders the latched, recessed look. */
@Composable
fun TactileButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String = label,
    sublabel: String? = null,
) {
    val c = Deck.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pressed) {
        if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val tint = if (active) c.primaryContainer else c.onSurface
    val shape = Deck.shapes.medium
    val bg = when {
        active -> Brush.verticalGradient(listOf(c.surfaceContainerLowest, c.surfaceContainerLowest))
        pressed -> Brush.verticalGradient(listOf(c.buttonBottom, c.buttonTop))
        else -> Brush.verticalGradient(listOf(c.buttonTop, c.buttonBottom))
    }
    val textStyle = Deck.type.labelSm.let { if (active) it.copy(shadow = glowShadow(c.glow)) else it }
    val nudge = if (pressed) 1.dp else 0.dp

    Box(
        modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (active) c.bevelShadow else c.bevelHighlight, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
                selected = active
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.offset(nudge, nudge), horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Text(label.uppercase(), style = textStyle, color = tint, textAlign = TextAlign.Center, maxLines = 1)
            if (sublabel != null) {
                Text(
                    sublabel.uppercase(),
                    style = Deck.type.labelSm,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun TactileButtonPreview() = CanhAmpTheme {
    Row(Modifier.padding(8.dp)) {
        TactileButton("Play", {}, icon = Icons.Filled.PlayArrow)
        TactileButton("Play", {}, icon = Icons.Filled.PlayArrow, active = true)
        TactileButton("Play", {}, enabled = false)
    }
}
