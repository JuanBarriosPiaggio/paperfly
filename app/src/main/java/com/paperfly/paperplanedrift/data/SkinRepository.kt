package com.paperfly.paperplanedrift.data

/** How a skin gets unlocked. */
sealed class UnlockMethod {
    data object Free : UnlockMethod()
    /** Unlocks automatically once the all-time high score reaches [score]. */
    data class Milestone(val score: Int) : UnlockMethod()
    data object AdWatch : UnlockMethod()
    data class Iap(val productId: String) : UnlockMethod()
}

data class PlaneSkin(
    val id: String,
    val name: String,
    /** ARGB colors used by the canvas renderer. */
    val bodyColor: Long,
    val shadeColor: Long,
    val accentColor: Long,
    val unlock: UnlockMethod,
)

/**
 * Static skin catalog. To add a new skin, append an entry here — the shop,
 * unlock logic, and renderer pick it up automatically.
 */
object SkinRepository {
    const val DEFAULT_SKIN_ID = "dart"
    const val PRODUCT_ORIGAMI = "skin_origami_crane"
    const val PRODUCT_SKIN_PACK = "skin_pack_all"
    const val PRODUCT_REMOVE_ADS = "remove_ads"

    val skins: List<PlaneSkin> = listOf(
        PlaneSkin("dart", "Classic Dart", 0xFFFFFFFF, 0xFFE3DCCB, 0xFF8A8171, UnlockMethod.Free),
        PlaneSkin("sunset", "Sunset Glider", 0xFFFFB74D, 0xFFF57C00, 0xFF8D4A00, UnlockMethod.Milestone(100)),
        PlaneSkin("mint", "Mint Breeze", 0xFFA5E8C8, 0xFF5DBB8E, 0xFF2E7D5B, UnlockMethod.Milestone(250)),
        PlaneSkin("midnight", "Midnight Note", 0xFF546E7A, 0xFF37474F, 0xFFB0BEC5, UnlockMethod.Milestone(500)),
        PlaneSkin("sky", "Sky Ribbon", 0xFF90CAF9, 0xFF42A5F5, 0xFF1565C0, UnlockMethod.AdWatch),
        PlaneSkin("crane", "Origami Crane", 0xFFF8BBD0, 0xFFEC7FA6, 0xFFAD1457, UnlockMethod.Iap(PRODUCT_ORIGAMI)),
    )

    fun byId(id: String): PlaneSkin = skins.firstOrNull { it.id == id } ?: skins.first()

    fun isUnlocked(skin: PlaneSkin, progress: Progress): Boolean = when (val u = skin.unlock) {
        is UnlockMethod.Free -> true
        is UnlockMethod.Milestone -> progress.highScore >= u.score || skin.id in progress.unlockedSkins
        is UnlockMethod.AdWatch -> skin.id in progress.unlockedSkins
        is UnlockMethod.Iap -> skin.id in progress.unlockedSkins
    }
}
