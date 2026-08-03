package com.paperfly.paperplanedrift.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.paperfly.paperplanedrift.data.PlaneSkin
import kotlin.math.sin

/** Bobbing paper-plane preview used on the menu and in the skin shop. */
@Composable
fun PlanePreview(skin: PlaneSkin, modifier: Modifier = Modifier, animate: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "bob")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bobValue",
    )
    Canvas(modifier = modifier) {
        val bob = if (animate) sin(t) else 0f
        val center = Offset(size.width / 2f, size.height / 2f + bob * size.height * 0.05f)
        drawPaperPlane(
            center = center,
            halfW = size.width * 0.34f,
            halfH = size.height * 0.20f,
            pitchDeg = bob * 5f,
            skin = skin,
        )
    }
}
