package com.example.myapplication.data

import java.time.LocalDate

data class RetailerSpend(
    val retailer: String,
    val amount: Double
)

data class ProductSpend(
    val productName: String,
    val amount: Double
)

data class PriceTrendObservation(
    val receiptDate: LocalDate,
    val retailer: String,
    val price: Double,
    val purchaseCount: Int
)

data class MonthlySpend(
    val year: Int,
    val month: Int,
    val amount: Double,
    val receiptCount: Int,
    val previousMonthChange: Double? = null,
    val previousMonthPercentChange: Double? = null
)

data class AnalyticsSummary(
    val totalSpend: Double,
    val receiptCount: Int,
    val retailerSpend: List<RetailerSpend>,
    val productSpend: List<ProductSpend>,
    val earliestReceiptDate: LocalDate?,
    val latestReceiptDate: LocalDate?,
    val monthlySpend: List<MonthlySpend>
)