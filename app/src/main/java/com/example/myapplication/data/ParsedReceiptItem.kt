package com.example.myapplication.data

data class ParsedReceiptItem(

    val name: String,

    val quantity: Double? = null,

    val unit: String? = null,

    val unitPrice: Double? = null,

    val totalPrice: Double? = null
)