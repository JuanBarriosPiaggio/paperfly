package com.paperfly.paperplanedrift.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paperfly.paperplanedrift.data.Progress
import com.paperfly.paperplanedrift.data.SkinRepository
import com.paperfly.paperplanedrift.ui.components.PaperButton
import com.paperfly.paperplanedrift.ui.components.PlanePreview
import com.paperfly.paperplanedrift.ui.theme.PaperColors

@Composable
fun MainMenuScreen(
    progress: Progress,
    onFly: () -> Unit,
    onDaily: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
    onToggleSound: () -> Unit,
    onLeaderboard: () -> Unit,
    onAchievements: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperColors.Cream)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Hand-drawn wordmark: Baloo 2 Bold with a slight fold tilt.
        Text(
            text = "Paper Plane Drift",
            style = MaterialTheme.typography.headlineLarge,
            color = PaperColors.Ink,
            modifier = Modifier.graphicsLayer { rotationZ = -1.5f },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Hold to climb, release to dive.\nRead the wind before it reads you.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = PaperColors.Tan,
        )
        Spacer(Modifier.height(24.dp))

        PlanePreview(
            skin = SkinRepository.byId(progress.equippedSkin),
            modifier = Modifier.size(160.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Best: ${progress.highScore}",
            style = MaterialTheme.typography.titleLarge,
            color = PaperColors.Ink,
        )
        if (progress.bestCleanStreak > 0) {
            Text(
                text = "Best clean-glide streak: ${progress.bestCleanStreak}",
                style = MaterialTheme.typography.bodySmall,
                color = PaperColors.Tan,
            )
        }
        Spacer(Modifier.height(28.dp))

        PaperButton(
            text = "Fly",
            onClick = onFly,
            big = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        PaperButton(
            text = if (progress.dailyBest > 0) "Daily Challenge  (today: ${progress.dailyBest})" else "Daily Challenge",
            onClick = onDaily,
            primary = false,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onShop) { Text("Skins", color = PaperColors.Teal) }
            TextButton(onClick = onSettings) { Text("Settings", color = PaperColors.Teal) }
            TextButton(onClick = onToggleSound) {
                Text(if (progress.soundEnabled) "Sound: On" else "Sound: Off", color = PaperColors.Teal)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onLeaderboard) { Text("Leaderboard", color = PaperColors.Teal) }
            TextButton(onClick = onAchievements) { Text("Achievements", color = PaperColors.Teal) }
        }
    }
}
