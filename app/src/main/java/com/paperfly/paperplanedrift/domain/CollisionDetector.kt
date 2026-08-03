package com.paperfly.paperplanedrift.domain

import kotlin.math.abs

object CollisionDetector {

    /** Crash against the desk (floor) or flying off the top of the world. */
    fun hitsBounds(planeY: Float, halfH: Float = GameConfig.PLANE_HALF_H): Boolean =
        planeY - halfH < 0f || planeY + halfH > GameConfig.WORLD_HEIGHT

    /**
     * Axis-aligned check of the plane's bounding box against an obstacle column.
     * The obstacle occupies the full world height except its (possibly animated) gap.
     */
    fun collides(
        planeWorldX: Float,
        planeY: Float,
        obstacle: Obstacle,
        timeSec: Float,
        halfW: Float = GameConfig.PLANE_HALF_W,
        halfH: Float = GameConfig.PLANE_HALF_H,
    ): Boolean {
        if (!obstacle.collidable) return false
        if (abs(planeWorldX - obstacle.worldX) > obstacle.halfWidth + halfW) return false
        val gapHalf = obstacle.currentGapHalf(timeSec)
        val topEdge = obstacle.gapCenter - gapHalf
        val bottomEdge = obstacle.gapCenter + gapHalf
        return planeY - halfH < topEdge || planeY + halfH > bottomEdge
    }

    fun collidesAny(
        planeWorldX: Float,
        planeY: Float,
        obstacles: List<Obstacle>,
        timeSec: Float,
    ): Boolean = obstacles.any { collides(planeWorldX, planeY, it, timeSec) }
}
