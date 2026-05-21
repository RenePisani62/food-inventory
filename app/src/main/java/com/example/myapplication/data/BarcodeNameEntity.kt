package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode_names")
data class BarcodeNameEntity(

    @PrimaryKey
    val barcode: String,

    val name: String,

    val lastUpdated: Long
)