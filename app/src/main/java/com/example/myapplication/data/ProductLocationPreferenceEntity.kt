package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_location_preferences")
data class ProductLocationPreferenceEntity(

    @PrimaryKey
    val productKey: String,

    val originalName: String,

    val location: String,

    val lastUpdated: Long
)