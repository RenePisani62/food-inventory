package com.example.myapplication.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AnalyticsPeriod {
    LAST_30_DAYS,
    LAST_3_MONTHS,
    LAST_6_MONTHS,
    LAST_12_MONTHS,
    ALL_TIME
}

object AnalyticsCalculator {

    private val receiptDateFormatter =
        DateTimeFormatter.ofPattern(
            "d MMM yyyy",
            Locale.ENGLISH
        )

    fun calculate(
        receipts: List<ReceiptEntity>,
        receiptItems: List<ReceiptItemEntity>,
        period: AnalyticsPeriod,
        today: LocalDate = LocalDate.now()
    ): AnalyticsSummary {

        val startDate =
            when (period) {
                AnalyticsPeriod.LAST_30_DAYS ->
                    today.minusDays(30)

                AnalyticsPeriod.LAST_3_MONTHS ->
                    today.minusMonths(3)

                AnalyticsPeriod.LAST_6_MONTHS ->
                    today.minusMonths(6)

                AnalyticsPeriod.LAST_12_MONTHS ->
                    today.minusMonths(12)

                AnalyticsPeriod.ALL_TIME ->
                    null
            }

        val filteredReceipts =
            receipts.filter { receipt ->

                val receiptDate =
                    parseReceiptDate(
                        receipt.receiptDate
                    )

                when {
                    period == AnalyticsPeriod.ALL_TIME ->
                        true

                    receiptDate == null ->
                        false

                    startDate == null ->
                        true

                    else ->
                        !receiptDate.isBefore(startDate) &&
                                !receiptDate.isAfter(today)
                }
            }

        val includedReceiptIds =
            filteredReceipts
                .map { it.id.toLong() }
                .toSet()

        val filteredItems =
            receiptItems.filter { item ->
                item.receiptId in includedReceiptIds
            }

        val totalSpend =
            filteredReceipts
                .mapNotNull { it.totalAmount }
                .sum()

        val retailerSpend =
            filteredReceipts
                .filter {
                    !it.storeName.isNullOrBlank() &&
                            it.totalAmount != null
                }
                .groupBy {
                    it.storeName!!.trim()
                }
                .map { (retailer, retailerReceipts) ->
                    RetailerSpend(
                        retailer = retailer,
                        amount = retailerReceipts
                            .mapNotNull { it.totalAmount }
                            .sum()
                    )
                }
                .sortedByDescending {
                    it.amount
                }

        val productSpend =
            filteredItems
                .filter {
                    it.productName.isNotBlank() &&
                            it.totalPrice != null
                }
                .groupBy {
                    it.productName
                        .trim()
                        .lowercase()
                }
                .map { (_, productItems) ->

                    val displayName =
                        productItems
                            .first()
                            .productName
                            .trim()

                    ProductSpend(
                        productName = displayName,
                        amount = productItems
                            .mapNotNull { it.totalPrice }
                            .sum()
                    )
                }
                .sortedByDescending {
                    it.amount
                }

        return AnalyticsSummary(
            totalSpend = totalSpend,
            receiptCount = filteredReceipts.size,
            retailerSpend = retailerSpend,
            productSpend = productSpend
        )
    }

    private fun parseReceiptDate(
        value: String?
    ): LocalDate? {

        if (value.isNullOrBlank()) {
            return null
        }

        return runCatching {
            LocalDate.parse(
                value.trim(),
                receiptDateFormatter
            )
        }.getOrNull()
    }
}