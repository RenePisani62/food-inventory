package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_items")
data class ReceiptItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val receiptId: Long,

    val retailer: String,

    val receiptDate: String?,

    val productName: String,

    val quantity: Double?,

    val unit: String?,

    val unitPrice: Double?,

    val totalPrice: Double?
)