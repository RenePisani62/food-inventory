package com.example.myapplication.data

import androidx.room.*

@Dao
interface ShoppingDao {

    @Insert
    suspend fun insertItem(item: ShoppingItemEntity)

    @Update
    suspend fun updateItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingItemEntity)

    @Query("""
        SELECT *
        FROM shopping_items
        WHERE normalisedDescription = :description
        LIMIT 1
    """)
    suspend fun findByDescription(
        description: String
    ): ShoppingItemEntity?

    @Query("""
        SELECT *
        FROM shopping_items
        ORDER BY checked ASC, description ASC
    """)
    suspend fun getAllItems(): List<ShoppingItemEntity>

    @Query("""
    UPDATE shopping_items
    SET checked = :checked,
        lastModified = :modified
    WHERE id = :id
""")
    suspend fun updateChecked(
        id: Int,
        checked: Boolean,
        modified: Long
    )

    @Query("""
        DELETE FROM shopping_items
    """)


    suspend fun clearAll()
}