package com.paperfly.paperplanedrift.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreCalculatorTest {

    @Test
    fun `meters follow the distance conversion`() {
        assertEquals(0, ScoreCalculator.meters(0f))
        assertEquals((1000f * GameConfig.METERS_PER_UNIT).toInt(), ScoreCalculator.meters(1000f))
    }

    @Test
    fun `total score adds clean glide bonuses`() {
        assertEquals(100, ScoreCalculator.totalScore(meters = 100, cleanGlideCount = 0))
        assertEquals(
            100 + 3 * GameConfig.CLEAN_GLIDE_BONUS,
            ScoreCalculator.totalScore(meters = 100, cleanGlideCount = 3),
        )
    }

    @Test
    fun `clean glide respects tolerance`() {
        assertTrue(ScoreCalculator.isCleanGlide(0f))
        assertTrue(ScoreCalculator.isCleanGlide(GameConfig.CLEAN_GLIDE_TOLERANCE))
        assertFalse(ScoreCalculator.isCleanGlide(GameConfig.CLEAN_GLIDE_TOLERANCE + 0.1f))
    }
}
