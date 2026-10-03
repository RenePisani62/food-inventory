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
    ORDER BY
        checked ASC,
        CASE WHEN source = 'MANUAL' THEN 0 ELSE 1 END ASC,
        CASE WHEN source = 'MANUAL' THEN created END DESC,
        description ASC
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
    UPDATE shopping_items
    SET quantity = :quantity,
        lastModified = :modified
    WHERE id = :id
""")
    suspend fun updateQuantity(
        id: Int,
        quantity: Int,
        modified: Long
    )

    @Query("""
    DELETE FROM shopping_items
    WHERE source = 'AUTO'
""")
    suspend fun clearAutoItems()
    @Query("""
        DELETE FROM shopping_items
    """)


    suspend fun clearAll()
}