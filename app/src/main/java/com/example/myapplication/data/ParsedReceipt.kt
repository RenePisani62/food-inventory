package com.example.myapplication.data

data class ParsedReceipt(

    val storeName: String,

    val receiptDate: String?,

    val receiptNumber: String?,

    val totalAmount: Double?,

    val itemCount: Int?,

    val products: List<String>,

// How the purchase was received.
// Expected values: IN_STORE, CLICK_AND_COLLECT, DELIVERY.
// Null when the source has not yet been determined.
    val receiptSource: String? = null,

    val structuredItems: List<ParsedReceiptItem> = emptyList(),

    val adjustments: List<ParsedReceiptAdjustment> = emptyList()
)