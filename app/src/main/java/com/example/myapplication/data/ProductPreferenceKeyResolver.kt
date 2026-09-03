package com.example.myapplication.data

object ProductPreferenceKeyResolver {

    fun resolve(productName: String): String {

        return productName
            .lowercase()
            // Remove common receipt pack-size information.
            .replace(
                Regex(
                    """\b\d+(?:\.\d+)?\s*(gram|grams|g|kg|ml|litre|litres|l|pack|pk)\b"""
                ),
                ""
            )
            // Remove punctuation and other receipt noise.
            .replace(
                Regex("[^a-z0-9 ]"),
                " "
            )
            // Collapse repeated whitespace.
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }
}