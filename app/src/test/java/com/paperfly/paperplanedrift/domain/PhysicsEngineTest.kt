package com.paperfly.paperplanedrift.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsEngineTest {

    private val physics = PhysicsEngine()

    @Test
    fun `holding accelerates the plane upward`() {
        var state = PlaneState(y = 50f, vy = 0f)
        repeat(30) { state = physics.step(state, holding = true, gustForceY = 0f, dt = 1f / 60f) }
        assertTrue("vy should be negative (climbing)", state.vy < 0f)
        assertTrue("plane should have moved up", state.y < 50f)
    }

    @Test
    fun `releasing lets the plane dive under gravity`() {
        var state = PlaneState(y = 50f, vy = 0f)
        repeat(30) { state = physics.step(state, holding = false, gustForceY = 0f, dt = 1f / 60f) }
        assertTrue("vy should be positive (falling)", state.vy > 0f)
        assertTrue("plane should have moved down", state.y > 50f)
    }

    @Test
    fun `fall speed is clamped`() {
        var state = PlaneState(y = 10f, vy = 0f)
        repeat(600) { state = physics.step(state, holding = false, gustForceY = 0f, dt = 1f / 60f) }
        assertEquals(GameConfig.MAX_FALL_SPEED, state.vy, 0.01f)
    }

    @Test
    fun `updraft gust pushes the plane up compared to no gust`() {
        var withGust = PlaneState(y = 50f, vy = 0f)
        var noGust = PlaneState(y = 50f, vy = 0f)
        repeat(30) {
            withGust = physics.step(withGust, holding = false, gustForceY = -120f, dt = 1f / 60f)
            noGust = physics.step(noGust, holding = false, gustForceY = 0f, dt = 1f / 60f)
        }
        assertTrue(withGust.y < noGust.y)
    }

    @Test
    fun `gust force only applies inside the zone`() {
        val gusts = listOf(WindGust(startX = 100f, endX = 120f, forceY = -80f))
        assertEquals(-80f, physics.gustForceAt(gusts, 110f), 0.001f)
        assertEquals(0f, physics.gustForceAt(gusts, 99f), 0.001f)
        assertEquals(0f, physics.gustForceAt(gusts, 121f), 0.001f)
    }

    @Test
    fun `overlapping gusts stack their forces`() {
        val gusts = listOf(
            WindGust(100f, 120f, -80f),
            WindGust(110f, 130f, 50f),
        )
        assertEquals(-30f, physics.gustForceAt(gusts, 115f), 0.001f)
    }

    @Test
    fun `forward speed ramps up but is capped`() {
        assertEquals(GameConfig.BASE_FORWARD_SPEED, physics.forwardSpeed(0f), 0.001f)
        assertTrue(physics.forwardSpeed(1000f) > physics.forwardSpeed(0f))
        assertEquals(GameConfig.MAX_FORWARD_SPEED, physics.forwardSpeed(1_000_000f), 0.001f)
    }
}
