package com.paperfly.paperplanedrift.domain

import kotlin.random.Random

/**
 * Procedurally places obstacle + wind-gust combinations ahead of the plane.
 * All randomness is driven by [seed], so the Daily Challenge can hand every
 * player the same course by seeding with the epoch day.
 */
class ObstacleSpawner(seed: Long) {

    private val random = Random(seed)
    private var nextX = GameConfig.FIRST_OBSTACLE_X
    private var nextId = 0

    /** 0 -> easiest, 1 -> hardest. */
    fun difficulty(distance: Float): Float =
        (distance / GameConfig.DIFFICULTY_FULL_DISTANCE).coerceIn(0f, 1f)

    fun spacingFor(distance: Float): Float =
        lerp(GameConfig.BASE_SPACING, GameConfig.MIN_SPACING, difficulty(distance))

    fun gapHalfFor(distance: Float): Float =
        lerp(GameConfig.BASE_GAP_HALF, GameConfig.MIN_GAP_HALF, difficulty(distance))

    fun gustStrengthFor(distance: Float): Float =
        lerp(GameConfig.BASE_GUST_STRENGTH, GameConfig.MAX_GUST_STRENGTH, difficulty(distance))

    fun gustChanceFor(distance: Float): Float =
        lerp(GameConfig.BASE_GUST_CHANCE, GameConfig.MAX_GUST_CHANCE, difficulty(distance))

    /**
     * Spawns everything whose x is below [untilX]. Returns the new obstacles
     * paired with an optional wind gust attached to each.
     */
    fun spawnUpTo(untilX: Float): List<Pair<Obstacle, WindGust?>> {
        val out = mutableListOf<Pair<Obstacle, WindGust?>>()
        while (nextX < untilX) {
            out += spawnAt(nextX)
            nextX += spacingFor(nextX) * (0.9f + random.nextFloat() * 0.25f)
        }
        return out
    }

    private fun spawnAt(x: Float): Pair<Obstacle, WindGust?> {
        val d = difficulty(x)
        val type = pickType(d)
        val gapHalf = gapHalfFor(x) * if (type == ObstacleType.SCISSORS || type == ObstacleType.STAPLER) 1.35f else 1f
        val margin = gapHalf + 8f
        val gapCenter = margin + random.nextFloat() * (GameConfig.WORLD_HEIGHT - 2f * margin)

        val obstacle = Obstacle(
            id = nextId++,
            type = type,
            worldX = x,
            halfWidth = when (type) {
                ObstacleType.TORN_PAPER -> 4.5f
                ObstacleType.SCISSORS -> 5.0f
                ObstacleType.STAPLER -> 5.5f
                ObstacleType.WINDOW -> 4.0f
                ObstacleType.FAN -> 6.0f
            },
            gapCenter = gapCenter,
            gapHalf = gapHalf,
            animSpeed = if (type == ObstacleType.SCISSORS) 1.6f + d * 1.2f
            else if (type == ObstacleType.STAPLER) 2.2f + d * 1.4f else 0f,
            animPhase = random.nextFloat() * 6.28f,
        )

        val gust = when (type) {
            // Windows always have a gust blowing through the frame; fans ARE a wind zone.
            ObstacleType.WINDOW -> makeGust(x - 10f, x + 14f, gustStrengthFor(x))
            ObstacleType.FAN -> makeGust(x - 12f, x + 12f, gustStrengthFor(x) * 1.25f)
            else -> if (random.nextFloat() < gustChanceFor(x)) {
                // Free-floating gust in the approach to the obstacle.
                val start = x - 34f - random.nextFloat() * 8f
                makeGust(start, start + 16f + random.nextFloat() * 10f, gustStrengthFor(x))
            } else null
        }
        return obstacle to gust
    }

    private fun makeGust(startX: Float, endX: Float, strength: Float): WindGust {
        val sign = if (random.nextBoolean()) 1f else -1f
        return WindGust(startX = startX, endX = endX, forceY = sign * strength)
    }

    private fun pickType(difficulty: Float): ObstacleType {
        // Torn paper dominates early; trickier obstacles blend in as difficulty grows.
        val roll = random.nextFloat()
        return when {
            difficulty < 0.08f -> ObstacleType.TORN_PAPER
            roll < 0.40f - difficulty * 0.15f -> ObstacleType.TORN_PAPER
            roll < 0.55f -> ObstacleType.WINDOW
            roll < 0.70f -> ObstacleType.FAN
            roll < 0.85f -> ObstacleType.SCISSORS
            else -> ObstacleType.STAPLER
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
