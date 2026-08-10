package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductKnowledgeOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOverride(
        override: ProductKnowledgeOverrideEntity
    )

    @Query("""
        SELECT *
        FROM product_knowledge_overrides
        WHERE barcode = :barcode
        LIMIT 1
    """)
    suspend fun getOverride(
        barcode: String
    ): ProductKnowledgeOverrideEntity?
}