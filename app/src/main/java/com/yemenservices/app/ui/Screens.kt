package com.yemenservices.app.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yemenservices.app.data.*
import java.io.File
import java.util.UUID

// Local custom push notification notifier helper
fun sendLocalNotification(context: Context, title: String, message: String) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, "dalili_notifications_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    } catch (e: Exception) {
        // Fallback or safely pass
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: AppViewModel,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val supervisors by viewModel.supervisors.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()

    var isArabic by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf("home") } // "home", "providers", "about", "profile", "chat", "admin"
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedProvider by remember { mutableStateOf<ServiceProvider?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Dynamic Theming setup as specified in design and admin config
    val primaryColor = remember(appConfig.primary_color_hex) {
        try { Color(android.graphics.Color.parseColor(appConfig.primary_color_hex)) } 
        catch (e: Exception) { Color(0xFF1B5E20) }
    }
    val secondaryColor = remember(appConfig.secondary_color_hex) {
        try { Color(android.graphics.Color.parseColor(appConfig.secondary_color_hex)) } 
        catch (e: Exception) { Color(0xFFFFC107) }
    }

    val scaffoldBgColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBgColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textMainColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF1A1A1A)
    val textSubColor = if (isDarkTheme) Color(0xFFBDBDBD) else Color(0xFF616161)

    // Backdoor secret tap detection counters on Header Logo
    var headerTapCount by remember { mutableStateOf(0) }
    var showBackdoorDialog by remember { mutableStateOf(false) }

    // Check for In-App update on launch
    var showUpdateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(appConfig.newest_apk_version) {
        if (appConfig.newest_apk_version.isNotBlank() && appConfig.newest_apk_version != "2.0") {
            showUpdateDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clickable {
                                headerTapCount++
                                if (headerTapCount >= 5) {
                                    headerTapCount = 0
                                    showBackdoorDialog = true
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(secondaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "D",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isArabic) "دليلي" else "Dalili",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMainColor
                            )
                            Text(
                                text = if (isArabic) "دليل المهن اليمني" else "Yemen Services Directory",
                                fontSize = 10.sp,
                                color = textSubColor
                            )
                        }
                    }
                },
                actions = {
                    // Top-bar navigation triggers
                    IconButton(onClick = { currentScreen = "home" }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = if (currentScreen == "home") primaryColor else textSubColor)
                    }
                    IconButton(onClick = { isArabic = !isArabic }) {
                        Icon(Icons.Default.Language, contentDescription = "Language", tint = textSubColor)
                    }
                    IconButton(onClick = { currentScreen = "profile" }) {
                        Icon(Icons.Default.Person, contentDescription = "Referral & Profile", tint = if (currentScreen == "profile") primaryColor else textSubColor)
                    }
                    IconButton(onClick = { currentScreen = "about" }) {
                        Icon(Icons.Default.Info, contentDescription = "About app", tint = if (currentScreen == "about") primaryColor else textSubColor)
                    }
                    IconButton(onClick = { viewModel.toggleTheme(!isDarkTheme) }) {
                        Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Dark Theme", tint = textSubColor)
                    }
                    // Admin Access Gear Triggered beside Language Toggle
                    IconButton(onClick = { 
                        if (currentAdmin != null) {
                            currentScreen = "admin"
                        } else {
                            showBackdoorDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Admin Area", tint = if (currentScreen == "admin") primaryColor else textSubColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBgColor)
            )
        },
        bottomBar = {
            // Elegant footer with "MAW 777644670" (support phone is customizable by Admin)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBgColor)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "دليلي للخدمات والمهن باليمن © ٢٠٢٦",
                    fontSize = 9.sp,
                    color = textSubColor
                )
                Text(
                    text = "تطوير Maher ahmed — رقم المبرمج والمصمم: ${appConfig.footer_phone}",
                    fontSize = 9.5.sp, // Reduced 15% smaller
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${appConfig.footer_phone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    }
                )
            }
        },
        floatingActionButton = {
            // Labeled small floating assistant circular widget labeled "خدمات"
            FloatingActionButton(
                onClick = { currentScreen = "chat" },
                containerColor = primaryColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = "AI")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "خدمات" else "Assistant",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = scaffoldBgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    categories = categories,
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    cardBgColor = cardBgColor,
                    textMain = textMainColor,
                    textSub = textSubColor,
                    appConfig = appConfig,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onCategorySelect = {
                        selectedCategory = it
                        currentScreen = "providers"
                    }
                )
                "providers" -> ProvidersScreen(
                    category = selectedCategory,
                    providers = providers.filter { it.category_id == selectedCategory?.id && it.is_approved },
                    reviews = reviews,
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    cardBgColor = cardBgColor,
                    textMain = textMainColor,
                    textSub = textSubColor,
                    appConfig = appConfig,
                    viewModel = viewModel,
                    onBack = { currentScreen = "home" },
                    onProviderSelect = {
                        selectedProvider = it
                    }
                )
                "about" -> AboutScreen(
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    cardBgColor = cardBgColor,
                    textMain = textMainColor,
                    textSub = textSubColor,
                    appConfig = appConfig
                )
                "profile" -> ProfileScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    cardBgColor = cardBgColor,
                    textMain = textMainColor,
                    textSub = textSubColor
                )
                "chat" -> ChatScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    cardBgColor = cardBgColor,
                    textMain = textMainColor,
                    textSub = textSubColor,
                    appConfig = appConfig
                )
                "admin" -> AdminDashboard(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    cardBgColor = cardBgColor,
                    textMain = textMainColor,
                    textSub = textSubColor,
                    appConfig = appConfig,
                    categories = categories,
                    providers = providers,
                    reviews = reviews,
                    supervisors = supervisors,
                    onLogout = {
                        viewModel.logoutSupervisor()
                        currentScreen = "home"
                    }
                )
            }

            // BACKDOOR SECRET ACCESS PASSWORD DIALOG INTERFACE
            if (showBackdoorDialog) {
                var passwordInput by remember { mutableStateOf("") }
                var usernameInput by remember { mutableStateOf("") }
                var rememberPass by remember { mutableStateOf(true) } // Save password check box
                val sharedPrefs = remember { context.getSharedPreferences("dalili_backdoor_prefs", 0) }

                LaunchedEffect(Unit) {
                    usernameInput = sharedPrefs.getString("saved_user", "") ?: ""
                    passwordInput = sharedPrefs.getString("saved_pass", "") ?: ""
                }

                Dialog(onDismissRequest = { showBackdoorDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "بوابة المشرفين" else "Supervisor Backdoor",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMainColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text(if (isArabic) "اسم المستخدم" else "Username") },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = rememberPass,
                                    onCheckedChange = { rememberPass = it },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                )
                                Text(
                                    text = if (isArabic) "حفظ معلومات الدخول" else "Save password credentials",
                                    color = textMainColor,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { showBackdoorDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                ) {
                                    Text(if (isArabic) "إلغاء" else "Cancel", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        // Support built-in backup code `maher--736462` too!
                                        val isDirectSecret = (usernameInput.trim() == "maher" && passwordInput.trim() == "736462")
                                        val success = if (isDirectSecret) {
                                            viewModel.loginSupervisor("maher", "736462")
                                        } else {
                                            viewModel.loginSupervisor(usernameInput, passwordInput)
                                        }

                                        if (success) {
                                            if (rememberPass) {
                                                sharedPrefs.edit()
                                                    .putString("saved_user", usernameInput)
                                                    .putString("saved_pass", passwordInput)
                                                    .apply()
                                            } else {
                                                sharedPrefs.edit().clear().apply()
                                            }
                                            showBackdoorDialog = false
                                            currentScreen = "admin"
                                            Toast.makeText(context, "مرحباً يا غالي تم تسجيل دخولك بنجاح!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "عذراً معلومات الدخول غير دقيقة", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text(if (isArabic) "دخول" else "Login", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // IN-APP UPDATE SYSTEM OVERLAY DIALOG DOWNLOADMANAGER
            if (showUpdateDialog) {
                Dialog(onDismissRequest = { showUpdateDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = primaryColor, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isArabic) "يتوفر تحديث جديد!" else "New Update Available!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMainColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "يتوفر إصدار أحدث من تطبيق دليلي (النسخة ${appConfig.newest_apk_version}) على الخادم. هل ترغب بتحميله وتثبيته الآن؟"
                                    else "A new update (${appConfig.newest_apk_version}) of Dalili is ready. Would you like to download and install it now?",
                                fontSize = 13.sp,
                                color = textSubColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(onClick = { showUpdateDialog = false }) {
                                    Text(if (isArabic) "لاحقاً" else "Later", color = textSubColor)
                                }
                                Button(
                                    onClick = {
                                        try {
                                            val i = Intent(Intent.ACTION_VIEW, Uri.parse(appConfig.apk_download_url))
                                            context.startActivity(i)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "فشل بدء التحميل: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        showUpdateDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text(if (isArabic) "تحميل الآن" else "Download Now", color = Color.White)
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
fun HomeScreen(
    categories: List<Category>,
    isArabic: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color,
    appConfig: AppConfig,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (Category) -> Unit
) {
    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories else {
            categories.filter {
                it.name_ar.contains(searchQuery, ignoreCase = true) || 
                it.name_en.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcoming Card (Custom/AI Greeting) 15% smaller
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(13.dp) // Smaller padding
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "مساعد دليلي الذكي" else "Dalili Smart Assistant",
                        color = primaryColor,
                        fontSize = 12.sp, // Reduced 15%
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Beautifully smaller welcome text
                Text(
                    text = if (isArabic) appConfig.welcome_msg_ar else appConfig.welcome_msg_en,
                    color = textMain,
                    fontSize = 11.sp, // Reduced 15%
                    lineHeight = 15.sp
                )
            }
        }

        // Search Bar 15% smaller and center aligned
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(if (isArabic) "ابحث عن قسم أو تخصص مهني..." else "Search for sectors...", color = Color.Gray, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(44.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = textMain),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.35f),
                    focusedContainerColor = cardBgColor,
                    unfocusedContainerColor = cardBgColor
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }

        // Categories Grid
        Text(
            text = if (isArabic) "الأقسام المتاحة" else "Sectors & Skills",
            color = textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
        )

        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "لا توجد نتائج مطابقة لطلبك" else "No matching items found",
                    color = textSub,
                    fontSize = 13.sp
                )
            }
        } else {
            // Display Grid
            val rows = filteredCategories.chunked(2)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { cat ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clickable { onCategorySelect(cat) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (!cat.image_url.isNullOrBlank()) {
                                    AsyncImage(
                                        model = cat.image_url,
                                        contentDescription = cat.name_en,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.25f
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val iconVector = when (cat.icon.lowercase()) {
                                            "plumbing", "build" -> Icons.Default.Build
                                            "bolt", "electricity" -> Icons.Default.Bolt
                                            "ac_unit", "cool" -> Icons.Default.AcUnit
                                            "smartphone" -> Icons.Default.Smartphone
                                            "architecture" -> Icons.Default.Architecture
                                            "local_shipping" -> Icons.Default.LocalShipping
                                            else -> Icons.Default.WorkOutline
                                        }
                                        Icon(iconVector, contentDescription = null, tint = primaryColor)
                                    }
                                    Text(
                                        text = if (isArabic) cat.name_ar else cat.name_en,
                                        color = textMain,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ProvidersScreen(
    category: Category?,
    providers: List<ServiceProvider>,
    reviews: List<Review>,
    isArabic: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color,
    appConfig: AppConfig,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onProviderSelect: (ServiceProvider) -> Unit
) {
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }

    // Advanced Filtering States
    var minRating by remember { mutableStateOf(0.0) }
    var selectDistance by remember { mutableStateOf("all") } // "all", "close", "medium", "far"
    var selectPrice by remember { mutableStateOf("all") }       // "all", "low", "medium", "high"

    // Propose provider registration dialog
    var showProposeDialog by remember { mutableStateOf(false) }

    // Selected practitioner details modal overlay
    var activeProviderDetails by remember { mutableStateOf<ServiceProvider?>(null) }

    val filteredProviders = remember(providers, reviews, minRating, selectDistance, selectPrice) {
        providers.filter { prov ->
            // Filter distance
            val matchDistance = (selectDistance == "all" || prov.distance.lowercase() == selectDistance.lowercase())
            // Filter price
            val matchPrice = (selectPrice == "all" || prov.price_range.lowercase() == selectPrice.lowercase())
            // Filter rating
            val pReviews = reviews.filter { it.provider_id == prov.id }
            val avgRating = if (pReviews.isNotEmpty()) pReviews.map { it.rating }.average() else 5.0
            val matchRating = avgRating >= minRating

            matchDistance && matchPrice && matchRating
        }.sortedWith(compareBy({ !it.is_pinned }, { if (appConfig.list_sort_mode == "name") (if (isArabic) it.name_ar else it.name_en) else it.id }))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Navigation Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(if (isArabic) Icons.Default.ArrowForward else Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
            }
            Text(
                text = "${if (isArabic) category?.name_ar else category?.name_en}",
                color = textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                // Filter triggers
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Advanced Filters", tint = primaryColor)
                }
                IconButton(onClick = { showProposeDialog = true }) {
                    Icon(Icons.Default.AddBusiness, contentDescription = "Propose Practitioner", tint = primaryColor)
                }
            }
        }

        if (filteredProviders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "عذراً، لم نجد أي مهني يطابق هذه الفلترة" else "No service providers found with selected parameters",
                    color = textSub,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProviders, key = { it.id }) { provider ->
                    val pReviews = reviews.filter { it.provider_id == provider.id }
                    val avgRating = if (pReviews.isNotEmpty()) pReviews.map { it.rating }.average() else 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeProviderDetails = provider },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (!provider.image_url.isNullOrBlank()) {
                                        AsyncImage(
                                            model = provider.image_url,
                                            contentDescription = provider.name_en,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isArabic) provider.name_ar else provider.name_en,
                                                color = textMain,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (provider.is_pinned) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(secondaryColor, shape = RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isArabic) "مميز" else "Pinned",
                                                        color = Color.Black,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "هاتف: ${provider.phone}",
                                            color = textSub,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Quick communication row (WhatsApp, SMS, Call)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            viewModel.trackCallClicked()
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = primaryColor)
                                    }
                                    if (provider.whatsapp.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                viewModel.trackCallClicked()
                                                try {
                                                    val url = "https://api.whatsapp.com/send?phone=${provider.whatsapp}"
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {}
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                                        }
                                    }
                                }
                            }

                            // Star rating summary display
                            if (pReviews.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${String.format("%.1f", avgRating)}/5 (${pReviews.size} ${if (isArabic) "تقييمات" else "reviews"})",
                                        fontSize = 11.sp,
                                        color = textMain,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ADVANCED FILTER DIALOG
    if (showFilterSheet) {
        Dialog(onDismissRequest = { showFilterSheet = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = if (isArabic) "خيارات الفلترة والترتيب" else "Advanced Filters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Distance filter options
                    Text(text = if (isArabic) "فلترة بالمسافة" else "Filter by Distance", fontSize = 12.sp, color = textSub)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all", "close", "medium", "far").forEach { ds ->
                            val txt = when(ds) {
                                "all" -> if (isArabic) "الكل" else "All"
                                "close" -> if (isArabic) "قريب" else "Close"
                                "medium" -> if (isArabic) "متوسط" else "Med"
                                else -> if (isArabic) "بعيد" else "Far"
                            }
                            FilterChip(
                                selected = selectDistance == ds,
                                onClick = { selectDistance = ds },
                                label = { Text(txt, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Price range filter options
                    Text(text = if (isArabic) "فلترة بالسعر المتوقع" else "Filter by Price Class", fontSize = 12.sp, color = textSub)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all", "low", "medium", "high").forEach { pc ->
                            val txt = when(pc) {
                                "all" -> if (isArabic) "الكل" else "All"
                                "low" -> if (isArabic) "منخفض" else "Low"
                                "medium" -> if (isArabic) "متوسط" else "Med"
                                else -> if (isArabic) "مرتفع" else "High"
                            }
                            FilterChip(
                                selected = selectPrice == pc,
                                onClick = { selectPrice = pc },
                                label = { Text(txt, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Rating filter options
                    Text(text = if (isArabic) "الحد الأدنى للتقييم" else "Minimum Rating threshold", fontSize = 12.sp, color = textSub)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.0, 3.0, 4.0, 5.0).forEach { rt ->
                            val txt = if (rt == 0.0) { if (isArabic) "الجميع" else "Any" } else "$rt ★"
                            FilterChip(
                                selected = minRating == rt,
                                onClick = { minRating = rt },
                                label = { Text(txt, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showFilterSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "تطبيق الفلترة" else "Apply Filters", color = Color.White)
                    }
                }
            }
        }
    }

    // PROPOSE DISH/PROVIDER REGISTRATION DIALOG (pending_providers logic)
    if (showProposeDialog) {
        var pNameAr by remember { mutableStateOf("") }
        var pNameEn by remember { mutableStateOf("") }
        var pPhone by remember { mutableStateOf("") }
        var pWhatsapp by remember { mutableStateOf("") }
        var pSms by remember { mutableStateOf("") }
        var pDistance by remember { mutableStateOf("medium") }
        var pPrice by remember { mutableStateOf("medium") }

        Dialog(onDismissRequest = { showProposeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (isArabic) "طلب تسجيل مقدم خدمة جديد" else "Request New Practitioner Listing",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "سجل بياناتك كمزود مهنة ليتم المصادقة عليها من قبل الإدارة الفنية فورا"
                             else "Fill your service parameters for supervisor validation approval",
                        fontSize = 11.sp,
                        color = textSub
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pNameAr,
                        onValueChange = { pNameAr = it },
                        label = { Text(if (isArabic) "الاسم بالكامل (عربي)" else "Full Name (Arabic)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pNameEn,
                        onValueChange = { pNameEn = it },
                        label = { Text(if (isArabic) "الاسم بالكامل (إنجليزي)" else "Full Name (English)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pPhone,
                        onValueChange = { pPhone = it },
                        label = { Text(if (isArabic) "رقم الهاتف" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pWhatsapp,
                        onValueChange = { pWhatsapp = it },
                        label = { Text(if (isArabic) "رقم الواتساب" else "WhatsApp contact") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { showProposeDialog = false }) {
                            Text(if (isArabic) "إلغاء" else "Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (pNameAr.isBlank() || pPhone.isBlank()) {
                                    Toast.makeText(context, "الرجاء تعبئة البيانات الأساسية", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newProvider = ServiceProvider(
                                        id = UUID.randomUUID().toString(),
                                        category_id = category?.id ?: "",
                                        name_ar = pNameAr,
                                        name_en = pNameEn.ifBlank { pNameAr },
                                        phone = pPhone,
                                        whatsapp = pWhatsapp,
                                        is_approved = false, // pending_providers review queue!
                                        distance = pDistance,
                                        price_range = pPrice
                                    )
                                    viewModel.addProvider(newProvider)
                                    showProposeDialog = false
                                    // Send out user-facing local notification instantly as requested
                                    sendLocalNotification(
                                        context,
                                        if (isArabic) "طلب تسجيل قيد المراجعة" else "Listing Request Received",
                                        if (isArabic) "تم استلام معلومات تقديمك لـ ${pNameAr} وسيتم التفعيل فوراً!" 
                                            else "We received your listing request. Approval in progress."
                                    )
                                    Toast.makeText(context, "تم حفظ طلبك وسيتم تفعيله من الإدارة فورا!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text(if (isArabic) "تقديم الطلب" else "Submit", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // THE COMPREHENSIVE PROVIDER DETAILS SCREEN MODAL & GOOGLE MAPS
    activeProviderDetails?.let { prov ->
        val pReviews = reviews.filter { it.provider_id == prov.id }
        val avgRating = if (pReviews.isNotEmpty()) pReviews.map { it.rating }.average() else 5.0
        var guestName by remember { mutableStateOf("") }
        var guestRating by remember { mutableStateOf(5.0) }
        var guestComment by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { activeProviderDetails = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "بطاقة المهني والاتصال" else "Practitioner Hub",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMain
                        )
                        IconButton(onClick = { activeProviderDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = primaryColor)
                        }
                    }

                    // Avatar Visual Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!prov.image_url.isNullOrBlank()) {
                            AsyncImage(
                                model = prov.image_url,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = if (isArabic) prov.name_ar else prov.name_en, color = textMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = "رقم المهني: ${prov.phone}", color = textSub, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.background(primaryColor.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                                    Text(text = "${if (isArabic) "سعر متوقع:" else "Price:"} ${prov.price_range}", fontSize = 10.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.background(secondaryColor.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                                    Text(text = prov.distance, fontSize = 10.sp, color = secondaryColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.Gray.copy(alpha = 0.15f))

                    // GOOGLE MAPS INTEGRATION CANVAS INTERFACE
                    if (appConfig.show_maps_enabled) {
                        Text(
                            text = if (isArabic) "موقع مزود الخدمة الجغرافي" else "Geographical Location Map",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMain
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Drawn Simulated Maps Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0F2F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color.Red, modifier = Modifier.size(28.dp))
                                Text(
                                    text = "Sana'a, Yemen (Coordinate Location Map Node)",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        viewModel.trackCallClicked()
                                        try {
                                            // Open real maps routing
                                            val uri = Uri.parse("geo:${prov.latitude},${prov.longitude}?q=${prov.latitude},${prov.longitude}(${prov.name_en})")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "تم فتح الخرائط البديلة لهاتفك", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(if (isArabic) "افتح الاتجاهات" else "Get Directions", fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Multi Contact Triggers
                    Text(text = if (isArabic) "طرق تواصل إضافية:" else "Additional Contacts:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Call
                        Button(
                            onClick = {
                                viewModel.trackCallClicked()
                                try {
                                    val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${prov.phone}"))
                                    context.startActivity(i)
                                } catch (e:Exception){}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "اتصال" else "Call", fontSize = 10.sp)
                        }
                        // SMS 
                        Button(
                            onClick = {
                                viewModel.trackCallClicked()
                                try {
                                    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${prov.phone}")).apply {
                                        putExtra("sms_body", if (isArabic) "السلام عليكم، وجدتك عبر تطبيق دليلي وأرغب في خدمتك" else "Hello, I found your service via Dalili app")
                                    }
                                    context.startActivity(i)
                                }catch(e:Exception){}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "رسالة SMS" else "SMS", fontSize = 10.sp)
                        }
                        // Whatsapp
                        if (prov.whatsapp.isNotBlank()) {
                            Button(
                                onClick = {
                                    viewModel.trackCallClicked()
                                    try {
                                        val url = "https://api.whatsapp.com/send?phone=${prov.whatsapp}&text=" +
                                                Uri.encode("السلام عليكم، وجدتك عبر تطبيق دليلي للخدمات وأود الاستفسار")
                                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(i)
                                    }catch(e:Exception){}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("واتساب", fontSize = 10.sp)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.Gray.copy(alpha = 0.15f))

                    // Reviews List
                    if (appConfig.show_reviews_enabled) {
                        Text(
                            text = if (isArabic) "آراء وتقييمات العملاء" else "Practitioner Reviews",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMain
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (pReviews.isEmpty()) {
                            Text(
                                text = if (isArabic) "لا توجد تقييمات سابقة. كن أول من يضيف رأيه!" else "No feedback submitted yet.",
                                fontSize = 11.sp,
                                color = textSub
                            )
                        } else {
                            pReviews.take(4).forEach { rx ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(primaryColor.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = rx.user_name.ifBlank { "عميل زائر" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                        Row {
                                            (1..5).forEach { st ->
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (st <= rx.rating) secondaryColor else Color.Gray.copy(alpha=0.3f),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = rx.comment, fontSize = 11.sp, color = textMain)
                                }
                            }
                        }

                        // Add feedback form
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = if (isArabic) "أضف رأيك وقيّم الخدمة" else "Submit your feedback", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = guestName,
                            onValueChange = { guestName = it },
                            placeholder = { Text(if (isArabic) "اسمك الكريم..." else "Your Name...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = guestComment,
                            onValueChange = { guestComment = it },
                            placeholder = { Text(if (isArabic) "اكتب تعليقك هنا..." else "Review details...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isArabic) "التقييم:" else "Rating:", fontSize = 11.sp, color = textMain)
                            Spacer(modifier = Modifier.width(6.dp))
                            (1..5).forEach { rt ->
                                IconButton(onClick = { guestRating = rt.toDouble() }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (rt <= guestRating) secondaryColor else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                if (guestComment.isNotBlank()) {
                                    viewModel.addReview(
                                        Review(
                                            id = UUID.randomUUID().toString(),
                                            provider_id = prov.id,
                                            user_name = guestName.ifBlank { if (isArabic) "زائر مجهول" else "Visitor" },
                                            comment = guestComment,
                                            rating = guestRating
                                        )
                                    )
                                    guestComment = ""
                                    guestName = ""
                                    Toast.makeText(context, "شكراً لك، تم حفظ تقييمك فوراً!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isArabic) "إرسال التقييم" else "Send Feedback", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    isArabic: Boolean,
    primaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color,
    appConfig: AppConfig
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = primaryColor, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "دليلي - Dalili", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textMain)
                Text(text = "نسخة الإصدار 2.0", fontSize = 12.sp, color = textSub)

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isArabic) "تطبيق دليلي للخدمات والمهن هو الدليل الرقمي الأول والمتكامل لتوفير تواصل مباشر وسلس بين العملاء وكافة الحرفيين والمهنيين والمصالح والمهن في جميع أرجاء اليمن الحبيبة دون أي وسيط أو تكاليف إضافية."
                         else "Dalili app is your premier direct digital directory, facilitating direct seamless connections with plumbers, electricians, transport services, and and all skill providers around Yemen.",
                    fontSize = 12.sp,
                    color = textMain,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.15f))

                Text(text = if (isArabic) "الدعم الفني وخدمة العملاء" else "Technical Support Helpline", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
                Spacer(modifier = Modifier.height(8.dp))

                // Support dials customizable by admin
                Button(
                    onClick = {
                        try {
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${appConfig.support_whatsapp}"))
                            context.startActivity(i)
                        } catch (e: Exception){}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "تواصل واتساب الدعم" else "WhatsApp Helpline Support")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        try {
                            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${appConfig.support_email}"))
                            context.startActivity(i)
                        } catch (e: Exception){}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "راسلنا عبر الإيميل" else "Send Helpline Email")
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    isArabic: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color
) {
    val context = LocalContext.current
    val clipManager = LocalClipboardManager.current
    val inviteCode by viewModel.userInviteCode.collectAsStateWithLifecycle()
    val points by viewModel.userPoints.collectAsStateWithLifecycle()
    val hasReferred by viewModel.hasReferred.collectAsStateWithLifecycle()
    var referrerInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(secondaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CardMembership, contentDescription = null, tint = secondaryColor, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = if (isArabic) "نظام الدعوات والمكافآت" else "Invitation & Referrals", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textMain)
                Text(text = if (isArabic) "اكسب نقاط ترويجية معنا وادعُ أصدقاءك" else "Earn promotion points by inviting others", fontSize = 11.sp, color = textSub)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.15f))

                // Invite Code Box
                Text(text = if (isArabic) "كود دعوتك الفريد الفريد:" else "Your Unique Referral Code:", fontSize = 12.sp, color = textSub)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = inviteCode, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        IconButton(
                            onClick = {
                                clipManager.setText(AnnotatedString(inviteCode))
                                Toast.makeText(context, "تم نسخ كود دعوتك الفاخر!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (isArabic) "نقاط رصيدك المتوفرة:" else "Your Accumulated Points:", fontSize = 12.sp, color = textMain)
                    Text(text = "$points نقطة / Points", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = secondaryColor)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.15f))

                // Apply referer dialog
                if (!hasReferred) {
                    Text(text = if (isArabic) "هل دعاك صديق؟ أدخل الكود هنا لتكسب نقاط:" else "Referred by a partner? Enter code:", fontSize = 12.sp, color = textSub)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = referrerInput,
                        onValueChange = { referrerInput = it },
                        placeholder = { Text("DALILI-XXXXXX", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (viewModel.applyReferralCode(referrerInput)) {
                                Toast.makeText(context, "رائع! تم تفعيل كود الدعوة وتعبئة +50 نقطة لحسابك!", Toast.LENGTH_SHORT).show()
                                referrerInput = ""
                            } else {
                                Toast.makeText(context, "عذراً الكود غير صحيح أو قمت بالاستخدام مسبقاً", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "تفعيل المكافأة" else "Apply Referral Code", color = Color.White)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isArabic) "لقد استفدت بالفعل كعميل مدعو بنجاح ✓" else "You successfully claimed referral bonus ✓", fontSize = 11.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    viewModel: AppViewModel,
    isArabic: Boolean,
    primaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color,
    appConfig: AppConfig
) {
    val context = LocalContext.current
    val chatLog by viewModel.aiChatLog.collectAsStateWithLifecycle()
    val isGenerating by viewModel.aiGenerating.collectAsStateWithLifecycle()
    var userInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = primaryColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isArabic) "مساعدك المعزز (Offline/AI)" else "Yemen AI Assistant", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textMain)
            }
            IconButton(onClick = { viewModel.clearChatHistory() }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.Red.copy(alpha=0.6f))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat bubble feed areas
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(cardBgColor, RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatLog) { item ->
                val isUser = item.second
                val content = item.first
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isUser) primaryColor else primaryColor.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(
                                    topStart = 10.dp,
                                    topEnd = 10.dp,
                                    bottomStart = if (isUser) 10.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 10.dp
                                )
                            )
                            .padding(10.dp)
                            .widthIn(max = 240.dp)
                    ) {
                        Text(
                            text = content,
                            fontSize = 12.sp,
                            color = if (isUser) Color.White else textMain,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Generating indicators
        if (isGenerating) {
            Text(
                text = if (isArabic) "جاري الاستفسار والمطابقة ذكياً..." else "Inquiring assistant engines...",
                fontSize = 10.sp,
                color = primaryColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Input Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                placeholder = { Text(if (isArabic) "سؤال: ماهي الأقسام المتوفرة؟" else "Ask assistant...", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (userInput.isNotBlank()) {
                        viewModel.sendChatMessage(userInput)
                        userInput = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(primaryColor)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun AdminDashboard(
    viewModel: AppViewModel,
    isArabic: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color,
    appConfig: AppConfig,
    categories: List<Category>,
    providers: List<ServiceProvider>,
    reviews: List<Review>,
    supervisors: List<Supervisor>,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var adminTab by remember { mutableStateOf(0) } // 0: Sectors/Practitioners, 1: Supervisors, 2: Config, 3: Export, 4: Stats

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor.copy(alpha = 0.05f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "لوحة التحكم الفنية" else "Supervisor Panel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                TextButton(onClick = onLogout) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "خروج" else "Log Out", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
        ) {
            // Horizontal Admin Tab Strip
            ScrollableTabRow(
                selectedTabIndex = adminTab,
                edgePadding = 8.dp,
                containerColor = cardBgColor
            ) {
                listOf(
                    if (isArabic) "القطاعات" else "Sectors",
                    if (isArabic) "المشرفين" else "Supervisors",
                    if (isArabic) "الإعدادات" else "Settings",
                    if (isArabic) "تصدير" else "Export",
                    if (isArabic) "إحصائيات" else "Stats"
                ).forEachIndexed { i, title ->
                    Tab(
                        selected = adminTab == i,
                        onClick = { adminTab = i },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (adminTab) {
                    0 -> AdminSectorsTab(viewModel, categories, providers, isArabic, primaryColor, secondaryColor, cardBgColor, textMain, textSub)
                    1 -> AdminSupervisorsTab(viewModel, supervisors, isArabic, primaryColor, cardBgColor, textMain, textSub)
                    2 -> AdminConfigTab(viewModel, appConfig, isArabic, primaryColor, secondaryColor, cardBgColor, textMain, textSub)
                    3 -> AdminExportTab(providers, reviews, isArabic, primaryColor, cardBgColor, textMain, textSub)
                    4 -> AdminStatsTab(viewModel, providers, isArabic, primaryColor, secondaryColor, cardBgColor, textMain, textSub)
                }
            }
        }
    }
}

@Composable
fun AdminSectorsTab(
    viewModel: AppViewModel,
    categories: List<Category>,
    providers: List<ServiceProvider>,
    isArabic: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color
) {
    val context = LocalContext.current
    var catNameAr by remember { mutableStateOf("") }
    var catNameEn by remember { mutableStateOf("") }
    var catIcon by remember { mutableStateOf("build") }

    Text(text = if (isArabic) "إضافة قسم/تخصص جديد:" else "Create New Category Sector:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = catNameAr, onValueChange = { catNameAr = it }, label = { Text("الاسم عربي") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = catNameEn, onValueChange = { catNameEn = it }, label = { Text("الاسم إنجليزي") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    Button(
        onClick = {
            if (catNameAr.isNotBlank()) {
                viewModel.addCategory(
                    Category(
                        id = UUID.randomUUID().toString(),
                        name_ar = catNameAr,
                        name_en = catNameEn.ifBlank { catNameAr },
                        icon = catIcon,
                        order_index = categories.size + 1
                    )
                )
                catNameAr = ""
                catNameEn = ""
                Toast.makeText(context, "تمت إضافة التخصص الجديد!", Toast.LENGTH_SHORT).show()
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isArabic) "إضافة التخصص" else "Add Sector", color = Color.White)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha=0.15f))

    // List of pending approvals review configurations
    Text(text = if (isArabic) "طلبات تسجيل قيد الانتظار (Pending Approvals):" else "Registration Approval Queue:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
    val pendingProviders = providers.filter { !it.is_approved }
    if (pendingProviders.isEmpty()) {
        Text(text = if (isArabic) "لا توجد طلبات معلقة حالياً" else "Clean review queue.", fontSize = 11.sp, color = textSub)
    } else {
        pendingProviders.forEach { prov ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = prov.name_ar, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
                        Text(text = "هاتف: ${prov.phone}", fontSize = 11.sp, color = textSub)
                    }
                    Button(
                        onClick = {
                            viewModel.approveProvider(prov.id)
                            sendLocalNotification(
                                context,
                                "تهانينا! تم تفعيل حسابك",
                                "${prov.name_ar} تفعيلك على دليل اليمن كشريك مهني نشط"
                            )
                            Toast.makeText(context, "تم تفعيل مقدم الخدمة ومصادقته وإرسال إشعار فوري له!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text(if (isArabic) "تفعيل وموافقة" else "Approve", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha=0.15f))

    // Master management lists (delete category/providers, toggle pin)
    Text(text = if (isArabic) "إدارة القطاعات والمهن المضافة:" else "Current active list:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
    categories.forEach { cat ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "- ${cat.name_ar}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
                    IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha=0.6f))
                    }
                }
                // List of subproviders under this sector configuration 
                val subProv = providers.filter { it.category_id == cat.id && it.is_approved }
                subProv.forEach { sp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "• ${sp.name_ar}", fontSize = 11.sp, color = textMain)
                        Row {
                            IconButton(onClick = { viewModel.togglePinProvider(sp) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = if (sp.is_pinned) secondaryColor else Color.Gray, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.deleteProvider(sp.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSupervisorsTab(
    viewModel: AppViewModel,
    supervisors: List<Supervisor>,
    isArabic: Boolean,
    primaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color
) {
    val context = LocalContext.current
    var superUsername by remember { mutableStateOf("") }
    var superPassword by remember { mutableStateOf("") }
    var superRole by remember { mutableStateOf("moderator") } // "admin" or "moderator"

    Text(text = if (isArabic) "إضافة مشرف فني جديد (Supervisors Management):" else "Create Active Supervisor Account:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = superUsername, onValueChange = { superUsername = it }, label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = superPassword, onValueChange = { superPassword = it }, label = { Text("كلمة السر") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = superRole == "moderator", onClick = { superRole = "moderator" })
        Text("مشرف عادي (حذف تقييمات/فقط)")
        Spacer(modifier = Modifier.width(12.dp))
        RadioButton(selected = superRole == "admin", onClick = { superRole = "admin" })
        Text("مدير عام (صلاحيات كاملة)")
    }
    
    Button(
        onClick = {
            if (superUsername.isNotBlank()) {
                viewModel.addSupervisor(
                    Supervisor(
                        id = UUID.randomUUID().toString(),
                        username = superUsername,
                        password = superPassword,
                        role = superRole
                    )
                )
                superUsername = ""
                superPassword = ""
                Toast.makeText(context, "تمت إضافة المشرف بنجاح!", Toast.LENGTH_SHORT).show()
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isArabic) "حفظ المشرف الفني" else "Save Supervisor", color = Color.White)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha=0.15f))

    // List of active supervisors
    Text(text = if (isArabic) "قائمة المشرفين المعتمدين:" else "Active Supervisor Hub:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
    supervisors.forEach { sv ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(primaryColor.copy(alpha=0.03f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = sv.username, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
                Text(text = "الدور: ${if (sv.role == "admin") "مدير عام" else "مشرف فني"}", fontSize = 11.sp, color = textSub)
            }
            IconButton(onClick = { viewModel.deleteSupervisor(sv.id) }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha=0.6f))
            }
        }
    }
}

@Composable
fun AdminConfigTab(
    viewModel: AppViewModel,
    appConfig: AppConfig,
    isArabic: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color
) {
    val context = LocalContext.current
    var configWelcomeAr by remember { mutableStateOf(appConfig.welcome_msg_ar) }
    var configWelcomeEn by remember { mutableStateOf(appConfig.welcome_msg_en) }
    var updateFileLink by remember { mutableStateOf(appConfig.apk_download_url) }
    var newestBuildNum by remember { mutableStateOf(appConfig.newest_apk_version) }
    
    // customizable helpline configurations setup
    var customFooterPhone by remember { mutableStateOf(appConfig.footer_phone) }
    var customHelplineWhatsapp by remember { mutableStateOf(appConfig.support_whatsapp) }
    var customHelplineEmail by remember { mutableStateOf(appConfig.support_email) }

    // Toggle switchable settings parameters
    var toggleMaps by remember { mutableStateOf(appConfig.show_maps_enabled) }
    var toggleReviews by remember { mutableStateOf(appConfig.show_reviews_enabled) }
    var toggleReferrals by remember { mutableStateOf(appConfig.invite_codes_enabled) }

    // Editable color presets hex code 
    var customPrimaryColorHex by remember { mutableStateOf(appConfig.primary_color_hex) }
    var customSecondaryColorHex by remember { mutableStateOf(appConfig.secondary_color_hex) }

    Text(text = if (isArabic) "تحرير الإعدادات العامة والربط الفني:" else "Update Config & Helper Services:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
    Spacer(modifier = Modifier.height(4.dp))

    OutlinedTextField(value = configWelcomeAr, onValueChange = { configWelcomeAr = it }, label = { Text("رسالة الترحيب بالعربية") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = configWelcomeEn, onValueChange = { configWelcomeEn = it }, label = { Text("رسالة الترحيب بالإنجليزية") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = customFooterPhone, onValueChange = { customFooterPhone = it }, label = { Text("رقم مبرمج التطبيق وتذييل الشاشات") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = customHelplineWhatsapp, onValueChange = { customHelplineWhatsapp = it }, label = { Text("رقم واتساب دعم المهنيين") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = customHelplineEmail, onValueChange = { customHelplineEmail = it }, label = { Text("إيميل الدعم") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = updateFileLink, onValueChange = { updateFileLink = it }, label = { Text("رابط تحميل تحديث التطبيق (.apk)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = newestBuildNum, onValueChange = { newestBuildNum = it }, label = { Text("رقم أحدث إصدار معتمد بالخادم") }, modifier = Modifier.fillMaxWidth())
    
    Spacer(modifier = Modifier.height(10.dp))
    Text(text = if (isArabic) "التعديل اللحظي لألوان التطبيق (مزامنة فورية):" else "Live Dual-Color Schemes Sync:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = customPrimaryColorHex, onValueChange = { customPrimaryColorHex = it }, label = { Text("اللون الأساسي هكس (e.g. #1B5E20)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(value = customSecondaryColorHex, onValueChange = { customSecondaryColorHex = it }, label = { Text("اللون الثانوي هكس (e.g. #FFC107)") }, modifier = Modifier.fillMaxWidth())

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive switchable toggles
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("تفعيل خرائط Google Maps والاتجاهات جغرافيّاً")
        Switch(checked = toggleMaps, onCheckedChange = { toggleMaps = it }, colors = SwitchDefaults.colors(checkedThumbColor = primaryColor))
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("السماح للمستخدمين بإضافة تقييمات مكتوبة")
        Switch(checked = toggleReviews, onCheckedChange = { toggleReviews = it }, colors = SwitchDefaults.colors(checkedThumbColor = primaryColor))
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("تفعيل نظام الدعوات وكسب نقاط Referrals")
        Switch(checked = toggleReferrals, onCheckedChange = { toggleReferrals = it }, colors = SwitchDefaults.colors(checkedThumbColor = primaryColor))
    }

    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = {
            viewModel.updateAppConfig(
                appConfig.copy(
                    welcome_msg_ar = configWelcomeAr,
                    welcome_msg_en = configWelcomeEn,
                    apk_download_url = updateFileLink,
                    newest_apk_version = newestBuildNum,
                    footer_phone = customFooterPhone,
                    support_whatsapp = customHelplineWhatsapp,
                    support_email = customHelplineEmail,
                    show_maps_enabled = toggleMaps,
                    show_reviews_enabled = toggleReviews,
                    invite_codes_enabled = toggleReferrals,
                    primary_color_hex = customPrimaryColorHex,
                    secondary_color_hex = customSecondaryColorHex
                )
            )
            Toast.makeText(context, "تم حفظ الإعدادات الفنية وبثها لجميع المستخدمين بنجاح!", Toast.LENGTH_SHORT).show()
        },
        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isArabic) "حفظ وبث وثيقة الإعدادات" else "Save & Propagate", color = Color.White)
    }
}

@Composable
fun AdminExportTab(
    providers: List<ServiceProvider>,
    reviews: List<Review>,
    isArabic: Boolean,
    primaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color
) {
    val clipManager = LocalClipboardManager.current
    val context = LocalContext.current

    Text(text = if (isArabic) "تصدير قواعد البيانات وقوائم المهنيين والتقييمات:" else "Export and Schema Backups:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = if (isArabic) "تتيح لك هذه المنصة توليد وثائق CSV وتقارير لنسخ بياناتك بسرعة" else "Export lists formatting directly as standard plain tables", fontSize = 11.sp, color = textSub)

    Spacer(modifier = Modifier.height(12.dp))

    // 1. Export Active Practitioner Database to CSV Excel
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = if (isArabic) "تصدير مقدمي الخدمات إلى Excel / CSV:" else "Export Practitioners database schema:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val header = "ID,Name(AR),Name(EN),Phone,Distance,PriceClass,Approved\n"
                    val csv = providers.joinToString("\n") { 
                        "${it.id},\"${it.name_ar}\",\"${it.name_en}\",${it.phone},${it.distance},${it.price_range},${it.is_approved}" 
                    }
                    clipManager.setText(AnnotatedString(header + csv))
                    Toast.makeText(context, "تم نسخ قاعدة بيانات مزودي الخدمة بتنسيق CSV!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isArabic) "انسخ التقرير بتنسيق (CSV)" else "Copy Practitioners CSV Database")
            }
        }
    }

    // 2. Export Reviewers files to PDF format text
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = if (isArabic) "تصدير التقييمات إلى تقرير (PDF Style):" else "Export Reviews Report:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textMain)
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val reportHeader = "==================================================\n" +
                                       "             DALILI APP FEEDBACK REPORT           \n" +
                                       "==================================================\n\n"
                    val reportContent = reviews.joinToString("\n--------------------------------------------------\n") { 
                        "User: ${it.user_name}\nRating: ${it.rating} Stars\nReview: ${it.comment}" 
                    }
                    clipManager.setText(AnnotatedString(reportHeader + reportContent))
                    Toast.makeText(context, "تم توليد وتصدير ملف التقييمات كتقرير منقح للحافظة!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isArabic) "تصدير تقرير التقييمات كملف" else "Generate Plain Report PDF text")
            }
        }
    }
}

@Composable
fun AdminStatsTab(
    viewModel: AppViewModel,
    providers: List<ServiceProvider>,
    isArabic: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMain: Color,
    textSub: Color
) {
    val totalCalls by viewModel.totalTrackedCalls.collectAsStateWithLifecycle()

    Text(text = if (isArabic) "إحصائيات وقراءات الاستخدام (App Analytic Logs):" else "Master Analytics & Telemetry Logs:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMain)
    Spacer(modifier = Modifier.height(10.dp))

    // Active users count (mock active sessions)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor.copy(alpha=0.08f), RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(text = if (isArabic) "عدد المستخدمين النشطين (Live Users Profile):" else "Analytical Active Users Base:", fontSize = 12.sp, color = textMain)
                Text(text = "184 مستخدم نشط / Active Users", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            }
        }
    }

    // Providers
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor.copy(alpha=0.08f), RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(text = if (isArabic) "مزودي الخدمات والمهنيين المعتمدين:" else "Approved Partner Skill Base:", fontSize = 12.sp, color = textMain)
                val appP = providers.filter { it.is_approved }.size
                Text(text = "$appP شريك مهني مدرج بالدليل", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            }
        }
    }

    // Number of calls (calls tracked clicking dial)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(secondaryColor.copy(alpha=0.08f), RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(text = if (isArabic) "إجمالي المكالمات والتواصل المباشر عبر التطبيق:" else "Aggregated Communications Click Rate:", fontSize = 12.sp, color = textMain)
                Text(text = "$totalCalls محاولة تواصل مباشر / Connect attempts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = secondaryColor)
            }
        }
    }

    // Pending registrees queue analytic counts
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor.copy(alpha=0.08f), RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(text = if (isArabic) "طلبات التسجيل المعلقة (Pending Requests):" else "Pending Review Approvals:", fontSize = 12.sp, color = textMain)
                val pendP = providers.filter { !it.is_approved }.size
                Text(text = "$pendP طلب معلق للمصادقة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            }
        }
    }
}
