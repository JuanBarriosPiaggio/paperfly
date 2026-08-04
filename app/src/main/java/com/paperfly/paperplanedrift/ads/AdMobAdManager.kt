package com.paperfly.paperplanedrift.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real AdMob implementation using production ad unit IDs.
 */
class AdMobAdManager : AdManager {

    companion object {
        private const val REWARDED_ID = "ca-app-pub-5881206053165150/9176736149"
        private const val INTERSTITIAL_ID = "ca-app-pub-5881206053165150/7863654475"
        private const val BANNER_ID = "ca-app-pub-5881206053165150/8609759936"
    }

    private val initialized = AtomicBoolean(false)
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    override val bannerAdUnitId: String get() = BANNER_ID

    override fun initialize(context: Context) {
        if (initialized.getAndSet(true)) return
        Executors.newSingleThreadExecutor().execute {
            runCatching { MobileAds.initialize(context) }
        }
    }

    override fun preload(activity: Activity) {
        loadRewarded(activity)
        loadInterstitial(activity)
    }

    private fun loadRewarded(context: Context) {
        RewardedAd.load(context, REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
            })
    }

    private fun loadInterstitial(context: Context) {
        InterstitialAd.load(context, INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
            })
    }

    override fun isRewardedReady(): Boolean = rewardedAd != null

    override fun showRewarded(activity: Activity, onResult: (Boolean) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onResult(false)
            loadRewarded(activity)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
                onResult(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewarded(activity)
                onResult(false)
            }
        }
        ad.show(activity) { earned = true }
    }

    override fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            loadInterstitial(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial(activity)
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
