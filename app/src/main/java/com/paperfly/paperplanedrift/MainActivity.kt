package com.paperfly.paperplanedrift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.paperfly.paperplanedrift.data.Progress
import com.paperfly.paperplanedrift.ui.screens.GameplayScreen
import com.paperfly.paperplanedrift.ui.screens.MainMenuScreen
import com.paperfly.paperplanedrift.ui.screens.SettingsScreen
import com.paperfly.paperplanedrift.ui.screens.SkinShopScreen
import com.paperfly.paperplanedrift.ui.screens.SplashScreen
import com.paperfly.paperplanedrift.ui.theme.PaperTheme
import kotlinx.coroutines.launch

enum class Screen { SPLASH, MENU, GAME, SHOP, SETTINGS }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PaperPlaneApp).container
        container.adManager.preload(this)
        container.billingManager.connect()
        container.playGamesManager.attach(this)

        setContent {
            PaperTheme {
                AppRoot(container, this)
            }
        }
    }
}

@Composable
fun AppRoot(container: AppContainer, activity: MainActivity) {
    var screen by remember { mutableStateOf(Screen.SPLASH) }
    var dailyMode by remember { mutableStateOf(false) }
    val progress by container.progressRepository.progress.collectAsState(initial = Progress())
    val scope = rememberCoroutineScope()

    // Keep the sound/haptics engines in sync with persisted settings.
    LaunchedEffect(progress.soundEnabled, progress.hapticsEnabled) {
        container.soundManager.enabled = progress.soundEnabled
        container.hapticsManager.enabled = progress.hapticsEnabled
    }

    when (screen) {
        Screen.SPLASH -> SplashScreen(onDone = { screen = Screen.MENU })

        Screen.MENU -> MainMenuScreen(
            progress = progress,
            onFly = { dailyMode = false; screen = Screen.GAME },
            onDaily = { dailyMode = true; screen = Screen.GAME },
            onShop = { screen = Screen.SHOP },
            onSettings = { screen = Screen.SETTINGS },
            onToggleSound = {
                scope.launch {
                    container.progressRepository.setSoundEnabled(!progress.soundEnabled)
                }
            },
            onLeaderboard = { container.playGamesManager.showLeaderboards(activity) },
            onAchievements = { container.playGamesManager.showAchievements(activity) },
        )

        Screen.GAME -> GameplayScreen(
            container = container,
            activity = activity,
            dailyMode = dailyMode,
            progress = progress,
            onExit = { screen = Screen.MENU },
        )

        Screen.SHOP -> SkinShopScreen(
            container = container,
            activity = activity,
            progress = progress,
            onBack = { screen = Screen.MENU },
        )

        Screen.SETTINGS -> SettingsScreen(
            container = container,
            activity = activity,
            progress = progress,
            onBack = { screen = Screen.MENU },
        )
    }
}
