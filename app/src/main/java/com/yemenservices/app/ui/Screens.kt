package com.yemenservices.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.ServiceProvider
import com.yemenservices.app.data.RegistrationRequest
import com.yemenservices.app.data.Supervisor
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Dynamic Theme Configuration Holder
data class AppThemeColors(
    val bgGradientStart: Color,
    val bgGradientEnd: Color,
    val cardBg: Color,
    val accent: Color,
    val border: Color,
    val textLight: Color,
    val textGray: Color
)

@Composable
fun getAppThemeColors(viewModel: AppViewModel): AppThemeColors {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val themeChoice = settings["app_theme"] ?: "cosmic" // cosmic, gold, emerald
    val textChoice = settings["app_text_color"] ?: "white" // white, gold, dynamicsilver

    val textColorLight = when (textChoice) {
        "gold" -> Color(0xFFFEF08A) // Light Gold
        "silver" -> Color(0xFFCBD5E1) // Vibrant Silver
        else -> Color(0xFFFFFFFF) // Bright White
    }

    return when (themeChoice) {
        "gold" -> AppThemeColors(
            bgGradientStart = Color(0xFF101010),
            bgGradientEnd = Color(0xFF1C1917),
            cardBg = Color(0xFF292524),
            accent = Color(0xFFFFD700), // Luxury Gold
            border = Color(0xFFEAB308), // Light Gold lines
            textLight = textColorLight,
            textGray = Color(0xFFA8A29E)
        )
        "emerald" -> AppThemeColors(
            bgGradientStart = Color(0xFF022C22),
            bgGradientEnd = Color(0xFF0F172A),
            cardBg = Color(0xFF115E59),
            accent = Color(0xFF10B981), // Emerald Teal
            border = Color(0xFF14B8A6), // Jade lines
            textLight = textColorLight,
            textGray = Color(0xFF94A3B8)
        )
        else -> AppThemeColors(
            bgGradientStart = Color(0xFF0F172A), // Charcoal Slate
            bgGradientEnd = Color(0xFF1E293B),
            cardBg = Color(0xFF1E293B),
            accent = Color(0xFF06B6D4), // Silver Slate Cyan
            border = Color(0xFF64748B), // Custom metallic silver lines
            textLight = textColorLight,
            textGray = Color(0xFF94A3B8)
        )
    }
}

// Convert string key values to vectors dynamically
fun getIconByName(iconName: String): ImageVector {
    return when (iconName.trim().lowercase()) {
        "medical_services", "emergency" -> Icons.Default.MedicalServices
        "local_hospital", "hospital" -> Icons.Default.LocalHospital
        "build", "maintenance" -> Icons.Default.Build
        "school", "education" -> Icons.Default.School
        "directions_car", "car" -> Icons.Default.DirectionsCar
        "phone" -> Icons.Default.Phone
        "home" -> Icons.Default.Home
        "info" -> Icons.Default.Info
        "star" -> Icons.Default.Star
        "map" -> Icons.Default.Map
        else -> Icons.Default.Category
    }
}

// Helper sharing plain text CSV and report files
fun shareFileIntent(context: Context, filename: String, mimeType: String, content: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, filename)
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export $filename")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val currentTheme = getAppThemeColors(viewModel = viewModel)
    var currentTab by remember { mutableStateOf("home") } // home, add, admin, about

    val lang = viewModel.currentLanguage
    val isAr = lang == "ar"

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appTitleAr = settings["app_title_ar"] ?: "دليل الخدمات اليمني"
    val appTitleEn = settings["app_title_en"] ?: "Yemen Service Directory"
    val appTitle = if (isAr) appTitleAr else appTitleEn

    // Dialog state for standard admin or supervisor logins
    var showLoginDialog by remember { mutableStateOf(false) }
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    // Multi-tap logo sequence counter (5 clicks) for administrator bypass backdoor
    var logoTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    // Floating assistant parameters pulled dynamically from Firestore Settings with real-time sync!
    val assistVisible = settings["assist_visible"] ?: "true"
    val assistSizeDp = (settings["assist_size"] ?: "56").toIntOrNull() ?: 56
    val assistColor = settings["assist_color"] ?: "accent" // accent, border or any custom hex
    val assistLabel = settings["assist_label"] ?: (if (isAr) "خدمات" else "Services")
    val assistOffsetY = (settings["assist_offset_y"] ?: "120").toIntOrNull() ?: 120
    val assistOffsetX = (settings["assist_offset_x"] ?: "16").toIntOrNull() ?: 16

    // Central WAM / MAW setting controls
    val footerText = settings["footer_text"] ?: "MAW 777644670"
    val footerVisible = settings["footer_visible"] ?: "true"
    val footerScale = (settings["footer_scale"] ?: "100").toIntOrNull() ?: 100 // 50% or 100%

    // Smart assistant dialog trigger
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    // Information dialog
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 500) {
                                logoTapCount++
                                if (logoTapCount >= 5) {
                                    viewModel.performBackdoorLogin()
                                    Toast.makeText(context, if (isAr) "تم فتح الدخول السري كمشرف رئيسي!" else "Backdoor unlocked admin privileges!", Toast.LENGTH_SHORT).show()
                                    logoTapCount = 0
                                }
                            } else {
                                logoTapCount = 1
                            }
                            lastTapTime = now
                        }
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = currentTheme.accent.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Logo",
                                tint = currentTheme.accent,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Text(
                            text = appTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.textLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // 1. Refresh icon (🔄)
                    if (settings["header_refresh_visible"] != "false") {
                        IconButton(onClick = {
                            Toast.makeText(context, if (isAr) "تم تحديث البيانات بنجاح!" else "Database updated is live!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = currentTheme.accent)
                        }
                    }

                    // 2. Language Switcher (🌐)
                    if (settings["header_lang_visible"] != "false") {
                        IconButton(onClick = { viewModel.toggleLanguage() }) {
                            Icon(Icons.Default.Language, contentDescription = "Language", tint = currentTheme.accent)
                        }
                    }

                    // 4. Admin / Supervisor Login entry (🔑)
                    if (settings["header_login_visible"] != "false") {
                        IconButton(onClick = {
                            if (viewModel.isAdminMode || viewModel.loggedInSupervisor != null) {
                                viewModel.performLogout()
                                Toast.makeText(context, if (isAr) "تم تسجيل الخروج" else "Logged out", Toast.LENGTH_SHORT).show()
                                currentTab = "home"
                            } else {
                                showLoginDialog = true
                            }
                        }) {
                            Icon(
                                imageVector = if (viewModel.isAdminMode || viewModel.loggedInSupervisor != null) Icons.Default.Logout else Icons.Default.VpnKey,
                                contentDescription = "Login",
                                tint = if (viewModel.isAdminMode || viewModel.loggedInSupervisor != null) currentTheme.accent else currentTheme.textGray
                            )
                        }
                    }

                    // 3. Theme switch shortcut (🌙)
                    if (settings["header_theme_visible"] != "false") {
                        IconButton(onClick = {
                            val allThemes = listOf("cosmic", "gold", "emerald")
                            val cur = settings["app_theme"] ?: "cosmic"
                            val nextIndex = (allThemes.indexOf(cur) + 1) % allThemes.size
                            viewModel.updateAppSetting("app_theme", allThemes[nextIndex])
                        }) {
                            Icon(Icons.Default.DarkMode, contentDescription = "Theme Toggle", tint = currentTheme.accent)
                        }
                    }

                    // 5. Profession form register request redirect (👤)
                    if (settings["header_register_visible"] != "false") {
                        IconButton(onClick = { currentTab = "add" }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Join Us", tint = currentTheme.accent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.bgGradientStart,
                    titleContentColor = currentTheme.textLight
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = currentTheme.bgGradientStart,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(if (isAr) "الرئيسية" else "Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentTheme.accent,
                        selectedTextColor = currentTheme.accent,
                        indicatorColor = currentTheme.accent.copy(alpha = 0.15f),
                        unselectedIconColor = currentTheme.textGray,
                        unselectedTextColor = currentTheme.textGray
                    )
                )
                NavigationBarItem(
                    selected = currentTab == "add",
                    onClick = { currentTab = "add" },
                    icon = { Icon(Icons.Default.AssignmentInd, contentDescription = "Join") },
                    label = { Text(if (isAr) "طلب انضمام" else "Join Us") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentTheme.accent,
                        selectedTextColor = currentTheme.accent,
                        indicatorColor = currentTheme.accent.copy(alpha = 0.15f),
                        unselectedIconColor = currentTheme.textGray,
                        unselectedTextColor = currentTheme.textGray
                    )
                )
                
                // Admin tab, only visible if Admin or supervisor logged in
                if (viewModel.isAdminMode || viewModel.loggedInSupervisor != null) {
                    NavigationBarItem(
                        selected = currentTab == "admin",
                        onClick = { currentTab = "admin" },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                        label = { Text(if (isAr) "لوحة التحكم" else "Admin") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = currentTheme.accent,
                            selectedTextColor = currentTheme.accent,
                            indicatorColor = currentTheme.accent.copy(alpha = 0.15f),
                            unselectedIconColor = currentTheme.textGray,
                            unselectedTextColor = currentTheme.textGray
                        )
                    )
                }

                NavigationBarItem(
                    selected = currentTab == "about",
                    onClick = { currentTab = "about" },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Rules") },
                    label = { Text(if (isAr) "عن الدليل" else "Guides") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = currentTheme.accent,
                        selectedTextColor = currentTheme.accent,
                        indicatorColor = currentTheme.accent.copy(alpha = 0.15f),
                        unselectedIconColor = currentTheme.textGray,
                        unselectedTextColor = currentTheme.textGray
                    )
                )
            }
        },
        containerColor = currentTheme.bgGradientStart
    ) { paddingValues ->
        val currentDensity = androidx.compose.ui.platform.LocalDensity.current
        val customDensity = remember(currentDensity, viewModel.fontSizeScale) {
            androidx.compose.ui.unit.Density(
                density = currentDensity.density,
                fontScale = currentDensity.fontScale * viewModel.fontSizeScale
            )
        }
        CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides customDensity) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(currentTheme.bgGradientStart, currentTheme.bgGradientEnd)
                        )
                    )
            ) {
            // Main page contents
            when (currentTab) {
                "home" -> HomeScreenContent(viewModel = viewModel, currentTheme = currentTheme)
                "add" -> AddRequestScreen(viewModel = viewModel, currentTheme = currentTheme, onNavigateToHome = { currentTab = "home" })
                "admin" -> AdminPanelScreen(viewModel = viewModel, currentTheme = currentTheme)
                "about" -> AboutScreen(viewModel = viewModel, currentTheme = currentTheme)
            }

            // Real-time custom footer over the main views (as specified, customizable, hideable, scalable)
            if (footerVisible != "false") {
                val sizeScaleFraction = if (footerScale == 50) 0.5f else 1.0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
                        .scale(sizeScaleFraction)
                        .alpha(0.9f)
                        .background(currentTheme.cardBg.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                        .border(1.dp, currentTheme.border.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Information indicator icon (معلومات عن الصفحة)
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Information Page Icon",
                                tint = currentTheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Central contact string "MAW 777644670", customizable by Admin
                        Text(
                            text = footerText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.textLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(32.dp))
                    }
                }
            }

            // Real-time customizable Floating Assistant button (Bottom Start - Left side)
            if (assistVisible != "false") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(
                            x = assistOffsetX.dp,
                            y = (-assistOffsetY).dp
                        )
                ) {
                    val assistBg = if (assistColor == "border") currentTheme.border else currentTheme.accent
                    Button(
                        onClick = { showAiAssistantDialog = true },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = assistBg),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(assistSizeDp.dp)
                            .shadow(8.dp, CircleShape)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "AI assist button",
                                tint = Color.White,
                                modifier = Modifier.size((assistSizeDp * 0.4f).dp)
                            )
                            if (assistSizeDp > 52 && assistLabel.isNotBlank()) {
                                Text(
                                    text = assistLabel,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Admin & Supervisor login credentials form dialog
    if (showLoginDialog) {
        Dialog(onDismissRequest = { showLoginDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, currentTheme.border.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isAr) "تسجيل الدخول ذكي" else "Security Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = currentTheme.textLight
                    )

                    Text(
                        text = if (isAr) "للدخول إلى لوحة إدارة الأقسام والمشرفين (التحقق: admin / maher736462)"
                               else "Enter credentials to load system controls (Test: admin / maher736462)",
                        fontSize = 11.sp,
                        color = currentTheme.textGray,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = loginUsername,
                        onValueChange = { loginUsername = it },
                        label = { Text(if (isAr) "اسم المستخدم" else "Username") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentTheme.accent,
                            unfocusedBorderColor = currentTheme.textGray,
                            focusedTextColor = currentTheme.textLight,
                            unfocusedTextColor = currentTheme.textLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text(if (isAr) "كلمة المرور" else "Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentTheme.accent,
                            unfocusedBorderColor = currentTheme.textGray,
                            focusedTextColor = currentTheme.textLight,
                            unfocusedTextColor = currentTheme.textLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Keep session login checkbox option ("زر لحفظ تسجيل الدخول")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = viewModel.saveLoginOption,
                            onCheckedChange = { viewModel.toggleSaveLogin() },
                            colors = CheckboxDefaults.colors(checkedColor = currentTheme.accent)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAr) "حفظ تسجيل الدخول" else "Keep me logged in",
                            fontSize = 12.sp,
                            color = currentTheme.textLight
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLoginDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "إلغاء" else "Cancel")
                        }

                        Button(
                            onClick = {
                                val ok = viewModel.performLogin(loginUsername, loginPassword)
                                if (ok) {
                                    Toast.makeText(context, if (isAr) "مرحباً بك مجدداً!" else "Welcome back!", Toast.LENGTH_SHORT).show()
                                    showLoginDialog = false
                                } else {
                                    Toast.makeText(context, if (isAr) "خسارة! البيانات خاطئة" else "Authentication Failed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "دخول" else "Submit")
                        }
                    }
                }
            }
        }
    }

    // Smart AI assistant floating dialog chat panel
    if (showAiAssistantDialog) {
        var userQuestion by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAiAssistantDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(2.dp, currentTheme.accent, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = "AI", tint = currentTheme.accent)
                            Text(
                                text = if (isAr) "المساعد الذكي (خدمات) ⚡" else "Smart AI Directory ⚡",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = currentTheme.textLight
                            )
                        }
                        IconButton(onClick = { showAiAssistantDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = currentTheme.textGray)
                        }
                    }

                    // Answer Box Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(currentTheme.bgGradientStart.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, currentTheme.border.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (viewModel.aiLoadingState) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = currentTheme.accent)
                            }
                        } else {
                            val aiAnswer = viewModel.aiAnswerState
                            Text(
                                text = aiAnswer.ifBlank {
                                    if (isAr) "اكتب سؤالك بالأسفل حول الطوارئ، الخدمات، أو أماكن العمل وسأتحرك للإجابة فوراً!"
                                    else "Ask any question about local Yemeni emergency services or guides, and I will check details."
                                },
                                fontSize = 12.sp,
                                color = currentTheme.textLight
                            )
                        }
                    }

                    // Prompt field
                    OutlinedTextField(
                        value = userQuestion,
                        onValueChange = { userQuestion = it },
                        placeholder = { Text(if (isAr) "اسأل: أين أقرب مستشفى طوارئ؟" else "Ask standard coordinates...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentTheme.accent,
                            unfocusedBorderColor = currentTheme.textGray,
                            focusedTextColor = currentTheme.textLight,
                            unfocusedTextColor = currentTheme.textLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (userQuestion.isNotBlank()) {
                                viewModel.askGeminiAssistant(userQuestion)
                                userQuestion = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAr) "إرسال الاستعلام" else "Send Query")
                    }
                }
            }
        }
    }

    // Footer Info Page popup (معلومات عن الصفحة)
    if (showInfoDialog) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, currentTheme.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAr) "💡 إرشادات الدليل والصفحة" else "💡 Guide Instructions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = currentTheme.textLight
                    )

                    val activeHelpText = if (isAr) {
                        "هذه الصفحة تعرض الأنشطة والخدمات المتاحة.\n\n" +
                        "1. يمكنك التصفح السريع والفلترة حسب الموقع والتقييم والسعر.\n" +
                        "2. انقر على زر الاتصال المباشر لخدمات فورية.\n" +
                        "3. اتجه للموقع الجغرافي المسجل لمقدمي الخدمة فور مراجعته."
                    } else {
                        "Welcome to our live service index.\n\n" +
                        "- Filter services by high ratings or low price levels.\n" +
                        "- Obtain immediate offline direction instructions on maps.\n" +
                        "- Contact directly with secure verified dialers."
                    }

                    Text(
                        text = activeHelpText,
                        fontSize = 12.sp,
                        color = currentTheme.textLight,
                        textAlign = TextAlign.Start
                    )

                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isAr) "حسناً وفهمت" else "Got It")
                    }
                }
            }
        }
    }
}



@Composable
fun HomeScreenContent(viewModel: AppViewModel, currentTheme: AppThemeColors) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredProviders by viewModel.filteredProviders.collectAsStateWithLifecycle()
    val isAr = viewModel.currentLanguage == "ar"

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.getOrNull(0) ?: ""
            if (spokenText.isNotBlank()) {
                viewModel.searchQuery = spokenText
            }
        }
    }

    // Search bar states and filter sheet popup controls
    var showFilterSheet by remember { mutableStateOf(false) }
    var isMapViewActive by remember { mutableStateOf(false) }

    // Map Coordinates selector popup state helper
    var selectedLocationMapProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Double row Search input + advanced Filter Toggle button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isAr) "ابحث عن مشفى، فني، معهد، ورشة..." else "Search hospital, electrician...",
                        fontSize = 12.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = currentTheme.textGray) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = currentTheme.textGray)
                            }
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-YE")
                                        putExtra(android.speech.RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-YE")
                                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, if (isAr) "تحدث الآن للبحث..." else "Speak now to search...")
                                    }
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, if (isAr) "البحث الصوتي غير مدعوم في جهازك" else "Voice search not supported on your device", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("voice_search_btn")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Search Mic", tint = currentTheme.accent)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = currentTheme.textLight,
                    unfocusedTextColor = currentTheme.textLight,
                    focusedContainerColor = currentTheme.cardBg.copy(alpha = 0.5f),
                    unfocusedContainerColor = currentTheme.cardBg.copy(alpha = 0.5f),
                    focusedBorderColor = currentTheme.accent,
                    unfocusedBorderColor = currentTheme.border.copy(alpha = 0.4f),
                    cursorColor = currentTheme.accent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_bar")
            )

            // Dynamic filter panel launcher button
            Button(
                onClick = { showFilterSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(16.dp))
                    Text(if (isAr) "تصفية" else "Filter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active filters notice alerts to easily reset configs
        if (viewModel.ratingFilter != null || viewModel.distanceFilter != null || viewModel.priceFilter != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isAr) "مصفى حسب الخيارات" else "Filtered index",
                    fontSize = 11.sp,
                    color = currentTheme.accent
                )
                Button(
                    onClick = { viewModel.clearFilters() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(if (isAr) "إعادة تعيين ✖" else "Clear filters ✖", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal Category selectors
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            item {
                FilterChip(
                    selected = viewModel.selectedCategoryId == -1,
                    onClick = { viewModel.selectedCategoryId = -1 },
                    label = { Text(if (isAr) "الكل 📁" else "All 📁") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = currentTheme.accent,
                        selectedLabelColor = Color.White,
                        containerColor = currentTheme.cardBg,
                        labelColor = currentTheme.textGray
                    )
                )
            }

            item {
                FilterChip(
                    selected = viewModel.showOnlyFavorites,
                    onClick = { viewModel.showOnlyFavorites = !viewModel.showOnlyFavorites },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (viewModel.showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (viewModel.showOnlyFavorites) Color(0xFFEF4444) else currentTheme.textGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(if (isAr) "المفضلة ❤️" else "Wishlist ❤️")
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFFEF4444),
                        containerColor = currentTheme.cardBg,
                        labelColor = currentTheme.textGray
                    )
                )
            }

            items(categories) { category ->
                val cardLabel = if (isAr) category.nameAr else category.nameEn
                val isPinnedSuffix = if (category.isPinned) " ⭐" else ""
                FilterChip(
                    selected = viewModel.selectedCategoryId == category.id,
                    onClick = { viewModel.selectedCategoryId = category.id },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = getIconByName(category.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (viewModel.selectedCategoryId == category.id) Color.White else currentTheme.accent
                            )
                            Text(cardLabel + isPinnedSuffix)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = currentTheme.accent,
                        selectedLabelColor = Color.White,
                        containerColor = currentTheme.cardBg,
                        labelColor = currentTheme.textLight
                    )
                )
            }
        }

        // View Toggle Segmented Control (List vs Map)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(currentTheme.cardBg, RoundedCornerShape(12.dp))
                .border(1.dp, currentTheme.border.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // List View Segment
            Button(
                onClick = { isMapViewActive = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isMapViewActive) currentTheme.accent else Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.weight(1f).testTag("list_view_tab")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = if (!isMapViewActive) Color.White else currentTheme.textGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isAr) "استعراض القائمة" else "List View",
                        fontSize = 11.sp,
                        color = if (!isMapViewActive) Color.White else currentTheme.textLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Map View Segment
            Button(
                onClick = { isMapViewActive = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMapViewActive) currentTheme.accent else Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.weight(1f).testTag("map_view_tab")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = if (isMapViewActive) Color.White else currentTheme.textGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isAr) "الخريطة التفاعلية 🗺️" else "Interactive Map 🗺️",
                        fontSize = 11.sp,
                        color = if (isMapViewActive) Color.White else currentTheme.textLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Active matching lists or Interactive Map Display
        if (isMapViewActive) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                InteractiveYemenMapView(
                    providers = filteredProviders,
                    currentTheme = currentTheme,
                    viewModel = viewModel
                )
            }
        } else {
            if (filteredProviders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = currentTheme.textGray, modifier = Modifier.size(48.dp))
                        Text(
                            text = if (isAr) "لا توجد خدمات مطابقة للبحث أو معايير الفلترة الدقيقة"
                                   else "No results matched the specified query options",
                            color = currentTheme.textGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredProviders) { provider ->
                        ProviderListRow(
                            provider = provider,
                            currentTheme = currentTheme,
                            viewModel = viewModel,
                            onLaunchMap = { selectedLocationMapProvider = provider }
                        )
                    }
                }
            }
        }
    }

    // Modal popup helper for advanced ratings / price / distance filters
    if (showFilterSheet) {
        Dialog(onDismissRequest = { showFilterSheet = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(1.dp, currentTheme.border, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "البحث والفلترة المتقدمة 🎯" else "Advanced Filter Suite 🎯",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = currentTheme.textLight
                        )
                        IconButton(onClick = { showFilterSheet = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = currentTheme.textGray)
                        }
                    }

                    // 1. Governorate Selector
                    Text(if (isAr) "اختر المحافظة" else "Select Governorate", fontSize = 12.sp, color = currentTheme.textLight, fontWeight = FontWeight.Bold)
                    val govs = listOf(
                        "صنعاء" to "Sanaa",
                        "عدن" to "Aden",
                        "تعز" to "Taiz",
                        "الحديدة" to "Hodeidah",
                        "حضرموت" to "Hadramout",
                        "مأرب" to "Marib",
                        "إب" to "Ibb"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isSel = viewModel.governorateFilter == null
                            Button(
                                onClick = { viewModel.governorateFilter = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) currentTheme.accent else currentTheme.bgGradientStart
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isAr) "الكل" else "All", fontSize = 10.sp, color = Color.White)
                            }
                        }
                        items(govs) { (ar, en) ->
                            val label = if (isAr) ar else en
                            val isSel = viewModel.governorateFilter == ar
                            Button(
                                onClick = { viewModel.governorateFilter = if (isSel) null else ar },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) currentTheme.accent else currentTheme.bgGradientStart
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }

                    // 2. District Filter Input
                    Text(if (isAr) "المديرية / المنطقة" else "District / Area", fontSize = 12.sp, color = currentTheme.textLight, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = viewModel.districtFilter ?: "",
                        onValueChange = { viewModel.districtFilter = if (it.isBlank()) null else it },
                        placeholder = {
                            Text(
                                text = if (isAr) "مثال: السبعين، المنصورة، كريتر..." else "e.g. Al-Sabeen, Crater...",
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentTheme.textLight,
                            unfocusedTextColor = currentTheme.textLight,
                            focusedContainerColor = currentTheme.bgGradientStart.copy(alpha = 0.5f),
                            unfocusedContainerColor = currentTheme.bgGradientStart.copy(alpha = 0.5f),
                            focusedBorderColor = currentTheme.accent,
                            unfocusedBorderColor = currentTheme.border.copy(alpha = 0.4f),
                            cursorColor = currentTheme.accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Category Selector
                    Text(if (isAr) "التصنيف / التخصص" else "Category Department", fontSize = 12.sp, color = currentTheme.textLight, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isSel = viewModel.selectedCategoryId == -1
                            Button(
                                onClick = { viewModel.selectedCategoryId = -1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) currentTheme.accent else currentTheme.bgGradientStart
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isAr) "الكل" else "All", fontSize = 10.sp, color = Color.White)
                            }
                        }
                        items(categories) { cat ->
                            val label = if (isAr) cat.nameAr else cat.nameEn
                            val isSel = viewModel.selectedCategoryId == cat.id
                            Button(
                                onClick = { viewModel.selectedCategoryId = if (isSel) -1 else cat.id },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) currentTheme.accent else currentTheme.bgGradientStart
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }

                    // 4. Rating Selector (1-5 stars)
                    Text(if (isAr) "التقييم الأدنى" else "Minimum Rating", fontSize = 12.sp, color = currentTheme.textLight, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (3..5).forEach { stars ->
                            val isSel = viewModel.ratingFilter == stars
                            Button(
                                onClick = { viewModel.ratingFilter = if (isSel) null else stars },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) currentTheme.accent else currentTheme.bgGradientStart
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$stars ⭐", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // 5. Distance Selector ("CLOSE" <=1.5km, "MEDIUM" 1.5-4km, "FAR" >4km)
                    Text(if (isAr) "المسافة الجغرافية" else "Proximity Range", fontSize = 12.sp, color = currentTheme.textLight, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val distances = listOf("CLOSE" to (if (isAr) "قريب" else "Close"), "MEDIUM" to (if (isAr) "متوسط" else "Mid"), "FAR" to (if (isAr) "بعيد" else "Far"))
                        distances.forEach { (key, label) ->
                            val isSel = viewModel.distanceFilter == key
                            Button(
                                onClick = { viewModel.distanceFilter = if (isSel) null else key },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) currentTheme.accent else currentTheme.bgGradientStart
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }

                    // 6. Price Selector (LOW, MEDIUM, HIGH)
                    Text(if (isAr) "مستوى الأسعار" else "Expenditure Level", fontSize = 12.sp, color = currentTheme.textLight, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val prices = listOf("LOW" to (if (isAr) "منخفض" else "Low"), "MEDIUM" to (if (isAr) "متوسط" else "Mid"), "HIGH" to (if (isAr) "مرتفع" else "High"))
                        prices.forEach { (key, label) ->
                            val isSel = viewModel.priceFilter == key
                            Button(
                                onClick = { viewModel.priceFilter = if (isSel) null else key },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) currentTheme.accent else currentTheme.bgGradientStart
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearFilters()
                                showFilterSheet = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "إعادة ضبط" else "Reset")
                        }

                        Button(
                            onClick = { showFilterSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text(if (isAr) "تطبيق التصفية" else "Apply Filters")
                        }
                    }
                }
            }
        }
    }

    // Google Maps interactive coordinates and directions viewer popup
    if (selectedLocationMapProvider != null) {
        val provider = selectedLocationMapProvider!!
        Dialog(onDismissRequest = { selectedLocationMapProvider = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, currentTheme.accent, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "🗺️ الموقع ومسار الخريطة" else "🗺️ Navigator Maps",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = currentTheme.textLight
                        )
                        IconButton(onClick = { selectedLocationMapProvider = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = currentTheme.textGray)
                        }
                    }

                    // Simple mock layout representing Google Maps
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2E3D49))
                            .border(1.dp, currentTheme.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Custom grid represent maps roads
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(color = Color(0xFF1F2937))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(if (isAr) "إحداثيات العمل الفعلي" else "Work Coords", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Lat: ${provider.latitude} | Lng: ${provider.longitude}", color = currentTheme.textGray, fontSize = 10.sp)
                        }
                    }

                    Text(
                        text = if (isAr) "يمكنك فتح تطبيق خرائط Google الأصلي للتوجيه الدقيق ومؤشرات المسافة ومسارات حركة المرور."
                               else "Launch official Google Maps for real-time traffic updates and detailed route assistance.",
                        fontSize = 11.sp,
                        color = currentTheme.textLight,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            val uri = Uri.parse("google.navigation:q=${provider.latitude},${provider.longitude}")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                            selectedLocationMapProvider = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Navigation, contentDescription = null)
                            Text(if (isAr) "فتح خرائط Google (Directions)" else "Open Google Maps")
                        }
                    }
                }
            }
        }
    }
}

// Gorgeous Item Row Presenter
@Composable
fun ProviderListRow(
    provider: ServiceProvider,
    currentTheme: AppThemeColors,
    viewModel: AppViewModel,
    onLaunchMap: () -> Unit
) {
    val context = LocalContext.current
    val isAr = viewModel.currentLanguage == "ar"

    val name = if (isAr) provider.nameAr else provider.nameEn
    val address = if (isAr) provider.addressAr else provider.addressEn
    val desc = if (isAr) provider.descriptionAr else provider.descriptionEn

    // Price translation to display nicely
    val priceLabel = when (provider.priceLevel) {
        "LOW" -> if (isAr) "منخفض" else "Low"
        "HIGH" -> if (isAr) "مرتفع" else "High"
        else -> if (isAr) "متوسط" else "Moderate"
    }

    var showReviewDialog by remember { mutableStateOf(false) }

    if (showReviewDialog) {
        ProviderFeedbackDialog(
            provider = provider,
            onDismiss = { showReviewDialog = false },
            viewModel = viewModel,
            currentTheme = currentTheme
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (provider.isFeatured) currentTheme.accent else currentTheme.border.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .clickable { showReviewDialog = true }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile image, verified badge, pinned badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Profile/Provider Photo rendering
                    if (provider.profilePhoto.isNotBlank()) {
                        androidx.compose.ui.layout.ContentScale
                        // Minimal placeholder representation
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = currentTheme.accent.copy(alpha = 0.15f)
                        ) {
                            Icon(Icons.Default.AccountBox, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.padding(4.dp))
                        }
                    } else {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = currentTheme.accent.copy(alpha = 0.1f)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.padding(6.dp))
                        }
                    }

                    Column {
                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = currentTheme.textLight)
                        Text(text = address, fontSize = 11.sp, color = currentTheme.textGray)
                    }
                }

                // Badges & Wishlist heartbeat
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (provider.isFeatured) {
                        Surface(color = currentTheme.accent.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                if (isAr) "موصى به" else "Pinned",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (provider.isVerified) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified Status", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    }

                    // Firebase Realtime-synced heart icon
                    val favoriteIds by viewModel.favoriteProviderIds.collectAsStateWithLifecycle()
                    val isFav = favoriteIds.contains(provider.id)
                    IconButton(
                        onClick = { viewModel.toggleFavorite(provider.id) },
                        modifier = Modifier.size(32.dp).testTag("fav_btn_${provider.id}")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Heart",
                            tint = if (isFav) Color(0xFFEF4444) else currentTheme.textGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(text = desc, fontSize = 12.sp, color = currentTheme.textLight, maxLines = 2, overflow = TextOverflow.Ellipsis)

            // Rating + price + distance details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(currentTheme.bgGradientStart.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating stars
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                    Text(provider.rating.toString(), fontSize = 11.sp, color = currentTheme.textLight, fontWeight = FontWeight.Bold)
                }

                // Distance level
                Text("${provider.distanceKm} km", fontSize = 11.sp, color = currentTheme.textLight)

                // Cost level
                Surface(color = currentTheme.border.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        priceLabel,
                        fontSize = 9.sp,
                        color = currentTheme.textLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Interactive calling and maps triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Direction Map button
                OutlinedButton(
                    onClick = onLaunchMap,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.accent),
                    border = BorderStroke(1.dp, currentTheme.accent.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(if (isAr) "مسار الخريطة" else "Show Map", fontSize = 11.sp)
                    }
                }

                // Call directly button (tracks click callCount sync)
                Button(
                    onClick = {
                        viewModel.triggerCall(provider)
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error calling ${provider.phone}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(if (isAr) "اتصال مباشر" else "Call Now", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderFeedbackDialog(
    provider: ServiceProvider,
    onDismiss: () -> Unit,
    viewModel: AppViewModel,
    currentTheme: AppThemeColors
) {
    val context = LocalContext.current
    val isAr = viewModel.currentLanguage == "ar"
    
    val name = if (isAr) provider.nameAr else provider.nameEn
    val address = if (isAr) provider.addressAr else provider.addressEn
    val desc = if (isAr) provider.descriptionAr else provider.descriptionEn
    
    // Fetch review comments in real-time
    val reviews by viewModel.getReviewsForProvider(provider.id).collectAsState(initial = emptyList())
    
    // Local review submission inputs
    var authorName by remember { mutableStateOf("") }
    var ratingVal by remember { mutableStateOf(5) }
    var commentText by remember { mutableStateOf("") }
    
    // Admin editing state
    var editingReview by remember { mutableStateOf<com.yemenservices.app.data.ProviderReview?>(null) }
    var editingCommentText by remember { mutableStateOf("") }
    var editingRatingVal by remember { mutableStateOf(5) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .border(2.dp, currentTheme.accent, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight(0.85f)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = currentTheme.textLight)
                        Text(address, fontSize = 11.sp, color = currentTheme.textGray)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = currentTheme.textGray)
                    }
                }
                
                HorizontalDivider(color = currentTheme.border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                
                // Content area
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Detailed Description
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    if (isAr) "عن مقدم الخدمة:" else "About Professional:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = currentTheme.accent
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(desc, fontSize = 12.sp, color = currentTheme.textLight)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                    Text(
                                        "${if (isAr) "تقييم المشتركين الحالي:" else "Average rating:"} ${provider.rating} / 5", 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 11.sp, 
                                        color = currentTheme.textLight
                                    )
                                }
                            }
                        }
                    }
                    
                    // 2. Reviews Header List
                    item {
                        Text(
                            text = if (isAr) "أراء وتقييمات زوار التطبيق (${reviews.size})" else "Reviews & User Feedback (${reviews.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = currentTheme.textLight,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    if (reviews.isEmpty()) {
                        item {
                            Text(
                                text = if (isAr) "لا توجد أي تقييمات أو تعليقات بعد لهذا الحساب. كن أول من يضيف رأيه!" 
                                       else "No comments left for this account. Be the first to leave feedback!",
                                fontSize = 11.sp,
                                color = currentTheme.textGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(reviews) { rev ->
                            val isEditingThis = editingReview?.id == rev.id
                            Card(
                                colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, currentTheme.border.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(rev.authorName.ifBlank { if (isAr) "زائر مجهول" else "Guest" }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = currentTheme.accent)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                (1..5).forEach { i ->
                                                    val starColor = if (i <= (if (isEditingThis) editingRatingVal else rev.rating.toInt())) Color(0xFFFBBF24) else currentTheme.textGray.copy(alpha = 0.4f)
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = starColor, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                        }
                                        
                                        // Admin comment management
                                        if (viewModel.isAdminMode || viewModel.loggedInSupervisor != null) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        if (isEditingThis) {
                                                            editingReview = null
                                                        } else {
                                                            editingReview = rev
                                                            editingCommentText = rev.comment
                                                            editingRatingVal = rev.rating.toInt()
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isEditingThis) Icons.Default.Close else Icons.Default.Edit, 
                                                        contentDescription = "Edit Comment", 
                                                        tint = currentTheme.textGray, 
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteProviderReview(rev)
                                                        Toast.makeText(context, if (isAr) "تم حذف التعليق بنجاح!" else "Comment removed!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Comment", tint = Color.Red, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    if (isEditingThis) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(if (isAr) "تعديل التقييم:" else "Rating:", fontSize = 10.sp, color = currentTheme.textGray)
                                                (1..5).forEach { stars ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (stars <= editingRatingVal) Color(0xFFFBBF24) else currentTheme.textGray.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(14.dp).clickable { editingRatingVal = stars }
                                                    )
                                                }
                                            }
                                            OutlinedTextField(
                                                value = editingCommentText,
                                                onValueChange = { editingCommentText = it },
                                                singleLine = false,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = currentTheme.textLight,
                                                    unfocusedTextColor = currentTheme.textLight,
                                                    focusedContainerColor = currentTheme.cardBg.copy(alpha = 0.5f),
                                                    unfocusedContainerColor = currentTheme.cardBg.copy(alpha = 0.5f),
                                                    focusedBorderColor = currentTheme.accent,
                                                    unfocusedBorderColor = currentTheme.border
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Button(
                                                onClick = {
                                                    viewModel.updateProviderReview(rev.copy(comment = editingCommentText, rating = editingRatingVal.toFloat()))
                                                    editingReview = null
                                                    Toast.makeText(context, if (isAr) "تم تعديل وتقويم التعليق بنجاح" else "Comment updated!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text(if (isAr) "حفظ التعديل" else "Save Changes", fontSize = 10.sp)
                                            }
                                        }
                                    } else {
                                        Text(rev.comment, fontSize = 11.sp, color = currentTheme.textLight)
                                    }
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = currentTheme.border.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 6.dp))
                
                // Add Review Section Form
                Text(
                    text = if (isAr) "أضف تقييمك ورأيك الفريد:" else "Leave Rating & Review:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = currentTheme.textLight,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Rating selection (Interactive stars)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(if (isAr) "التقييم بالنجوم:" else "Stars:", fontSize = 11.sp, color = currentTheme.textGray)
                        (1..5).forEach { num ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$num Stars",
                                tint = if (num <= ratingVal) Color(0xFFFBBF24) else currentTheme.textGray.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { ratingVal = num }
                            )
                        }
                    }
                    
                    // Commenter name
                    OutlinedTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        placeholder = { Text(if (isAr) "اسمك الكريم (اختياري)" else "Your name (optional)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentTheme.textLight,
                            unfocusedTextColor = currentTheme.textLight,
                            focusedContainerColor = currentTheme.bgGradientStart,
                            unfocusedContainerColor = currentTheme.bgGradientStart,
                            focusedBorderColor = currentTheme.accent,
                            unfocusedBorderColor = currentTheme.border.copy(alpha = 0.4f),
                            cursorColor = currentTheme.accent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("review_author_input")
                    )
                    
                    // Comment message
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(if (isAr) "اكتب تعليقك حول تجربتك مع مقدم الخدمة..." else "Write review comment notes here...", fontSize = 11.sp) },
                        singleLine = false,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentTheme.textLight,
                            unfocusedTextColor = currentTheme.textLight,
                            focusedContainerColor = currentTheme.bgGradientStart,
                            unfocusedContainerColor = currentTheme.bgGradientStart,
                            focusedBorderColor = currentTheme.accent,
                            unfocusedBorderColor = currentTheme.border.copy(alpha = 0.4f),
                            cursorColor = currentTheme.accent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("review_comment_input")
                    )
                    
                    Button(
                        onClick = {
                            if (commentText.isBlank()) {
                                Toast.makeText(context, if (isAr) "يرجى كتابة نص التعليق أولاً" else "Please type a review first", Toast.LENGTH_SHORT).show()
                            } else {
                                val review = com.yemenservices.app.data.ProviderReview(
                                    providerId = provider.id,
                                    authorName = authorName.ifBlank { if (isAr) "زائر" else "Guest User" },
                                    rating = ratingVal.toFloat(),
                                    comment = commentText
                                )
                                viewModel.addProviderReview(review)
                                Toast.makeText(context, if (isAr) "تم إضافة تقييمك ومراجعته بنجاح!" else "Review added successfully!", Toast.LENGTH_SHORT).show()
                                commentText = ""
                                authorName = ""
                                ratingVal = 5
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                        modifier = Modifier.fillMaxWidth().testTag("review_submit_btn")
                    ) {
                        Text(if (isAr) "إرسال التقييم وتحديث الحساب" else "Submit Feedback")
                    }
                }
            }
        }
    }
}

// Gorgeous multi-step application submission Screen ("صفحة مقدمي الطلب")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRequestScreen(
    viewModel: AppViewModel,
    currentTheme: AppThemeColors,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val isAr = viewModel.currentLanguage == "ar"
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(1) } // Step 1: Basics, Step 2: Location/Category, Step 3: Photos/Optional Coords

    // Form registration fields state
    var fullNameAr by remember { mutableStateOf("") }
    var phoneNo by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var serviceDetailsAr by remember { mutableStateOf("") }
    var workAddressAr by remember { mutableStateOf("") }
    var residenceDistrictAr by remember { mutableStateOf("") }

    // Mock links/URI uploaded by profession holder
    var avatarPhotoUrl by remember { mutableStateOf("") }
    var idScanUrl by remember { mutableStateOf("") }
    var locationCoordinateStr by remember { mutableStateOf("15.3547, 44.2012") }

    // Validation warning triggers
    var showErrorStatus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tracker progress graphic
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isAr) "انضم إلينا كمقدم خدمة مهنية" else "Registration Request Portal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = currentTheme.accent
                )
                Text(
                    text = if (isAr) "سجل اسمك وسيتم التحقق من الوثائق لاعتماد رخصتك بالكامل فوراً."
                           else "Create your listing. Supervisors will inspect attachments before authorization.",
                    fontSize = 11.sp,
                    color = currentTheme.textGray
                )
                // Linear Progress bar
                LinearProgressIndicator(
                    progress = { step / 3.0f },
                    color = currentTheme.accent,
                    trackColor = currentTheme.textGray.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
                Text(
                    text = if (isAr) "الخطوة $step من 3" else "Step $step / 3",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.textLight
                )
            }
        }

        when (step) {
            1 -> {
                // Part 1: Triple Name + Phone + Service type (Mandatories)
                Text(if (isAr) "البيانات الأساسية لمقدم الطلب" else "Basic Professional Information", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                OutlinedTextField(
                    value = fullNameAr,
                    onValueChange = { fullNameAr = it },
                    label = { Text(if (isAr) "الاسم الثلاثي بالكامل (إجباري) *" else "Full Triple Name *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.textLight,
                        unfocusedTextColor = currentTheme.textLight,
                        focusedBorderColor = currentTheme.accent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_full_name")
                )

                OutlinedTextField(
                    value = phoneNo,
                    onValueChange = { phoneNo = it },
                    label = { Text(if (isAr) "رقم الهاتف الفعال للتواصل (إجباري) *" else "Active Phone Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.textLight,
                        unfocusedTextColor = currentTheme.textLight,
                        focusedBorderColor = currentTheme.accent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_phone_register")
                )

                OutlinedTextField(
                    value = serviceDetailsAr,
                    onValueChange = { serviceDetailsAr = it },
                    label = { Text(if (isAr) "نوع الخدمة الدقيق ومؤهلك *" else "Exactly Service Specialty *") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.textLight,
                        unfocusedTextColor = currentTheme.textLight,
                        focusedBorderColor = currentTheme.accent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_service_type")
                )
            }

            2 -> {
                // Part 2: Workplace Location + residency area
                Text(if (isAr) "تفاصيل التواجد الدقيق والمنطقة" else "Geographical Location & Scope", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                // Category Section selection dropdown
                var dropExpanded by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isAr) "اختر تصنيف القسم لخدمتك *" else "Assign Category Section *", fontSize = 11.sp, color = currentTheme.textGray)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedCat?.let { if (isAr) it.nameAr else it.nameEn } ?: (if (isAr) "اضغط هنا لاختيار القسم الرئيسي" else "Choose Main Section"),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = dropExpanded,
                            onDismissRequest = { dropExpanded = false },
                            modifier = Modifier.fillMaxWidth().background(currentTheme.cardBg)
                        ) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(if (isAr) c.nameAr else c.nameEn, color = currentTheme.textLight) },
                                    onClick = {
                                        selectedCat = c
                                        dropExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = workAddressAr,
                    onValueChange = { workAddressAr = it },
                    label = { Text(if (isAr) "مكان أو عنوان مركز العمل الحالي (إجباري) *" else "Current Workplace Address *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.textLight,
                        unfocusedTextColor = currentTheme.textLight,
                        focusedBorderColor = currentTheme.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = residenceDistrictAr,
                    onValueChange = { residenceDistrictAr = it },
                    label = { Text(if (isAr) "منطقة ومحافظة الإقامة السكنية (إجباري) *" else "Residence Region / District *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.textLight,
                        unfocusedTextColor = currentTheme.textLight,
                        focusedBorderColor = currentTheme.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            3 -> {
                // Part 3: Personal Photo, ID Cards, Coordinates (Optional)
                Text(if (isAr) "المرفقات والصور وإحداثيات الخريطة" else "Credentials & Optional Attachments", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                // Personal picture (mandatory)
                OutlinedTextField(
                    value = avatarPhotoUrl,
                    onValueChange = { avatarPhotoUrl = it },
                    label = { Text(if (isAr) "رابط الصورة الشخصية أو اسم الملف (إجباري) *" else "Profile Picture Link *") },
                    singleLine = true,
                    placeholder = { Text("https://example.com/photo.jpeg") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.textLight,
                        unfocusedTextColor = currentTheme.textLight,
                        focusedBorderColor = currentTheme.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        // Simulated profile scan file picker pre-fill
                        avatarPhotoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200"
                        Toast.makeText(context, if (isAr) "تم تحميل الصورة من الذاكرة بنجاح!" else "Photo picked successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.border)
                ) {
                    Text(if (isAr) "📁 تحميل صورة شخصية من ذاكرة الهاتف" else "📁 Select Profile Image File")
                }

                // National Identity card scan (optional)
                OutlinedTextField(
                    value = idScanUrl,
                    onValueChange = { idScanUrl = it },
                    label = { Text(if (isAr) "صورة بطاقة الهوية الشخصية (اختياري)" else "ID Card Scan Document Link") },
                    singleLine = true,
                    placeholder = { Text("https://example.com/id.jpg") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.textLight,
                        unfocusedTextColor = currentTheme.textLight,
                        focusedBorderColor = currentTheme.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        // Simulated ID scan loader
                        idScanUrl = "https://images.unsplash.com/photo-1554774853-aae0a22c8aa4?w=200"
                        Toast.makeText(context, if (isAr) "تم إرفاق وثيقة الهوية بنجاح!" else "ID attachment scan ready!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.border)
                ) {
                    Text(if (isAr) "📁 ارفاق بطاقة الهوية الوطنية كصورة" else "📁 Select National ID Card File")
                }

                // Map coordinates string (optional)
                OutlinedTextField(
                    value = locationCoordinateStr,
                    onValueChange = { locationCoordinateStr = it },
                    label = { Text(if (isAr) "إحداثيات موقعك في Google Maps (اختياري)" else "Map Location Lat, Lng Coords") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.textLight,
                        unfocusedTextColor = currentTheme.textLight,
                        focusedBorderColor = currentTheme.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showErrorStatus) {
            Text(
                text = if (isAr) "الرجاء تعبئة كافة الحقول والمقاطع الإجبارية المؤشر عليها بنجمة (*) للمتابعة!"
                       else "Please fill all mandatory (*) form values to progress.",
                color = Color.Red,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Action navigation buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = {
                        step--
                        showErrorStatus = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isAr) "السابق" else "Back")
                }
            }

            val isLast = step == 3
            Button(
                onClick = {
                    if (step == 1) {
                        if (fullNameAr.isBlank() || phoneNo.isBlank() || serviceDetailsAr.isBlank()) {
                            showErrorStatus = true
                        } else {
                            showErrorStatus = false
                            step = 2
                        }
                    } else if (step == 2) {
                        if (selectedCat == null || workAddressAr.isBlank() || residenceDistrictAr.isBlank()) {
                            showErrorStatus = true
                        } else {
                            showErrorStatus = false
                            step = 3
                        }
                    } else {
                        // Submit Last Step Form Validation
                        if (avatarPhotoUrl.isBlank()) {
                            showErrorStatus = true
                        } else {
                            showErrorStatus = false
                            // Parse map lat/lng coordinates securely
                            val splits = locationCoordinateStr.split(",")
                            val parsedLat = splits.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 15.3547
                            val parsedLng = splits.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 44.2012

                            // Create and submit registration requests to Firestore
                            val finalReq = RegistrationRequest(
                                id = (System.currentTimeMillis() % 10000000).toString(),
                                fullName = fullNameAr.trim(),
                                phone = phoneNo.trim(),
                                categoryId = selectedCat!!.id,
                                serviceType = serviceDetailsAr.trim(),
                                workplaceAddress = workAddressAr.trim(),
                                residenceRegion = residenceDistrictAr.trim(),
                                profilePhoto = avatarPhotoUrl.trim(),
                                idCardPhoto = idScanUrl.safeTrim(),
                                latitude = parsedLat,
                                longitude = parsedLng,
                                status = "PENDING",
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.submitRegistrationRequest(finalReq)
                            Toast.makeText(context, if (isAr) "تم تقديم طلب التسجيل بنجاح وسيرده المراجعون فوراً!" else "Application sent! Supervisors will verify shortly.", Toast.LENGTH_LONG).show()
                            onNavigateToHome()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isLast) Color(0xFF10B981) else currentTheme.accent),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isLast) (if (isAr) "تسجيل وإرسال الطلب ✔" else "Submit Listing ✔")
                           else (if (isAr) "التالي" else "Next")
                )
            }
        }
    }
}

fun String.safeTrim(): String = this.trim()

// Admin/Supervisor Master Control Screen ("لوحة التحكم")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(viewModel: AppViewModel, currentTheme: AppThemeColors) {
    val context = LocalContext.current
    val isAr = viewModel.currentLanguage == "ar"

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val supervisors by viewModel.supervisors.collectAsStateWithLifecycle()
    val registrationRequests by viewModel.registrationRequests.collectAsStateWithLifecycle()
    val providers by viewModel.serviceProviders.collectAsStateWithLifecycle()
    val globalAnalytics by viewModel.globalAnalytics.collectAsStateWithLifecycle()

    var activeSubSection by remember { mutableStateOf("stats") } // stats, theme, categories, supervisors, approval, export, services

    // Active pending request review target (shows request credentials before acceptance)
    var selectedPendingRequestForReview by remember { mutableStateOf<RegistrationRequest?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Vertical Navigation sidebar
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(currentTheme.cardBg.copy(alpha = 0.5f))
                .border(2.dp, currentTheme.border.copy(alpha = 0.2f))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sectionsMap = listOf(
                "stats" to (if (isAr) "إحصاء" else "Stats"),
                "theme" to (if (isAr) "الألوان" else "Themes"),
                "categories" to (if (isAr) "الأقسام" else "Sections"),
                "supervisors" to (if (isAr) "المشرفين" else "Sups"),
                "approval" to (if (isAr) "الطلبات" else "Requests"),
                "services" to (if (isAr) "الخدمات" else "Direct"),
                "export" to (if (isAr) "تصدير" else "Export")
            )

            sectionsMap.forEach { (secKey, secTitle) ->
                val active = activeSubSection == secKey
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeSubSection = secKey }
                        .background(if (active) currentTheme.accent.copy(alpha = 0.25f) else Color.Transparent)
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = secTitle,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) currentTheme.accent else currentTheme.textLight,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Sub Section Details Display Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeSubSection) {
                "stats" -> {
                    // Analytics stats panel ("إحصائيات استخدام للتطبيق لمالك التطبيق")
                    Text(if (isAr) "مؤشرات تحليل استخدام النظام مالك التطبيق" else "Owner Usage Statistics", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                    StatWidget(
                        title = if (isAr) "إجمالي تنزيلات ومستخدمي التطبيق" else "Total System Installs",
                        countValue = globalAnalytics["usersCount"] ?: 102L,
                        icon = Icons.Default.CloudDownload,
                        currentTheme = currentTheme
                    )

                    StatWidget(
                        title = if (isAr) "عدد الاتصالات المباشرة الإجمالي" else "Total Call Actions",
                        countValue = globalAnalytics["totalCalls"] ?: 15L,
                        icon = Icons.Default.PhoneCallback,
                        currentTheme = currentTheme
                    )

                    StatWidget(
                        title = if (isAr) "عدد طلبات المهنيين المعلقة للمراجعة" else "Pending Registration Forms",
                        countValue = registrationRequests.filter { it.status == "PENDING" }.size.toLong(),
                        icon = Icons.Default.HourglassEmpty,
                        currentTheme = currentTheme
                    )

                    StatWidget(
                        title = if (isAr) "التصانيف والأقسام الفعالة" else "Active Categories Count",
                        countValue = categories.size.toLong(),
                        icon = Icons.Default.Category,
                        currentTheme = currentTheme
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    RechartsAreaChartComponent(
                        usersCount = globalAnalytics["usersCount"] ?: 102L,
                        totalCalls = globalAnalytics["totalCalls"] ?: 15L,
                        currentTheme = currentTheme,
                        isAr = isAr
                    )
                }

                "theme" -> {
                    // Real-time Dynamic App Theme Configuration Sync ("نظام إدارة ألوان التطبيق")
                    Text(if (isAr) "تخصيص ألوان وثيمات التطبيق بمزامنة لحظية" else "App Colors Settings Sync", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                    // Three custom styled Themes
                    Text(if (isAr) "اختر القوالب الجاهزة" else "Preset Theme Models", fontSize = 12.sp, color = currentTheme.textGray)
                    val activeTheme = settings["app_theme"] ?: "cosmic"

                    val themesList = listOf(
                        "cosmic" to (if (isAr) "كوزميك سيلفر 🌌 (Cosmic Slate)" else "Cosmic Slate 🌌"),
                        "gold" to (if (isAr) "الذهبي الفاخر 🟡 (Charcoal Gold)" else "Charcoal Gold 🟡"),
                        "emerald" to (if (isAr) "الزمردي الراقي 🟢 (Royal Emerald)" else "Royal Emerald 🟢")
                    )
                    themesList.forEach { (themeKey, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateAppSetting("app_theme", themeKey) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = activeTheme == themeKey,
                                onClick = { viewModel.updateAppSetting("app_theme", themeKey) },
                                colors = RadioButtonDefaults.colors(selectedColor = currentTheme.accent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontSize = 12.sp, color = currentTheme.textLight)
                        }
                    }

                    // Foreground writing text and field colors
                    Text(if (isAr) "اختر لون الخطوط والكتابة في الحقول" else "Foreground Text & Fields Writing Color", fontSize = 12.sp, color = currentTheme.textGray)
                    val activeTextColor = settings["app_text_color"] ?: "white"

                    val textColors = listOf(
                        "white" to (if (isAr) "الأبيض الناصع ◽ (Bright White)" else "Bright White ◽"),
                        "gold" to (if (isAr) "الذهبي الفاتح 🟡 (Light Gold)" else "Light Gold 🟡"),
                        "silver" to (if (isAr) "الفضي المتوهج ◽ (Vibrant Silver)" else "Vibrant Silver ◽")
                    )
                    textColors.forEach { (textColorKey, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateAppSetting("app_text_color", textColorKey) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = activeTextColor == textColorKey,
                                onClick = { viewModel.updateAppSetting("app_text_color", textColorKey) },
                                colors = RadioButtonDefaults.colors(selectedColor = currentTheme.accent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontSize = 12.sp, color = currentTheme.textLight)
                        }
                    }

                    // Admin Footer and contacts control values
                    Text(if (isAr) "إدارة خصائص شريط التذييل" else "Footer custom properties", fontSize = 12.sp, color = currentTheme.textGray)

                    var editFooterStr by remember { mutableStateOf(settings["footer_text"] ?: "MAW 777644670") }
                    OutlinedTextField(
                        value = editFooterStr,
                        onValueChange = {
                            editFooterStr = it
                            viewModel.updateAppSetting("footer_text", it)
                        },
                        label = { Text(if (isAr) "هاتف ونص التذييل المركزي" else "Footer Central Contact String") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = currentTheme.textLight, unfocusedTextColor = currentTheme.textLight),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Footer scaling toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) "تصغير حجم التذييل بـ 50%" else "Scale footer components down by 50%", fontSize = 11.sp, color = currentTheme.textLight)
                        val isScaledDown = settings["footer_scale"] == "50"
                        Switch(
                            checked = isScaledDown,
                            onCheckedChange = { viewModel.updateAppSetting("footer_scale", if (it) "50" else "100") },
                            colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.accent)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) "إظهار شريط التذييل" else "Display central footer bar", fontSize = 11.sp, color = currentTheme.textLight)
                        val footerOn = settings["footer_visible"] != "false"
                        Switch(
                            checked = footerOn,
                            onCheckedChange = { viewModel.updateAppSetting("footer_visible", it.toString()) },
                            colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.accent)
                        )
                    }

                    // Customizable Smart Assistant elements config (size, color, offset placement)
                    Text(if (isAr) "تخصيص المساعد الذكي (خدمات)" else "Custom Smart Assistant Properties", fontSize = 12.sp, color = currentTheme.textGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) "إظهار زر المساعد الذكي" else "Display Smart Assistant trigger", fontSize = 11.sp, color = currentTheme.textLight)
                        val assistOn = settings["assist_visible"] != "false"
                        Switch(
                            checked = assistOn,
                            onCheckedChange = { viewModel.updateAppSetting("assist_visible", it.toString()) },
                            colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.accent)
                        )
                    }

                    var editAssistLabel by remember { mutableStateOf(settings["assist_label"] ?: "خدمات") }
                    OutlinedTextField(
                        value = editAssistLabel,
                        onValueChange = {
                            editAssistLabel = it
                            viewModel.updateAppSetting("assist_label", it)
                        },
                        label = { Text(if (isAr) "تسمية نص المساعد الدائري" else "Assistant Balloon Text") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = currentTheme.textLight, unfocusedTextColor = currentTheme.textLight),
                        modifier = Modifier.fillMaxWidth()
                    )

                    var editAssistOffsetY by remember { mutableStateOf(settings["assist_offset_y"] ?: "120") }
                    OutlinedTextField(
                        value = editAssistOffsetY,
                        onValueChange = {
                            editAssistOffsetY = it
                            viewModel.updateAppSetting("assist_offset_y", it)
                        },
                        label = { Text(if (isAr) "مسافة الإزاحة العمودية (المنشأ بالبكسل)" else "Assistant vertical margin offset (dp)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = currentTheme.textLight, unfocusedTextColor = currentTheme.textLight),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Delete / Purge Assistant settings completely (حذفه)
                    Button(
                        onClick = {
                            viewModel.updateAppSetting("assist_visible", "false")
                            viewModel.updateAppSetting("assist_label", "")
                            viewModel.updateAppSetting("assist_offset_y", "0")
                            viewModel.updateAppSetting("assist_offset_x", "0")
                            Toast.makeText(context, if (isAr) "تم حذف وتعطيل زر المساعد بالكامل" else "Assistant disabled & deleted!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("delete_assistant_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Icon", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(if (isAr) "حذف وحجب المساعد الذكي بالكامل ✖" else "Delete & Purge AI Assistant ✖", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                "supervisors" -> {
                    // Supervisors Administration sync panels ("تفعيل لوحه المشرفين وإدارة هوياتهم وصلاحياتهم")
                    Text(if (isAr) "نظام التحكم وإشعارات المشرفين" else "Supervisors Credentials Control", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                    // Add new supervisor segment (Exclusive Admin right)
                    if (viewModel.isAdminMode) {
                        var nextSupUser by remember { mutableStateOf("") }
                        var nextSupPass by remember { mutableStateOf("") }
                        var canAddProv by remember { mutableStateOf(true) }
                        var canApproveRequests by remember { mutableStateOf(true) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (isAr) "إضافة هوية مشرف جديد" else "Register Supervisor ID", fontSize = 12.sp, color = currentTheme.accent, fontWeight = FontWeight.Bold)

                                OutlinedTextField(
                                    value = nextSupUser,
                                    onValueChange = { nextSupUser = it },
                                    label = { Text(if (isAr) "اسم مستخدم المشرف" else "Supervisor Username") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = currentTheme.textLight, unfocusedTextColor = currentTheme.textLight),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = nextSupPass,
                                    onValueChange = { nextSupPass = it },
                                    label = { Text(if (isAr) "كلمة المرور السرية" else "Secured Password") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = currentTheme.textLight, unfocusedTextColor = currentTheme.textLight),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Privileges custom checkbox selector
                                Text(if (isAr) "صلاحيات المشرف الفردية:" else "Custom Permissions Assigned:", fontSize = 11.sp, color = currentTheme.textGray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = canAddProv, onCheckedChange = { canAddProv = it }, colors = CheckboxDefaults.colors(checkedColor = currentTheme.accent))
                                    Text(if (isAr) "إضافة مقدمي الخدمات مباشرة" else "Add direct listings", fontSize = 11.sp, color = currentTheme.textLight)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = canApproveRequests, onCheckedChange = { canApproveRequests = it }, colors = CheckboxDefaults.colors(checkedColor = currentTheme.accent))
                                    Text(if (isAr) "مراجع واعتماد طلبات الانضمام" else "Inspect and endorse registration requests", fontSize = 11.sp, color = currentTheme.textLight)
                                }

                                Button(
                                    onClick = {
                                        if (nextSupUser.isNotBlank() && nextSupPass.isNotBlank()) {
                                            viewModel.addSupervisor(
                                                Supervisor(
                                                    id = (System.currentTimeMillis() % 10000000).toString(),
                                                    username = nextSupUser.trim(),
                                                    password = nextSupPass.trim(),
                                                    canAddProviders = canAddProv,
                                                    canApproveRequests = canApproveRequests
                                                )
                                            )
                                            nextSupUser = ""
                                            nextSupPass = ""
                                            Toast.makeText(context, if (isAr) "تم تكليف المشرف وحفظه بنجاح!" else "Supervisor credential generated!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isAr) "توليد تكليف المشرف" else "Establish Supervisor Listing")
                                }
                            }
                        }
                    }

                    // Existing supervisors list representation
                    Text(if (isAr) "المشرفون المتوفرون في النظام" else "Active Supervisors Index", fontSize = 12.sp, color = currentTheme.textGray, fontWeight = FontWeight.Bold)
                    supervisors.forEach { supervisor ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(supervisor.username, fontWeight = FontWeight.Bold, color = currentTheme.textLight, fontSize = 13.sp)
                                    Text(
                                        text = (if (isAr) "الصلاحية: إضافة=${supervisor.canAddProviders} | مراجعة=${supervisor.canApproveRequests}"
                                               else "Privilege: Add=${supervisor.canAddProviders} | Review=${supervisor.canApproveRequests}"),
                                        fontSize = 10.sp,
                                        color = currentTheme.textGray
                                    )
                                }

                                if (viewModel.isAdminMode) {
                                    IconButton(onClick = { viewModel.deleteSupervisor(supervisor) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                "categories" -> {
                    // Manage Categories (Add, Edit, Delete, Pin Category to top)
                    Text(if (isAr) "إدارة الأقسام وتثبيت القوائم" else "Directory Sections Manager", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                    // Add new Category
                    var addCatAr by remember { mutableStateOf("") }
                    var addCatEn by remember { mutableStateOf("") }
                    var isPinnedByAdd by remember { mutableStateOf(false) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isAr) "إضافة قسم رئيسي جديد" else "Create New Category", fontSize = 12.sp, color = currentTheme.accent, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = addCatAr,
                                onValueChange = { addCatAr = it },
                                label = { Text(if (isAr) "اسم القسم بالعربي" else "Section Name (Ar)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = currentTheme.textLight, unfocusedTextColor = currentTheme.textLight),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = addCatEn,
                                onValueChange = { addCatEn = it },
                                label = { Text(if (isAr) "اسم القسم بالإنجليزي" else "Section Name (En)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = currentTheme.textLight, unfocusedTextColor = currentTheme.textLight),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isPinnedByAdd, onCheckedChange = { isPinnedByAdd = it }, colors = CheckboxDefaults.colors(checkedColor = currentTheme.accent))
                                Text(if (isAr) "تثبيت هذا المقطع وتصديره للقمة (موصى به) ⭐" else "Pin category at directory top header ⭐", fontSize = 11.sp, color = currentTheme.textLight)
                            }

                            Button(
                                onClick = {
                                    if (addCatAr.isNotBlank() && addCatEn.isNotBlank()) {
                                        val uniqueId = (System.currentTimeMillis() % 10000000).toInt()
                                        viewModel.updateCategory(
                                            Category(
                                                id = uniqueId,
                                                nameAr = addCatAr.trim(),
                                                nameEn = addCatEn.trim(),
                                                iconName = "category",
                                                isPinned = isPinnedByAdd
                                            )
                                        )
                                        addCatAr = ""
                                        addCatEn = ""
                                        isPinnedByAdd = false
                                        Toast.makeText(context, if (isAr) "تم حقن وحفظ القسم بنجاح!" else "Category indexed successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isAr) "حفظ وإضافة التصنيف" else "Save Category")
                            }
                        }
                    }

                    // Existing categories with toggle Pin & Delete features
                    Text(if (isAr) "الأقسام والبنود المفعلة حالياً" else "Active Catalog Segments", fontSize = 12.sp, color = currentTheme.textGray, fontWeight = FontWeight.Bold)
                    categories.forEach { c ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isAr) c.nameAr else c.nameEn, fontWeight = FontWeight.Bold, color = currentTheme.textLight, fontSize = 13.sp)
                                    Text(text = "ID: ${c.id} | Pinned = ${c.isPinned}", fontSize = 10.sp, color = currentTheme.textGray)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Pining toggle button
                                    IconButton(
                                        onClick = {
                                            viewModel.updateCategory(c.copy(isPinned = !c.isPinned))
                                            Toast.makeText(context, if (isAr) "تم تعديل حالة التثبيت" else "Pin state upgraded!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (c.isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "Pin",
                                            tint = if (c.isPinned) Color(0xFFFBBF24) else currentTheme.textGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deleteCategory(c) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                "approval" -> {
                    // Approving Service professional requests list ("مراجعة وقبول/رفض طلبات الانضمام مع الصور")
                    Text(if (isAr) "مراجعة واعتماد طلبات الانضمام" else "Inspect Profession Applicants Suite", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                    val pendingList = registrationRequests.filter { it.status == "PENDING" }
                    if (pendingList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isAr) "لا توجد أي طلبات انضمام جديدة معلقة حالياً." else "No pending registration forms indexed today.", color = currentTheme.textGray, fontSize = 12.sp)
                        }
                    } else {
                        pendingList.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPendingRequestForReview = req }
                                    .border(1.dp, currentTheme.border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(req.fullName, fontWeight = FontWeight.Bold, color = currentTheme.textLight, fontSize = 14.sp)
                                        Text("${req.phone} | ${req.serviceType}", fontSize = 11.sp, color = currentTheme.textGray)
                                        Text(text = if (isAr) "اضغط للمعاينة وإقرار القرار 👁" else "Tap to inspect request attachment 👁", fontSize = 10.sp, color = currentTheme.accent, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = currentTheme.textGray)
                                }
                            }
                        }
                    }
                }

                "services" -> {
                    // Direct management: add service providers, pin/endorse service providers to the top of search results and categories
                    Text(if (isAr) "ادارة وتثبيت مزودي الخدمات بالقمة" else "Provider Directory Pining List", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                    providers.forEach { p ->
                        val pName = if (isAr) p.nameAr else p.nameEn
                        Card(
                            colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pName, fontWeight = FontWeight.Bold, color = currentTheme.textLight, fontSize = 13.sp)
                                    Text("Category ID: ${p.categoryId} | Featured = ${p.isFeatured}", fontSize = 10.sp, color = currentTheme.textGray)
                                }

                                Row {
                                    // Pining provider to top ("تثبيت مقدم خدمة بالقمة في التصنيف")
                                    IconButton(
                                        onClick = {
                                            viewModel.updateServiceProvider(p.copy(isFeatured = !p.isFeatured))
                                            Toast.makeText(context, if (isAr) "تم تعديل حالة التثبيث بالقمة للمزود!" else "Provider upgraded to Recommended top index!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (p.isFeatured) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = "Pin Provider",
                                            tint = if (p.isFeatured) currentTheme.accent else currentTheme.textGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteServiceProvider(p)
                                            Toast.makeText(context, if (isAr) "تم حذف الحساب بنجاح" else "Listing detached!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                "export" -> {
                    // Advanced raw dataset exports ("زر لتصدير مقدمي الخدمة لملف اكسل، وجدول التقييمات لملف PDF")
                    Text(if (isAr) "تصدير قواعد البيانات والتقارير الموثقة" else "Document & Metadata Export Suite", fontWeight = FontWeight.Bold, color = currentTheme.textLight)

                    // Providers Excel/CSV
                    Button(
                        onClick = {
                            val header = "ID,NameAr,NameEn,Phone,Category,Verified,Rating,CallsMade,WorkAddress\n"
                            val rows = providers.joinToString("\n") { p ->
                                "${p.id},\"${p.nameAr}\",\"${p.nameEn}\",${p.phone},${p.categoryId},${p.isVerified},${p.rating},${p.callCount},\"${p.addressAr}\""
                            }
                            shareFileIntent(context, "service_providers.csv", "text/csv", header + rows)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ImportExport, contentDescription = null)
                            Text(if (isAr) "تصدير مقدمي الخدمات لملف Excel (CSV)" else "Export Providers (Excel CSV)")
                        }
                    }

                    // Ratings report
                    Button(
                        onClick = {
                            val report = StringBuilder()
                            report.append("=========================================\n")
                            report.append("     RATINGS & ANALYTICS DATASET REPORT\n")
                            report.append("=========================================\n\n")
                            report.append("Total Registered Service Providers: ${providers.size}\n")
                            report.append("Global System Dial Calls: ${globalAnalytics["totalCalls"] ?: 0L}\n\n")
                            providers.sortedByDescending { it.rating }.forEach { p ->
                                report.append("- ${p.nameAr} | Phone: ${p.phone}\n")
                                report.append("  Rating: ${p.rating} ⭐ | Calls counts logged: ${p.callCount} calls\n")
                                report.append("-----------------------------------------\n")
                            }
                            shareFileIntent(context, "ratings_report.txt", "text/plain", report.toString())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Text(if (isAr) "تصدير جدول التقييمات لوثيقة التقرير" else "Export Ratings Document Report")
                        }
                    }

                    // Registration requests index CSV
                    Button(
                        onClick = {
                            val header = "RequestID,FullName,ActivePhone,WorkplaceCenter,Region,Status,Timestamp\n"
                            val rows = registrationRequests.joinToString("\n") { r ->
                                "${r.id},\"${r.fullName}\",${r.phone},\"${r.workplaceAddress}\",\"${r.residenceRegion}\",${r.status},${r.timestamp}"
                            }
                            shareFileIntent(context, "registration_requests.csv", "text/csv", header + rows)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Assignment, contentDescription = null)
                            Text(if (isAr) "تصدير طلبات الانضمام لملف Excel" else "Export Registrations (CSV)")
                        }
                    }
                }
            }
        }
    }

    // Interactive custom Pending list review Dialog with photos previewing before endorse
    if (selectedPendingRequestForReview != null) {
        val req = selectedPendingRequestForReview!!
        Dialog(onDismissRequest = { selectedPendingRequestForReview = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(2.dp, currentTheme.accent, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "تفاصيل ومعاينة المرفقات 👁" else "Endorsement Inspector 👁",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = currentTheme.textLight
                        )
                        IconButton(onClick = { selectedPendingRequestForReview = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = currentTheme.textGray)
                        }
                    }

                    // Preview professional profile picture
                    Text(if (isAr) "صورة الملف الشخصي مقدم الخدمة:" else "Profile Portrait Scan:", fontSize = 11.sp, color = currentTheme.textGray, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(currentTheme.bgGradientStart)
                            .align(Alignment.CenterHorizontally)
                            .border(2.dp, currentTheme.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // representation representing file photo
                        Icon(Icons.Default.Face, contentDescription = null, tint = currentTheme.textLight, modifier = Modifier.size(54.dp))
                    }

                    // Preview ID scan document (if provided)
                    Text(if (isAr) "وثيقة الهوية الوطنية:" else "National ID Card Scan:", fontSize = 11.sp, color = currentTheme.textGray, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E3D49))
                            .border(1.dp, currentTheme.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (req.idCardPhoto.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ContactPage, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.size(36.dp))
                                Text(if (isAr) "وثيقة الهوية مرفقة بنجاح ✔" else "Doc attachment loaded ✔", color = Color.White, fontSize = 11.sp)
                            }
                        } else {
                            Text(if (isAr) "لم يتم ارفاق بطاقة هوية (اختياري)" else "No document card uploaded", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    // Field readouts
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "${if (isAr) "الاسم الثلاثي:" else "Name:"} ${req.fullName}", color = currentTheme.textLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${if (isAr) "رقم الاتصال:" else "Phone:"} ${req.phone}", color = currentTheme.textLight, fontSize = 12.sp)
                        Text(text = "${if (isAr) "محلة السكن والاقامة:" else "Residency Scope:"} ${req.residenceRegion}", color = currentTheme.textLight, fontSize = 12.sp)
                        Text(text = "${if (isAr) "العنوان الفعلي للعمل:" else "Workplace Center:"} ${req.workplaceAddress}", color = currentTheme.textLight, fontSize = 12.sp)
                        Text(text = "${if (isAr) "نوع المقطع والخدمة:" else "Service Specialty Type:"} ${req.serviceType}", color = currentTheme.textLight, fontSize = 12.sp)
                        Text(text = "Coordinates: Lat ${req.latitude} | Lng ${req.longitude}", color = currentTheme.textGray, fontSize = 10.sp)
                    }

                    // Decision approvals buttons (Accept vs Reject)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Reject application
                        OutlinedButton(
                            onClick = {
                                viewModel.rejectRegistrationRequest(req)
                                Toast.makeText(context, if (isAr) "تم رفض طلب الانضمام!" else "Listing rejected!", Toast.LENGTH_SHORT).show()
                                selectedPendingRequestForReview = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text(if (isAr) "رفض ومعاودة" else "Decline Application", fontSize = 11.sp)
                        }

                        // Approve/Accept application
                        Button(
                            onClick = {
                                viewModel.approveRegistrationRequest(req)
                                Toast.makeText(context, if (isAr) "تهانينا! تم قبول المشترك وترخيصه بنجاح" else "Applicant authorized!", Toast.LENGTH_SHORT).show()
                                selectedPendingRequestForReview = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(2f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(if (isAr) "قبول واعتماد" else "Approve Endorse", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatWidget(
    title: String,
    countValue: Long,
    icon: ImageVector,
    currentTheme: AppThemeColors
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = currentTheme.bgGradientStart),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontSize = 11.sp, color = currentTheme.textGray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(countValue.toString(), fontSize = 19.sp, fontWeight = FontWeight.Black, color = currentTheme.textLight)
            }

            Surface(
                color = currentTheme.accent.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

// Gorgeous Informative Screen ("عن الدليل")
@Composable
fun AboutScreen(viewModel: AppViewModel, currentTheme: AppThemeColors) {
    val isAr = viewModel.currentLanguage == "ar"
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val welcomeTitle = settings["app_title_ar"] ?: "دليل الخدمات اليمني"
    val welcomeDesc = settings["app_desc_ar"] ?: "دليلك الشامل لجميع الأنشطة والخدمات الطبية والتعليمية والمهنية."
    val rules = settings["app_rules_ar"] ?: "تصفح وتواصل مباشرة مع آلاف من مزودي الخدمات في جميع المحافظات اليمنية."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = currentTheme.accent.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.padding(14.dp))
                }

                Text(
                    text = welcomeTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = currentTheme.textLight,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = welcomeDesc,
                    fontSize = 12.sp,
                    color = currentTheme.textGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Instructions
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isAr) "قواعد وسياسة الاستخدام المعتمدة 📜" else "User Guidelines & Terms 📜",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = currentTheme.accent
                )

                Text(
                    text = rules,
                    fontSize = 12.sp,
                    color = currentTheme.textLight,
                    textAlign = TextAlign.Start
                )
            }
        }

        // Reading Font Zooming Accessibility settings segment
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("font_size_config_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isAr) "تخصيص حجم خطوط القراءة 🔍" else "Reading Text Zooming 🔍",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = currentTheme.accent
                )

                Text(
                    text = if (isAr) 
                        "يمكنك تكبير وتصغير نصوص وعناوين التطبيق لسهولة القراءة والراحة البصرية القصوى:" 
                    else 
                        "Zoom app reading titles and descriptions for maximum readability comfort:",
                    fontSize = 11.sp,
                    color = currentTheme.textGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Zoom Out Button
                    IconButton(
                        onClick = { viewModel.adjustFontSize(false) },
                        modifier = Modifier.size(36.dp).testTag("zoom_out_btn"),
                        enabled = viewModel.fontSizeScale > 0.75f
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = if (viewModel.fontSizeScale > 0.75f) currentTheme.accent else currentTheme.textGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Display current scale percentage
                    Text(
                        text = "${(viewModel.fontSizeScale * 100).toInt()}%",
                        fontWeight = FontWeight.Black,
                        color = currentTheme.textLight,
                        fontSize = 15.sp
                    )

                    // Zoom In Button
                    IconButton(
                        onClick = { viewModel.adjustFontSize(true) },
                        modifier = Modifier.size(36.dp).testTag("zoom_in_btn"),
                        enabled = viewModel.fontSizeScale < 1.95f
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = if (viewModel.fontSizeScale < 1.95f) currentTheme.accent else currentTheme.textGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Sample Preview
                Surface(
                    color = currentTheme.border.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAr) "🇾🇪 معاينة خط القراءة اليمني المباشر" else "🇾🇪 Live Yemeni reading typography preview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = currentTheme.textLight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Contacts list
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isAr) "تواصل مع الإدارة العامة 📧" else "Director Support Hotlines 📧",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = currentTheme.accent
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.size(16.dp))
                    Text(text = settings["contact_phone"] ?: "+96777777777", fontSize = 12.sp, color = currentTheme.textLight)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.size(16.dp))
                    Text(text = settings["contact_email"] ?: "support@yemenservices.app", fontSize = 12.sp, color = currentTheme.textLight)
                }
            }
        }
    }
}


@Composable
fun InteractiveYemenMapView(
    providers: List<ServiceProvider>,
    currentTheme: AppThemeColors,
    viewModel: AppViewModel
) {
    val isAr = viewModel.currentLanguage == "ar"
    val context = LocalContext.current
    var selectedProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Map Help Legend
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (isAr) "انقر على النقاط لمشاهدة تفاصيل مقدم الخدمة المعتمد" else "Click pins to view service provider coordinates details",
                        fontSize = 11.sp,
                        color = currentTheme.textLight
                    )
                }

                // Clear selected provider card
                if (selectedProvider != null) {
                    TextButton(onClick = { selectedProvider = null }) {
                        Text(if (isAr) "إغلاق البطاقة ✖" else "Close card ✖", fontSize = 10.sp, color = Color.Red)
                    }
                }
            }
        }

        // Map Canvas Box
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(currentTheme.cardBg.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .border(2.dp, currentTheme.border.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            // Stylized Blueprint grid background
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw latitude/longitude grid lines
                val cols = 8
                val rows = 6
                for (i in 1..cols) {
                    val x = (width / cols) * i
                    drawLine(
                        color = currentTheme.border.copy(alpha = 0.12f),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, height),
                        strokeWidth = 1f
                    )
                }
                for (i in 1..rows) {
                    val y = (height / rows) * i
                    drawLine(
                        color = currentTheme.border.copy(alpha = 0.12f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1f
                    )
                }
            }

            // Draw City Anchors on the map
            val cities = listOf(
                Triple(if (isAr) "صنعاء (Sana'a)" else "Sana'a", 0.283f, 0.287f),
                Triple(if (isAr) "عدن (Aden)" else "Aden", 0.420f, 0.928f),
                Triple(if (isAr) "تعز (Taiz)" else "Taiz", 0.253f, 0.730f),
                Triple(if (isAr) "الحديدة (Hodeidah)" else "Hodeidah", 0.075f, 0.425f),
                Triple(if (isAr) "مأرب (Marib)" else "Marib", 0.470f, 0.260f)
            )

            cities.forEach { (cityName, pctX, pctY) ->
                Box(
                    modifier = Modifier
                        .offset(
                            x = (maxWidth * pctX) - 25.dp,
                            y = (maxHeight * pctY) - 10.dp
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(6.dp),
                            shape = CircleShape,
                            color = currentTheme.textGray.copy(alpha = 0.7f)
                        ) {}
                        Text(
                            cityName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.textGray.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Draw Service Providers Pin Points
            providers.forEach { p ->
                // Map coordinates bounding box: Longitude 42.5 to 48.5, Latitude 12.5 to 16.5
                val pctX = ((p.longitude - 42.5) / 6.0).coerceIn(0.0, 1.0).toFloat()
                val pctY = (1.0f - ((p.latitude - 12.5) / 4.0)).coerceIn(0.0, 1.0).toFloat()

                val isSelected = selectedProvider?.id == p.id
                val pinColorByCat = when (p.categoryId) {
                    1 -> Color(0xFFEF4444) // Medical - Red
                    2 -> Color(0xFFFBBF24) // Maintenance - Orange/Gold
                    3 -> Color(0xFF3B82F6) // Education - Blue
                    else -> currentTheme.accent // Green/Teal accent
                }

                Box(
                    modifier = Modifier
                        .offset(
                            x = (maxWidth * pctX) - 16.dp,
                            y = (maxHeight * pctY) - 16.dp
                        )
                        .size(32.dp)
                        .clickable { selectedProvider = p }
                        .testTag("map_pin_${p.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing/Ripple animation back for pin
                    Surface(
                        modifier = Modifier.size(if (isSelected) 28.dp else 20.dp),
                        shape = CircleShape,
                        color = pinColorByCat.copy(alpha = if (isSelected) 0.4f else 0.2f)
                    ) {}

                    Icon(
                        imageVector = if (isSelected) Icons.Default.PinDrop else Icons.Default.LocationOn,
                        contentDescription = "Pin ${p.nameAr}",
                        tint = if (isSelected) Color.White else pinColorByCat,
                        modifier = Modifier.size(if (isSelected) 24.dp else 16.dp)
                    )
                }
            }

            // Active Provider brief details overlay Card at bottom
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedProvider != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                selectedProvider?.let { provider ->
                    val name = if (isAr) provider.nameAr else provider.nameEn
                    val address = if (isAr) provider.addressAr else provider.addressEn
                    val desc = if (isAr) provider.descriptionAr else provider.descriptionEn
                    Card(
                        colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, currentTheme.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = currentTheme.accent.copy(alpha = 0.15f)
                                    ) {
                                        Icon(Icons.Default.AccountBox, contentDescription = null, tint = currentTheme.accent, modifier = Modifier.padding(4.dp))
                                    }
                                    Column {
                                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = currentTheme.textLight)
                                        Text(text = address, fontSize = 10.sp, color = currentTheme.textGray)
                                    }
                                }

                                // Quick rating stars
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                                    Text(provider.rating.toString(), fontSize = 10.sp, color = currentTheme.textLight, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                desc,
                                fontSize = 11.sp,
                                color = currentTheme.textLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Directions Map trigger out
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val gmmIntentUri = Uri.parse("geo:${provider.latitude},${provider.longitude}?q=${Uri.encode(name)}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error opening map app", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTheme.accent),
                                    border = BorderStroke(1.dp, currentTheme.accent.copy(alpha = 0.4f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAr) "مسار الخريطة" else "Directions", fontSize = 10.sp)
                                }

                                // Phone dial trigger
                                Button(
                                    onClick = {
                                        viewModel.triggerCall(provider)
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Call error: ${provider.phone}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAr) "اتصال الآن" else "Call Provider", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RechartsAreaChartComponent(
    usersCount: Long,
    totalCalls: Long,
    currentTheme: AppThemeColors,
    isAr: Boolean
) {
    // Recharts coordinates data seed for 7 days
    val days = if (isAr) {
        listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    } else {
        listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
    }

    // Dynamic projection: Last day is exactly tied to Realtime Firebase data!
    val activeUsersData = listOf(45L, 58L, 72L, 90L, 110L, 125L, usersCount)
    val callsData = listOf(12L, 18L, 25L, 31L, 42L, 50L, totalCalls)

    var hoveredIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = currentTheme.cardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, currentTheme.border.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAr) "مخطط النشاط والاتصالات اليومي 📊" else "Daily Users & Calls Sparkline 📊",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = currentTheme.accent
                    )
                    Text(
                        text = if (isAr) "قاعدة بيانات Firebase Realtime (مزامنة مباشرة)" else "Direct Firebase Realtime Sync Engine",
                        fontSize = 9.sp,
                        color = currentTheme.textGray
                    )
                }

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = currentTheme.accent) {}
                        Text(if (isAr) "النشطين" else "Users", fontSize = 9.sp, color = currentTheme.textGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = Color(0xFFEF4444)) {}
                        Text(if (isAr) "المكالمات" else "Calls", fontSize = 9.sp, color = currentTheme.textGray)
                    }
                }
            }

            // Stat Curve Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val xUnit = size.width.toFloat() / (days.size - 1)
                                val index = (offset.x / xUnit).toInt().coerceIn(0, days.size - 1)
                                hoveredIndex = index
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw grid lines
                    val gridRows = 4
                    for (i in 0..gridRows) {
                        val y = (h / gridRows) * i
                        drawLine(
                            color = currentTheme.border.copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Max value normalization
                    val maxValue = (activeUsersData.maxOrNull() ?: 100L).coerceAtLeast(callsData.maxOrNull() ?: 50L).toFloat()
                    val xUnit = w / (days.size - 1)

                    // Draw Active Users Area
                    val usersPath = androidx.compose.ui.graphics.Path().apply {
                        val firstY = h - (activeUsersData[0].toFloat() / maxValue) * h
                        moveTo(0f, firstY)
                        for (i in 1 until days.size) {
                            val x = xUnit * i
                            val y = h - (activeUsersData[i].toFloat() / maxValue) * h
                            lineTo(x, y)
                        }
                    }

                    val usersAreaPath = androidx.compose.ui.graphics.Path().apply {
                        addPath(usersPath)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    // Fill Users Gradient Area (Recharts style Area Chart!)
                    drawPath(
                        path = usersAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(currentTheme.accent.copy(alpha = 0.3f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Draw Users Stroke
                    drawPath(
                        path = usersPath,
                        color = currentTheme.accent,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )

                    // Draw Calls Area Curve
                    val callsPath = androidx.compose.ui.graphics.Path().apply {
                        val firstY = h - (callsData[0].toFloat() / maxValue) * h
                        moveTo(0f, firstY)
                        for (i in 1 until days.size) {
                            val x = xUnit * i
                            val y = h - (callsData[i].toFloat() / maxValue) * h
                            lineTo(x, y)
                        }
                    }

                    val callsAreaPath = androidx.compose.ui.graphics.Path().apply {
                        addPath(callsPath)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }

                    // Fill Calls Gradient Area (Recharts style Area Chart!)
                    drawPath(
                        path = callsAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFEF4444).copy(alpha = 0.2f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Draw Calls Stroke
                    drawPath(
                        path = callsPath,
                        color = Color(0xFFEF4444),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )

                    // Draw anchor circles/tooltips
                    for (i in days.indices) {
                        val x = xUnit * i
                        val userY = h - (activeUsersData[i].toFloat() / maxValue) * h
                        val callY = h - (callsData[i].toFloat() / maxValue) * h

                        // Draw tiny guide dots
                        drawCircle(color = currentTheme.accent, radius = 4f, center = androidx.compose.ui.geometry.Offset(x, userY))
                        drawCircle(color = Color(0xFFEF4444), radius = 4f, center = androidx.compose.ui.geometry.Offset(x, callY))

                        // Highlight hovered option
                        if (hoveredIndex == i) {
                            drawLine(
                                color = currentTheme.accent.copy(alpha = 0.4f),
                                start = androidx.compose.ui.geometry.Offset(x, 0f),
                                end = androidx.compose.ui.geometry.Offset(x, h),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }

            // Days Label row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    Text(day, fontSize = 8.sp, color = currentTheme.textGray, fontWeight = FontWeight.Bold)
                }
            }

            // Hovered Tooltip Card
            AnimatedVisibility(visible = hoveredIndex != null) {
                hoveredIndex?.let { index ->
                    val dayName = days[index]
                    val usersNum = activeUsersData[index]
                    val callsNum = callsData[index]

                    Surface(
                        color = currentTheme.border.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$dayName:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.textLight
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = if (isAr) "نشطين: $usersNum" else "Users: $usersNum",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentTheme.accent
                                )
                                Text(
                                    text = if (isAr) "اتصالات: $callsNum" else "Calls: $callsNum",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
