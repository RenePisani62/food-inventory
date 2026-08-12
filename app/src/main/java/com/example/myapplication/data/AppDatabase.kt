package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.ProductEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        BarcodeNameEntity::class,
        ShoppingItemEntity::class,
        ProductKnowledgeOverrideEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class
        ],
    version = 10
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun productKnowledgeOverrideDao():
            ProductKnowledgeOverrideDao

    abstract fun receiptDao(): ReceiptDao

    abstract fun receiptItemDao(): ReceiptItemDao
    companion object {
        private val MIGRATION_9_10 =
            object : Migration(9, 10) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                CREATE TABLE IF NOT EXISTS `receipt_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `receiptId` INTEGER NOT NULL,
                    `retailer` TEXT NOT NULL,
                    `receiptDate` TEXT,
                    `productName` TEXT NOT NULL,
                    `quantity` REAL,
                    `unit` TEXT,
                    `unitPrice` REAL,
                    `totalPrice` REAL
                )
                """.trimIndent()
                    )
                }
            }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food_inventory_db"
                )
                    .addMigrations(MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}