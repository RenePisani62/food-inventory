package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductLocationPreferenceDao {

    @Query(
        """
        SELECT * 
        FROM product_location_preferences
        WHERE productKey = :productKey
        LIMIT 1
        """
    )
    suspend fun getPreference(
        productKey: String
    ): ProductLocationPreferenceEntity?

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun savePreference(
        preference: ProductLocationPreferenceEntity
    )
}