package com.paperfly.paperplanedrift.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.paperfly.paperplanedrift.AppContainer
import com.paperfly.paperplanedrift.data.Progress
import com.paperfly.paperplanedrift.data.SkinRepository
import com.paperfly.paperplanedrift.domain.GamePhase
import com.paperfly.paperplanedrift.ui.GameViewModel
import com.paperfly.paperplanedrift.ui.components.AdBanner
import com.paperfly.paperplanedrift.ui.components.GameCanvas
import com.paperfly.paperplanedrift.ui.components.PaperButton
import com.paperfly.paperplanedrift.ui.theme.PaperColors

@Composable
fun GameplayScreen(
    container: AppContainer,
    activity: Activity,
    dailyMode: Boolean,
    progress: Progress,
    onExit: () -> Unit,
) {
    val vm: GameViewModel = viewModel(factory = viewModelFactory {
        initializer {
            GameViewModel(
                progressRepository = container.progressRepository,
                soundManager = container.soundManager,
                hapticsManager = container.hapticsManager,
            )
        }
    })

    // Fresh run every time this screen is entered.
    LaunchedEffect(Unit) { vm.startRun(dailyMode) }

    // Physics/render loop driven by the display's frame clock.
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { vm.onFrame(it) }
        }
    }

    val state = vm.uiState
    val skin = SkinRepository.byId(progress.equippedSkin)

    // Interstitial cadence: decided once per crash, shown when leaving the overlay.
    var pendingInterstitial by remember { mutableStateOf(false) }
    LaunchedEffect(state.phase) {
        if (state.phase == GamePhase.GAME_OVER && !progress.adsRemoved) {
            if (container.interstitialPolicy.onCrash()) pendingInterstitial = true
        }
    }

    fun leaveOverlay(action: () -> Unit) {
        if (pendingInterstitial) {
            pendingInterstitial = false
            container.adManager.showInterstitial(activity) { action() }
        } else {
            action()
        }
    }

    BackHandler { onExit() }

    Box(modifier = Modifier.fillMaxSize()) {
        GameCanvas(
            state = state,
            skin = skin,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (size.height > 0) vm.setViewportAspect(size.width.toFloat() / size.height)
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            vm.setHolding(true)
                            tryAwaitRelease()
                            vm.setHolding(false)
                        }
                    )
                },
        )

        // HUD: ink numerals on a translucent cream pill; milestone crossings flash sage.
        if (state.phase == GamePhase.RUNNING || state.phase == GamePhase.CRASHING) {
            var flashUntil by remember { mutableFloatStateOf(-1f) }
            val milestone = state.meters / 100
            LaunchedEffect(milestone) {
                if (milestone > 0) flashUntil = state.elapsed + 0.4f
            }
            val hudColor by animateColorAsState(
                if (state.elapsed < flashUntil) PaperColors.Sage else PaperColors.Ink,
                label = "hudFlash",
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    color = PaperColors.Cream.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = "${state.meters} m",
                        style = MaterialTheme.typography.headlineMedium,
                        color = hudColor,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }
                if (state.cleanGlideStreak > 0) {
                    Text(
                        text = "clean glide x${state.cleanGlideStreak}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaperColors.Sage,
                    )
                }
            }
        }

        if (state.phase == GamePhase.READY) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 180.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (state.dailyMode) "Daily Challenge" else "Ready?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PaperColors.Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Hold to climb • Release to dive\nTap and hold to launch",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PaperColors.Tan,
                )
            }
        }

        if (state.phase == GamePhase.GAME_OVER) {
            GameOverOverlay(
                meters = state.meters,
                bonuses = state.cleanGlideCount,
                score = state.score,
                bestScore = state.bestScore,
                canRevive = !state.reviveUsed && container.adManager.isRewardedReady(),
                onRevive = {
                    container.adManager.showRewarded(activity) { earned ->
                        if (earned) vm.revive()
                    }
                },
                onRetry = { leaveOverlay { vm.startRun(dailyMode) } },
                onMenu = { leaveOverlay { onExit() } },
                banner = { AdBanner(container.adManager, progress.adsRemoved) },
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    meters: Int,
    bonuses: Int,
    score: Int,
    bestScore: Int,
    canRevive: Boolean,
    onRevive: () -> Unit,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
    banner: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x553A322A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Cream card, 2dp ink border, 20dp corner radius (per brief).
            Surface(
                color = PaperColors.Cream,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(2.dp, PaperColors.Ink, RoundedCornerShape(20.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Crumpled!", style = MaterialTheme.typography.headlineSmall, color = PaperColors.Ink)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "$score",
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 46.sp,
                        color = PaperColors.Terracotta,
                    )
                    Text(
                        "$meters m" + if (bonuses > 0) "  +  $bonuses clean glides" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperColors.Tan,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Best: $bestScore",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperColors.Tan,
                    )
                    Spacer(Modifier.height(20.dp))

                    if (canRevive) {
                        PaperButton(
                            text = "Continue  (watch ad)",
                            onClick = onRevive,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        PaperButton(
                            text = "Retry",
                            onClick = onRetry,
                            primary = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        PaperButton(
                            text = "Retry",
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    TextButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) {
                        Text("Menu", color = PaperColors.Teal)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            banner()
        }
    }
}
