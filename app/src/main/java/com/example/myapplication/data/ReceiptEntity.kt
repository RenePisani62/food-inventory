package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val storeName: String? = null,

    val receiptDate: String? = null,

    val totalAmount: Double? = null,

    val rawText: String,

    val receiptNumber: String?,

    val fingerprint: String? = null,

    // Retailer's authoritative item count where available.
    // Null for historical receipts or formats that do not provide one.
    val itemCount: Int? = null,

    val createdAt: Long = System.currentTimeMillis()
)