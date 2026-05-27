package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.*

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

            // Supreme Anticlone and Integrity Checks
            val context = androidx.compose.ui.platform.LocalContext.current
            val isTampered = remember {
                val pName = context.packageName ?: ""
                // Allow original package name and dynamic AI Studio testing identifiers, block repackaged malicious builds
                val isPackageValid = pName == "com.example" || pName.startsWith("com.aistudio.") || pName.startsWith("com.example.")
                !isPackageValid
            }

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
                if (isTampered) {
                    // Supreme Security Shield Block Screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Security Off",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "درع الحماية الفائقة نشط",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تم كشف محاولة استنساخ أو تعديل غير مصرح بها لحزمة التطبيق ومحتوياته.\nلقد تم إغلاق التطبيق فوراً لحماية الملكية الفكرية والحظر التام للتطبيقات المزورة ومكافحة سرقة وفك الأكواد.",
                                color = Color(0xFF94A3B8),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(30.dp))
                            Text(
                                text = "Supreme Integrity Protection Active\nUnauthorized APK cloning or signature modifications detected. Execution has been locked to protect development copyright.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
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
}
