package com.yemenservices.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.yemenservices.app.ui.AppViewModel
import com.yemenservices.app.ui.MainNavigationSystem

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContent {
            val appConfig by viewModel.appConfig.collectAsState()
            
            val colorScheme = when (appConfig.globalTheme) {
                "cosmic_slate" -> darkColorScheme(
                    primary = Color(0xFFA5C9FF),      // Celestial Blue Accent
                    onPrimary = Color(0xFF00315E),
                    secondary = Color(0xFF8AB4F8),
                    background = Color(0xFF0F141C),     // Deep Nebula Space Gray
                    surface = Color(0xFF161D26),        // Obsidian Card
                    onBackground = Color(0xFFE2E2E6),
                    onSurface = Color(0xFFDCDFE4)
                )
                "charcoal_gold" -> darkColorScheme(
                    primary = Color(0xFFFFD700),      // Luxurious Gold Shimmer
                    onPrimary = Color(0xFF3F3000),
                    secondary = Color(0xFFF3E5AB),
                    background = Color(0xFF121212),     // Pure Matte Charcoal
                    surface = Color(0xFF1E1E1E),        // Luxury Card Gray
                    onBackground = Color(0xFFFFFFFF),
                    onSurface = Color(0xFFECEFF1)
                )
                "royal_emerald" -> darkColorScheme(
                    primary = Color(0xFF50C878),      // Royal Vivid Emerald
                    onPrimary = Color(0xFF00391E),
                    secondary = Color(0xFF90EE90),
                    background = Color(0xFF0A1B12),     // Emerald Forest Depth
                    surface = Color(0xFF12281D),        // Deep Greenish Dark Card
                    onBackground = Color(0xFFE2F0E6),
                    onSurface = Color(0xFFDFEFE5)
                )
                "red_black" -> darkColorScheme(
                    primary = Color(0xFFFF4D4D),      // Energetic Crimson Red
                    onPrimary = Color(0xFF4B0000),
                    secondary = Color(0xFFFF8F8F),
                    background = Color(0xFF080808),     // Infinite Pitch Dark
                    surface = Color(0xFF141414),
                    onBackground = Color(0xFFFDFBFB),
                    onSurface = Color(0xFFEEEEEE)
                )
                "slate_silver" -> darkColorScheme(
                    primary = Color(0xFFCFD8DC),      // Premium Brushed Silver
                    onPrimary = Color(0xFF263238),
                    secondary = Color(0xFF90A4AE),
                    background = Color(0xFF1E2429),     // Industrial Dark Slate
                    surface = Color(0xFF272F35),
                    onBackground = Color(0xFFECEFF1),
                    onSurface = Color(0xFFCFD8DC)
                )
                "ocean_teal" -> darkColorScheme(
                    primary = Color(0xFF4DB6AC),      // Soothing Ocean Teal
                    onPrimary = Color(0xFF003732),
                    secondary = Color(0xFF80CBC4),
                    background = Color(0xFF001A18),     // Deep Sea Abyss
                    surface = Color(0xFF022B27),
                    onBackground = Color(0xFFE0F2F1),
                    onSurface = Color(0xFFB2DFDB)
                )
                "beige_cream" -> lightColorScheme(
                    primary = Color(0xFF795548),      // Warm Coffee Cream
                    onPrimary = Color(0xFFFFFFFF),
                    secondary = Color(0xFF8D6E63),
                    background = Color(0xFFFAF6F0),     // Soft Warm Earth
                    surface = Color(0xFFF4EDE4),
                    onBackground = Color(0xFF3E2723),
                    onSurface = Color(0xFF4E342E)
                )
                else -> darkColorScheme(
                    primary = Color(0xFFA5C9FF),
                    background = Color(0xFF0F141C),
                    surface = Color(0xFF161D26)
                )
            }

            MaterialTheme(
                colorScheme = colorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigationSystem(viewModel = viewModel)
                }
            }
        }
    }
}
