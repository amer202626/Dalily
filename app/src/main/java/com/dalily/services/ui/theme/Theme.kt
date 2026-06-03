package com.dalily.services.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom Cosmic Slate/Yemeni Teal palette
val DarkPrimary = Color(0xFF0D9488)      // Teal active
val DarkSecondary = Color(0xFF0EA5E9)    // Sky accent
val DarkTertiary = Color(0xFFF59E0B)     // Amber warning/stars
val DarkBackground = Color(0xFF0B0F19)   // Midnight slate canvas
val DarkSurface = Color(0xFF1E293B)      // Secondary dark slate card

val LightPrimary = Color(0xFF0D9488)
val LightSecondary = Color(0xFF0369A1)
val LightTertiary = Color(0xFFD97706)
val LightBackground = Color(0xFFF8FAFC)  // Clean modern grey-white
val LightSurface = Color(0xFFFFFFFF)     // High-contrast clean card

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFF1F5F9), // Slate white
    onSurface = Color(0xFFF1F5F9)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A), // Midnight slate text
    onSurface = Color(0xFF0F172A)
)

@Composable
fun DaliliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val typography = Typography(
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
        ),
        titleMedium = MaterialTheme.typography.titleMedium.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
        ),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
