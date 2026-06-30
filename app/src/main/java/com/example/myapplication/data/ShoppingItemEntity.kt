package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val description: String,

    val normalisedDescription: String,

    val barcode: String? = null,

    val checked: Boolean = false,

    val source: String = "MANUAL",

    val created: Long = System.currentTimeMillis(),

    val lastModified: Long = System.currentTimeMillis()
)