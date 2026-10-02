package com.example.myapplication

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
import com.example.myapplication.data.BarcodeNameEntity
import androidx.compose.material3.Checkbox
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.util.Log
import com.example.myapplication.data.ShoppingItemEntity
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
    private var searchText = mutableStateOf("")
    private var showClearConfirm = mutableStateOf(false)
    private var showRebuildInventoryConfirm = mutableStateOf(false)

    private var selectedLocation = mutableStateOf("Pantry")

    private lateinit var database: AppDatabase

    private lateinit var prefs: SharedPreferences
    private val productLookup = ProductLookup()

    private var deleteQuantityInput = mutableStateOf("1")

    private var shoppingListDialogVisible = mutableStateOf(false)

    private var receiptItems =
        mutableStateOf<List<ReceiptEntity>>(emptyList())
    private var shoppingListItems =
        mutableStateOf(mutableListOf<String>())
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
                // RECEIPT IMPORT - SAVE RECEIPT AND ITEMS
                // ============================================================

                val receipt =
                    ReceiptEntity(
                        storeName =
                            parsedReceipt.storeName,

                        receiptDate =
                            parsedReceipt.receiptDate,

                        totalAmount =
                            parsedReceipt.totalAmount,

                        rawText =
                            text,

                        receiptNumber =
                            parsedReceipt.receiptNumber,

                        fingerprint =
                            fingerprint
                    )

                val receiptId =
                    database
                        .receiptDao()
                        .insertReceipt(receipt)

                lastImportedReceiptId.longValue =
                    receiptId

                if (
                    parsedReceipt
                        .structuredItems
                        .isNotEmpty()
                ) {

                    val entities =
                        parsedReceipt
                            .structuredItems
                            .map { item ->

                                ReceiptItemEntity(
                                    receiptId =
                                        receiptId,

                                    retailer =
                                        parsedReceipt.storeName,

                                    receiptDate =
                                        parsedReceipt.receiptDate,

                                    productName =
                                        item.name,

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

                    database
                        .receiptItemDao()
                        .insertAll(entities)

                    android.util.Log.e(
                        "HouseholdCategoryDebug",
                        "IMPORT TEST: retailer=${parsedReceipt.storeName}, " +
                                "products=${parsedReceipt.products.size}, " +
                                "structuredItems=${parsedReceipt.structuredItems.size}"
                    )

                    // ========================================================
                    // RECEIPT IMPORT - UPDATE INVENTORY
                    // ========================================================

                    parsedReceipt
                        .structuredItems
                        .forEach { item ->

                            val householdCategory =
                                resolveHouseholdCategory(
                                    item.name
                                )

                            // Temporary logging
                            android.util.Log.e(
                                "HouseholdCategoryDebug",
                                "COLES TEST: ${item.name} -> $householdCategory"
                            )

                            if (
                                householdCategory ==
                                HouseholdCategory.FOOD
                            ) {

                                val inventoryQuantity =
                                    when {

                                        item.unit == "kg" ->
                                            1

                                        item.quantity != null ->
                                            item.quantity
                                                .toInt()
                                                .coerceAtLeast(1)

                                        else ->
                                            1
                                    }

                                val purchaseDate =
                                    parsedReceipt
                                        .receiptDate
                                        ?.let { dateText ->

                                            runCatching {

                                                LocalDate.parse(
                                                    dateText,
                                                    java.time.format
                                                        .DateTimeFormatter
                                                        .ofPattern(
                                                            "d MMM yyyy"
                                                        )
                                                )

                                            }.getOrNull()
                                        }

                                addOrUpdateInventoryItem(
                                    productName =
                                        item.name,

                                    quantity =
                                        inventoryQuantity,

                                    barcode =
                                        "",

                                    purchaseDate =
                                        purchaseDate
                                )
                            }
                        }

                    showImportReview.value =
                        true
                }

                refreshProducts()

                expirySummary.value =
                    getExpirySummary()

                refreshReceipts()

                Toast.makeText(
                    this@MainActivity,
                    "Receipt imported successfully.",
                    Toast.LENGTH_LONG
                ).show()

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

                        dao.clearAllProducts()

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

    @Composable
    private fun ImportReviewScreen() {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .padding(bottom = 80.dp)
        ) {

            // ============================================================
            // IMPORT REVIEW - PANTRYPAL HEADER
            // ============================================================

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
                text = "Review imported items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Check PantryPal's suggestions before continuing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                items(
                    importedItemsForReview.value
                ) { reviewItem ->

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = reviewItem.productName,
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleSmall,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

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

                            if (reviewItem.category == HouseholdCategory.UNKNOWN) {

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
                                        Text("Food")
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
                                            text = "Non-food",
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

                        if (
                            parsedReceipt != null &&
                            !rawText.isNullOrBlank()
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
                                            parsedReceipt.storeName,

                                        receiptDate =
                                            parsedReceipt.receiptDate,

                                        totalAmount =
                                            parsedReceipt.totalAmount,

                                        rawText =
                                            rawText,

                                        receiptNumber =
                                            parsedReceipt.receiptNumber,

                                        fingerprint =
                                            fingerprint
                                    )

                                val receiptId =
                                    database
                                        .receiptDao()
                                        .insertReceipt(receipt)

                                lastImportedReceiptId.longValue =
                                    receiptId

                                // ============================================================
                                // OCR IMPORT - SAVE REVIEWED RECEIPT ITEMS
                                // ============================================================

                                val reviewedItems =
                                    importedItemsForReview.value

                                if (reviewedItems.isNotEmpty()) {

                                    val entities =
                                        reviewedItems.map { item ->

                                            ReceiptItemEntity(
                                                receiptId =
                                                    receiptId,

                                                retailer =
                                                    parsedReceipt.storeName,

                                                receiptDate =
                                                    parsedReceipt.receiptDate,

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

                                    database
                                        .receiptItemDao()
                                        .insertAll(entities)
                                }

                                // ============================================================
                                // OCR IMPORT - UPDATE INVENTORY FROM REVIEWED FOOD ITEMS
                                // ============================================================

                                reviewedItems
                                    .filter { item ->
                                        item.category ==
                                                HouseholdCategory.FOOD
                                    }
                                    .forEach { item ->

                                        val inventoryQuantity =
                                            when {

                                                item.unit == "kg" ->
                                                    1

                                                item.quantity != null ->
                                                    item.quantity
                                                        .toInt()
                                                        .coerceAtLeast(1)

                                                else ->
                                                    1
                                            }

                                        // Support both existing PDF dates such as
                                        // "16 Sep 2026" and OCR dates such as "16SEP26".
                                        val purchaseDate =
                                            item.receiptDate
                                                ?.let { dateText ->

                                                    runCatching {

                                                        LocalDate.parse(
                                                            dateText,
                                                            java.time.format
                                                                .DateTimeFormatter
                                                                .ofPattern(
                                                                    "d MMM yyyy",
                                                                    java.util.Locale.ENGLISH
                                                                )
                                                        )

                                                    }.getOrNull()
                                                        ?: runCatching {

                                                            LocalDate.parse(
                                                                dateText.uppercase(
                                                                    java.util.Locale.ENGLISH
                                                                ),
                                                                java.time.format
                                                                    .DateTimeFormatter
                                                                    .ofPattern(
                                                                        "dMMMyy",
                                                                        java.util.Locale.ENGLISH
                                                                    )
                                                            )

                                                        }.getOrNull()
                                                }

                                        addOrUpdateInventoryItem(
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

        if (intent?.action != Intent.ACTION_SEND) {
            return
        }

        val mimeType =
            intent.type ?: return

        val sharedUri =
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

        if (sharedUri == null) {

            android.util.Log.e(
                "PantryPalSHARE",
                "ACTION_SEND received but no URI was supplied"
            )

            return
        }

        android.util.Log.e(
            "PantryPalSHARE",
            "Received shared receipt | " +
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

                    pendingOcrReceipt.value = parsedReceipt
                    pendingOcrRawText.value = normalisedText

                    lifecycleScope.launch {

                        importedItemsForReview.value =
                            parsedReceipt
                                .structuredItems
                                .map { receiptItem ->

                                    val category =
                                        resolveHouseholdCategory(
                                            receiptItem.name
                                        )

                                    ImportReviewItem(
                                        productName =
                                            receiptItem.name,

                                        location =
                                            resolveStorageLocation(
                                                receiptItem.name
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            val shoppingItems =
                database.shoppingDao().getAllItems()

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

                            "ANALYTICS" ->
                                AnalyticsScreen()

                            "IMPORT_REVIEW" ->
                                ImportReviewScreen()

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

                                    if (
                                        androidx.core.content.ContextCompat
                                            .checkSelfPermission(
                                                this@MainActivity,
                                                android.Manifest.permission.CAMERA
                                            ) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {

                                        launchReceiptCamera()

                                    } else {

                                        receiptCameraPermissionLauncher.launch(
                                            android.Manifest.permission.CAMERA
                                        )
                                    }
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
                                "${products.count { it.quantity > 0 }} items in stock",
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

                                    database.productDao().clearAllProducts()
                                    database.shoppingDao().clearAll()

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
                    onDismissRequest = {
                        showRebuildInventoryConfirm.value = false
                    },

                    title = {
                        Text("Rebuild inventory?")
                    },

                    text = {

                        Text(
                            "PantryPal will rebuild your inventory using food " +
                                    "products found in your retained receipt history.\n\n" +
                                    "Recovered quantities will be set to 1 because " +
                                    "receipt history cannot determine what has since " +
                                    "been consumed or removed.\n\n" +
                                    "Some recovered products may therefore no longer " +
                                    "be in stock."
                        )
                    },

                    confirmButton = {

                        Button(
                            onClick = {

                                lifecycleScope.launch {

                                    val receiptItems =
                                        database
                                            .receiptItemDao()
                                            .getAllItems()

                                    val latestFoodItems =
                                        receiptItems
                                            .asReversed()
                                            .filter { receiptItem ->

                                                resolveHouseholdCategory(
                                                    receiptItem.productName
                                                ) == HouseholdCategory.FOOD
                                            }
                                            .distinctBy { receiptItem ->

                                                receiptItem
                                                    .productName
                                                    .trim()
                                                    .lowercase()
                                            }

                                    latestFoodItems.forEach { receiptItem ->

                                        val purchaseDate =
                                            receiptItem.receiptDate?.let { dateText ->

                                                runCatching {

                                                    LocalDate.parse(
                                                        dateText,
                                                        java.time.format.DateTimeFormatter
                                                            .ofPattern("d MMM yyyy")
                                                    )
                                                }.getOrNull()
                                            }

                                        addOrUpdateInventoryItem(
                                            productName =
                                                receiptItem.productName,

                                            quantity = 1,

                                            barcode = "",

                                            purchaseDate =
                                                purchaseDate
                                        )
                                    }

                                    refreshProducts()

                                    expirySummary.value =
                                        getExpirySummary()

                                    showRebuildInventoryConfirm.value = false

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Inventory rebuilt from receipt history.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        ) {
                            Text("Rebuild")
                        }
                    },

                    dismissButton = {

                        Button(
                            onClick = {
                                showRebuildInventoryConfirm.value = false
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
    private fun InventoryScreen() {
        val filteredProducts = products.filter { product ->

            product.itemName.contains(
                searchText.value,
                ignoreCase = true
            ) ||
                    product.barcode.contains(
                        searchText.value,
                        ignoreCase = true
                    )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            // ============================================================
// INVENTORY - PANTRYPAL HEADER
// ============================================================

            Text(
                text = "PantryPal",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Inventory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Search and manage your household items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = {
                    currentScreen.value = "HOME"
                    searchText.value = ""
                }
            ) {
                Text(
                    text = "‹ Back to Home",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = searchText.value,
                onValueChange = {
                    searchText.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Search inventory",
                        maxLines = 1
                    )
                },
                trailingIcon = {

                    if (searchText.value.isNotBlank()) {

                        IconButton(
                            onClick = {
                                searchText.value = ""
                            }
                        ) {
                            Text(
                                text = "×",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            )

            Text(
                text = "Matches: ${filteredProducts.size}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))


            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(filteredProducts) { product ->

                    val expiryText =
                        product.expiryDate ?: "Not set"

                    val statusText =
                        expiryStatus(product.expiryDate)

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
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = product.itemName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Column {

                                    Text(
                                        text = "Location",
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = product.location,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {

                                    Text(
                                        text = "Quantity",
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = product.quantity.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Expiry: $expiryText",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (statusText.isNotBlank()) {

                                Spacer(
                                    modifier = Modifier.height(2.dp)
                                )

                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (
                                            statusText.contains(
                                                "expired",
                                                ignoreCase = true
                                            )
                                        ) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                )
                            }

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

        var showDeleteConfirmation by remember {
            mutableStateOf(false)
        }

        if (product == null) {

            currentScreen.value = "INVENTORY"
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            // ============================================================
            // PRODUCT DETAIL - PANTRYPAL HEADER
            // ============================================================

            Text(
                text = "PantryPal",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Product details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "View and manage this inventory item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    editingProduct.value = null
                    navigateBack()
                }
            ) {
                Text(
                    text = "‹ Back to Inventory",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ============================================================
// PRODUCT DETAILS CARD
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
                        text = "Product",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Product name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = editNameInput.value,

                        onValueChange = {
                            editNameInput.value = it
                        },

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {

                            lifecycleScope.launch {

                                database.productDao().updateProductNameById(
                                    product.id,
                                    editNameInput.value
                                )

                                refreshProducts()
                            }
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Product Name")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

// ============================================================
// QUANTITY CARD
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
                        text = "Quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Current quantity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = product.quantity.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = deleteQuantityInput.value,

                        onValueChange = {
                            deleteQuantityInput.value = it
                        },

                        label = {
                            Text("Quantity")
                        },

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {

                            val amount =
                                deleteQuantityInput.value.toIntOrNull() ?: 0

                            lifecycleScope.launch {

                                database.productDao().updateQuantityById(
                                    product.id,
                                    amount
                                )

                                editingProduct.value =
                                    product.copy(
                                        quantity = amount
                                    )

                                refreshProducts()

                                expirySummary.value =
                                    getExpirySummary()
                            }
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Set Quantity")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {

                            val amount =
                                deleteQuantityInput.value.toIntOrNull() ?: 1

                            lifecycleScope.launch {

                                val newQuantity =
                                    if (product.quantity <= amount) {
                                        0
                                    } else {
                                        product.quantity - amount
                                    }

                                database.productDao()
                                    .updateQuantityById(
                                        product.id,
                                        newQuantity
                                    )

                                editingProduct.value =
                                    product.copy(
                                        quantity = newQuantity
                                    )

                                refreshProducts()

                                expirySummary.value =
                                    getExpirySummary()
                                expirySummary.value =
                                    getExpirySummary()
                            }
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove Quantity")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Text(
                text = "Keep track of items that need replacing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

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

                    Text(
                        text = "Add something to your shopping list manually",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(16.dp))


            Spacer(modifier = Modifier.height(16.dp))

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

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = productName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

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

                        var expanded by remember {

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

                            onDelete = {

                                lifecycleScope.launch {

                                    database
                                        .receiptItemDao()
                                        .deleteForReceipt(receipt.id.toLong())

                                    database
                                        .receiptDao()
                                        .deleteReceiptById(receipt.id)

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
                        Text("Delete all imported receipts?")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                lifecycleScope.launch {

                                    database.receiptItemDao().deleteAll()

                                    database.receiptDao().deleteAllReceipts()

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

                    if (
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.CAMERA
                        ) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {

                        launchReceiptCamera()

                    } else {

                        receiptCameraPermissionLauncher.launch(
                            android.Manifest.permission.CAMERA
                        )
                    }
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

                                selectedReceiptIds.value.forEach { receiptId ->

                                    database
                                        .receiptItemDao()
                                        .deleteForReceipt(receiptId.toLong())

                                    database
                                        .receiptDao()
                                        .deleteReceiptById(receiptId)
                                }

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

                        onDelete = {
                            // Archived receipts are
                            // view-only for now.
                        },

                        selectionMode = false,

                        selected = false,

                        onSelectionChange = {
                            // No selection mode
                            // in Archive V1.
                        }
                    )
                }
            }
        }
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

    @Composable
    private fun PriceHistoryScreen() {

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
                text = "Price history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Compare previous purchases and prices",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = date,
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
    private fun copyShoppingListToClipboard() {

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager

        val clip = ClipData.newPlainText(
            "PantryPal Shopping List",
            buildShoppingListText()
        )

        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            this,
            "Shopping list copied",
            Toast.LENGTH_SHORT
        ).show()
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

    private val pendingOcrReceipt =
        mutableStateOf<ParsedReceipt?>(null)

    private val pendingOcrRawText =
        mutableStateOf<String?>(null)

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

            shoppingListItems.value.forEach { item ->

                val checked =
                    checkedShoppingItems.value.contains(item)

                val marker =
                    if (checked) "☑" else "☐"

                append("$marker $item\n")
            }
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

        return HouseholdCategoryResolver.resolve(
            productName
        )
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
    ) {

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

        if (existing != null) {

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

            dao.insertProduct(newProduct)
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

        // Generate the shopping requirements from CURRENT inventory.
        val generatedItems =
            generateShoppingList()

        // AUTO entries are derived from inventory.
        // Remove the old derived entries before rebuilding them.
        // MANUAL entries are preserved.
        database.shoppingDao().clearAutoItems()

        generatedItems.forEach { generatedItem ->

            val normalisedDescription =
                generatedItem
                    .lowercase()
                    .trim()

            val existing =
                database.shoppingDao()
                    .findByDescription(
                        normalisedDescription
                    )

            // A MANUAL item with the same description may already exist.
            // In that case, do not create a duplicate AUTO entry.
            if (existing == null) {

                database.shoppingDao().insertItem(

                    ShoppingItemEntity(

                        description =
                            generatedItem,

                        normalisedDescription =
                            normalisedDescription,

                        source = "AUTO"
                    )
                )
            }
        }

        val shoppingItems =
            database.shoppingDao().getAllItems()

        shoppingListItems.value =
            shoppingItems
                .map { it.description }
                .toMutableList()
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