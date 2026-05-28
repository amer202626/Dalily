package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// Helper to Safely Parse Hex Colors
fun parseColor(hex: String, default: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}

// Convert String Icon Name to Material Icon vector
fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "home_repair_service", "home_maintenance" -> Icons.Default.HomeRepairService
        "laptop_mac", "technology", "tech" -> Icons.Default.LaptopMac
        "school", "education" -> Icons.Default.School
        "brush", "beauty" -> Icons.Default.Brush
        "directions_car", "cars" -> Icons.Default.DirectionsCar
        "cleaning_services", "home_services" -> Icons.Default.CleaningServices
        "local_shipping", "shipping", "delivery" -> Icons.Default.LocalShipping
        "work", "professional", "professional_services" -> Icons.Default.Work
        "local_taxi", "taxi" -> Icons.Default.LocalTaxi
        "delivery_dining", "order_delivery" -> Icons.Default.DeliveryDining
        "car_rental" -> Icons.Default.DirectionsCar // Fallback or rental
        "hotel", "apartments" -> Icons.Default.Hotel
        else -> Icons.Default.Category
    }
}

// Splash Screen showing the required custom adaptive round App Icon
@Composable
fun SplashScreen(navController: NavController, viewModel: AppViewModel) {
    val context = LocalContext.current
    val primaryColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
    val secondaryColor = parseColor(viewModel.secondaryColorHex, Color(0xFFFB8C00))
    val isArabic = viewModel.currentLanguage == "ar"

    val welcomeMsg = if (isArabic) viewModel.welcomeMessageAr else viewModel.welcomeMessageEn
    val showWelcomeInstead = viewModel.showWelcomeMessageInsteadOfLogo
    val customLogoUrl = viewModel.customWelcomeLogoUrl

    LaunchedEffect(Unit) {
        delay(2500)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            if (showWelcomeInstead) {
                // --- CUSTOM GREETING MESSAGE LAYOUT ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = welcomeMsg,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp
                        )
                    }
                }
            } else {
                // --- PREMIUM LOGO IMAGE / BLACK BADGE LAYOUT ---
                if (!customLogoUrl.isBlank()) {
                    // Customizable remote image
                    Card(
                        modifier = Modifier
                            .size(160.dp)
                            .testTag("custom_splash_logo_card"),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            model = customLogoUrl,
                            contentDescription = "Custom Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // Modern black circular badge with white text
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111827)) // Rich Black Icon Background
                            .testTag("default_black_logo_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            val logoText = if (isArabic) viewModel.iconLetterAr else viewModel.iconLetterEn
                            // Dynamic font sizing bounds to fit words like "خدمات" beautifully inside the circle
                            val fontSizeVal = if (logoText.length > 2) 28.sp else 72.sp
                            Text(
                                text = logoText,
                                color = Color.White,
                                fontSize = fontSizeVal,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic) "دليلي" else "Dalili",
                                color = secondaryColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main App Header Label
            Text(
                text = if (isArabic) viewModel.appNameAr else viewModel.appNameEn,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Static / Default message
            Text(
                text = if (isArabic) "كل الخدمات في تطبيق واحد" else "All services in one app",
                fontSize = 15.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Parent Wrapper for Centralized Core Footer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: AppViewModel,
    navController: NavController,
    showBackButton: Boolean = false,
    titleContent: @Composable () -> Unit,
    actionsContent: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val primaryColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
    val isArabic = viewModel.currentLanguage == "ar"
    val layoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = titleContent,
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = if (isArabic) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                               )
                            }
                        }
                    },
                    actions = actionsContent,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            floatingActionButton = floatingActionButton,
            bottomBar = {
                // Centered small grey footer text as per requirement
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.footerText,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            },
            content = content
        )
    }
}

// Primary Home Directory Screen
@Composable
fun HomeScreen(navController: NavController, viewModel: AppViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categoriesList.collectAsStateWithLifecycle()
    val providers by viewModel.serviceProvidersList.collectAsStateWithLifecycle()
    
    val primaryColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
    val secondaryColor = parseColor(viewModel.secondaryColorHex, Color(0xFFFB8C00))
    val isArabic = viewModel.currentLanguage == "ar"

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    // Tap counter for secret backdoor portal
    var secretTaps by remember { mutableStateOf(0) }

    // Filter categories based on search input
    val filteredCategories = remember(categories, searchQuery, isArabic) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.filter {
                it.nameAr.contains(searchQuery, ignoreCase = true) ||
                        it.nameEn.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    MainLayout(
        viewModel = viewModel,
        navController = navController,
        showBackButton = false,
        titleContent = {
            // Accessible 5-click header triggers backdoor portal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        secretTaps++
                        if (secretTaps >= 5) {
                            secretTaps = 0
                            showBackdoorDialog = true
                        }
                    }
                    .padding(vertical = 4.dp)
                    .testTag("app_logo_header")
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) viewModel.iconLetterAr else viewModel.iconLetterEn,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) viewModel.appNameAr else viewModel.appNameEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        actionsContent = {
            // Search toggle icon
            IconButton(
                onClick = {
                    isSearchExpanded = !isSearchExpanded
                    if (!isSearchExpanded) searchQuery = ""
                },
                modifier = Modifier.testTag("search_icon")
            ) {
                Icon(
                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = Translator.getString("search", viewModel.currentLanguage)
                )
            }

            // Language Toggle Icon 🌐
            IconButton(
                onClick = { viewModel.toggleLanguage() },
                modifier = Modifier.testTag("lang_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Toggle Language"
                )
            }

            // User authorization status 👤
            IconButton(
                onClick = { showLoginDialog = true },
                modifier = Modifier.testTag("profile_icon")
            ) {
                Icon(
                    imageVector = if (viewModel.loggedInUser != null) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = Translator.getString("my_profile", viewModel.currentLanguage),
                    tint = if (viewModel.loggedInUser != null) secondaryColor else Color.White
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAiDialog = true },
                containerColor = Color(0xFF111827), // Deep black background
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(68.dp)
                    .testTag("ai_floating_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🤖", fontSize = 16.sp)
                    Text(
                        text = if (isArabic) "خدمات" else "Services",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9F9F9))
        ) {
            // Real-time search banner bar
            AnimatedVisibility(visible = isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(Translator.getString("search", viewModel.currentLanguage)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("search_input_field"),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        cursorColor = primaryColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Categories Grid Layout
            if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "لا توجد أقسام متطابقة" else "No matching categories",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCategories) { category ->
                        CategoryCard(
                            category = category,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            language = viewModel.currentLanguage,
                            onClick = {
                                navController.navigate("providers/${category.id}")
                            }
                        )
                    }
                }
            }
        }

        // --- BACKDOOR ACCESS DIALOG ---
        if (showBackdoorDialog) {
            BackdoorDialog(
                viewModel = viewModel,
                onDismiss = { showBackdoorDialog = false },
                onAuthenticated = {
                    showBackdoorDialog = false
                    navController.navigate("owner_dashboard")
                }
            )
        }

        // --- PROFILE CONTROL / LOGIN DIALOG ---
        if (showLoginDialog) {
            ProfileOrLoginDialog(
                viewModel = viewModel,
                navController = navController,
                onDismiss = { showLoginDialog = false }
            )
        }

        // --- DALILI AI CHAT ASSISTANT ---
        if (showAiDialog) {
            DaliliAiAssistantDialog(
                viewModel = viewModel,
                onDismiss = { showAiDialog = false }
            )
        }
    }
}

// Category Grid Card UI Cell
@Composable
fun CategoryCard(
    category: Category,
    primaryColor: Color,
    secondaryColor: Color,
    language: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!category.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = category.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark vertical gradient to ensure high readability of text at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            } else {
                // Colored fallback gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    primaryColor,
                                    primaryColor.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }

            // Category Details Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = if (language == "ar") Alignment.End else Alignment.Start
            ) {
                if (category.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category.icon),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = if (language == "en") category.nameEn else category.nameAr,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    textAlign = if (language == "ar") TextAlign.Right else TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Category Service Providers Listing Directory
@Composable
fun ProvidersScreen(navController: NavController, categoryId: String, viewModel: AppViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categoriesList.collectAsStateWithLifecycle()
    val allProviders by viewModel.serviceProvidersList.collectAsStateWithLifecycle()

    val primaryColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
    val isArabic = viewModel.currentLanguage == "ar"

    val currentCategory = remember(categories, categoryId) {
        categories.find { it.id == categoryId }
    }

    val providersForCategory = remember(allProviders, categoryId) {
        allProviders.filter { it.categoryId == categoryId }
    }

    val title = if (currentCategory != null) {
        if (isArabic) currentCategory.nameAr else currentCategory.nameEn
    } else {
        Translator.getString("service_providers", viewModel.currentLanguage)
    }

    MainLayout(
        viewModel = viewModel,
        navController = navController,
        showBackButton = true,
        titleContent = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        actionsContent = {
            // Language Select
            IconButton(onClick = { viewModel.toggleLanguage() }) {
                Icon(Icons.Default.Language, "Toggle Language")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F6F6))
        ) {
            if (providersForCategory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "لا يوجد مقدمو خدمات في هذا القسم حالياً." else "No service providers found inside this category.",
                            color = Color.Gray,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(providersForCategory) { provider ->
                        ProviderCard(
                            provider = provider,
                            primaryColor = primaryColor,
                            language = viewModel.currentLanguage,
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                context.startActivity(intent)
                            },
                            onWhatsApp = {
                                try {
                                    val url = "https://api.whatsapp.com/send?phone=${provider.phone}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp can't be opened", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShare = {
                                val providerName = if (isArabic) provider.nameAr else provider.nameEn
                                val shareText = if (isArabic) {
                                    "دليلي: مقدم الخدمة ($providerName) - هاتف: ${provider.phone}"
                                } else {
                                    "Dalili App: Provider ($providerName) - Phone: ${provider.phone}"
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Dalili Directory")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            }
                        )
                    }
                }
            }
        }
    }
}

// Single Service Provider Profile Card Cell
@Composable
fun ProviderCard(
    provider: ServiceProvider,
    primaryColor: Color,
    language: String,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onShare: () -> Unit
) {
    val isArabic = language == "ar"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Coil Async Image for service provider avatar
                AsyncImage(
                    model = provider.imageUrl ?: "https://images.unsplash.com/photo-1521791136368-1a4682c20ef5?w=120&auto=format&fit=crop&q=60",
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) provider.nameAr else provider.nameEn,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = provider.phone,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Star Rating View
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = provider.rating.toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Contact Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("call_button_${provider.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translator.getString("call", language), fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // WhatsApp
                OutlinedButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("whatsapp_button_${provider.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Chat, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translator.getString("whatsapp", language), color = Color(0xFF4CAF50), fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Share
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_button_${provider.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translator.getString("share", language), fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

// Access Dialogue for Backdoor Setup
@Composable
fun BackdoorDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val language = viewModel.currentLanguage
    val isArabic = language == "ar"
    val context = LocalContext.current
    var inputPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "بوابة الدخول الخفية للمالك" else "Hidden Owner Backdoor",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = if (isArabic) "الرجاء إدخال كلمة المرور الخلفية للوصول إلى لوحة التحكم:" else "Enter the Backdoor Password to secure owner options:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    placeholder = { Text(Translator.getString("password", language)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("backdoor_pwd_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.verifyBackdoorPassword(inputPassword) { success ->
                        if (success) {
                            Toast.makeText(context, if (isArabic) "تم الدخول بنجاح!" else "Owner authenticated!", Toast.LENGTH_SHORT).show()
                            onAuthenticated()
                        } else {
                            Toast.makeText(context, if (isArabic) "كلمة المرور خاطئة" else "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))),
                modifier = Modifier.testTag("backdoor_enter_btn")
            ) {
                Text(Translator.getString("enter", language))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Translator.getString("cancel", language))
            }
        }
    )
}

// User profiles dialog layout (Login / Register / Profile view)
@Composable
fun ProfileOrLoginDialog(
    viewModel: AppViewModel,
    navController: NavController,
    onDismiss: () -> Unit
) {
    val language = viewModel.currentLanguage
    val isArabic = language == "ar"
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (viewModel.loggedInUser != null) {
                        Translator.getString("my_profile", language)
                    } else {
                        Translator.getString("login", language)
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (viewModel.loggedInUser != null) {
                    // Display Logged In State
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${Translator.getString("username", language)}: ${viewModel.loggedInUser}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) {
                            if (viewModel.loggedInUser == "admin") "العضوية: مدير عام التطبيق" 
                            else if (viewModel.adminsList.contains(viewModel.loggedInUser)) "العضوية: مشرف معتمد"
                            else "العضوية: مستخدم نشط"
                        } else {
                            if (viewModel.loggedInUser == "admin") "Rank: General Director"
                            else if (viewModel.adminsList.contains(viewModel.loggedInUser)) "Rank: Verified Moderator"
                            else "Rank: Active Client"
                        },
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    
                    if (viewModel.loggedInUser == "admin" || viewModel.adminsList.contains(viewModel.loggedInUser)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                navController.navigate("owner_dashboard")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isArabic) "دخول لوحة التحكم" else "Access Dashboard")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.logoutUser()
                            Toast.makeText(context, if (isArabic) "تم تسجيل الخروج" else "Logged out", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Translator.getString("logout", language))
                    }
                } else {
                    // Enter Login inputs
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(Translator.getString("username", language)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("username_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(Translator.getString("password", language)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.loginUser(username, password) { loginError ->
                                if (loginError == null) {
                                    Toast.makeText(context, if (isArabic) "تم تسجيل الدخول بنجاح!" else "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, loginError, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))),
                        modifier = Modifier.fillMaxWidth().testTag("login_submit_btn")
                    ) {
                        Text(Translator.getString("enter", language))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text(Translator.getString("cancel", language), color = Color.Gray)
                }
            }
        }
    }
}

// =========================================================================================
// OWNER BACKDOOR CMS DASHBOARD SCREEN
// =========================================================================================
@Composable
fun OwnerDashboardScreen(navController: NavController, viewModel: AppViewModel) {
    val categories by viewModel.categoriesList.collectAsStateWithLifecycle()
    val providers by viewModel.serviceProvidersList.collectAsStateWithLifecycle()
    val isArabic = viewModel.currentLanguage == "ar"
    val language = viewModel.currentLanguage

    val isSuperAdmin = viewModel.loggedInUser == "admin"
    var activeTab by remember { mutableStateOf(0) } 

    LaunchedEffect(Unit) {
        viewModel.syncAdmins()
    }

    val tabs = if (isSuperAdmin) {
        listOf(
            if (isArabic) "الأقسام" else "Categories",
            if (isArabic) "مقدمو الخدمات" else "Providers",
            if (isArabic) "الهوية" else "Brand",
            if (isArabic) "المشرفون" else "Admins"
        )
    } else {
        listOf(
            if (isArabic) "الأقسام" else "Categories",
            if (isArabic) "مقدمو الخدمات" else "Providers"
        )
    }

    MainLayout(
        viewModel = viewModel,
        navController = navController,
        showBackButton = true,
        titleContent = {
            Text(
                text = if (isArabic) "لوحة التحكم" else "Dashboard",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        actionsContent = {
            // Language Select Inside Dashboard
            IconButton(onClick = { viewModel.toggleLanguage() }) {
                Icon(Icons.Default.Language, "Toggle Language")
            }
            // Logout Admin Dash
            IconButton(onClick = {
                viewModel.exitOwnerMode()
                navController.popBackStack()
            }) {
                Icon(Icons.Default.ExitToApp, "Exit Owner Mode")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // Admin control TabBar scrolling/scrollable
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFFF5F5F5),
                contentColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            // Tab contents rendering
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isSuperAdmin) {
                    when (activeTab) {
                        0 -> DashboardCategoriesTab(categories, viewModel)
                        1 -> DashboardProvidersTab(providers, categories, viewModel)
                        2 -> DashboardBrandingTab(viewModel)
                        3 -> DashboardAdminsTab(viewModel)
                    }
                } else {
                    when (activeTab) {
                        0 -> DashboardCategoriesTab(categories, viewModel)
                        1 -> DashboardProvidersTab(providers, categories, viewModel)
                    }
                }
            }
        }
    }
}

// 1. Dashboard: CATEGORIES ADMIN TAB
@Composable
fun DashboardCategoriesTab(categories: List<Category>, viewModel: AppViewModel) {
    val isArabic = viewModel.currentLanguage == "ar"
    val language = viewModel.currentLanguage
    val context = LocalContext.current

    var showFormDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<Category?>(null) }

    // Form attributes
    var nameArInput by remember { mutableStateOf("") }
    var nameEnInput by remember { mutableStateOf("") }
    var iconInput by remember { mutableStateOf("") }
    var orderIndexInput by remember { mutableStateOf("1") }
    var imageUrlInput by remember { mutableStateOf("") }

    LaunchedEffect(showFormDialog, editCategory) {
        if (showFormDialog) {
            nameArInput = editCategory?.nameAr ?: ""
            nameEnInput = editCategory?.nameEn ?: ""
            iconInput = editCategory?.icon ?: "home_repair_service"
            orderIndexInput = (editCategory?.orderIndex ?: (categories.size + 1)).toString()
            imageUrlInput = editCategory?.imageUrl ?: ""
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editCategory = null
                    showFormDialog = true
                },
                containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Category")
            }
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAFA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!category.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = category.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                getCategoryIcon(category.icon),
                                null,
                                tint = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AR: ${category.nameAr}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("EN: ${category.nameEn}", color = Color.Gray, fontSize = 13.sp)
                            Text("Order index: ${category.orderIndex}", color = Color.Gray, fontSize = 12.sp)
                        }
                        IconButton(onClick = {
                            editCategory = category
                            showFormDialog = true
                        }) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.Blue)
                        }
                        IconButton(onClick = {
                            viewModel.deleteCategory(category.id)
                            Toast.makeText(context, if (isArabic) "تم حذف القسم" else "Category deleted", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        // Add/Edit Dialog modal
        if (showFormDialog) {
            AlertDialog(
                onDismissRequest = { showFormDialog = false },
                title = {
                    Text(
                        if (editCategory == null) {
                            if (isArabic) "إضافة قسم جديد" else "Add New Category"
                        } else {
                            if (isArabic) "تعديل قسم" else "Edit Category"
                        }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = nameArInput,
                            onValueChange = { nameArInput = it },
                            label = { Text("الاسم بالعربية (name_ar)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nameEnInput,
                            onValueChange = { nameEnInput = it },
                            label = { Text("الاسم بالإنجليزية (name_en)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = iconInput,
                            onValueChange = { iconInput = it },
                            label = { Text("اسم الأيقونة (icon)") },
                            placeholder = { Text("e.g. hotel, school, cars") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = orderIndexInput,
                            onValueChange = { orderIndexInput = it },
                            label = { Text("الترتيب الرقمي (order_index)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Custom local image selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = imageUrlInput,
                                onValueChange = { imageUrlInput = it },
                                label = { Text(if (isArabic) "رابط الصورة أو مسارها" else "Category Image URL / Local path") },
                                modifier = Modifier.weight(1f)
                            )
                            val pickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri: android.net.Uri? ->
                                if (uri != null) {
                                    val localPath = saveImageToInternalStorage(context, uri)
                                    if (localPath != null) {
                                        imageUrlInput = localPath
                                        Toast.makeText(context, if (isArabic) "تم اختيار الصورة!" else "Image selected!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            Button(
                                onClick = { pickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Pick Image")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = editCategory?.id ?: UUID.randomUUID().toString()
                            val oIndex = orderIndexInput.toIntOrNull() ?: 1
                            if (editCategory == null) {
                                viewModel.addCategory(id, nameArInput, nameEnInput, iconInput, oIndex, imageUrlInput.ifBlank { null })
                            } else {
                                viewModel.updateCategory(id, nameArInput, nameEnInput, iconInput, oIndex, imageUrlInput.ifBlank { null })
                            }
                            showFormDialog = false
                            Toast.makeText(context, if (isArabic) "تم الحفظ بنجاح!" else "Category saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)))
                    ) {
                        Text(Translator.getString("save", language))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFormDialog = false }) {
                        Text(Translator.getString("cancel", language))
                    }
                }
            )
        }
    }
}

// 2. Dashboard: SERVICE PROVIDERS ADMIN TAB
@Composable
fun DashboardProvidersTab(
    providers: List<ServiceProvider>,
    categories: List<Category>,
    viewModel: AppViewModel
) {
    val isArabic = viewModel.currentLanguage == "ar"
    val language = viewModel.currentLanguage
    val context = LocalContext.current

    var showFormDialog by remember { mutableStateOf(false) }
    var editProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    // Form inputs
    var nameArInput by remember { mutableStateOf("") }
    var nameEnInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var ratingInput by remember { mutableStateOf("5.0") }
    var imageUrlInput by remember { mutableStateOf("") }

    // Auto dropdown expand
    var showCategoryDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(showFormDialog, editProvider) {
        if (showFormDialog) {
            nameArInput = editProvider?.nameAr ?: ""
            nameEnInput = editProvider?.nameEn ?: ""
            phoneInput = editProvider?.phone ?: ""
            selectedCategoryId = editProvider?.categoryId ?: (categories.firstOrNull()?.id ?: "")
            ratingInput = (editProvider?.rating ?: 5.0f).toString()
            imageUrlInput = editProvider?.imageUrl ?: ""
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editProvider = null
                    showFormDialog = true
                },
                containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Provider")
            }
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(providers) { provider ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AR: ${provider.nameAr}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("EN: ${provider.nameEn}", color = Color.Gray, fontSize = 13.sp)
                            Text("Phone: ${provider.phone} | Rating: ${provider.rating}", color = Color.Gray, fontSize = 12.sp)
                        }
                        IconButton(onClick = {
                            editProvider = provider
                            showFormDialog = true
                        }) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.Blue)
                        }
                        IconButton(onClick = {
                            viewModel.deleteServiceProvider(provider.id)
                            Toast.makeText(context, if (isArabic) "تم حذف مقدم الخدمة" else "Provider deleted", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }

        // Add/Edit Dialog modal
        if (showFormDialog) {
            AlertDialog(
                onDismissRequest = { showFormDialog = false },
                title = {
                    Text(
                        if (editProvider == null) {
                            if (isArabic) "إضافة مقدم خدمة جديد" else "Add New Service Provider"
                        } else {
                            if (isArabic) "تعديل مقدم الخدمة" else "Edit Service Provider"
                        }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = nameArInput,
                            onValueChange = { nameArInput = it },
                            label = { Text("الاسم بالعربية (name_ar)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nameEnInput,
                            onValueChange = { nameEnInput = it },
                            label = { Text("الاسم بالإنجليزية (name_en)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("رقم الهاتف (phone)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ratingInput,
                            onValueChange = { ratingInput = it },
                            label = { Text("التقييم من 5 (rating)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = imageUrlInput,
                            onValueChange = { imageUrlInput = it },
                            label = { Text("رابط الصورة الشخصية (image_url)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Selector Category Field
                        val currentSelectedCategoryName = remember(categories, selectedCategoryId, isArabic) {
                            categories.find { it.id == selectedCategoryId }?.let {
                                if (isArabic) it.nameAr else it.nameEn
                            } ?: "Select Category"
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showCategoryDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Category: $currentSelectedCategoryName")
                            }
                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(if (isArabic) cat.nameAr else cat.nameEn) },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            showCategoryDropdown = false
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
                            val id = editProvider?.id ?: UUID.randomUUID().toString()
                            val rating = ratingInput.toFloatOrNull() ?: 5.0f
                            val imgUrl = imageUrlInput.ifBlank { null }
                            if (editProvider == null) {
                                viewModel.addServiceProvider(id, nameArInput, nameEnInput, phoneInput, selectedCategoryId, rating, imgUrl)
                            } else {
                                viewModel.updateServiceProvider(id, nameArInput, nameEnInput, phoneInput, selectedCategoryId, rating, imgUrl)
                            }
                            showFormDialog = false
                            Toast.makeText(context, if (isArabic) "تم الحفظ بنجاح!" else "Provider saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)))
                    ) {
                        Text(Translator.getString("save", language))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFormDialog = false }) {
                        Text(Translator.getString("cancel", language))
                    }
                }
            )
        }
    }
}

// 3. Dashboard: BRANDING / GRAPHICS THEME TAB
@Composable
fun DashboardBrandingTab(viewModel: AppViewModel) {
    val isArabic = viewModel.currentLanguage == "ar"
    val context = LocalContext.current

    var arAppName by remember { mutableStateOf(viewModel.appNameAr) }
    var enAppName by remember { mutableStateOf(viewModel.appNameEn) }
    var pColorHex by remember { mutableStateOf(viewModel.primaryColorHex) }
    var sColorHex by remember { mutableStateOf(viewModel.secondaryColorHex) }
    var arLetter by remember { mutableStateOf(viewModel.iconLetterAr) }
    var enLetter by remember { mutableStateOf(viewModel.iconLetterEn) }
    var footerTextVal by remember { mutableStateOf(viewModel.footerText) }
    var defaultLangVal by remember { mutableStateOf(viewModel.defaultLanguage) }

    var welcomeMsgAr by remember { mutableStateOf(viewModel.welcomeMessageAr) }
    var welcomeMsgEn by remember { mutableStateOf(viewModel.welcomeMessageEn) }
    var showInsteadOfLogo by remember { mutableStateOf(viewModel.showWelcomeMessageInsteadOfLogo) }
    var customLogoUrlVal by remember { mutableStateOf(viewModel.customWelcomeLogoUrl) }
    var geminiKeyVal by remember { mutableStateOf(viewModel.geminiApiKeySetting) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isArabic) "إعدادات الهوية البصرية والتسمية" else "App Branding & Graphics Customizer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = parseColor(pColorHex, Color(0xFF1E88E5))
            )
        }

        // App names inputs
        item {
            OutlinedTextField(
                value = arAppName,
                onValueChange = { arAppName = it },
                label = { Text(if (isArabic) "اسم التطبيق باللغة العربية" else "Arabic App Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = enAppName,
                onValueChange = { enAppName = it },
                label = { Text(if (isArabic) "اسم التطبيق باللغة الإنجليزية" else "English App Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Accent Colors Configs
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pColorHex,
                    onValueChange = { pColorHex = it },
                    label = { Text(if (isArabic) "اللون الرئيسي" else "Primary Color") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sColorHex,
                    onValueChange = { sColorHex = it },
                    label = { Text(if (isArabic) "اللون الثانوي" else "Secondary Color") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // App Icon Letters Config
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = arLetter,
                    onValueChange = { arLetter = it },
                    label = { Text(if (isArabic) "حرف الأيقونة (عربي)" else "Icon Letter (AR)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = enLetter,
                    onValueChange = { enLetter = it },
                    label = { Text(if (isArabic) "حرف الأيقونة (إنجليزي)" else "Icon Letter (EN)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Footer customization
        item {
            OutlinedTextField(
                value = footerTextVal,
                onValueChange = { footerTextVal = it },
                label = { Text(if (isArabic) "التذييل الإعلاني الثابت" else "Static Footer Advertisement") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Customizable Welcome messages & Welcome Image / Logo override
        item {
            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = if (isArabic) "إعدادات رسالة الترحيب والواجهة" else "Welcome & Alternative Greeting Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = parseColor(pColorHex, Color(0xFF1E88E5))
            )
        }

        item {
            OutlinedTextField(
                value = welcomeMsgAr,
                onValueChange = { welcomeMsgAr = it },
                label = { Text(if (isArabic) "رسالة الترحيب البديلة (عربي)" else "Alternative Welcome Message (AR)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = welcomeMsgEn,
                onValueChange = { welcomeMsgEn = it },
                label = { Text(if (isArabic) "رسالة الترحيب البديلة (إنجليزي)" else "Alternative Welcome Message (EN)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = showInsteadOfLogo,
                    onCheckedChange = { showInsteadOfLogo = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) "إظهار رسالة الترحيب كبديل للأيقونة في شاشة البداية" else "Show welcome message instead of logo icon on splash",
                    fontSize = 13.sp
                )
            }
        }

        item {
            OutlinedTextField(
                value = customLogoUrlVal,
                onValueChange = { customLogoUrlVal = it },
                label = { Text(if (isArabic) "رابط شارة/شعار مخصص (custom_logo_url)" else "Overwriting Image Logo URL") },
                placeholder = { Text("https://example.com/logo.png") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Gemini AI control configuration API Key
        item {
            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = if (isArabic) "إعدادات الذكاء الاصطناعي (Gemini AI)" else "Gemini Artificial Intelligence Key",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = parseColor(pColorHex, Color(0xFF1E88E5))
            )
        }

        item {
            OutlinedTextField(
                value = geminiKeyVal,
                onValueChange = { geminiKeyVal = it },
                label = { Text(if (isArabic) "مفتاح Gemini API Key مخصص" else "Custom Gemini API Key") },
                placeholder = { Text("AIzaSy...") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Default language config
        item {
            Text(if (isArabic) "اللغة الافتراضية للتطبيق:" else "Default Startup Language:", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = defaultLangVal == "ar",
                    onClick = { defaultLangVal = "ar" }
                )
                Text("العربية (Arabic)")
                Spacer(modifier = Modifier.width(20.dp))
                RadioButton(
                    selected = defaultLangVal == "en",
                    onClick = { defaultLangVal = "en" }
                )
                Text("English")
            }
        }

        // Submit actions
        item {
            Button(
                onClick = {
                    viewModel.updateAppConfig(
                        arName = arAppName,
                        enName = enAppName,
                        pColor = pColorHex,
                        sColor = sColorHex,
                        iconAr = arLetter,
                        iconEn = enLetter,
                        footer = footerTextVal,
                        defLang = defaultLangVal,
                        welcomeAr = welcomeMsgAr,
                        welcomeEn = welcomeMsgEn,
                        showInstead = showInsteadOfLogo,
                        logoUrl = customLogoUrlVal,
                        gKey = geminiKeyVal
                    )
                    Toast.makeText(context, if (isArabic) "تم حفظ التعديلات بنجاح!" else "Branding options updated!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = parseColor(pColorHex, Color(0xFF1E88E5))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isArabic) "تطبيق الهوية الآن" else "Apply Visual Identity")
            }
        }
    }
}

// 4. Dashboard: ADMISSIONS / ADMINS CONTROL TAB
@Composable
fun DashboardAdminsTab(viewModel: AppViewModel) {
    val isArabic = viewModel.currentLanguage == "ar"
    val context = LocalContext.current
    
    // Check if current user is admin
    val isMainAdmin = viewModel.loggedInUser == "admin"
    
    if (!isMainAdmin) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isArabic) "صلاحية إدارة المشرفين تظهر وتتاح للمدير العام (admin) فقط." else "Moderators management section is only visible to the general manager (admin).",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    var inputAdminName by remember { mutableStateOf("") }
    var inputAdminPassword by remember { mutableStateOf("") }
    
    // Changing password dialog states
    var showChangePwdDialog by remember { mutableStateOf(false) }
    var targetUserToChangePwd by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isArabic) "إدارة صلاحيات وبيانات المشرفين" else "Supervisor Management & Passwords",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
        )

        // Adding supervisor card styling
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEFEFEF))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "إضافة مشرف جديد" else "Register a New Supervisor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = inputAdminName,
                    onValueChange = { inputAdminName = it },
                    placeholder = { Text(if (isArabic) "اسم المشرف (مثال: maher)" else "Supervisor username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputAdminPassword,
                    onValueChange = { inputAdminPassword = it },
                    placeholder = { Text(if (isArabic) "كلمة المرور الابتدائية" else "Initial password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val username = inputAdminName.trim()
                        val pwd = inputAdminPassword.trim()
                        if (username.isNotBlank() && pwd.isNotBlank()) {
                            viewModel.addAdminUser(username, pwd)
                            Toast.makeText(context, if (isArabic) "تمت إضافة المشرف بنجاح!" else "New supervisor added successfully!", Toast.LENGTH_SHORT).show()
                            inputAdminName = ""
                            inputAdminPassword = ""
                        } else {
                            Toast.makeText(context, if (isArabic) "الرجاء تعبئة الاسم وكلمة المرور" else "Please enter username and initial password", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isArabic) "تسجيل المشرف" else "Register Supervisor")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isArabic) "المشرفون وصلاحيات الدخول الحالية:" else "Active Administrators & Passwords:",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.adminsList.toList()) { admin ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, null, tint = if (admin == "admin") Color(0xFFE91E63) else Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(admin, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${if (isArabic) "كلمة المرور" else "Password"}: ${viewModel.getAdminPassword(admin)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (admin == "admin") {
                                    Text(
                                        text = if (isArabic) "المدير العام" else "Administrator",
                                        color = Color(0xFFE91E63),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    targetUserToChangePwd = admin
                                    newPasswordInput = ""
                                    showChangePwdDialog = true
                                }
                            ) {
                                Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "تغيير كلمة المرور" else "Change Password", fontSize = 13.sp)
                            }

                            if (admin != "admin") {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        viewModel.removeAdminUser(admin)
                                        Toast.makeText(context, if (isArabic) "تم إلغاء صلاحية المشرف" else "Admin badge revoked", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isArabic) "حذف" else "Delete", color = Color.Red, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangePwdDialog) {
        AlertDialog(
            onDismissRequest = { showChangePwdDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تعديل كلمة مرور ($targetUserToChangePwd)" else "Edit password for ($targetUserToChangePwd)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic) "الرجاء إدخال كلمة المرور الجديدة أدناه:" else "Kindly enter the new passcode credentials below:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        placeholder = { Text(if (isArabic) "الرقم السري الجديد" else "New secret key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = newPasswordInput.trim()
                        if (cleaned.isNotBlank()) {
                            viewModel.updateAdminPassword(targetUserToChangePwd, cleaned)
                            Toast.makeText(context, if (isArabic) "تم تحديث كلمة المرور!" else "Credentials updated successfully!", Toast.LENGTH_SHORT).show()
                            showChangePwdDialog = false
                        } else {
                            Toast.makeText(context, if (isArabic) "لا يمكن أن تكون فارغة" else "Password can't be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)))
                ) {
                    Text(Translator.getString("save", viewModel.currentLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePwdDialog = false }) {
                    Text(Translator.getString("cancel", viewModel.currentLanguage))
                }
            }
        )
    }
}

// Translations Directory Map helper data
object Translator {
    val en = mapOf(
        "app_name" to "Dalili",
        "subtitle" to "All services in one app",
        "login" to "Login",
        "signup" to "Sign Up",
        "username" to "Username",
        "password" to "Password",
        "enter" to "Enter",
        "forgot_password" to "Forgot password?",
        "home" to "Home",
        "categories" to "Categories",
        "service_providers" to "Service Providers",
        "name" to "Name",
        "phone" to "Phone",
        "call" to "Call",
        "share" to "Share",
        "whatsapp" to "WhatsApp",
        "rating" to "Rating",
        "add" to "Add",
        "edit" to "Edit",
        "delete" to "Delete",
        "save" to "Save",
        "cancel" to "Cancel",
        "search" to "Search",
        "my_profile" to "My Profile",
        "settings" to "Settings",
        "about" to "About",
        "logout" to "Logout"
    )

    val ar = mapOf(
        "app_name" to "دليلي",
        "subtitle" to "كل الخدمات في تطبيق واحد",
        "login" to "تسجيل الدخول",
        "signup" to "إنشاء حساب جديد",
        "username" to "اسم المستخدم",
        "password" to "كلمة المرور",
        "enter" to "دخول",
        "forgot_password" to "نسيت كلمة المرور؟",
        "home" to "الصفحة الرئيسية",
        "categories" to "الأقسام",
        "service_providers" to "مقدمي الخدمات",
        "name" to "الاسم",
        "phone" to "رقم الهاتف",
        "call" to "اتصل",
        "share" to "مشاركة",
        "whatsapp" to "واتساب",
        "rating" to "تقييم",
        "add" to "إضافة",
        "edit" to "تعديل",
        "delete" to "حذف",
        "save" to "حفظ",
        "cancel" to "إلغاء",
        "search" to "بحث",
        "my_profile" to "ملفي الشخصي",
        "settings" to "إعدادات",
        "about" to "عن التطبيق",
        "logout" to "تسجيل خروج"
    )

    fun getString(key: String, language: String): String {
        return if (language == "en") {
            en[key] ?: ar[key] ?: key
        } else {
            ar[key] ?: en[key] ?: key
        }
    }
}

fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val dir = java.io.File(context.filesDir, "category_images")
        if (!dir.exists()) dir.mkdirs()
        
        val outFile = java.io.File(dir, "cat_${System.currentTimeMillis()}.jpg")
        val outputStream = java.io.FileOutputStream(outFile)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        outFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Chat message model
data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun DaliliAiAssistantDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categoriesList.collectAsStateWithLifecycle()
    val providers by viewModel.serviceProvidersList.collectAsStateWithLifecycle()
    val isArabic = viewModel.currentLanguage == "ar"
    val scope = rememberCoroutineScope()
    val primaryColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
    val secondaryColor = parseColor(viewModel.secondaryColorHex, Color(0xFFFB8C00))

    val chatMessages = remember {
        mutableStateListOf<ChatMessage>().apply {
            add(
                ChatMessage(
                    if (isArabic) 
                        "مرحباً بك! أنا مساعدك الذكي 🤖. كيف يمكنني مساعدتك اليوم في البحث عن الخدمات أو الهواتف في تطبيق دليلي؟"
                    else 
                        "Hello! I am your AI Assistant 🤖. How can I help you search for service providers or phone numbers today in Dalili?",
                    isUser = false
                )
            )
        }
    }

    var textInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Key selection logic: prioritizes admin's custom key, fallbacks to BuildConfig
    val apiKey = remember(viewModel.geminiApiKeySetting) {
        val customKey = viewModel.geminiApiKeySetting.trim()
        if (customKey.isNotEmpty()) customKey else com.example.BuildConfig.GEMINI_API_KEY
    }

    val missingKeyError = if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
        if (isArabic) 
            "عذرًا! لم يقم مدير التطبيق بمفتاح الذكاء الاصطناعي الخاص بـ Gemini API حتى الآن. الرجاء تهيئته من 'لوحة التحكم -> الهوية'."
        else 
            "Sorry! The Gemini API key hasn't been configured by the admin yet. Please set it in 'Dashboard -> Brand'."
    } else null

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.size > 0) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(top = 28.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of AI assistant
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🤖", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "مساعد دليلي الذكي" else "Dalili AI Assistant",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isArabic) "مستشارك المحلي المتكامل" else "Your smart local scout assistant",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = Color.White
                        )
                    }
                }

                // Chat message canvas list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!msg.isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(secondaryColor.copy(alpha = 0.15f))
                                        .align(Alignment.Top),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🤖", fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            
                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                    bottomEnd = if (msg.isUser) 4.dp else 16.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (msg.isUser) primaryColor else Color(0xFFF1F5F9)
                                ),
                                modifier = Modifier.widthIn(max = 280.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (msg.isUser) Color.White else Color(0xFF1E293B),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(secondaryColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🤖", fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    shape = RoundedCornerShape(topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                    modifier = Modifier.widthIn(max = 120.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "يكتب الآن..." else "Typing...",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (missingKeyError != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = missingKeyError,
                                    color = Color(0xFF991B1B),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Interactive recommendation chips
                if (chatMessages.size == 1 && missingKeyError == null) {
                    val promptChips = if (isArabic) {
                        listOf(
                            "هل يوجد كهربائي ممتاز؟",
                            "ما هي أقسام الخدمات المتاحة؟",
                            "ابحث لي عن هاتف نجار"
                        )
                    } else {
                        listOf(
                            "Is there any electrician?",
                            "List current categories",
                            "Explain how to use this app"
                        )
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(promptChips) { chipText ->
                            Card(
                                modifier = Modifier.clickable {
                                    textInput = chipText
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAED)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, secondaryColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = chipText,
                                    fontSize = 12.sp,
                                    color = secondaryColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Divider and input panel bar
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { 
                            Text(if (isArabic) "اسأل المساعد الذكي عن خدمات دليلي..." else "Ask AI assistant about Dalili services...") 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_input_field"),
                        singleLine = false,
                        maxLines = 3,
                        enabled = (missingKeyError == null && !isLoading),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            cursorColor = primaryColor
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val userMsg = textInput.trim()
                            if (userMsg.isNotEmpty()) {
                                textInput = ""
                                chatMessages.add(ChatMessage(userMsg, isUser = true))
                                isLoading = true

                                scope.launch {
                                    val conversationContext = chatMessages.joinToString("\n") { 
                                        if (it.isUser) "User: ${it.text}" else "AI Assistant: ${it.text}"
                                    }
                                    val systemInstructionBuilt = buildSystemInstruction(categories, providers, isArabic)
                                    val aiResponse = com.example.data.GeminiApi.generateContent(
                                        prompt = conversationContext,
                                        systemInstruction = systemInstructionBuilt,
                                        apiKey = apiKey
                                    )
                                    
                                    isLoading = false
                                    if (aiResponse == "API_KEY_MISSING") {
                                        chatMessages.add(
                                            ChatMessage(
                                                if (isArabic) 
                                                    "لم يتم تكوين مفتاح Gemini API بشكل صحيح. الرجاء التحقق من الإعدادات."
                                                else 
                                                    "API Key is missing or default. Please configure a valid key.",
                                                isUser = false
                                            )
                                        )
                                    } else {
                                        chatMessages.add(ChatMessage(aiResponse, isUser = false))
                                    }
                                }
                            }
                        },
                        enabled = (textInput.isNotBlank() && missingKeyError == null && !isLoading),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (textInput.isBlank() || missingKeyError != null || isLoading) Color(0xFFCBD5E1) else primaryColor)
                            .testTag("ai_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// System Instructions builder injected with database parameters dynamically!
fun buildSystemInstruction(categories: List<Category>, providers: List<ServiceProvider>, isArabic: Boolean): String {
    val categoryListStr = categories.joinToString("\n- ") { 
        if (isArabic) "${it.nameAr} (الرمز: ${it.id})" else "${it.nameEn} (ID: ${it.id})"
    }
    val providerListStr = providers.joinToString("\n- ") {
        val cat = categories.find { c -> c.id == it.categoryId }
        val catName = if (cat != null) {
            if (isArabic) cat.nameAr else cat.nameEn
        } else "منوع"
        if (isArabic) {
            "${it.nameAr} - الهاتف: ${it.phone} - القسم: $catName (التقييم: ${it.rating})"
        } else {
            "${it.nameEn} - Phone: ${it.phone} - Category: $catName (Rating: ${it.rating})"
        }
    }

    return """
        أنت "مساعد دليلي الذكي" 🤖، الدليل المحلي الذكي الوحيد والمستشار المتكامل لتطبيق "دليلي" (Dalili اليمن).
        مهمتك الأساسية هي الإجابة بوجه بشوش، محترم، ولهجة يمنية لطيفة ومحببة (أو لغة عربية فصحى مبسطة) على أسئلة المستخدمين ومساعدتهم في العثور على مقدمي الخدمات المناسبين في اليمن.
        
        إليك قائمة الأقسام الحالية المتوفرة في التطبيق بشكل حي ومباشر:
        - $categoryListStr
        
        وإليك قائمة مقدمي الخدمات الفعليين المتوفرين في قاعدة بياناتنا:
        - $providerListStr
        
        تعليمات هامة:
        1. شجع المستخدم واقترح عليه مقدمي الخدمات الفعليين المتوفرين في القائمة أعلاه فقط عندما يسألك عن خدمة معينة! واذكر لهم رقم الهاتف والتقييم وقسم الخدمة بوضوح!
        2. إذا لم يكن هناك مقدم الخدمة المطلوب متوفراً في قائمتنا الحالية، فاعتذر بلطف وأخبرهم أنك ستنقل اقتراحهم للإدارة لإضافته قريباً، ولكن اقترح عليهم أقرب قسم أو ممثل بديل في القائمة إن وجد.
        3. تحدث بروح يمنية دافئة ولطيفة (مثلاً استخدم كلمات ترحيبية يمنية مثل: "أهلاً وسهلاً بك يا غالي"، "على راسي"، "من عيوني").
        4. إذا سألوا عن معلومات عامة عن مناطق ومدن اليمن أو كيفية استخدام التطبيق، أجبهم بذكاء ودقة واحترافية.
        
        احرص دائماً أن تكون إجابتك مختصرة وواضحة لكي تسع شاشة الهاتف الذكي بشكل مريح وسهل القراءة.
    """.trimIndent()
}

