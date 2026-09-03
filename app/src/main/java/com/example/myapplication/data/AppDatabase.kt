package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        BarcodeNameEntity::class,
        ShoppingItemEntity::class,
        ProductKnowledgeOverrideEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        ProductLocationPreferenceEntity::class
    ],
    version = 12
)

abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun shoppingDao(): ShoppingDao

    abstract fun productKnowledgeOverrideDao():
            ProductKnowledgeOverrideDao

    abstract fun productLocationPreferenceDao():
            ProductLocationPreferenceDao

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

        private val MIGRATION_10_11 =
            object : Migration(10, 11) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                        ALTER TABLE receipts
                        ADD COLUMN fingerprint TEXT
                        """.trimIndent()
                    )
                }
            }
        private val MIGRATION_11_12 =
            object : Migration(11, 12) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    db.execSQL(
                        """
                CREATE TABLE IF NOT EXISTS `product_location_preferences` (
                    `productKey` TEXT NOT NULL,
                    `originalName` TEXT NOT NULL,
                    `location` TEXT NOT NULL,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`productKey`)
                )
                """.trimIndent()
                    )
                }
            }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context
        ): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "food_inventory_db"
                    )
                        .addMigrations(
                            MIGRATION_9_10,
                            MIGRATION_10_11,
                            MIGRATION_11_12
                        )
                        .fallbackToDestructiveMigration()
                        .build()

                INSTANCE = instance

                instance
            }
        }
    }
}