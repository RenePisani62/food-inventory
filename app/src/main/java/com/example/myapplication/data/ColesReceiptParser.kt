package com.example.myapplication.data

object ColesReceiptParser {

    private fun isOnlineInvoice(rawText: String): Boolean {

        val text = rawText.lowercase()

        return text.contains("online order") &&
                text.contains("invoice number:") &&
                text.contains("invoice date:") &&
                text.contains("payment total")
    }

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

        if (isOnlineInvoice(rawText)) {
            return extractOnlineStructuredItems(rawText)
        }

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

        if (isOnlineInvoice(rawText)) {

            val onlineRegex =
                Regex(
                    """Your\s+trolley\s+\((\d+)\s+items\)""",
                    RegexOption.IGNORE_CASE
                )

            return onlineRegex
                .find(rawText)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }

        // Existing Coles in-store receipt format.
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

        if (isOnlineInvoice(rawText)) {

            val onlineRegex =
                Regex(
                    """Payment\s+total\s+\(with\s+GST\)\s+\$?(\d+\.\d{2})""",
                    RegexOption.IGNORE_CASE
                )

            val onlineMatch =
                onlineRegex.find(rawText)

            android.util.Log.e(
                "ColesOnlineTotal",
                "Matched line/value = ${onlineMatch?.value}"
            )

            return onlineMatch
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
        }

        // Existing Coles in-store receipt format.
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

            if (isOnlineInvoice(rawText)) {

                val onlineRegex =
                    Regex(
                        """Invoice\s+date:\s+(\d{1,2}\s+[A-Za-z]+\s+\d{4})""",
                        RegexOption.IGNORE_CASE
                    )

                val match =
                    onlineRegex
                        .find(rawText)
                        ?.groupValues
                        ?.get(1)
                        ?: return null

                return try {

                    val date =
                        java.time.LocalDate.parse(
                            match,
                            java.time.format.DateTimeFormatter.ofPattern(
                                "d MMMM yyyy",
                                java.util.Locale.ENGLISH
                            )
                        )

                    date.format(
                        java.time.format.DateTimeFormatter.ofPattern(
                            "d MMM yyyy",
                            java.util.Locale.ENGLISH
                        )
                    )

                } catch (e: Exception) {

                    match
                }
            }

            // Existing Coles in-store receipt format.
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
    private fun extractOnlineStructuredItems(
        rawText: String
    ): List<ParsedReceiptItem> {

        val lines =
            rawText
                .lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

        val results =
            mutableListOf<ParsedReceiptItem>()

        // Standard online product row:
        // Coles Blueberries 125g 4 4 $3.50 $14.00
        //
        // Weighted online product row:
        // Coles Cara Cara Oranges approx. 250g each 2 0.540kg $2.90 $1.57
        val productRegex =
            Regex(
                """^(.+?)\s+(\d+)\s+(\d+(?:\.\d+)?(?:kg)?)\s+\$(\d+\.\d{2})\s+\$(\d+\.\d{2})$""",
                RegexOption.IGNORE_CASE
            )

        // Some Coles status rows are split over three lines:
        //
        // Product name
        // Out of Stock
        // 1 0 $11.00 $0.00
        //
        // or:
        //
        // Product name
        // Substitute
        // 0 1 $17.00 $11.00
        val valuesOnlyRegex =
            Regex(
                """^(\d+)\s+(\d+(?:\.\d+)?(?:kg)?)\s+\$(\d+\.\d{2})\s+\$(\d+\.\d{2})$""",
                RegexOption.IGNORE_CASE
            )

        var index = 0

        while (index < lines.size) {

            val line = lines[index]

            val productMatch =
                productRegex.matchEntire(line)

            if (productMatch != null) {

                val name =
                    productMatch
                        .groupValues[1]
                        .trim()
                        .trimStart('*', '%')
                        .trim()

                val ordered =
                    productMatch
                        .groupValues[2]
                        .toDoubleOrNull()

                val pickedText =
                    productMatch.groupValues[3]

                val unitPrice =
                    productMatch
                        .groupValues[4]
                        .toDoubleOrNull()

                val totalPrice =
                    productMatch
                        .groupValues[5]
                        .toDoubleOrNull()

                val isWeight =
                    pickedText.endsWith(
                        "kg",
                        ignoreCase = true
                    )

                val picked =
                    pickedText
                        .removeSuffix("kg")
                        .removeSuffix("KG")
                        .toDoubleOrNull()

                // Do not import products that were not supplied.
                if (picked != null && picked > 0.0) {

                    results.add(
                        ParsedReceiptItem(
                            name = name,
                            quantity = picked,
                            unit =
                                if (isWeight) {
                                    "kg"
                                } else {
                                    "each"
                                },
                            unitPrice = unitPrice,
                            totalPrice = totalPrice
                        )
                    )
                }

                index++
                continue
            }

            // Handle split Out-of-Stock / Substitute rows.
            if (
                index + 2 < lines.size &&
                (
                        lines[index + 1].equals(
                            "Out of Stock",
                            ignoreCase = true
                        ) ||
                                lines[index + 1].equals(
                                    "Substitute",
                                    ignoreCase = true
                                )
                        )
            ) {

                val status =
                    lines[index + 1]

                val valuesMatch =
                    valuesOnlyRegex.matchEntire(
                        lines[index + 2]
                    )

                if (valuesMatch != null) {

                    val name =
                        line
                            .trimStart('*', '%')
                            .trim()

                    val pickedText =
                        valuesMatch.groupValues[2]

                    val unitPrice =
                        valuesMatch
                            .groupValues[3]
                            .toDoubleOrNull()

                    val totalPrice =
                        valuesMatch
                            .groupValues[4]
                            .toDoubleOrNull()

                    val isWeight =
                        pickedText.endsWith(
                            "kg",
                            ignoreCase = true
                        )

                    val picked =
                        pickedText
                            .removeSuffix("kg")
                            .removeSuffix("KG")
                            .toDoubleOrNull()

                    // Out-of-stock originals are deliberately excluded.
                    // Supplied substitutes are retained.
                    if (
                        status.equals(
                            "Substitute",
                            ignoreCase = true
                        ) &&
                        picked != null &&
                        picked > 0.0
                    ) {

                        results.add(
                            ParsedReceiptItem(
                                name = name,
                                quantity = picked,
                                unit =
                                    if (isWeight) {
                                        "kg"
                                    } else {
                                        "each"
                                    },
                                unitPrice = unitPrice,
                                totalPrice = totalPrice
                            )
                        )
                    }

                    index += 3
                    continue
                }
            }

            index++
        }

        return results
    }
    }