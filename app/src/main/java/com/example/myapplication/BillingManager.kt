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
import android.util.Base64
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

// ============================================================
// GOOGLE PLAY BILLING MANAGER
// ============================================================

class BillingManager(
    context: Context,
    private val onProPurchased: () -> Unit
) {

    companion object {
        const val PRO_PRODUCT_ID = "pantrypal_pro"

        // Paste the Base64 RSA public license key from Play Console here.
        // Until configured, purchase validation fails closed.
        private const val PLAY_LICENSE_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxXQ0F06gz6ea4dT7aQCBHz5lUE7UJYHcWzFJhVDSDj3ls8vPQSA4ZHQZscoT6melBG8hVBXmhYlnPqRehbLvk7pKTJdJ1c6oXhO7M5QVaTglLnGsK4184VYumsEyUyJOizOIx3OlUcgNK3oyTtUnoLPXJMk078NsDuWIroPYgg1/N0rfebMhC6nCfMqojnCtOSAI5Nq0XlXXzm7EP2/oiNp+W3PvW+ob2axnRhraRUTyOOOwIKdh7Y+OsvB5F9Af4lYjQxOMd2t3PxI2HrvmPhWoRurJKubVbFEquxGU0VssEOsSReRQGaxevaTL+uDkBASdo2FXbTvX1Ylw3MFnQwIDAQAB"
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

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                onError("Purchase query failed: ${billingResult.debugMessage}")
                return@queryPurchasesAsync
            }

            val validPurchases = purchases.filter { purchase ->
                purchase.products.contains(PRO_PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    verifyPurchaseSignature(purchase)
            }

            if (validPurchases.isEmpty()) {
                onResult(false)
                return@queryPurchasesAsync
            }

            // A restored purchase must also be acknowledged, if necessary.
            acknowledgeProPurchase(
                validPurchases.first(),
                onSuccess = { onResult(true) },
                onError = onError
            )
        }
    }

    private fun verifyPurchaseSignature(purchase: Purchase): Boolean {
        if (PLAY_LICENSE_PUBLIC_KEY == "REPLACE_WITH_PLAY_CONSOLE_LICENSE_KEY") {
            android.util.Log.e("PantryPalBilling", "Play license public key is not configured")
            return false
        }

        return try {
            val keyBytes = Base64.decode(PLAY_LICENSE_PUBLIC_KEY, Base64.DEFAULT)
            val publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(keyBytes))
            val verifier = Signature.getInstance("SHA1withRSA")
            verifier.initVerify(publicKey)
            verifier.update(purchase.originalJson.toByteArray(Charsets.UTF_8))
            verifier.verify(Base64.decode(purchase.signature, Base64.DEFAULT))
        } catch (exception: Exception) {
            android.util.Log.e("PantryPalBilling", "Purchase signature verification failed", exception)
            false
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

                android.util.Log.d(
                    "PantryPalBilling",
                    "launchBillingFlow result: " +
                            "code=${result.responseCode}, " +
                            "message=${result.debugMessage}"
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
        if (!purchase.products.contains(PRO_PRODUCT_ID)) return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!verifyPurchaseSignature(purchase)) {
                    android.util.Log.e("PantryPalBilling", "Unverified Pro purchase; access denied")
                    return
                }

                acknowledgeProPurchase(
                    purchase = purchase,
                    onSuccess = {
                        android.util.Log.d("PantryPalBilling", "Verified Pro purchase acknowledged")
                        onProPurchased()
                    },
                    onError = { error ->
                        android.util.Log.e("PantryPalBilling", error)
                    }
                )
            }

            Purchase.PurchaseState.PENDING -> {
                android.util.Log.d("PantryPalBilling", "Pro payment pending; access not granted")
            }

            else -> {
                android.util.Log.d("PantryPalBilling", "Pro purchase not completed")
            }
        }
    }

    fun disconnect() {
        billingClient.endConnection()
    }
}