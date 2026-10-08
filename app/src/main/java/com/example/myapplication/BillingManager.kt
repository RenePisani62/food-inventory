package com.example.myapplication

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener

// ============================================================
// GOOGLE PLAY BILLING MANAGER
// ============================================================

class BillingManager(
    context: Context
) {

    companion object {
        const val PRO_PRODUCT_ID = "pantrypal_pro"
    }

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->

            // Purchase processing will be added
            // after the billing connection is tested.
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

    fun disconnect() {
        billingClient.endConnection()
    }
}