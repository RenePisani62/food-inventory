package com.example.myapplication.data

import android.text.Html
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Locale

/** Read-only parser for Coles in-store order HTML / browser MHTML archives.
 * No network requests, account credentials, or inventory writes.
 * Coles markup may change: fail closed when no rows can be recognised.
 */
object ColesRichOrderParser {
    data class Item(
        val productId: String,
        val name: String,
        val quantity: Double,
        val unit: String,
        val lineTotal: Double,
        val imageUrl: String?
    )

    data class Result(val items: List<Item>, val lineTotal: Double)

    private val rowStart = Regex("data-testid=[\\\"']item-in-store--(\\d+)[\\\"']")
    private val title = Regex("<[^>]+data-testid=[\\\"']item-in-store--title[\\\"'][^>]*>(.*?)</[^>]+>", RegexOption.DOT_MATCHES_ALL)
    private val quantity = Regex("<[^>]+data-testid=[\\\"']item-in-store--quantity[\\\"'][^>]*>(.*?)</[^>]+>", RegexOption.DOT_MATCHES_ALL)
    private val priceSection = Regex("<section[^>]+data-testid=[\\\"']product_price[\\\"'][^>]*>(.*?)</section>", RegexOption.DOT_MATCHES_ALL)
    private val price = Regex("(?:aria-label=[\\\"']Price\\s*\\$|>\\s*\\$)([\\d,]+(?:\\.\\d{2})?)")
    private val image = Regex("<img\\b[^>]*data-testid=[\\\"']item-in-store--image[\\\"'][^>]*>", RegexOption.DOT_MATCHES_ALL)
    private val src = Regex("\\bsrc=[\\\"']([^\\\"']+)[\\\"']")
    private val stripTags = Regex("<[^>]*>")
    private val number = Regex("(?:Quantity|Weight)\\s*:\\s*([\\d.]+)", RegexOption.IGNORE_CASE)

    fun parse(stream: InputStream): Result = parseBytes(stream.use { it.readBytes() })

    fun parseBytes(bytes: ByteArray): Result {
        require(bytes.size <= 15_000_000) { "Order archive exceeds 15 MB safety limit" }
        val raw = bytes.toString(Charsets.ISO_8859_1)
        val html = if (raw.trimStart().startsWith("<", ignoreCase = true)) {
            raw
        } else {
            val htmlPart = Regex("Content-Type:\\s*text/html[^\\r\\n]*", RegexOption.IGNORE_CASE).find(raw)
                ?: error("No HTML section in order archive")
            val headerEnd = Regex("\\r?\\n\\r?\\n").find(raw, htmlPart.range.last + 1)
                ?: error("Malformed HTML section")
            val header = raw.substring(htmlPart.range.first, headerEnd.range.first)
            val nextBoundary = Regex("\\r?\\n--[^\\r\\n]+", RegexOption.IGNORE_CASE).find(raw, headerEnd.range.last + 1)
            val payload = raw.substring(headerEnd.range.last + 1, nextBoundary?.range?.first ?: raw.length)
            when {
                header.contains("quoted-printable", ignoreCase = true) -> decodeQuotedPrintable(payload)
                header.contains("base64", ignoreCase = true) -> String(android.util.Base64.decode(payload, android.util.Base64.DEFAULT), Charsets.UTF_8)
                else -> payload
            }
        }
        return parseHtml(html)
    }

    fun parseHtml(html: String): Result {
        val starts = rowStart.findAll(html).toList()
        require(starts.isNotEmpty()) { "No Coles in-store order products found" }
        val items = starts.mapNotNull { match ->
            val next = starts.firstOrNull { it.range.first > match.range.first }?.range?.first ?: html.length
            val section = html.substring(match.range.first, next)
            val rawTitle = title.find(section)?.groupValues?.get(1) ?: return@mapNotNull null
            val rawQty = quantity.find(section)?.groupValues?.get(1) ?: return@mapNotNull null
            val qtyText = clean(rawQty)
            val qty = number.find(qtyText)?.groupValues?.get(1)?.toDoubleOrNull() ?: return@mapNotNull null
            val rawPrice = priceSection.find(section)?.groupValues?.get(1) ?: return@mapNotNull null
            val total = price.find(rawPrice)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return@mapNotNull null
            val imageUrl = image.find(section)?.value?.let { src.find(it)?.groupValues?.get(1) }
            Item(match.groupValues[1], clean(rawTitle), qty, if (qtyText.startsWith("Weight", true)) "kg" else "each", total, imageUrl)
        }
        require(items.isNotEmpty() && items.size == starts.size) { "Incomplete Coles order: ${items.size} of ${starts.size} rows parsed" }
        return Result(items, items.sumOf { it.lineTotal })
    }

    private fun clean(text: String): String = Html.fromHtml(text.replace(stripTags, " "), Html.FROM_HTML_MODE_LEGACY).toString().trim().replace(Regex("\\s+"), " ")

    private fun decodeQuotedPrintable(value: String): String {
        val compact = value.replace("=\\r\\n", "").replace("=\\n", "")
        val bytes = ArrayList<Byte>()
        var i = 0
        while (i < compact.length) {
            if (compact[i] == '=' && i + 2 < compact.length) {
                val hex = compact.substring(i + 1, i + 3).toIntOrNull(16)
                if (hex != null) { bytes.add(hex.toByte()); i += 3; continue }
            }
            bytes.add(compact[i].code.toByte()); i++
        }
        return bytes.toByteArray().toString(Charsets.UTF_8)
    }
}
