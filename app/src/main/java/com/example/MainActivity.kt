package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AppViewModel
import com.example.ui.HomeScreen
import com.example.ui.OwnerDashboardScreen
import com.example.ui.ProvidersScreen
import com.example.ui.SplashScreen
import com.example.ui.parseColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val navController = rememberNavController()

            // Dynamic light color scheme synchronized with Owner Custom settings
            val primaryColorVal = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
            val secondaryColorVal = parseColor(viewModel.secondaryColorHex, Color(0xFFFB8C00))

            val dynamicColorScheme = remember(primaryColorVal, secondaryColorVal) {
                lightColorScheme(
                    primary = primaryColorVal,
                    secondary = secondaryColorVal,
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFFFFFF),
                    onPrimary = Color(0xFFFFFFFF),
                    onSecondary = Color(0xFFFFFFFF),
                    onBackground = Color(0xFF212121),
                    onSurface = Color(0xFF212121)
                )
            }

            MaterialTheme(
                colorScheme = dynamicColorScheme
            ) {
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(navController, viewModel)
                    }
                    composable("home") {
                        HomeScreen(navController, viewModel)
                    }
                    composable("providers/{categoryId}") { backStackEntry ->
                        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                        ProvidersScreen(navController, categoryId, viewModel)
                    }
                    composable("owner_dashboard") {
                        OwnerDashboardScreen(navController, viewModel)
                    }
                }
            }
        }
    }
}
