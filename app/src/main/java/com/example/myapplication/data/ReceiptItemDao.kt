package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReceiptItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReceiptItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ReceiptItemEntity>)

    @Query("""
        SELECT *
        FROM receipt_items
        WHERE receiptId = :receiptId
        ORDER BY id ASC
    """)
    suspend fun getItemsForReceipt(
        receiptId: Long
    ): List<ReceiptItemEntity>

    @Query("""
        SELECT *
        FROM receipt_items
        WHERE productName = :productName
        ORDER BY receiptDate DESC
    """)
    suspend fun getPriceHistory(
        productName: String
    ): List<ReceiptItemEntity>

    @Query("""
        DELETE FROM receipt_items
        WHERE receiptId = :receiptId
    """)
    suspend fun deleteForReceipt(
        receiptId: Long
    )
    @Query("""
    DELETE FROM receipt_items
""")
    suspend fun deleteAll()
}