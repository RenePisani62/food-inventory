package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface ProductDao {

    @Query("SELECT * FROM barcode_names WHERE barcode = :barcode LIMIT 1")
    suspend fun getBarcodeName(
        barcode: String
    ): BarcodeNameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcodeName(
        barcodeName: BarcodeNameEntity
    )
    @Query("SELECT * FROM barcode_names")
    suspend fun getAllBarcodeNames(): List<BarcodeNameEntity>
    @Query("UPDATE products SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantityById(id: Int, quantity: Int)

    @Query("UPDATE products SET quantity = :quantity WHERE id = :id")
    suspend fun setQuantityById(id: Int, quantity: Int)
    @Query("UPDATE products SET quantity = quantity - :amount WHERE id = :id")
    suspend fun subtractQuantityById(id: Int, amount: Int)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    @Query("UPDATE products SET expiryDate = :expiryDate WHERE barcode = :barcode")
    suspend fun updateExpiryDate(barcode: String, expiryDate: String?)
    @Query("SELECT * FROM products ORDER BY itemName ASC")
    suspend fun getAllProductsAlphabetical(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE barcode = :barcode AND quantity <= 0 LIMIT 1")
    suspend fun getZeroStockProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode ORDER BY expiryDate ASC LIMIT 1")
    suspend fun getProductByBarcodeOnly(barcode: String): ProductEntity?
    @Query("UPDATE products SET expiryDate = :expiryDate WHERE id = :id")
    suspend fun updateExpiryDateById(id: Int, expiryDate: String?)
    @Query("UPDATE products SET itemName = :name WHERE id = :id")
    suspend fun updateProductNameById(id: Int, name: String)
    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Query("UPDATE products SET itemName = :newName WHERE barcode = :barcode")
    suspend fun updateProductName(barcode: String, newName: String)

    @Query("SELECT * FROM products ORDER BY lastScanned DESC")
    suspend fun getAllProducts(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("UPDATE products SET quantity = quantity + 1, lastScanned = :time WHERE barcode = :barcode")
    suspend fun incrementQuantity(barcode: String, time: Long)

    @Query("UPDATE products SET quantity = quantity + :amount, lastScanned = :time WHERE barcode = :barcode")
    suspend fun addQuantity(barcode: String, amount: Int, time: Long)

    @Query("UPDATE products SET quantity = quantity - :amount WHERE barcode = :barcode")
    suspend fun subtractQuantity(barcode: String, amount: Int)

    @Query("DELETE FROM products WHERE barcode = :barcode")
    suspend fun deleteProduct(barcode: String)

    @Query("SELECT * FROM products WHERE barcode = :barcode AND expiryDate = :expiry LIMIT 1")
    suspend fun getProductByBarcodeAndExpiry(
        barcode: String,
        expiry: String?
    )
    : ProductEntity?
    @Query("""
    SELECT *
    FROM products
    WHERE barcode = :barcode
    ORDER BY expiryDate ASC
        """)
    suspend fun getProductsByBarcode(
        barcode: String
    ): List<ProductEntity>

    @Query("UPDATE products SET location = :location WHERE id = :id")
    suspend fun updateLocationById(
        id: Int,
        location: String
    )

    @Query("""
    SELECT *
    FROM products
    WHERE itemName = :itemName COLLATE NOCASE
    LIMIT 1
""")
    suspend fun getProductByName(
        itemName: String
    ): ProductEntity?
}