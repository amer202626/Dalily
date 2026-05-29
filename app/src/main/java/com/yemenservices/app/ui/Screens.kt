package com.yemenservices.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.PendingProvider
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.ServiceProvider
import com.yemenservices.app.data.Supervisor
import kotlinx.coroutines.launch

enum class AppScreen {
    Home,
    ProviderDetail,
    AdminDashboard,
    SecretSettings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AppViewModel) {
    val config by viewModel.appConfig.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()
    val isDark = isSystemInDarkTheme()

    // Parse App Secret Settings colors dynamically
    val primaryColor = Color(android.graphics.Color.parseColor(config.primary_color_hex))
    val secondaryColor = Color(android.graphics.Color.parseColor(config.secondary_color_hex))
    
    // Core brand themes defined by properties
    val systemBgColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textMainColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF212121)
    val textSecColor = if (isDark) Color(0xFFB0B0B0) else Color(0xFF616161)

    // State management for navigation and modals
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var selectedProviderForDetail by remember { mutableStateOf<ServiceProvider?>(null) }
    
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Click tracker for backdoor challenge (5 times on app logo)
    var homeIconClicks by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Logo",
                                tint = secondaryColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = config.app_name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMainColor
                            )
                        }
                    },
                    actions = {
                        // Right to left icons:
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

                        // 3. Admin login ⚙️
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
                            Icon(Icons.Default.Settings, contentDescription = "Admin", tint = primaryColor)
                        }

                        // 4. Provider Registration 👤
                        IconButton(
                            onClick = { showRegisterDialog = true },
                            modifier = Modifier.testTag("register_btn")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Register", tint = primaryColor)
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
                    currentScreen = AppScreen.Home
                }

                when (currentScreen) {
                    AppScreen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onProviderClick = { provider ->
                                selectedProviderForDetail = provider
                                currentScreen = AppScreen.ProviderDetail
                            },
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            cardBgColor = cardBgColor,
                            textMainColor = textMainColor,
                            textSecColor = textSecColor,
                            isDark = isDark
                        )
                    }
                    AppScreen.ProviderDetail -> {
                        selectedProviderForDetail?.let { provider ->
                            ProviderDetailScreen(
                                provider = provider,
                                viewModel = viewModel,
                                onBack = { currentScreen = AppScreen.Home },
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
                        onSubmit = { user, pass ->
                            val success = viewModel.loginAdmin(user, pass)
                            showAdminLoginDialog = false
                            if (success) {
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
            }
        }
    }
}

// --- SUB-SCREENS ---

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onProviderClick: (ServiceProvider) -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color,
    isDark: Boolean
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val providers by viewModel.filteredProviders.collectAsState()
    
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    // Smart assistant chatbot states
    var isChatOpen by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Welcoming Card displaying the customizable welcome message
        Spacer(modifier = Modifier.height(12.dp))
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
            }
        }

        // Expanded/Horizontal categories selector carousel
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isArabic) "الأقسام المتاحة" else "Departments Available",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textMainColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" filter Pill
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedCatId == null) primaryColor else cardBgColor
                ),
                modifier = Modifier
                    .clickable { viewModel.selectCategory(null) }
                    .testTag("cat_all_button")
            ) {
                Text(
                    text = if (isArabic) "الكل" else "All",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (selectedCatId == null) Color.White else textMainColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
            
            categories.forEach { cat ->
                val isSelected = selectedCatId == cat.id
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) primaryColor else cardBgColor
                    ),
                    modifier = Modifier
                        .clickable { viewModel.selectCategory(cat.id) }
                        .testTag("cat_button_${cat.id}")
                ) {
                    Text(
                        text = if (isArabic) cat.name_ar else cat.name_en,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isSelected) Color.White else textMainColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Core visual Search bar input field and providers grid
        Spacer(modifier = Modifier.height(14.dp))
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

        Spacer(modifier = Modifier.height(12.dp))

        // Providers results feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (providers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                    // Pinned provider crown icon styling
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
                                    // Price indicator label
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
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Avoid blocking bottom panels
                    }
                }
            }

            // --- SMART FLOATING CHAT ASSISTANT PANEL ---
            // Requirement 7: Circular shape, reduced size by 30% (button 44dp instead of 60dp), raised slightly higher next to writing field
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomEnd)
            ) {
                if (isChatOpen) {
                    // Chat window modal overlaying the floating action elegantly
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 56.dp)
                            .size(width = 310.dp, height = 360.dp)
                            .testTag("chat_window")
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Chat Window Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(primaryColor)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isArabic) "مساعد دليل الخدمات الذكي" else "Services Smart Assistant",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { isChatOpen = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            // Conversation Log list
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                reverseLayout = false,
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
                                            modifier = Modifier.fillMaxWidth(),
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

                            // Chat input writing row and Send action button
                            // Optimized: circle assistant reduced size by 30% next to writing bar and does not block send button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = chatInput,
                                    onValueChange = { chatInput = it },
                                    placeholder = { Text(if (isArabic) "اسألني شيئاً..." else "Ask me...") },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("chat_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
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

                // AI floating circular action button (RTL, 30% smaller, raised higher next to search bar spacing)
                FloatingActionButton(
                    onClick = { isChatOpen = !isChatOpen },
                    shape = CircleShape,
                    // 30% smaller than normal 56dp FAB: standard Mini Fab size of 40dp is perfect!
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .size(42.dp)
                        .testTag("ai_fab_icon"),
                    containerColor = secondaryColor,
                    contentColor = primaryColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Assistant",
                        modifier = Modifier.size(20.dp) // Reduced size proportionately
                    )
                }
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

    var showAddProviderSheet by remember { mutableStateOf(false) }

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
                text = if (isArabic) "قائمة المهن ومقدمي الخدمات الحاليين" else "Current Registered list",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textMainColor
            )
            IconButton(onClick = { showAddProviderSheet = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = primaryColor, modifier = Modifier.size(32.dp))
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
                // If normal supervisor (⚙️ icon login): Alert they do not have auth to accept registers
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تنبيه: لا يملك المشرف صلاحية الموافقة على عروض المتقاعدين والطلبات المعلقة؛ تتطلب دخول المالك من البوابة الخلفية."
                                   else "Notice: Supervisor accounts lack access to approve pending registries. Access via backdoor owner bypass.",
                            fontSize = 11.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
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
        AddProviderSheetDialog(
            isArabic = isArabic,
            categories = categories,
            onDismiss = { showAddProviderSheet = false },
            onSubmit = { nameAr, phone, catId, location ->
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
                    is_approved = true
                )
                viewModel.addApprovedProvider(newProv)
                showAddProviderSheet = false
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
    var customFooterText by remember { mutableStateOf(sysConfig.footer_text) }
    var customFooterPhone by remember { mutableStateOf(sysConfig.footer_phone) }

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
                Spacer(modifier = Modifier.height(8.dp))

                // Primary Color Field Hex String
                OutlinedTextField(
                    value = customPrimaryColor,
                    onValueChange = { customPrimaryColor = it },
                    label = { Text(if (isArabic) "كود لون التطبيق (Hex)" else "Application color accent (Hex)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val updatedConfig = sysConfig.copy(
                            app_name = customAppName,
                            welcomeMessage = welcomeString,
                            primary_color_hex = customPrimaryColor,
                            footer_text = customFooterText,
                            footer_phone = customFooterPhone
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
    onSubmit: (String, String) -> Unit
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

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
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(user, pass) },
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
                            DropdownMenuItem(
                                text = { Text(if (isArabic) cat.name_ar else cat.name_en) },
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
    onSubmit: (String, String, String, String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var phoneVal by remember { mutableStateOf("") }
    var locationAr by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("") }
    var showDropdownMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إضافة محترف يدوي" else "Add Authorized Professional Manual") },
        text = {
            Column {
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
                            DropdownMenuItem(
                                text = { Text(if (isArabic) cat.name_ar else cat.name_en) },
                                onClick = {
                                    categoryId = cat.id
                                    showDropdownMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nameAr.isNotBlank() && phoneVal.isNotBlank() && categoryId.isNotBlank()) {
                    onSubmit(nameAr, phoneVal, categoryId, locationAr)
                }
            }) {
                Text(if (isArabic) "إضافة المالك" else "Manual Register Approved")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}
