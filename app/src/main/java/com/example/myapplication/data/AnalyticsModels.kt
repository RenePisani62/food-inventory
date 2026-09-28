package com.example.myapplication.data

data class RetailerSpend(
    val retailer: String,
    val amount: Double
)

data class ProductSpend(
    val productName: String,
    val amount: Double
)

data class AnalyticsSummary(
    val totalSpend: Double,
    val receiptCount: Int,
    val retailerSpend: List<RetailerSpend>,
    val productSpend: List<ProductSpend>
)