package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// High Density Theme Color Schemes
private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoText,
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldText,
    tertiary = RoseAlert,
    onTertiary = Color.White,
    tertiaryContainer = RoseContainer,
    onTertiaryContainer = RoseText,
    background = SlateBackgroundLight,
    onBackground = SlateTextPrimary,
    surface = CardBackgroundWhite,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateBorderVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorderLight,
    error = RoseAlert,
    onError = Color.White,
    errorContainer = RoseContainer,
    onErrorContainer = RoseText
)

// Define a matching professional dark scheme that also matches Indigo and Emerald signatures
private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = IndigoPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = EmeraldAccentLight,
    onSecondary = Color.Black,
    secondaryContainer = EmeraldAccentDark,
    onSecondaryContainer = Color.White,
    tertiary = RoseAlertLight,
    onTertiary = Color.Black,
    tertiaryContainer = RoseAlertDark,
    onTertiaryContainer = Color.White,
    background = Color(0xFF0F172A), // Deep Slate Dark
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set false to default directly to the requested light Slate/Indigo design signature
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
