package com.paperfly.paperplanedrift.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.paperfly.paperplanedrift.ads.AdManager

/**
 * Banner slot: only shown on Game Over and in the Skin Shop, never in flight.
 * Renders nothing when ads are removed or the AdManager has no banner (mock).
 */
@Composable
fun AdBanner(adManager: AdManager, adsRemoved: Boolean, modifier: Modifier = Modifier) {
    val unitId = adManager.bannerAdUnitId
    if (adsRemoved || unitId == null) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = unitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
