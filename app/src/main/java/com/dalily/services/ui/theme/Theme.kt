package com.dalily.services.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

fun parseColor(hex: String, defaultColor: Color): Color {
    return try {
        val sanitized = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(sanitized))
    } catch (e: Exception) {
        defaultColor
    }
}

fun getDynamicThemeColors(
    themeName: String,
    primaryHex: String,
    secondaryHex: String
): ColorScheme {
    val primaryColor = parseColor(primaryHex, Color(0xFF708090))
    val secondaryColor = parseColor(secondaryHex, Color(0xFFA9A9A9))

    return when (themeName) {
        "كوزميك سيلفر" -> darkColorScheme(
            primary = Color(0xFFC0C0C0), // Silver
            onPrimary = Color(0xFF1A1A24),
            primaryContainer = Color(0xFF4A5560), // Slate gray container
            onPrimaryContainer = Color.White,
            secondary = Color(0xFFE0E0E0),
            onSecondary = Color(0xFF1A1A24),
            background = Color(0xFF12121B), // Beautiful slate eye-safe dark
            onBackground = Color.White,
            surface = Color(0xFF1A1A26),
            onSurface = Color.White
        )
        "الذهبي الفاخر" -> darkColorScheme(
            primary = Color(0xFFFFD700), // Luxury Gold
            onPrimary = Color(0xFF121212),
            primaryContainer = Color(0xFF8B7500),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFFDAA520),
            onSecondary = Color(0xFF121212),
            background = Color(0xFF0F0F0F), // Charcoal rich dark
            onBackground = Color(0xFFFFFEFA),
            surface = Color(0xFF181818),
            onSurface = Color(0xFFFFFEFA)
        )
        "الزمردي الراقي" -> darkColorScheme(
            primary = Color(0xFF50C878), // Royal Emerald
            onPrimary = Color(0xFF07170E),
            primaryContainer = Color(0xFF1B4D2F),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFF00A86B),
            onSecondary = Color(0xFF07170E),
            background = Color(0xFF05120B), // Deep royal forest green
            onBackground = Color(0xFFF0FDF4),
            surface = Color(0xFF0B1E13),
            onSurface = Color(0xFFF0FDF4)
        )
        else -> { // "مخصص" or any custom primary/secondary defined by admin
            darkColorScheme(
                primary = primaryColor,
                onPrimary = Color.Black,
                primaryContainer = primaryColor.copy(alpha = 0.3f),
                onPrimaryContainer = Color.White,
                secondary = secondaryColor,
                onSecondary = Color.Black,
                background = Color(0xFF101015),
                onBackground = Color.White,
                surface = Color(0xFF181822),
                onSurface = Color.White
            )
        }
    }
}
