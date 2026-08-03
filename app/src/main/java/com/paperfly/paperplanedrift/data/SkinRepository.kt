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

    // Craft Paper palette: terracotta hero plane, ink outlines throughout.
    val skins: List<PlaneSkin> = listOf(
        PlaneSkin("dart", "Terracotta Dart", 0xFFE2703A, 0xFFC95F2E, 0xFF3A322A, UnlockMethod.Free),
        PlaneSkin("sunset", "Kraft Classic", 0xFFFFFDF4, 0xFFD8CDBA, 0xFF3A322A, UnlockMethod.Milestone(100)),
        PlaneSkin("mint", "Sage Glider", 0xFF7E9A5B, 0xFF5F7A42, 0xFF3A322A, UnlockMethod.Milestone(250)),
        PlaneSkin("midnight", "Midnight Note", 0xFF546E7A, 0xFF37474F, 0xFF3A322A, UnlockMethod.Milestone(500)),
        PlaneSkin("sky", "Teal Breeze", 0xFF4F8C93, 0xFF3D6F75, 0xFF3A322A, UnlockMethod.AdWatch),
        PlaneSkin("crane", "Origami Crane", 0xFFF8BBD0, 0xFFEC7FA6, 0xFF3A322A, UnlockMethod.Iap(PRODUCT_ORIGAMI)),
    )

    fun byId(id: String): PlaneSkin = skins.firstOrNull { it.id == id } ?: skins.first()

    fun isUnlocked(skin: PlaneSkin, progress: Progress): Boolean = when (val u = skin.unlock) {
        is UnlockMethod.Free -> true
        is UnlockMethod.Milestone -> progress.highScore >= u.score || skin.id in progress.unlockedSkins
        is UnlockMethod.AdWatch -> skin.id in progress.unlockedSkins
        is UnlockMethod.Iap -> skin.id in progress.unlockedSkins
    }
}
