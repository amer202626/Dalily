package com.dalily.services

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.dalily.services.data.FirebaseSimulator
import com.dalily.services.ui.screens.*
import com.dalily.services.ui.theme.DaliliTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize edge-to-edge and DB state in context
        enableEdgeToEdge()
        FirebaseSimulator.initialize(applicationContext)

        setContent {
            var isGuest by remember { mutableStateOf(true) }
            val systemSettings by FirebaseSimulator.systemSettings.collectAsState()
            val context = LocalContext.current

            DaliliTheme {
                // If Maintenance Mode is enabled globally, block regular users. Let Admin bypass.
                if (systemSettings.maintenanceMode && isGuest) {
                    MaintenanceBarrierScreen(
                        onBypassAdminClick = {
                            isGuest = false
                            FirebaseSimulator.currentUserIsAdmin = true
                            FirebaseSimulator.currentUserName = "الآدمن الرئيسي"
                            Toast.makeText(context, "التحول الأمني: تم تسجيل الدخول كمسؤول لتعديل لوحة التحكم", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // 1. Home Explorer Dashboard Route
                        composable("home") {
                            HomeScreen(
                                onNavigateToProvider = { prvId ->
                                    navController.navigate("provider_detail/$prvId")
                                },
                                onNavigateToAdmin = {
                                    if (isGuest) {
                                        // Auto-escalate representing quick authentication for this prototype app setting
                                        isGuest = false
                                        FirebaseSimulator.currentUserIsAdmin = true
                                        FirebaseSimulator.currentUserName = "الآدمن المطور"
                                        Toast.makeText(context, "تم رفع الصلاحيات لآدمن تلقائياً لتسهيل فحص لوحة الإعدادات 🛠️", Toast.LENGTH_LONG).show()
                                    }
                                    navController.navigate("admin")
                                },
                                onNavigateToChatsList = {
                                    navController.navigate("chats")
                                },
                                onNavigateToFavorites = {
                                    navController.navigate("favorites")
                                },
                                isGuest = isGuest,
                                onLoginClick = {
                                    isGuest = false
                                    FirebaseSimulator.currentUserIsAdmin = false
                                    FirebaseSimulator.currentUserName = "مرتجى اليماني"
                                    Toast.makeText(context, "تم تسجيل دخول مبرمج كـ (مرتجى اليماني) بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // 2. Service Provider Professional Profile Details Route
                        composable(
                            route = "provider_detail/{providerId}",
                            arguments = listOf(navArgument("providerId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                            DetailScreen(
                                providerId = providerId,
                                onBackClick = { navController.popBackStack() },
                                isGuest = isGuest,
                                onLoginClick = {
                                    isGuest = false
                                    FirebaseSimulator.currentUserIsAdmin = false
                                    FirebaseSimulator.currentUserName = "مرتجى اليماني"
                                    Toast.makeText(context, "تم تسجيل دخول مبرمج كـ (مرتجى اليماني) بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // 3. Super Admin Dashboard Parameters Route
                        composable("admin") {
                            AdminScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // 4. Admin Chats Monitoring Queue Route
                        composable("chats") {
                            ChatListScreen(
                                onBackClick = { navController.popBackStack() },
                                onNavigateToProvider = { prvId ->
                                    navController.navigate("provider_detail/$prvId")
                                }
                            )
                        }

                        // 5. Customer Labeled Favorites Collections Screen
                        composable("favorites") {
                            FavoritesScreen(
                                onBackClick = { navController.popBackStack() },
                                onNavigateToProvider = { prvId ->
                                    navController.navigate("provider_detail/$prvId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceBarrierScreen(
    onBypassAdminClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Construction,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = Color(0xFFF59E0B)
                )
                Text(
                    text = "وضعية الصيانة المجدولة نشطة حالياً 🛠️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Text(
                    text = "نجري حالياً مجموعة من التحديثات وقواعد البيانات التحتية الهامة لتشجيع ومواكبة سرعة دليل الخدمات اليمني.\nسنعود للعمل والتشغيل الكامل فورا!",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onBypassAdminClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    modifier = Modifier.fillMaxWidth().testTag("maintenance_bypass_admin_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("التحول الأمني لآدمن وتجاوز Barrier 🔓", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
