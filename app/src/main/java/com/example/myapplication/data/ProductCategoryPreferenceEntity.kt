package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// ============================================================
// PRODUCT CLASSIFICATION - LEARNED CATEGORY PREFERENCE
// ============================================================

@Entity(
    tableName = "product_category_preferences"
)
data class ProductCategoryPreferenceEntity(

    @PrimaryKey
    val productKey: String,

    val originalName: String,

    val category: String,

    val lastUpdated: Long
)