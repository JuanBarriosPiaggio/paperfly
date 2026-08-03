package com.paperfly.paperplanedrift.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObstacleSpawnerTest {

    @Test
    fun `difficulty scales from zero to one`() {
        val spawner = ObstacleSpawner(seed = 1L)
        assertEquals(0f, spawner.difficulty(0f), 0.001f)
        assertEquals(1f, spawner.difficulty(GameConfig.DIFFICULTY_FULL_DISTANCE), 0.001f)
        assertEquals(1f, spawner.difficulty(GameConfig.DIFFICULTY_FULL_DISTANCE * 10f), 0.001f)
    }

    @Test
    fun `spacing shrinks and gusts strengthen as difficulty rises`() {
        val spawner = ObstacleSpawner(seed = 1L)
        assertTrue(spawner.spacingFor(5000f) < spawner.spacingFor(0f))
        assertTrue(spawner.gapHalfFor(5000f) < spawner.gapHalfFor(0f))
        assertTrue(spawner.gustStrengthFor(5000f) > spawner.gustStrengthFor(0f))
        assertTrue(spawner.gustChanceFor(5000f) > spawner.gustChanceFor(0f))
        // Floors/ceilings.
        assertEquals(GameConfig.MIN_SPACING, spawner.spacingFor(1_000_000f), 0.01f)
        assertEquals(GameConfig.MAX_GUST_STRENGTH, spawner.gustStrengthFor(1_000_000f), 0.01f)
    }

    @Test
    fun `spawner fills the requested range in order`() {
        val spawner = ObstacleSpawner(seed = 7L)
        val spawned = spawner.spawnUpTo(1000f)
        assertTrue(spawned.isNotEmpty())
        val xs = spawned.map { it.first.worldX }
        assertEquals(xs, xs.sorted())
        assertTrue(xs.first() >= GameConfig.FIRST_OBSTACLE_X)
        assertTrue(xs.last() < 1000f)
        // Subsequent calls only spawn beyond what was already produced.
        val more = spawner.spawnUpTo(1500f)
        assertTrue(more.all { it.first.worldX >= xs.last() })
    }

    @Test
    fun `gaps always fit inside the world`() {
        val spawner = ObstacleSpawner(seed = 42L)
        val spawned = spawner.spawnUpTo(20_000f)
        for ((obstacle, _) in spawned) {
            assertTrue(obstacle.gapCenter - obstacle.gapHalf > 0f)
            assertTrue(obstacle.gapCenter + obstacle.gapHalf < GameConfig.WORLD_HEIGHT)
        }
    }

    @Test
    fun `same seed produces the same course - daily challenge determinism`() {
        val a = ObstacleSpawner(seed = 123L).spawnUpTo(3000f)
        val b = ObstacleSpawner(seed = 123L).spawnUpTo(3000f)
        assertEquals(a, b)
    }

    @Test
    fun `windows and fans always come with a gust`() {
        val spawned = ObstacleSpawner(seed = 99L).spawnUpTo(30_000f)
        for ((obstacle, gust) in spawned) {
            if (obstacle.type == ObstacleType.WINDOW || obstacle.type == ObstacleType.FAN) {
                assertTrue("${obstacle.type} should carry a gust", gust != null)
            }
        }
    }
}
