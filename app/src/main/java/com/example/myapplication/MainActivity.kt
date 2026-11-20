package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import android.app.DatePickerDialog
import java.util.Calendar
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ProductEntity
import com.example.myapplication.data.ProductLookup
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import android.content.SharedPreferences
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged
import com.example.myapplication.data.BarcodeNameEntity
import androidx.compose.material3.Checkbox
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.util.Log
import com.example.myapplication.data.ShoppingItemEntity
import com.example.myapplication.data.RetailerProductResolver
import com.example.myapplication.data.PantryPalPhotoCatalogue
import com.example.myapplication.data.RetailerAliasStore
import com.example.myapplication.data.ProductKnowledgeResolver
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.example.myapplication.data.ReceiptEntity
import com.example.myapplication.data.ReceiptParser
import com.example.myapplication.data.RetailerThemeResolver
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import com.example.myapplication.data.ReceiptInventoryContributionEntity
import com.example.myapplication.data.ReceiptItemEntity
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import com.example.myapplication.data.HouseholdCategoryResolver
import com.example.myapplication.data.HouseholdCategory
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.myapplication.data.ProductPreferenceKeyResolver
import com.example.myapplication.data.ProductLocationPreferenceEntity
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import com.example.myapplication.data.ProductCategoryPreferenceEntity
import androidx.compose.foundation.BorderStroke
import com.example.myapplication.data.ParsedReceipt
import com.example.myapplication.data.AnalyticsCalculator
import com.example.myapplication.data.AnalyticsPeriod
import com.example.myapplication.data.AnalyticsSummary
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.derivedStateOf
import com.example.myapplication.data.PriceTrendObservation
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import com.example.myapplication.data.MonthlySpend
import com.example.myapplication.data.ColesRichOrderParser



class MainActivity : ComponentActivity() {

    private var scanAction = mutableStateOf("ADD")
    private var expirySummary = mutableStateOf(Pair(0, 0))
    private var expiryInput = mutableStateOf("")
    private var editingProduct = mutableStateOf<ProductEntity?>(null)
    private var editNameInput = mutableStateOf("")
    private var scannedBarcode = mutableStateOf("Ready")
    private var products = mutableStateListOf<ProductEntity>()
    private var quantityInput = mutableStateOf("1")
    private var mode = mutableStateOf("ADD")
    private var showDeleteSelectedReceiptsDialog =
        mutableStateOf(false)
    private var manualShoppingItemInput =
        mutableStateOf("")
    private var priceHistorySearch =
        mutableStateOf("")

    private var priceHistoryResults =
        mutableStateOf<List<ReceiptItemEntity>>(emptyList())
    private var pendingUnknownAmount =
        mutableStateOf(1)
    private var priceHistoryHasSearched =
        mutableStateOf(false)

    private var checkedShoppingItems =
        mutableStateOf<Set<String>>(emptySet())

    // ============================================================
// GOOGLE PLAY BILLING
// ============================================================

    private lateinit var billingManager: BillingManager

    // ============================================================
// PRO ENTITLEMENT - DEVELOPMENT ONLY
// ============================================================

// Temporary switch for testing Free and Pro behaviour.
// Replace with verified Google Play purchase entitlement
// before production release.

    private val isProUser = mutableStateOf(false)

    private var quickScanMode =
        mutableStateOf(false)

    private var currentScreen = mutableStateOf("HOME")

    private val screenHistory =
        mutableListOf<String>()

    private fun navigateTo(
        screen: String
    ) {
        if (currentScreen.value != screen) {

            screenHistory.add(
                currentScreen.value
            )

            currentScreen.value =
                screen
        }
    }

    private fun navigateBack(): Boolean {

        if (screenHistory.isEmpty()) {
            return false
        }

        currentScreen.value =
            screenHistory.removeAt(
                screenHistory.lastIndex
            )

        return true
    }
    private var inventoryStockFilter = mutableStateOf("IN_STOCK")
    private var inventoryFoodFilter = mutableStateOf("ALL")
    private var inventoryCategoryRefresh = mutableLongStateOf(0L)
    private var searchText = mutableStateOf("")
    private var showClearConfirm = mutableStateOf(false)
    private var showDeleteExpiredConfirm = mutableStateOf(false)
    private var showRebuildInventoryConfirm = mutableStateOf(false)

    private var selectedLocation = mutableStateOf("Pantry")

    private lateinit var database: AppDatabase

    private lateinit var prefs: SharedPreferences
    private val retailerAliasStore by lazy { RetailerAliasStore(applicationContext) }

    private fun resolvedReviewName(retailer: String, original: String): String =
        retailerAliasStore.lookup(retailer, original) ?: original
    private val productLookup = ProductLookup()

    private var deleteQuantityInput = mutableStateOf("1")

    private var shoppingListDialogVisible = mutableStateOf(false)

    private var receiptItems =
        mutableStateOf<List<ReceiptEntity>>(emptyList())
    private var shoppingListItems =
        mutableStateOf(mutableListOf<String>())

    private var shoppingListEntities =
        mutableStateOf<List<ShoppingItemEntity>>(emptyList())

    private var showClearReceiptsDialog =
        mutableStateOf(false)

    private var receiptSelectionMode =
        mutableStateOf(false)

    private val lastImportedReceiptId = mutableLongStateOf(0L)
    private val showImportReview = mutableStateOf(false)
    private var selectedReceiptIds =
        mutableStateOf(setOf<Int>())
    private val categories = listOf(
        "Pantry Dry Goods",
        "Canned Goods",
        "Refrigerated: Fresh",
        "Refrigerated: Long-life",
        "Frozen",
        "Bakery",
        "Fruit",
        "Other"
    )

    private var selectedCategory =
        mutableStateOf("Fresh vegetables")

    private var manualBarcodeInput =
        mutableStateOf("")

    private var pendingUnknownBarcode =
        mutableStateOf<String?>(null)

    private var pendingUnknownNameInput =
        mutableStateOf("")

    private fun processBarcode(barcode: String) {

        val amount =
            quantityInput.value.toIntOrNull() ?: 1

        lifecycleScope.launch {

            val dao = database.productDao()

            val expiry =
                expiryInput.value.trim().ifBlank { null }

            val existing =
                dao.getProductByBarcodeAndExpiry(
                    barcode,
                    expiry
                )


            val cachedName =
                dao.getBarcodeName(barcode)

            val productName =
                cachedName?.name ?: withContext(Dispatchers.IO) {
                    productLookup.lookupProductName(barcode)
                }

            val isUnknownProduct =
                productName.isBlank()
                        || productName.equals("Unknown Item", ignoreCase = true)
                        || productName.equals("Unknown", ignoreCase = true)
                        || productName.contains("not found", ignoreCase = true)

            if (
                isUnknownProduct
                && mode.value == "ADD"
            ) {
                pendingUnknownBarcode.value = barcode
                pendingUnknownNameInput.value = ""
                pendingUnknownAmount.value = amount
                return@launch
            }

            if (mode.value == "ADD") {

                val productToUpdate =
                    existing
                        ?: dao.getZeroStockProductByBarcode(barcode)

                if (productToUpdate != null) {

                    dao.updateQuantityById(
                        productToUpdate.id,
                        productToUpdate.quantity + amount
                    )
                    Toast.makeText(
                        this@MainActivity,
                        "Added: ${productToUpdate.itemName}",
                        Toast.LENGTH_SHORT
                    ).show()

                    dao.updateExpiryDateById(
                        productToUpdate.id,
                        expiry
                    )

                } else {
                    val knowledge =
                        ProductKnowledgeResolver.resolve(productName)

                    val resolvedExpiry =
                        expiry ?: java.time.LocalDate.now()
                            .plusDays(knowledge.suggestedShelfLifeDays.toLong())
                            .toString()

                    val newProduct =
                        ProductEntity(
                            barcode = barcode,
                            itemName = productName,
                            quantity = amount,
                            lastScanned = System.currentTimeMillis(),
                            expiryDate = resolvedExpiry,
                            location = knowledge.storageLocation
                        )

                    dao.insertProduct(newProduct)
                    Toast.makeText(
                        this@MainActivity,
                        "Added: $productName",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else if (mode.value == "DELETE") {

                val productToReduce =
                    existing
                        ?: dao.getProductByBarcodeOnly(barcode)

                if (productToReduce != null) {

                    if (productToReduce.quantity <= amount) {

                        // Product is now out of stock.
                        // Keep the product record for shopping-list/history purposes,
                        // but there is no physical stock left to expire.
                        dao.updateQuantityById(
                            productToReduce.id,
                            0
                        )

                        dao.updateExpiryDateById(
                            productToReduce.id,
                            null
                        )

                    } else {

                        dao.subtractQuantityById(
                            productToReduce.id,
                            amount
                        )
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Removed: ${productToReduce.itemName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            scannedBarcode.value =
                "$barcode (x$amount)"

            refreshProducts()

            expirySummary.value =
                getExpirySummary()

            if (quickScanMode.value) {

                applySuggestedExpiryDate()

                scannedBarcode.value = ""

                if (mode.value == "ADD") {
                    launchScanner()
                }
            }
        }
    }

    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result ->

            if (result.contents != null) {

                processBarcode(result.contents)
            }
        }

    // ============================================================
    // OCR - CAPTURE PAPER RECEIPT WITH CAMERA
    // ============================================================

    private var receiptCameraUri: android.net.Uri? = null

    private val receiptCameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                val uri = receiptCameraUri

                if (uri != null) {

                    processReceiptOcr(uri)

                    android.util.Log.e(
                        "PantryPalCamera",
                        "Full-resolution receipt capture successful: $uri"
                    )
                }

            } else {

                android.util.Log.e(
                    "PantryPalCamera",
                    "Receipt camera capture cancelled"
                )
            }
        }

    private val receiptDocumentScannerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {

                val scanResult =
                    com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
                        .fromActivityResultIntent(result.data)

                val imageUri =
                    scanResult
                        ?.pages
                        ?.firstOrNull()
                        ?.imageUri

                if (imageUri != null) {

                    processReceiptOcr(imageUri)

                    android.util.Log.e(
                        "PantryPalScanner",
                        "Document Scanner capture successful: $imageUri"
                    )

                } else {

                    android.util.Log.e(
                        "PantryPalScanner",
                        "Document Scanner returned no image"
                    )
                }

            } else {

                android.util.Log.e(
                    "PantryPalScanner",
                    "Document Scanner cancelled"
                )
            }
        }

    // ============================================================
    // OCR TEST - SELECT RECEIPT IMAGE
    // ============================================================
    private val receiptImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                processReceiptOcr(uri)
            }
        }
    // PP-158A: read-only rich order preview. Intentionally does not write inventory.
    private val richOrderPreview = mutableStateOf<String?>(null)
    // PP-158B.6: transient, editable staging. No receipt/inventory writes.
    private val colesStagingItems = mutableStateOf<List<ColesStagedItem>>(emptyList())
    private val colesPossibleDuplicates = mutableStateOf<List<String>>(emptyList())
    private val colesOrderId = mutableStateOf<String?>(null)
    private val colesOrderDate = mutableStateOf<String?>(null)
    private val colesStoreName = mutableStateOf<String?>(null)
    private val colesExpectedCount = mutableStateOf<Int?>(null)
    private val colesDuplicateStatus = mutableStateOf("Not checked")
    private data class ColesStagedItem(val name: String, val quantity: Double?, val unit: String, val price: Double?)
    private val colesStagingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val raw = result.data?.getStringExtra("pp_coles_staging_items") ?: return@registerForActivityResult
                try {
                    require(raw.length <= 100_000) { "Order preview is too large" }
                    val arr = org.json.JSONArray(raw)
                    require(arr.length() in 1..200) { "Unexpected product count" }
                    val staged = (0 until arr.length()).map { index ->
                        val obj = arr.getJSONObject(index)
                        val name = obj.getString("name").trim()
                        require(name.isNotBlank() && name.length <= 200) { "Invalid product name" }
                        val weight = if (!obj.isNull("weightKg")) obj.getDouble("weightKg") else null
                        val qty = if (weight != null) weight else if (!obj.isNull("quantity")) obj.getDouble("quantity") else null
                        val price = if (!obj.isNull("tentativeLinePrice")) obj.getDouble("tentativeLinePrice") else null
                        require(qty == null || (qty.isFinite() && qty > 0)) { "Invalid quantity" }
                        require(price == null || (price.isFinite() && price >= 0)) { "Invalid price" }
                        ColesStagedItem(name, qty, if (weight != null) "kg" else "each", price)
                    }
                    val meta = result.data?.getStringExtra("pp_coles_order_metadata")?.let { JSONObject(it) }
                    val extractedId = meta?.optString("receiptId")?.trim()?.takeIf { it.isNotBlank() && it != "null" }
                    val extractedDate = meta?.optString("orderDate")?.trim()?.takeIf { it.isNotBlank() && it != "null" }
                    colesOrderId.value = extractedId
                    colesOrderDate.value = extractedDate
                    colesStoreName.value = meta?.optString("storeName")?.trim()?.takeIf { it.isNotBlank() && it != "null" }
                    colesExpectedCount.value = meta?.optInt("expectedCount", -1)?.takeIf { it >= 0 }
                    colesDuplicateStatus.value = "Checking existing receipts…"
                    colesStagingItems.value = staged
                    colesPossibleDuplicates.value = emptyList()
                    currentScreen.value = "COLES_STAGING"
                    // Read-only heuristic. Equal totals/counts do NOT establish receipt identity.
                    lifecycleScope.launch {
                        try {
                            val total = staged.sumOf { it.price ?: 0.0 }
                            val existing = withContext(Dispatchers.IO) { database.receiptDao().getAllReceipts() }
                            // Receipt numbers may differ between an online order and an eReceipt.
                            // A short Coles in-store Receipt ID is NOT globally unique.
                            // Number-only matches are candidates, never confirmed duplicates.
                            val numberMatches = extractedId?.let { id ->
                                existing.filter { receipt ->
                                    receipt.storeName?.contains("coles", ignoreCase = true) == true &&
                                    receipt.receiptNumber?.trim()?.equals(id, ignoreCase = true) == true
                                }
                            }.orEmpty()
                            val possible = existing.filter { receipt ->
                                receipt.storeName?.contains("coles", ignoreCase = true) == true &&
                                receipt.totalAmount != null &&
                                kotlin.math.abs(receipt.totalAmount - total) < 0.005 &&
                                (receipt.itemCount == null || receipt.itemCount == staged.size) &&
                                numberMatches.none { it.id == receipt.id }
                            }
                            colesDuplicateStatus.value = when {
                                numberMatches.isNotEmpty() -> "POSSIBLE: matching short receipt ID (verify store, date and total)"
                                possible.isNotEmpty() -> "POSSIBLE: matching Coles total and compatible count"
                                else -> "No candidate in existing Coles receipts — duplicate status unverified"
                            }
                            colesPossibleDuplicates.value = (numberMatches.map { receipt ->
                                "Matching receipt ID (not conclusive): receipt #${receipt.id}" +
                                    (receipt.receiptDate?.let { " ($it)" } ?: "")
                            } + possible.take(5).map { receipt ->
                                "Possible match: receipt #${receipt.id}" +
                                    (receipt.receiptDate?.let { " ($it)" } ?: "")
                            }).take(8)
                        } catch (e: Exception) {
                            Log.w("PPColesStaging", "Read-only receipt comparison unavailable", e)
                            colesPossibleDuplicates.value = listOf("Existing receipts could not be checked")
                            colesDuplicateStatus.value = "CHECK UNAVAILABLE"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PPColesStaging", "Invalid staged order", e)
                    Toast.makeText(this, "Unable to stage Coles order", Toast.LENGTH_LONG).show()
                }
            }
        }

    @Composable
    private fun ColesStagingScreen() {
        val staged = colesStagingItems.value
        val sum = staged.sumOf { it.price ?: 0.0 }
        val missingPrices = staged.count { it.price == null }
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Coles order — staging review", style = MaterialTheme.typography.titleSmall)
            Text("${staged.size} lines · provisional $" + String.format(Locale.US, "%.2f", sum),
                style = MaterialTheme.typography.bodyMedium)
            Text("Edit rows below. No receipt or inventory changes.", style = MaterialTheme.typography.bodySmall)
            Text("Coles receipt ID: ${colesOrderId.value ?: "Not identified"}", style = MaterialTheme.typography.bodySmall)
            Text("Store: ${colesStoreName.value ?: "Not identified"}", style = MaterialTheme.typography.bodySmall)
            Text("Expected items: ${colesExpectedCount.value?.toString() ?: "Not identified"}" +
                (colesExpectedCount.value?.let { if (it == staged.size) " — count matches" else " — COUNT MISMATCH, review required" } ?: ""),
                style = MaterialTheme.typography.bodySmall)
            Text("Order date: ${colesOrderDate.value ?: "Not identified"}", style = MaterialTheme.typography.bodySmall)
            Text("Duplicate check: ${colesDuplicateStatus.value}", style = MaterialTheme.typography.bodySmall)
            if (missingPrices > 0) {
                Text("$missingPrices prices still need review", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            val possible = colesPossibleDuplicates.value
            if (possible.isNotEmpty()) {
                Text("POSSIBLE DUPLICATE / CHECK REQUIRED", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium)
                possible.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text("Total and item count alone cannot identify an order.", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) {
                items(staged.size) { index ->
                    val item = staged[index]
                    // Remember text locally so partially typed numbers remain editable.
                    var nameText by remember(index, item.name) { mutableStateOf(item.name) }
                    var qtyText by remember(index) { mutableStateOf(item.quantity?.toString() ?: "") }
                    var priceText by remember(index) { mutableStateOf(item.price?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text("Item ${index + 1} · ${item.unit}", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = nameText, onValueChange = { value ->
                                nameText = value
                                if (value.isNotBlank() && value.length <= 200)
                                    colesStagingItems.value = colesStagingItems.value.toMutableList().also {
                                        it[index] = it[index].copy(name = value.trim())
                                    }
                            }, label = { Text("Product") }, singleLine = false,
                            modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 2,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qtyText, onValueChange = { value ->
                                    qtyText = value
                                    val parsed = value.toDoubleOrNull()
                                    colesStagingItems.value = colesStagingItems.value.toMutableList().also {
                                        it[index] = it[index].copy(quantity = parsed?.takeIf { n -> n.isFinite() && n > 0 })
                                    }
                                }, label = { Text("Qty / weight") }, singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = priceText, onValueChange = { value ->
                                    priceText = value
                                    val parsed = value.toDoubleOrNull()
                                    colesStagingItems.value = colesStagingItems.value.toMutableList().also {
                                        it[index] = it[index].copy(price = parsed?.takeIf { n -> n.isFinite() && n >= 0 })
                                    }
                                }, label = { Text("Line total ($)") }, singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
            Button(onClick = {
                colesStagingItems.value = emptyList()
                colesPossibleDuplicates.value = emptyList()
                colesOrderId.value = null
                colesOrderDate.value = null
                colesStoreName.value = null
                colesExpectedCount.value = null
                colesDuplicateStatus.value = "Not checked"
                currentScreen.value = "HOME"
            }, modifier = Modifier.fillMaxWidth()) { Text("Close staging preview") }
            Text("Import disabled — identity and duplicate checks pending.",
                style = MaterialTheme.typography.labelSmall)
        }
    }

    private val richOrderLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(uri)?.use { input ->
                            com.example.myapplication.data.ColesRichOrderParser.parse(input)
                        } ?: error("Unable to open selected file")
                    }
                    richOrderPreview.value = buildString {
                        append("${result.items.size} products found\n")
                        append("Line total: $" + String.format(Locale.US, "%.2f", result.lineTotal) + "\n\n")
                        result.items.forEach { item ->
                            append(item.name + "  × " + item.quantity + " " + item.unit)
                            append("  $" + String.format(Locale.US, "%.2f", item.lineTotal) + "\n")
                        }
                        append("\nPREVIEW ONLY — nothing has been added to inventory.")
                    }
                } catch (e: Exception) {
                    Log.e("PantryPalRichOrder", "Rich order preview failed", e)
                    Toast.makeText(this@MainActivity, "Cannot read Coles order: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

    private val importReceiptLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                importReceiptPdf(uri)
            }
        }

    private fun importReceiptPdf(
        uri: Uri
    ) {

        lifecycleScope.launch {

            try {

                val text =
                    readPdfText(uri)

                if (text.isBlank()) {

                    Toast.makeText(
                        this@MainActivity,
                        "No readable text was found in this receipt.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                val parsedReceipt =
                    ReceiptParser.parse(text)

                val fingerprint =
                    generateReceiptFingerprint(text)


                // Primary duplicate protection:
                // normalized PDF text fingerprint.
                val receiptWithSameFingerprint =
                    database
                        .receiptDao()
                        .getReceiptByFingerprint(fingerprint)

                if (receiptWithSameFingerprint != null) {

                    Toast.makeText(
                        this@MainActivity,
                        "This receipt has already been imported.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // Secondary duplicate protection where
                // the retailer supplied a receipt number.
                val receiptWithSameNumber =
                    parsedReceipt.receiptNumber
                        ?.let { receiptNumber ->

                            if (receiptNumber.isBlank()) {

                                null

                            } else {

                                database
                                    .receiptDao()
                                    .getReceiptByNumber(receiptNumber)
                            }
                        }

                if (receiptWithSameNumber != null) {

                    Toast.makeText(
                        this@MainActivity,
                        "Receipt already imported.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                // ============================================================
                // PDF IMPORT - PREPARE REVIEW WITHOUT SAVING
                // ============================================================

                importedItemsForReview.value =
                    parsedReceipt.structuredItems.map { item ->

                        ImportReviewItem(
                            productName = resolvedReviewName(parsedReceipt.storeName, item.name),
                            originalDescription = item.name,
                            location = resolveStorageLocation(resolvedReviewName(parsedReceipt.storeName, item.name)),
                            category = resolveHouseholdCategory(resolvedReviewName(parsedReceipt.storeName, item.name)),
                            quantity = item.quantity,
                            unit = item.unit,
                            unitPrice = item.unitPrice,
                            totalPrice = item.totalPrice,
                            receiptDate = parsedReceipt.receiptDate
                        )
                    }


                reviewReceiptDate.value =
                    parseReceiptDateForSorting(parsedReceipt.receiptDate)
                        ?.format(DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH))
                        ?: (parsedReceipt.receiptDate ?: "")
                reviewRetailerName.value = parsedReceipt.storeName
                reviewReceiptTotal.value = parsedReceipt.totalAmount?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                reviewDiscountAmount.value = parsedReceipt.adjustments.filter { it.amount < 0.0 }.sumOf { -it.amount }.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.2f", it) } ?: ""


                pendingOcrReceipt.value = parsedReceipt
                pendingOcrRawText.value = text
                addImportedProductsToInventory.value = true

                currentScreen.value = "IMPORT_REVIEW"


            } catch (e: Exception) {

                Log.e(
                    "PantryPalReceipt",
                    "Receipt import failed",
                    e
                )

                Toast.makeText(
                    this@MainActivity,
                    "Receipt import failed: " +
                            "${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val importCsvLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            if (uri != null) {

                lifecycleScope.launch {

                    try {

                        val inputStream =
                            contentResolver.openInputStream(uri)

                        val reader =
                            inputStream?.bufferedReader()

                        val lines =
                            reader?.readLines() ?: emptyList()

                        val dao = database.productDao()

                        database.withTransaction {
                            dao.clearAllProducts()
                            database.receiptInventoryContributionDao().invalidateProductLinks()
                        }

                        lines.drop(1).forEach { line ->

                            val parts = line.split(",").map { value ->
                                value.trim().removeSurrounding("\"")
                            }

                            if (parts.size >= 6) {

                                val product = ProductEntity(
                                    itemName = parts[0],
                                    barcode = parts[1],
                                    quantity = parts[2].toIntOrNull() ?: 1,
                                    expiryDate = parts[3].ifBlank { null },
                                    location = parts[4].ifBlank { "Pantry" },
                                    lastScanned = parts[5].toLongOrNull()
                                        ?: System.currentTimeMillis()
                                )

                                dao.insertProduct(product)
                            }
                        }

                        refreshProducts()

                        expirySummary.value =
                            getExpirySummary()

                    } catch (e: Exception) {

                        e.printStackTrace()
                    }
                }
            }
        }
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchScanner()
            } else {
                scannedBarcode.value = "Camera permission denied"
            }
        }

    private val receiptCameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                launchReceiptCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to scan a paper receipt.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // ============================================================
// IMPORT REVIEW - REVIEW IMPORTED PRODUCT LOCATIONS
// ============================================================

    // ============================================================
// IMPORT REVIEW - REVIEW IMPORTED PRODUCT LOCATIONS
// ============================================================

    // Count discrete purchased units; weighted/measured lines count as one purchase.
    // The measured quantity remains unchanged in ReceiptItemEntity for price history.
    private fun receiptPurchaseUnits(quantity: Double?, unit: String?): Int {
        val measuredUnits = setOf("kg", "g", "gram", "grams", "l", "ml")
        if (unit?.trim()?.lowercase() in measuredUnits) return 1
        val q = quantity ?: return 1
        if (!q.isFinite() || q <= 0.0) return 1
        return if (q % 1.0 != 0.0) 1 else q.toInt().coerceAtLeast(1)
    }

    @Composable
    private fun ImportReviewScreen() {
        // The editor updates the staged item; persistence happens only on Confirm Import.
        var editingReviewIndex by remember { mutableStateOf<Int?>(null) }
        var editProductName by remember { mutableStateOf("") }
        var editProductQuantity by remember { mutableStateOf("") }
        var editProductUnitPrice by remember { mutableStateOf("") }
        var editProductTotal by remember { mutableStateOf("") }

        editingReviewIndex?.let { editIndex ->
            AlertDialog(
                onDismissRequest = { editingReviewIndex = null },
                title = { Text("Edit product") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editProductName,
                            onValueChange = { editProductName = it },
                            label = { Text("Product name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editProductQuantity,
                            onValueChange = { editProductQuantity = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editProductUnitPrice,
                            onValueChange = { editProductUnitPrice = it },
                            label = { Text("Unit price ($) — optional") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editProductTotal,
                            onValueChange = { editProductTotal = it },
                            label = { Text("Line total ($) — optional") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        Text("If both prices are entered, the line total is used for spending; the unit price is stored separately.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = editProductName.trim()
                        val qty = editProductQuantity.trim().toDoubleOrNull()
                        val unitText = editProductUnitPrice.trim().removePrefix("$")
                        val totalText = editProductTotal.trim().removePrefix("$")
                        val unitPrice = unitText.toDoubleOrNull()
                        val lineTotal = totalText.toDoubleOrNull()
                        val valid = name.isNotEmpty() && qty != null && qty.isFinite() && qty > 0.0 &&
                            (unitText.isBlank() || (unitPrice != null && unitPrice.isFinite() && unitPrice >= 0.0)) &&
                            (totalText.isBlank() || (lineTotal != null && lineTotal.isFinite() && lineTotal >= 0.0))
                        if (valid && qty != null) {
                            importedItemsForReview.value = importedItemsForReview.value.mapIndexed { index, item ->
                                if (index == editIndex) item.copy(
                                    productName = name,
                                    manuallyCorrected = item.manuallyCorrected ||
                                        RetailerProductResolver.normalizeDescription(name) !=
                                        RetailerProductResolver.normalizeDescription(item.originalDescription),
                                    quantity = qty,
                                    unitPrice = unitPrice ?: lineTotal?.div(qty),
                                    totalPrice = lineTotal ?: unitPrice?.times(qty)
                                ) else item
                            }
                            editingReviewIndex = null
                        } else {
                            Toast.makeText(this@MainActivity, "Enter a name, positive quantity and valid prices.", Toast.LENGTH_LONG).show()
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editingReviewIndex = null }) { Text("Cancel") }
                }
            )
        }


        // ============================================================
        // IMPORT REVIEW - LIVE RECOGNISED PRODUCT TOTAL
        // ============================================================

        val recognisedProductTotal =
            importedItemsForReview.value.sumOf { item ->
                item.totalPrice
                    ?: item.unitPrice?.let { unitPrice ->
                        unitPrice * (item.quantity ?: 1.0)
                    }
                    ?: 0.0
            }


        // Discount is a positive savings amount. The receipt total remains authoritative.
        val savings = reviewDiscountAmount.value.trim().removePrefix("$").toDoubleOrNull() ?: 0.0
        val afterDiscountTotal = recognisedProductTotal - savings

        if (showAddReviewProductDialog.value) {

            AlertDialog(
                onDismissRequest = {
                    showAddReviewProductDialog.value = false
                },
                title = {
                    Text("Add Missing Product")
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        OutlinedTextField(
                            value = newReviewProductName.value,
                            onValueChange = {
                                newReviewProductName.value = it
                            },
                            label = { Text("Product name") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newReviewProductQuantity.value,
                            onValueChange = {
                                newReviewProductQuantity.value = it
                            },
                            label = { Text("Quantity") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newReviewProductPrice.value,
                            onValueChange = {
                                newReviewProductPrice.value = it
                            },
                            label = { Text("Total price ($)") },
                            singleLine = true
                        )

                        Text("Category")

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Food" to HouseholdCategory.FOOD,
                                "Non-food" to HouseholdCategory.OTHER
                            ).forEach { (label, category) ->
                                FilterChip(
                                    selected =
                                        newReviewProductCategory.value == category,
                                    onClick = {
                                        newReviewProductCategory.value = category
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Text("Storage location")

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Pantry",
                                "Fridge",
                                "Freezer"
                            ).forEach { location ->
                                FilterChip(
                                    selected =
                                        newReviewProductLocation.value == location,
                                    onClick = {
                                        newReviewProductLocation.value = location
                                    },
                                    label = { Text(location) }
                                )
                            }
                        }
                    }
                },

                confirmButton = {
                    TextButton(
                        onClick = {
                            val name =
                                newReviewProductName.value.trim()

                            val quantity =
                                newReviewProductQuantity.value
                                    .trim()
                                    .toDoubleOrNull()

                            val price =
                                newReviewProductPrice.value
                                    .trim()
                                    .toDoubleOrNull()

                            if (
                                name.isNotEmpty() &&
                                quantity != null &&
                                quantity > 0.0 &&
                                price != null &&
                                price >= 0.0
                            ) {
                                val newItem = ImportReviewItem(
                                    productName = name,
                                    location =
                                        newReviewProductLocation.value,
                                    category =
                                        newReviewProductCategory.value,
                                    quantity = quantity,
                                    unit = null,
                                    unitPrice =
                                        if (quantity > 0.0)
                                            price / quantity
                                        else null,
                                    totalPrice = price,
                                    receiptDate =
                                        pendingOcrReceipt.value?.receiptDate
                                )

                                importedItemsForReview.value =
                                    importedItemsForReview.value + newItem

                                showAddReviewProductDialog.value = false
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Enter a product name, positive quantity and valid price.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },

                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddReviewProductDialog.value = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .padding(bottom = 80.dp)
        ) {

            // ============================================================
            // IMPORT REVIEW - PANTRYPAL HEADER
            // ============================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review Receipt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${importedItemsForReview.value.size} products",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            var showReceiptDetails by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${reviewRetailerName.value.ifBlank { "Unknown retailer" }} · $${reviewReceiptTotal.value.ifBlank { "?" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showReceiptDetails = !showReceiptDetails }) {
                    Text(if (showReceiptDetails) "Hide details" else "Edit details")
                }
            }
            if (showReceiptDetails) {
            OutlinedTextField(
                value = reviewRetailerName.value,
                onValueChange = { reviewRetailerName.value = it },
                label = { Text("Retailer name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().onFocusChanged { state ->
                    if (state.isFocused && reviewRetailerName.value.trim().lowercase() in
                        setOf("unknown", "unknown retailer", "unrecognised", "unrecognized")) {
                        reviewRetailerName.value = ""
                    }
                }
            )
            OutlinedTextField(
                value = reviewReceiptTotal.value,
                onValueChange = { reviewReceiptTotal.value = it },
                label = { Text("Receipt total ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().onFocusChanged { state ->
                    if (state.isFocused && reviewReceiptTotal.value.trim().lowercase() in
                        setOf("unknown", "unknown total", "unrecognised", "unrecognized", "?") ) {
                        reviewReceiptTotal.value = ""
                    }
                }
            )
            OutlinedTextField(
                value = reviewDiscountAmount.value,
                onValueChange = { reviewDiscountAmount.value = it },
                label = { Text("Promotional savings ($) — optional") },
                placeholder = { Text("e.g. 8.30") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Enter total savings as a positive amount. Receipt total is the amount paid.",
                style = MaterialTheme.typography.bodySmall)
            val manuallyEnteredTotal = reviewReceiptTotal.value.trim().removePrefix("$").toDoubleOrNull()
            if (manuallyEnteredTotal != null && kotlin.math.abs(manuallyEnteredTotal - afterDiscountTotal) > 0.02) {
                Text("Receipt total differs from product total minus savings. Check product prices, discounts or missing items.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // ============================================================
            // IMPORT REVIEW - EDITABLE RECEIPT DATE
            // ============================================================

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            // Keep cursor position explicitly when adding date separators.
            // Selecting the prefilled OCR/PDF date on focus makes replacement immediate.
            var dateField by remember {
                mutableStateOf(TextFieldValue(reviewReceiptDate.value.takeUnless { it.equals("Unknown", ignoreCase = true) } ?: ""))
            }
            LaunchedEffect(reviewReceiptDate.value) {
                val visibleDate = reviewReceiptDate.value.takeUnless {
                    it.equals("Unknown", ignoreCase = true)
                } ?: ""
                if (dateField.text != visibleDate) {
                    dateField = TextFieldValue(visibleDate)
                }
            }

            OutlinedTextField(
                value = dateField,
                onValueChange = { incoming ->
                    val typed = if (dateField.text.equals("Unknown", ignoreCase = true)) {
                        incoming.text.filter(Char::isDigit)
                    } else incoming.text
                    val digits = typed.filter(Char::isDigit)
                    val numericEntry = typed.all { it.isDigit() || it == '/' }

                    val formatted = if (numericEntry && digits.length <= 8) {
                        buildString {
                            digits.forEachIndexed { index, digit ->
                                if (index == 2 || index == 4) append('/')
                                append(digit)
                            }
                        }
                    } else typed

                    // Place the cursor after the entered date, not before inserted '/'.
                    dateField = incoming.copy(
                        text = formatted,
                        selection = TextRange(formatted.length)
                    )
                    reviewReceiptDate.value = formatted
                },
                label = { Text("Receipt Date") },
                placeholder = { Text("DD/MM/YYYY") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused && dateField.text.isNotEmpty()) {
                            dateField = dateField.copy(
                                selection = TextRange(0, dateField.text.length)
                            )
                        }
                    }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = addImportedProductsToInventory.value,
                    onCheckedChange = { addImportedProductsToInventory.value = it }
                )
                Text(
                    "Add to Inventory",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            }

            // ============================================================
            // IMPORT REVIEW - ADD MISSING PRODUCT BUTTON
            // ============================================================

            Button(
                onClick = {
                    newReviewProductName.value = ""
                    newReviewProductQuantity.value = "1"
                    newReviewProductPrice.value = ""
                    newReviewProductCategory.value =
                        HouseholdCategory.FOOD
                    newReviewProductLocation.value = "Pantry"

                    showAddReviewProductDialog.value = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Missing Product")
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = String.format(
                    java.util.Locale.getDefault(),
                    "Product Total: $%.2f",
                    recognisedProductTotal
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (savings > 0.0) {
                Text(String.format(Locale.US, "Savings: -$%.2f   Calculated paid: $%.2f", savings, afterDiscountTotal),
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )


            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                items(
                    count = importedItemsForReview.value.size
                ) { index ->

                    val reviewItem = importedItemsForReview.value[index]
                    val productNumber = index + 1
                    val totalProducts = importedItemsForReview.value.size

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    ) {

                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {

                            Text(
                                text = "$productNumber/$totalProducts  ${reviewItem.productName}",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleSmall,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            TextButton(onClick = {
                                editProductName = reviewItem.productName
                                editProductQuantity = (reviewItem.quantity ?: 1.0).toString()
                                editProductUnitPrice = reviewItem.unitPrice?.toString() ?: ""
                                editProductTotal = reviewItem.totalPrice?.toString() ?: ""
                                editingReviewIndex = index
                            }) { Text("Edit name / quantity / price") }

                            if (
                                reviewItem.totalPrice != null ||
                                reviewItem.unitPrice != null
                            ) {

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                val displayPrice =
                                    reviewItem.totalPrice
                                        ?: reviewItem.unitPrice

                                Text(
                                    text = String.format(
                                        java.util.Locale.getDefault(),
                                        "$%.2f",
                                        displayPrice
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

// ============================================================
// IMPORT REVIEW - UNKNOWN CATEGORY INDICATOR
// ============================================================

                            if (reviewItem.category == HouseholdCategory.UNKNOWN) {

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = "Needs classification",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text = "Storage location",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(
                                            modifier = Modifier.height(2.dp)
                                        )

                                        Text(
                                            text = reviewItem.location,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            locationEditItem.value = reviewItem
                                            locationEditSelection.value = reviewItem.location
                                        }
                                    ) {
                                        Text(
                                            text = "Change",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
// ============================================================
// IMPORT REVIEW - REMOVE ITEM FROM STAGING
// ============================================================

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {

                                TextButton(
                                    onClick = {

                                        importedItemsForReview.value =
                                            importedItemsForReview.value.filterNot { item ->
                                                item === reviewItem
                                            }
                                    }
                                ) {

                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
// ============================================================
// IMPORT REVIEW - UNKNOWN CATEGORY ACTIONS
// ============================================================

                            run {

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    Button(
                                        onClick = {

                                            importedItemsForReview.value =
                                                importedItemsForReview.value.map { item ->

                                                    if (item === reviewItem) {

                                                        item.copy(
                                                            category =
                                                                HouseholdCategory.FOOD
                                                        )

                                                    } else {

                                                        item
                                                    }
                                                }
                                        },
                                        modifier = Modifier.weight(0.9f)
                                    ) {
                                        Text(if (reviewItem.category == HouseholdCategory.FOOD) "✓ Food" else "Food")
                                    }
                                    OutlinedButton(
                                        onClick = {

                                            importedItemsForReview.value =
                                                importedItemsForReview.value.map { item ->

                                                    if (
                                                        item.productName ==
                                                        reviewItem.productName
                                                    ) {

                                                        item.copy(
                                                            category =
                                                                HouseholdCategory.OTHER
                                                        )

                                                    } else {

                                                        item
                                                    }
                                                }
                                        },
                                        modifier = Modifier.weight(1.1f)
                                    ) {
                                        Text(
                                            text = if (reviewItem.category == HouseholdCategory.OTHER) "✓ Non-food" else "Non-food",
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ============================================================
// IMPORT REVIEW - LOCATION EDITOR DIALOG
// ============================================================

            locationEditItem.value?.let { editItem ->

                AlertDialog(
                    onDismissRequest = {
                        locationEditItem.value = null
                    },

                    title = {
                        Text("Change location")
                    },

                    text = {

                        Column {

                            Text(
                                text = editItem.productName,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            listOf(
                                "Pantry",
                                "Fridge",
                                "Freezer"
                            ).forEach { location ->

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            locationEditSelection.value = location
                                        },
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    RadioButton(
                                        selected =
                                            locationEditSelection.value == location,
                                        onClick = {
                                            locationEditSelection.value = location
                                        }
                                    )

                                    Text(location)
                                }
                            }
                        }
                    },

                    confirmButton = {

                        Button(
                            onClick = {

                                val itemToUpdate =
                                    locationEditItem.value

                                val newLocation =
                                    locationEditSelection.value

                                if (
                                    itemToUpdate != null &&
                                    newLocation.isNotBlank()
                                ) {

                                    // ============================================================
// IMPORT REVIEW - STAGE LOCATION CHANGE ONLY
// ============================================================

                                    importedItemsForReview.value =
                                        importedItemsForReview.value.map { reviewItem ->

                                            if (
                                                reviewItem.productName ==
                                                itemToUpdate.productName
                                            ) {
                                                reviewItem.copy(
                                                    location = newLocation
                                                )
                                            } else {
                                                reviewItem
                                            }
                                        }

                                    locationEditItem.value = null
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },

                    dismissButton = {

                        TextButton(
                            onClick = {
                                locationEditItem.value = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (pendingOcrReceipt.value != null) {

                Button(
                    onClick = {

                        val parsedReceipt =
                            pendingOcrReceipt.value

                        val rawText =
                            pendingOcrRawText.value


                        val enteredReceiptDate =
                            reviewReceiptDate.value.trim().takeUnless {
                                it.equals("Unknown", ignoreCase = true)
                            } ?: ""

                        val parsedDate = parseReceiptDateForSorting(enteredReceiptDate)
                        val validReceiptDate = enteredReceiptDate.isEmpty() || parsedDate != null

                        if (!validReceiptDate) {
                            Toast.makeText(
                                this@MainActivity,
                                "Enter a valid date (DD/MM/YYYY).",
                                Toast.LENGTH_LONG
                            ).show()
                        }



                        val enteredTotalText = reviewReceiptTotal.value.trim().removePrefix("$")
                        val enteredTotal = enteredTotalText.toDoubleOrNull()
                        val validTotal = enteredTotalText.isBlank() || (enteredTotal != null && enteredTotal >= 0.0)
                        val enteredSavingsText = reviewDiscountAmount.value.trim().removePrefix("$")
                        val enteredSavings = enteredSavingsText.toDoubleOrNull()
                        val validSavings = enteredSavingsText.isBlank() ||
                            (enteredSavings != null && enteredSavings.isFinite() && enteredSavings >= 0.0)
                        val validRetailer = reviewRetailerName.value.trim().isNotEmpty()
                        if (!validTotal || !validRetailer || !validSavings) {
                            Toast.makeText(this@MainActivity, "Enter a retailer, valid receipt total and non-negative savings.", Toast.LENGTH_LONG).show()
                        }

                        val unclassifiedCount =
                            importedItemsForReview.value.count {
                                it.category == HouseholdCategory.UNKNOWN
                            }

                        if (unclassifiedCount > 0) {
                            Toast.makeText(
                                this@MainActivity,
                                "Classify all $unclassifiedCount remaining products before importing.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        if (
                            parsedReceipt != null &&
                            !rawText.isNullOrBlank() &&
                            validReceiptDate &&
                            unclassifiedCount == 0 && validTotal && validRetailer && validSavings
                        ) {


                            lifecycleScope.launch {

                                // ============================================================
                                // OCR IMPORT - DUPLICATE PROTECTION
                                // ============================================================

                                val fingerprint =
                                    generateReceiptFingerprint(rawText)

                                val receiptWithSameFingerprint =
                                    database
                                        .receiptDao()
                                        .getReceiptByFingerprint(
                                            fingerprint
                                        )

                                if (receiptWithSameFingerprint != null) {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "This receipt has already been imported.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return@launch
                                }

                                val receiptWithSameNumber =
                                    parsedReceipt.receiptNumber
                                        ?.let { receiptNumber ->

                                            if (receiptNumber.isBlank()) {

                                                null

                                            } else {

                                                database
                                                    .receiptDao()
                                                    .getReceiptByNumber(
                                                        receiptNumber
                                                    )
                                            }
                                        }

                                if (receiptWithSameNumber != null) {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Receipt already imported.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    return@launch
                                }

                                // ============================================================
                                // OCR IMPORT - SAVE RECEIPT
                                // ============================================================

                                val receipt =
                                    ReceiptEntity(
                                        storeName =
                                            reviewRetailerName.value.trim(),


                                        receiptDate =
                                            parsedDate?.format(receiptDisplayFormatter),


                                        totalAmount =
                                            enteredTotal ?: importedItemsForReview.value.sumOf { item ->
                                                    item.totalPrice
                                                        ?: (item.unitPrice ?: 0.0) * (item.quantity ?: 1.0)
                                                }.let { total -> (total - (enteredSavings ?: 0.0)).takeIf { it >= 0.0 } },

                                        rawText =
                                            if ((enteredSavings ?: 0.0) > 0.0)
                                                rawText + "\n\n[PANTRYPAL_REVIEW_SAVINGS_AUD=" +
                                                    String.format(Locale.US, "%.2f", enteredSavings) + "]"
                                            else rawText,

                                        receiptNumber =
                                            parsedReceipt.receiptNumber,

                                        fingerprint =
                                            fingerprint,

                                        itemCount =
                                            importedItemsForReview.value.sumOf { item ->
                                                receiptPurchaseUnits(item.quantity, item.unit)
                                            },

                                        receiptSource =
                                            parsedReceipt.receiptSource ?: "IN_STORE"
                                    )

                                val reviewedItems = importedItemsForReview.value

                                val receiptId = database.withTransaction {
                                val receiptId =
                                    database
                                        .receiptDao()
                                        .insertReceipt(receipt)


                                // ============================================================
                                // OCR IMPORT - SAVE REVIEWED RECEIPT ITEMS
                                // ============================================================


                                var receiptItemIds: List<Long> = emptyList()

                                if (reviewedItems.isNotEmpty()) {


                                    val entities =
                                        reviewedItems.map { item ->

                                            ReceiptItemEntity(
                                                receiptId =
                                                    receiptId,

                                                retailer =
                                                    reviewRetailerName.value.trim(),


                                                receiptDate =
                                                    parsedDate?.format(receiptDisplayFormatter),


                                                productName =
                                                    item.productName,

                                                quantity =
                                                    item.quantity,

                                                unit =
                                                    item.unit,

                                                unitPrice =
                                                    item.unitPrice,

                                                totalPrice =
                                                    item.totalPrice
                                            )
                                        }



                                    receiptItemIds =
                                        database
                                            .receiptItemDao()
                                            .insertAll(entities)


                                }

                                // ============================================================
                                // OCR IMPORT - UPDATE INVENTORY FROM REVIEWED FOOD ITEMS
                                // ============================================================

                                reviewedItems.forEach { item ->
                                    Log.d(
                                        "PantryPalInventory",
                                        "Import candidate: ${item.productName} | " +
                                                "category=${item.category} | " +
                                                "quantity=${item.quantity}"
                                    )
                                }


                                reviewedItems
                                    .withIndex()
                                    .filter { (_, item) ->
                                        addImportedProductsToInventory.value &&
                                                item.category == HouseholdCategory.FOOD
                                    }
                                    .forEach { (itemIndex, item) ->

                                        val receiptItemId =
                                            receiptItemIds[itemIndex]


                                        val inventoryQuantity = receiptPurchaseUnits(item.quantity, item.unit)


                                        // Use the date confirmed during receipt review.
                                        // Supports manual DD/MM/YYYY entry and
                                        // existing PDF/OCR date formats.

                                        val purchaseDate = parsedDate

                                        val productId = addOrUpdateInventoryItem(
                                            productName =
                                                item.productName,

                                            quantity =
                                                inventoryQuantity,

                                            barcode = "",

                                            purchaseDate =
                                                purchaseDate,

                                            explicitLocation =
                                                item.location
                                        )
                                        database.receiptInventoryContributionDao().insert(
                                            ReceiptInventoryContributionEntity(
                                                receiptId = receiptId,
                                                receiptItemId = receiptItemId,
                                                productId = productId,
                                                quantityAdded = inventoryQuantity
                                            )
                                        )
                                    }

                                    receiptId
                                }
                                lastImportedReceiptId.longValue = receiptId

                                // PP-156B: learn only confirmed manual name corrections.
                                // Do not modify historical receipt items or the contribution ledger.
                                reviewedItems.filter { it.manuallyCorrected }.forEach { item ->
                                    retailerAliasStore.learn(
                                        reviewRetailerName.value.trim(),
                                        item.originalDescription,
                                        item.productName
                                    )
                                }

                                // ============================================================
                                // OCR IMPORT - COMMIT REVIEWED PREFERENCES
                                // ============================================================

                                reviewedItems.forEach { item ->

                                    if (
                                        item.category ==
                                        HouseholdCategory.FOOD ||
                                        item.category ==
                                        HouseholdCategory.OTHER
                                    ) {

                                        saveCategoryPreference(
                                            productName =
                                                item.productName,

                                            category =
                                                item.category
                                        )
                                    }

                                    saveLocationCorrection(
                                        productName =
                                            item.productName,

                                        location =
                                            item.location,

                                        purchaseDate =
                                            null
                                    )
                                }

                                // ============================================================
                                // OCR IMPORT - COMPLETE
                                // ============================================================

                                pendingOcrReceipt.value = null
                                pendingOcrRawText.value = null
                                reviewDiscountAmount.value = ""
                                importedItemsForReview.value =
                                    emptyList()

                                refreshProducts()

                                expirySummary.value =
                                    getExpirySummary()

                                Toast.makeText(
                                    this@MainActivity,
                                    "Receipt imported.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                currentScreen.value =
                                    "HOME"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Import")
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedButton(
                    onClick = {
                        pendingOcrReceipt.value = null
                        pendingOcrRawText.value = null
                        importedItemsForReview.value = emptyList()

                        currentScreen.value = "HOME"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }

            } else {

                Button(
                    onClick = {
                        currentScreen.value = "HOME"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }

    private fun handleSharedReceiptIntent(
        intent: Intent?
    ) {

        if (intent == null) {
            return
        }

        val mimeType =
            intent.type ?: return

        val sharedUri =
            when (intent.action) {

                Intent.ACTION_SEND -> {

                    if (android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.TIRAMISU
                    ) {
                        intent.getParcelableExtra(
                            Intent.EXTRA_STREAM,
                            android.net.Uri::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<android.net.Uri>(
                            Intent.EXTRA_STREAM
                        )
                    }
                }

                Intent.ACTION_VIEW -> {
                    intent.data
                }

                else -> {
                    return
                }
            }

        if (sharedUri == null) {

            android.util.Log.e(
                "PantryPalSHARE",
                "${intent.action} received but no URI was supplied"
            )

            return
        }

        android.util.Log.e(
            "PantryPalSHARE",
            "Received receipt | " +
                    "action=${intent.action} | " +
                    "type=$mimeType | " +
                    "uri=$sharedUri"
        )

        when {

            mimeType.startsWith("image/") -> {

                android.util.Log.e(
                    "PantryPalSHARE",
                    "Routing shared image to OCR"
                )

                processReceiptOcr(sharedUri)
            }

            mimeType == "application/pdf" -> {

                android.util.Log.e(
                    "PantryPalSHARE",
                    "Routing shared PDF to PDF import"
                )

                importReceiptPdf(sharedUri)
            }

            else -> {

                android.util.Log.e(
                    "PantryPalSHARE",
                    "Unsupported shared type: $mimeType"
                )

                Toast.makeText(
                    this,
                    "PantryPal cannot import this file type.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun processReceiptOcr(
        uri: android.net.Uri
    ) {

        try {

            val inputImage =
                com.google.mlkit.vision.common.InputImage
                    .fromFilePath(
                        this,
                        uri
                    )

            val recognizer =
                com.google.mlkit.vision.text.TextRecognition
                    .getClient(
                        com.google.mlkit.vision.text.latin
                            .TextRecognizerOptions
                            .DEFAULT_OPTIONS
                    )

            recognizer
                .process(inputImage)
                .addOnSuccessListener { visionText ->

                    val recognisedText =
                        visionText.text

                    visionText.textBlocks.forEach { block ->

                        block.lines.forEach { line ->

                            val box =
                                line.boundingBox

                            android.util.Log.e(
                                "PantryPalOCRPOS",
                                "x=${box?.left}, " +
                                        "y=${box?.top}, " +
                                        "right=${box?.right}, " +
                                        "bottom=${box?.bottom} | " +
                                        line.text
                            )
                        }
                    }

                    val normalisedText =
                        com.example.myapplication.data
                            .OcrReceiptInterpreter
                            .normalise(visionText)

                    pendingOcrReceipt.value = null

                    pendingOcrRawText.value = null

                    val parsedReceipt =
                        com.example.myapplication.data
                            .OcrReceiptInterpreter
                            .parse(visionText)

                    android.util.Log.e(
                        "PantryPalOCR",
                        "OCR RESULT:\n$recognisedText"
                    )

                    android.util.Log.e(
                        "PantryPalOCRNORMAL",
                        "NORMALISED RECEIPT:\n$normalisedText"
                    )

                    if (
                        parsedReceipt == null ||
                        parsedReceipt.structuredItems.isEmpty()
                    ) {

                        Toast.makeText(
                            this,
                            "No receipt items could be identified.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnSuccessListener
                    }

                    reviewReceiptDate.value =
                        parseReceiptDateForSorting(parsedReceipt.receiptDate)
                            ?.format(DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH))
                            ?: (parsedReceipt.receiptDate ?: "")
                    reviewRetailerName.value = parsedReceipt.storeName
                    reviewReceiptTotal.value = parsedReceipt.totalAmount?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                reviewDiscountAmount.value = parsedReceipt.adjustments.filter { it.amount < 0.0 }.sumOf { -it.amount }.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.2f", it) } ?: ""


                    pendingOcrReceipt.value = parsedReceipt
                    pendingOcrRawText.value = normalisedText
                    addImportedProductsToInventory.value = true

                    lifecycleScope.launch {

                        importedItemsForReview.value =
                            parsedReceipt
                                .structuredItems
                                .map { receiptItem ->

                                    val category =
                                        resolveHouseholdCategory(
                                            resolvedReviewName(parsedReceipt.storeName, receiptItem.name)
                                        )

                                    ImportReviewItem(
                                        productName =
                                            resolvedReviewName(parsedReceipt.storeName, receiptItem.name),
                                        originalDescription = receiptItem.name,

                                        location =
                                            resolveStorageLocation(
                                                resolvedReviewName(parsedReceipt.storeName, receiptItem.name)
                                            ),

                                        category =
                                            category,

                                        quantity =
                                            receiptItem.quantity,

                                        unit =
                                            receiptItem.unit,

                                        unitPrice =
                                            receiptItem.unitPrice,

                                        totalPrice =
                                            receiptItem.totalPrice,

                                        receiptDate =
                                            parsedReceipt.receiptDate
                                    )
                                }

                        android.util.Log.e(
                            "PantryPalOCRREVIEW",
                            "Prepared " +
                                    "${importedItemsForReview.value.size} " +
                                    "OCR items for review"
                        )

                        currentScreen.value =
                            "IMPORT_REVIEW"
                    }
                }
                .addOnFailureListener { exception ->

                    android.util.Log.e(
                        "PantryPalOCR",
                        "Receipt OCR failed",
                        exception
                    )

                    Toast.makeText(
                        this,
                        "Receipt OCR failed",
                        Toast.LENGTH_LONG
                    ).show()
                }

        } catch (exception: Exception) {

            android.util.Log.e(
                "PantryPalOCR",
                "Unable to process receipt image",
                exception
            )

            Toast.makeText(
                this,
                "Unable to process receipt image",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
// GOOGLE PLAY BILLING - LIFECYCLE CLEANUP
// ============================================================

    override fun onDestroy() {
        if (::billingManager.isInitialized) {
            billingManager.disconnect()
        }

        super.onDestroy()
    }


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // ============================================================
// GOOGLE PLAY BILLING - CONNECTION TEST
// ============================================================

            // ============================================================
// GOOGLE PLAY BILLING - CONNECTION AND PRODUCT CHECKS
// ============================================================

            billingManager = BillingManager(
                context = this,
                onProPurchased = {
                    runOnUiThread {
                        isProUser.value = true
                    }
                }
            )

            billingManager.connect(
                onConnected = {

                    android.util.Log.d(
                        "PantryPalBilling",
                        "Google Play Billing connected successfully"
                    )

                    // QUERY GOOGLE PLAY FOR PANTRYPAL PRO
                    billingManager.queryProProduct(
                        onResult = { result ->
                            android.util.Log.d(
                                "PantryPalBilling",
                                result
                            )
                        },
                        onError = { error ->
                            android.util.Log.e(
                                "PantryPalBilling",
                                error
                            )
                        }
                    )

                    // CHECK WHETHER PRO IS ALREADY OWNED
                    billingManager.checkProEntitlement(
                        onResult = { ownsPro ->
                            runOnUiThread {
                                isProUser.value = ownsPro
                            }
                            android.util.Log.d(
                                "PantryPalBilling",
                                "Verified existing Pro entitlement: $ownsPro"
                            )
                        },
                        onError = { error ->
                            android.util.Log.e(
                                "PantryPalBilling",
                                error
                            )
                        }
                    )
                },

                // BILLING CONNECTION ERROR HANDLER
                onError = { error ->
                    android.util.Log.e(
                        "PantryPalBilling",
                        error
                    )
                }
            )

            android.util.Log.e(
                "PantryPalShareDebug",
                "ACTION = ${intent?.action}"
            )

            android.util.Log.e(
                "PantryPalShareDebug",
                "TYPE = ${intent?.type}"
            )

            android.util.Log.e(
                "PantryPalShareDebug",
                "DATA = ${intent?.data}"
            )

            val sharedUri =
                if (android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.TIRAMISU
                ) {
                    intent?.getParcelableExtra(
                        android.content.Intent.EXTRA_STREAM,
                        android.net.Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra<android.net.Uri>(
                        android.content.Intent.EXTRA_STREAM
                    )
                }

            android.util.Log.e(
                "PantryPalShareDebug",
                "EXTRA_STREAM = $sharedUri"
            )

            enableEdgeToEdge()



        prefs = getSharedPreferences("pantrypal_prefs", MODE_PRIVATE)

        selectedCategory.value =
            prefs.getString("category", "Fresh vegetables") ?: "Fresh vegetables"
        selectedLocation.value = prefs.getString("location", "Pantry") ?: "Pantry"
        quantityInput.value = prefs.getString("quantity", "1") ?: "1"

        applySuggestedExpiryDate()

        val workRequest =
            androidx.work.PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(
                1,
                java.util.concurrent.TimeUnit.DAYS
            ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "expiry_notifications",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )


        database = AppDatabase.getDatabase(this)

        PDFBoxResourceLoader.init(applicationContext)

        refreshProducts()

        handleSharedReceiptIntent(intent)

        lifecycleScope.launch {

            // PP-150: remove legacy generated suggestions; retain explicit shopping intent.
            database.shoppingDao().clearAutoItems()
            val shoppingItems =
                database.shoppingDao().getAllItems()

            shoppingListEntities.value =
                shoppingItems

            shoppingListItems.value =
                shoppingItems
                    .map { it.description }
                    .toMutableList()
        }

        lifecycleScope.launch {
            expirySummary.value = getExpirySummary()

            setContent {

                MyApplicationTheme {

                    androidx.activity.compose.BackHandler(
                        enabled = currentScreen.value != "HOME"
                    ) {
                        navigateBack()
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {

                        when (currentScreen.value) {

                            "HOME" ->
                                HomeScreen()

                            "INVENTORY" ->
                                InventoryScreen()

                            "DETAIL" ->
                                ProductDetailScreen()

                            "SHOPPING" ->
                                ShoppingListScreen()

                            "PRICE_HISTORY" ->
                                PriceHistoryScreen()

                            "RECEIPTS" ->
                                ReceiptScreen()

                            "ARCHIVED_RECEIPTS" ->
                                ArchivedReceiptScreen()

                            "ANALYTICS" -> {
                                if (isProUser.value) {
                                    AnalyticsScreen()
                                } else {
                                    ProUpgradeScreen(
                                        onUpgradeClick = {
                                            billingManager.launchProPurchase(
                                                activity = this@MainActivity,
                                                onError = { error ->
                                                    android.util.Log.e(
                                                        "PantryPalBilling",
                                                        error
                                                    )
                                                }
                                            )
                                        }
                                    )
                                }
                            }


                            "IMPORT_REVIEW" ->
                                ImportReviewScreen()

                            "COLES_STAGING" ->
                                ColesStagingScreen()

                            else ->
                                HomeScreen()
                        }
                    }

                    if (showImportReview.value) {

                        AlertDialog(
                            onDismissRequest = {
                                showImportReview.value = false
                            },

                            title = {
                                Text("Receipt imported")
                            },

                            text = {

                                Column {

                                    Text(
                                        "PantryPal has updated your inventory " +
                                                "and automatically assigned storage locations."
                                    )

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    Text(
                                        "You can review the imported items and " +
                                                "correct any locations that need changing."
                                    )
                                }
                            },

                            confirmButton = {

                                Button(
                                    onClick = {

                                        lifecycleScope.launch {


                                                // ============================================================
                                                // IMPORT REVIEW - PREPARE DISPLAY ITEMS
                                                // ============================================================

                                                val receiptItems =
                                                    database
                                                        .receiptItemDao()
                                                        .getItemsForReceipt(
                                                            lastImportedReceiptId.longValue
                                                        )

                                                importedItemsForReview.value =
                                                    receiptItems
                                                        .filter { receiptItem ->

                                                            val category =
                                                                resolveHouseholdCategory(
                                                                    receiptItem.productName
                                                                )

                                                            category == HouseholdCategory.FOOD ||
                                                                    category == HouseholdCategory.UNKNOWN
                                                        }
                                                        .map { receiptItem ->

                                                            val category =
                                                                resolveHouseholdCategory(
                                                                    receiptItem.productName
                                                                )

                                                            ImportReviewItem(
                                                                productName =
                                                                    receiptItem.productName,

                                                                location =
                                                                    resolveStorageLocation(
                                                                        receiptItem.productName
                                                                    ),

                                                                category =
                                                                    category,

                                                                quantity =
                                                                    receiptItem.quantity,

                                                                unit =
                                                                    receiptItem.unit,

                                                                unitPrice =
                                                                    receiptItem.unitPrice,

                                                                totalPrice =
                                                                    receiptItem.totalPrice,

                                                                receiptDate =
                                                                    receiptItem.receiptDate
                                                            )
                                                        }

                                                showImportReview.value = false
                                                currentScreen.value = "IMPORT_REVIEW"
                                            }
                                    }
                                ) {
                                    Text("Review locations")
                                }

                            },

                            dismissButton = {

                                TextButton(
                                    onClick = {
                                        showImportReview.value = false
                                    }
                                ) {
                                    Text("Done")
                                }
                            }

                        )
                    }
                }
            }
        }
   }

    @Composable
    private fun PriceTrendChart(
        observations: List<PriceTrendObservation>
    ) {

        if (observations.isEmpty()) {
            return
        }

        val sortedObservations =
            observations.sortedBy {
                it.receiptDate
            }

        val minPrice =
            sortedObservations.minOf {
                it.price
            }

        val maxPrice =
            sortedObservations.maxOf {
                it.price
            }

        val priceRange =
            (maxPrice - minPrice)
                .takeIf { it > 0.0 }
                ?: 1.0

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Price over time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "$${"%.2f".format(minPrice)} – " +
                                "$${"%.2f".format(maxPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                val lineColor =
                    MaterialTheme.colorScheme.primary

                val axisColor =
                    MaterialTheme.colorScheme.onSurfaceVariant

                val gridColor =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.25f
                    )

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // Y-axis price labels
                    Column(
                        modifier = Modifier.height(220.dp),
                        verticalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text =
                                "$${"%.2f".format(maxPrice)}",
                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text =
                                "$${"%.2f".format(
                                    (maxPrice + minPrice) / 2.0
                                )}",
                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text =
                                "$${"%.2f".format(minPrice)}",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(220.dp)
                    ) {

                        val leftPadding =
                            8.dp.toPx()

                        val rightPadding =
                            8.dp.toPx()

                        val topPadding =
                            10.dp.toPx()

                        val bottomPadding =
                            32.dp.toPx()

                        val graphWidth =
                            size.width -
                                    leftPadding -
                                    rightPadding

                        val graphHeight =
                            size.height -
                                    topPadding -
                                    bottomPadding

                        // Horizontal grid lines
                        val gridYValues =
                            listOf(
                                topPadding,
                                topPadding +
                                        graphHeight / 2f,
                                topPadding +
                                        graphHeight
                            )

                        gridYValues.forEach { y ->

                            drawLine(
                                color = gridColor,
                                start =
                                    Offset(
                                        leftPadding,
                                        y
                                    ),
                                end =
                                    Offset(
                                        leftPadding +
                                                graphWidth,
                                        y
                                    ),
                                strokeWidth =
                                    1.dp.toPx()
                            )
                        }

                        // Y axis
                        drawLine(
                            color = axisColor,
                            start =
                                Offset(
                                    leftPadding,
                                    topPadding
                                ),
                            end =
                                Offset(
                                    leftPadding,
                                    topPadding +
                                            graphHeight
                                ),
                            strokeWidth =
                                2.dp.toPx()
                        )

                        // X axis
                        drawLine(
                            color = axisColor,
                            start =
                                Offset(
                                    leftPadding,
                                    topPadding +
                                            graphHeight
                                ),
                            end =
                                Offset(
                                    leftPadding +
                                            graphWidth,
                                    topPadding +
                                            graphHeight
                                ),
                            strokeWidth =
                                2.dp.toPx()
                        )

                        val firstDate =
                            sortedObservations
                                .first()
                                .receiptDate

                        val lastDate =
                            sortedObservations
                                .last()
                                .receiptDate

                        val totalDays =
                            java.time.temporal.ChronoUnit.DAYS
                                .between(
                                    firstDate,
                                    lastDate
                                )
                                .coerceAtLeast(1)

                        val points =
                            sortedObservations.map { observation ->

                                val daysFromStart =
                                    java.time.temporal.ChronoUnit.DAYS
                                        .between(
                                            firstDate,
                                            observation.receiptDate
                                        )

                                val datePosition =
                                    daysFromStart.toFloat() /
                                            totalDays.toFloat()

                                val x =
                                    if (
                                        sortedObservations.size == 1
                                    ) {
                                        leftPadding +
                                                graphWidth / 2f
                                    } else {
                                        leftPadding +
                                                graphWidth * datePosition
                                    }

                                val normalisedPrice =
                                    (
                                            observation.price -
                                                    minPrice
                                            ) /
                                            priceRange

                                val y =
                                    topPadding +
                                            graphHeight -
                                            (
                                                    graphHeight *
                                                            normalisedPrice
                                                    ).toFloat()

                                Offset(
                                    x = x,
                                    y = y
                                )
                            }

                        if (points.size > 1) {

                            val path =
                                Path().apply {

                                    moveTo(
                                        points.first().x,
                                        points.first().y
                                    )

                                    points
                                        .drop(1)
                                        .forEach { point ->

                                            lineTo(
                                                point.x,
                                                point.y
                                            )
                                        }
                                }

                            drawPath(
                                path = path,
                                color = lineColor,
                                style =
                                    androidx.compose.ui.graphics
                                        .drawscope.Stroke(
                                            width =
                                                4.dp.toPx()
                                        )
                            )
                        }

                        points.forEach { point ->

                            drawCircle(
                                color = lineColor,
                                radius = 7.dp.toPx(),
                                center = point
                            )
                        }

// X-axis date labels.
// Draw them in the same coordinate system as the plotted points.
                        val dateFormatter =
                            DateTimeFormatter.ofPattern(
                                "d MMM",
                                Locale.ENGLISH
                            )

                        val firstDateLabel =
                            sortedObservations
                                .first()
                                .receiptDate
                                .format(dateFormatter)

                        val lastDateLabel =
                            sortedObservations
                                .last()
                                .receiptDate
                                .format(dateFormatter)

                        val labelPaint =
                            android.graphics.Paint().apply {
                                color =
                                    android.graphics.Color.rgb(
                                        45,
                                        91,
                                        45
                                    )

                                textSize =
                                    14.dp.toPx()

                                isAntiAlias = true
                            }

                        val labelY =
                            size.height -
                                    2.dp.toPx()

// First date begins at the exact X coordinate
// of the first plotted observation.
                        drawContext.canvas.nativeCanvas.drawText(
                            firstDateLabel,
                            points.first().x,
                            labelY,
                            labelPaint
                        )

// Last date ends at the exact X coordinate
// of the final plotted observation.
                        labelPaint.textAlign =
                            android.graphics.Paint.Align.RIGHT

                        drawContext.canvas.nativeCanvas.drawText(
                            lastDateLabel,
                            points.last().x,
                            labelY,
                            labelPaint
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )


            }
        }
    }

    @Composable
    private fun MonthlySpendChart(
        monthlySpend: List<MonthlySpend>
    ) {

        if (monthlySpend.isEmpty()) {
            return
        }

        val sortedMonths =
            monthlySpend.sortedWith(
                compareBy<MonthlySpend> {
                    it.year
                }.thenBy {
                    it.month
                }
            )

        val minSpend =
            sortedMonths.minOf {
                it.amount
            }

        val maxSpend =
            sortedMonths.maxOf {
                it.amount
            }

        val spendRange =
            (maxSpend - minSpend)
                .takeIf { it > 0.0 }
                ?: 1.0

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Monthly spending trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "$${"%.2f".format(minSpend)} – " +
                                "$${"%.2f".format(maxSpend)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                val lineColor =
                    MaterialTheme.colorScheme.primary

                val axisColor =
                    MaterialTheme.colorScheme.onSurfaceVariant

                val gridColor =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.25f
                    )

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.height(220.dp),
                        verticalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text =
                                "$${"%.2f".format(maxSpend)}",
                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text =
                                "$${"%.2f".format(
                                    (maxSpend + minSpend) / 2.0
                                )}",
                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text =
                                "$${"%.2f".format(minSpend)}",
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(220.dp)
                    ) {

                        val leftPadding =
                            8.dp.toPx()

                        val rightPadding =
                            8.dp.toPx()

                        val topPadding =
                            10.dp.toPx()

                        val bottomPadding =
                            32.dp.toPx()

                        val graphWidth =
                            size.width -
                                    leftPadding -
                                    rightPadding

                        val graphHeight =
                            size.height -
                                    topPadding -
                                    bottomPadding

                        val gridYValues =
                            listOf(
                                topPadding,
                                topPadding +
                                        graphHeight / 2f,
                                topPadding +
                                        graphHeight
                            )

                        gridYValues.forEach { y ->

                            drawLine(
                                color = gridColor,
                                start =
                                    Offset(
                                        leftPadding,
                                        y
                                    ),
                                end =
                                    Offset(
                                        leftPadding +
                                                graphWidth,
                                        y
                                    ),
                                strokeWidth =
                                    1.dp.toPx()
                            )
                        }

                        drawLine(
                            color = axisColor,
                            start =
                                Offset(
                                    leftPadding,
                                    topPadding
                                ),
                            end =
                                Offset(
                                    leftPadding,
                                    topPadding +
                                            graphHeight
                                ),
                            strokeWidth =
                                2.dp.toPx()
                        )

                        drawLine(
                            color = axisColor,
                            start =
                                Offset(
                                    leftPadding,
                                    topPadding +
                                            graphHeight
                                ),
                            end =
                                Offset(
                                    leftPadding +
                                            graphWidth,
                                    topPadding +
                                            graphHeight
                                ),
                            strokeWidth =
                                2.dp.toPx()
                        )

                        val firstMonthIndex =
                            sortedMonths.first().year * 12 +
                                    sortedMonths.first().month

                        val lastMonthIndex =
                            sortedMonths.last().year * 12 +
                                    sortedMonths.last().month

                        val totalMonths =
                            (lastMonthIndex - firstMonthIndex)
                                .coerceAtLeast(1)

                        val points =
                            sortedMonths.map { month ->

                                val monthIndex =
                                    month.year * 12 +
                                            month.month

                                val monthsFromStart =
                                    monthIndex -
                                            firstMonthIndex

                                val monthPosition =
                                    monthsFromStart.toFloat() /
                                            totalMonths.toFloat()

                                val x =
                                    if (sortedMonths.size == 1) {
                                        leftPadding +
                                                graphWidth / 2f
                                    } else {
                                        leftPadding +
                                                graphWidth *
                                                monthPosition
                                    }

                                val normalisedSpend =
                                    (month.amount -
                                            minSpend) /
                                            spendRange

                                val y =
                                    topPadding +
                                            graphHeight -
                                            (
                                                    graphHeight *
                                                            normalisedSpend
                                                    ).toFloat()

                                Offset(
                                    x = x,
                                    y = y
                                )
                            }

                        if (points.size > 1) {

                            val path =
                                Path().apply {

                                    moveTo(
                                        points.first().x,
                                        points.first().y
                                    )

                                    points
                                        .drop(1)
                                        .forEach { point ->

                                            lineTo(
                                                point.x,
                                                point.y
                                            )
                                        }
                                }

                            drawPath(
                                path = path,
                                color = lineColor,
                                style =
                                    androidx.compose.ui.graphics
                                        .drawscope.Stroke(
                                            width =
                                                4.dp.toPx()
                                        )
                            )
                        }

                        points.forEach { point ->

                            drawCircle(
                                color = lineColor,
                                radius = 7.dp.toPx(),
                                center = point
                            )
                        }

                        val monthFormatter =
                            DateTimeFormatter.ofPattern(
                                "MMM yy",
                                Locale.ENGLISH
                            )

                        val firstMonthLabel =
                            LocalDate.of(
                                sortedMonths.first().year,
                                sortedMonths.first().month,
                                1
                            ).format(monthFormatter)

                        val lastMonthLabel =
                            LocalDate.of(
                                sortedMonths.last().year,
                                sortedMonths.last().month,
                                1
                            ).format(monthFormatter)

                        val labelPaint =
                            android.graphics.Paint().apply {

                                color =
                                    android.graphics.Color.rgb(
                                        45,
                                        91,
                                        45
                                    )

                                textSize =
                                    14.dp.toPx()

                                isAntiAlias = true
                            }

                        val labelY =
                            size.height -
                                    2.dp.toPx()

                        drawContext.canvas.nativeCanvas.drawText(
                            firstMonthLabel,
                            points.first().x,
                            labelY,
                            labelPaint
                        )

                        labelPaint.textAlign =
                            android.graphics.Paint.Align.RIGHT

                        drawContext.canvas.nativeCanvas.drawText(
                            lastMonthLabel,
                            points.last().x,
                            labelY,
                            labelPaint
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }
    }

    // ============================================================
// PRO UPGRADE SCREEN
// ============================================================

    @Composable
    fun ProUpgradeScreen(
        onUpgradeClick: () -> Unit
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "PantryPal Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Unlock advanced household spending insights.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Pro includes:\n\n" +
                        "• Monthly spending analytics\n" +
                        "• Retailer comparisons\n" +
                        "• Product price trends\n" +
                        "• Advanced spending reports",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onUpgradeClick,
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upgrade to Pro")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Purchase securely through Google Play to unlock Pro analytics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun AnalyticsScreen() {

        var selectedPeriod by remember {
            mutableStateOf(
                AnalyticsPeriod.ALL_TIME
            )
        }

        var periodMenuExpanded by remember {
            mutableStateOf(false)
        }

        var analyticsSummary by remember {
            mutableStateOf<AnalyticsSummary?>(null)
        }

        var priceTrendSearch by remember {
            mutableStateOf("")
        }

        var priceTrendResults by remember {
            mutableStateOf<List<ReceiptItemEntity>>(
                emptyList()
            )
        }

        var priceTrendHasSearched by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(selectedPeriod) {

            val receipts =
                database
                    .receiptDao()
                    .getAllReceipts()

            val receiptItems =
                database
                    .receiptItemDao()
                    .getAllItems()

            analyticsSummary =
                AnalyticsCalculator.calculate(
                    receipts = receipts,
                    receiptItems = receiptItems,
                    period = selectedPeriod
                )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            Text(
                text = "PantryPal",
                style =
                    MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Analytics",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Understand where your household spending goes.",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            OutlinedButton(
                onClick = {
                    currentScreen.value = "HOME"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("← Back")
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = {
                        periodMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    val periodLabel =
                        when (selectedPeriod) {
                            AnalyticsPeriod.LAST_30_DAYS ->
                                "Last 30 days"

                            AnalyticsPeriod.LAST_3_MONTHS ->
                                "Last 3 months"

                            AnalyticsPeriod.LAST_6_MONTHS ->
                                "Last 6 months"

                            AnalyticsPeriod.LAST_12_MONTHS ->
                                "Last 12 months"

                            AnalyticsPeriod.ALL_TIME ->
                                "All time"
                        }

                    Text(
                        text = "$periodLabel  ▼",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                DropdownMenu(
                    expanded = periodMenuExpanded,
                    onDismissRequest = {
                        periodMenuExpanded = false
                    }
                ) {

                    DropdownMenuItem(
                        text = {
                            Text("Last 30 days")
                        },
                        onClick = {
                            selectedPeriod =
                                AnalyticsPeriod.LAST_30_DAYS

                            periodMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Last 3 months")
                        },
                        onClick = {
                            selectedPeriod =
                                AnalyticsPeriod.LAST_3_MONTHS

                            periodMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Last 6 months")
                        },
                        onClick = {
                            selectedPeriod =
                                AnalyticsPeriod.LAST_6_MONTHS

                            periodMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Last 12 months")
                        },
                        onClick = {
                            selectedPeriod =
                                AnalyticsPeriod.LAST_12_MONTHS

                            periodMenuExpanded = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("All time")
                        },
                        onClick = {
                            selectedPeriod =
                                AnalyticsPeriod.ALL_TIME

                            periodMenuExpanded = false
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            analyticsSummary?.let { summary ->

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(20.dp),
                    color =
                        MaterialTheme.colorScheme.primary
                ) {

                    Column(
                        modifier =
                            Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Total Spend",
                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.SemiBold,
                            color =
                                MaterialTheme.colorScheme
                                    .onPrimary
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "$${"%.2f".format(summary.totalSpend)}",
                            style =
                                MaterialTheme.typography
                                    .headlineLarge,
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                MaterialTheme.colorScheme
                                    .onPrimary
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "${summary.receiptCount} receipts",
                            style =
                                MaterialTheme.typography
                                    .bodyMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onPrimary
                        )

                        if (
                            summary.earliestReceiptDate != null &&
                            summary.latestReceiptDate != null
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            Text(
                                text = "Receipt period",
                                style =
                                    MaterialTheme.typography
                                        .bodyMedium,
                                fontWeight =
                                    FontWeight.SemiBold,
                                color =
                                    MaterialTheme.colorScheme
                                        .onPrimary
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(2.dp)
                            )

                            val displayDateFormatter =
                                DateTimeFormatter.ofPattern(
                                    "d MMM yyyy",
                                    Locale.ENGLISH
                                )

                            Text(
                                text =
                                    "${summary.earliestReceiptDate.format(displayDateFormatter)}" +
                                            " – " +
                                            "${summary.latestReceiptDate.format(displayDateFormatter)}",
                                style =
                                    MaterialTheme.typography
                                        .bodyMedium,
                                color =
                                    MaterialTheme.colorScheme
                                        .onPrimary
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "Spend by retailer",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                summary.retailerSpend.forEach {
                        retailer ->

                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(16.dp),
                        color =
                            MaterialTheme.colorScheme
                                .surfaceVariant
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    retailer.retailer,
                                style =
                                    MaterialTheme.typography
                                        .bodyLarge,
                                fontWeight =
                                    FontWeight.SemiBold,
                                modifier =
                                    Modifier.weight(1f)
                            )

                            Text(
                                text =
                                    "$${"%.2f".format(retailer.amount)}",
                                style =
                                    MaterialTheme.typography
                                        .bodyLarge,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme
                                        .primary
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Top products by spend",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                summary.productSpend
                    .take(10)
                    .forEach { product ->

                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(16.dp),
                            color =
                                MaterialTheme.colorScheme
                                    .surfaceVariant
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text =
                                        product.productName,
                                    style =
                                        MaterialTheme.typography
                                            .bodyMedium,
                                    modifier =
                                        Modifier.weight(1f)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(12.dp)
                                )

                                Text(
                                    text =
                                        "$${"%.2f".format(product.amount)}",
                                    style =
                                        MaterialTheme.typography
                                            .bodyMedium,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color =
                                        MaterialTheme.colorScheme
                                            .primary
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )
                    }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
                Text(
                    text = "Monthly spend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Household spending by purchase month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                MonthlySpendChart(
                    monthlySpend = summary.monthlySpend
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                summary.monthlySpend
                    .asReversed()
                    .forEach { month ->

                        val monthDate =
                            LocalDate.of(
                                month.year,
                                month.month,
                                1
                            )

                        val monthLabel =
                            monthDate.format(
                                DateTimeFormatter.ofPattern(
                                    "MMMM yyyy",
                                    Locale.ENGLISH
                                )
                            )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color =
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = monthLabel,
                                        style =
                                            MaterialTheme.typography.bodyLarge,
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )

                                    Spacer(
                                        modifier = Modifier.height(2.dp)
                                    )

                                    Text(
                                        text =
                                            "${month.receiptCount} " +
                                                    if (month.receiptCount == 1) {
                                                        "receipt"
                                                    } else {
                                                        "receipts"
                                                    },
                                        style =
                                            MaterialTheme.typography.bodyMedium,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    if (
                                        month.previousMonthChange != null &&
                                        month.previousMonthPercentChange != null
                                    ) {

                                        val changeAmountText =
                                            if (month.previousMonthChange >= 0) {
                                                "+$" +
                                                        "%.2f".format(
                                                            month.previousMonthChange
                                                        )
                                            } else {
                                                "-$" +
                                                        "%.2f".format(
                                                            -month.previousMonthChange
                                                        )
                                            }

                                        val percentPrefix =
                                            if (month.previousMonthPercentChange >= 0) {
                                                "+"
                                            } else {
                                                ""
                                            }

                                        Text(
                                            text =
                                                "vs previous month:\n" +
                                                        changeAmountText +
                                                        " (" +
                                                        percentPrefix +
                                                        "%.1f".format(
                                                            month.previousMonthPercentChange
                                                        ) +
                                                        "%)",
                                            style =
                                                MaterialTheme.typography.bodySmall,
                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant
                                        )

                                    } else {

                                        Text(
                                            text = "No previous-month data",
                                            style =
                                                MaterialTheme.typography.bodySmall,
                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant
                                        )
                                   }

                                }

                                Text(
                                    text =
                                        "$${"%.2f".format(month.amount)}",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text = "Price trends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Track how a product's price has changed over time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = priceTrendSearch,
                    onValueChange = {
                        priceTrendSearch = it
                    },
                    label = {
                        Text("Product name")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Button(
                    onClick = {

                        lifecycleScope.launch {

                            val searchTerm =
                                priceTrendSearch.trim()

                            priceTrendResults =
                                database
                                    .receiptItemDao()
                                    .getPriceHistory(
                                        searchTerm
                                    )

                            priceTrendHasSearched = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Search Price Trends")
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                if (priceTrendResults.isNotEmpty()) {
                    val consolidatedPriceTrends =
                        priceTrendResults
                            .mapNotNull { item ->

                                val price =
                                    item.totalPrice
                                        ?: item.unitPrice

                                val receiptDate =
                                    parseReceiptDateForSorting(
                                        item.receiptDate
                                    )

                                if (
                                    price == null ||
                                    receiptDate == null
                                ) {
                                    null
                                } else {
                                    Triple(
                                        receiptDate,
                                        item.retailer,
                                        price
                                    )
                                }
                            }
                            .groupingBy { observation ->
                                observation
                            }
                            .eachCount()
                            .map { (observation, purchaseCount) ->

                                PriceTrendObservation(
                                    receiptDate =
                                        observation.first,
                                    retailer =
                                        observation.second,
                                    price =
                                        observation.third,
                                    purchaseCount =
                                        purchaseCount
                                )
                            }
                            .sortedBy { observation ->
                                observation.receiptDate
                            }

                    Text(
                        text =
                            "${consolidatedPriceTrends.size} price observations",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    PriceTrendChart(
                        observations = consolidatedPriceTrends
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    consolidatedPriceTrends.forEach { observation ->

                        val receiptDate =
                            observation.receiptDate

                        val retailer =
                            observation.retailer

                        val price =
                            observation.price

                        val purchaseCount =
                            observation.purchaseCount

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color =
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text =
                                        receiptDate.format(
                                            DateTimeFormatter.ofPattern(
                                                "d MMM yyyy",
                                                Locale.ENGLISH
                                            )
                                        ),
                                    style =
                                        MaterialTheme.typography.bodyLarge,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Spacer(
                                    modifier = Modifier.height(2.dp)
                                )

                                Text(
                                    text = retailer,
                                    style =
                                        MaterialTheme.typography.bodyMedium,
                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text =
                                        "$${"%.2f".format(price)}",
                                    style =
                                        MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        MaterialTheme.colorScheme.primary
                                )

                                if (purchaseCount > 1) {

                                    Spacer(
                                        modifier = Modifier.height(2.dp)
                                    )

                                    Text(
                                        text =
                                            "$purchaseCount purchased",
                                        style =
                                            MaterialTheme.typography.bodyMedium,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }


                } else if (priceTrendHasSearched) {

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color =
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {

                        Text(
                            text = "No price history found.",
                            style =
                                MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
        @Composable
        private fun HomeScreen() {
            val barcodeFocusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

        var quickAddExpanded by remember {
            mutableStateOf(false)
        }

        var showReceiptImportOptions by remember {
                mutableStateOf(false)
            }

        LaunchedEffect(Unit) {
            barcodeFocusRequester.requestFocus()

            kotlinx.coroutines.delay(300)
            keyboardController?.hide()

            kotlinx.coroutines.delay(500)
            keyboardController?.hide()
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            // PantryPal header

            Text(
                text = "PantryPal",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your household, organised",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ============================================================
            // HOME SCREEN - PRIMARY RECEIPT WORKFLOW
            // ============================================================

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showReceiptImportOptions = true
                    },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Import Receipt",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Let PantryPal update your household automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "📥",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }

            richOrderPreview.value?.let { preview ->
            AlertDialog(
                onDismissRequest = { richOrderPreview.value = null },
                title = { Text("Coles order preview (PP-158A)") },
                text = { androidx.compose.foundation.layout.Column(modifier = Modifier.verticalScroll(rememberScrollState())) { Text(preview) } },
                confirmButton = { TextButton(onClick = { richOrderPreview.value = null }) { Text("Close") } }
            )
        }

        if (showReceiptImportOptions) {

                AlertDialog(
                    onDismissRequest = {
                        showReceiptImportOptions = false
                    },

                    title = {
                        Text(
                            text = "Import Receipt",
                            fontWeight = FontWeight.Bold
                        )
                    },

                    text = {

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            Button(
                                onClick = {

                                    showReceiptImportOptions = false

                                    launchReceiptDocumentScanner()
                                },

                                  modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📷  Scan Paper Receipt")
                            }

                            Button(
                                onClick = {

                                    showReceiptImportOptions = false

                                    receiptImageLauncher.launch(
                                        "image/*"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🖼  Import Receipt Image")
                            }


                            // PP-158B: isolated, read-only browser feasibility test.
                            Button(
                                onClick = {
                                    showReceiptImportOptions = false
                                    colesStagingLauncher.launch(Intent(this@MainActivity, ColesCaptureFeasibilityActivity::class.java))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🛒 Open Coles Order (Test)")
                            }

                            Button(
                                onClick = {
                                    showReceiptImportOptions = false
                                    richOrderLauncher.launch("*/*")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🛒 Coles Rich Order Preview")
                            }


                            Button(
                                onClick = {

                                    showReceiptImportOptions = false

                                    importReceiptLauncher.launch(
                                        "application/pdf"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📄  Import Receipt PDF")
                            }
                        }
                    },

                    confirmButton = {},

                    dismissButton = {

                        TextButton(
                            onClick = {
                                showReceiptImportOptions = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Household",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

// Inventory summary
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        refreshProducts()
                        navigateTo("INVENTORY")
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Inventory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text =
                                "${products.count { it.quantity > 0 }} products | " +
                                        "${products.sumOf { it.quantity.coerceAtLeast(0) }} items in stock",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (expirySummary.value.first > 0 ||
                            expirySummary.value.second > 0
                        ) {

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = buildString {

                                    if (expirySummary.value.first > 0) {
                                        append(
                                            "${expirySummary.value.first} expiring soon"
                                        )
                                    }

                                    if (
                                        expirySummary.value.first > 0 &&
                                        expirySummary.value.second > 0
                                    ) {
                                        append("  •  ")
                                    }

                                    if (expirySummary.value.second > 0) {
                                        append(
                                            "${expirySummary.value.second} expired"
                                        )
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Text(
                        text = "View ›",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

// ============================================================
// HOME SCREEN - SHOPPING AND RECEIPT CARDS
// ============================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Shopping List
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {

                            lifecycleScope.launch {

                                refreshShoppingList()

                                navigateTo("SHOPPING")
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 18.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "🛒",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Shopping List",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // Receipts
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {

                            lifecycleScope.launch {

                                refreshReceipts()

                                navigateTo("RECEIPTS")
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 18.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "🧾",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Receipts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))


// ============================================================
// HOME SCREEN - ANALYTICS
// ============================================================

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navigateTo("ANALYTICS")
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "📊",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Spending, retailers and price trends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "🔒  Pro",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quick Add/ Remove",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        quickAddExpanded = !quickAddExpanded
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 16.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Scan or enter an item",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Barcode or manual entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text =
                            if (quickAddExpanded) {
                                "⌃"
                            } else {
                                "›"
                            },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }


            Spacer(modifier = Modifier.height(8.dp))

            if (quickAddExpanded) {

                // Existing Quick Add controls will go inside here.

            }


            if (pendingUnknownBarcode.value != null) {
                AlertDialog(
                    onDismissRequest = {
                        pendingUnknownBarcode.value = null
                    },

                    title = {
                        Text("Unknown Product")
                    },
                    text = {
                        Column {
                            Text(
                                "Enter a name for this product:"
                            )
                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            OutlinedTextField(

                                value =
                                    pendingUnknownNameInput.value,

                                onValueChange = {
                                    pendingUnknownNameInput.value = it
                                },

                                label = {
                                    Text("Product name")
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            )
                        }
                    },

                    confirmButton = {

                        Button(
                            onClick = {
                                val barcode = pendingUnknownBarcode.value
                                val name = pendingUnknownNameInput.value.trim()

                                if (barcode != null && name.isNotBlank()) {

                                    lifecycleScope.launch {

                                        val dao = database.productDao()
                                        Log.d("PantryPal", "Saving barcode = '$barcode'")
                                        dao.insertBarcodeName(
                                            BarcodeNameEntity(
                                                barcode = barcode,
                                                name = name,
                                                lastUpdated = System.currentTimeMillis()
                                            )
                                        )
                                        val verify = dao.getBarcodeName(barcode)

                                        val expiry = expiryInput.value.trim().ifBlank { null }

                                        val existing =
                                            dao.getProductByBarcodeAndExpiry(barcode, expiry)

                                        val amount = pendingUnknownAmount.value

                                        if (existing != null) {
                                            dao.updateQuantityById(
                                                existing.id,
                                                existing.quantity + amount
                                            )
                                        } else {
                                            dao.insertProduct(
                                                ProductEntity(
                                                    barcode = barcode,
                                                    itemName = name,
                                                    quantity = amount,
                                                    lastScanned = System.currentTimeMillis(),
                                                    expiryDate = expiry,
                                                    location = selectedLocation.value
                                                )
                                            )
                                        }

                                        refreshProducts()
                                        expirySummary.value = getExpirySummary()

                                        pendingUnknownBarcode.value = null
                                        pendingUnknownNameInput.value = ""
                                        pendingUnknownAmount.value = 1
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },

                    dismissButton = {

                        Button(

                            onClick = {
                                pendingUnknownBarcode.value =
                                    null
                            }
                        ) {

                            Text("Cancel")
                        }
                    }
                )
            }
            if (quickAddExpanded) {

                val (soon, expired) = expirySummary.value

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            )
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    if (mode.value == "ADD") {
                                        "Add mode"
                                    } else {
                                        "Remove mode"
                                    },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color =
                                    if (mode.value == "ADD") {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = mode.value == "DELETE",
                                onCheckedChange = { removeMode ->
                                    mode.value =
                                        if (removeMode) {
                                            "DELETE"
                                        } else {
                                            "ADD"
                                        }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = "Quick Scan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = quickScanMode.value,
                                onCheckedChange = {
                                    quickScanMode.value = it
                                },
                                modifier = Modifier.focusable(false)
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                var expanded by remember {
                    mutableStateOf(false)
                }

                Text(
                    text = "Scan or enter barcode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = manualBarcodeInput.value,
                    onValueChange = {
                        manualBarcodeInput.value = it
                    },
                    label = {
                        Text("Barcode")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {

                            val barcode =
                                manualBarcodeInput.value.trim()

                            if (barcode.isNotBlank()) {

                                processBarcode(barcode)

                                manualBarcodeInput.value = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(barcodeFocusRequester)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Quantity",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = quantityInput.value,
                    onValueChange = {
                        quantityInput.value = it
                        saveIntakeDefaults()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        cameraPermissionLauncher.launch(
                            android.Manifest.permission.CAMERA
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan")
                }
            }

            // ============================================================
// HOME SCREEN - DATA MANAGEMENT
// ============================================================

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Export CSV
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            exportInventoryCsv()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 18.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "📤",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Export CSV",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // Import CSV
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            importCsvLauncher.launch("*/*")
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 18.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "📥",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Import CSV",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bulk-clear expired stock only; retain product records and shopping history.
            OutlinedButton(
                onClick = { showDeleteExpiredConfirm.value = true },
                enabled = expirySummary.value.second > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🗑️  Remove All Expired Stock (${expirySummary.value.second})")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    showClearConfirm.value = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "🗑️  Clear Inventory",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedButton(
                onClick = {
                    showRebuildInventoryConfirm.value = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "↻  Rebuild Inventory from Receipts",
                    fontWeight = FontWeight.SemiBold
                )
            }


// ============================================================
// REMOVE EXPIRED STOCK CONFIRMATION
// ============================================================
            if (showDeleteExpiredConfirm.value) {
                AlertDialog(
                    onDismissRequest = { showDeleteExpiredConfirm.value = false },
                    title = { Text("Remove all expired stock?") },
                    text = {
                        Text("${expirySummary.value.second} expired inventory records will be marked out of stock. " +
                            "Other inventory and receipt history will be retained. " +
                            "Shopping-list items will be refreshed.")
                    },
                    confirmButton = {
                        Button(onClick = {
                            showDeleteExpiredConfirm.value = false
                            lifecycleScope.launch {
                                val affected = database.productDao().markExpiredStockOutOfStock(
                                    LocalDate.now().toString()
                                )
                                refreshProducts()
                                expirySummary.value = getExpirySummary()
                                refreshShoppingList()
                                Toast.makeText(
                                    this@MainActivity,
                                    "$affected expired inventory records removed from stock.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }) { Text("Remove expired stock") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteExpiredConfirm.value = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

// ============================================================
// CLEAR INVENTORY CONFIRMATION
// ============================================================

            if (showClearConfirm.value) {

                AlertDialog(
                    onDismissRequest = {
                        showClearConfirm.value = false
                    },

                    title = {
                        Text("Clear inventory?")
                    },

                    text = {
                        Text(
                            "This will delete all stored inventory items " +
                                    "and clear the shopping list. " +
                                    "Your imported receipt history will be kept."
                        )
                    },

                    confirmButton = {

                        Button(
                            onClick = {

                                lifecycleScope.launch {

                                    database.withTransaction {
                                        database.productDao().clearAllProducts()
                                        database.receiptInventoryContributionDao().invalidateProductLinks()
                                        database.shoppingDao().clearAll()
                                    }

                                    refreshProducts()

                                    expirySummary.value =
                                        getExpirySummary()

                                    shoppingListItems.value =
                                        mutableListOf()

                                    showClearConfirm.value = false
                                }
                            }
                        ) {
                            Text("Clear inventory")
                        }
                    },

                    dismissButton = {

                        Button(
                            onClick = {
                                showClearConfirm.value = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }


// ============================================================
// REBUILD INVENTORY FROM RECEIPTS
// ============================================================

            if (showRebuildInventoryConfirm.value) {
                AlertDialog(
                    onDismissRequest = { showRebuildInventoryConfirm.value = false },
                    title = { Text("Replace inventory from receipts?") },
                    text = {
                        Text(
                            "This recovery operation REPLACES all current inventory " +
                                "with products originally added from saved receipts. " +
                                "Manually added products, stock adjustments and current " +
                                "expiry dates will be lost. Receipt history and your " +
                                "shopping list will be retained.\n\n" +
                                "Purchased quantities will be restored, but PantryPal " +
                                "cannot know what has since been consumed or removed. " +
                                "Only use this to recover inventory from receipt history."
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showRebuildInventoryConfirm.value = false
                            lifecycleScope.launch {
                                try {
                                    val allReceiptItems = database.receiptItemDao().getAllItems()
                                    val allContributions = database.receiptInventoryContributionDao().getAll()
                                    val itemById = allReceiptItems.associateBy { it.id }
                                    val recoverable = allContributions.mapNotNull { contribution ->
                                        itemById[contribution.receiptItemId]?.let { item ->
                                            contribution to item
                                        }
                                    }
                                    if (recoverable.isEmpty()) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "No receipt-linked inventory contributions found. Inventory was not changed.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }
                                    val distinctProducts = recoverable.map { it.second.productName.trim().lowercase(Locale.ROOT) }.toSet().size
                                    val totalUnits = recoverable.sumOf { it.first.quantityAdded.toLong() }
                                    database.withTransaction {
                                        database.productDao().clearAllProducts()
                                        database.receiptInventoryContributionDao().deleteAll()
                                        recoverable.forEach { (contribution, item) ->
                                            val productId = addOrUpdateInventoryItem(
                                                productName = item.productName,
                                                quantity = contribution.quantityAdded,
                                                barcode = "",
                                                purchaseDate = parseReceiptDateForSorting(item.receiptDate)
                                            )
                                            database.receiptInventoryContributionDao().insert(
                                                contribution.copy(id = 0, productId = productId)
                                            )
                                        }
                                    }
                                    refreshProducts()
                                    expirySummary.value = getExpirySummary()
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Recovered $distinctProducts products and $totalUnits receipt-linked units.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    Log.e("PantryPalInventory", "Receipt inventory rebuild failed", e)
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Rebuild failed; inventory was not replaced: ${e.message ?: "Unknown error"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }) { Text("Replace inventory") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRebuildInventoryConfirm.value = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
    // PP-154: Barcode-only image lookup; never guess an exact product from a receipt name.
    // Android's cacheDir keeps downloaded thumbnails off the Room database.
    private suspend fun loadProductThumbnail(barcode: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val code = barcode.trim()
            if (!code.matches(Regex("[0-9]{8,14}"))) return@withContext null
            val cacheKey = MessageDigest.getInstance("SHA-256")
                .digest(code.toByteArray()).joinToString("") { "%02x".format(it) }
            val cached = File(cacheDir, "pp_product_$cacheKey.jpg")
            try {
                if (cached.exists()) return@withContext BitmapFactory.decodeFile(cached.path)
                val api = URL("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=status,code,image_front_small_url,image_front_url")
                val connection = (api.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "PantryPal/1.0 (Android; product-image-lookup)")
                }
                val response = try {
                    if (connection.responseCode != 200) return@withContext null
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally { connection.disconnect() }
                val json = JSONObject(response)
                if (json.optInt("status") != 1) return@withContext null
                val item = json.optJSONObject("product") ?: return@withContext null
                val imageUrl = item.optString("image_front_small_url")
                    .ifBlank { item.optString("image_front_url") }
                if (!imageUrl.startsWith("https://")) return@withContext null
                val imageConnection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                val bytes = try {
                    if (imageConnection.responseCode != 200) return@withContext null
                    imageConnection.inputStream.use { it.readBytes() }
                } finally { imageConnection.disconnect() }
                if (bytes.size > 2_000_000) return@withContext null
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext null
                cached.writeBytes(bytes)
                bitmap
            } catch (_: Exception) { null }
        }

    // PP-156F: classify the primary product, not an incidental flavour or brand.
    // Emoji are illustrations, not verified catalogue photographs. In particular,
    // Unicode has no dedicated blackberry or raspberry emoji: do not show a blueberry.
    private fun productCategorySymbol(name: String): String =
        com.example.myapplication.data.ProductVisualClassifier.symbol(name)

    @Composable
    private fun InventoryThumbnail(product: ProductEntity) {
        // PP-157 pilot: verified local catalogue images take priority over remote lookup.
        // Name matching is deliberately strict; unrelated variants keep their existing image.
        val bitmap by produceState<Bitmap?>(initialValue = null, key1 = product.itemName, key2 = product.barcode) {
            value = withContext(Dispatchers.IO) {
                PantryPalPhotoCatalogue.imageFor(applicationContext, product.itemName)
            } ?: if (PantryPalPhotoCatalogue.isAmbiguousTimTam(product.itemName)) {
                // Avoid an unrelated barcode image for an unverified Tim Tam variety.
                null
            } else {
                loadProductThumbnail(product.barcode)
            }
        }
        Surface(
            modifier = Modifier.size(86.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Product image for ${product.itemName}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        if (PantryPalPhotoCatalogue.isAmbiguousTimTam(product.itemName)) "🍪"
                        else productCategorySymbol(product.itemName),
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
        }
    }

    @Composable
    private fun InventoryScreen() {
        // Load learned classifications asynchronously; saved corrections override heuristics.
        val categoryRefresh = inventoryCategoryRefresh.longValue
        val productNames = products.map { it.itemName }.distinct()
        var categoryByName by remember { mutableStateOf<Map<String, HouseholdCategory>>(emptyMap()) }
        LaunchedEffect(productNames, categoryRefresh) {
            categoryByName = withContext(Dispatchers.IO) {
                productNames.associateWith { name -> resolveHouseholdCategory(name) }
            }
        }
        fun categoryFor(product: ProductEntity): HouseholdCategory =
            categoryByName[product.itemName]
                ?: HouseholdCategoryResolver.resolve(product.itemName)
        val categoryFiltered = products.filter { product ->
            when (inventoryFoodFilter.value) {
                "FOOD" -> categoryFor(product) == HouseholdCategory.FOOD
                "NON_FOOD" -> categoryFor(product) == HouseholdCategory.OTHER
                else -> true
            }
        }
        val inStockCount = categoryFiltered.count { it.quantity > 0 }
        val outOfStockCount = categoryFiltered.count { it.quantity <= 0 }
        val allCount = categoryFiltered.size
        val filteredProducts = categoryFiltered.filter { product ->
            val matchesStock = when (inventoryStockFilter.value) {
                "IN_STOCK" -> product.quantity > 0
                "OUT_OF_STOCK" -> product.quantity <= 0
                else -> true
            }
            matchesStock && (
                product.itemName.contains(searchText.value, ignoreCase = true) ||
                    product.barcode.contains(searchText.value, ignoreCase = true)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PantryPal", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Inventory", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    currentScreen.value = "HOME"
                    searchText.value = ""
                }) { Text("‹ Back to Home") }
                if (searchText.value.isNotBlank()) {
                    Text("${filteredProducts.size} matches", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(
                value = searchText.value,
                onValueChange = { searchText.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search inventory") },
                trailingIcon = {
                    if (searchText.value.isNotBlank()) {
                        IconButton(onClick = { searchText.value = "" }) { Text("×") }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("IN_STOCK", "In Stock", inStockCount),
                    Triple("OUT_OF_STOCK", "Out of Stock", outOfStockCount),
                    Triple("ALL", "All", allCount)
                ).forEach { (filter, label, count) ->
                    val selected = inventoryStockFilter.value == filter
                    Surface(
                        modifier = Modifier.weight(1f)
                            .clickable { inventoryStockFilter.value = filter },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, maxLines = 2, softWrap = true,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold)
                            Text("$count", maxLines = 1, softWrap = false,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ALL" to "All", "FOOD" to "Food", "NON_FOOD" to "Non-food")
                    .forEach { (key, label) ->
                        FilterChip(
                            selected = inventoryFoodFilter.value == key,
                            onClick = { inventoryFoodFilter.value = key },
                            label = { Text(label, maxLines = 1, softWrap = false,
                                style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(if (key == "NON_FOOD") 1.45f else 0.8f)
                        )
                    }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(filteredProducts) { product ->

                    val expiryText =
                        product.expiryDate ?: "Not set"

                    val statusText =
                        if (product.quantity <= 0) "Out of stock"
                        else expiryStatus(product.expiryDate)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingProduct.value = product
                                editNameInput.value = product.itemName
                                deleteQuantityInput.value = "1"
                                navigateTo("DETAIL")
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.itemName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${product.location}  ·  Qty ${product.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Expiry: $expiryText",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (statusText.isNotBlank()) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (statusText.contains("expired", ignoreCase = true))
                                                MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                InventoryThumbnail(product)
                            }
                            TextButton(onClick = {
                                lifecycleScope.launch {
                                    val description = product.itemName.trim()
                                    val key = description.lowercase()
                                    val existing = database.shoppingDao().findByDescription(key)
                                    if (existing == null) {
                                        database.shoppingDao().insertItem(
                                            ShoppingItemEntity(
                                                description = description,
                                                normalisedDescription = key,
                                                source = "MANUAL"
                                            )
                                        )
                                    }
                                    refreshShoppingList()
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (existing == null) "Added to shopping list" else "Already on shopping list",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }) { Text("Add to shopping list") }

                            if (product.barcode.isNotBlank()) {

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text = "Barcode: ${product.barcode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }


    }

    @Composable
    private fun ProductDetailScreen() {
        val product = editingProduct.value
        var showDeleteConfirmation by remember { mutableStateOf(false) }
        var selectedDetailLocation by remember(product?.id) {
            mutableStateOf(product?.location ?: "Pantry")
        }
        var detailExpiryText by remember(product?.id) {
            mutableStateOf(product?.expiryDate ?: "")
        }
        var detailQuantityText by remember(product?.id) {
            mutableStateOf(product?.quantity?.toString() ?: "0")
        }
        var detailCategory by remember(product?.id) {
            mutableStateOf(HouseholdCategory.UNKNOWN)
        }
        LaunchedEffect(product?.id) {
            if (product != null) {
                detailCategory = withContext(Dispatchers.IO) {
                    resolveHouseholdCategory(product.itemName)
                }
            }
        }

        if (product == null) {
            currentScreen.value = "INVENTORY"
            return
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).padding(bottom = 80.dp)
        ) {
            Text("PantryPal", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Product details", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                editingProduct.value = null
                currentScreen.value = "INVENTORY"
            }) { Text("‹ Back to Inventory") }

            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editNameInput.value,
                        onValueChange = { editNameInput.value = it },
                        label = { Text("Product name") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = detailQuantityText,
                        onValueChange = { detailQuantityText = it },
                        label = { Text("Quantity in stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Text("Product classification", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            HouseholdCategory.FOOD to "Food",
                            HouseholdCategory.OTHER to "Non-food"
                        ).forEach { (category, label) ->
                            FilterChip(
                                selected = (detailCategory == HouseholdCategory.FOOD) == (category == HouseholdCategory.FOOD) &&
                                    detailCategory != HouseholdCategory.UNKNOWN,
                                onClick = { detailCategory = category },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text("Classification is saved with the other changes.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Storage location", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Pantry", "Fridge", "Freezer").forEach { location ->
                            FilterChip(
                                selected = selectedDetailLocation == location,
                                onClick = {
                                    if (selectedDetailLocation != location) {
                                        selectedDetailLocation = location
                                        val knowledge = ProductKnowledgeResolver.resolve(
                                            editNameInput.value.trim().ifBlank { product.itemName }
                                        )
                                        val shelfDays = if (location == "Freezer")
                                            maxOf(knowledge.suggestedShelfLifeDays, 365)
                                        else knowledge.suggestedShelfLifeDays
                                        detailExpiryText = LocalDate.now()
                                            .plusDays(shelfDays.toLong()).toString()
                                    }
                                },
                                label = { Text(location, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = detailExpiryText,
                        onValueChange = { detailExpiryText = it },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Text("Changing storage updates the suggested expiry. You can edit it before saving.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val name = editNameInput.value.trim()
                    val quantity = detailQuantityText.trim().toIntOrNull()
                    val expiry = detailExpiryText.trim().ifBlank { null }
                    val validExpiry = expiry == null || try {
                        LocalDate.parse(expiry); true
                    } catch (_: Exception) { false }
                    when {
                        name.isBlank() -> Toast.makeText(this@MainActivity,
                            "Enter a product name", Toast.LENGTH_LONG).show()
                        quantity == null || quantity < 0 -> Toast.makeText(this@MainActivity,
                            "Enter a non-negative whole-number quantity", Toast.LENGTH_LONG).show()
                        !validExpiry -> Toast.makeText(this@MainActivity,
                            "Enter an expiry date as YYYY-MM-DD", Toast.LENGTH_LONG).show()
                        else -> lifecycleScope.launch {
                            database.withTransaction {
                                database.productDao().updateProductNameById(product.id, name)
                                database.productDao().updateQuantityById(product.id, quantity)
                                database.productDao().updateLocationById(product.id, selectedDetailLocation)
                                database.productDao().updateExpiryDateById(product.id,
                                    if (quantity == 0) null else expiry)
                            }
                            if (detailCategory != HouseholdCategory.UNKNOWN) {
                                saveCategoryPreference(name, detailCategory)
                            }
                            inventoryCategoryRefresh.longValue++
                            refreshProducts()
                            expirySummary.value = getExpirySummary()
                            editingProduct.value = null
                            currentScreen.value = "INVENTORY"
                            Toast.makeText(this@MainActivity, "Product saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()
            ) { Text("Save Changes") }
            Spacer(modifier = Modifier.height(20.dp))
// ============================================================
// PERMANENT DELETE
// ============================================================

            OutlinedButton(
                onClick = {
                    showDeleteConfirmation = true
                },

                modifier = Modifier.fillMaxWidth(),

                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error
                )
            ) {

                Text(
                    text = "Delete Product Permanently",
                    fontWeight = FontWeight.SemiBold
                )
            }

// ============================================================
// DELETE CONFIRMATION
// ============================================================

            if (showDeleteConfirmation) {

                AlertDialog(
                    onDismissRequest = {
                        showDeleteConfirmation = false
                    },

                    title = {
                        Text(
                            text = "Delete product?"
                        )
                    },

                    text = {
                        Text(
                            text = "This will permanently delete " +
                                    "\"${product.itemName}\" from PantryPal."
                        )
                    },

                    confirmButton = {

                        TextButton(
                            onClick = {

                                showDeleteConfirmation = false

                                lifecycleScope.launch {

                                    database.productDao()
                                        .deleteProductById(product.id)

                                    refreshProducts()

                                    editingProduct.value = null

                                    currentScreen.value = "INVENTORY"
                                }
                            }
                        ) {

                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },

                    dismissButton = {

                        TextButton(
                            onClick = {
                                showDeleteConfirmation = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }


        }
    }
    @Composable
    private fun ShoppingListScreen() {
        var showClearShoppingConfirmation by remember { mutableStateOf(false) }

        if (showClearShoppingConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearShoppingConfirmation = false },
                title = { Text("Clear shopping list?") },
                text = { Text("Remove every shopping-list entry, including checked items? Inventory and receipts will not be changed.") },
                confirmButton = {
                    TextButton(onClick = {
                        showClearShoppingConfirmation = false
                        lifecycleScope.launch {
                            database.shoppingDao().clearAll()
                            refreshShoppingList()
                            checkedShoppingItems.value = emptySet()
                            Toast.makeText(this@MainActivity, "Shopping list cleared", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Clear list") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearShoppingConfirmation = false }) { Text("Cancel") }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            // ============================================================
// SHOPPING LIST - PANTRYPAL HEADER
// ============================================================

            Text(
                text = "PantryPal",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Shopping list",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))


            TextButton(
                onClick = {
                    currentScreen.value = "HOME"
                }
            ) {
                Text(
                    text = "‹ Back to Home",
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = { showClearShoppingConfirmation = true },
                enabled = shoppingListItems.value.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear Shopping List") }

            Spacer(modifier = Modifier.height(8.dp))

            // ============================================================
// SHOPPING LIST - MANUAL ADD CARD
// ============================================================

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Add an item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))


                    OutlinedTextField(
                        value = manualShoppingItemInput.value,

                        onValueChange = {
                            manualShoppingItemInput.value = it
                        },

                        label = {
                            Text("Shopping item")
                        },

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {

                            val item =
                                manualShoppingItemInput.value.trim()

                            if (item.isNotBlank()) {

                                lifecycleScope.launch {

                                    val existing =
                                        database.shoppingDao().findByDescription(
                                            item.lowercase().trim()
                                        )

                                    if (existing == null) {

                                        database.shoppingDao().insertItem(
                                            ShoppingItemEntity(
                                                description = item,
                                                normalisedDescription =
                                                    item.trim().lowercase(),
                                                source = "MANUAL"
                                            )
                                        )
                                    }

                                    val shoppingItems =
                                        database.shoppingDao().getAllItems()

                                    shoppingListEntities.value =
                                        shoppingItems

                                    shoppingListItems.value =
                                        shoppingItems
                                            .map { it.description }
                                            .toMutableList()
                                }

                                manualShoppingItemInput.value = ""
                            }
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Item")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (shoppingListItems.value.isEmpty()) {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Text(
                        text = "No items currently need replacing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }

            } else {

                shoppingListItems.value.forEach { item ->

                    val shoppingEntity =
                        shoppingListEntities.value
                            .firstOrNull {
                                it.description == item
                            }

                    val reason =
                        when {
                            item.endsWith("— Out of stock") ->
                                "Out of stock"

                            item.endsWith("— Expiring soon") ->
                                "Expiring soon"

                            item.endsWith("— Expired") ->
                                "Expired"

                            else ->
                                null
                        }

                    val productName =
                        when (reason) {
                            "Out of stock" ->
                                item.removeSuffix("— Out of stock").trim()

                            "Expiring soon" ->
                                item.removeSuffix("— Expiring soon").trim()

                            "Expired" ->
                                item.removeSuffix("— Expired").trim()

                            else ->
                                item
                        }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Checkbox(
                                checked =
                                    checkedShoppingItems.value.contains(item),

                                onCheckedChange = { checked ->

                                    checkedShoppingItems.value =
                                        if (checked) {
                                            checkedShoppingItems.value + item
                                        } else {
                                            checkedShoppingItems.value - item
                                        }
                                }
                            )

                            Spacer(modifier = Modifier.width(2.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = productName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (shoppingEntity?.source == "MANUAL") {

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {

                                        OutlinedButton(
                                            onClick = {

                                                if (shoppingEntity.quantity > 1) {

                                                    lifecycleScope.launch {

                                                        database.shoppingDao().updateQuantity(
                                                            id = shoppingEntity.id,
                                                            quantity = shoppingEntity.quantity - 1,
                                                            modified = System.currentTimeMillis()
                                                        )

                                                        refreshShoppingList()
                                                    }
                                                }
                                            },
                                            enabled = shoppingEntity.quantity > 1
                                        ) {
                                            Text("−", style = MaterialTheme.typography.bodyMedium)
                                        }

                                        Text(
                                            text = "Qty: ${shoppingEntity.quantity}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        OutlinedButton(
                                            onClick = {

                                                lifecycleScope.launch {

                                                    database.shoppingDao().updateQuantity(
                                                        id = shoppingEntity.id,
                                                        quantity = shoppingEntity.quantity + 1,
                                                        modified = System.currentTimeMillis()
                                                    )

                                                    refreshShoppingList()
                                                }
                                            }
                                        ) {
                                            Text("+", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }

                                if (reason != null) {

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color =
                                            when (reason) {

                                                "Expired" ->
                                                    MaterialTheme.colorScheme.error

                                                "Out of stock" ->
                                                    MaterialTheme.colorScheme.error

                                                else ->
                                                    MaterialTheme.colorScheme.primary
                                            }
                                    )
                                }
                            }
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                }
            }

// ============================================================
// SHOPPING LIST - SHARE SELECTED
// ============================================================

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = {
                    shareSelectedShoppingItems()
                },
                enabled =
                    checkedShoppingItems.value.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Share Selected"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    lifecycleScope.launch {
                        refreshReceipts()
                        currentScreen.value = "RECEIPTS"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Receipts",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun ReceiptScreen() {

        android.util.Log.e(
            "PantryPalReceipt",
            "RECEIPT SCREEN OPENED"
        )

        val archiveCutoffDate =
            LocalDate.now().minusYears(1)

        val activeReceipts =
            receiptItems.value.filter { receipt ->

                val receiptDate =
                    parseReceiptDateForSorting(
                        receipt.receiptDate
                    )

                receiptDate == null ||
                        !receiptDate.isBefore(
                            archiveCutoffDate
                        )
            }

        val archivedReceipts =
            receiptItems.value.filter { receipt ->

                val receiptDate =
                    parseReceiptDateForSorting(
                        receipt.receiptDate
                    )

                receiptDate != null &&
                        receiptDate.isBefore(
                            archiveCutoffDate
                        )
            }

        Column(

            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)

        ) {

            Text(
                text = "PantryPal",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Receipts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Review imported receipts and purchase history",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                TextButton(
                    onClick = {
                        currentScreen.value = "HOME"
                    }
                ) {
                    Text(
                        text = "‹ Back to Home",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = {
                        receiptSelectionMode.value =
                            !receiptSelectionMode.value

                        if (!receiptSelectionMode.value) {
                            selectedReceiptIds.value = emptySet()
                        }
                    }
                ) {
                    Text(
                        text =
                            if (receiptSelectionMode.value)
                                "Cancel"
                            else
                                "Select",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = {
                    navigateTo("ARCHIVED_RECEIPTS")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = archivedReceipts.isNotEmpty()
            ) {
                Text(
                    text =
                        "View Archived Receipts (${archivedReceipts.size})",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (
                receiptSelectionMode.value &&
                selectedReceiptIds.value.isNotEmpty()
            ) {

                OutlinedButton(
                    onClick = {
                        showDeleteSelectedReceiptsDialog.value = true
                    },

                    modifier = Modifier.fillMaxWidth(),

                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),

                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    )
                ) {

                    Text(
                        text = "Delete Selected (${selectedReceiptIds.value.size})",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )


            if (activeReceipts.isEmpty()) {

                Text("No active receipts.")

            } else {

                activeReceipts.forEach { receipt ->
                        android.util.Log.e(
                            "PantryPalReceipt",
                            "RECEIPT FOUND: ${receipt.storeName}"
                        )

                        receipt.rawText.lines().forEachIndexed { index, line ->

                            android.util.Log.e(
                                "PantryPalReceipt",
                                "${index + 1}: $line"
                            )
                        }

                    var expanded by remember(receipt.id) {
                        mutableStateOf(false)
                    }
                        val theme =
                            RetailerThemeResolver.getTheme(receipt.storeName)

                        var storedReceiptProducts by remember(receipt.id) {
                            mutableStateOf<List<ReceiptItemEntity>>(emptyList())
                        }

                        LaunchedEffect(receipt.id) {

                            storedReceiptProducts =
                                database
                                    .receiptItemDao()
                                    .getItemsForReceipt(
                                        receipt.id.toLong()
                                    )
                        }

                        ReceiptCard(
                            receipt = receipt,
                            receiptProducts = storedReceiptProducts,
                            theme = theme,
                            expanded = expanded,

                            onExpandToggle = {
                                expanded = !expanded
                            },

                            onLongPress = {

                                receiptSelectionMode.value = true

                                selectedReceiptIds.value =
                                    selectedReceiptIds.value + receipt.id
                            },

                            onShare = {
                                shareReceipt(
                                    receipt = receipt,
                                    receiptProducts = storedReceiptProducts
                                )
                            },

                            onDelete = {

                                lifecycleScope.launch {

                                    deleteReceiptsWithInventory(listOf(receipt.id))
                                    refreshProducts()
                                    expirySummary.value = getExpirySummary()
                                    refreshReceipts()
                                }
                            },

                            selectionMode = receiptSelectionMode.value,

                            selected =
                                selectedReceiptIds.value.contains(receipt.id),

                            onSelectionChange = { checked ->

                                selectedReceiptIds.value =
                                    if (checked) {

                                        selectedReceiptIds.value + receipt.id

                                    } else {

                                        selectedReceiptIds.value - receipt.id
                                    }
                            }
                        )
                    }
                }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    showClearReceiptsDialog.value = true
                },

                modifier = Modifier.fillMaxWidth(),

                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Clear Imported Receipts",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (showClearReceiptsDialog.value) {

                AlertDialog(
                    onDismissRequest = {
                        showClearReceiptsDialog.value = false
                    },
                    title = {
                        Text("Delete Receipts")
                    },
                    text = {
                        Text("Delete all imported receipts and subtract their recorded inventory contributions?")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                lifecycleScope.launch {

                                    deleteReceiptsWithInventory(database.receiptDao().getAllReceipts().map { it.id })
                                    refreshProducts()
                                    expirySummary.value = getExpirySummary()
                                    refreshReceipts()

                                    showClearReceiptsDialog.value = false
                                }
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showClearReceiptsDialog.value = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = {
                    launchReceiptDocumentScanner()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Scan Paper Receipt",
                    fontWeight = FontWeight.SemiBold
                )
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )
            // OCR Button start

            Button(
                onClick = {
                    receiptImageLauncher.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Import Receipt Image",
                    fontWeight = FontWeight.SemiBold
                )
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )
            // OCR Button finish

            Button(
                onClick = {
                    importReceiptLauncher.launch("application/pdf")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Import Receipt PDF",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedButton(
                onClick = {
                    navigateTo("PRICE_HISTORY")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Price History",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (showDeleteSelectedReceiptsDialog.value) {

            AlertDialog(
                onDismissRequest = {
                    showDeleteSelectedReceiptsDialog.value = false
                },
                title = {
                    Text("Delete Selected Receipts")
                },
                text = {
                    Text(
                        "Delete ${selectedReceiptIds.value.size} selected receipt(s) " +
                                "and their associated purchase history?"
                    )
                },
                confirmButton = {

                    Button(
                        onClick = {

                            lifecycleScope.launch {

                                deleteReceiptsWithInventory(selectedReceiptIds.value)
                                refreshProducts()
                                expirySummary.value = getExpirySummary()

                                selectedReceiptIds.value = emptySet()

                                receiptSelectionMode.value = false

                                refreshReceipts()

                                showDeleteSelectedReceiptsDialog.value = false
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {

                    Button(
                        onClick = {
                            showDeleteSelectedReceiptsDialog.value = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

    }

    @Composable
    private fun ArchivedReceiptScreen() {

        val archiveCutoffDate =
            LocalDate.now().minusYears(1)

        val archivedReceipts =
            receiptItems.value.filter { receipt ->

                val receiptDate =
                    parseReceiptDateForSorting(
                        receipt.receiptDate
                    )

                receiptDate != null &&
                        receiptDate.isBefore(
                            archiveCutoffDate
                        )
            }

        var archiveSelectionMode by remember { mutableStateOf(false) }
        var archiveSelectedIds by remember { mutableStateOf(setOf<Int>()) }
        var showArchiveDeleteConfirm by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            Text(
                text = "PantryPal",
                style =
                    MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Archived Receipts",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Receipts more than 12 months old",
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TextButton(
                onClick = {
                    navigateBack()
                }
            ) {
                Text(
                    text = "‹ Back to Receipts",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    archiveSelectionMode = !archiveSelectionMode
                    if (!archiveSelectionMode) archiveSelectedIds = emptySet()
                }) {
                    Text(if (archiveSelectionMode) "Cancel selection" else "Select receipts")
                }
            }

            if (archiveSelectionMode && archiveSelectedIds.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showArchiveDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Selected (${archiveSelectedIds.size})")
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            if (archivedReceipts.isEmpty()) {

                Text(
                    text = "No archived receipts."
                )

            } else {

                archivedReceipts.forEach { receipt ->

                    var expanded by remember(
                        receipt.id
                    ) {
                        mutableStateOf(false)
                    }

                    val theme =
                        RetailerThemeResolver
                            .getTheme(
                                receipt.storeName
                            )

                    var storedReceiptProducts by remember(
                        receipt.id
                    ) {
                        mutableStateOf<
                                List<ReceiptItemEntity>
                                >(emptyList())
                    }

                    LaunchedEffect(receipt.id) {

                        storedReceiptProducts =
                            database
                                .receiptItemDao()
                                .getItemsForReceipt(
                                    receipt.id.toLong()
                                )
                    }

                    ReceiptCard(
                        receipt = receipt,
                        receiptProducts =
                            storedReceiptProducts,
                        theme = theme,
                        expanded = expanded,

                        onExpandToggle = {
                            expanded = !expanded
                        },

                        onShare = {
                            shareReceipt(
                                receipt = receipt,
                                receiptProducts = storedReceiptProducts
                            )
                        },

                        onDelete = {
                            lifecycleScope.launch {
                                deleteReceiptsWithInventory(listOf(receipt.id))
                                refreshProducts()
                                expirySummary.value = getExpirySummary()
                                archiveSelectedIds = archiveSelectedIds - receipt.id
                                refreshReceipts()
                            }
                        },
                        onLongPress = {
                            archiveSelectionMode = true
                            archiveSelectedIds = archiveSelectedIds + receipt.id
                        },
                        selectionMode = archiveSelectionMode,
                        selected = receipt.id in archiveSelectedIds,
                        onSelectionChange = { checked ->
                            archiveSelectedIds = if (checked) archiveSelectedIds + receipt.id
                                else archiveSelectedIds - receipt.id
                        }
                    )
                }
            }
        }
        if (showArchiveDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showArchiveDeleteConfirm = false },
                title = { Text("Delete archived receipts?") },
                text = { Text("Delete ${archiveSelectedIds.size} receipt(s), their purchase history, and subtract recorded inventory contributions?") },
                confirmButton = {
                    TextButton(onClick = {
                        lifecycleScope.launch {
                            deleteReceiptsWithInventory(archiveSelectedIds)
                            refreshProducts()
                            expirySummary.value = getExpirySummary()
                            archiveSelectedIds = emptySet()
                            archiveSelectionMode = false
                            showArchiveDeleteConfirm = false
                            refreshReceipts()
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showArchiveDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }

    // PP-148: Reconcile receipt-linked stock and history atomically.
    private suspend fun deleteReceiptsWithInventory(receiptIds: Collection<Int>) {
        database.withTransaction {
            val productDao = database.productDao()
            val contributionDao = database.receiptInventoryContributionDao()
            receiptIds.distinct().forEach { receiptId ->
                val contributions = contributionDao.getForReceipt(receiptId.toLong())
                contributions.groupBy { it.productId }.forEach { (productId, entries) ->
                    val product = productDao.getProductById(productId)
                    if (product != null) {
                        val contributed = entries.sumOf { it.quantityAdded.toLong() }
                        val newQuantity = (product.quantity.toLong() - contributed)
                            .coerceAtLeast(0L).toInt()
                        productDao.updateQuantityById(productId, newQuantity)
                        if (newQuantity == 0) {
                            productDao.updateExpiryDateById(productId, null)
                        }
                    }
                }
                contributionDao.deleteForReceipt(receiptId.toLong())
                database.receiptItemDao().deleteForReceipt(receiptId.toLong())
                database.receiptDao().deleteReceiptById(receiptId)
            }
        }
    }

    private fun shareReceipt(
        receipt: ReceiptEntity,
        receiptProducts: List<ReceiptItemEntity>
    ) {

        val retailer =
            receipt.storeName
                ?.takeIf { it.isNotBlank() }
                ?: "Unknown retailer"

        val receiptText =
            buildString {

                appendLine("PantryPal Receipt")
                appendLine()
                appendLine(retailer)
                appendLine(
                    "Date: ${formatReceiptDate(receipt.receiptDate)}"
                )

                appendLine(
                    "Total: ${
                        receipt.totalAmount
                            ?.let { "$%.2f".format(it) }
                            ?: "-"
                    }"
                )

                appendLine()
                appendLine("Items")
                appendLine()

                receiptProducts.forEach { item ->

                    appendLine(item.productName)

                    val details =
                        buildList {

                            item.quantity?.let {
                                add("Qty: $it")
                            }

                            item.unit?.let {
                                if (it.isNotBlank()) {
                                    add(it)
                                }
                            }
                        }
                            .joinToString(" • ")

                    if (details.isNotBlank()) {
                        appendLine(details)
                    }

                    val price =
                        item.totalPrice
                            ?.let { "$%.2f".format(it) }
                            ?: item.unitPrice
                                ?.let { "$%.2f".format(it) }
                            ?: ""

                    if (price.isNotBlank()) {
                        appendLine(price)
                    }

                    appendLine()
                }

                appendLine("Shared from PantryPal")
            }

        val shareIntent =
            android.content.Intent(
                android.content.Intent.ACTION_SEND
            ).apply {

                type = "text/plain"

                putExtra(
                    android.content.Intent.EXTRA_SUBJECT,
                    "PantryPal Receipt - $retailer"
                )

                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    receiptText
                )
            }

        startActivity(
            android.content.Intent.createChooser(
                shareIntent,
                "Share receipt"
            )
        )
    }

    private fun launchReceiptCamera() {

        val photoFile =
            java.io.File.createTempFile(
                "pantrypal_receipt_",
                ".jpg",
                cacheDir
            )

        val photoUri =
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

        receiptCameraUri =
            photoUri

        receiptCameraLauncher.launch(
            photoUri
        )
    }

    private fun launchReceiptDocumentScanner() {

        val options =
            com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
                .Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(
                    com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
                )
                .setScannerMode(
                    com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
                )
                .build()

        val scanner =
            com.google.mlkit.vision.documentscanner.GmsDocumentScanning
                .getClient(options)

        scanner
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->

                val request =
                    androidx.activity.result.IntentSenderRequest
                        .Builder(intentSender)
                        .build()

                receiptDocumentScannerLauncher.launch(request)
            }
            .addOnFailureListener { exception ->

                android.util.Log.e(
                    "PantryPalScanner",
                    "Unable to launch Document Scanner",
                    exception
                )

                Toast.makeText(
                    this,
                    "Unable to start receipt scanner.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    @Composable
    private fun PriceHistoryScreen() {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            // ============================================================
            // IMPORT REVIEW - COMPACT HEADER
            // ============================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Review Receipt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${importedItemsForReview.value.size} products",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TextButton(
                onClick = {
                    navigateBack()
                }
            ) {
                Text(
                    text = "‹ Back to Receipts",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = priceHistorySearch.value,
                onValueChange = {
                    priceHistorySearch.value = it
                },
                label = {
                    Text("Product name")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = {

                    lifecycleScope.launch {

                        val searchTerm =
                            priceHistorySearch.value.trim()

                        priceHistoryResults.value =
                            database
                                .receiptItemDao()
                                .getPriceHistory(searchTerm)
                        priceHistoryHasSearched.value = true

                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search Price History")
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            if (priceHistoryResults.value.isNotEmpty()) {

                Text(
                    text = "Purchase history",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                priceHistoryResults.value.forEach { item ->

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = item.retailer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            item.receiptDate?.let { date ->

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = formatReceiptDate(date),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val displayPrice =
                                item.totalPrice
                                    ?: item.unitPrice

                            displayPrice?.let { price ->

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Text(
                                    text = "$%.2f".format(price),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }

            } else if (priceHistoryHasSearched.value) {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {

                    Text(
                        text = "No price history found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

        }
    }
    private fun saveIntakeDefaults() {
        prefs.edit()
            .putString("category", selectedCategory.value)
            .putString("location", selectedLocation.value)
            .putString("quantity", quantityInput.value)
            .apply()
    }
    private fun launchScanner() {
        val options = ScanOptions().apply {
            setPrompt("Scan a food barcode")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(PortraitCaptureActivity::class.java)
            setDesiredBarcodeFormats(ScanOptions.PRODUCT_CODE_TYPES)
        }
        barcodeLauncher.launch(options)
    }
    private fun shareSelectedShoppingItems() {

        if (checkedShoppingItems.value.isEmpty()) {

            Toast.makeText(
                this,
                "Select at least one shopping item.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "PantryPal Shopping List"
                )

                putExtra(
                    Intent.EXTRA_TEXT,
                    buildShoppingListText()
                )
            }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Share shopping list"
            )
        )
    }
    private val categoryExpiryDays = mapOf(
        "Pantry Dry Goods" to 180,
        "Canned Goods" to 365,
        "Refrigerated: Fresh" to 14,
        "Refrigerated: Long-life" to 90,
        "Frozen" to 90,
        "Bakery" to 5,
        "Fruit" to 7,
        "Other" to 14
    )

    // ============================================================
    // IMPORT REVIEW - SAVE LOCATION CORRECTION
    // ============================================================

    private suspend fun saveLocationCorrection(
        productName: String,
        location: String,
        purchaseDate: LocalDate? = null
    ) {

        val dao =
            database.productDao()

        val product =
            dao.getProductByName(productName)

        if (product != null) {

            dao.updateLocationById(
                product.id,
                location
            )

            // Recalculate PantryPal's estimated expiry when
            // the storage location is corrected during receipt review.
            //
            // Receipt imports currently generate estimated expiry dates,
            // so storage location should influence that estimate.

            if (purchaseDate != null) {

                val knowledge =
                    ProductKnowledgeResolver.resolve(productName)

                val shelfLifeDays =
                    when (location) {

                        "Freezer" -> 365

                        else ->
                            knowledge.suggestedShelfLifeDays
                    }

                val correctedExpiry =
                    purchaseDate
                        .plusDays(shelfLifeDays.toLong())
                        .toString()

                dao.updateExpiryDateById(
                    product.id,
                    correctedExpiry
                )
            }
        }

        val productKey =
            ProductPreferenceKeyResolver.resolve(productName)

        database
            .productLocationPreferenceDao()
            .savePreference(
                ProductLocationPreferenceEntity(
                    productKey = productKey,
                    originalName = productName,
                    location = location,
                    lastUpdated = System.currentTimeMillis()
                )
            )
    }
// ============================================================
// IMPORT REVIEW - DISPLAY MODEL
// ============================================================

    private data class ImportReviewItem(
        val productName: String,
        val originalDescription: String = productName,
        val manuallyCorrected: Boolean = false,
        val location: String,
        val category: HouseholdCategory,
        val quantity: Double?,
        val unit: String?,
        val unitPrice: Double? = null,
        val totalPrice: Double? = null,
        val receiptDate: String?
    )

    private val importedItemsForReview =
        mutableStateOf<List<ImportReviewItem>>(emptyList())

    private val reviewDiscountAmount = mutableStateOf("")

    private val pendingOcrReceipt =
        mutableStateOf<ParsedReceipt?>(null)

    private val pendingOcrRawText =
        mutableStateOf<String?>(null)


    // ============================================================
    // IMPORT REVIEW - MANUAL PRODUCT ENTRY STATE
    // ============================================================

    private val showAddReviewProductDialog =
        mutableStateOf(false)

    private val newReviewProductName =
        mutableStateOf("")

    private val newReviewProductQuantity =
        mutableStateOf("1")

    private val newReviewProductPrice =
        mutableStateOf("")

    private val newReviewProductCategory =
        mutableStateOf(HouseholdCategory.FOOD)

    private val newReviewProductLocation =
        mutableStateOf("Pantry")


    // ============================================================
    // IMPORT REVIEW - EDITABLE RECEIPT DATE
    // ============================================================

    private val reviewReceiptDate =
        mutableStateOf("")

    private val reviewRetailerName = mutableStateOf("")
    private val reviewReceiptTotal = mutableStateOf("")

    private val addImportedProductsToInventory = mutableStateOf(true)


    // ============================================================
// IMPORT REVIEW - LOCATION EDITOR STATE
// ============================================================

    private val locationEditItem =
        mutableStateOf<ImportReviewItem?>(null)

    private val locationEditSelection =
        mutableStateOf("")
    private val categoryDefaultLocations = mapOf(
        "Pantry Dry Goods" to "Pantry",
        "Canned Goods" to "Pantry",
        "Refrigerated: Fresh" to "Fridge",
        "Refrigerated: Long-life" to "Fridge",
        "Frozen" to "Freezer",
        "Bakery" to "Pantry",
        "Fruit" to "Pantry",
        "Other" to "Pantry"
    )
    private fun buildShoppingListText(): String {

        return buildString {

            append("PantryPal Shopping List\n\n")

            shoppingListItems.value
                .filter { item ->
                    checkedShoppingItems.value.contains(item)
                }
                .forEach { item ->

                    val shoppingEntity =
                        shoppingListEntities.value
                            .firstOrNull {
                                it.description == item
                            }

                    val reason =
                        when {
                            item.endsWith("— Out of stock") ->
                                "Out of stock"

                            item.endsWith("— Expiring soon") ->
                                "Expiring soon"

                            item.endsWith("— Expired") ->
                                "Expired"

                            else ->
                                null
                        }

                    val productName =
                        when (reason) {
                            "Out of stock" ->
                                item.removeSuffix("— Out of stock").trim()

                            "Expiring soon" ->
                                item.removeSuffix("— Expiring soon").trim()

                            "Expired" ->
                                item.removeSuffix("— Expired").trim()

                            else ->
                                item
                        }

                    val quantity =
                        shoppingEntity?.quantity ?: 1

                    append(
                        "$productName — Qty: $quantity\n"
                    )
                }



            append("\nShared from PantryPal")
        }
    }
    private fun applySuggestedExpiryDate() {

        val days =
            categoryExpiryDays[selectedCategory.value] ?: 0

        if (days > 0) {

            expiryInput.value =
                java.time.LocalDate.now()
                    .plusDays(days.toLong())
                    .toString()
        }
    }
    private fun applySuggestedLocation() {
        selectedLocation.value =
            categoryDefaultLocations[selectedCategory.value] ?: "Pantry"
    }

    private suspend fun resolveStorageLocation(
        productName: String
    ): String {

        val productKey =
            ProductPreferenceKeyResolver.resolve(productName)

        val learnedPreference =
            database
                .productLocationPreferenceDao()
                .getPreference(productKey)

        android.util.Log.e(
            "LocationPreferenceDebug",
            "name='$productName' | " +
                    "key='$productKey' | " +
                    "learned=${learnedPreference?.location}"
        )

        if (learnedPreference != null) {
            return learnedPreference.location
        }

        val defaultLocation =
            ProductKnowledgeResolver
                .resolve(productName)
                .storageLocation

        android.util.Log.e(
            "LocationPreferenceDebug",
            "Using default location=$defaultLocation"
        )

        return defaultLocation
    }
    // ============================================================
// PRODUCT CLASSIFICATION - RESOLVE LEARNED CATEGORY
// ============================================================

    private suspend fun resolveHouseholdCategory(
        productName: String
    ): HouseholdCategory {

        val productKey =
            ProductPreferenceKeyResolver.resolve(productName)

        val learnedPreference =
            database
                .productCategoryPreferenceDao()
                .getPreference(productKey)

// PP-155 TEMPORARY CATEGORY DIAGNOSTIC
        if (productName.contains("ZYMIL", ignoreCase = true)) {
            Log.d(
                "PantryPalCategory",
                "Product=$productName | " +
                        "preferenceKey=$productKey | " +
                        "savedCategory=${learnedPreference?.category} | " +
                        "resolverCategory=${HouseholdCategoryResolver.resolve(productName)}"
            )
        }

        if (learnedPreference != null) {

            return try {

                HouseholdCategory.valueOf(
                    learnedPreference.category
                )

            } catch (_: IllegalArgumentException) {

                HouseholdCategoryResolver.resolve(
                    productName
                )
            }
        }

        return HouseholdCategoryResolver.resolve(productName).let { resolved ->
            if (resolved == HouseholdCategory.UNKNOWN) HouseholdCategory.FOOD else resolved
        }
    }


// ============================================================
// PRODUCT CLASSIFICATION - SAVE LEARNED CATEGORY
// ============================================================

    private suspend fun saveCategoryPreference(
        productName: String,
        category: HouseholdCategory
    ) {

        val productKey =
            ProductPreferenceKeyResolver.resolve(productName)

        database
            .productCategoryPreferenceDao()
            .savePreference(
                ProductCategoryPreferenceEntity(
                    productKey = productKey,
                    originalName = productName,
                    category = category.name,
                    lastUpdated = System.currentTimeMillis()
                )
            )
    }
    private suspend fun addOrUpdateInventoryItem(
        productName: String,
        quantity: Int = 1,
        barcode: String = "",
        purchaseDate: LocalDate? = null,
        explicitExpiry: String? = null,
        explicitLocation: String? = null
    ): Int {

        val dao =
            database.productDao()

        val knowledge =
            ProductKnowledgeResolver.resolve(productName)

        val resolvedLocation =
            explicitLocation
                ?: resolveStorageLocation(productName)

        val baseDate =
            purchaseDate ?: LocalDate.now()

// ============================================================
// EXPIRY - ADJUST ESTIMATE FOR STORAGE LOCATION
// ============================================================

        val suggestedShelfLifeDays =
            when (resolvedLocation) {

                "Freezer" ->
                    maxOf(
                        knowledge.suggestedShelfLifeDays,
                        365
                    )

                else ->
                    knowledge.suggestedShelfLifeDays
            }

        val resolvedExpiry =
            explicitExpiry
                ?: baseDate
                    .plusDays(
                        suggestedShelfLifeDays.toLong()
                    )
                    .toString()

        val existing =
            if (barcode.isNotBlank()) {

                dao.getProductByBarcodeAndExpiry(
                    barcode,
                    resolvedExpiry
                )

            } else {

                dao.getProductByName(productName)
            }


        return if (existing != null) {


            dao.updateQuantityById(
                existing.id,
                existing.quantity + quantity
            )

            dao.updateLocationById(
                existing.id,
                resolvedLocation
            )

            // A receipt import has a purchase date, so its PantryPal-generated
            // expiry should be refreshed from that purchase date.
            if (purchaseDate != null && explicitExpiry == null) {

                dao.updateExpiryDateById(
                    existing.id,
                    resolvedExpiry
                )
            }
            existing.id

        } else {

            val newProduct =
                ProductEntity(
                    barcode = barcode,
                    itemName = productName,
                    quantity = quantity,
                    lastScanned = System.currentTimeMillis(),
                    expiryDate = resolvedExpiry,
                    location = resolvedLocation
                )

            dao.insertProduct(newProduct).toInt()
        }
    }
    private fun generateReceiptFingerprint(
        rawText: String
    ): String {

        val normalizedText =
            rawText
                .trim()
                .replace(Regex("\\s+"), " ")
                .lowercase()

        val digest =
            java.security.MessageDigest
                .getInstance("SHA-256")
                .digest(
                    normalizedText.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return digest.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }
            private fun refreshProducts() {
        lifecycleScope.launch {
            products.clear()
            products.addAll(database.productDao().getAllProductsAlphabetical())
        }
    }
    private fun expiryStatus(expiryDate: String?): String {
        val cleanedDate = expiryDate?.trim()

        if (cleanedDate.isNullOrBlank()) {
            return "No expiry set"
        }
        return try {
            val today = LocalDate.now()
            val expiry = LocalDate.parse(cleanedDate)
            val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiry)

            when {
                daysUntilExpiry < 0 -> "🔴 Expired"
                daysUntilExpiry <= 7 -> "🟠 Expiring soon"
                else -> "🟢 Fresh"
            }
        } catch (e: Exception) {
            "⚠️ Invalid date: '${cleanedDate}'"
        }
    }
    private fun exportInventoryCsv() {
        lifecycleScope.launch {
            val items = database.productDao().getAllProductsAlphabetical()

            val csv = buildString {
                appendLine("Item Name,Barcode,Quantity,Expiry Date,Location,Last Scanned")

                items.forEach { item ->
                    appendLine(
                        listOf(
                            item.itemName,
                            item.barcode,
                            item.quantity.toString(),
                            item.expiryDate ?: "",
                            item.location,
                            item.lastScanned.toString()
                        ).joinToString(",") { value ->
                            "\"${value.replace("\"", "\"\"")}\""
                        }
                    )
                }
            }

            val file = File(cacheDir, "food_inventory_backup.csv")
            file.writeText(csv)

            val uri = FileProvider.getUriForFile(
                this@MainActivity,
                "${packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Export inventory CSV"))
        }
    }
    private fun expiryColor(expiryDate: String?): Color {
        if (expiryDate.isNullOrBlank()) {
            return Color.Gray
        }

        return try {
            val today = LocalDate.now()
            val expiry = LocalDate.parse(expiryDate)
            val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiry)

            when {
                daysUntilExpiry < 0 -> Color.Red
                daysUntilExpiry <= 7 -> Color(0xFFFFA500) // orange
                else -> Color(0xFF4CAF50) // green
            }
        } catch (e: Exception) {
            Color.Gray
        }
    }
    // ============================================================
// RECEIPTS - REFRESH RECEIPT LIST
// ============================================================

    private suspend fun refreshReceipts() {

        val receipts =
            database
                .receiptDao()
                .getAllReceipts()

        receiptItems.value =
            receipts.sortedWith(
                compareByDescending<ReceiptEntity> { receipt ->
                    parseReceiptDateForSorting(
                        receipt.receiptDate
                    )
                }
                    .thenBy { receipt ->

                        val storeName =
                            receipt.storeName
                                ?.trim()
                                .orEmpty()

                        if (
                            storeName.isBlank() ||
                            storeName.equals(
                                "Unknown",
                                ignoreCase = true
                            )
                        ) {
                            1
                        } else {
                            0
                        }
                    }
                    .thenBy { receipt ->
                        receipt.storeName
                            ?.trim()
                            ?.lowercase(Locale.ENGLISH)
                            .orEmpty()
                    }
                    .thenByDescending { receipt ->
                        receipt.createdAt
                    }
            )
    }

    private val receiptDisplayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM, uuuu", Locale.ENGLISH)

    private fun formatReceiptDate(value: String?): String =
        parseReceiptDateForSorting(value)?.format(receiptDisplayFormatter)
            ?: value?.takeIf { it.isNotBlank() }
            ?: "Unknown"

    private fun parseReceiptDateForSorting(
        value: String?
    ): LocalDate? {

        if (value.isNullOrBlank()) {
            return null
        }

        val originalValue =
            value.trim()

        val normalisedValue =
            originalValue.replace(
                Regex("""(?i)\bSept\b"""),
                "Sep"
            )

        // Australian short-year dates (e.g. 22/10/26) from paper receipts.
        // Interpret 00-99 as 2000-2099, matching existing compact OCR dates.
        val shortNumeric = Regex("""^(\d{1,2})[/.-](\d{1,2})[/.-](\d{2})$""")
            .matchEntire(normalisedValue)
        if (shortNumeric != null) {
            val day = shortNumeric.groupValues[1].toInt()
            val month = shortNumeric.groupValues[2].toInt()
            val year = 2000 + shortNumeric.groupValues[3].toInt()
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }

        // Existing saved values and numeric/manual entry.
        val supportedFormats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM, uuuu", Locale.ENGLISH)
        )
        supportedFormats.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(normalisedValue, formatter) }.getOrNull()
        }?.let { return it }

        val shortMonthDate =
            runCatching {
                LocalDate.parse(
                    normalisedValue,
                    DateTimeFormatter.ofPattern(
                        "d MMM yyyy",
                        Locale.ENGLISH
                    )
                )
            }.getOrNull()

        if (shortMonthDate != null) {
            return shortMonthDate
        }

        val longMonthDate =
            runCatching {
                LocalDate.parse(
                    normalisedValue,
                    DateTimeFormatter.ofPattern(
                        "d MMMM yyyy",
                        Locale.ENGLISH
                    )
                )
            }.getOrNull()

        if (longMonthDate != null) {
            return longMonthDate
        }

        val compactValue =
            originalValue
                .replace(" ", "")
                .uppercase(Locale.ENGLISH)

        val compactMatch =
            Regex("""^(\d{1,2})([A-Z]{3})(\d{2})$""")
                .matchEntire(compactValue)

        if (compactMatch != null) {

            val day =
                compactMatch.groupValues[1]
                    .toIntOrNull()

            val month =
                when (compactMatch.groupValues[2]) {
                    "JAN" -> 1
                    "FEB" -> 2
                    "MAR" -> 3
                    "APR" -> 4
                    "MAY" -> 5
                    "JUN" -> 6
                    "JUL" -> 7
                    "AUG" -> 8
                    "SEP" -> 9
                    "OCT" -> 10
                    "NOV" -> 11
                    "DEC" -> 12
                    else -> null
                }

            val year =
                compactMatch.groupValues[3]
                    .toIntOrNull()
                    ?.plus(2000)

            if (
                day != null &&
                month != null &&
                year != null
            ) {
                return runCatching {
                    LocalDate.of(
                        year,
                        month,
                        day
                    )
                }.getOrNull()
            }
        }

        return null
    }

    private suspend fun refreshShoppingList() {
        // PP-150: shopping entries represent explicit purchasing intent only.
        // Legacy AUTO entries are discarded; manually requested items are retained.
        database.shoppingDao().clearAutoItems()
        val shoppingItems = database.shoppingDao().getAllItems()
        shoppingListEntities.value = shoppingItems
        shoppingListItems.value = shoppingItems.map { it.description }.toMutableList()
    }

    private fun readPdfText(uri: Uri): String {

        return try {

            contentResolver.openInputStream(uri)?.use { input ->

                PDDocument.load(input).use { document ->

                    PDFTextStripper().getText(document)

                }

            } ?: ""

        } catch (e: Exception) {

            e.printStackTrace()

            ""

        }
    }



    private suspend fun loadShoppingItems(): List<ShoppingItemEntity> {

        return database
            .shoppingDao()
            .getAllItems()
    }
    private suspend fun generateShoppingList(): List<String> {

        val items =
            database.productDao().getAllProductsAlphabetical()

        val today =
            java.time.LocalDate.now()

        val results =
            mutableListOf<String>()

        // Group separate inventory rows belonging to the same product.
        // PantryPal may have multiple rows for a product because batches
        // can have different expiry dates.
        val productGroups =
            items.groupBy {
                it.itemName
                    .trim()
                    .lowercase()
            }

        productGroups.forEach { (_, productRows) ->

            val productName =
                productRows.first().itemName.trim()

            // ------------------------------------------------------------
            // Determine whether any physical stock remains.
            // ------------------------------------------------------------

            val stockedRows =
                productRows.filter {
                    it.quantity > 0
                }

            if (stockedRows.isEmpty()) {

                results.add(
                    "$productName — Out of stock"
                )

            } else {

                var hasFreshStock = false
                var hasExpiringSoonStock = false
                var hasExpiredStock = false

                stockedRows.forEach { item ->

                    val expiry =
                        item.expiryDate?.trim()

                    if (expiry.isNullOrBlank()) {

                        // Stock exists and has no usable expiry warning.
                        // Treat it as available stock.
                        hasFreshStock = true

                    } else {

                        try {

                            val expiryDate =
                                java.time.LocalDate.parse(expiry)

                            val days =
                                java.time.temporal.ChronoUnit.DAYS
                                    .between(
                                        today,
                                        expiryDate
                                    )

                            when {

                                days < 0 ->
                                    hasExpiredStock = true

                                days <= 7 ->
                                    hasExpiringSoonStock = true

                                else ->
                                    hasFreshStock = true
                            }

                        } catch (_: Exception) {

                            // If PantryPal cannot interpret the expiry,
                            // don't incorrectly tell the user to replace
                            // stock that still exists.
                            hasFreshStock = true
                        }
                    }
                }

                // --------------------------------------------------------
                // One shopping-list decision per product.
                // Fresh usable stock takes precedence.
                // --------------------------------------------------------

                when {

                    hasFreshStock -> {
                        // Usable stock remains.
                        // Nothing needs adding to the shopping list.
                    }

                    hasExpiringSoonStock -> {
                        results.add(
                            "$productName — Expiring soon"
                        )
                    }

                    hasExpiredStock -> {
                        results.add(
                            "$productName — Expired"
                        )
                    }
                }
            }
        }

        return results.sorted()
    }
    private suspend fun getExpirySummary(): Pair<Int, Int> {
        val items = database.productDao().getAllProductsAlphabetical()

        var expiringSoon = 0
        var expired = 0

        val today = java.time.LocalDate.now()

        items.forEach { item ->
            // Depleted products are retained for history/shopping, not expiry warnings.
            if (item.quantity <= 0) return@forEach
            val date = item.expiryDate?.trim()

            if (!date.isNullOrBlank()) {
                try {
                    val expiry = java.time.LocalDate.parse(date)
                    val days = java.time.temporal.ChronoUnit.DAYS.between(today, expiry)

                    when {
                        days < 0 -> expired++
                        days <= 7 -> expiringSoon++
                    }
                } catch (_: Exception) {
                    // ignore invalid
                }
            }
        }

        return Pair(expiringSoon, expired)
    }
    private fun showExpiryDatePicker() {
        val calendar = Calendar.getInstance()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "%04d-%02d-%02d".format(
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )

                expiryInput.value = formattedDate
            },
            year,
            month,
            day
        ).show()
    }

}