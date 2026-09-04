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
    private var searchText = mutableStateOf("")
    private var showClearConfirm = mutableStateOf(false)

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

    private val importReceiptLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    try {
                        val text = readPdfText(uri)

                        if (text.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                "No readable text was found in this receipt.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        val parsedReceipt = ReceiptParser.parse(text)
                        val fingerprint = generateReceiptFingerprint(text)

                        // Primary duplicate protection: the normalized PDF text fingerprint.
                        val receiptWithSameFingerprint =
                            database.receiptDao().getReceiptByFingerprint(fingerprint)

                        if (receiptWithSameFingerprint != null) {
                            Toast.makeText(
                                this@MainActivity,
                                "This receipt has already been imported.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        // Secondary duplicate protection where the retailer supplied a receipt number.
                        val receiptWithSameNumber =
                            parsedReceipt.receiptNumber?.let { receiptNumber ->
                                if (receiptNumber.isBlank()) {
                                    null
                                } else {
                                    database.receiptDao().getReceiptByNumber(receiptNumber)
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
                                storeName = parsedReceipt.storeName,
                                receiptDate = parsedReceipt.receiptDate,
                                totalAmount = parsedReceipt.totalAmount,
                                rawText = text,
                                receiptNumber = parsedReceipt.receiptNumber,
                                fingerprint = fingerprint
                            )

                        val receiptId =
                            database.receiptDao().insertReceipt(receipt)

                        lastImportedReceiptId.longValue = receiptId

                        if (parsedReceipt.structuredItems.isNotEmpty()) {
                            val entities =
                                parsedReceipt.structuredItems.map { item ->
                                    ReceiptItemEntity(
                                        receiptId = receiptId,
                                        retailer = parsedReceipt.storeName,
                                        receiptDate = parsedReceipt.receiptDate,
                                        productName = item.name,
                                        quantity = item.quantity,
                                        unit = item.unit,
                                        unitPrice = item.unitPrice,
                                        totalPrice = item.totalPrice
                                    )
                                }

                            database.receiptItemDao().insertAll(entities)

                            android.util.Log.e(
                                "HouseholdCategoryDebug",
                                "IMPORT TEST: retailer=${parsedReceipt.storeName}, " +
                                        "products=${parsedReceipt.products.size}, " +
                                        "structuredItems=${parsedReceipt.structuredItems.size}"
                            )

                            // ============================================================
                            // RECEIPT IMPORT - UPDATE INVENTORY
                            // ============================================================
                            parsedReceipt.structuredItems.forEach { item ->


                                val householdCategory =
                                    HouseholdCategoryResolver.resolve(item.name)

                                // Temporary Logging
                                android.util.Log.e(
                                    "HouseholdCategoryDebug",
                                    "COLES TEST: ${item.name} -> $householdCategory"
                                )

                                if (householdCategory == HouseholdCategory.FOOD) {

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
                                        parsedReceipt.receiptDate
                                            ?.let { dateText ->

                                                runCatching {
                                                    LocalDate.parse(
                                                        dateText,
                                                        java.time.format.DateTimeFormatter
                                                            .ofPattern("d MMM yyyy")
                                                    )
                                                }.getOrNull()
                                            }

                                    addOrUpdateInventoryItem(
                                        productName = item.name,
                                        quantity = inventoryQuantity,
                                        barcode = "",
                                        purchaseDate = purchaseDate
                                    )
                                }
                            }
                            showImportReview.value = true
                        }

                        refreshProducts()
                        expirySummary.value = getExpirySummary()
                        refreshReceipts()

                        Toast.makeText(
                            this@MainActivity,
                            "Receipt imported successfully.",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Log.e("PantryPalReceipt", "Receipt import failed", e)
                        Toast.makeText(
                            this@MainActivity,
                            "Receipt import failed: ${e.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
                .padding(16.dp)
        ) {

            Text(
                text = "Review Imported Items",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Check the storage locations PantryPal assigned.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(16.dp)
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
                        shape = RoundedCornerShape(12.dp),
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    ) {

                        Column(
                            modifier = Modifier.padding(14.dp)
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

                                Text(
                                    text =
                                        "Location: ${reviewItem.location}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium
                                )

                                TextButton(
                                    onClick = {
                                        locationEditItem.value = reviewItem
                                        locationEditSelection.value = reviewItem.location
                                    }
                                ) {
                                    Text("Change")
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

                                    lifecycleScope.launch {

                                        saveLocationCorrection(
                                            productName =
                                                itemToUpdate.productName,
                                            location =
                                                newLocation
                                        )

                                        // ============================================================
                                        // IMPORT REVIEW - REFRESH CORRECTED LOCATION
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

                                        refreshProducts()

                                        locationEditItem.value = null
                                    }
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
        refreshProducts()

        // Temporary block
        lifecycleScope.launch {

            saveTestLocationPreference(
                productName = "GOUDA CHEESE SLICES 200GRAM",
                location = "Pantry"
            )
        }
        // To here
        lifecycleScope.launch {

            val shoppingItems =
                database.shoppingDao().getAllItems()

            shoppingListItems.value =
                shoppingItems
                    .map { it.description }
                    .toMutableList()
        }
        PDFBoxResourceLoader.init(applicationContext)
        lifecycleScope.launch {
            expirySummary.value = getExpirySummary()

            setContent {

                MyApplicationTheme {

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

                                                        HouseholdCategoryResolver.resolve(
                                                            receiptItem.productName
                                                        ) == HouseholdCategory.FOOD
                                                    }
                                                    .map { receiptItem ->

                                                        ImportReviewItem(
                                                            productName =
                                                                receiptItem.productName,

                                                            location =
                                                                resolveStorageLocation(
                                                                    receiptItem.productName
                                                                )
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
        private fun HomeScreen() {
            val barcodeFocusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

        var quickAddExpanded by remember {
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

            // Primary receipt workflow
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        importReceiptLauncher.launch("application/pdf")
                    },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
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
                        currentScreen.value = "INVENTORY"
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

// Shopping and receipts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = {

                        lifecycleScope.launch {

                            refreshShoppingList()

                            currentScreen.value = "SHOPPING"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Shopping List")
                }

                OutlinedButton(
                    onClick = {

                        lifecycleScope.launch {

                            refreshReceipts()

                            currentScreen.value = "RECEIPTS"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Receipts")
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
            Button(
                onClick = {

                    lifecycleScope.launch {

                        refreshShoppingList()

                        currentScreen.value = "SHOPPING"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Shopping List")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {

                    lifecycleScope.launch {

                        refreshReceipts()

                        currentScreen.value = "RECEIPTS"

                    }

                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Receipts")
            }


            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    refreshProducts()
                    currentScreen.value = "INVENTORY"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check Inventory")
            }
            Spacer(modifier = Modifier.height(12.dp))


            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        exportInventoryCsv()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export CSV")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        importCsvLauncher.launch("*/*")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Import CSV")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    showClearConfirm.value = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Database")
            }

            if (showClearConfirm.value) {
                AlertDialog(
                    onDismissRequest = {
                        showClearConfirm.value = false
                    },
                    title = {
                        Text("Clear database?")
                    },
                    text = {
                        Text("This will delete all stored inventory items.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                lifecycleScope.launch {
                                    database.productDao().clearAllProducts()
                                    database.shoppingDao().clearAll()
                                    refreshProducts()
                                    expirySummary.value = getExpirySummary()
                                    shoppingListItems.value = mutableListOf()
                                    showClearConfirm.value = false
                                }
                            }
                        ) {
                            Text("Clear")
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

            Text(
                text = "Inventory",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = searchText.value,
                onValueChange = {
                    searchText.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Search food or barcode")
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
            Button(
                onClick = {
                    currentScreen.value = "HOME"
                    searchText.value = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { product ->

                    val expiryText =
                        "[${product.expiryDate ?: "Not set"}]"

                    Text(
                        text = "${product.itemName} | " +
                                "Location: ${product.location} | " +
                                "Barcode: ${product.barcode} | " +
                                "Qty: ${product.quantity} | " +
                                "Expiry: $expiryText | ${
                                    expiryStatus(
                                        product.expiryDate
                                    )
                                }",
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .clickable {
                                editingProduct.value = product
                                editNameInput.value = product.itemName
                                deleteQuantityInput.value = "1"
                                currentScreen.value = "DETAIL"
                            }
                    )
                }
            }
        }


    }

    @Composable
    private fun ProductDetailScreen() {

        val product = editingProduct.value

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

            Text(
                text = "Product Detail",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Product Name")

            OutlinedTextField(
                value = editNameInput.value,

                onValueChange = {
                    editNameInput.value = it
                },

                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Current Quantity: ${product.quantity}")

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

                        refreshProducts()

                        expirySummary.value = getExpirySummary()
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set Quantity")
            }



            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {

                    lifecycleScope.launch {

                        database.productDao()
                            .deleteProductById(product.id)

                        refreshProducts()

                        editingProduct.value = null

                        currentScreen.value = "INVENTORY"
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Delete Product Permanently")
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {

                    val amount =
                        deleteQuantityInput.value.toIntOrNull() ?: 1

                    lifecycleScope.launch {

                        if (product.quantity <= amount) {

                            database.productDao()
                                .updateQuantityById(
                                    product.id,
                                    0
                                )

                        } else {

                            database.productDao()
                                .subtractQuantityById(
                                    product.id,
                                    amount
                                )
                        }

                        refreshProducts()

                        expirySummary.value =
                            getExpirySummary()
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove Quantity")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    editingProduct.value = null
                    currentScreen.value = "INVENTORY"
                },

                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Inventory")
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

            Text(
                text = "Shopping List",
                style = MaterialTheme.typography.headlineSmall
            )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                        value = manualShoppingItemInput.value,

                onValueChange = {
                    manualShoppingItemInput.value = it
                },
                label = {
                    Text("Add shopping item")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

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

                            if (existing != null) {




                            }

                            database.shoppingDao().insertItem(

                                ShoppingItemEntity(

                                    description = item,

                                    normalisedDescription = item
                                        .trim()
                                        .lowercase(),

                                    source = "MANUAL"
                                )
                            )

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


            Spacer(modifier = Modifier.height(16.dp))

            if (shoppingListItems.value.isEmpty()) {

                Text("No items currently need replacing.")

            } else {

                shoppingListItems.value.forEach { item ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Checkbox(
                            checked = checkedShoppingItems.value.contains(item),
                            onCheckedChange = { checked ->
                                checkedShoppingItems.value =
                                    if (checked) {
                                        checkedShoppingItems.value + item
                                    } else {
                                        checkedShoppingItems.value - item
                                    }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(item)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {

                    lifecycleScope.launch {

                        refreshShoppingList()

                        currentScreen.value = "SHOPPING"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Shopping List")
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {

                    lifecycleScope.launch {

                        refreshReceipts()

                        currentScreen.value = "RECEIPTS"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Receipts")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    currentScreen.value = "HOME"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
            }
        }
    }

    @Composable
    private fun ReceiptScreen() {

        android.util.Log.e(
            "PantryPalReceipt",
            "RECEIPT SCREEN OPENED"
        )

        Column(

            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)

        ) {

            Text(
                text = "Receipts",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

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
                        if (receiptSelectionMode.value)
                            "Cancel selection"
                        else
                            "Select receipt"
                    )
                }
            }
            if (
                receiptSelectionMode.value &&
                selectedReceiptIds.value.isNotEmpty()
            ) {

                Button(
                    onClick = {
                        showDeleteSelectedReceiptsDialog.value = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Delete Selected (${selectedReceiptIds.value.size})"
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )


                if (receiptItems.value.isEmpty()) {

                    Text("No receipts imported yet.")

                } else {

                    receiptItems.value.forEach { receipt ->
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

                        ReceiptCard(
                            receipt = receipt,
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

            Button(
                onClick = {
                    showClearReceiptsDialog.value = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Imported Receipts")
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

            Button(

                onClick = {

                    importReceiptLauncher.launch("application/pdf")

                }

            ) {

                Text("Import Receipt")

            }

            Spacer(

                modifier = Modifier.height(16.dp)

            )
            Button(
                onClick = {
                    currentScreen.value = "PRICE_HISTORY"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Price History")
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(

                onClick = {

                    currentScreen.value = "HOME"

                }

            )
            {

                Text("Back")

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
    private fun PriceHistoryScreen() {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {

            Text(
                text = "Price History",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(16.dp)
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
                    text = "Purchase History",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                priceHistoryResults.value.forEach { item ->

                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = item.retailer,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    item.receiptDate?.let { date ->

                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    item.unitPrice?.let { price ->

                        Text(
                            text = "$%.2f".format(price),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )
                }

            } else if (priceHistoryHasSearched.value) {

                Text(
                    text = "No price history found."
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = {
                    currentScreen.value = "HOME"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
            }
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
        location: String
    ) {

        val product =
            database
                .productDao()
                .getProductByName(productName)

        if (product != null) {
            database
                .productDao()
                .updateLocationById(
                    product.id,
                    location
                )
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
        val location: String
    )

    private val importedItemsForReview =
        mutableStateOf<List<ImportReviewItem>>(emptyList())

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
    private suspend fun addOrUpdateInventoryItem(
        productName: String,
        quantity: Int = 1,
        barcode: String = "",
        purchaseDate: LocalDate? = null,
        explicitExpiry: String? = null
    ) {

        val dao =
            database.productDao()

        val knowledge =
            ProductKnowledgeResolver.resolve(productName)

        val resolvedLocation =
            resolveStorageLocation(productName)

        val baseDate =
            purchaseDate ?: LocalDate.now()

        val resolvedExpiry =
            explicitExpiry
                ?: baseDate
                    .plusDays(
                        knowledge.suggestedShelfLifeDays.toLong()
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
            database
                .productDao()
                .updateLocationById(
                    existing.id,
                    resolvedLocation
                )

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

        receiptItems.value =
            database
                .receiptDao()
                .getAllReceipts()
    }

    // Temporary block
    private suspend fun saveTestLocationPreference(
        productName: String,
        location: String
    ) {

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
    // To here
    private suspend fun refreshShoppingList() {

        val generatedItems = generateShoppingList()

        generatedItems.forEach { generatedItem ->

            val existing =
                database.shoppingDao().findByDescription(
                    generatedItem.lowercase().trim()
                )

            if (existing == null) {

                database.shoppingDao().insertItem(

                    ShoppingItemEntity(

                        description = generatedItem,

                        normalisedDescription =
                            generatedItem.lowercase().trim(),

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

        items.forEach { item ->

            if (item.quantity <= 0) {

                results.add("${item.itemName} — Out of stock")

            } else {

                val expiry =
                    item.expiryDate?.trim()

                if (!expiry.isNullOrBlank()) {

                    try {

                        val expiryDate =
                            java.time.LocalDate.parse(expiry)

                        val days =
                            java.time.temporal.ChronoUnit.DAYS
                                .between(today, expiryDate)

                        if (days < 0) {

                            results.add("${item.itemName} — Expired")

                        } else if (days <= 7) {

                            results.add("${item.itemName} — Expiring soon")
                        }

                    } catch (_: Exception) {
                    }
                }
            }
        }

        return results.distinct().sorted()
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