package com.dalily.services.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00B0FF), // Dynamic Electric Blue
    secondary = Color(0xFFFFD700), // Amber Gold
    tertiary = Color(0xFF00E676), // Rich Emerald Green
    background = Color(0xFF121212), // Deep Matte Dark
    surface = Color(0xFF1E1E1E), // Soft Card Charcoal
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9),
    primaryContainer = Color(0xFF003B5C),
    surfaceVariant = Color(0xFF2B2D31)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0288D1), // Ocean Sky Blue
    secondary = Color(0xFFFBC02D), // Yemeni Gold Yellow
    tertiary = Color(0xFF2E7D32), // Forest Green
    background = Color(0xFFF8FAFC), // Pure Soft Slate
    surface = Color.White, // Seamless White
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF0F172A), // Slate 900
    onSurface = Color(0xFF1E293B), // Slate 800
    primaryContainer = Color(0xFFE0F2FE),
    surfaceVariant = Color(0xFFF1F5F9)
)

@Composable
fun DalilyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
