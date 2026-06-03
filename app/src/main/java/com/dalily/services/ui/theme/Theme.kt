package com.dalily.services.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.dalily.services.data.FirebaseRepository

@Composable
fun DalilyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val config = FirebaseRepository.appConfig.collectAsState()
    
    // Dynamically parse hex colors with robust fallback
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(config.value.primaryColorHex))
    } catch (e: Exception) {
        Color(0xFF0D9488) // Default Teal
    }

    val secondaryColor = try {
        Color(android.graphics.Color.parseColor(config.value.secondaryColorHex))
    } catch (e: Exception) {
        Color(0xFF0F766E)
    }

    val colorScheme = darkColorScheme(
        primary = primaryColor,
        secondary = secondaryColor,
        background = Color(0xFF0B0F19), // Eye-safe pitch dark modern canvas
        surface = Color(0xFF1E293B),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFFF8FAFC),
        onSurface = Color(0xFFF1F5F9)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
