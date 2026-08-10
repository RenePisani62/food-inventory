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