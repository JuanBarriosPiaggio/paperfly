package com.paperfly.paperplanedrift.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.paperfly.paperplanedrift.AppContainer
import com.paperfly.paperplanedrift.data.PlaneSkin
import com.paperfly.paperplanedrift.data.Progress
import com.paperfly.paperplanedrift.data.SkinRepository
import com.paperfly.paperplanedrift.data.UnlockMethod
import com.paperfly.paperplanedrift.ui.components.AdBanner
import com.paperfly.paperplanedrift.ui.components.PlanePreview
import com.paperfly.paperplanedrift.ui.theme.PaperColors
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
            .background(PaperColors.Cream)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back", color = PaperColors.Teal) }
            Spacer(Modifier.weight(1f))
            Text("Plane Skins", style = MaterialTheme.typography.titleLarge, color = PaperColors.Ink)
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

/** Grayscale version of a skin for locked tiles. */
private fun desaturate(argb: Long): Long {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toLong().coerceIn(0, 255)
    return (0xFFL shl 24) or (gray shl 16) or (gray shl 8) or gray
}

@Composable
private fun SkinCard(skin: PlaneSkin, progress: Progress, onTap: () -> Unit) {
    val unlocked = SkinRepository.isUnlocked(skin, progress)
    val equipped = progress.equippedSkin == skin.id
    val shape = RoundedCornerShape(16.dp)

    val previewSkin = if (unlocked) skin else skin.copy(
        bodyColor = desaturate(skin.bodyColor),
        shadeColor = desaturate(skin.shadeColor),
        accentColor = desaturate(skin.accentColor),
    )

    Box(
        modifier = Modifier
            .aspectRatio(0.92f)
            .background(if (unlocked) Color(0xFFFFFDF4) else PaperColors.Disabled.copy(alpha = 0.4f), shape)
            .then(
                if (equipped) Modifier.border(3.dp, PaperColors.Sage, shape)
                else Modifier.border(1.dp, PaperColors.Tan.copy(alpha = 0.5f), shape)
            )
            .clickable(onClick = onTap),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PlanePreview(skin = previewSkin, modifier = Modifier.size(84.dp), animate = unlocked)
            Spacer(Modifier.height(8.dp))
            Text(skin.name, style = MaterialTheme.typography.titleSmall, color = PaperColors.Ink)
            Spacer(Modifier.height(2.dp))
            val statusText = when {
                equipped -> "Equipped"
                unlocked -> "Tap to equip"
                else -> when (val u = skin.unlock) {
                    is UnlockMethod.Milestone -> "Reach ${u.score} pts"
                    is UnlockMethod.AdWatch -> "Watch an ad"
                    is UnlockMethod.Iap -> "One-time purchase"
                    is UnlockMethod.Free -> "Free"
                }
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (equipped) PaperColors.Sage else PaperColors.Tan,
            )
        }

        // Padlock badge, top-right, on locked tiles.
        if (!unlocked) {
            Badge(
                icon = Icons.Filled.Lock,
                tint = PaperColors.Ink,
                background = PaperColors.Disabled,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
        // Checkmark badge, top-right, on the equipped tile.
        if (equipped) {
            Badge(
                icon = Icons.Filled.Check,
                tint = PaperColors.Cream,
                background = PaperColors.Sage,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
        // Unlock-method badge, bottom-left: star = milestone, play = ad, cart = purchase.
        val methodIcon: ImageVector? = when (skin.unlock) {
            is UnlockMethod.Milestone -> Icons.Filled.Star
            is UnlockMethod.AdWatch -> Icons.Filled.PlayArrow
            is UnlockMethod.Iap -> Icons.Filled.ShoppingCart
            is UnlockMethod.Free -> null
        }
        if (methodIcon != null) {
            Badge(
                icon = methodIcon,
                tint = PaperColors.Cream,
                background = PaperColors.Terracotta,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun Badge(icon: ImageVector, tint: Color, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}
