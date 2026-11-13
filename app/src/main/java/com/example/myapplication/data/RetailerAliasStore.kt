package com.example.myapplication.data

import android.content.Context
import org.json.JSONObject

/** Local, retailer-scoped aliases learned only from explicit receipt-review name edits.
 * Kept separate from the Room schema until the v17 migration history is established.
 */
class RetailerAliasStore(context: Context) {
    private val prefs = context.getSharedPreferences("pp156b_retailer_aliases", Context.MODE_PRIVATE)

    private fun key(retailer: String, description: String): String =
        RetailerProductResolver.normalizeRetailer(retailer) + "|" +
            RetailerProductResolver.normalizeDescription(description)

    fun lookup(retailer: String, description: String): String? {
        if (retailer.isBlank() || description.isBlank()) return null
        val value = prefs.getString(key(retailer, description), null)?.trim()
        return value?.takeIf { it.isNotBlank() }
    }

    fun learn(retailer: String, originalDescription: String, correctedName: String) {
        if (retailer.isBlank() || originalDescription.isBlank() || correctedName.isBlank()) return
        val normalizedOriginal = RetailerProductResolver.normalizeDescription(originalDescription)
        val normalizedCorrected = RetailerProductResolver.normalizeDescription(correctedName)
        if (normalizedOriginal.isBlank() || normalizedOriginal == normalizedCorrected) return
        prefs.edit().putString(key(retailer, originalDescription), correctedName.trim()).apply()
    }
}
