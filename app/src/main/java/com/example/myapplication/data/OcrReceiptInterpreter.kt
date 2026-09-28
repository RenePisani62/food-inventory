package com.example.myapplication.data

import com.google.mlkit.vision.text.Text
import kotlin.math.abs

object OcrReceiptInterpreter {

    /*
     * A single piece of text recognised by ML Kit,
     * together with its original position in the image.
     */
    data class OcrLine(
        val text: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val centreX: Int
            get() = (left + right) / 2

        val centreY: Int
            get() = (top + bottom) / 2

        val width: Int
            get() = right - left

        val height: Int
            get() = bottom - top
    }

    /*
     * Lines which appear to occupy the same visual row.
     *
     * We deliberately retain the individual OcrLine
     * objects rather than flattening them into text.
     */
    data class SpatialRow(
        val lines: List<OcrLine>
    ) {
        val top: Int
            get() = lines.minOf { it.top }

        val bottom: Int
            get() = lines.maxOf { it.bottom }

        val left: Int
            get() = lines.minOf { it.left }

        val right: Int
            get() = lines.maxOf { it.right }

        val centreY: Double
            get() =
                lines
                    .map { it.centreY }
                    .average()

        fun text(): String =
            lines
                .sortedBy { it.left }
                .joinToString(" ") {
                    it.text
                }
    }

    /*
     * Spatial representation of the OCR result.
     *
     * This becomes the input for later stages:
     *
     *   receipt-region detection
     *   layout discovery
     *   description/price pairing
     *
     * Nothing here knows anything about Aldi,
     * Priceline, Chemist Warehouse, etc.
     */
    data class SpatialReceipt(
        val lines: List<OcrLine>,
        val rows: List<SpatialRow>
    )

    /*
 * Stage 2D
 *
 * Conservative semantic role assigned to a
 * monetary candidate.
 *
 * UNKNOWN is intentional: if the available
 * evidence is not strong enough, we do not guess.
 */
    enum class MonetaryRole {
        PRODUCT,
        TOTAL,
        PAYMENT,
        CHANGE,
        SAVING,
        COMPARISON,
        TAX,
        UNKNOWN
    }

    fun normalise(
        visionText: Text
    ): String {

        val spatialReceipt =
            buildSpatialReceipt(
                visionText
            )

        if (spatialReceipt.lines.isEmpty()) {
            return visionText.text
        }

        /*
         * Stage 2A deliberately preserves the output
         * behaviour established during Stage 1.
         *
         * Later stages will operate on SpatialReceipt
         * before producing the final normalised text.
         */
        return spatialReceipt
            .rows
            .joinToString("\n") {
                it.text()
            }
    }

    fun parse(
        visionText: Text
    ): ParsedReceipt? {

        val lines =
            extractLines(
                visionText
            )

        if (lines.isEmpty()) {
            return null
        }

        return logPriceRelationships(
            lines
        )
    }

    private fun buildSpatialReceipt(
        visionText: Text
    ): SpatialReceipt {

        val lines =
            extractLines(
                visionText
            )

        if (lines.isEmpty()) {

            return SpatialReceipt(
                lines = emptyList(),
                rows = emptyList()
            )
        }

        val rows =
            buildRows(
                lines
            )

        return SpatialReceipt(
            lines = lines,
            rows = rows
        )
    }

    private fun extractLines(
        visionText: Text
    ): List<OcrLine> {

        return visionText
            .textBlocks
            .flatMap { block ->
                block.lines
            }
            .mapNotNull { line ->

                val box =
                    line.boundingBox
                        ?: return@mapNotNull null

                OcrLine(
                    text = line.text.trim(),
                    left = box.left,
                    top = box.top,
                    right = box.right,
                    bottom = box.bottom
                )
            }
            .filter {
                it.text.isNotBlank()
            }
    }

    private fun buildRows(
        lines: List<OcrLine>
    ): List<SpatialRow> {

        val sortedLines =
            lines.sortedWith(
                compareBy<OcrLine> {
                    it.centreY
                }.thenBy {
                    it.left
                }
            )

        val workingRows =
            mutableListOf<
                    MutableList<OcrLine>
                    >()

        sortedLines.forEach { line ->

            val matchingRow =
                workingRows
                    .lastOrNull()
                    ?.takeIf { row ->

                        val rowCentreY =
                            row
                                .map {
                                    it.centreY
                                }
                                .average()

                        val rowHeight =
                            maxOf(
                                line.height,
                                row.maxOf {
                                    it.height
                                }
                            )

                        val tolerance =
                            maxOf(
                                30,
                                rowHeight / 2
                            )

                        abs(
                            line.centreY -
                                    rowCentreY
                        ) <= tolerance
                    }

            if (matchingRow != null) {

                matchingRow.add(
                    line
                )

            } else {

                workingRows.add(
                    mutableListOf(line)
                )
            }
        }

        return workingRows
            .map { row ->

                SpatialRow(
                    lines =
                        row.sortedBy {
                            it.left
                        }
                )
            }
    }

    private fun classifyMonetaryRole(
        priceLine: OcrLine,
        neighbours: List<OcrLine>
    ): MonetaryRole {

        /*
         * Stage 2D1b
         *
         * Semantic evidence must be spatially local.
         *
         * A label 100 px away must not override a
         * product description only 2 px away.
         */

        data class Evidence(
            val line: OcrLine,
            val verticalDelta: Int
        )

        val evidence =
            neighbours
                .map { candidate ->

                    Evidence(
                        line = candidate,
                        verticalDelta =
                            kotlin.math.abs(
                                candidate.centreY -
                                        priceLine.centreY
                            )
                    )
                }
                .sortedBy {
                    it.verticalDelta
                }

        fun nearestMatching(
            maximumDelta: Int,
            predicate: (String) -> Boolean
        ): Evidence? {

            return evidence
                .firstOrNull { item ->

                    item.verticalDelta <= maximumDelta &&
                            predicate(
                                item.line.text
                                    .trim()
                                    .lowercase()
                            )
                }
        }

        /*
         * Very strong semantic labels.
         *
         * These must be genuinely close to the
         * monetary value they describe.
         */

        nearestMatching(30) {
            "why pay" in it
        }?.let {
            return MonetaryRole.COMPARISON
        }

        nearestMatching(30) {
            "you have saved" in it ||
                    "you saved" in it
        }?.let {
            return MonetaryRole.SAVING
        }

        nearestMatching(30) {
            "change" in it
        }?.let {
            return MonetaryRole.CHANGE
        }

        nearestMatching(30) {
            "gst" in it ||
                    "tax" in it
        }?.let {
            return MonetaryRole.TAX
        }

        nearestMatching(30) {
            "total" in it ||
                    "balance due" in it
        }?.let {
            return MonetaryRole.TOTAL
        }

        nearestMatching(30) {
            "eft" in it ||
                    "purchase" in it ||
                    "amount" in it
        }?.let {
            return MonetaryRole.PAYMENT
        }

        /*
         * Product relationship.
         *
         * Across our current receipts the genuine
         * description is normally extremely close
         * vertically to its transaction price.
         *
         * We still require it to be on the left and
         * contain enough text to plausibly represent
         * a description.
         */

        val productCandidate =
            evidence
                .filter { item ->

                    item.verticalDelta <= 30 &&
                            item.line.centreX <
                            priceLine.centreX &&
                            item.line.text
                                .trim()
                                .length >= 4
                }
                .minByOrNull {
                    it.verticalDelta
                }

        if (productCandidate != null) {
            return MonetaryRole.PRODUCT
        }

        return MonetaryRole.UNKNOWN
    }

    data class MonetaryRelationship(
        val priceLine: OcrLine,
        val role: MonetaryRole
    )

    /*
 * Stage 2D3B
 *
 * A confirmed merchandise relationship.
 *
 * This preserves the original OCR lines and their
 * coordinates rather than flattening them into text.
 *
 * Metadata such as quantity/weight information will
 * be attached during Stage 2D4.
 */
    data class MerchandiseItem(
        val descriptionLine: OcrLine,
        val priceLine: OcrLine,
        val metadataLines: List<OcrLine>
    )

    data class NormalisedReceiptItem(
        val description: String,
        val price: String,
        val metadata: List<String>
    )

    data class NormalisedReceipt(
        val retailer: String?,
        val receiptDate: String?,
        val totalAmount: Double?,
        val items: List<NormalisedReceiptItem>
    )
    private fun findProductDescription(
        priceLine: OcrLine,
        allLines: List<OcrLine>,
        previousPriceLine: OcrLine?
    ): OcrLine? {

        /*
         * Stage 2D3C
         *
         * Sequence-aware product-description pairing.
         *
         * Product descriptions normally appear between
         * the previous merchandise price and very close
         * to the current merchandise price.
         *
         * OCR bounding boxes are not perfectly aligned,
         * so a description may sit a few pixels below
         * its corresponding price.
         */

        val lowerBoundary =
            previousPriceLine
                ?.centreY
                ?: Int.MIN_VALUE

        val verticalTolerance = 12

        val candidates =
            allLines
                .asSequence()
                .filter { candidate ->

                    candidate != priceLine &&
                            candidate.centreY >
                            lowerBoundary &&
                            candidate.centreY <=
                            priceLine.centreY +
                            verticalTolerance &&
                            candidate.text
                                .trim()
                                .length >= 4
                }
                .filter { candidate ->

                    /*
                     * Description should be horizontally
                     * separated from the monetary column.
                     */

                    kotlin.math.abs(
                        candidate.centreX -
                                priceLine.centreX
                    ) >= 40
                }
                .filter { candidate ->

                    /*
                     * Reject obvious weighted-item metadata.
                     * It remains available for metadata
                     * attachment elsewhere.
                     */

                    val text =
                        candidate.text
                            .trim()
                            .lowercase()

                    !(
                            " net @" in text ||
                                    "$/kg" in text
                            )
                }
                .filter { candidate ->

                    /*
                     * Final safety guard.
                     *
                     * A product description must be
                     * physically close to its price.
                     * This prevents footer/header text
                     * from ever winning the pairing.
                     */

                    kotlin.math.abs(
                        candidate.centreY -
                                priceLine.centreY
                    ) <= 60
                }
                .sortedBy { candidate ->

                    /*
                     * Prefer the line physically closest
                     * to the price rather than simply the
                     * lowest line on the page.
                     */

                    kotlin.math.abs(
                        candidate.centreY -
                                priceLine.centreY
                    )
                }
                .toList()

        return candidates.firstOrNull()
    }

    private fun findItemMetadata(
        descriptionLine: OcrLine,
        priceLine: OcrLine,
        nextPriceLine: OcrLine?,
        allLines: List<OcrLine>
    ): List<OcrLine> {

        /*
         * Stage 2D4A
         *
         * Attach weighted-item metadata using the
         * merchandise sequence rather than assuming
         * metadata must physically sit between the
         * description and price.
         *
         * Example:
         *
         * 380072 Garlic per kg
         * 4.47 A
         * 0.149kg Net @ 29.99 $/kg
         *
         * The next merchandise price provides the
         * upper boundary for this item's metadata.
         */

        val lowerBoundary =
            minOf(
                descriptionLine.centreY,
                priceLine.centreY
            )

        val upperBoundary =
            nextPriceLine
                ?.centreY
                ?: (
                        priceLine.centreY +
                                maxOf(
                                    priceLine.height * 3,
                                    60
                                )
                        )

        return allLines
            .asSequence()
            .filter { candidate ->

                candidate != descriptionLine &&
                        candidate != priceLine
            }
            .filter { candidate ->

                candidate.centreY >= lowerBoundary &&
                        candidate.centreY <
                        upperBoundary
            }
            .filter { candidate ->

                val text =
                    candidate.text
                        .trim()
                        .lowercase()

                /*
                 * Deliberately narrow for Stage 2D4A.
                 *
                 * We are recognising known weighted-item
                 * metadata, not arbitrary nearby OCR text.
                 */

                " net @" in text ||
                        "$/kg" in text
            }
            .sortedBy {
                it.centreY
            }
            .toList()
    }

    private fun detectReceiptRetailer(
        allLines: List<OcrLine>
    ): String? {

        val rawText =
            allLines.joinToString("\n") {
                it.text
            }

        return when (
            RetailerDetector.detect(rawText)
        ) {
            Retailer.COLES ->
                "Coles"

            Retailer.WOOLWORTHS ->
                "Woolworths"

            Retailer.ALDI ->
                "ALDI"

            else ->
                null
        }
    }


    private fun extractOcrReceiptDate(
        allLines: List<OcrLine>
    ): String? {

        /*
         * Stage 2E3
         *
         * Keep the original recognised date text for now.
         * Date conversion can happen at the application
         * boundary once the retailer-specific format is known.
         *
         * Aldi example:
         * DATE/TIME 16SEP26 12:00
         */

        val dateTimeRegex =
            Regex(
                """\b\d{1,2}[A-Z]{3}\d{2}\b""",
                RegexOption.IGNORE_CASE
            )

        return allLines
            .asSequence()
            .map {
                it.text.trim()
            }
            .mapNotNull { text ->
                dateTimeRegex
                    .find(text)
                    ?.value
            }
            .firstOrNull()
    }


    private fun extractOcrReceiptTotal(
        allLines: List<OcrLine>
    ): Double? {

        /*
         * Stage 2E3
         *
         * Prefer the receipt summary line:
         *
         * 10 Items $ 37.56
         *
         * This avoids confusing subtotal, GST,
         * EFTPOS amount and surcharge percentage
         * values with the final receipt total.
         */

        val itemSummaryRegex =
            Regex(
                """(?i)\b\d+\s+items?\b.*?[${'$'}]\s*(\d+[.,]\d{2})"""
            )

        val summaryTotal =
            allLines
                .asSequence()
                .map {
                    it.text.trim()
                }
                .mapNotNull { text ->

                    itemSummaryRegex
                        .find(text)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.replace(',', '.')
                        ?.toDoubleOrNull()
                }
                .firstOrNull()

        if (summaryTotal != null) {
            return summaryTotal
        }

        /*
         * Fallback for receipts where TOTAL and its
         * amount were recognised on the same OCR line.
         */

        val explicitTotalRegex =
            Regex(
                """(?i)\btotal\b.*?[${'$'}]\s*(\d+[.,]\d{2})"""
            )

        val explicitTotal =
            allLines
                .asSequence()
                .map {
                    it.text.trim()
                }
                .filterNot { text ->

                    val lower =
                        text.lowercase()

                    "subtotal" in lower ||
                            "surcharge" in lower ||
                            "%" in text
                }
                .mapNotNull { text ->

                    explicitTotalRegex
                        .find(text)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.replace(',', '.')
                        ?.toDoubleOrNull()
                }
                .firstOrNull()

        if (explicitTotal != null) {
            return explicitTotal
        }

        /*
         * Spatial fallback for OCR that separates
         * the TOTAL label from its amount.
         */

        val totalLabel =
            allLines
                .filter { line ->
                    line.text
                        .trim()
                        .uppercase()
                        .let { text ->
                            text == "TOTAL" ||
                                    text == "TOTAL:" ||
                                    text == "TOTAL AUD"
                        }
                }
                .maxByOrNull { line ->
                    line.centreY
                }

        if (totalLabel != null) {

            val nearbyAmount =
                allLines
                    .filter { line ->
                        line != totalLabel &&
                                kotlin.math.abs(
                                    line.centreY -
                                            totalLabel.centreY
                                ) <= 40
                    }
                    .mapNotNull { line ->

                        val amount =
                            Regex("""\d+[.,]\d{2}""")
                                .find(
                                    line.text
                                )
                                ?.value
                                ?.replace(',', '.')
                                ?.toDoubleOrNull()

                        if (amount != null) {
                            line to amount
                        } else {
                            null
                        }
                    }
                    .minByOrNull { pair ->
                        kotlin.math.abs(
                            pair.first.centreY -
                                    totalLabel.centreY
                        )
                    }
                    ?.second

            if (nearbyAmount != null) {
                return nearbyAmount
            }
        }

        return null
    }

    private fun buildNormalisedReceipt(
        items: List<MerchandiseItem>,
        allLines: List<OcrLine>
    ): NormalisedReceipt {

        val normalisedItems =
            items.map { item ->

                NormalisedReceiptItem(
                    description =
                        item.descriptionLine.text.trim(),

                    price =
                        item.priceLine.text.trim(),

                    metadata =
                        item.metadataLines
                            .sortedBy {
                                it.centreY
                            }
                            .map {
                                it.text.trim()
                            }
                )
            }

        return NormalisedReceipt(
            retailer =
                detectReceiptRetailer(
                    allLines
                ),

            receiptDate =
                extractOcrReceiptDate(
                    allLines
                ),

            totalAmount =
                extractOcrReceiptTotal(
                    allLines
                ),

            items =
                normalisedItems
        )
    }

    fun toParsedReceipt(
        receipt: NormalisedReceipt
    ): ParsedReceipt {

        val structuredItems =
            receipt.items.map { item ->

                /*
                 * OCR transaction prices may contain
                 * retailer suffixes such as:
                 *
                 * 7.99 A
                 * 7.99 B
                 * $7.99
                 *
                 * Extract only the monetary value.
                 */

                val price =
                    Regex(
                        """\d+[.,]\d{2}"""
                    )
                        .find(
                            item.price
                        )
                        ?.value
                        ?.replace(',', '.')
                        ?.toDoubleOrNull()

                ParsedReceiptItem(
                    name =
                        item.description,

                    quantity =
                        null,

                    unit =
                        null,

                    unitPrice =
                        null,

                    totalPrice =
                        price
                )
            }

        return ParsedReceipt(
            storeName =
                receipt.retailer
                    ?: "Unknown",

            receiptDate =
                receipt.receiptDate,

            receiptNumber =
                null,

            totalAmount =
                receipt.totalAmount,

            itemCount =
                structuredItems.size,

            products =
                structuredItems.map {
                    it.name
                },

            structuredItems =
                structuredItems,

            adjustments =
                emptyList()
        )
    }

    private fun buildNormalisedMerchandiseText(
        items: List<MerchandiseItem>
    ): String {

        /*
         * Stage 2E1
         *
         * Convert the confirmed spatial merchandise
         * relationships into predictable text.
         *
         * Geometry has already done its job by this point.
         * From here onward we work with logical receipt data.
         */

        return items
            .joinToString("\n") { item ->

                buildList {

                    add(
                        item.descriptionLine.text
                    )

                    item.metadataLines
                        .sortedBy {
                            it.centreY
                        }
                        .forEach {
                                metadataLine ->

                            add(
                                metadataLine.text
                            )
                        }

                    add(
                        item.priceLine.text
                    )
                }
                    .joinToString("\n")
            }
    }
    private fun logMerchandiseSequence(
        relationships: List<MonetaryRelationship>,
        allLines: List<OcrLine>
    ): ParsedReceipt? {

        if (relationships.isEmpty()) {
            return null
        }

        val ordered =
            relationships.sortedBy {
                it.priceLine.centreY
            }

        /*
         * Stage 2D2
         *
         * Locate the first strong transaction boundary
         * after merchandise has begun.
         *
         * TOTAL is deliberately used as the boundary
         * rather than relying on retailer names or
         * receipt coordinates.
         */

        val firstProductIndex =
            ordered.indexOfFirst {
                it.role == MonetaryRole.PRODUCT
            }

        if (firstProductIndex == -1) {

            android.util.Log.e(
                "PantryPalOCRSEQ",
                "MERCHANDISE SEQUENCE | none detected"
            )

            return null
        }

        val boundaryIndex =
            ordered
                .drop(
                    firstProductIndex + 1
                )
                .indexOfFirst {
                    it.role == MonetaryRole.TOTAL
                }
                .let { relativeIndex ->

                    if (relativeIndex == -1) {
                        ordered.size
                    } else {
                        firstProductIndex +
                                1 +
                                relativeIndex
                    }
                }

        val merchandise =
            ordered
                .subList(
                    firstProductIndex,
                    boundaryIndex
                )
                .filter {
                    it.role ==
                            MonetaryRole.PRODUCT
                }

        android.util.Log.e(
            "PantryPalOCRSEQ",
            "MERCHANDISE SEQUENCE | " +
                    "startY=" +
                    ordered[firstProductIndex]
                        .priceLine.centreY +
                    " | " +
                    "boundaryY=" +
                    if (boundaryIndex < ordered.size) {
                        ordered[boundaryIndex]
                            .priceLine.centreY
                    } else {
                        "NONE"
                    } +
                    " | products=${merchandise.size}"
        )

        val pairedItems =
            merchandise
                .mapIndexedNotNull {
                        index,
                        relationship ->

                    val previousPriceLine =
                        merchandise
                            .getOrNull(index - 1)
                            ?.priceLine

                    val description =
                        findProductDescription(
                            priceLine =
                                relationship.priceLine,
                            allLines =
                                allLines,
                            previousPriceLine =
                                previousPriceLine
                        )

                    if (description == null) {

                        android.util.Log.e(
                            "PantryPalOCRPAIR2",
                            "UNPAIRED | " +
                                    "price='" +
                                    relationship.priceLine.text +
                                    "' | " +
                                    "priceY=" +
                                    relationship.priceLine.centreY
                        )

                        null

                    } else {

                        MerchandiseItem(
                            descriptionLine =
                                description,
                            priceLine =
                                relationship.priceLine,
                            metadataLines =
                                findItemMetadata(
                                    descriptionLine =
                                        description,
                                    priceLine =
                                        relationship.priceLine,
                                    nextPriceLine =
                                        merchandise
                                            .getOrNull(index + 1)
                                            ?.priceLine,
                                    allLines =
                                        allLines
                                )
                        )

                    }

                }
        pairedItems.forEachIndexed {
                index,
                item ->

            android.util.Log.e(
                "PantryPalOCRPAIR2",
                "PRODUCT ${index + 1} | " +
                        "description='" +
                        item.descriptionLine.text +
                        "' | " +
                        "price='" +
                        item.priceLine.text +
                        "' | " +
                        "priceY=" +
                        item.priceLine.centreY +
                        " | descriptionY=" +
                        item.descriptionLine.centreY
            )
            item.metadataLines.forEach {
                    metadataLine ->

                android.util.Log.e(
                    "PantryPalOCRPAIR2",
                    "  METADATA | " +
                            "text='" +
                            metadataLine.text +
                            "' | " +
                            "centreY=" +
                            metadataLine.centreY
                )
            }
        }

        android.util.Log.e(
            "PantryPalOCRPAIR2",
            "PAIRING SUMMARY | " +
                    "expected=${merchandise.size} | " +
                    "paired=${pairedItems.size} | " +
                    "unpaired=" +
                    (merchandise.size -
                            pairedItems.size)
        )
        if (pairedItems.isEmpty()) {
            return null
        }

        val normalisedReceipt =
            buildNormalisedReceipt(
                items = pairedItems,
                allLines = allLines
            )

        android.util.Log.e(
            "PantryPalOCRSTRUCT",
            "RECEIPT | " +
                    "retailer=${normalisedReceipt.retailer} | " +
                    "date=${normalisedReceipt.receiptDate} | " +
                    "total=${normalisedReceipt.totalAmount} | " +
                    "items=${normalisedReceipt.items.size}"
        )

        normalisedReceipt.items
            .forEachIndexed { index, item ->

                android.util.Log.e(
                    "PantryPalOCRSTRUCT",
                    "ITEM ${index + 1} | " +
                            "description='${item.description}' | " +
                            "price='${item.price}' | " +
                            "metadata=${item.metadata}"
                )
            }

        val parsedReceipt =
            toParsedReceipt(
                normalisedReceipt
            )

        android.util.Log.e(
            "PantryPalOCRPARSED",
            "RECEIPT | " +
                    "store=${parsedReceipt.storeName} | " +
                    "date=${parsedReceipt.receiptDate} | " +
                    "total=${parsedReceipt.totalAmount} | " +
                    "itemCount=${parsedReceipt.itemCount}"
        )

        parsedReceipt.structuredItems
            .forEachIndexed { index, item ->

                android.util.Log.e(
                    "PantryPalOCRPARSED",
                    "ITEM ${index + 1} | " +
                            "name='${item.name}' | " +
                            "quantity=${item.quantity} | " +
                            "unit=${item.unit} | " +
                            "unitPrice=${item.unitPrice} | " +
                            "totalPrice=${item.totalPrice}"
                )
            }

        val normalisedMerchandise =
            buildNormalisedMerchandiseText(
                pairedItems
            )

        android.util.Log.e(
            "PantryPalOCRNORMAL",
            "NORMALISED MERCHANDISE\n" +
                    normalisedMerchandise
        )

        return parsedReceipt
        if (pairedItems.isNotEmpty()) {

            val normalisedReceipt =
                buildNormalisedReceipt(
                    items = pairedItems,
                    allLines = allLines
                )

            android.util.Log.e(
                "PantryPalOCRSTRUCT",
                "RECEIPT | " +
                        "retailer=${normalisedReceipt.retailer} | " +
                        "date=${normalisedReceipt.receiptDate} | " +
                        "total=${normalisedReceipt.totalAmount} | " +
                        "items=${normalisedReceipt.items.size}"
            )

            normalisedReceipt.items
                .forEachIndexed { index, item ->

                    android.util.Log.e(
                        "PantryPalOCRSTRUCT",
                        "ITEM ${index + 1} | " +
                                "description='${item.description}' | " +
                                "price='${item.price}' | " +
                                "metadata=${item.metadata}"
                    )
                }

            val parsedReceipt =
                toParsedReceipt(
                    normalisedReceipt
                )

            android.util.Log.e(
                "PantryPalOCRPARSED",
                "RECEIPT | " +
                        "store=${parsedReceipt.storeName} | " +
                        "date=${parsedReceipt.receiptDate} | " +
                        "total=${parsedReceipt.totalAmount} | " +
                        "itemCount=${parsedReceipt.itemCount}"
            )

            parsedReceipt.structuredItems
                .forEachIndexed { index, item ->

                    android.util.Log.e(
                        "PantryPalOCRPARSED",
                        "ITEM ${index + 1} | " +
                                "name='${item.name}' | " +
                                "quantity=${item.quantity} | " +
                                "unit=${item.unit} | " +
                                "unitPrice=${item.unitPrice} | " +
                                "totalPrice=${item.totalPrice}"
                    )
                }

            val normalisedMerchandise =
                buildNormalisedMerchandiseText(
                    pairedItems
                )

            android.util.Log.e(
                "PantryPalOCRNORMAL",
                "NORMALISED MERCHANDISE\n" +
                        normalisedMerchandise
            )
        }
    }
    private fun logPriceRelationships(
        lines: List<OcrLine>
    ): ParsedReceipt? {

        if (lines.isEmpty()) {
            return null
        }

        /*
         * Stage 2C1
         *
         * Diagnostic only.
         *
         * Identify lines which look like monetary values,
         * then find nearby OCR text using the original
         * spatial coordinates.
         *
         * Nothing is filtered, paired permanently,
         * parsed, or written to the database.
         */

        val priceRegex =
            Regex(
                """^[${'$'}S]?\s*\d+(?:[.,]\d{2}|\s+\d{2})(?:\s*[A-Z*])?\??${'$'}""",
                RegexOption.IGNORE_CASE
            )

        val priceCandidates =
            lines.filter { line ->

                priceRegex.matches(
                    line.text.trim()
                )
            }
        val relationships =
            mutableListOf<MonetaryRelationship>()

        android.util.Log.e(
            "PantryPalOCRPAIR",
            "PRICE RELATIONSHIP DIAGNOSTIC | " +
                    "candidates=${priceCandidates.size}"
        )

        priceCandidates
            .sortedBy {
                it.centreY
            }
            .forEach { priceLine ->

                /*
                 * Find nearby non-price text.
                 *
                 * At this stage we deliberately look both
                 * above and below the price and both left
                 * and right.
                 */

                val neighbours =
                    lines
                        .asSequence()
                        .filter {
                            it != priceLine
                        }
                        .filter { candidate ->

                            !priceRegex.matches(
                                candidate.text.trim()
                            )
                        }
                        .map { candidate ->

                            val verticalDelta =
                                kotlin.math.abs(
                                    candidate.centreY -
                                            priceLine.centreY
                                )

                            val horizontalDelta =
                                kotlin.math.abs(
                                    candidate.centreX -
                                            priceLine.centreX
                                )

                            Triple(
                                candidate,
                                verticalDelta,
                                horizontalDelta
                            )
                        }
                        .filter { result ->

                            /*
                             * Keep the diagnostic local.
                             *
                             * 140 px vertically is generous
                             * enough to inspect neighbouring
                             * receipt rows without searching
                             * the entire receipt.
                             */

                            result.second <= 140
                        }
                        .sortedWith(
                            compareBy<
                                    Triple<OcrLine, Int, Int>
                                    > {
                                it.second
                            }.thenBy {
                                it.third
                            }
                        )
                        .take(4)
                        .toList()

                val role =
                    classifyMonetaryRole(
                        priceLine = priceLine,
                        neighbours =
                            neighbours.map {
                                it.first
                            }
                    )

                android.util.Log.e(
                    "PantryPalOCRPAIR",
                    "PRICE | " +
                            "text='${priceLine.text}' | " +
                            "role=$role | " +
                            "centreX=${priceLine.centreX} | " +
                            "centreY=${priceLine.centreY}"
                )

                neighbours.forEach { result ->

                    val candidate =
                        result.first

                    val verticalDelta =
                        result.second

                    val horizontalDelta =
                        result.third

                    val horizontalDirection =
                        when {

                            candidate.centreX <
                                    priceLine.centreX ->
                                "LEFT"

                            candidate.centreX >
                                    priceLine.centreX ->
                                "RIGHT"

                            else ->
                                "SAME"
                        }

                    val verticalDirection =
                        when {

                            candidate.centreY <
                                    priceLine.centreY ->
                                "ABOVE"

                            candidate.centreY >
                                    priceLine.centreY ->
                                "BELOW"

                            else ->
                                "SAME"
                        }

                    android.util.Log.e(
                        "PantryPalOCRPAIR",
                        "  NEIGHBOUR | " +
                                "text='${candidate.text}' | " +
                                "dY=$verticalDelta | " +
                                "dX=$horizontalDelta | " +
                                "$horizontalDirection | " +
                                "$verticalDirection"
                    )
                }
                relationships.add(
                    MonetaryRelationship(
                        priceLine = priceLine,
                        role = role
                    )
                )
            }
        return logMerchandiseSequence(
            relationships = relationships,
            allLines = lines
        )
    }


}