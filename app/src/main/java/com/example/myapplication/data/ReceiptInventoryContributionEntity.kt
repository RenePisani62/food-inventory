
package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt_inventory_contributions",
    indices = [
        Index(value = ["receiptId"]),
        Index(value = ["productId"])
    ]
)
data class ReceiptInventoryContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val receiptId: Long,
    val receiptItemId: Long,
    val productId: Int,
    val quantityAdded: Int
)