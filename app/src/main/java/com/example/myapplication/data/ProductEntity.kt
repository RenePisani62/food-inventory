package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val itemName: String,
    val quantity: Int,
    val lastScanned: Long,
    val expiryDate: String? = null,
    val location: String = "Pantry"
)