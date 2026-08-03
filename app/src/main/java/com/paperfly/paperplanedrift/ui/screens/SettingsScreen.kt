package com.paperfly.paperplanedrift.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperfly.paperplanedrift.AppContainer
import com.paperfly.paperplanedrift.BuildConfig
import com.paperfly.paperplanedrift.data.Progress
import com.paperfly.paperplanedrift.data.SkinRepository
import com.paperfly.paperplanedrift.ui.components.PaperButton
import com.paperfly.paperplanedrift.ui.theme.PaperColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
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
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back", color = PaperColors.Teal) }
            Spacer(Modifier.weight(1f))
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = PaperColors.Ink)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        SettingRow(label = "Sound", checked = progress.soundEnabled) {
            scope.launch { container.progressRepository.setSoundEnabled(it) }
        }
        SettingRow(label = "Haptics", checked = progress.hapticsEnabled) {
            scope.launch { container.progressRepository.setHapticsEnabled(it) }
        }

        Spacer(Modifier.height(32.dp))

        if (progress.adsRemoved) {
            Text(
                "Ads removed — thank you!",
                style = MaterialTheme.typography.titleMedium,
                color = PaperColors.Sage,
            )
        } else {
            val price = container.billingManager.priceOf(SkinRepository.PRODUCT_REMOVE_ADS)
            PaperButton(
                text = if (price != null) "Remove Ads  ($price)" else "Remove Ads",
                onClick = {
                    container.billingManager.launchPurchase(activity, SkinRepository.PRODUCT_REMOVE_ADS)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        PaperButton(
            text = "Restore purchases",
            onClick = { container.billingManager.restorePurchases() },
            primary = false,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))
        Text(
            "Paper Plane Drift v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = PaperColors.Tan,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = PaperColors.Ink)
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = PaperColors.Sage,
                checkedThumbColor = PaperColors.Cream,
                uncheckedTrackColor = PaperColors.Disabled,
                uncheckedThumbColor = PaperColors.Tan,
                uncheckedBorderColor = PaperColors.Tan,
            ),
        )
    }
}
