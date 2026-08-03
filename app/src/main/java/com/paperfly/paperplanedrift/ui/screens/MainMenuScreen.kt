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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperfly.paperplanedrift.data.Progress
import com.paperfly.paperplanedrift.data.SkinRepository
import com.paperfly.paperplanedrift.ui.components.PlanePreview

@Composable
fun MainMenuScreen(
    progress: Progress,
    onFly: () -> Unit,
    onDaily: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
    onToggleSound: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Paper Plane Drift",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Hold to climb, release to dive.\nRead the wind before it reads you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            fontWeight = FontWeight.SemiBold,
        )
        if (progress.bestCleanStreak > 0) {
            Text(
                text = "Best clean-glide streak: ${progress.bestCleanStreak}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onFly,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text("Fly", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onDaily,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val dailyText = if (progress.dailyBest > 0) "Daily Challenge  (today: ${progress.dailyBest})"
            else "Daily Challenge"
            Text(dailyText)
        }
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onShop) { Text("Skins") }
            TextButton(onClick = onSettings) { Text("Settings") }
            TextButton(onClick = onToggleSound) {
                Text(if (progress.soundEnabled) "Sound: On" else "Sound: Off")
            }
        }
    }
}
