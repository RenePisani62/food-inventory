package com.example.myapplication.data

data class ParsedReceiptItem(

    val name: String,

    val quantity: Double? = null,

    val unit: String? = null,

    val unitPrice: Double? = null,

    val totalPrice: Double? = null
)

fun ParsedReceiptItem.displayDetails(): String {

    return when {

        unit == "kg" &&
                quantity != null &&
                unitPrice != null -> {

            "%.3f kg @ $%.2f/kg".format(
                quantity,
                unitPrice
            )
        }

        quantity != null &&
                quantity > 1 &&
                unitPrice != null -> {

            "%.0f × $%.2f".format(
                quantity,
                unitPrice
            )
        }

        else -> ""
    }
}