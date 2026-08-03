package com.paperfly.paperplanedrift.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paperfly.paperplanedrift.AppContainer
import com.paperfly.paperplanedrift.data.PlaneSkin
import com.paperfly.paperplanedrift.data.Progress
import com.paperfly.paperplanedrift.data.SkinRepository
import com.paperfly.paperplanedrift.data.UnlockMethod
import com.paperfly.paperplanedrift.ui.components.AdBanner
import com.paperfly.paperplanedrift.ui.components.PlanePreview
import kotlinx.coroutines.launch

@Composable
fun SkinShopScreen(
    container: AppContainer,
    activity: Activity,
    progress: Progress,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back") }
            Spacer(Modifier.weight(1f))
            Text(
                "Plane Skins",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(64.dp))
        }
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(SkinRepository.skins) { skin ->
                SkinCard(
                    skin = skin,
                    progress = progress,
                    onTap = {
                        val unlocked = SkinRepository.isUnlocked(skin, progress)
                        when {
                            unlocked -> scope.launch { container.progressRepository.equipSkin(skin.id) }
                            skin.unlock is UnlockMethod.AdWatch ->
                                container.adManager.showRewarded(activity) { earned ->
                                    if (earned) scope.launch { container.progressRepository.unlockSkin(skin.id) }
                                }
                            skin.unlock is UnlockMethod.Iap ->
                                container.billingManager.launchPurchase(
                                    activity,
                                    (skin.unlock as UnlockMethod.Iap).productId,
                                )
                            else -> Unit // Milestone skins unlock automatically by score.
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        AdBanner(container.adManager, progress.adsRemoved)
    }
}

@Composable
private fun SkinCard(skin: PlaneSkin, progress: Progress, onTap: () -> Unit) {
    val unlocked = SkinRepository.isUnlocked(skin, progress)
    val equipped = progress.equippedSkin == skin.id

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (equipped) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.clickable(onClick = onTap),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlanePreview(skin = skin, modifier = Modifier.size(90.dp), animate = unlocked)
            Spacer(Modifier.height(8.dp))
            Text(skin.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            val statusText = when {
                equipped -> "Equipped"
                unlocked -> "Tap to equip"
                else -> when (val u = skin.unlock) {
                    is UnlockMethod.Milestone -> "Locked — reach ${u.score} pts"
                    is UnlockMethod.AdWatch -> "Locked — watch an ad"
                    is UnlockMethod.Iap -> "Locked — buy in shop"
                    is UnlockMethod.Free -> "Free"
                }
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (equipped) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
