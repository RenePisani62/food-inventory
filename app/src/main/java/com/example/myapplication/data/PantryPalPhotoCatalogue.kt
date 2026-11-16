package com.example.myapplication.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.Locale

/** PP-157C: conservative, local exact-product photo matching. No database changes. */
object PantryPalPhotoCatalogue {
    private data class Entry(val asset: String, val aliases: List<String>)

    private val entries = listOf(
        Entry("coles_kitchen_chicken_korma_rice_350g.webp", listOf(
            "COLES CHKN KORMA 350GRAM", "COLES CHICKEN KORMA 350G",
            "COLES KITCHEN CHICKEN KORMA RICE 350G"
        )),
        Entry("cadbury_dairy_milk_bubbly_top_deck_160g.webp", listOf(
            "Cadbury Top Deck Bubbly Block 160g",
            "Cadbury Dairy Milk Bubbly Top Deck 160g",
            "Cadbury Bubbly Top Deck 160g"
        )),
        Entry("cadbury_dairy_milk_breakaway_180g.webp", listOf(
            "Cadbury Dairy Milk Breakaway 180g",
            "Cadbury Breakaway 180g"
        )),
        Entry("woolworths_belgian_dark_chocolate_72pct_100g.webp", listOf(
            "Woolworths Belgian Dark Chocolate 72% 100g",
            "Woolworths Belgian Dark Chocolate 72% Cocoa 100g"
        )),
        Entry("golden_palm_pitted_medjool_dates_340g.webp", listOf(
            "Golden Palm Pitted Medjool Dates 340g"
        )),
        // PP-157B: Batch 02. Conservative exact aliases; no unverified barcodes.
        Entry("yopro_salted_caramel_choc_protein_yoghurt.webp", listOf(
            "YoPRO Salted Caramel Choc protein yoghurt", "YoPRO Salted Caramel Choc"
        )),
        Entry("fast_fuel_chicken_mac_cheese.webp", listOf(
            "Fast Fuel Chicken Mac & Cheese", "Fast Fuel Chicken Mac and Cheese"
        )),
        Entry("extra_white_peppermint_gum.webp", listOf(
            "Extra White Peppermint Soft Chew", "Wrigley Extra White Peppermint Soft Chew"
        )),
        Entry("woolworths_chocolate_chunk_cookies_5pack.webp", listOf(
            "Woolworths Chocolate Chunk Cookies 5 Pack", "Woolworths Chocolate Chunk Cookies 5pk"
        )),
        Entry("tim_tam_deluxe_decadent_triple_choc.webp", listOf(
            "Tim Tam Deluxe Decadent Triple Choc", "Arnotts Tim Tam Deluxe Decadent Triple Choc"
        )),
        Entry("tim_tam_mms.webp", listOf(
            "Tim Tam M&Ms", "Arnotts Tim Tam M&Ms", "Tim Tam M&M's"
        )),
        Entry("peckish_parmesan_cheese_herbs.webp", listOf(
            "Peckish Parmesan Cheese & Herbs Rice Crackers", "Peckish Parmesan Cheese and Herbs"
        )),
        Entry("peckish_thai_chilli_lime.webp", listOf(
            "Peckish Thai Chilli & Lime Rice Crackers", "Peckish Thai Chilli and Lime"
        )),
        Entry("peckish_golden_honey_soy.webp", listOf(
            "Peckish Golden Honey & Soy Rice Crackers", "Peckish Golden Honey and Soy"
        ))
    )

    /** Normalise case, punctuation and grams; do not infer a variety from generic descriptions. */
    private fun normalise(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("([a-z])([0-9])"), "$1 $2")
        .replace(Regex("([0-9])([a-z])"), "$1 $2")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            when (token) {
                "grams", "gram", "g" -> "g"
                else -> token
            }
        }
        .replace(Regex("\\b([0-9]+) g\\b"), "$1g")

    /** Recognise the product family without guessing a particular package variant. */
    fun isAmbiguousTimTam(productName: String): Boolean {
        val key = normalise(productName)
        val tokens = key.split(" ").filter { it.isNotBlank() }
        val hasTimTam = key.contains("tim tam") ||
            (tokens.contains("tim") && tokens.contains("ta") && tokens.contains("m"))
        val hasArnott = tokens.any { it == "arnotts" || it == "arnott" }
        if (!hasTimTam || !hasArnott) return false
        // Only descriptions without a confirmed variety receive the generic symbol.
        return entries.none { entry ->
            entry.aliases.any { normalise(it) == key }
        }
    }

    fun imageFor(context: Context, productName: String): Bitmap? {
        val key = normalise(productName)
        val match = entries.firstOrNull { entry ->
            entry.aliases.any { normalise(it) == key }
        } ?: return null
        return try {
            context.assets.open("pp_catalogue/${match.asset}").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }
}
