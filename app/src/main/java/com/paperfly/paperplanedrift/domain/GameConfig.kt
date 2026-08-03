package com.paperfly.paperplanedrift.domain

/**
 * All gameplay tuning constants live here. Units are "world units":
 * the visible world is always [WORLD_HEIGHT] units tall, width depends on aspect ratio.
 */
object GameConfig {
    const val WORLD_HEIGHT = 100f
    const val DEFAULT_WORLD_WIDTH = 56f // ~9:16 portrait fallback until the canvas reports its size

    // --- Plane physics ---
    /** Downward acceleration while the finger is up (glide/dive). */
    const val GRAVITY = 215f
    /** Net upward acceleration while holding (already includes gravity). */
    const val CLIMB_ACCEL = -310f
    const val MAX_FALL_SPEED = 95f
    const val MAX_CLIMB_SPEED = -82f

    // --- Forward scroll ---
    const val BASE_FORWARD_SPEED = 36f
    const val MAX_FORWARD_SPEED = 52f
    /** Extra forward speed per world unit travelled (very gentle ramp). */
    const val SPEED_RAMP_PER_UNIT = 0.006f

    // --- Plane placement / size ---
    const val PLANE_X_FRACTION = 0.30f // fraction of viewport width where the plane sits
    const val PLANE_HALF_W = 3.2f
    const val PLANE_HALF_H = 1.9f

    // --- Scoring ---
    const val METERS_PER_UNIT = 0.22f
    const val CLEAN_GLIDE_BONUS = 5
    /** Max vertical drift (world units) inside a gust that still counts as a "clean glide". */
    const val CLEAN_GLIDE_TOLERANCE = 12f

    // --- Spawner / difficulty ---
    const val FIRST_OBSTACLE_X = 150f
    const val BASE_SPACING = 70f
    const val MIN_SPACING = 48f
    const val BASE_GAP_HALF = 17f
    const val MIN_GAP_HALF = 11.5f
    /** Distance (world units) over which difficulty ramps from 0 to 1. */
    const val DIFFICULTY_FULL_DISTANCE = 2400f
    const val BASE_GUST_STRENGTH = 60f
    const val MAX_GUST_STRENGTH = 135f
    const val BASE_GUST_CHANCE = 0.30f
    const val MAX_GUST_CHANCE = 0.75f

    // --- Crash / revive ---
    const val CRASH_ANIM_SECONDS = 0.55f
    const val REVIVE_INVULNERABLE_SECONDS = 1.6f
}
