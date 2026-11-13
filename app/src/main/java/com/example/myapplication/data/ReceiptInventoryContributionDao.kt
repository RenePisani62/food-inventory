
package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReceiptInventoryContributionDao {

    @Insert
    suspend fun insert(
        contribution: ReceiptInventoryContributionEntity
    ): Long

    @Insert
    suspend fun insertAll(
        contributions: List<ReceiptInventoryContributionEntity>
    )

    @Query("""
        SELECT *
        FROM receipt_inventory_contributions
        WHERE receiptId = :receiptId
        ORDER BY id ASC
    """)
    suspend fun getForReceipt(
        receiptId: Long
    ): List<ReceiptInventoryContributionEntity>

    // Inventory was replaced/cleared; retain receipt history for future rebuild,
    // but never subtract from a newly assigned product ID.
    @Query("UPDATE receipt_inventory_contributions SET productId = -1")
    suspend fun invalidateProductLinks()

    @Query("SELECT * FROM receipt_inventory_contributions ORDER BY id ASC")
    suspend fun getAll(): List<ReceiptInventoryContributionEntity>

    @Query("""
        DELETE FROM receipt_inventory_contributions
        WHERE receiptId = :receiptId
    """)
    suspend fun deleteForReceipt(
        receiptId: Long
    )

    @Query("""
        DELETE FROM receipt_inventory_contributions
    """)
    suspend fun deleteAll()
}