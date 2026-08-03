package com.paperfly.paperplanedrift.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.paperfly.paperplanedrift.data.SkinRepository

/**
 * Google Play Billing (v8) wrapper for the "Remove Ads" non-consumable
 * and cosmetic skin purchases. Entitlements are reported through
 * [onEntitlement] with the product ID.
 *
 * All calls fail soft: with no Play Store connection the game keeps working.
 */
class BillingManager(
    context: Context,
    private val onEntitlement: (productId: String) -> Unit,
) : PurchasesUpdatedListener {

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    private var productDetails: Map<String, ProductDetails> = emptyMap()

    private val allProductIds = listOf(
        SkinRepository.PRODUCT_REMOVE_ADS,
        SkinRepository.PRODUCT_ORIGAMI,
        SkinRepository.PRODUCT_SKIN_PACK,
    )

    fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Will retry on next connect() call.
            }
        })
    }

    private fun queryProducts() {
        val products = allProductIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()
        // PBL 8: the callback delivers a QueryProductDetailsResult instead of a plain list.
        client.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList.associateBy { it.productId }
            }
        }
    }

    fun priceOf(productId: String): String? =
        productDetails[productId]?.oneTimePurchaseOfferDetails?.formattedPrice

    fun launchPurchase(activity: Activity, productId: String) {
        val details = productDetails[productId]
        if (details == null) {
            Log.w("BillingManager", "No product details for $productId (not connected to Play?)")
            connect()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    /** Re-grants everything already owned (also used by "Restore purchases"). */
    fun restorePurchases() {
        if (!client.isReady) {
            connect()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        purchase.products.forEach { onEntitlement(it) }
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { }
        }
    }
}
