package com.yemenservices.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.yemenservices.app.data.AppConfig
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.ServiceProvider

enum class AppScreen {
    Home,
    CategoryDetail,
    ProviderDetail,
    RegisterProvider,
    AdminDashboard
}

@Composable
fun MainAppContent(viewModel: AppViewModel) {
    val appConfig by viewModel.appConfig.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    
    // Parse Colors Dynamic Theme
    val primaryColor = remember(appConfig.primary_color_hex) {
        try {
            Color(android.graphics.Color.parseColor(appConfig.primary_color_hex))
        } catch (e: Exception) {
            Color(0xFF2E7D32) // Fallback green
        }
    }
    val secondaryColor = remember(appConfig.secondary_color_hex) {
        try {
            Color(android.graphics.Color.parseColor(appConfig.secondary_color_hex))
        } catch (e: Exception) {
            Color(0xFF81C784)
        }
    }

    val backgroundColor = if (isSystemDark) Color(0xFF121212) else Color(0xFFF7F9FA)
    val cardBgColor = if (isSystemDark) Color(0xFF1E1E1E) else Color.White
    val textMainColor = if (isSystemDark) Color(0xFFEEEEEE) else Color(0xFF2C3E50)
    val textSecColor = if (isSystemDark) Color(0xFFB0B0B0) else Color(0xFF7F8C8D)

    // RTL and locale setup
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Navigation and stack state
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var selectedProviderForDetail by remember { mutableStateOf<ServiceProvider?>(null) }
    val categoryBackStack = remember { mutableStateListOf<Category>() }
    val selectedCategoryForDetail = categoryBackStack.lastOrNull()

    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor
        ) {
            Scaffold(
                topBar = {
                    HeaderBar(
                        isArabic = isArabic,
                        isLoggedIn = viewModel.isOwnerLoggedIn.collectAsState().value,
                        onLangToggle = { viewModel.toggleLanguage() },
                        onAdminClick = {
                            if (viewModel.isOwnerLoggedIn.value) {
                                currentScreen = AppScreen.AdminDashboard
                            } else {
                                showAdminLoginDialog = true
                            }
                        },
                        onHomeClick = {
                            categoryBackStack.clear()
                            currentScreen = AppScreen.Home
                        },
                        appTitle = appConfig.app_name,
                        primaryColor = primaryColor
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentScreen) {
                        AppScreen.Home -> {
                            HomeScreen(
                                viewModel = viewModel,
                                appConfig = appConfig,
                                isArabic = isArabic,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                backgroundColor = backgroundColor,
                                cardBgColor = cardBgColor,
                                textMainColor = textMainColor,
                                textSecColor = textSecColor,
                                onCategoryClick = { category ->
                                    categoryBackStack.clear()
                                    categoryBackStack.add(category)
                                    currentScreen = AppScreen.CategoryDetail
                                },
                                onRegisterClick = { currentScreen = AppScreen.RegisterProvider },
                                onAboutClick = { showAboutDialog = true }
                            )
                        }
                        AppScreen.CategoryDetail -> {
                            if (selectedCategoryForDetail != null) {
                                CategoryDetailScreen(
                                    category = selectedCategoryForDetail,
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
                            } else {
                                currentScreen = AppScreen.Home
                            }
                        }
                        AppScreen.ProviderDetail -> {
                            if (selectedProviderForDetail != null) {
                                ProviderDetailScreen(
                                    provider = selectedProviderForDetail!!,
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
                            } else {
                                currentScreen = AppScreen.Home
                            }
                        }
                        AppScreen.RegisterProvider -> {
                            RegisterProviderScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = AppScreen.Home },
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                cardBgColor = cardBgColor,
                                textMainColor = textMainColor,
                                textSecColor = textSecColor
                            )
                        }
                        AppScreen.AdminDashboard -> {
                            AdminDashboardScreen(
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

                    // Global Back System Handler
                    BackHandler(enabled = currentScreen != AppScreen.Home) {
                        if (currentScreen == AppScreen.CategoryDetail) {
                            if (categoryBackStack.size > 1) {
                                categoryBackStack.removeAt(categoryBackStack.size - 1)
                            } else {
                                categoryBackStack.clear()
                                currentScreen = AppScreen.Home
                            }
                        } else if (currentScreen == AppScreen.ProviderDetail) {
                            currentScreen = AppScreen.CategoryDetail
                        } else if (currentScreen == AppScreen.RegisterProvider || currentScreen == AppScreen.AdminDashboard) {
                            currentScreen = AppScreen.Home
                        }
                    }
                }
            }

            // About Dialog
            if (showAboutDialog) {
                AboutSupportDialog(
                    isArabic = isArabic,
                    appConfig = appConfig,
                    primaryColor = primaryColor,
                    onDismiss = { showAboutDialog = false }
                )
            }

            // Admin Login Dialog
            if (showAdminLoginDialog) {
                AdminLoginDialog(
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    onDismiss = { showAdminLoginDialog = false },
                    onLoginSubmit = { pin ->
                        val success = viewModel.loginAdmin(pin)
                        if (success) {
                            showAdminLoginDialog = false
                            currentScreen = AppScreen.AdminDashboard
                            true
                        } else {
                            false
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    isArabic: Boolean,
    isLoggedIn: Boolean,
    onLangToggle: () -> Unit,
    onAdminClick: () -> Unit,
    onHomeClick: () -> Unit,
    appTitle: String,
    primaryColor: Color
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onHomeClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = appTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            // Language Switcher
            OutlinedButton(
                onClick = onLangToggle,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = if (isArabic) "English EN" else "العربية AR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            // Administrator Dashboard button
            IconButton(onClick = onAdminClick) {
                Icon(
                    imageVector = if (isLoggedIn) Icons.Default.Settings else Icons.Default.Lock,
                    contentDescription = "Admin Area",
                    tint = primaryColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.testTag("app_header_bar")
    )
}

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    appConfig: AppConfig,
    isArabic: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color,
    onCategoryClick: (Category) -> Unit,
    onRegisterClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val allProviders by viewModel.rawProviders.collectAsState()
    var searchKeyword by remember { mutableStateOf("") }

    val topLevelCategories = remember(categories) {
        categories.filter { it.parent_id.isNullOrBlank() }
    }

    val filteredProviders = remember(allProviders, searchKeyword) {
        if (searchKeyword.isBlank()) {
            emptyList()
        } else {
            allProviders.filter {
                it.name.contains(searchKeyword, ignoreCase = true) ||
                        it.description_ar.contains(searchKeyword, ignoreCase = true) ||
                        it.description_en.contains(searchKeyword, ignoreCase = true) ||
                        it.phone.contains(searchKeyword)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Interactive App Search Bar
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = {
                    Text(
                        text = if (isArabic) "ابحث عن مهني، كهربائي، طبيب..." else "Search pros, mechanics, doctor...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = primaryColor
                    )
                },
                trailingIcon = {
                    if (searchKeyword.isNotEmpty()) {
                        IconButton(onClick = { searchKeyword = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = primaryColor.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_bar")
            )
        }

        // 2. Search results panel
        if (searchKeyword.isNotEmpty()) {
            item {
                Text(
                    text = if (isArabic) "نتائج البحث (${filteredProviders.size}):" else "Search Results (${filteredProviders.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = primaryColor
                )
            }

            if (filteredProviders.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isArabic) "عذراً، لم يتم العثور على نتائج تطابق بحثك." else "No approved providers match your query.",
                            fontSize = 12.sp,
                            color = textSecColor,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredProviders) { provider ->
                    ServiceProviderCard(
                        provider = provider,
                        isArabic = isArabic,
                        primaryColor = primaryColor,
                        cardBgColor = cardBgColor,
                        textMainColor = textMainColor,
                        textSecColor = textSecColor,
                        onClick = {
                            // Find parent category to trigger detail stacking
                            val parentCat = categories.firstOrNull { it.id == provider.category_id }
                            if (parentCat != null) {
                                onCategoryClick(parentCat)
                            } else {
                                onCategoryClick(Category(provider.category_id, "دليل", "Directory", "tools"))
                            }
                        }
                    )
                }
            }
            item {
                Divider(color = primaryColor.copy(alpha = 0.15f), thickness = 1.dp)
            }
        }

        // 3. Welcome Banner
        item {
            val fontSizeSp = appConfig.welcomeTextSize.sp
            val type = appConfig.welcomeType
            val bannerImageUrl = if (!appConfig.welcomeImageUrl.isNullOrBlank()) {
                appConfig.welcomeImageUrl
            } else {
                "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80&w=600"
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_banner_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (type == "image" || type == "both") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            AsyncImage(
                                model = bannerImageUrl,
                                contentDescription = "Directory Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                        )
                                    )
                            )
                        }
                    }

                    if (type == "text" || type == "both") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (isArabic) "دليل المهن والخدمات في اليمن" else "Yemen Professional Directory",
                                fontSize = (fontSizeSp.value + 4).sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = appConfig.welcomeMessage,
                                fontSize = fontSizeSp,
                                lineHeight = (fontSizeSp.value + 6).sp,
                                color = textMainColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = onAboutClick,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) "المزيد عن التطبيق والدعم" else "More about App & Support",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                        }
                    } else {
                        // Image only support links
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onAboutClick) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "دعم التطبيق" else "Support Info", fontSize = 11.sp, color = primaryColor)
                            }
                        }
                    }
                }
            }
        }

        // 4. Register as professional Banner
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "هل أنت صاحب مهنة؟" else "Are you a professional?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = if (isArabic) "سجل اسمك ترويجاً لخدماتك مجاناً ليصل إليك العملاء فوراً!" else "Submit your directory listing now with your personal photo!",
                            fontSize = 11.sp,
                            color = textSecColor
                        )
                    }
                    Button(
                        onClick = onRegisterClick,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "سجل الآن" else "Register now",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 5. Main Categories Grid Title
        item {
            Text(
                text = if (isArabic) "تصفح الأقسام الرئيسية:" else "Browse Top Categories:",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Categories list layout
        if (topLevelCategories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            }
        } else {
            val chunks = topLevelCategories.chunked(3)
            items(chunks) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (index in 0 until 3) {
                        if (index < rowItems.size) {
                            val cat = rowItems[index]
                            // Count of approved providers in this category or subcategory
                            val count = allProviders.count { it.category_id == cat.id && it.is_approved }
                            CategoryCard(
                                category = cat,
                                isArabic = isArabic,
                                providerCount = count,
                                primaryColor = primaryColor,
                                cardBgColor = cardBgColor,
                                textMainColor = textMainColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCategoryClick(cat) }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    isArabic: Boolean,
    providerCount: Int,
    primaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.08f)),
        modifier = modifier
            .aspectRatio(1f)
            .testTag("category_card_${category.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val vectorIcon = remember(category.icon) {
                    when (category.icon) {
                        "electrician" -> Icons.Default.FlashOn
                        "plumber" -> Icons.Default.WaterDrop
                        "car_mechanic" -> Icons.Default.Build
                        "doctor" -> Icons.Default.LocalHospital
                        "ac_technician" -> Icons.Default.AcUnit
                        "teacher" -> Icons.Default.School
                        "programmer" -> Icons.Default.Terminal
                        "cleaning" -> Icons.Default.CleaningServices
                        "carpenter" -> Icons.Default.Handyman
                        else -> Icons.Default.GridView
                    }
                }
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isArabic) category.name_ar else category.name_en,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textMainColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isArabic) "$providerCount مهني" else "$providerCount pros",
                fontSize = 9.sp,
                color = primaryColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ServiceProviderCard(
    provider: ServiceProvider,
    isArabic: Boolean,
    primaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("provider_card_${provider.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image view support
            if (!provider.profileImage.isNullOrBlank()) {
                AsyncImage(
                    model = provider.profileImage,
                    contentDescription = "Personal Photo",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.08f)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    val fallbackLetter = provider.name.firstOrNull()?.toString() ?: "Y"
                    Text(
                        text = fallbackLetter,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = provider.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMainColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", provider.rating),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textMainColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isArabic) provider.description_ar else provider.description_en,
                    fontSize = 11.sp,
                    color = textSecColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Address",
                            tint = textSecColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isArabic) provider.address_ar else provider.address_en,
                            fontSize = 10.sp,
                            color = textSecColor
                        )
                    }
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = textSecColor
                    )
                    Text(
                        text = if (isArabic) "سعر: ${provider.price_level}" else "Price: ${provider.price_level}",
                        fontSize = 10.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

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
    val allCategories by viewModel.categories.collectAsState()
    val isOwner by viewModel.isOwnerLoggedIn.collectAsState()

    var showAddSubcategoryDialog by remember { mutableStateOf(false) }

    val subcategories = remember(allCategories, category.id) {
        allCategories.filter { it.parent_id == category.id }
    }

    val context = LocalContext.current

    // Filters state
    val ratingFilterList = listOf("All", "4.5+", "4.0+", "3.5+")
    val priceFilterList = listOf("All", "Low", "Average", "High")
    
    var selectedRatingFilter by remember { mutableStateOf("All") }
    var selectedPriceFilter by remember { mutableStateOf("All") }

    val categoryProviders = remember(allProviders, category.id) {
        allProviders.filter { it.category_id == category.id && it.is_approved }
    }

    val filteredProviders = remember(categoryProviders, selectedRatingFilter, selectedPriceFilter) {
        categoryProviders.filter { prov ->
            val matchRating = when (selectedRatingFilter) {
                "4.5+" -> prov.rating >= 4.5f
                "4.0+" -> prov.rating >= 4.0f
                "3.5+" -> prov.rating >= 3.5f
                else -> true
            }
            val matchPrice = when (selectedPriceFilter) {
                "Low" -> prov.price_level.equals("Low", ignoreCase = true)
                "Average" -> prov.price_level.equals("Average", ignoreCase = true)
                "High" -> prov.price_level.equals("High", ignoreCase = true)
                else -> true
            }
            matchRating && matchPrice
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
                        icon = "tools",
                        image_url = if (imgUrl.isNotBlank()) imgUrl else null,
                        parent_id = category.id
                    )
                )
                showAddSubcategoryDialog = false
                Toast.makeText(
                    context,
                    if (isArabic) "تمت إضافة القسم الفرعي بنجاح!" else "Subcategory added successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Category Header with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = primaryColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isArabic) category.name_ar else category.name_en,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text(
                    text = if (isArabic) "مقدمو الخدمات المؤهلون في هذا المجال" else "Verified professionals under this service.",
                    fontSize = 11.sp,
                    color = textSecColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subcategories Row (Real-time update)
        if (subcategories.isNotEmpty() || isOwner) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isArabic) "الأقسام الفرعية التابعة:" else "Nested Subcategories:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                if (isOwner) {
                    IconButton(
                        onClick = { showAddSubcategoryDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add sub",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (subcategories.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    items(subcategories) { subcat ->
                        val count = allProviders.count { it.category_id == subcat.id && it.is_approved }
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.15f)),
                            modifier = Modifier.clickable { onSubcategoryClick(subcat) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) subcat.name_ar else subcat.name_en,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textMainColor
                                    )
                                    Text(
                                        text = if (isArabic) "$count مهني" else "$count pros",
                                        fontSize = 8.sp,
                                        color = textSecColor
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = if (isArabic) "لا توجد أقسام فرعية حالياً." else "No subcategories loaded under this field.",
                    fontSize = 10.sp,
                    color = textSecColor,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }

        // Filters Panel
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Ratings filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "التقييم:" else "Rating:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMainColor,
                        modifier = Modifier.width(55.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(ratingFilterList) { rate ->
                            val isSelected = selectedRatingFilter == rate
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor else primaryColor.copy(alpha = 0.08f))
                                    .clickable { selectedRatingFilter = rate }
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isArabic && rate == "All") "الكل" else rate,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else textMainColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Price levels filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "مستوى السعر:" else "Price:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMainColor,
                        modifier = Modifier.width(55.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(priceFilterList) { price ->
                            val isSelected = selectedPriceFilter == price
                            val priceLabel = when (price) {
                                "All" -> if (isArabic) "الكل" else "All"
                                "Low" -> if (isArabic) "اقتصادي" else "Low"
                                "Average" -> if (isArabic) "متوسط" else "Average"
                                "High" -> if (isArabic) "مرتفع" else "High"
                                else -> price
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor else primaryColor.copy(alpha = 0.08f))
                                    .clickable { selectedPriceFilter = price }
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = priceLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else textMainColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Service Providers list of this Category
        if (filteredProviders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WorkOutline,
                        contentDescription = "Empty",
                        tint = textSecColor,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic) "عذراً، لا يوجد مقدمو خدمة متاحون حالياً وفقاً للفلاتر المحددة." else "No service providers registered under these filters.",
                        fontSize = 12.sp,
                        color = textSecColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredProviders) { provider ->
                    ServiceProviderCard(
                        provider = provider,
                        isArabic = isArabic,
                        primaryColor = primaryColor,
                        cardBgColor = cardBgColor,
                        textMainColor = textMainColor,
                        textSecColor = textSecColor,
                        onClick = { onProviderClick(provider) }
                    )
                }
            }
        }
    }
}

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
    val allReviews by viewModel.reviews.collectAsState()
    val context = LocalContext.current

    val providerReviews = remember(allReviews, provider.id) {
        allReviews.filter { it.provider_id == provider.id }
    }

    var showReviewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Detail Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = primaryColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "تفاصيل المهني" else "Provider Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }

        // Profile Card with Photo or custom avatar placeholder
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.padding(top = 4.dp)) {
                        if (!provider.profileImage.isNullOrBlank()) {
                            AsyncImage(
                                model = provider.profileImage,
                                contentDescription = "Personal Image",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, primaryColor, CircleShape)
                                    .background(primaryColor.copy(alpha = 0.08f)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.08f))
                                    .border(2.dp, primaryColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val letter = provider.name.firstOrNull()?.toString() ?: "Y"
                                Text(
                                    text = letter,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = provider.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMainColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rating stars
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(5) { rateIndex ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (rateIndex < provider.rating.toInt()) Color(0xFFFFB300) else textSecColor.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${String.format("%.1f", provider.rating)} (${providerReviews.size} ${if (isArabic) "تقييم" else "reviews"})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMainColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Addresses and specs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isArabic) "العنوان" else "Address",
                                fontSize = 10.sp,
                                color = textSecColor
                            )
                            Text(
                                text = if (isArabic) provider.address_ar else provider.address_en,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMainColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isArabic) "مستوى السعر" else "Price Tier",
                                fontSize = 10.sp,
                                color = textSecColor
                            )
                            Text(
                                text = if (provider.price_level.equals("Low", ignoreCase = true)) {
                                    if (isArabic) "اقتصادي" else "Low"
                                } else if (provider.price_level.equals("Average", ignoreCase = true)) {
                                    if (isArabic) "متوسط" else "Average"
                                } else {
                                    if (isArabic) "مرتفع" else "High"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                }
            }
        }

        // Bio section
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isArabic) "عن المهني والخدمة:" else "Service Description:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) provider.description_ar else provider.description_en,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = textMainColor
                    )
                }
            }
        }

        // Contact buttons (Direct communication)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isArabic) "قنوات التواصل المباشر:" else "Direct Contacts:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Phone Call
                        Button(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot make call: ${provider.phone}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "اتصال" else "Call", fontSize = 11.sp, color = Color.White)
                        }

                        // WhatsApp Message
                        Button(
                            onClick = {
                                try {
                                    // Remove special symbols to construct clean link
                                    val cleanNum = provider.whatsapp.replace("+", "").replace(" ", "")
                                    val url = "https://api.whatsapp.com/send?phone=$cleanNum"
                                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open Whatsapp", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("واتساب", fontSize = 11.sp, color = Color.White)
                        }

                        // SMS
                        Button(
                            onClick = {
                                try {
                                    val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${provider.phone}"))
                                    smsIntent.putExtra("sms_body", if (isArabic) "مرحباً، أود الاستفسار عن خدماتك المعروضة في تطبيق دليل اليمن لخدمات." else "Hello, I read your listing on Yemen Services directory.")
                                    context.startActivity(smsIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot send SMS", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isArabic) "رسالة" else "SMS", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Reviews header and button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "تقييمات وآراء العملاء:" else "Customer Reviews:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                OutlinedButton(
                    onClick = { showReviewDialog = true },
                    border = BorderStroke(1.dp, primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "أضف تقييمك" else "Submit Review", fontSize = 11.sp, color = primaryColor)
                }
            }
        }

        // Customer Reviews Display
        if (providerReviews.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor)
                ) {
                    Text(
                        text = if (isArabic) "لا توجد تقييمات لهذا المهني بعد. كن أول من يكتب تقييماً!" else "No customer reviews yet. Share your experience!",
                        fontSize = 11.sp,
                        color = textSecColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(providerReviews) { review ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = review.reviewer_name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMainColor
                            )
                            Row {
                                repeat(5) { star ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (star < review.rating.toInt()) Color(0xFFFFB300) else textSecColor.copy(alpha = 0.2f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        if (review.comment.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = review.comment,
                                fontSize = 11.sp,
                                color = textMainColor
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(15.dp))
        }
    }

    // Submit Review Dialog
    if (showReviewDialog) {
        WriteReviewDialog(
            isArabic = isArabic,
            primaryColor = primaryColor,
            onDismiss = { showReviewDialog = false },
            onSubmit = { name, rate, comment ->
                viewModel.submitReview(provider.id, name, rate, comment)
                showReviewDialog = false
                Toast.makeText(context, if (isArabic) "تم إرسال تقييمك بنجاح!" else "Review submitted successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun RegisterProviderScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current

    // Fields states
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var descAr by remember { mutableStateOf("") }
    var descEn by remember { mutableStateOf("") }
    var selectCategoryId by remember { mutableStateOf("") }
    var selectPriceLevel by remember { mutableStateOf("Average") }
    var addressAr by remember { mutableStateOf("") }
    var addressEn by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") } // Personal photo custom link URL!

    var showDropdownMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Navigation header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = primaryColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isArabic) "تسجيل مهني جديد" else "Register Professional Listing",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = if (isArabic) "أدخل بياناتك وسيتم عرضها فور اعتمادها والتحقق منها." else "Fill your services info and they will be verified and approved instantly.",
                        fontSize = 11.sp,
                        color = textSecColor
                    )
                }
            }
        }

        // Form Fields
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Full name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 2. Specialty Category Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isArabic) "القسم المهني الرئيسي:" else "Select Professional Field:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedCat = categories.firstOrNull { it.id == selectCategoryId }
                            val fieldText = if (selectedCat != null) {
                                if (isArabic) selectedCat.name_ar else selectedCat.name_en
                            } else {
                                if (isArabic) "اختر القسم المهني..." else "Choose service type..."
                            }
                            Button(
                                onClick = { showDropdownMenu = true },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, primaryColor),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(fieldText, color = textMainColor, fontSize = 12.sp)
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

                    // 3. Phone and Contact
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isArabic) "رقم الهاتف" else "Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text(if (isArabic) "رقم واتساب (بما في ذلك رمز خط البلد، مثال: 967770000000)" else "WhatsApp Phone (with prefix, e.g., 967770000000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(if (isArabic) "رقم واتساب الثانوي أو البريد الإلكتروني (اختياري)" else "Alternative contacts / Email (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Profile Personal Photo Custom URL Link! ("كذالك انشاء خانه عند تسجيل اصحاب المهن بوضع صوره شخصيه")
                    OutlinedTextField(
                        value = profileImageUrl,
                        onValueChange = { profileImageUrl = it },
                        label = { Text(if (isArabic) "رابط الصورة الشخصية (صورة شخصية للتطبيق - اختياري)" else "Personal Profile Image Link (Photo URL - optional)") },
                        placeholder = { Text("https://example.com/my-photo.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Address Fields
                    OutlinedTextField(
                        value = addressAr,
                        onValueChange = { addressAr = it },
                        label = { Text(if (isArabic) "العنوان بالتفصيل (مثل: صنعاء، شارع الستين)" else "Address detailed (Arabic)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = addressEn,
                        onValueChange = { addressEn = it },
                        label = { Text(if (isArabic) "العنوان بالإنجليزية (مثل: Sanaa, 60m St)" else "Address detailed (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Price Level tier
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isArabic) "مستوى أسعار خدماتك المتوقع:" else "Estimated price levels:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            listOf("Low", "Average", "High").forEach { level ->
                                val isSelected = selectPriceLevel == level
                                val label = when (level) {
                                    "Low" -> if (isArabic) "اقتصادي ($)" else "Low"
                                    "Average" -> if (isArabic) "متوسط ($$)" else "Average"
                                    else -> if (isArabic) "مرتفع ($$$)" else "High"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) primaryColor else primaryColor.copy(alpha = 0.08f))
                                        .clickable { selectPriceLevel = level }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                   ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else textMainColor
                                    )
                                }
                            }
                        }
                    }

                    // Description in Arabic and English
                    OutlinedTextField(
                        value = descAr,
                        onValueChange = { descAr = it },
                        label = { Text(if (isArabic) "نبذة عن عملك/خبرتك بالكامل (العربية)" else "Describe your service (Arabic)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = descEn,
                        onValueChange = { descEn = it },
                        label = { Text(if (isArabic) "نبذة عن عملك/خبرتك بالكامل (إنجليزية)" else "Describe your service (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            }
        }

        // Submit action button
        item {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || selectCategoryId.isBlank() || descAr.isBlank() || addressAr.isBlank()) {
                        Toast.makeText(context, if (isArabic) "الرجاء تعبئة الحقول الأساسية (الاسم، الهاتف، القسم، العنوان، النبذة)" else "Please fill name, phone, chosen field, address, and bio.", Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.registerProfessional(
                            name = name,
                            phone = phone,
                            whatsapp = whatsapp,
                            email = email,
                            descAr = descAr,
                            descEn = descEn,
                            categoryId = selectCategoryId,
                            priceLevel = selectPriceLevel,
                            addressAr = addressAr,
                            addressEn = addressEn,
                            profileImage = profileImageUrl
                        )
                        Toast.makeText(
                            context,
                            if (isArabic) "تم إرسال طلبك بنجاح وسيكون متاحاً فور المراجعة والاعتماد الفوري!" else "Application sent successfully! Admin will review details and approve soon.",
                            Toast.LENGTH_LONG
                        ).show()
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_registration_button"),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Submit")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "تقديم طلب التسجيل مجاناً" else "Submit Registration",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color,
    cardBgColor: Color,
    textMainColor: Color,
    textSecColor: Color
) {
    val isArabic by viewModel.isArabic.collectAsState()
    val rawPending by viewModel.pendingProviders.collectAsState()
    val rawApproved by viewModel.rawProviders.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val sysConfig by viewModel.appConfig.collectAsState()

    val context = LocalContext.current

    // Admin layout controls tabs
    var currentTab by remember { mutableStateOf(0) } // 0 = Pending registrations, 1 = Approved, 2 = App customization config

    // Metadata controls state holding variables
    var customAppName by remember { mutableStateOf(sysConfig.app_name) }
    var welcomeString by remember { mutableStateOf(sysConfig.welcomeMessage) }
    var welcomeTextSize by remember { mutableStateOf(sysConfig.welcomeTextSize) }
    var welcomeImageUrl by remember { mutableStateOf(sysConfig.welcomeImageUrl ?: "") }
    var welcomeType by remember { mutableStateOf(sysConfig.welcomeType) }
    var customPrimaryColor by remember { mutableStateOf(sysConfig.primary_color_hex) }
    var customSecondaryColor by remember { mutableStateOf(sysConfig.secondary_color_hex) }
    var customSupportEmail by remember { mutableStateOf(sysConfig.support_email) }
    var customSupportWhatsapp by remember { mutableStateOf(sysConfig.support_whatsapp) }
    var customFooterPhone by remember { mutableStateOf(sysConfig.footer_phone) }
    var customIconType by remember { mutableStateOf(sysConfig.selected_icon_type) }

    var showAddCategoryMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Navigation bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primaryColor)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "لوحة إدارة النظام" else "Admin Management Console",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
                Button(
                    onClick = {
                        viewModel.logoutAdmin()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.82f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "خروج" else "Logout", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Sub-Tabs Header selection Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabLabels = listOf(
                    if (isArabic) "طلبات معلقة (${rawPending.size})" else "Pending (${rawPending.size})",
                    if (isArabic) "معتمدون (${rawApproved.size})" else "Approved (${rawApproved.size})",
                    if (isArabic) "تخصيص وبانر" else "Banner & Theme"
                )
                tabLabels.forEachIndexed { idx, label ->
                    val isSel = currentTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) primaryColor else primaryColor.copy(alpha = 0.08f))
                            .clickable { currentTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else textMainColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Tab content conditions
        when (currentTab) {
            0 -> { // Pending listings
                if (rawPending.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isArabic) "ممتاز! لا توجد طلبات تسجيل معلقة حالياً." else "Excellent! No pending registrations await review.",
                                    fontSize = 12.sp,
                                    color = textSecColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(rawPending) { pend ->
                        val matchingCategory = categories.firstOrNull { it.id == pend.category_id }
                        val fieldName = if (matchingCategory != null) {
                            if (isArabic) matchingCategory.name_ar else matchingCategory.name_en
                        } else {
                            "Unknown"
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pend.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMainColor)
                                        Text(
                                            text = "${if (isArabic) "القسم:" else "Field:"} $fieldName | ${if (isArabic) "العنوان:" else "Address:"} ${if (isArabic) pend.address_ar else pend.address_en}",
                                            fontSize = 11.sp,
                                            color = textSecColor
                                        )
                                    }
                                    if (!pend.profileImage.isNullOrBlank()) {
                                        AsyncImage(
                                            model = pend.profileImage,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isArabic) pend.description_ar else pend.description_en,
                                    fontSize = 11.sp,
                                    color = textMainColor,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.approveProvider(pend) },
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isArabic) "اعتماد ونشر" else "Approve & Publish", fontSize = 11.sp, color = Color.White)
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.deleteProvider(pend.id, isApproved = false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isArabic) "رفض وحذف" else "Reject & Delete", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> { // Manage Approved Listings
                item {
                    // Quick add static categories button
                    Button(
                        onClick = { showAddCategoryMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Category, contentDescription = "Category")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "إضافة وتخصيص الأقسام العامة" else "Add or Modify Categories", fontSize = 12.sp, color = Color.White)
                    }
                }

                if (rawApproved.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor)
                        ) {
                            Text(
                                text = if (isArabic) "لا يوجد مهنيون معتمدون حالياً." else "No approved directory listings yet.",
                                fontSize = 12.sp,
                                color = textSecColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(rawApproved) { approved ->
                        val matchedCategory = categories.firstOrNull { it.id == approved.category_id }
                        val specName = if (matchedCategory != null) {
                            if (isArabic) matchedCategory.name_ar else matchedCategory.name_en
                        } else {
                            "Unknown"
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBgColor),
                            border = BorderStroke(1.dp, textSecColor.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!approved.profileImage.isNullOrBlank()) {
                                    AsyncImage(
                                        model = approved.profileImage,
                                        contentDescription = "Personal Image",
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(approved.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textMainColor)
                                    Text("$specName | ${approved.phone}", fontSize = 11.sp, color = textSecColor)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteProvider(approved.id, isApproved = true) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> { // Real-time branding and welcome configurations panel
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isArabic) "إعدادات بنر الترحيب للمستخدم:" else "Welcome Banner Configuration:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            
                            // Welcome Type selection tabs (text, image, both)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val welcomeTypes = listOf(
                                    Triple("text", if (isArabic) "نص فقط" else "Text only", Icons.Filled.Notes),
                                    Triple("image", if (isArabic) "صورة فقط" else "Image only", Icons.Filled.Image),
                                    Triple("both", if (isArabic) "نص وصورة" else "Text & Image", Icons.Filled.Wallpaper)
                                )
                                welcomeTypes.forEach { (mode, label, icon) ->
                                    val isSelected = welcomeType == mode
                                    OutlinedButton(
                                        onClick = { welcomeType = mode },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent
                                        ),
                                        border = BorderStroke(1.5.dp, if (isSelected) primaryColor else textSecColor.copy(alpha = 0.4f)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                            Icon(icon, contentDescription = null, tint = if (isSelected) primaryColor else textSecColor, modifier = Modifier.size(16.dp))
                                            Text(label, fontSize = 9.sp, color = textMainColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }

                            // Welcome message customizable text field (if type is text or both)
                            if (welcomeType == "text" || welcomeType == "both") {
                                OutlinedTextField(
                                    value = welcomeString,
                                    onValueChange = { welcomeString = it },
                                    label = { Text(if (isArabic) "نص رسالة الترحيب" else "Welcome messaging string") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4
                                )
                                
                                // Text Size controller presets & slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isArabic) "حجم خط رسالة الترحيب: $welcomeTextSize sp" else "Welcome font size: $welcomeTextSize sp",
                                            fontSize = 11.sp,
                                            color = textMainColor
                                        )
                                        // Quick select chips
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(12, 14, 16, 18, 20).forEach { size ->
                                                val isCurr = welcomeTextSize == size
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isCurr) primaryColor else textSecColor.copy(alpha = 0.1f))
                                                        .clickable { welcomeTextSize = size }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(size.toString(), fontSize = 9.sp, color = if (isCurr) Color.White else textMainColor)
                                                }
                                            }
                                        }
                                    }
                                    Slider(
                                        value = welcomeTextSize.toFloat(),
                                        onValueChange = { welcomeTextSize = it.toInt() },
                                        valueRange = 10f..28f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = primaryColor,
                                            activeTrackColor = primaryColor
                                        )
                                    )
                                }
                            }

                            // Welcome image URL (if type is image or both)
                            if (welcomeType == "image" || welcomeType == "both") {
                                OutlinedTextField(
                                    value = welcomeImageUrl,
                                    onValueChange = { welcomeImageUrl = it },
                                    label = { Text(if (isArabic) "رابط الصورة الترحيبية (أو فارغ للافتراضية)" else "Welcome Image URL (Blank for default)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("https://example.com/banner.png") }
                                )
                            }

                            Divider(color = primaryColor.copy(alpha = 0.15f))

                            // Branding metadata configuration
                            Text(
                                text = if (isArabic) "بيانات ومعلومات الهوية والدعم للتطبيق:" else "Theme, brand & support links details:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )

                            OutlinedTextField(
                                value = customAppName,
                                onValueChange = { customAppName = it },
                                label = { Text(if (isArabic) "اسم التطبيق" else "Product App Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customPrimaryColor,
                                onValueChange = { customPrimaryColor = it },
                                label = { Text(if (isArabic) "كود اللون الرئيسي (Hex)" else "Primary Hex Color") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customSecondaryColor,
                                onValueChange = { customSecondaryColor = it },
                                label = { Text(if (isArabic) "كود اللون الفرعي (Hex)" else "Secondary Hex Color") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customSupportEmail,
                                onValueChange = { customSupportEmail = it },
                                label = { Text(if (isArabic) "بريد الدعم والمقترحات" else "Support Mail") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customSupportWhatsapp,
                                onValueChange = { customSupportWhatsapp = it },
                                label = { Text(if (isArabic) "رقم واتساب المشرف" else "Whatsapp Support Line") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customFooterPhone,
                                onValueChange = { customFooterPhone = it },
                                label = { Text(if (isArabic) "هاتف الإتصال المباشر للمكتب" else "Direct Office call line") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Submit save configuration
                            Button(
                                onClick = {
                                    val updatedConfig = sysConfig.copy(
                                        app_name = customAppName,
                                        welcomeMessage = welcomeString,
                                        primary_color_hex = customPrimaryColor,
                                        secondary_color_hex = customSecondaryColor,
                                        footer_phone = customFooterPhone,
                                        support_email = customSupportEmail,
                                        support_whatsapp = customSupportWhatsapp,
                                        selected_icon_type = customIconType,
                                        welcomeTextSize = welcomeTextSize,
                                        welcomeImageUrl = if (welcomeImageUrl.isNotBlank()) welcomeImageUrl else null,
                                        welcomeType = welcomeType
                                    )
                                    viewModel.updateSystemConfig(updatedConfig)
                                    Toast.makeText(context, if (isArabic) "تم حفظ خصائص النظام للجميع بنجاح!" else "Metadata saved to Firestore successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "حفظ التغييرات" else "Save customization details", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryMenu) {
        CustomCategoryManagementDialog(
            isArabic = isArabic,
            categories = categories,
            primaryColor = primaryColor,
            onDismiss = { showAddCategoryMenu = false },
            onAddCategory = { ar, en, iconVal, parentId ->
                viewModel.addCategory(Category("", ar, en, iconVal, null, parentId))
                Toast.makeText(context, if (isArabic) "تمت إضافة القسم!" else "Category configured!", Toast.LENGTH_SHORT).show()
            },
            onDeleteCategory = { id ->
                viewModel.deleteCategory(id)
                Toast.makeText(context, if (isArabic) "تم حذف القسم!" else "Category deleted!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Dialogs implementations

@Composable
fun WriteReviewDialog(
    isArabic: Boolean,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSubmit: (String, Float, String) -> Unit
) {
    var reviewerName by remember { mutableStateOf("") }
    var reviewerComment by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(5f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "كتابة تقييم ومراجعة للخدمة" else "Review Professional Service",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                OutlinedTextField(
                    value = reviewerName,
                    onValueChange = { reviewerName = it },
                    label = { Text(if (isArabic) "اسمك الكامل الموقّر" else "Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Stars selection
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "حدد عدد النجوم:" else "Select stars rating:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            val isSel = star <= selectedRating
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSel) Color(0xFFFFB300) else Color.Gray.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { selectedRating = star.toFloat() }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reviewerComment,
                    onValueChange = { reviewerComment = it },
                    label = { Text(if (isArabic) "تعليقك أو تفاصيل تجربتك..." else "Your review/comment...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSubmit(reviewerName, selectedRating, reviewerComment) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إرسال التقييم" else "Submit", fontSize = 12.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إلغاء الأمر" else "Cancel", fontSize = 12.sp, color = primaryColor)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLoginDialog(
    isArabic: Boolean,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onLoginSubmit: (String) -> Boolean
) {
    var pinValue by remember { mutableStateOf("") }
    var showErrorAlert by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "تسجيل الدخول للمشرف" else "Admin Authorization Area",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                Text(
                    text = if (isArabic) "أدخل رمز التحقق (الرمز الافتراضي هو: 1234 أو 2026)" else "Type authorization code (Default code: 1234 or 2026)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = pinValue,
                    onValueChange = {
                        pinValue = it
                        showErrorAlert = false
                    },
                    label = { Text(if (isArabic) "رمز المرور (PIN)" else "Authorization PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                if (showErrorAlert) {
                    Text(
                        text = if (isArabic) "عذراً، الرمز المدخل خاطئ!" else "Invalid authorization PIN code!",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val ok = onLoginSubmit(pinValue)
                            if (!ok) showErrorAlert = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "دخول" else "Login", fontSize = 12.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إلغاء" else "Cancel", fontSize = 12.sp, color = primaryColor)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutSupportDialog(
    isArabic: Boolean,
    appConfig: AppConfig,
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "عن تطبيق دليل اليمن للخدمات" else "About Yemen Services Directory",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                Text(
                    text = if (isArabic) {
                        "دليل خدمات اليمن هو منصة تواصل تفاعلية فورية تربطك مع أصحاب الكفاءات من مقدمي الخدمات و المهن المتنوعة في جميع المحافظات اليمينة مجاناً تماماً."
                    } else {
                        "Yemen Service Directory is a community hub letting customers map, navigate, and call verified local service providers and craftsmen instantly, fully powered by live Google Cloud Firebase."
                    },
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Divider()

                Text(
                    text = if (isArabic) "للدعم والمقترحات والشكاوى:" else "Helpdesk, Support & Operations:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                Text(
                    text = "${if (isArabic) "البريد الإلكتروني للإدارة:" else "Support Mail:"} ${appConfig.support_email}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Help Skype / Support Whatsapp
                    Button(
                        onClick = {
                            try {
                                val cleanNum = appConfig.support_whatsapp.replace("+", "").replace(" ", "")
                                val url = "https://api.whatsapp.com/send?phone=$cleanNum"
                                val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(urlIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open Whatsapp support", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اتصل بواتساب الإدارة", fontSize = 10.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إغلاق" else "Close", fontSize = 11.sp, color = primaryColor)
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
    var imgUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "إضافة قسم فرعي جديد" else "Create Nested Subcategory",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isArabic) "اسم القسم (العربية)" else "Subcategory Title (Arabic)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isArabic) "اسم القسم (بالإنكليزية)" else "Subcategory Title (English)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = imgUrl,
                    onValueChange = { imgUrl = it },
                    label = { Text(if (isArabic) "رابط الصورة التوضيحية للقسم (اختياري)" else "Icon/Photo URL (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (nameAr.isNotBlank() && nameEn.isNotBlank()) {
                                onSubmit(nameAr, nameEn, imgUrl)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إضافة" else "Add", fontSize = 12.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إلغاء" else "Cancel", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomCategoryManagementDialog(
    isArabic: Boolean,
    categories: List<Category>,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onAddCategory: (String, String, String, String?) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var selectedIconType by remember { mutableStateOf("electrician") }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var showParentSelectionDropdown by remember { mutableStateOf(false) }

    val iconTypes = listOf("electrician", "plumber", "car_mechanic", "doctor", "ac_technician", "teacher", "programmer", "cleaning", "carpenter")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "إدارة وتخصيص الأقسام العامة" else "Configure App Categories",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                Divider()

                // List existing categories with delete action
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(primaryColor.copy(alpha = 0.03f))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val rootCategories = categories.filter { it.parent_id.isNullOrBlank() }
                    items(rootCategories) { cat ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, primaryColor.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) cat.name_ar else cat.name_en,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { onDeleteCategory(cat.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Divider()

                // Form to add a new top-level categories
                Text(
                    text = if (isArabic) "إنشاء قسم جديد سريع:" else "Create New Category:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isArabic) "الاسم بالعربية" else "Arabic Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isArabic) "الاسم بالإنجليزية" else "English Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Parent link selection option
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "تبعية القسم (اختياري لربط فروع):" else "Parent Attachment Link (optional):",
                        fontSize = 10.sp,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val parentText = if (selectedParentId != null) {
                            val par = categories.firstOrNull { it.id == selectedParentId }
                            if (par != null) (if (isArabic) par.name_ar else par.name_en) else "None"
                        } else {
                            if (isArabic) "قسم رئيسي (لا توجد تبعية)" else "Top-level Category"
                        }
                        Button(
                            onClick = { showParentSelectionDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(parentText, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                        DropdownMenu(
                            expanded = showParentSelectionDropdown,
                            onDismissRequest = { showParentSelectionDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "بلا تبعية (قسم رئيسي)" else "Top-Level") },
                                onClick = {
                                    selectedParentId = null
                                    showParentSelectionDropdown = false
                                }
                            )
                            val topOnly = categories.filter { it.parent_id.isNullOrBlank() }
                            topOnly.forEach { parentItem ->
                                DropdownMenuItem(
                                    text = { Text(if (isArabic) parentItem.name_ar else parentItem.name_en) },
                                    onClick = {
                                        selectedParentId = parentItem.id
                                        showParentSelectionDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Icon type selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "الرمز:" else "Icon:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.width(42.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(iconTypes) { iconKey ->
                            val isSel = selectedIconType == iconKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) primaryColor else primaryColor.copy(alpha = 0.08f))
                                    .clickable { selectedIconType = iconKey }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(iconKey, fontSize = 9.sp, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (nameAr.isNotBlank() && nameEn.isNotBlank()) {
                                onAddCategory(nameAr, nameEn, selectedIconType, selectedParentId)
                                nameAr = ""
                                nameEn = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إضافة قسم" else "Add category", fontSize = 11.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إغلاق" else "Close", fontSize = 11.sp, color = primaryColor)
                    }
                }
            }
        }
    }
}
