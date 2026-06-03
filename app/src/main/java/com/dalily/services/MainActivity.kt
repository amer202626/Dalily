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
        
        // Initialize the local-and-cloud database
        FirebaseSimulator.init(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            DalilyTheme {
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
    
    // Shared state for logged in role
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
                onRoleChanged = { role, adminName ->
                    currentRole = role
                    activeAdminName = adminName
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
                currentRole = currentRole
            )
        }
        
        composable("favorites") {
            FavoritesScreen(navController = navController)
        }
        
        composable("chat_list") {
            ChatListScreen(navController = navController)
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
