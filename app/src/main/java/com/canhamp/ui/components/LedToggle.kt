package com.canhamp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhamp.R
import com.canhamp.ui.theme.CanhAmpTheme
import com.canhamp.ui.theme.Deck

/** Push-latch toggle with a small LED. */
@Composable
fun LedToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    ledColor: Color = Deck.colors.primaryContainer,
    pulse: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Deck.colors
    val shape = Deck.shapes.medium
    val reduced = rememberReducedMotion()
    val pulseAlpha = if (checked && pulse && !reduced) {
        val t = rememberInfiniteTransition(label = "ledPulse")
        val a by t.animateFloat(
            initialValue = 1f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "ledPulseAlpha",
        )
        a
    } else {
        1f
    }
    val state = stringResource(if (checked) R.string.led_on else R.string.led_off)
    val dot = if (checked) ledColor else c.surfaceVariant

    Row(
        modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(if (checked) c.surfaceContainerLowest else c.surfaceContainerHigh)
            .border(1.dp, if (checked) c.bevelShadow else c.bevelHighlight, shape)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { stateDescription = state }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            Modifier
                .size(8.dp)
                .alpha(pulseAlpha)
                .drawBehind {
                    if (checked) {
                        drawCircle(
                            Brush.radialGradient(listOf(ledColor.copy(alpha = 0.5f), Color.Transparent)),
                            radius = size.minDimension * 1.5f,
                        )
                    }
                }
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label.uppercase(),
            style = Deck.type.labelSm,
            color = if (checked) c.onSurface else c.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF101417)
@Composable
private fun LedTogglePreview() = CanhAmpTheme {
    Row(Modifier.padding(8.dp)) {
        LedToggle("Shuffle", checked = true, onCheckedChange = {}, pulse = true)
        Spacer(Modifier.width(8.dp))
        LedToggle("Repeat", checked = false, onCheckedChange = {})
    }
}
