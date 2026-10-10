package com.example.myapplication

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient.BillingResponseCode
import android.app.Activity
import com.android.billingclient.api.BillingFlowParams

// ============================================================
// GOOGLE PLAY BILLING MANAGER
// ============================================================

class BillingManager(
    context: Context
) {

    companion object {
        const val PRO_PRODUCT_ID = "pantrypal_pro"
    }

    // ============================================================
// HANDLE GOOGLE PLAY PURCHASE NOTIFICATIONS
// ============================================================

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->

            when (billingResult.responseCode) {

                BillingResponseCode.OK -> {

                    purchases?.forEach { purchase ->
                        processProPurchase(purchase)
                    }
                }

                BillingResponseCode.USER_CANCELED -> {

                    android.util.Log.d(
                        "PantryPalBilling",
                        "User cancelled Pro purchase"
                    )
                }

                else -> {

                    android.util.Log.e(
                        "PantryPalBilling",
                        "Purchase update failed. " +
                                "Response code: ${billingResult.responseCode}, " +
                                "Debug message: ${billingResult.debugMessage}"
                    )
                }
            }
        }

    private val billingClient =
        BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                com.android.billingclient.api
                    .PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

    fun connect(
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {

        billingClient.startConnection(
            object : BillingClientStateListener {

                override fun onBillingSetupFinished(
                    billingResult: BillingResult
                ) {

                    if (
                        billingResult.responseCode ==
                        BillingClient.BillingResponseCode.OK
                    ) {
                        onConnected()
                    } else {
                        onError(
                            "Billing connection failed: " +
                                    billingResult.debugMessage
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Billing service may reconnect later.
                }
            }
        )
    }

    // ============================================================
// QUERY GOOGLE PLAY FOR PANTRYPAL PRO
// ============================================================

    fun queryProProduct(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) {
                billingResult, queryResult ->

            if (billingResult.responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                onError(
                    "Product query failed: ${billingResult.debugMessage}"
                )
                return@queryProductDetailsAsync
            }

            val productDetails: ProductDetails? =
                queryResult.productDetailsList.firstOrNull()

            if (productDetails == null) {
                onError(
                    "Google Play did not return product: $PRO_PRODUCT_ID"
                )
                return@queryProductDetailsAsync
            }

            val price =
                productDetails.oneTimePurchaseOfferDetailsList
                    ?.firstOrNull()
                    ?.formattedPrice
                    ?: "Price not available"

            onResult(
                "Product found: ${productDetails.productId}, " +
                        "Name: ${productDetails.name}, " +
                        "Price: $price"
            )
        }
    }

    // ============================================================
// CHECK EXISTING GOOGLE PLAY PRO PURCHASE
// ============================================================

    fun checkProEntitlement(
        onResult: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) {
                billingResult, purchases ->

            if (billingResult.responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                onError(
                    "Purchase query failed: ${billingResult.debugMessage}"
                )
                return@queryPurchasesAsync
            }

            val ownsPro = purchases.any { purchase ->
                purchase.products.contains(PRO_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            onResult(ownsPro)
        }
    }

    // ============================================================
// ACKNOWLEDGE COMPLETED GOOGLE PLAY PURCHASE
// ============================================================

    fun acknowledgeProPurchase(
        purchase: Purchase,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        // Only acknowledge completed purchases.
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            onError("Purchase is not completed.")
            return
        }

        // Confirm that this purchase belongs to PantryPal Pro.
        if (!purchase.products.contains(PRO_PRODUCT_ID)) {
            onError("Purchase does not contain PantryPal Pro.")
            return
        }

        // Google Play must not be asked to acknowledge twice.
        if (purchase.isAcknowledged) {
            onSuccess()
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->

            if (billingResult.responseCode == BillingResponseCode.OK) {
                onSuccess()
            } else {
                onError(
                    "Purchase acknowledgement failed: " +
                            billingResult.debugMessage
                )
            }
        }
    }

    // ============================================================
// LAUNCH GOOGLE PLAY PRO PURCHASE
// ============================================================

    fun launchProPurchase(
        activity: Activity,
        onError: (String) -> Unit
    ) {

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(queryParams) {
                billingResult, queryResult ->

            if (billingResult.responseCode != BillingResponseCode.OK) {
                onError(
                    "Unable to retrieve Pro product: " +
                            billingResult.debugMessage
                )
                return@queryProductDetailsAsync
            }

            val productDetails =
                queryResult.productDetailsList.firstOrNull {
                    it.productId == PRO_PRODUCT_ID
                }

            if (productDetails == null) {
                onError("PantryPal Pro product not available.")
                return@queryProductDetailsAsync
            }

            val offer =
                productDetails.oneTimePurchaseOfferDetailsList
                    ?.firstOrNull()

            if (offer == null) {
                onError("No Pro purchase offer available.")
                return@queryProductDetailsAsync
            }

            val offerToken = offer.offerToken

            if (offerToken.isNullOrBlank()) {
                onError("Google Play did not provide a valid Pro offer token.")
                return@queryProductDetailsAsync
            }

            val productParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()

            val flowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(productParams)
                    )
                    .build()

            activity.runOnUiThread {

                val result =
                    billingClient.launchBillingFlow(
                        activity,
                        flowParams
                    )

                if (result.responseCode != BillingResponseCode.OK) {
                    onError(
                        "Unable to start purchase: " +
                                result.debugMessage
                    )
                }
            }
        }
    }

    // ============================================================
// PROCESS COMPLETED PRO PURCHASE
// ============================================================

    private fun processProPurchase(purchase: Purchase) {

        if (!purchase.products.contains(PRO_PRODUCT_ID)) {
            return
        }

        when (purchase.purchaseState) {

            Purchase.PurchaseState.PURCHASED -> {

                android.util.Log.d(
                    "PantryPalBilling",
                    "Completed Pro purchase received"
                )

                // IMPORTANT:
                // Verification must be completed before
                // acknowledgement or Pro entitlement activation.
                //
                // This function deliberately does not grant
                // Pro access or acknowledge the purchase yet.
            }

            Purchase.PurchaseState.PENDING -> {

                android.util.Log.d(
                    "PantryPalBilling",
                    "Pro purchase awaiting payment"
                )
            }

            else -> {

                android.util.Log.d(
                    "PantryPalBilling",
                    "Pro purchase not completed"
                )
            }
        }
    }
    fun disconnect() {
        billingClient.endConnection()
    }
}