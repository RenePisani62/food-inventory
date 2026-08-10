package com.example.myapplication.data

class ReceiptService(

    private val repository: ReceiptRepository

) {

    suspend fun saveRawReceipt(

        rawText: String,

        store: String? = null,

        receiptDate: String? = null,

        total: Double? = null

    ) {

        repository.saveReceipt(

            ReceiptEntity(
                storeName = store,
                receiptDate = receiptDate,
                totalAmount = total,
                receiptNumber = null,
                rawText = rawText
            )
        )
    }

    suspend fun getReceipts() =

        repository.getReceipts()

    suspend fun clearReceipts() =

        repository.clearReceipts()
}