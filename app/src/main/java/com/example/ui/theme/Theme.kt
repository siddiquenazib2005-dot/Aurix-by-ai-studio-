package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AurixColorScheme = darkColorScheme(
    primary = AurixCyan,
    onPrimary = AurixVoid,
    primaryContainer = AurixCardHover,
    onPrimaryContainer = AurixCyan,
    secondary = AurixViolet,
    onSecondary = Color.White,
    secondaryContainer = AurixCardSurface,
    onSecondaryContainer = AurixVioletLight,
    tertiary = AurixElectricBlue,
    onTertiary = Color.White,
    background = AurixVoid,
    onBackground = AurixTextPrimary,
    surface = AurixDarkSurface,
    onSurface = AurixTextPrimary,
    surfaceVariant = AurixCardSurface,
    onSurfaceVariant = AurixTextSecondary,
    outline = AurixBorder,
    error = AurixCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve distinctive futuristic Aurix styling
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AurixColorScheme,
        typography = Typography,
        content = content
    )
}
