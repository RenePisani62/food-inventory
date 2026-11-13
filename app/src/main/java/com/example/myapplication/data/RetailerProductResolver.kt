package com.example.myapplication.data

import java.util.Locale

/** PP-156 foundation: deterministic retailer-scoped aliases, no speculative matching. */
object RetailerProductResolver {
    enum class MatchLevel { VERIFIED_ALIAS, UNRESOLVED }

    data class ProductIdentity(
        val canonicalId: String,
        val displayName: String,
        val productType: String,
        val variant: String?,
        val brand: String?,
        val packageQuantity: Double?,
        val packageUnit: String?
    )

    data class Alias(
        val retailer: String,
        val receiptDescription: String,
        val product: ProductIdentity,
        val verified: Boolean = true
    )

    data class Result(
        val retailer: String,
        val originalDescription: String,
        val normalizedDescription: String,
        val product: ProductIdentity?,
        val level: MatchLevel
    )

    fun normalizeRetailer(value: String): String = when {
        value.trim().uppercase(Locale.ROOT).contains("WOOLWORTHS") -> "WOOLWORTHS"
        value.trim().uppercase(Locale.ROOT).contains("COLES") -> "COLES"
        value.trim().uppercase(Locale.ROOT).contains("ALDI") -> "ALDI"
        else -> value.trim().uppercase(Locale.ROOT)
    }

    /** Preserve quantities and variant tokens; do NOT use ProductPreferenceKeyResolver here. */
    fun normalizeDescription(value: String): String = value
        .uppercase(Locale.ROOT)
        .replace(Regex("[^A-Z0-9]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun resolve(retailer: String, originalDescription: String, aliases: Collection<Alias>): Result {
        val retailerKey = normalizeRetailer(retailer)
        val descriptionKey = normalizeDescription(originalDescription)
        val candidates = aliases.filter {
            it.verified && normalizeRetailer(it.retailer) == retailerKey &&
                normalizeDescription(it.receiptDescription) == descriptionKey
        }.map { it.product }.distinctBy { it.canonicalId }
        // Conflicting mappings are deliberately unresolved; never guess a price match.
        val product = candidates.singleOrNull()
        return Result(retailerKey, originalDescription, descriptionKey, product,
            if (product == null) MatchLevel.UNRESOLVED else MatchLevel.VERIFIED_ALIAS)
    }
}
