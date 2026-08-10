package com.example.myapplication.data

object RetailerDetector {

    fun detect(rawText: String): Retailer {

        val text = rawText.lowercase()

        return when {

            "coles" in text ->
                Retailer.COLES

            "woolworths" in text ->
                Retailer.WOOLWORTHS

            "aldi" in text ->
                Retailer.ALDI

            else ->
                Retailer.UNKNOWN
        }
    }
}