package com.paperfly.paperplanedrift.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.paperfly.paperplanedrift.ui.components.OrigamiMark
import com.paperfly.paperplanedrift.ui.theme.PaperColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    // Reveal motion from the brief: fold-in, then a 200ms settle bounce (1.04 -> 1.0).
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1.04f, animationSpec = tween(durationMillis = 500))
        scale.animateTo(1f, animationSpec = tween(durationMillis = 200))
        delay(600)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperColors.Cream),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OrigamiMark(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
            )
            Spacer(Modifier.height(24.dp))
            // Hand-drawn wordmark: Baloo 2 Bold with a slight paper-fold tilt.
            Text(
                text = "Paper Plane Drift",
                style = MaterialTheme.typography.headlineLarge,
                color = PaperColors.Ink,
                modifier = Modifier.graphicsLayer { rotationZ = -1.5f },
            )
        }
    }
}
