package com.paperfly.paperplanedrift.domain

/**
 * Simple constant-acceleration flight model:
 * hold -> climb, release -> dive, plus vertical wind-gust forces.
 */
class PhysicsEngine {

    fun step(state: PlaneState, holding: Boolean, gustForceY: Float, dt: Float): PlaneState {
        val accel = (if (holding) GameConfig.CLIMB_ACCEL else GameConfig.GRAVITY) + gustForceY
        val vy = (state.vy + accel * dt)
            .coerceIn(GameConfig.MAX_CLIMB_SPEED, GameConfig.MAX_FALL_SPEED)
        val y = state.y + vy * dt
        return PlaneState(y = y, vy = vy)
    }

    /** Sum of the vertical forces of every gust the plane is currently inside. */
    fun gustForceAt(gusts: List<WindGust>, planeWorldX: Float): Float {
        var total = 0f
        for (gust in gusts) {
            if (gust.contains(planeWorldX)) total += gust.forceY
        }
        return total
    }

    fun forwardSpeed(distance: Float): Float =
        (GameConfig.BASE_FORWARD_SPEED + distance * GameConfig.SPEED_RAMP_PER_UNIT)
            .coerceAtMost(GameConfig.MAX_FORWARD_SPEED)
}
