package com.paperfly.paperplanedrift.ads

import android.app.Activity
import android.content.Context

/**
 * All ad calls go through this interface so gameplay can be tested with
 * [MockAdManager] and shipped with [AdMobAdManager].
 */
interface AdManager {
    fun initialize(context: Context)
    /** Preloads rewarded + interstitial ads. Call early (e.g. Activity onCreate). */
    fun preload(activity: Activity)
    fun isRewardedReady(): Boolean
    /** Shows a rewarded ad; [onResult] receives true only if the reward was earned. */
    fun showRewarded(activity: Activity, onResult: (Boolean) -> Unit)
    /** Shows an interstitial if one is loaded; [onDismissed] always fires. */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit)
    /** Banner ad unit ID, or null if this implementation has no banners (mock). */
    val bannerAdUnitId: String?
}

/**
 * Session-scoped interstitial cadence:
 * no interstitials for the first [graceRuns] crashes, then one every [interval] crashes.
 */
class InterstitialPolicy(
    private val graceRuns: Int = 3,
    private val interval: Int = 3,
) {
    private var crashes = 0

    /** Call once per crash; returns true when an interstitial should be shown. */
    fun onCrash(): Boolean {
        crashes++
        return crashes > graceRuns && (crashes - graceRuns) % interval == 0
    }
}
