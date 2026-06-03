package com.dalily.services

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dalily.services.data.FirebaseSimulator
import com.dalily.services.ui.screens.*
import com.dalily.services.ui.theme.DalilyTheme

enum class UserRole {
    USER, ADMIN, OWNER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local cache & fetch current configuration state
        FirebaseSimulator.init(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            val dbState by FirebaseSimulator.dbState.collectAsState()
            val activeTheme = dbState.config.themeColors

            DalilyTheme(themeName = activeTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppNavigation()
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation() {
    val navController = rememberNavController()
    
    // Controlled role state across screen scopes
    var currentRole by remember { mutableStateOf(UserRole.USER) }
    var activeAdminName by remember { mutableStateOf<String?>(null) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                navController = navController,
                currentRole = currentRole,
                onLogout = {
                    currentRole = UserRole.USER
                    activeAdminName = null
                },
                onRoleChanged = { role, name ->
                    currentRole = role
                    activeAdminName = name
                }
            )
        }
        
        composable(
            route = "detail/{providerId}",
            arguments = listOf(navArgument("providerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
            DetailScreen(
                navController = navController,
                providerId = providerId,
                currentRole = currentRole,
                activeAdminName = activeAdminName
            )
        }
        
        composable("favorites") {
            FavoritesScreen(navController = navController)
        }
        
        composable("chat_list") {
            ChatListScreen(navController = navController, currentRole = currentRole)
        }
        
        composable("admin_dashboard") {
            AdminScreen(
                navController = navController,
                currentRole = currentRole,
                activeAdminName = activeAdminName,
                onLogout = {
                    currentRole = UserRole.USER
                    activeAdminName = null
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
