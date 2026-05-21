package com.example.myapplication.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ProductLookup {

    private val client = OkHttpClient()

    fun lookupProductName(barcode: String): String {
        return try {
            val url =
                "https://world.openfoodfacts.org/api/v2/product/$barcode?fields=product_name,brands"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FoodInventoryApp - Android prototype")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return "Unknown Item"
            }

            val body = response.body?.string() ?: return "Unknown Item"
            val json = JSONObject(body)

            val product = json.optJSONObject("product") ?: return "Unknown Item"

            val name = product.optString("product_name", "")
            val brand = product.optString("brands", "")

            when {
                name.isNotBlank() && brand.isNotBlank() -> "$brand - $name"
                name.isNotBlank() -> name
                else -> "Unknown Item"
            }

        } catch (e: Exception) {
            "Unknown Item"
        }
    }
}