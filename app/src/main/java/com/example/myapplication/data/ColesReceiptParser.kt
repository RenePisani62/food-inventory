package com.example.myapplication.data

object ColesReceiptParser {

    fun extractProducts(rawText: String): List<String> {

       return rawText
            .lines()
            .map { it.trim() }
            .filter { it.any { char -> char.isLetterOrDigit() } }
            .filter { it.isNotBlank() }
            .filter { it.length > 3 }
            .filterNot {

                val line = it.lowercase()

                line.contains("total") ||
                        line.contains("gst") ||
                        line.contains("eft") ||
                        line.contains("change") ||
                        line.contains("flybuys") ||
                        line.contains("receipt") ||
                        line.contains("cashier") ||
                        line.contains("store") ||
                        line.contains("phone") ||
                        line.contains("description") ||
                        line.contains("served") ||
                        line.contains("date:") ||
                        line.contains("time:") ||
                        line.contains("abn") ||
                        line.contains("www.") ||
                        line.contains("http") ||
                        line.contains("supermarkets") ||
                        line.contains("pty ltd") ||
                        line.contains("tax invoice") ||
                        line.contains("store manager") ||
                        line.contains("register") ||
                        line.contains("net @") ||
                        line.contains("@ $") ||
                        line.contains("each") ||
                        line.contains("taxable items") ||
                        line.contains("specials") ||
                        line.contains("loyalty discounts") ||
                        line.contains("credits") ||
                        line.contains("transaction") ||
                        line.contains("terms") ||
                        line.contains("coles.com.au") ||
                        line.contains("purchase") ||
                        line.contains("aud$") ||
                        line.contains("rrn") ||
                        line.contains("approved") ||
                        line.contains("auth") ||
                        line.contains("scanned card") ||
                        line.contains("includes") ||
                        line.all { char -> char == '*' } ||
                        line.matches(Regex(""".*\d{16,}.*""")) ||
                        line == "." ||
                        line.matches(Regex("""^\d+$"""))
            }
    }

    fun extractStructuredItems(
        rawText: String
    ): List<ParsedReceiptItem> {

        val lines =
            rawText
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

        val results =
            mutableListOf<ParsedReceiptItem>()

        // Main product line:
        // PREPACK CARROTS 1KG     5.00
        val productRegex =
            Regex(
                """^[*% ]*(.+?)\s+\$?(-?\d+\.\d{2})$"""
            )

        // Quantity line:
        // 2 @ $2.50 EACH
        val quantityRegex =
            Regex(
                """^(\d+)\s*@\s*\$(\d+\.\d{2})\s*EACH$""",
                RegexOption.IGNORE_CASE
            )

        // Weight line:
        // 0.191 kg NET @ $5.90/kg
        val weightRegex =
            Regex(
                """^(\d+(?:\.\d+)?)\s*kg\s+NET\s*@\s*\$(\d+\.\d{2})/kg$""",
                RegexOption.IGNORE_CASE
            )

        var index = 0

        while (index < lines.size) {

            val line =
                lines[index]

            val productMatch =
                productRegex.matchEntire(line)

            if (productMatch != null) {

                var name =
                    productMatch
                        .groupValues[1]
                        .trim()
                        .trimStart('*', '%')
                        .trim()

                val totalPrice =
                    productMatch
                        .groupValues[2]
                        .toDoubleOrNull()

                // ============================================================
                // COLES PARSER - EXCLUDE NON-PRODUCT RECEIPT LINES
                // ============================================================
                val lowerName =
                    name
                        .lowercase()
                        .replace('\u00A0', ' ')
                        .replace(Regex("""\s+"""), " ")
                        .trim()

                val excluded =
                    lowerName.startsWith("total") ||
                            lowerName == "eft" ||
                            lowerName.startsWith("purchase") ||
                            lowerName.contains("gst included") ||
                            lowerName.contains("total savings") ||
                            lowerName.contains("arnotts 2 for")

                if (!excluded) {

                    var quantity: Double? = 1.0
                    var unit: String? = "each"
                    var unitPrice: Double? = totalPrice

                    // Look at the following line for quantity/weight metadata.
                    if (index + 1 < lines.size) {

                        val nextLine =
                            lines[index + 1]

                        val quantityMatch =
                            quantityRegex.matchEntire(nextLine)

                        val weightMatch =
                            weightRegex.matchEntire(nextLine)

                        when {

                            quantityMatch != null -> {

                                quantity =
                                    quantityMatch
                                        .groupValues[1]
                                        .toDoubleOrNull()

                                unit = "each"

                                unitPrice =
                                    quantityMatch
                                        .groupValues[2]
                                        .toDoubleOrNull()

                                index++
                            }

                            weightMatch != null -> {

                                quantity =
                                    weightMatch
                                        .groupValues[1]
                                        .toDoubleOrNull()

                                unit = "kg"

                                unitPrice =
                                    weightMatch
                                        .groupValues[2]
                                        .toDoubleOrNull()

                                index++
                            }
                        }
                    }

                    results.add(
                        ParsedReceiptItem(
                            name = name,
                            quantity = quantity,
                            unit = unit,
                            unitPrice = unitPrice,
                            totalPrice = totalPrice
                        )
                    )
                }
            }

            index++
        }

        return results
    }

    fun extractItemCount(rawText: String): Int? {

        val regex =
            Regex(
                """Total\s+for\s+(\d+)\s+items:""",
                RegexOption.IGNORE_CASE
            )

        return regex
            .find(rawText)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    fun extractTotal(rawText: String): Double? {

        val regex =
            Regex(
                """Total\s+for\s+\d+\s+items:\s*\$?(\d+\.\d{2})""",
                RegexOption.IGNORE_CASE
            )

        return regex
            .find(rawText)
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()
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

    fun extractReceiptNumber(rawText: String): String? {

        val regex =
            Regex("""\b\d{20}\b""")

        return regex
            .find(rawText)
            ?.value
    }
}