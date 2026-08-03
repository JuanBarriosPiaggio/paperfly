package com.paperfly.paperplanedrift

import android.app.Application
import android.content.Context
import com.paperfly.paperplanedrift.ads.AdManager
import com.paperfly.paperplanedrift.ads.AdMobAdManager
import com.paperfly.paperplanedrift.ads.InterstitialPolicy
import com.paperfly.paperplanedrift.billing.BillingManager
import com.paperfly.paperplanedrift.data.ProgressRepository
import com.paperfly.paperplanedrift.data.SkinRepository
import com.paperfly.paperplanedrift.data.UnlockMethod
import com.paperfly.paperplanedrift.util.HapticsManager
import com.paperfly.paperplanedrift.util.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaperPlaneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Simple manual DI container — everything the screens and view models need. */
class AppContainer(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val progressRepository = ProgressRepository(context)
    val soundManager = SoundManager()
    val hapticsManager = HapticsManager(context)

    // Swap for MockAdManager() to test gameplay in an emulator with no AdMob at all.
    val adManager: AdManager = AdMobAdManager()
    val interstitialPolicy = InterstitialPolicy()

    val billingManager = BillingManager(context) { productId ->
        scope.launch {
            when (productId) {
                SkinRepository.PRODUCT_REMOVE_ADS -> progressRepository.setAdsRemoved(true)
                SkinRepository.PRODUCT_SKIN_PACK ->
                    SkinRepository.skins
                        .filter { it.unlock is UnlockMethod.Iap }
                        .forEach { progressRepository.unlockSkin(it.id) }
                else ->
                    SkinRepository.skins
                        .firstOrNull { (it.unlock as? UnlockMethod.Iap)?.productId == productId }
                        ?.let { progressRepository.unlockSkin(it.id) }
            }
        }
    }

    init {
        adManager.initialize(context)
    }
}
