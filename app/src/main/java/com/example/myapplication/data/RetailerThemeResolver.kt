package com.example.myapplication.data

import androidx.compose.ui.graphics.Color
import java.util.Locale

data class RetailerTheme(
    val headerColor: Color,
    val accentColor: Color,
    val retailerName: String
)

object RetailerThemeResolver {
    fun getTheme(store: String?): RetailerTheme {
        val name = store?.trim().orEmpty()
        val normalized = name.lowercase(Locale.ROOT)

        return when {
            "coles" in normalized -> RetailerTheme(Color(0xFFD32F2F), Color(0xFFFFCDD2), "Coles")
            "woolworths" in normalized -> RetailerTheme(Color(0xFF2E7D32), Color(0xFFC8E6C9), "Woolworths")
            "aldi" in normalized -> RetailerTheme(Color(0xFF173D8D), Color(0xFFDCE8FF), "ALDI")
            "fresko" in normalized -> RetailerTheme(Color(0xFF276749), Color(0xFFD7F1DF), "Fresko")
            "priceline" in normalized -> RetailerTheme(Color(0xFFAD286D), Color(0xFFF9DCEB), "Priceline")
            "chemist warehouse" in normalized -> RetailerTheme(Color(0xFF005CA9), Color(0xFFDCEBFA), "Chemist Warehouse")
            "bunnings" in normalized -> RetailerTheme(Color(0xFF176B39), Color(0xFFD8EEDC), "Bunnings")
            else -> {
                // Stable fallback: identical retailer names get identical colours across launches.
                val palette = listOf(
                    Color(0xFF5C4B8A) to Color(0xFFEAE2F7),
                    Color(0xFF8B532C) to Color(0xFFF5E5D8),
                    Color(0xFF176B78) to Color(0xFFD8EEF0),
                    Color(0xFF7A4262) to Color(0xFFF2E1EC),
                    Color(0xFF52682A) to Color(0xFFEAF0D9)
                )
                val index = (normalized.hashCode().toLong() and 0x7FFFFFFFL).rem(palette.size).toInt()
                val (header, accent) = palette[index]
                RetailerTheme(header, accent, name.ifBlank { "Unknown" })
            }
        }
    }
}
