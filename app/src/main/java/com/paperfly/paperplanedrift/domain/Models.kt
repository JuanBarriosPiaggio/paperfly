package com.paperfly.paperplanedrift.domain

import kotlin.math.abs
import kotlin.math.sin

enum class GamePhase { READY, COUNTDOWN, RUNNING, CRASHING, GAME_OVER }

enum class ObstacleType { TORN_PAPER, SCISSORS, STAPLER, WINDOW, FAN }

/**
 * A vertical obstacle column at [worldX] with a fly-through gap.
 * Scissors and staplers animate their gap over time; fans never collide
 * (they are pure wind challenges).
 */
data class Obstacle(
    val id: Int,
    val type: ObstacleType,
    val worldX: Float,
    val halfWidth: Float,
    val gapCenter: Float,
    val gapHalf: Float,
    val animSpeed: Float = 0f,
    val animPhase: Float = 0f,
) {
    /** Current half-height of the gap, accounting for closing scissors / snapping staplers. */
    fun currentGapHalf(timeSec: Float): Float = when (type) {
        ObstacleType.SCISSORS -> {
            // Smoothly narrows then reopens.
            val s = abs(sin(animPhase + timeSec * animSpeed))
            gapHalf * (0.42f + 0.58f * s)
        }
        ObstacleType.STAPLER -> {
            // Mostly open, then a quick snap shut.
            val s = (sin(animPhase + timeSec * animSpeed) + 1f) / 2f
            val factor = if (s < 0.82f) 1f else 1f - ((s - 0.82f) / 0.18f) * 0.78f
            gapHalf * factor
        }
        else -> gapHalf
    }

    val collidable: Boolean get() = type != ObstacleType.FAN
}

/**
 * A wind zone spanning [startX]..[endX] in world coordinates.
 * [forceY] is a vertical acceleration applied to the plane while inside
 * (positive pushes down, negative pushes up).
 */
data class WindGust(
    val startX: Float,
    val endX: Float,
    val forceY: Float,
) {
    fun contains(worldX: Float): Boolean = worldX in startX..endX
}

data class PlaneState(
    val y: Float = GameConfig.WORLD_HEIGHT / 2f,
    val vy: Float = 0f,
)

/** Paper-scrap particle for the crash burst. */
data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val size: Float,
    val life: Float,
    val colorIndex: Int,
)
