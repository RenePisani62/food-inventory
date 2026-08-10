package com.example.myapplication.data

class ReceiptRepository(

    private val receiptDao: ReceiptDao

) {

    suspend fun saveReceipt(

        receipt: ReceiptEntity

    ) {

        receiptDao.insertReceipt(receipt)
    }

    suspend fun getReceipts():

            List<ReceiptEntity> {

        return receiptDao.getAllReceipts()
    }

    suspend fun clearReceipts() {

        receiptDao.clearAllReceipts()
    }
}