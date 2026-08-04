package com.paperfly.paperplanedrift.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperfly.paperplanedrift.data.ProgressRepository
import com.paperfly.paperplanedrift.domain.CollisionDetector
import com.paperfly.paperplanedrift.games.PlayGamesManager
import com.paperfly.paperplanedrift.domain.GameConfig
import com.paperfly.paperplanedrift.domain.GamePhase
import com.paperfly.paperplanedrift.domain.Obstacle
import com.paperfly.paperplanedrift.domain.ObstacleSpawner
import com.paperfly.paperplanedrift.domain.ObstacleType
import com.paperfly.paperplanedrift.domain.Particle
import com.paperfly.paperplanedrift.domain.PhysicsEngine
import com.paperfly.paperplanedrift.domain.PlaneState
import com.paperfly.paperplanedrift.domain.ScoreCalculator
import com.paperfly.paperplanedrift.domain.WindGust
import com.paperfly.paperplanedrift.util.HapticsManager
import com.paperfly.paperplanedrift.util.SoundManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

data class GameUiState(
    val phase: GamePhase = GamePhase.READY,
    val plane: PlaneState = PlaneState(),
    val holding: Boolean = false,
    val distance: Float = 0f,
    val elapsed: Float = 0f,
    val worldWidth: Float = GameConfig.DEFAULT_WORLD_WIDTH,
    val obstacles: List<Obstacle> = emptyList(),
    val gusts: List<WindGust> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val meters: Int = 0,
    val cleanGlideCount: Int = 0,
    val cleanGlideStreak: Int = 0,
    val bestCleanGlideStreak: Int = 0,
    val score: Int = 0,
    val bestScore: Int = 0,
    val reviveUsed: Boolean = false,
    val invulnerableFor: Float = 0f,
    val crashTimer: Float = 0f,
    val dailyMode: Boolean = false,
    /** Seconds left in the post-revive countdown (COUNTDOWN phase). */
    val countdown: Float = 0f,
) {
    val planeWorldX: Float get() = distance + worldWidth * GameConfig.PLANE_X_FRACTION
}

class GameViewModel(
    private val progressRepository: ProgressRepository,
    private val soundManager: SoundManager,
    private val hapticsManager: HapticsManager,
    private val playGamesManager: PlayGamesManager? = null,
) : ViewModel() {

    var uiState by mutableStateOf(GameUiState())
        private set

    private val physics = PhysicsEngine()
    private var spawner = ObstacleSpawner(Random.nextLong())
    private var lastFrameNanos = 0L

    // Clean-glide tracking for the gust the plane is currently inside.
    private var activeGust: WindGust? = null
    private var gustEntryY = 0f
    private var gustMaxDeviation = 0f

    // Tracks whether each animated obstacle (scissors/stapler) is currently
    // "closed", so we can fire a snip/snap sound exactly on the closing edge.
    private val obstacleClosedState = HashMap<Int, Boolean>()

    /** Elapsed value at launch, to measure real run duration for achievements. */
    private var runStartElapsed = 0f

    init {
        viewModelScope.launch {
            val p = progressRepository.progress.first()
            uiState = uiState.copy(bestScore = p.highScore)
        }
    }

    fun setViewportAspect(widthOverHeight: Float) {
        val w = GameConfig.WORLD_HEIGHT * widthOverHeight
        if (abs(w - uiState.worldWidth) > 0.5f) {
            uiState = uiState.copy(worldWidth = w)
        }
    }

    fun startRun(dailyMode: Boolean = false) {
        val seed = if (dailyMode) LocalDate.now().toEpochDay() else Random.nextLong()
        spawner = ObstacleSpawner(seed)
        lastFrameNanos = 0L
        activeGust = null
        obstacleClosedState.clear()
        soundManager.stopLoops()
        uiState = GameUiState(
            phase = GamePhase.READY,
            worldWidth = uiState.worldWidth,
            bestScore = uiState.bestScore,
            dailyMode = dailyMode,
        )
    }

    fun setHolding(down: Boolean) {
        val s = uiState
        when (s.phase) {
            GamePhase.READY -> if (down) {
                soundManager.playSwoosh()
                soundManager.startLoops()
                runStartElapsed = s.elapsed
                uiState = s.copy(phase = GamePhase.RUNNING, holding = true)
            }
            GamePhase.RUNNING -> uiState = s.copy(holding = down)
            // Let the player pre-hold during the countdown so flight resumes seamlessly.
            GamePhase.COUNTDOWN -> uiState = s.copy(holding = down)
            else -> uiState = s.copy(holding = false)
        }
    }

    /** Rewarded-ad revive: resume from the crash point, once per run. */
    fun revive() {
        val s = uiState
        if (s.reviveUsed || s.phase != GamePhase.GAME_OVER) return
        val planeX = s.planeWorldX
        // Clear anything dangerous around and just ahead of the plane.
        val safeObstacles = s.obstacles.filter { it.worldX < planeX - 20f || it.worldX > planeX + 90f }
        soundManager.playSwoosh()
        // Give the player a 3-second countdown to get re-oriented after the ad
        // before gravity kicks back in.
        uiState = s.copy(
            phase = GamePhase.COUNTDOWN,
            countdown = GameConfig.REVIVE_COUNTDOWN_SECONDS,
            plane = PlaneState(y = GameConfig.WORLD_HEIGHT / 2f, vy = 0f),
            holding = false,
            obstacles = safeObstacles,
            particles = emptyList(),
            reviveUsed = true,
            invulnerableFor = GameConfig.REVIVE_INVULNERABLE_SECONDS,
            crashTimer = 0f,
        )
        lastFrameNanos = 0L
    }

    fun onFrame(frameNanos: Long) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = frameNanos
            return
        }
        val dt = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrameNanos = frameNanos

        when (uiState.phase) {
            GamePhase.READY -> tickReady(dt)
            GamePhase.COUNTDOWN -> tickCountdown(dt)
            GamePhase.RUNNING -> tickRunning(dt)
            GamePhase.CRASHING -> tickCrashing(dt)
            GamePhase.GAME_OVER -> uiState = uiState.copy(particles = stepParticles(uiState.particles, dt))
        }
    }

    /** Post-revive countdown: world frozen, plane hovers, big 3-2-1 in the HUD. */
    private fun tickCountdown(dt: Float) {
        val s = uiState
        val remaining = s.countdown - dt
        // Tick once each time a whole second boundary is crossed.
        if (remaining > 0f && remaining.toInt() != s.countdown.toInt()) soundManager.playTick()
        if (remaining <= 0f) {
            soundManager.playSwoosh()
            soundManager.startLoops()
            runStartElapsed = s.elapsed
            uiState = s.copy(phase = GamePhase.RUNNING, countdown = 0f)
        } else {
            // Gentle hover bob so the plane feels alive while frozen.
            val y = GameConfig.WORLD_HEIGHT / 2f + sin(s.elapsed * 2.2f) * 1.2f
            uiState = s.copy(
                countdown = remaining,
                elapsed = s.elapsed + dt,
                plane = PlaneState(y = y, vy = 0f),
            )
        }
    }

    private fun tickReady(dt: Float) {
        val s = uiState
        val elapsed = s.elapsed + dt
        // Gentle hover bob while waiting for the first touch.
        val y = GameConfig.WORLD_HEIGHT / 2f + sin(elapsed * 2.2f) * 2.2f
        uiState = s.copy(elapsed = elapsed, plane = PlaneState(y = y, vy = 0f))
    }

    private fun tickRunning(dt: Float) {
        var s = uiState
        val elapsed = s.elapsed + dt
        val speed = physics.forwardSpeed(s.distance)
        val distance = s.distance + speed * dt
        val invulnerable = (s.invulnerableFor - dt).coerceAtLeast(0f)

        // Spawn ahead, drop what scrolled far off-screen behind us.
        var obstacles = s.obstacles
        var gusts = s.gusts
        val spawned = spawner.spawnUpTo(distance + s.worldWidth * 1.6f)
        if (spawned.isNotEmpty()) {
            obstacles = obstacles + spawned.map { it.first }
            gusts = gusts + spawned.mapNotNull { it.second }
        }
        obstacles = obstacles.filter { it.worldX + it.halfWidth > distance - 20f }
        gusts = gusts.filter { it.endX > distance - 20f }

        val planeWorldX = distance + s.worldWidth * GameConfig.PLANE_X_FRACTION
        val gustForce = physics.gustForceAt(gusts, planeWorldX)
        val plane = physics.step(s.plane, s.holding, gustForce, dt)

        // Clean-glide bonus tracking.
        var cleanCount = s.cleanGlideCount
        var streak = s.cleanGlideStreak
        var bestStreak = s.bestCleanGlideStreak
        val gustNow = gusts.firstOrNull { it.contains(planeWorldX) }
        if (gustNow != null) {
            if (activeGust != gustNow) {
                activeGust = gustNow
                gustEntryY = plane.y
                gustMaxDeviation = 0f
            } else {
                gustMaxDeviation = maxOf(gustMaxDeviation, abs(plane.y - gustEntryY))
            }
        } else if (activeGust != null) {
            if (ScoreCalculator.isCleanGlide(gustMaxDeviation)) {
                cleanCount++
                streak++
                bestStreak = maxOf(bestStreak, streak)
                soundManager.playTick()
            } else {
                streak = 0
            }
            activeGust = null
        }

        val meters = ScoreCalculator.meters(distance)
        if (meters / 100 > s.meters / 100) soundManager.playTick()
        val score = ScoreCalculator.totalScore(meters, cleanCount)

        // --- Ambient audio mix ---
        // Glide gets louder while diving; wind rustles inside a gust;
        // the fan hum swells as a desk fan approaches.
        val diveT = (plane.vy / GameConfig.MAX_FALL_SPEED).coerceIn(0f, 1f)
        val glideLevel = 0.5f + 0.5f * diveT
        val windLevel = gustNow?.let {
            0.35f + 0.65f * (abs(it.forceY) / GameConfig.MAX_GUST_STRENGTH).coerceIn(0f, 1f)
        } ?: 0f
        var fanLevel = 0f
        for (o in obstacles) {
            if (o.type != ObstacleType.FAN) continue
            val t = 1f - abs(o.worldX - planeWorldX) / (s.worldWidth * 0.9f)
            if (t > fanLevel) fanLevel = t
        }
        soundManager.setLoopLevels(glideLevel, windLevel, fanLevel * fanLevel)

        // --- Closing-animation sounds (scissors snip, stapler snap) ---
        for (o in obstacles) {
            val isScissors = o.type == ObstacleType.SCISSORS
            if (!isScissors && o.type != ObstacleType.STAPLER) continue
            if (abs(o.worldX - planeWorldX) > s.worldWidth) {
                obstacleClosedState.remove(o.id)
                continue
            }
            val threshold = if (isScissors) 0.55f else 0.5f
            val closedNow = o.currentGapHalf(elapsed) < o.gapHalf * threshold
            if (closedNow && obstacleClosedState[o.id] != true) {
                if (isScissors) soundManager.playSnip() else soundManager.playSnap()
            }
            obstacleClosedState[o.id] = closedNow
        }

        s = s.copy(
            distance = distance,
            elapsed = elapsed,
            plane = plane,
            obstacles = obstacles,
            gusts = gusts,
            meters = meters,
            cleanGlideCount = cleanCount,
            cleanGlideStreak = streak,
            bestCleanGlideStreak = bestStreak,
            score = score,
            invulnerableFor = invulnerable,
        )

        val crashed = CollisionDetector.hitsBounds(plane.y) ||
            (invulnerable <= 0f && CollisionDetector.collidesAny(planeWorldX, plane.y, obstacles, elapsed))

        uiState = if (crashed) {
            soundManager.stopLoops()
            soundManager.playThud()
            hapticsManager.crash()
            s.copy(
                phase = GamePhase.CRASHING,
                holding = false,
                crashTimer = 0f,
                particles = crashBurst(planeWorldX, plane.y),
            )
        } else {
            s
        }
    }

    private fun tickCrashing(dt: Float) {
        val s = uiState
        val timer = s.crashTimer + dt
        val particles = stepParticles(s.particles, dt)
        if (timer >= GameConfig.CRASH_ANIM_SECONDS) {
            uiState = s.copy(phase = GamePhase.GAME_OVER, crashTimer = timer, particles = particles)
            persistRun()
        } else {
            uiState = s.copy(crashTimer = timer, particles = particles)
        }
    }

    private fun persistRun() {
        val s = uiState
        viewModelScope.launch {
            progressRepository.submitRun(
                score = s.score,
                cleanStreak = s.bestCleanGlideStreak,
                dailyMode = s.dailyMode,
                dateKey = LocalDate.now().toString(),
            )
            if (s.score > s.bestScore) {
                uiState = uiState.copy(bestScore = s.score)
            }
        }
        reportToPlayGames(s)
    }

    /** Leaderboard scores + achievement unlocks. All calls fail soft offline. */
    private fun reportToPlayGames(s: GameUiState) {
        val games = playGamesManager ?: return
        games.submitScore(PlayGamesManager.Ids.LEADERBOARD_HIGH_SCORE, s.score.toLong())
        if (s.dailyMode) {
            games.submitScore(PlayGamesManager.Ids.LEADERBOARD_DAILY, s.score.toLong())
            games.unlockAchievement(PlayGamesManager.Ids.ACH_DAILY_CHALLENGE)
        }

        games.unlockAchievement(PlayGamesManager.Ids.ACH_FIRST_FLIGHT)
        for ((threshold, id) in listOf(
            100 to PlayGamesManager.Ids.ACH_SCORE_100,
            250 to PlayGamesManager.Ids.ACH_SCORE_250,
            500 to PlayGamesManager.Ids.ACH_SCORE_500,
            1000 to PlayGamesManager.Ids.ACH_SCORE_1000,
        )) {
            if (s.score >= threshold) games.unlockAchievement(id)
        }
        if (s.bestCleanGlideStreak >= 3) games.unlockAchievement(PlayGamesManager.Ids.ACH_CLEAN_STREAK_3)
        if (s.bestCleanGlideStreak >= 5) games.unlockAchievement(PlayGamesManager.Ids.ACH_CLEAN_STREAK_5)
        // The fun one: crumpled within 3 seconds of launch.
        if (s.elapsed - runStartElapsed < 3f) games.unlockAchievement(PlayGamesManager.Ids.ACH_EARLY_CRUMPLE)
    }

    private fun crashBurst(x: Float, y: Float): List<Particle> {
        val rnd = Random(System.nanoTime())
        return List(26) {
            val angle = rnd.nextFloat() * 6.283f
            val speed = 18f + rnd.nextFloat() * 42f
            Particle(
                x = x,
                y = y,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed - 20f,
                rotation = rnd.nextFloat() * 360f,
                rotationSpeed = (rnd.nextFloat() - 0.5f) * 720f,
                size = 0.8f + rnd.nextFloat() * 1.6f,
                life = 0.7f + rnd.nextFloat() * 0.6f,
                colorIndex = rnd.nextInt(3),
            )
        }
    }

    override fun onCleared() {
        soundManager.stopLoops()
        super.onCleared()
    }

    private fun stepParticles(particles: List<Particle>, dt: Float): List<Particle> =
        particles.mapNotNull { p ->
            val life = p.life - dt
            if (life <= 0f) null
            else p.copy(
                x = p.x + p.vx * dt,
                y = p.y + p.vy * dt,
                vy = p.vy + 70f * dt,
                rotation = p.rotation + p.rotationSpeed * dt,
                life = life,
            )
        }
}
