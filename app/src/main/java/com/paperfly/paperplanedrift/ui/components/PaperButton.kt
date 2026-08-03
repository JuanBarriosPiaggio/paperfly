package com.paperfly.paperplanedrift.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperfly.paperplanedrift.ui.theme.PaperColors

/**
 * Buttons from the design brief:
 * - Primary CTA: terracotta fill, cream text, 16dp radius, 3dp hard shadow;
 *   press scales to 0.96 and the shadow contracts to 1dp.
 * - Secondary: cream fill, 2dp ink outline, same radius.
 */
@Composable
fun PaperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    big: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "btnScale")
    val shadowOffset by animateDpAsState(if (pressed) 1.dp else 3.dp, label = "btnShadow")
    val shape = RoundedCornerShape(16.dp)

    val fill by animateColorAsState(
        when {
            !enabled -> PaperColors.Disabled
            primary -> PaperColors.Terracotta
            else -> PaperColors.Cream
        },
        label = "btnFill",
    )
    val textColor = when {
        !enabled -> PaperColors.Tan
        primary -> PaperColors.Cream
        else -> PaperColors.Ink
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        if (primary && enabled) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(y = shadowOffset)
                    .background(PaperColors.TerracottaShadow, shape),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(fill)
                .then(
                    if (!primary) Modifier.border(2.dp, PaperColors.Ink, shape) else Modifier
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(PaddingValues(horizontal = 28.dp, vertical = if (big) 16.dp else 12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontSize = if (big) 22.sp else 18.sp,
                color = textColor,
            )
        }
    }
}
