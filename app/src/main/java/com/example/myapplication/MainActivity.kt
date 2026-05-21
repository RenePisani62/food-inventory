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
import androidx.compose.ui.Alignment
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.myapplication.data.BarcodeNameEntity
import androidx.compose.material3.Checkbox



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

    private var manualShoppingItemInput =
        mutableStateOf("")

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

    private var shoppingListItems =
        mutableStateOf(mutableListOf<String>())

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

            if (
                productName == "Unknown Item"
                && mode.value == "ADD"
            ) {

                pendingUnknownBarcode.value = barcode

                pendingUnknownNameInput.value = ""

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

                    dao.updateExpiryDateById(
                        productToUpdate.id,
                        expiry
                    )

                } else {

                    val newProduct =
                        ProductEntity(
                            barcode = barcode,
                            itemName = productName,
                            quantity = amount,
                            lastScanned = System.currentTimeMillis(),
                            expiryDate = expiry,
                            location = selectedLocation.value
                        )

                    dao.insertProduct(newProduct)
                }

            } else if (mode.value == "DELETE") {

                val productToReduce =
                    existing
                        ?: dao.getProductByBarcodeOnly(barcode)

                if (productToReduce != null) {

                    if (productToReduce.quantity <= amount) {

                        dao.updateQuantityById(
                            productToReduce.id,
                            0
                        )

                    } else {

                        dao.subtractQuantityById(
                            productToReduce.id,
                            amount
                        )
                    }
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

        lifecycleScope.launch {
            expirySummary.value = getExpirySummary()

            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen.value) {

                            "HOME" -> HomeScreen()

                            "INVENTORY" -> InventoryScreen()

                            "DETAIL" -> ProductDetailScreen()

                            "SHOPPING" -> ShoppingListScreen()

                            else -> HomeScreen()
                        }
                    }
                }
            }

        }
    }


    @Composable
    private fun HomeScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "PantryPal",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
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
                                val barcode =
                                    pendingUnknownBarcode.value
                                val name =
                                    pendingUnknownNameInput
                                        .value
                                        .trim()

                                if (
                                    barcode != null
                                    && name.isNotBlank()
                                ) {
                                    lifecycleScope.launch {
                                        database
                                            .productDao()
                                            .insertBarcodeName(
                                                BarcodeNameEntity(
                                                    barcode = barcode,
                                                    name = name,
                                                    lastUpdated =
                                                        System.currentTimeMillis()
                                                )
                                            )

                                        pendingUnknownBarcode.value =
                                            null

                                        pendingUnknownNameInput.value =
                                            ""
                                    }
                                    processBarcode(barcode)
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

            val (soon, expired) = expirySummary.value

            if (soon > 0 || expired > 0) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = buildString {
                        if (soon > 0) append("⚠️ $soon expiring soon  ")
                        if (expired > 0) append("❌ $expired expired")
                    },
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spacer(modifier = Modifier.width(8.dp))
                }
               //  Spacer(modifier = Modifier.height(12.dp))
            }
            Text("Quick Scan Mode")
            Switch(
                checked = quickScanMode.value,
                onCheckedChange = {
                    quickScanMode.value = it
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            var expanded by remember { mutableStateOf(false) }
            // Spacer(modifier = Modifier.height(16.dp))
            Text("Bluetooth / Manual Barcode Input")
            // Spacer(modifier = Modifier.height(2.dp))
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

                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {

                    val barcode =
                        manualBarcodeInput.value.trim()

                    if (barcode.isNotBlank()) {

                        processBarcode(barcode)

                        manualBarcodeInput.value = ""
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Process Barcode")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box {

                Button(
                    onClick = {
                        expanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedCategory.value)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {

                    categories.forEach { category ->

                        DropdownMenuItem(
                            text = {
                                Text(category)
                            },

                            onClick = {

                                selectedCategory.value = category

                                applySuggestedExpiryDate()
                                saveIntakeDefaults()
                                applySuggestedLocation()
                                saveIntakeDefaults()

                                expanded = false
                            }
                        )
                    }
                }
            }
            Text("Quantity:")

            TextField(
                value = quantityInput.value,
                onValueChange = {
                    quantityInput.value = it
                    saveIntakeDefaults()
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )


            Spacer(modifier = Modifier.height(12.dp))

            Text("Expiry Date:")

            Button(
                onClick = {
                    showExpiryDatePicker()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (expiryInput.value.isBlank()) {
                        "Select expiry date"
                    } else {
                        expiryInput.value
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        mode.value = "ADD"
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add / Scan")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        mode.value = "DELETE"
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete / Scan")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    lifecycleScope.launch {
                        shoppingListItems.value =
                            generateShoppingList().toMutableList()
                        currentScreen.value = "SHOPPING"
                    }
                },
                modifier = Modifier.fillMaxWidth()
                ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Generate Shopping List")
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
            product.itemName.contains(searchText.value, ignoreCase = true) ||
                    product.barcode.contains(searchText.value, ignoreCase = true)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Inventory",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = searchText.value,
                onValueChange = { searchText.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Search food or barcode")
                }
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

            LazyColumn {
                items(filteredProducts) { product ->
                    val expiryText = "[${product.expiryDate ?: "Not set"}]"


                    Text(
                        text = "${product.itemName} | Location: ${product.location} | Barcode: ${product.barcode} | Qty: ${product.quantity} | Expiry: $expiryText | ${
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

                        shoppingListItems.value.add(item)

                        shoppingListItems.value =
                            shoppingListItems.value.toMutableList()

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
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, buildShoppingListText())
                        type = "text/plain"
                    }

                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Share Shopping List"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share Shopping List")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    copyShoppingListToClipboard()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy Shopping List")
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
            setOrientationLocked(false)
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

                append("• $item\n")
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