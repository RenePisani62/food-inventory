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

    private val longReceiptDateFormatter =
        DateTimeFormatter.ofPattern(
            "d MMMM yyyy",
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

           // From here

           receipts.forEach { receipt ->

               val parsedDate =
                   parseReceiptDate(
                       receipt.receiptDate
                   )

               android.util.Log.e(
                   "PantryPalAnalytics",
                   "ID=${receipt.id} | " +
                           "store=${receipt.storeName} | " +
                           "rawDate='${receipt.receiptDate}' | " +
                           "parsedDate=$parsedDate | " +
                           "total=${receipt.totalAmount}"
               )
           }
           // To here

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

        val originalValue =
            value.trim()

        /*
         * Normalise a couple of receipt-specific
         * month representations.
         *
         * Example:
         * 16 Sept 2026 -> 16 Sep 2026
         */
        val normalisedValue =
            originalValue.replace(
                Regex("""(?i)\bSept\b"""),
                "Sep"
            )

        /*
         * Standard abbreviated month.
         *
         * Examples:
         * 2 Apr 2026
         * 27 May 2026
         * 16 Sep 2026
         */
        val shortMonthDate =
            runCatching {
                LocalDate.parse(
                    normalisedValue,
                    receiptDateFormatter
                )
            }.getOrNull()

        if (shortMonthDate != null) {
            return shortMonthDate
        }

        /*
         * Full month name.
         *
         * Examples:
         * 17 June 2026
         * 2 July 2026
         * 8 July 2026
         */
        val longMonthDate =
            runCatching {
                LocalDate.parse(
                    normalisedValue,
                    longReceiptDateFormatter
                )
            }.getOrNull()

        if (longMonthDate != null) {
            return longMonthDate
        }

        /*
         * Compact OCR date.
         *
         * Example:
         * 16SEP26
         */
        val compactValue =
            originalValue
                .replace(" ", "")
                .uppercase(Locale.ENGLISH)

        val compactMatch =
            Regex(
                """^(\d{1,2})([A-Z]{3})(\d{2})$"""
            )
                .matchEntire(compactValue)

        if (compactMatch != null) {

            val day =
                compactMatch
                    .groupValues[1]
                    .toIntOrNull()

            val monthText =
                compactMatch
                    .groupValues[2]

            val year =
                compactMatch
                    .groupValues[3]
                    .toIntOrNull()
                    ?.plus(2000)

            val month =
                when (monthText) {
                    "JAN" -> 1
                    "FEB" -> 2
                    "MAR" -> 3
                    "APR" -> 4
                    "MAY" -> 5
                    "JUN" -> 6
                    "JUL" -> 7
                    "AUG" -> 8
                    "SEP" -> 9
                    "OCT" -> 10
                    "NOV" -> 11
                    "DEC" -> 12
                    else -> null
                }

            if (
                day != null &&
                month != null &&
                year != null
            ) {

                return runCatching {
                    LocalDate.of(
                        year,
                        month,
                        day
                    )
                }.getOrNull()
            }
        }

        return null
    }
}