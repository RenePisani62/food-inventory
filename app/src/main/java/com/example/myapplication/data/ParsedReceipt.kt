package com.example.myapplication.data

data class ParsedReceipt(

    val storeName: String,

    val receiptDate: String?,

    val receiptNumber: String?,

    val totalAmount: Double?,

    val itemCount: Int?,

    val products: List<String>,

    val structuredItems: List<ParsedReceiptItem> = emptyList(),

    val adjustments: List<ParsedReceiptAdjustment> = emptyList()
)