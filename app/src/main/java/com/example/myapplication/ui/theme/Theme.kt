package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(

    primary = PantryGreen,
    onPrimary = PantryWhite,

    primaryContainer = PantrySurfaceGreen,
    onPrimaryContainer = PantryGreenDark,

    secondary = PantryGreenLight,
    onSecondary = PantryTextDark,

    background = PantryCream,
    onBackground = PantryTextDark,

    surface = PantrySurface,
    onSurface = PantryTextDark,

    surfaceVariant = PantrySurfaceGreen,
    onSurfaceVariant = PantryTextSecondary,

    outline = PantryOutline,

    error = PantryRed,
    onError = PantryWhite
)

private val DarkColorScheme = darkColorScheme(

    primary = PantryGreenLight,
    onPrimary = Color(0xFF17310E),

    primaryContainer = PantryGreenDark,
    onPrimaryContainer = Color(0xFFE3F0D7),

    secondary = PantryGreenLight,
    onSecondary = Color(0xFF1C2B17),

    background = Color(0xFF172016),
    onBackground = Color(0xFFE9EEE5),

    surface = Color(0xFF1F291D),
    onSurface = Color(0xFFE9EEE5),

    surfaceVariant = Color(0xFF293626),
    onSurfaceVariant = Color(0xFFC5D0BD),

    outline = Color(0xFF829078),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}