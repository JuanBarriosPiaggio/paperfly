package com.paperfly.paperplanedrift.domain

object ScoreCalculator {

    fun meters(distanceUnits: Float): Int =
        (distanceUnits * GameConfig.METERS_PER_UNIT).toInt()

    /** Distance score plus the accumulated clean-glide bonuses. */
    fun totalScore(meters: Int, cleanGlideCount: Int): Int =
        meters + cleanGlideCount * GameConfig.CLEAN_GLIDE_BONUS

    /** A glide through a gust is "clean" if vertical drift stayed inside the tolerance band. */
    fun isCleanGlide(maxDeviation: Float): Boolean =
        maxDeviation <= GameConfig.CLEAN_GLIDE_TOLERANCE
}
