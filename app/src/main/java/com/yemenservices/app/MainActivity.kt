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
            
            // Generate the six database-synchronized custom Material 3 themes
            val colorScheme = when (welcomeConfig.globalTheme) {
                "royal_indigo" -> darkColorScheme(
                    primary = Color(0xFF8F94FB),
                    onPrimary = Color.White,
                    secondary = Color(0xFF4D54C8),
                    onSecondary = Color.White,
                    background = Color(0xFF0C0A1B),
                    onBackground = Color(0xFFE5E2FF),
                    surface = Color(0xFF15122B),
                    onSurface = Color(0xFFE5E2FF),
                    surfaceVariant = Color(0xFF23203C),
                    onSurfaceVariant = Color(0xFFCCC5FF)
                )
                "emerald_green" -> darkColorScheme(
                    primary = Color(0xFF00FF88),
                    onPrimary = Color.Black,
                    secondary = Color(0xFF00B359),
                    onSecondary = Color.White,
                    background = Color(0xFF060D0A),
                    onBackground = Color(0xFFE2F9EF),
                    surface = Color(0xFF0E1A14),
                    onSurface = Color(0xFFE2F9EF),
                    surfaceVariant = Color(0xFF182B21),
                    onSurfaceVariant = Color(0xFFBFEFDB)
                )
                "slate_silver" -> darkColorScheme(
                    primary = Color(0xFFDCDFE4),
                    onPrimary = Color.Black,
                    secondary = Color(0xFF8F97A3),
                    onSecondary = Color.White,
                    background = Color(0xFF161B21),
                    onBackground = Color.White,
                    surface = Color(0xFF202630),
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF2D3542),
                    onSurfaceVariant = Color(0xFFD0D7E0)
                )
                "ocean_teal" -> darkColorScheme(
                    primary = Color(0xFF00F5D4),
                    onPrimary = Color.Black,
                    secondary = Color(0xFF00BFA5),
                    onSecondary = Color.White,
                    background = Color(0xFF001518),
                    onBackground = Color(0xFFE0FCFA),
                    surface = Color(0xFF02252A),
                    onSurface = Color(0xFFE0FCFA),
                    surfaceVariant = Color(0xFF04373F),
                    onSurfaceVariant = Color(0xFFBFFBF5)
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
                    onBackground = Color.White,
                    surface = Color(0xFF161616),
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF2C2C2C),
                    onSurfaceVariant = Color(0xFFDDDDDD)
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
