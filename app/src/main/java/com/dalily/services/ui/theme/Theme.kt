package com.dalily.services.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 1. Cosmic Slate ColorScheme (🌌 كوزميك سيلفر)
private val CosmicSlateColorScheme = darkColorScheme(
    primary = Color(0xFFCBD5E1),       // Vibrant Metallic Silver
    secondary = Color(0xFF94A3B8),     // Slate Silver Grey
    tertiary = Color(0xFF38BDF8),      // Light Blue Glow
    background = Color(0xFF0F172A),    // Relaxing Deep Slate Blue 900
    surface = Color(0xFF1E293B),       // Slate 800
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),  // Pure White-ish
    onSurface = Color(0xFFF8FAFC)
)

// 2. Charcoal Gold ColorScheme (✨ الذهبي الفاخر)
private val CharcoalGoldColorScheme = darkColorScheme(
    primary = Color(0xFFFFD700),       // Luxurious Gold
    secondary = Color(0xFFD4AF37),     // Soft Gold
    tertiary = Color(0xFFF59E0B),      // Vibrant Amber
    background = Color(0xFF121212),    // Charcoal Black
    surface = Color(0xFF1E1E1E),       // Light Charcoal Dark
    onPrimary = Color(0xFF121212),
    onSecondary = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFF1F5F9)
)

// 3. Royal Emerald ColorScheme (🟢 الزمردي الراقي)
private val RoyalEmeraldColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),       // Royal Emerald Green
    secondary = Color(0xFF10B981),     // Soft Minty Green
    tertiary = Color(0xFF84CC16),      // Soft Needly Leaf Accents
    background = Color(0xFF022C22),    // Dark Forest Emerald
    surface = Color(0xFF064E3B),       // Emerald Moss Green
    onPrimary = Color(0xFF022C22),
    onSecondary = Color(0xFF022C22),
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFF0FDF4)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
)

@Composable
fun DalilyTheme(
    themeName: String = "Cosmic Slate",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Charcoal Gold" -> CharcoalGoldColorScheme
        "Royal Emerald" -> RoyalEmeraldColorScheme
        else -> CosmicSlateColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
