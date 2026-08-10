package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_knowledge_overrides")
data class ProductKnowledgeOverrideEntity(

    @PrimaryKey
    val barcode: String,

    val productName: String,

    val storageLocation: String,

    val foodCategory: String,

    val shoppingCategory: String,

    val suggestedShelfLifeDays: Int,

    val lastUpdated: Long = System.currentTimeMillis()
)