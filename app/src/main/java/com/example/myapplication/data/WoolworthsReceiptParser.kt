package com.example.myapplication.data

object WoolworthsReceiptParser {

    fun extractProducts(rawText: String): List<String> {

        val lines =
            rawText
                .lines()
                .map { it.trim() }

        val endIndex =
            lines.indexOfFirst {
                it.contains(
                    "SUBTOTAL",
                    ignoreCase = true
                )
            }

        if (endIndex == -1) {
            return emptyList()
        }

        return lines
            .subList(
                0,
                endIndex
            )
            .filter { it.isNotBlank() }
            .filterNot {

                val line = it.lowercase()

                line.contains("qty ") ||
                        line.contains("net @") ||
                        line.contains("buy 2 for") ||
                        line.contains("promotional price") ||
                        line.contains("tax invoice") ||
                        line.contains("description") ||
                        line.contains("ph:") ||
                        line.contains("abn")
            }
            .filter {

                it.any { char ->
                    char.isLetter()
                }
            }
    }
    fun extractReceiptDate(rawText: String): String? {

        val regex =
            Regex("""\b\d{2}/\d{2}/\d{4}\b""")

        val match =
            regex.find(rawText)?.value
                ?: return null

        return try {

            val date =
                java.time.LocalDate.parse(
                    match,
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )

            date.format(
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")
            )

        } catch (e: Exception) {

            match
        }
    }

    fun extractTotal(rawText: String): Double? {

        val regex =
            Regex(
                """TOTAL\s+\$?(\d+\.\d{2})""",
                RegexOption.IGNORE_CASE
            )

        return regex
            .find(rawText)
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()
    }

    fun extractItemCount(rawText: String): Int? {

        val regex =
            Regex(
                """(\d+)\s+SUBTOTAL""",
                RegexOption.IGNORE_CASE
            )

        return regex
            .find(rawText)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }
    fun extractStructuredItems(
        rawText: String
    ): List<ParsedReceiptItem> {

        val lines =
            rawText
                .lines()
                .map { it.trim() }

        val endIndex =
            lines.indexOfFirst {
                it.contains(
                    "SUBTOTAL",
                    ignoreCase = true
                )
            }

        if (endIndex == -1) {
            return emptyList()
        }

        val productLines =
            lines.subList(
                0,
                endIndex
            )

        val results =
            mutableListOf<ParsedReceiptItem>()

        val trailingPriceRegex =
            Regex("""\s+(-?\d+\.\d{2})$""")

        val quantityRegex =
            Regex(
                """Qty\s+(\d+(?:\.\d+)?)\s+@\s+\$(\d+\.\d{2})\s+each\s+(-?\d+\.\d{2})""",
                RegexOption.IGNORE_CASE
            )

        val weightRegex =
            Regex(
                """(\d+(?:\.\d+)?)\s+kg\s+NET\s+@\s+\$(\d+\.\d{2})/kg\s+(-?\d+\.\d{2})""",
                RegexOption.IGNORE_CASE
            )

        var index = 0

        while (index < productLines.size) {

            val line =
                productLines[index]

            if (
                line.isBlank() ||
                line.contains("tax invoice", ignoreCase = true) ||
                line.contains("description", ignoreCase = true) ||
                line.contains("ph:", ignoreCase = true) ||
                line.contains("abn", ignoreCase = true) ||
                line.contains("terrigal drive", ignoreCase = true)
            ) {
                index++
                continue
            }

            if (
                line.contains("BUY 2 for", ignoreCase = true) ||
                line.contains("Promotional Price", ignoreCase = true)
            ) {
                index++
                continue
            }

            if (!line.any { it.isLetter() }) {
                index++
                continue
            }

            val nextLine =
                productLines
                    .getOrNull(index + 1)
                    ?.trim()

            val quantityMatch =
                nextLine?.let {
                    quantityRegex.find(it)
                }

            if (quantityMatch != null) {

                val quantity =
                    quantityMatch.groupValues[1]
                        .toDoubleOrNull()

                val unitPrice =
                    quantityMatch.groupValues[2]
                        .toDoubleOrNull()

                val totalPrice =
                    quantityMatch.groupValues[3]
                        .toDoubleOrNull()

                results.add(
                    ParsedReceiptItem(
                        name = cleanProductName(line),
                        quantity = quantity,
                        unit = "each",
                        unitPrice = unitPrice,
                        totalPrice = totalPrice
                    )
                )

                index += 2
                continue
            }

            val weightMatch =
                nextLine?.let {
                    weightRegex.find(it)
                }

            if (weightMatch != null) {

                val quantity =
                    weightMatch.groupValues[1]
                        .toDoubleOrNull()

                val unitPrice =
                    weightMatch.groupValues[2]
                        .toDoubleOrNull()

                val totalPrice =
                    weightMatch.groupValues[3]
                        .toDoubleOrNull()

                results.add(
                    ParsedReceiptItem(
                        name = cleanProductName(line),
                        quantity = quantity,
                        unit = "kg",
                        unitPrice = unitPrice,
                        totalPrice = totalPrice
                    )
                )

                index += 2
                continue
            }

            val priceMatch =
                trailingPriceRegex.find(line)

            val totalPrice =
                priceMatch
                    ?.groupValues
                    ?.get(1)
                    ?.toDoubleOrNull()

            val name =
                if (priceMatch != null) {
                    line
                        .removeRange(priceMatch.range)
                        .trim()
                } else {
                    line
                }

            results.add(
                ParsedReceiptItem(
                    name = cleanProductName(name),
                    quantity = 1.0,
                    unit = "each",
                    unitPrice = totalPrice,
                    totalPrice = totalPrice
                )
            )

            index++
        }

        return results
    }
    private fun cleanProductName(
        value: String
    ): String {

        return value
            .replace("^#", "")
            .replace("#", "")
            .trim()
    }
    fun extractAdjustments(
        rawText: String
    ): List<ParsedReceiptAdjustment> {

        val adjustmentRegex =
            Regex(
                """(BUY\s+\d+\s+for\s+\$\d+\.\d{2}).*?(-\d+\.\d{2})""",
                RegexOption.IGNORE_CASE
            )

        return rawText
            .lines()
            .map { it.trim() }
            .mapNotNull { line ->

                val match =
                    adjustmentRegex.find(line)
                        ?: return@mapNotNull null

                val description =
                    match.groupValues[1].trim()

                val amount =
                    match.groupValues[2]
                        .toDoubleOrNull()
                        ?: return@mapNotNull null

                ParsedReceiptAdjustment(
                    description = description,
                    amount = amount
                )
            }
    }
}