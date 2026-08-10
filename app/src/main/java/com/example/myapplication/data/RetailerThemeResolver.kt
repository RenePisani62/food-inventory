package com.example.myapplication.data

import androidx.compose.ui.graphics.Color

data class RetailerTheme(

    val headerColor: Color,

    val accentColor: Color,

    val retailerName: String
)

object RetailerThemeResolver {

    fun getTheme(store: String?): RetailerTheme {

        return when (store?.lowercase()) {

            "coles" ->

                RetailerTheme(

                    headerColor = Color(0xFFD32F2F),

                    accentColor = Color(0xFFFFCDD2),

                    retailerName = "Coles"

                )

            "woolworths" ->

                RetailerTheme(

                    headerColor = Color(0xFF2E7D32),

                    accentColor = Color(0xFFC8E6C9),

                    retailerName = "Woolworths"

                )

            else ->

                RetailerTheme(

                    headerColor = Color.DarkGray,

                    accentColor = Color.LightGray,

                    retailerName = "Unknown"

                )
        }
    }
}