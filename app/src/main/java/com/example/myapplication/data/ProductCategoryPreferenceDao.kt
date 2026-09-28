package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// ============================================================
// PRODUCT CLASSIFICATION - LEARNED CATEGORY DAO
// ============================================================

@Dao
interface ProductCategoryPreferenceDao {

    @Query(
        """
        SELECT *
        FROM product_category_preferences
        WHERE productKey = :productKey
        LIMIT 1
        """
    )
    suspend fun getPreference(
        productKey: String
    ): ProductCategoryPreferenceEntity?

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun savePreference(
        preference: ProductCategoryPreferenceEntity
    )
}