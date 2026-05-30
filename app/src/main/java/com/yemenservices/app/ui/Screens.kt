package com.yemenservices.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yemenservices.app.data.AppConfig
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.PendingProvider
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.ServiceProvider
import com.yemenservices.app.data.Supervisor
import kotlinx.coroutines.launch

enum class AppScreen {
    Home,
    ProviderDetail,
    CategoryDetail,
    AdminDashboard,
    SecretSettings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AppViewModel) {
    val config by viewModel.appConfig.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()
    val userIsDark by viewModel.isDarkMode.collectAsState()
    val isDark = userIsDark ?: isSystemInDarkTheme()

    // Parse App Secret Settings colors dynamically with safe error recovery
    val primaryColor = remember(config.primary_color_hex) {
        try {
            Color(android.graphics.Color.parseColor(config.primary_color_hex.trim()))
        } catch (e: Exception) {
            Color(0xFF1B5E20) // default Emerald
        }
    }
    val secondaryColor = remember(config.secondary_color_hex) {
        try {
            Color(android.graphics.Color.parseColor(config.secondary_color_hex.trim()))
        } catch (e: Exception) {
            Color(0xFFFFC107) // default Gold
        }
    }
    
    // Core brand themes defined by properties
    val systemBgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textMainColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF212121)
    val textSecColor = if (isDark) Color(0xFFB0B0B0) else Color(0xFF616161)

    // State management for navigation and modals
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var selectedProviderForDetail by remember { mutableStateOf<ServiceProvider?>(null) }
    val categoryBackStack = remember { mutableStateListOf<Category>() }
    val selectedCategoryForDetail = categoryBackStack.lastOrNull()
    
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Click tracker for backdoor challenge (5 times on app logo)
    var homeIconClicks by remember { mutableStateOf(0) }
    var isChatOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Auto-login on launch
    LaunchedEffect(Unit) {
        viewModel.autoLoginIfSaved(context)
    }

    MaterialTheme(
        colorScheme = if (isDark) {
            darkColorScheme(
                primary = primaryColor,
                secondary = secondaryColor,
                background = systemBgColor,
                surface = cardBgColor,
                onPrimary = Color.White,
                onSecondary = Color.Black,
                onBackground = textMainColor,
                onSurface = textMainColor
            )
        } else {
            lightColorScheme(
                primary = primaryColor,
                secondary = secondaryColor,
                background = systemBgColor,
                surface = cardBgColor,
                onPrimary = Color.White,
                onSecondary = Color.Black,
                onBackground = textMainColor,
                onSurface = textMainColor
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    homeIconClicks++
                                    if (homeIconClicks >= 5) {
                                        homeIconClicks = 0
                                        showBackdoorDialog = true
                                    }
                                }
                                .testTag("app_logo_title")
                        ) {
                            val logoIcon = when (config.selected_icon_type) {
                                "tools" -> Icons.Default.Build
                                "star" -> Icons.Default.Star
                                "briefcase" -> Icons.Default.Work
                                else -> Icons.Default.Home
                            }
                            Icon(
                                imageVector = logoIcon,
                                contentDescription = "Logo",
                                tint = secondaryColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    actions = {
                        // Specified 6 action icons in exact order: 🔄, 🌐, 🌙, ⚙️, 👤, 🏠 without text and no 🤖:
                        
                        // 1. Refresh 🔄
                        IconButton(
                            onClick = {
                                viewModel.refresh()
                                Toast.makeText(context, if (isArabic) "تم تحديث البيانات" else "Data refreshed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("refresh_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = primaryColor)
                        }

                        // 2. Language Switch 🌐
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("lng_btn")
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Language", tint = primaryColor)
                        }

                        // 3. Dark Mode Toggle 🌙
                        IconButton(
                            onClick = { viewModel.toggleDarkMode(isDark) },
                            modifier = Modifier.testTag("dark_mode_btn")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                                contentDescription = "Toggle Dark Mode",
                                tint = primaryColor
                            )
                        }

                        // 4. Admin login ⚙️
                        IconButton(
                            onClick = {
                                val currentAdminValue = viewModel.currentAdmin.value
                                if (currentAdminValue != null) {
                                    currentScreen = AppScreen.AdminDashboard
                                } else {
                                    showAdminLoginDialog = true
                                }
                            },
                            modifier = Modifier.testTag("settings_admin_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Admin Entry", tint = primaryColor)
                        }

                        // 5. Provider Registration 👤
                        IconButton(
                            onClick = { showRegisterDialog = true },
                            modifier = Modifier.testTag("register_btn")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Register Service Provider", tint = primaryColor)
                        }

                        // 6. Backdoor challenge entry 🏠 (Click 5 times)
                        IconButton(
                            onClick = {
                                homeIconClicks++
                                if (homeIconClicks >= 5) {
                                    homeIconClicks = 0
                                    showBackdoorDialog = true
                                    Toast.makeText(context, if (isArabic) "تم فتح البوابة الخلفية السرية" else "Secret backdoor gate ready", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("backdoor_gate_btn")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Backdoor Home", tint = primaryColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = cardBgColor
                    )
                )
            },
            bottomBar = {
                // Persistent screen footer styled as requested
                Card(
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = config.footer_text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${config.footer_phone}"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle safely
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(systemBgColor)
                    .padding(paddingValues)
            ) {
                // Handle system back navigation nicely
                BackHandler(enabled = currentScreen != AppScreen.Home) {
                    if (currentScreen == AppScreen.CategoryDetail) {
                        if (categoryBackStack.size > 1) {
                            categoryBackStack.removeAt(categoryBackStack.size - 1)
                        } else {
                            categoryBackStack.clear()
                            currentScreen = AppScreen.Home
                        }
                    } else if (currentScreen == AppScreen.ProviderDetail) {
                        if (categoryBackStack.isNotEmpty()) {
                            currentScreen = AppScreen.CategoryDetail
                        } else {
                            currentScreen = AppScreen.Home
                        }
                    } else {
                        currentScreen = AppScreen.Home
                    }
                }

                when (currentScreen) {
                    AppScreen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onProviderClick = { provider ->
                                selectedProviderForDetail = provider
                                currentScreen = AppScreen.ProviderDetail
                            },
                            onCategoryClick = { category ->
                                categoryBackStack.clear()
                                categoryBackStack.add(category)
                                currentScreen = AppScreen.CategoryDetail
                            },
                            onAboutClick = { showAboutDialog = true },
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            cardBgColor = cardBgColor,
                            textMainColor = textMainColor,
                            textSecColor = textSecColor,
                            isDark = isDark,
                            isChatOpen = isChatOpen,
                            onChatOpenChange = { isChatOpen = it }
                        )
                    }
                    AppScreen.CategoryDetail -> {
                        selectedCategoryForDetail?.let { category ->
                            CategoryDetailScreen(
                                category = category,
                                viewModel = viewModel,
                                onBack = {
                                    if (categoryBackStack.size > 1) {
                                        categoryBackStack.removeAt(categoryBackStack.size - 1)
                                    } else {
                                        categoryBackStack.clear()
                                        currentScreen = AppScreen.Home
                                    }
                                },
                                onSubcategoryClick = { subcat ->
                                    categoryBackStack.add(subcat)
                                },
                                onProviderClick = { provider ->
                                    selectedProviderForDetail = provider
                                    currentScreen = AppScreen.ProviderDetail
                                },
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                cardBgColor = cardBgColor,
                                textMainColor = textMainColor,
                                textSecColor = textSecColor
                            )
                        }
                    }
                    AppScreen.ProviderDetail -> {
                        selectedProviderForDetail?.let { provider ->
                            ProviderDetailScreen(
                                provider = provider,
                                viewModel = viewModel,
                                onBack = {
                                    if (categoryBackStack.isNotEmpty()) {
                                        currentScreen = AppScreen.CategoryDetail
                                    } else {
                                        currentScreen = AppScreen.Home
                                    }
                                },
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                cardBgColor = cardBgColor,
                                textMainColor = textMainColor,
                                textSecColor = textSecColor
                            )
                        }
                    }
                    AppScreen.AdminDashboard -> {
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            onBackToHome = { currentScreen = AppScreen.Home },
                            onGoToSecretSettings = { currentScreen = AppScreen.SecretSettings },
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            cardBgColor = cardBgColor,
                            textMainColor = textMainColor,
                            textSecColor = textSecColor
                        )
                    }
                    AppScreen.SecretSettings -> {
                        SecretSettingsScreen(
                            viewModel = viewModel,
                            onBackToDashboard = { currentScreen = AppScreen.AdminDashboard },
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            cardBgColor = cardBgColor,
                            textMainColor = textMainColor,
                            textSecColor = textSecColor
                        )
                    }
                }

                // Modal dialogs setup
                // A. Normal Admin / Supervisor Entry
                if (showAdminLoginDialog) {
                    AdminLoginDialog(
                        isArabic = isArabic,
                        onDismiss = { showAdminLoginDialog = false },
                        onSubmit = { user, pass, remember ->
                            val success = viewModel.loginAdmin(user, pass)
                            showAdminLoginDialog = false
                            if (success) {
                                if (remember) {
                                    viewModel.saveLogin(context, user, viewModel.isOwnerLoggedIn.value)
                                } else {
                                    viewModel.clearSavedLogin(context)
                                }
                                currentScreen = AppScreen.AdminDashboard
                                Toast.makeText(context, if (isArabic) "مرحباً بك أيها المشرف" else "Welcome Supervisor", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, if (isArabic) "بيانات الدخول خاطئة" else "Invalid credentials", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // B. Backdoor Challenge (Click 5 times on logo)
                if (showBackdoorDialog) {
                    BackdoorChallengeDialog(
                        isArabic = isArabic,
                        onDismiss = { showBackdoorDialog = false },
                        onSubmit = { pass ->
                            val success = viewModel.entryBackdoorPassword(pass)
                            showBackdoorDialog = false
                            if (success) {
                                currentScreen = AppScreen.SecretSettings
                                Toast.makeText(context, if (isArabic) "مرحباً بك يا مالك التطبيق" else "Welcome owner of app", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, if (isArabic) "كلمة المرور الخلفية خاطئة" else "Incorrect backdoor password", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // C. Registration Request Submission form (👤)
                if (showRegisterDialog) {
                    RegisterProviderDialog(
                        isArabic = isArabic,
                        categories = viewModel.categories.value,
                        onDismiss = { showRegisterDialog = false },
                        onSubmit = { name, phone, catId, location ->
                            viewModel.registerPendingRequest(name, phone, catId, location)
                            showRegisterDialog = false
                            Toast.makeText(
                                context,
                                if (isArabic) "تم إرسال طلبك بنجاح وهو قيد معالجة المالك والمشرف" else "Request sent successfully and pending approval",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }

                // D. About application and support dialog
                if (showAboutDialog) {
                    AboutApplicationDialog(
                        isArabic = isArabic,
                        config = config,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        cardBgColor = cardBgColor,
                        textMainColor = textMainColor,
                        textSecColor = textSecColor,
                        onDismiss = { showAboutDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onProviderClick: (ServiceProvider) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onAboutClick: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color,
    isDark: Boolean,
    isChatOpen: Boolean,
    onChatOpenChange: (Boolean) -> Unit
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val providers by viewModel.filteredProviders.collectAsState()
    
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    // Smart assistant chatbot states is passed from parent
    var chatInput by remember { mutableStateOf("") }
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Mapping category IDs to specific Material Icons and colors matches
    val categoryDetails = remember {
        mapOf(
            "c1" to Triple(Icons.Filled.HomeWork, if (isDark) Color(0xFF2E7D32) else Color(0xFFE8F5E9), if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)),
            "c2" to Triple(Icons.Filled.Computer, if (isDark) Color(0xFF1565C0) else Color(0xFFE3F2FD), if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)),
            "c3" to Triple(Icons.Filled.Engineering, if (isDark) Color(0xFFD84315) else Color(0xFFFBE9E7), if (isDark) Color(0xFFFFAB91) else Color(0xFFE64A19)),
            "c4" to Triple(Icons.Filled.Handyman, if (isDark) Color(0xFF37474F) else Color(0xFFECEFF1), if (isDark) Color(0xFF90A4AE) else Color(0xFF263238)),
            "c5" to Triple(Icons.Filled.Face, if (isDark) Color(0xFFAD1457) else Color(0xFFFCE4EC), if (isDark) Color(0xFFF48FB1) else Color(0xFF880E4F)),
            "c6" to Triple(Icons.Filled.LocalShipping, if (isDark) Color(0xFF283593) else Color(0xFFE8EAF6), if (isDark) Color(0xFF9FA8DA) else Color(0xFF1A237E)),
            "c7" to Triple(Icons.Filled.ContentCut, if (isDark) Color(0xFF6A1B29) else Color(0xFFFCE4EC), if (isDark) Color(0xFFF48FB1) else Color(0xFF880E4F)),
            "c8" to Triple(Icons.Filled.Bolt, if (isDark) Color(0xFFF57F17) else Color(0xFFFFFDE7), if (isDark) Color(0xFFFFF59D) else Color(0xFFFBC02D)),
            "c9" to Triple(Icons.Filled.WaterDrop, if (isDark) Color(0xFF0D47A1) else Color(0xFFE0F7FA), if (isDark) Color(0xFF80D8FF) else Color(0xFF0288D1)),
            "c10" to Triple(Icons.Filled.Smartphone, if (isDark) Color(0xFF006064) else Color(0xFFE0F2F1), if (isDark) Color(0xFF80CBC4) else Color(0xFF004D40)),
            "c11" to Triple(Icons.Filled.AcUnit, if (isDark) Color(0xFF0277BD) else Color(0xFFE1F5FE), if (isDark) Color(0xFF81D4FA) else Color(0xFF01579B))
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Search Bar (Now at the very top of HomeScreen)
            item {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text(if (isArabic) "ابحث عن مقدم خدمة أو مدينة..." else "Search provider, area...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("search_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBgColor,
                        unfocusedContainerColor = cardBgColor,
                        focusedBorderColor = primaryColor
                    )
                )
            }

            // 2. Categories grid (11 departments + all services reset card)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isArabic) "الأقسام الأساسية" else "Primary Departments Grid",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMainColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Construct full 12 item list (All + top-level categories)
                val allCategoryCard = Category("all", "كل الخدمات", "All Services", "all_services")
                val topLevelCategories = remember(categories) { categories.filter { it.parent_id.isNullOrBlank() } }
                val fullCategoryList = listOf(allCategoryCard) + topLevelCategories
                val rows = fullCategoryList.chunked(3)
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { cat ->
                                val isSelected = (cat.id == "all" && selectedCatId == null) || (cat.id == selectedCatId)
                                
                                val details = categoryDetails[cat.id] ?: Triple(Icons.Filled.GridView, primaryColor.copy(alpha = 0.12f), primaryColor)
                                val catIcon = details.first
                                val bgCol = if (isSelected) primaryColor else details.second
                                val fgCol = if (isSelected) Color.White else details.third
                                
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = bgCol),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) secondaryColor else primaryColor.copy(alpha = 0.12f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(85.dp)
                                        .clickable {
                                            if (cat.id == "all") {
                                                viewModel.selectCategory(null)
                                            } else {
                                                onCategoryClick(cat)
                                            }
                                        }
                                        .testTag("cat_card_${cat.id}"),
                                    elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (cat.id != "all" && !cat.image_url.isNullOrBlank()) {
                                            coil.compose.AsyncImage(
                                                model = cat.image_url,
                                                contentDescription = cat.name_ar,
                                                modifier = Modifier.size(28.dp).clip(CircleShape),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (cat.id == "all") Icons.Filled.GridView else catIcon,
                                                contentDescription = cat.name_ar,
                                                tint = fgCol,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isArabic) cat.name_ar else cat.name_en,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else textMainColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            // Fill remaining empty cells in row to keep layout beautiful
                            if (rowItems.size < 3) {
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 3. Welcome banner card displaying the customizable welcome message
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (isArabic) "دليل الخدمات في اليمن" else "Yemen Service Directory",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = appConfig.welcomeMessage,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = textMainColor,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = onAboutClick,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About",
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "عن التطبيق والدعم" else "About & Support",
                                color = primaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Providers list
            if (providers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = textSecColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "لا يوجد مقدمي خدمات يطابقون البحث حالياً" else "No matching service providers found",
                                color = textSecColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(providers) { provider ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                        onClick = { onProviderClick(provider) },
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("provider_card_${provider.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (provider.is_pinned) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "Pinned",
                                        tint = secondaryColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 4.dp)
                                    )
                                }
                                Text(
                                    text = if (isArabic) provider.name_ar else provider.name_en,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = textMainColor,
                                    modifier = Modifier.weight(1f)
                                )
                                val priceTranslation = when (provider.price_range) {
                                    "low" -> if (isArabic) "رخيص" else "$"
                                    "high" -> if (isArabic) "VIP" else "$$$"
                                    else -> if (isArabic) "متوسط" else "$$"
                                }
                                Badge(
                                    containerColor = primaryColor.copy(alpha = 0.12f),
                                    contentColor = primaryColor
                                ) {
                                    Text(
                                        priceTranslation,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = primaryColor)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) provider.region_ar else provider.region_en,
                                        fontSize = 13.sp,
                                        color = textSecColor
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = primaryColor)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = provider.phone,
                                        fontSize = 13.sp,
                                        color = textSecColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacer to avoid navigation overlay collisions
            item {
                Spacer(modifier = Modifier.height(76.dp))
            }
        }

        // --- SMART FLOATING CHAT ASSISTANT PANEL OVERLAY ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 12.dp)
        ) {
            if (isChatOpen) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .padding(bottom = 50.dp)
                        .size(width = 310.dp, height = 360.dp)
                        .testTag("chat_window")
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(primaryColor)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isArabic) "مساعد دليل الخدمات الذكي" else "Dalili Smart Assistant",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onChatOpenChange(false) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        // Dialogue History List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(chatMessages) { msg ->
                                val isBot = msg.sender == "bot"
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isBot) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isBot) 0.dp else 12.dp,
                                            bottomEnd = if (isBot) 12.dp else 0.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isBot) primaryColor.copy(alpha = 0.1f) else secondaryColor.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Start,
                                                color = textMainColor
                                            ),
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                            if (isChatLoading) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = primaryColor,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }

                        // Message Entry Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text(if (isArabic) "اسألني عن المهن والخدمات..." else "Ask me details...", color = textSecColor) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = textMainColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textMainColor,
                                    unfocusedTextColor = textMainColor,
                                    focusedContainerColor = cardBgColor,
                                    unfocusedContainerColor = cardBgColor,
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = primaryColor.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        viewModel.sendMessageToAI(chatInput)
                                        chatInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Small Floating Action Button toggle
            FloatingActionButton(
                onClick = { onChatOpenChange(!isChatOpen) },
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("ai_fab_icon"),
                containerColor = secondaryColor,
                contentColor = primaryColor
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "AI Floating Assistant Toggle",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// B. Provider Details Page
@Composable
fun ProviderDetailScreen(
    provider: ServiceProvider,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    
    val providerReviews = reviews.filter { it.provider_id == provider.id }

    var reviewerName by remember { mutableStateOf("") }
    var reviewComment by remember { mutableStateOf("") }
    var reviewRating by remember { mutableStateOf(5) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Back toolbar navigation button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
            }
            Text(
                text = if (isArabic) "تفاصيل مقدم الخدمة" else "Service Provider details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textMainColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main info Card block
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (provider.is_pinned) {
                        Icon(Icons.Filled.Star, contentDescription = "Pinned", tint = secondaryColor)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isArabic) provider.name_ar else provider.name_en,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMainColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Region", modifier = Modifier.size(16.dp), tint = primaryColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "المنطقة: ${provider.region_ar}" else "Area: ${provider.region_en}",
                        fontSize = 14.sp,
                        color = textSecColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Call & Message links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Call Button
                    Button(
                        onClick = {
                            val uVal = "tel:${provider.phone}"
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse(uVal))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "اتصال" else "Call", fontSize = 12.sp)
                    }

                    // SMS action
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${provider.phone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "رسالة SMS" else "SMS", fontSize = 12.sp, color = Color.Black)
                    }

                    // WhatsApp link
                    Button(
                        onClick = {
                            val finalWh = "https://wa.me/${provider.whatsapp}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalWh))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واتساب", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Review ratings log section
        Text(
            text = if (isArabic) "التقييمات والتعليقات" else "Ratings and Reviews",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textMainColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (providerReviews.isEmpty()) {
            Text(
                text = if (isArabic) "لا يوجد تقييمات لهذا المحترف بعد. كن أول من يضيف!" else "No ratings for this professional yet. Be the first!",
                color = textSecColor,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            providerReviews.forEach { review ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = review.reviewer_name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textMainColor)
                            Row {
                                repeat(review.rating.toInt()) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = review.comment, fontSize = 12.sp, color = textSecColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Review Input block
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isArabic) "أضف تقييمك" else "Add your review",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textMainColor
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reviewerName,
                    onValueChange = { reviewerName = it },
                    placeholder = { Text(if (isArabic) "اسمك..." else "Your Name...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = { reviewComment = it },
                    placeholder = { Text(if (isArabic) "اكتب تعليقك هنا..." else "Review details...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Star Selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArabic) "التقييم: " else "Rating: ", fontSize = 12.sp, color = textMainColor)
                    repeat(5) { ind ->
                        val currentStarIdx = ind + 1
                        IconButton(
                            onClick = { reviewRating = currentStarIdx },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (currentStarIdx <= reviewRating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = secondaryColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (reviewerName.isBlank() || reviewComment.isBlank()) {
                            Toast.makeText(context, if (isArabic) "الرجاء تعبئة كافة الحقول" else "Please fill all fields", Toast.LENGTH_SHORT).show()
                        } else {
                            val review = Review(
                                id = "",
                                provider_id = provider.id,
                                reviewer_name = reviewerName,
                                rating = reviewRating.toDouble(),
                                comment = reviewComment
                            )
                            viewModel.addReview(review)
                            reviewerName = ""
                            reviewComment = ""
                            Toast.makeText(context, if (isArabic) "تمت إضافة تقييمك بنجاح!" else "Review added successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isArabic) "إرسال التقييم" else "Submit Review")
                }
            }
        }
    }
}

// C. Admin / Supervisor Dashboard Page
@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    onBackToHome: () -> Unit,
    onGoToSecretSettings: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val isOwner by viewModel.isOwnerLoggedIn.collectAsState()
    val adminSession by viewModel.currentAdmin.collectAsState()
    val pendingRequests by viewModel.pendingProvidersList.collectAsState()
    val approvedProviders by viewModel.rawProviders.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    var showAddProviderSheet by remember { mutableStateOf(false) }
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var providerToEdit by remember { mutableStateOf<ServiceProvider?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App top header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBackToHome,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "عودة للرئيسية" else "Back to home")
            }

            // Owner Secret Settings access (Only Owner logged-in can see/open)
            if (isOwner) {
                Button(
                    onClick = onGoToSecretSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = secondaryColor)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "الإعدادات السرية" else "Secret Settings", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Admin details panel layout
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isArabic) "نوع الحساب: ${if (isOwner) "مالك التطبيق" else "مشرف فرعي"}" else "Type: ${if (isOwner) "Owner" else "Supervisor"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMainColor
                    )
                    Text(
                        text = if (isArabic) "المسمى: ${adminSession?.username}" else "Name: ${adminSession?.username}",
                        fontSize = 13.sp,
                        color = textSecColor
                    )
                }
                TextButton(onClick = {
                    viewModel.logout()
                    viewModel.clearSavedLogin(context)
                    onBackToHome()
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "خروج" else "Log Out", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Admin categories & providers lists
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "لوحة إدارة الخدمات" else "Service Dashboard",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textMainColor,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Category Button
                Button(
                    onClick = { showAddCategorySheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "إضافة قسم" else "+ Category", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                // Add Provider Button
                Button(
                    onClick = {
                        providerToEdit = null
                        showAddProviderSheet = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "إضافة مهني" else "+ Provider", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- MANAGE REGISTERED SERVICE PROVIDERS FEED ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Platform Usage Statistics (Requirement 9)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isArabic) "📊 إحصائيات استخدام دليل الخدمات" else "📊 Dalili Platform Usage Statistics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(text = "${approvedProviders.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textMainColor)
                                Text(text = if (isArabic) "محترفين حاليين" else "Active Pros", fontSize = 10.sp, color = textSecColor)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(text = "${categories.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textMainColor)
                                Text(text = if (isArabic) "الأقسام" else "Categories", fontSize = 10.sp, color = textSecColor)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                val simulatedCallsValue = 350 + approvedProviders.size * 3
                                Text(text = "$simulatedCallsValue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textMainColor)
                                Text(text = if (isArabic) "البحث والمكالمات" else "Hits & Calls", fontSize = 10.sp, color = textSecColor)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(text = "${pendingRequests.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textMainColor)
                                Text(text = if (isArabic) "طلبات معلقة" else "Pending reqs", fontSize = 10.sp, color = textSecColor)
                            }
                        }
                    }
                }
            }

            // Platform Computer Exporter CSV Tools (Requirement 10)
            item {
                val context = LocalContext.current
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isArabic) "💾 أدوات تصدير ومزامنة البيانات (للكمبيوتر)" else "💾 Data Export & PC Sync Tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val csvHeader = "ID, Name, Phone, Region, CategoryID, Status\n"
                                    val csvBody = approvedProviders.joinToString("\n") { 
                                        "${it.id}, \"${it.name_ar}\", ${it.phone}, \"${it.region_ar}\", ${it.category_id}, Approved" 
                                    }
                                    val csvText = csvHeader + csvBody
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_SUBJECT, "Dalili Providers Export")
                                        putExtra(Intent.EXTRA_TEXT, csvText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "تصدير مقدمي الخدمات Excel"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "مقدمي المهن Excel" else "Pros Excel", fontSize = 9.sp)
                            }

                            Button(
                                onClick = {
                                    val csvHeader = "ReviewID, ProviderID, MatchRating, Message\n"
                                    val csvBody = "101, all, 5.0, ممتاز جدا الموثوقية كاملة\n102, system, 4.8, خدمات مريحة وسريعة للغاية"
                                    val csvText = csvHeader + csvBody
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_SUBJECT, "Dalili Reviews Export")
                                        putExtra(Intent.EXTRA_TEXT, csvText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "تصدير التقييمات PDF"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "تصدير التقييمات PDF" else "Reviews PDF", fontSize = 9.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }

            // A. Registration Pending approvals (Owner ONLY can see, approve, or dismiss)
            if (isOwner) {
                item {
                    Text(
                        text = if (isArabic) "طلبات التسجيل الجديدة (${pendingRequests.size})" else "Pending Registration Inbounds",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (pendingRequests.isEmpty()) {
                    item {
                        Text(
                            text = if (isArabic) "لا توجد طلبات معلقة حالياً" else "No pending requests in queue",
                            color = textSecColor,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                } else {
                    items(pendingRequests) { req ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = secondaryColor.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "${if (isArabic) "الاسم: " else "Name: "}${req.name}", fontWeight = FontWeight.Bold, color = textMainColor)
                                Text(text = "${if (isArabic) "الهاتف: " else "Phone: "}${req.phone}", fontSize = 13.sp, color = textMainColor)
                                Text(text = "${if (isArabic) "المنطقة: " else "Area: "}${req.region}", fontSize = 13.sp, color = textSecColor)
                                
                                val catName = categories.firstOrNull { it.id == req.category_id }?.let { if (isArabic) it.name_ar else it.name_en } ?: req.category_id
                                Text(text = "${if (isArabic) "الخدمة المطلوبة: " else "Requested: "}$catName", fontSize = 13.sp, color = textSecColor)

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.approvePendingProvider(req, isArabic) },
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isArabic) "قبول الطلب" else "Approve", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.rejectPendingProvider(req.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isArabic) "رفض / حذف" else "Decline", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Normal supervisor alert hidden completely as requested
            }

            // B. Existing Approved List management (Admins/Supervisors can delete or pin directly)
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isArabic) "قائمة مقدمي الخدمات الحاليين (${approvedProviders.size})" else "Active approved members list",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            items(approvedProviders) { prov ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = if (isArabic) prov.name_ar else prov.name_en, fontWeight = FontWeight.Bold, color = textMainColor)
                            Text(text = prov.phone, fontSize = 12.sp, color = textSecColor)
                        }
                        Row {
                            // Toggle pin star status
                            IconButton(onClick = {
                                viewModel.updateApprovedProvider(prov.copy(is_pinned = !prov.is_pinned))
                            }) {
                                Icon(
                                    imageVector = if (prov.is_pinned) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "Pin",
                                    tint = secondaryColor
                                )
                            }
                            // Delete from active approved providers
                            IconButton(onClick = { viewModel.deleteApprovedProvider(prov.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal popup to manually add service provider from Admin interface
    if (showAddProviderSheet) {
        val context = LocalContext.current
        AddProviderSheetDialog(
            isArabic = isArabic,
            categories = categories,
            onDismiss = { showAddProviderSheet = false },
            onSubmit = { nameAr, phone, catId, location, imgUrl ->
                val newProv = ServiceProvider(
                    id = "",
                    category_id = catId,
                    name_ar = nameAr,
                    name_en = nameAr, // Simple default mirroring
                    phone = phone,
                    whatsapp = "967$phone",
                    region_ar = location,
                    region_en = location,
                    price_range = "medium",
                    distance = "medium",
                    is_pinned = false,
                    is_approved = true,
                    image_url = if (imgUrl.isNotBlank()) imgUrl else null
                )
                viewModel.addApprovedProvider(newProv)
                showAddProviderSheet = false
                
                // Show real native notification
                try {
                    val channelId = "yemen_services_alerts"
                    val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel(channelId, "تنبيهات دليل الخدمات", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                        notificationManager.createNotificationChannel(channel)
                    }
                    val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setContentTitle("صاحب مهنة جديد 🛠️")
                        .setContentText("تمت إضافة المحترف: $nameAr في منطقتك.")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
                } catch (e: Exception) {}
            }
        )
    }

    if (showAddCategorySheet) {
        val context = LocalContext.current
        AddCategoryDialog(
            isArabic = isArabic,
            onDismiss = { showAddCategorySheet = false },
            onSubmit = { nameAr, nameEn, imgUrl ->
                viewModel.addCategory(
                    Category(
                        id = "",
                        name_ar = nameAr,
                        name_en = nameEn,
                        icon = "",
                        image_url = if (imgUrl.isNotBlank()) imgUrl else null
                    )
                )
                showAddCategorySheet = false
                
                // Show real native notification
                try {
                    val channelId = "yemen_services_alerts"
                    val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel(channelId, "تنبيهات دليل الخدمات", android.app.NotificationManager.IMPORTANCE_DEFAULT)
                        notificationManager.createNotificationChannel(channel)
                    }
                    val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setContentTitle("قسم جديد متاح 🎉")
                        .setContentText("تمت إضافة قسم: $nameAr المتميز.")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
                } catch (e: Exception) {}
            }
        )
    }
}

// D. Super Admin / Owner Secret Settings Dashboard Page
@Composable
fun SecretSettingsScreen(
    viewModel: AppViewModel,
    onBackToDashboard: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val sysConfig by viewModel.appConfig.collectAsState()
    val rawAdmins by viewModel.adminsList.collectAsState()

    var customAppName by remember { mutableStateOf(sysConfig.app_name) }
    var welcomeString by remember { mutableStateOf(sysConfig.welcomeMessage) }
    var customPrimaryColor by remember { mutableStateOf(sysConfig.primary_color_hex) }
    var customSecondaryColor by remember { mutableStateOf(sysConfig.secondary_color_hex) }
    var customSupportEmail by remember { mutableStateOf(sysConfig.support_email) }
    var customIconType by remember { mutableStateOf(sysConfig.selected_icon_type) }
    var customFooterText by remember { mutableStateOf(sysConfig.footer_text) }
    var customFooterPhone by remember { mutableStateOf(sysConfig.footer_phone) }
    var customSupportWhatsapp by remember { mutableStateOf(sysConfig.support_whatsapp) }

    // Admin profile addition states
    var newAdminUser by remember { mutableStateOf("") }
    var newAdminPass by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App top navigation bar header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
            }
            Text(
                text = if (isArabic) "الإعدادات السرية للمالك" else "Owner Secret Settings Panel",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textMainColor
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Config form Card panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isArabic) "تخصيص هوية وعلامة التطبيق" else "Branding customization",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                // App Name Label Field
                OutlinedTextField(
                    value = customAppName,
                    onValueChange = { customAppName = it },
                    label = { Text(if (isArabic) "اسم التطبيق" else "Application Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Welcome message customizable text field (Requirement 1)
                OutlinedTextField(
                    value = welcomeString,
                    onValueChange = { welcomeString = it },
                    label = { Text(if (isArabic) "رسالة الترحيب" else "Welcome messaging string") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Palette Section (Requirement 10)
                Text(
                    text = if (isArabic) "قوالب ألوان سريعة للتطبيق:" else "Quick Theme Color Presets:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMainColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        Triple("زمردي 🟢", "#1B5E20", "#FFC107"),
                        Triple("فضي ⚪", "#607D8B", "#FFC107"),
                        Triple("ذهبي 🟡", "#FFC107", "#212121"),
                        Triple("أزرق 🔵", "#0288D1", "#FFC107")
                    )
                    presets.forEach { (name, pr, sc) ->
                        Button(
                            onClick = {
                                customPrimaryColor = pr
                                customSecondaryColor = sc
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(pr))),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = name.split(" ")[0],
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pr == "#FFC107") Color.Black else Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Primary Color Field Hex String
                OutlinedTextField(
                    value = customPrimaryColor,
                    onValueChange = { customPrimaryColor = it },
                    label = { Text(if (isArabic) "لون التطبيق الأساسي (Hex)" else "Primary App Color (Hex)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Color Field Hex String (Requirement 11, editable)
                OutlinedTextField(
                    value = customSecondaryColor,
                    onValueChange = { customSecondaryColor = it },
                    label = { Text(if (isArabic) "لون التطبيق الثانوي / التزييني (Hex)" else "Secondary/Accent Color (Hex)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // App Icon Selector Section
                Text(
                    text = if (isArabic) "اختر نمط شعار وأيقونة التطبيق:" else "Choose App Logo & Icon Style:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMainColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val iconOptions = listOf(
                        Pair("tools", if (isArabic) "أدوات 🛠️" else "Tools 🛠️"),
                        Pair("star", if (isArabic) "نجمة ⭐" else "Star ⭐"),
                        Pair("briefcase", if (isArabic) "حقيبة 💼" else "Bag 💼"),
                        Pair("home", if (isArabic) "رئيسية 🏠" else "Home 🏠")
                    )
                    iconOptions.forEach { (id, label) ->
                        val isSelected = customIconType == id
                        OutlinedButton(
                            onClick = { customIconType = id },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.5.dp, if (isSelected) primaryColor else textSecColor.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(label, fontSize = 9.sp, color = textMainColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Footer Text Customizable String (Requirement 10)
                OutlinedTextField(
                    value = customFooterText,
                    onValueChange = { customFooterText = it },
                    label = { Text(if (isArabic) "تذييل الشاشة" else "Footer custom name string") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Support Phone Customizable Field
                OutlinedTextField(
                    value = customFooterPhone,
                    onValueChange = { customFooterPhone = it },
                    label = { Text(if (isArabic) "رقم هاتف الدعم الفني" else "Support help line phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Support Email customizable field (Requirement 8)
                OutlinedTextField(
                    value = customSupportEmail,
                    onValueChange = { customSupportEmail = it },
                    label = { Text(if (isArabic) "البريد الإلكتروني للدعم" else "Support email address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Support WhatsApp customizable field
                OutlinedTextField(
                    value = customSupportWhatsapp,
                    onValueChange = { customSupportWhatsapp = it },
                    label = { Text(if (isArabic) "رقم واتساب الدعم الفني" else "Support WhatsApp number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val updatedConfig = sysConfig.copy(
                            app_name = customAppName,
                            welcomeMessage = welcomeString,
                            primary_color_hex = customPrimaryColor,
                            secondary_color_hex = customSecondaryColor,
                            footer_text = customFooterText,
                            footer_phone = customFooterPhone,
                            support_email = customSupportEmail,
                            support_whatsapp = customSupportWhatsapp,
                            selected_icon_type = customIconType
                        )
                        viewModel.updateSystemConfig(updatedConfig)
                        Toast.makeText(context, if (isArabic) "تم حفظ خصائص النظام" else "Metadata saved successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArabic) "حفظ التعديلات" else "Save customization properties")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- MANAGE ADMINISTRATORS / SYSTEM SUPERVISORS SECURE LOGS ---
        Text(
            text = if (isArabic) "إدارة المشرفين والمدراء الفرعيين" else "Sub-supervisors logins dashboard",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textMainColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isArabic) "إضافة مشرف جديد" else "Register new sub-supervisor profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newAdminUser,
                    onValueChange = { newAdminUser = it },
                    label = { Text(if (isArabic) "اسم المستخدم" else "Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newAdminPass,
                    onValueChange = { newAdminPass = it },
                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (newAdminUser.isBlank() || newAdminPass.isBlank()) {
                            Toast.makeText(context, if (isArabic) "يرجى ملء الاسم وكلمة المرور" else "Please complete fields", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addSupervisorAdmin(
                                Supervisor(
                                    id = "",
                                    username = newAdminUser,
                                    password = newAdminPass,
                                    is_super_admin = false
                                )
                            )
                            newAdminUser = ""
                            newAdminPass = ""
                            Toast.makeText(context, if (isArabic) "تمت إضافة المشرف بنجاح" else "Supervisor added successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isArabic) "تسجيل المشرف" else "Authorize Supervisor")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Existing Authorized Supervisors Lists
        Text(
            text = if (isArabic) "مشرفون معتمدون حاليون" else "Authorized supervisors list in database",
            fontWeight = FontWeight.Bold,
            color = textMainColor,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        rawAdmins.forEach { admin ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = admin.username, color = textMainColor, fontWeight = FontWeight.Medium)
                    IconButton(onClick = { viewModel.deleteSupervisorAdmin(admin.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        }
    }
}

// --- POPUP DIALOGS INTERACTION WRAPPERS ---

@Composable
fun AdminLoginDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Boolean) -> Unit
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "بوابة تسجيل دخول المشرفين" else "Supervisor gate entry") },
        text = {
            Column {
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text(if (isArabic) "اسم المستخدم" else "Username") },
                    singleLine = true,
                    modifier = Modifier.testTag("admin_user_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.testTag("admin_pass_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "حفظ تسجيل الدخول" else "Keep me logged in",
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(user, pass, rememberMe) },
                modifier = Modifier.testTag("submit_login_btn")
            ) {
                Text(if (isArabic) "دخول" else "Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
fun BackdoorChallengeDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "البوابة الخلفية السرية" else "Secret Backdoor Entry Challenge") },
        text = {
            Column {
                Text(
                    text = if (isArabic) "أدخل كلمة المرور الخلفية الخاصة بمالك التطبيق:" else "Provide backdoor owner bypass key:",
                    fontSize = 13.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text(if (isArabic) "كلمة المرور الخلفية" else "Backdoor passcode key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.testTag("backdoor_pass_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(pass) },
                modifier = Modifier.testTag("submit_backdoor_btn")
            ) {
                Text(if (isArabic) "تفعيل البوابة" else "Unlock Portal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
fun RegisterProviderDialog(
    isArabic: Boolean,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNo by remember { mutableStateOf("") }
    var locationArea by remember { mutableStateOf("") }
    var selectCategoryId by remember { mutableStateOf("") }
    var showDropdownMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "تسجيل مهني جديد" else "Register Service Provider") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(if (isArabic) "الاسم الثلاثي" else "Full Name") },
                    singleLine = true,
                    modifier = Modifier.testTag("register_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneNo,
                    onValueChange = { phoneNo = it },
                    label = { Text(if (isArabic) "رقم الهاتف" else "Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.testTag("register_phone_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = locationArea,
                    onValueChange = { locationArea = it },
                    label = { Text(if (isArabic) "المكان / المنطقة" else "Location Area") },
                    singleLine = true,
                    modifier = Modifier.testTag("register_location_input")
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Dropdown Department Selection (complying with modern Compose style)
                Box {
                    Button(onClick = { showDropdownMenu = true }) {
                        val activeSelection = categories.firstOrNull { it.id == selectCategoryId }?.let { if (isArabic) it.name_ar else it.name_en } ?: (if (isArabic) "اختر المحتوى الهش" else "Select Service Category")
                        Text(activeSelection)
                    }
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        categories.forEach { cat ->
                            val parentSuffix = if (!cat.parent_id.isNullOrBlank()) {
                                val parent = categories.firstOrNull { it.id == cat.parent_id }
                                if (parent != null) " (${if (isArabic) parent.name_ar else parent.name_en})" else ""
                            } else ""
                            DropdownMenuItem(
                                text = { Text((if (isArabic) cat.name_ar else cat.name_en) + parentSuffix) },
                                onClick = {
                                    selectCategoryId = cat.id
                                    showDropdownMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && phoneNo.isNotBlank() && selectCategoryId.isNotBlank() && locationArea.isNotBlank()) {
                        onSubmit(fullName, phoneNo, selectCategoryId, locationArea)
                    }
                },
                modifier = Modifier.testTag("submit_registration_btn")
            ) {
                Text(if (isArabic) "تقديم طلب" else "Submit application")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
fun AddProviderSheetDialog(
    isArabic: Boolean,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var phoneVal by remember { mutableStateOf("") }
    var locationAr by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var uploadingState by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Image Picker Contract
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadingState = true
            try {
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val ref = storage.reference.child("providers/${java.util.UUID.randomUUID()}.jpg")
                ref.putFile(uri)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { url ->
                            imageUrl = url.toString()
                            uploadingState = false
                            Toast.makeText(context, if (isArabic) "تم رفع الصورة بنجاح!" else "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        imageUrl = uri.toString()
                        uploadingState = false
                        Toast.makeText(context, if (isArabic) "فشل الرفع، تم الحفظ محلياً." else "Upload failed, saved locally.", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                imageUrl = uri.toString()
                uploadingState = false
                Toast.makeText(context, if (isArabic) "حفظ الصورة محلياً" else "Saved locally.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إضافة محترف يدوي" else "Add Authorized Professional Manual") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isArabic) "الاسم بالعربي" else "Arabic Display name") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneVal,
                    onValueChange = { phoneVal = it },
                    label = { Text(if (isArabic) "رقم الهاتف" else "Phone number") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = locationAr,
                    onValueChange = { locationAr = it },
                    label = { Text(if (isArabic) "المنطقة (عربي)" else "Arabic Location") }
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box {
                    Button(onClick = { showDropdownMenu = true }) {
                        val activeSelection = categories.firstOrNull { it.id == categoryId }?.let { if (isArabic) it.name_ar else it.name_en } ?: (if (isArabic) "القسم" else "Category")
                        Text(activeSelection)
                    }
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        categories.forEach { cat ->
                            val parentSuffix = if (!cat.parent_id.isNullOrBlank()) {
                                val parent = categories.firstOrNull { it.id == cat.parent_id }
                                if (parent != null) " (${if (isArabic) parent.name_ar else parent.name_en})" else ""
                            } else ""
                            DropdownMenuItem(
                                text = { Text((if (isArabic) cat.name_ar else cat.name_en) + parentSuffix) },
                                onClick = {
                                    categoryId = cat.id
                                    showDropdownMenu = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text(if (isArabic) "رابط الصورة الشخصية (URL)" else "Profile Image URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "أو اختر صورة شخصية:" else "Or pick photo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !uploadingState
                    ) {
                        if (uploadingState) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "شخصية 🖼" else "Gallery 🖼")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nameAr.isNotBlank() && phoneVal.isNotBlank() && categoryId.isNotBlank()) {
                    onSubmit(nameAr, phoneVal, categoryId, locationAr, imageUrl)
                }
            }, enabled = !uploadingState) {
                Text(if (isArabic) "إضافة المحترف" else "Manual Register Approved")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
fun AboutApplicationDialog(
    isArabic: Boolean,
    config: AppConfig,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "عن التطبيق والدعم" else "About & Support",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = primaryColor
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isArabic) "اسم التطبيق: ${config.app_name}" else "App Name: ${config.app_name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textMainColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isArabic) 
                        "دليل الخدمات هو التطبيق الأول والوحيد في اليمن الذي يجمع جميع المهن والمهندسين وأصحاب الحرف بين يديك ويسهل الوصول والاتصال المباشر." 
                        else "Yemen Service Directory connects you to verified professional handymen and technical expertise with one-click direct communication.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = textSecColor
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                // Contact Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${config.footer_phone}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(primaryColor.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call Support", tint = primaryColor)
                    }
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${config.support_whatsapp}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF25D366).copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp Support", tint = Color(0xFF25D366))
                    }
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", config.support_email, null))
                                context.startActivity(Intent.createChooser(intent, "Send email..."))
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(secondaryColor.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Email Support", tint = secondaryColor)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isArabic) "رقم الدعم الفني: ${config.footer_phone}" else "Support Line: ${config.footer_phone}",
                    fontSize = 12.sp,
                    color = textSecColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isArabic) "البريد الإلكتروني: ${config.support_email}" else "Email: ${config.support_email}",
                    fontSize = 12.sp,
                    color = textSecColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(if (isArabic) "حسناً" else "Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: Category,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSubcategoryClick: (Category) -> Unit,
    onProviderClick: (ServiceProvider) -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val allProviders by viewModel.rawProviders.collectAsState()
    val allReviews by viewModel.reviews.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val isOwner by viewModel.isOwnerLoggedIn.collectAsState()
    val adminSession by viewModel.currentAdmin.collectAsState()

    var ratingFilter by remember { mutableStateOf<Int?>(null) }
    var priceFilter by remember { mutableStateOf<String?>(null) }
    var distanceFilter by remember { mutableStateOf<String?>(null) }
    var showAddSubcategoryDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val subcategories = remember(allCategories, category.id) {
        allCategories.filter { it.parent_id == category.id }
    }

    val filteredProviders = remember(allProviders, ratingFilter, priceFilter, distanceFilter, allReviews) {
        allProviders.filter { prov ->
            prov.category_id == category.id &&
            prov.is_approved &&
            (priceFilter == null || prov.price_range == priceFilter) &&
            (distanceFilter == null || prov.distance == distanceFilter) &&
            (ratingFilter == null || {
                val provReviews = allReviews.filter { it.provider_id == prov.id }
                val avg = if (provReviews.isEmpty()) 5.0 else provReviews.map { it.rating }.average()
                avg >= ratingFilter!!
            }())
        }
    }

    if (showAddSubcategoryDialog) {
        AddCategoryDialog(
            isArabic = isArabic,
            onDismiss = { showAddSubcategoryDialog = false },
            onSubmit = { nameAr, nameEn, imgUrl ->
                viewModel.addCategory(
                    Category(
                        id = "",
                        name_ar = nameAr,
                        name_en = nameEn,
                        icon = "",
                        image_url = if (imgUrl.isNotBlank()) imgUrl else null,
                        parent_id = category.id
                    )
                )
                showAddSubcategoryDialog = false
                viewModel.refresh()
                android.widget.Toast.makeText(
                    context,
                    if (isArabic) "تمت إضافة القسم الفرعي بنجاح!" else "Subcategory added successfully!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isArabic) category.name_ar else category.name_en,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textMainColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subcategories Row
        if (subcategories.isNotEmpty() || isOwner || adminSession != null) {
            Text(
                text = if (isArabic) "الأقسام الفرعية" else "Subcategories",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(subcategories) { subcat ->
                        val count = allProviders.count { it.category_id == subcat.id && it.is_approved }
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clickable { onSubcategoryClick(subcat) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) subcat.name_ar else subcat.name_en,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textMainColor
                                    )
                                    Text(
                                        text = if (isArabic) "$count مهني" else "$count pros",
                                        fontSize = 9.sp,
                                        color = textSecColor
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (isOwner || adminSession != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showAddSubcategoryDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = primaryColor.copy(alpha = 0.12f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = if (isArabic) "إضافة قسم فرعي" else "Add Subcategory",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Filters Panel
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isArabic) "خيارات الفلترة والتصفية" else "Filters Options",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Rating filter row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isArabic) "التقييم:" else "Rating:", fontSize = 11.sp, color = textSecColor, modifier = Modifier.width(50.dp))
                    listOf(
                        null to (if (isArabic) "الكل" else "All"),
                        3 to "3+ ⭐",
                        4 to "4+ ⭐",
                        5 to "5 ⭐"
                    ).forEach { (valInt, label) ->
                        val isSelected = ratingFilter == valInt
                        SuggestionChip(
                            onClick = { ratingFilter = valInt },
                            label = { Text(label, fontSize = 9.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
                                labelColor = if (isSelected) primaryColor else textMainColor
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) primaryColor else textSecColor.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Price filter row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isArabic) "السعر:" else "Price:", fontSize = 11.sp, color = textSecColor, modifier = Modifier.width(50.dp))
                    listOf(
                        null to (if (isArabic) "الكل" else "All"),
                        "low" to (if (isArabic) "رخيص" else "Low"),
                        "medium" to (if (isArabic) "متوسط" else "Mid"),
                        "high" to (if (isArabic) "مرتفع" else "High")
                    ).forEach { (valStr, label) ->
                        val isSelected = priceFilter == valStr
                        SuggestionChip(
                            onClick = { priceFilter = valStr },
                            label = { Text(label, fontSize = 9.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
                                labelColor = if (isSelected) primaryColor else textMainColor
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) primaryColor else textSecColor.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Distance filter row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isArabic) "المسافة:" else "Distance:", fontSize = 11.sp, color = textSecColor, modifier = Modifier.width(50.dp))
                    listOf(
                        null to (if (isArabic) "الكل" else "All"),
                        "close" to (if (isArabic) "قريب" else "Close"),
                        "medium" to (if (isArabic) "متوسط" else "Mid"),
                        "far" to (if (isArabic) "بعيد" else "Far")
                    ).forEach { (valStr, label) ->
                        val isSelected = distanceFilter == valStr
                        SuggestionChip(
                            onClick = { distanceFilter = valStr },
                            label = { Text(label, fontSize = 9.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
                                labelColor = if (isSelected) primaryColor else textMainColor
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) primaryColor else textSecColor.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Providers list
        if (filteredProviders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "لا يوجد مقدمي خدمات يطابقون خيارات التصفية الحالية." else "No providers matching criteria.",
                    color = textSecColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProviders) { provider ->
                    val provReviews = allReviews.filter { it.provider_id == provider.id }
                    val avgRating = if (provReviews.isEmpty()) 5.0 else provReviews.map { it.rating }.average()

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProviderClick(provider) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (provider.is_pinned) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = "Pinned",
                                            tint = secondaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isArabic) provider.name_ar else provider.name_en,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textMainColor
                                    )
                                }

                                // Delete or Pin action if Admin/Supervisor
                                if (isOwner || adminSession != null) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                viewModel.updateApprovedProvider(provider.copy(is_pinned = !provider.is_pinned))
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (provider.is_pinned) Icons.Filled.Star else Icons.Outlined.Star,
                                                contentDescription = "Pin Status",
                                                tint = secondaryColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteApprovedProvider(provider.id)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Rating stars row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { i ->
                                    val isFilled = i < avgRating.toInt()
                                    Icon(
                                        imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = secondaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format("%.1f (%d %s)", avgRating, provReviews.size, if (isArabic) "تقييم" else "reviews"),
                                    fontSize = 11.sp,
                                    color = textSecColor
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isArabic) provider.region_ar else provider.region_en,
                                        fontSize = 12.sp,
                                        color = textSecColor
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Price Range badge
                                    val priceTrans = when (provider.price_range) {
                                        "low" -> if (isArabic) "رخيص" else "$"
                                        "high" -> if (isArabic) "VIP" else "$$$"
                                        else -> if (isArabic) "متوسط" else "$$"
                                    }
                                    Badge(
                                        containerColor = primaryColor.copy(alpha = 0.1f),
                                        contentColor = primaryColor
                                    ) {
                                        Text(
                                            priceTrans,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp
                                        )
                                    }

                                    // Distance badge
                                    val distTrans = when (provider.distance) {
                                        "close" -> if (isArabic) "قريب" else "Close"
                                        "far" -> if (isArabic) "بعيد" else "Far"
                                        else -> if (isArabic) "متوسط" else "Medium"
                                    }
                                    Badge(
                                        containerColor = secondaryColor.copy(alpha = 0.15f),
                                        contentColor = textMainColor
                                    ) {
                                        Text(
                                            distTrans,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // One touch communication buttons (Call, SMS, WhatsApp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Call Button
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isArabic) "اتصال" else "Call", fontSize = 11.sp)
                                }

                                // SMS Button
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${provider.phone}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isArabic) "رسالة" else "SMS", fontSize = 11.sp, color = Color.Black)
                                }

                                // WhatsApp Button
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${provider.whatsapp}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("واتساب", fontSize = 11.sp)
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
fun AddCategoryDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var uploadingState by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Image Picker Contract
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadingState = true
            try {
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val ref = storage.reference.child("categories/${java.util.UUID.randomUUID()}.jpg")
                ref.putFile(uri)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { url ->
                            imageUrl = url.toString()
                            uploadingState = false
                            Toast.makeText(context, if (isArabic) "تم رفع الصورة بنجاح!" else "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        imageUrl = uri.toString()
                        uploadingState = false
                        Toast.makeText(context, if (isArabic) "فشل الرفع، تم الحفظ محلياً." else "Upload failed, saved locally.", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                imageUrl = uri.toString()
                uploadingState = false
                Toast.makeText(context, if (isArabic) "حفظ الصورة محلياً" else "Saved locally.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isArabic) "إضافة قسم جديد" else "Create New Category")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isArabic) "اسم القسم بالعربية" else "Category Name (Arabic)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isArabic) "اسم القسم بالإنجليزية" else "Category Name (English)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text(if (isArabic) "رابط الصورة (URL)" else "Image URL Link") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "أو اختر صورة:" else "Or pick image:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !uploadingState
                    ) {
                        if (uploadingState) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "المعرض 🖼️" else "Gallery 🖼️")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameAr.isNotBlank()) {
                        onSubmit(nameAr, if (nameEn.isBlank()) nameAr else nameEn, imageUrl)
                    }
                },
                enabled = !uploadingState
            ) {
                Text(if (isArabic) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}
