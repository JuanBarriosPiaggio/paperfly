package com.paperfly.paperplanedrift.ads

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Debug/mock implementation: no network, instantly "grants" rewards.
 * Useful for emulator testing without live AdMob credentials.
 */
class MockAdManager : AdManager {

    override val bannerAdUnitId: String? = null

    override fun initialize(context: Context) {
        Log.d("MockAdManager", "initialize")
    }

    override fun preload(activity: Activity) {
        Log.d("MockAdManager", "preload")
    }

    override fun isRewardedReady(): Boolean = true

    override fun showRewarded(activity: Activity, onResult: (Boolean) -> Unit) {
        Log.d("MockAdManager", "showRewarded -> granting reward")
        onResult(true)
    }

    override fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        Log.d("MockAdManager", "showInterstitial -> skipping")
        onDismissed()
    }
}
