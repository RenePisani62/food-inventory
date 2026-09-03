package com.example.myapplication.data

object ReceiptParser {

    fun parse(rawText: String): ParsedReceipt {

        // ============================================================
        // RECEIPT PARSER - RETAILER-SPECIFIC STRUCTURED ITEM EXTRACTION
        // ============================================================
        val retailer =
            RetailerDetector.detect(rawText)

        val structuredItems =
            when (retailer) {

                Retailer.WOOLWORTHS ->
                    WoolworthsReceiptParser.extractStructuredItems(rawText)

                Retailer.COLES ->
                    ColesReceiptParser.extractStructuredItems(rawText)

                else ->
                    emptyList()
            }
        structuredItems.forEachIndexed { index, item ->

            if (retailer == Retailer.COLES) {

                extractProducts(rawText).forEach { product ->

                    val found =
                        structuredItems.any { structured ->
                            structured.name.equals(
                                product,
                                ignoreCase = true
                            )
                        }

                    if (!found) {
                        android.util.Log.e(
                            "ColesParserCompare",
                            "MISSING: $product"
                        )
                    }
                }
            }

            android.util.Log.e(
                "StructuredReceipt",
                "${index + 1}: " +
                        "${item.name} | " +
                        "qty=${item.quantity} | " +
                        "unit=${item.unit} | " +
                        "unitPrice=${item.unitPrice} | " +
                        "total=${item.totalPrice}"
            )
        }
        val adjustments =
            when (retailer) {

                Retailer.WOOLWORTHS ->
                    WoolworthsReceiptParser.extractAdjustments(rawText)

                else ->
                    emptyList()
            }
        adjustments.forEachIndexed { index, adjustment ->

            android.util.Log.e(
                "ReceiptAdjustment",
                "${index + 1}: ${adjustment.description} | amount=${adjustment.amount}"
            )
        }
        val structuredTotal =
            structuredItems.sumOf {
                it.totalPrice ?: 0.0
            }

        val adjustmentTotal =
            adjustments.sumOf {
                it.amount
            }

        val calculatedTotal =
            structuredTotal + adjustmentTotal

        android.util.Log.e(
            "ReceiptReconcile",
            "structured=$structuredTotal | " +
                    "adjustments=$adjustmentTotal | " +
                    "calculated=$calculatedTotal | " +
                    "receipt=${extractTotal(rawText)}"
        )

        return ParsedReceipt(

            storeName = detectStore(rawText),

            receiptDate = extractReceiptDate(rawText),

            receiptNumber = extractReceiptNumber(rawText),

            totalAmount = extractTotal(rawText),

            itemCount = extractItemCount(rawText),

            products = extractProducts(rawText),

            structuredItems = structuredItems,

            adjustments = adjustments
        )
    }
    fun detectStore(rawText: String): String {

        val text = rawText.lowercase()

        return when {

            "coles" in text -> "Coles"

            "woolworths" in text -> "Woolworths"

            "aldi" in text -> "ALDI"

            else -> "Unknown"

        }
    }
    fun extractReceiptDate(rawText: String): String? {

        return when (RetailerDetector.detect(rawText)) {

            Retailer.COLES ->
                ColesReceiptParser.extractReceiptDate(rawText)

            Retailer.WOOLWORTHS ->
                WoolworthsReceiptParser.extractReceiptDate(rawText)

            else ->
                null
        }
    }
    fun extractTotal(rawText: String): Double? {

        return when (RetailerDetector.detect(rawText)) {

            Retailer.COLES ->
                ColesReceiptParser.extractTotal(rawText)

            Retailer.WOOLWORTHS ->
                WoolworthsReceiptParser.extractTotal(rawText)

            else ->
                null
        }
    }
    fun extractReceiptNumber(rawText: String): String? {

        return when (RetailerDetector.detect(rawText)) {

            Retailer.COLES ->
                ColesReceiptParser.extractReceiptNumber(rawText)

            else ->
                null
        }
    }
    fun extractProducts(rawText: String): List<String> {

        return when (RetailerDetector.detect(rawText)) {

            Retailer.COLES ->
                ColesReceiptParser.extractProducts(rawText)

            Retailer.WOOLWORTHS ->
                WoolworthsReceiptParser.extractProducts(rawText)

            else ->
                emptyList()
        }
    }
    private fun isLikelyProduct(line: String): Boolean {

        val text = line.lowercase()

        if (text.contains("served by")) return false
        if (text.contains("phone")) return false
        if (text.contains("date:")) return false
        if (text.contains("time:")) return false
        if (text.contains("net @")) return false
        if (text.contains("@ $")) return false
        if (text.contains("description")) return false
        if (text.contains("supermarkets")) return false

        return true
    }
    fun extractItemCount(rawText: String): Int? {

        return when (RetailerDetector.detect(rawText)) {

            Retailer.COLES ->
                ColesReceiptParser.extractItemCount(rawText)

            Retailer.WOOLWORTHS ->
                WoolworthsReceiptParser.extractItemCount(rawText)

            else ->
                null
        }
    }
}