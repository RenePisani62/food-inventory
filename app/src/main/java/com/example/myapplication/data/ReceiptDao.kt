package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReceiptDao {

    @Insert
    suspend fun insertReceipt(
        receipt: ReceiptEntity
    )

    @Query("""
        SELECT *
        FROM receipts
        ORDER BY createdAt DESC
    """)
    suspend fun getAllReceipts(): List<ReceiptEntity>

    @Query("""
        DELETE FROM receipts
    """)
    suspend fun clearAllReceipts()

    @Query("""
          DELETE FROM receipts
          """)
    suspend fun deleteAllReceipts()

    @Query(
        "SELECT * FROM receipts WHERE receiptNumber = :receiptNumber LIMIT 1"
    )
    suspend fun getReceiptByNumber(
        receiptNumber: String
    ): ReceiptEntity?

}

