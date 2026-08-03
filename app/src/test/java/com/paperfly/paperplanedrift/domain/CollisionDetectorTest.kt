package com.paperfly.paperplanedrift.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionDetectorTest {

    private fun obstacle(
        type: ObstacleType = ObstacleType.TORN_PAPER,
        worldX: Float = 100f,
        halfWidth: Float = 4.5f,
        gapCenter: Float = 50f,
        gapHalf: Float = 15f,
    ) = Obstacle(
        id = 1, type = type, worldX = worldX, halfWidth = halfWidth,
        gapCenter = gapCenter, gapHalf = gapHalf,
    )

    @Test
    fun `plane inside gap does not collide`() {
        assertFalse(CollisionDetector.collides(100f, 50f, obstacle(), timeSec = 0f))
    }

    @Test
    fun `plane above gap collides`() {
        assertTrue(CollisionDetector.collides(100f, 30f, obstacle(), timeSec = 0f))
    }

    @Test
    fun `plane below gap collides`() {
        assertTrue(CollisionDetector.collides(100f, 70f, obstacle(), timeSec = 0f))
    }

    @Test
    fun `plane horizontally away never collides`() {
        assertFalse(CollisionDetector.collides(50f, 10f, obstacle(), timeSec = 0f))
        assertFalse(CollisionDetector.collides(150f, 90f, obstacle(), timeSec = 0f))
    }

    @Test
    fun `plane clipping gap edge collides`() {
        // Gap spans 35..65; plane center at 36 with halfH 1.9 pokes above the top edge.
        assertTrue(CollisionDetector.collides(100f, 35.5f, obstacle(), timeSec = 0f))
    }

    @Test
    fun `fan never collides`() {
        val fan = obstacle(type = ObstacleType.FAN)
        assertFalse(CollisionDetector.collides(100f, 5f, fan, timeSec = 0f))
        assertFalse(CollisionDetector.collides(100f, 95f, fan, timeSec = 0f))
    }

    @Test
    fun `bounds detection catches floor and ceiling`() {
        assertTrue(CollisionDetector.hitsBounds(0.5f))
        assertTrue(CollisionDetector.hitsBounds(GameConfig.WORLD_HEIGHT - 0.5f))
        assertFalse(CollisionDetector.hitsBounds(GameConfig.WORLD_HEIGHT / 2f))
    }

    @Test
    fun `scissors gap narrows over time and can catch the plane`() {
        val scissors = obstacle(type = ObstacleType.SCISSORS, gapHalf = 15f).copy(animSpeed = 1.6f, animPhase = 0f)
        // At phase 0 (sin = 0) the scissors are nearly closed: factor 0.42.
        val nearlyClosedGap = scissors.currentGapHalf(0f)
        assertTrue(nearlyClosedGap < 15f * 0.5f)
        // A plane that fits a fully open gap gets caught by the nearly closed one.
        val yNearGapTop = 50f - 10f
        assertTrue(CollisionDetector.collides(100f, yNearGapTop, scissors, timeSec = 0f))
    }
}
