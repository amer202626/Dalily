package com.yemenservices.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.yemenservices.app.ui.AppViewModel
import com.yemenservices.app.ui.MainScreen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val welcomeConfig by viewModel.welcomeConfig.collectAsState()
            
            val fontColorVal = when (welcomeConfig.fontColor) {
                "light_gold" -> Color(0xFFECC94B)
                "vibrant_silver" -> Color(0xFFCBD5E1)
                else -> Color.White
            }
            
            // Generate the synchronized custom Material 3 themes
            val colorScheme = when (welcomeConfig.globalTheme) {
                "cosmic_slate" -> darkColorScheme(
                    primary = Color(0xFFCBD5E1),
                    onPrimary = Color.Black,
                    secondary = Color(0xFF64748B),
                    onSecondary = Color.White,
                    background = Color(0xFF0F172A),
                    onBackground = fontColorVal,
                    surface = Color(0xFF1E293B),
                    onSurface = fontColorVal,
                    surfaceVariant = Color(0xFF334155),
                    onSurfaceVariant = fontColorVal.copy(alpha = 0.8f)
                )
                "charcoal_gold" -> darkColorScheme(
                    primary = Color(0xFFECC94B),
                    onPrimary = Color.Black,
                    secondary = Color(0xFFD69E2E),
                    onSecondary = Color.White,
                    background = Color(0xFF111111),
                    onBackground = fontColorVal,
                    surface = Color(0xFF1A1A1A),
                    onSurface = fontColorVal,
                    surfaceVariant = Color(0xFF2D2D2D),
                    onSurfaceVariant = fontColorVal.copy(alpha = 0.8f)
                )
                "royal_emerald" -> darkColorScheme(
                    primary = Color(0xFF10B981),
                    onPrimary = Color.Black,
                    secondary = Color(0xFF047857),
                    onSecondary = Color.White,
                    background = Color(0xFF022C22),
                    onBackground = fontColorVal,
                    surface = Color(0xFF064E3B),
                    onSurface = fontColorVal,
                    surfaceVariant = Color(0xFF0F766E),
                    onSurfaceVariant = fontColorVal.copy(alpha = 0.8f)
                )
                "royal_indigo" -> darkColorScheme(
                    primary = Color(0xFF8F94FB),
                    onPrimary = Color.White,
                    secondary = Color(0xFF4D54C8),
                    onSecondary = Color.White,
                    background = Color(0xFF0C0A1B),
                    onBackground = fontColorVal,
                    surface = Color(0xFF15122B),
                    onSurface = fontColorVal,
                    surfaceVariant = Color(0xFF23203C),
                    onSurfaceVariant = fontColorVal.copy(alpha = 0.8f)
                )
                "emerald_green" -> darkColorScheme(
                    primary = Color(0xFF00FF88),
                    onPrimary = Color.Black,
                    secondary = Color(0xFF00B359),
                    onSecondary = Color.White,
                    background = Color(0xFF060D0A),
                    onBackground = fontColorVal,
                    surface = Color(0xFF0E1A14),
                    onSurface = fontColorVal,
                    surfaceVariant = Color(0xFF182B21),
                    onSurfaceVariant = fontColorVal.copy(alpha = 0.8f)
                )
                "slate_silver" -> darkColorScheme(
                    primary = Color(0xFFDCDFE4),
                    onPrimary = Color.Black,
                    secondary = Color(0xFF8F97A3),
                    onSecondary = Color.White,
                    background = Color(0xFF161B21),
                    onBackground = fontColorVal,
                    surface = Color(0xFF202630),
                    onSurface = fontColorVal,
                    surfaceVariant = Color(0xFF2D3542),
                    onSurfaceVariant = fontColorVal.copy(alpha = 0.8f)
                )
                "ocean_teal" -> darkColorScheme(
                    primary = Color(0xFF00F5D4),
                    onPrimary = Color.Black,
                    secondary = Color(0xFF00BFA5),
                    onSecondary = Color.White,
                    background = Color(0xFF001518),
                    onBackground = fontColorVal,
                    surface = Color(0xFF02252A),
                    onSurface = fontColorVal,
                    surfaceVariant = Color(0xFF04373F),
                    onSurfaceVariant = fontColorVal.copy(alpha = 0.8f)
                )
                "beige_cream" -> lightColorScheme(
                    primary = Color(0xFF795548),
                    onPrimary = Color.White,
                    secondary = Color(0xFF8D6E63),
                    onSecondary = Color.White,
                    background = Color(0xFFFAF6F0),
                    onBackground = Color(0xFF3E2723),
                    surface = Color(0xFFF5EBE1),
                    onSurface = Color(0xFF3E2723),
                    surfaceVariant = Color(0xFFE5D5C5),
                    onSurfaceVariant = Color(0xFF5D4037)
                )
                else -> darkColorScheme( // "red_black" - Default Dark Theme with Vibrant Red Shimmers
                    primary = Color(0xFFE50914),
                    onPrimary = Color.White,
                    secondary = Color(0xFFB30710),
                    onSecondary = Color.White,
                    background = Color(0xFF0F0F0F),
                    onBackground = fontColorVal,
                    surface = Color(0xFF161616),
                    onSurface = fontColorVal,
                    surfaceVariant = Color(0xFF2C2C2C),
                    onSurfaceVariant = fontColorVal.copy(alpha = 0.8f)
                )
            }

            MaterialTheme(
                colorScheme = colorScheme
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
