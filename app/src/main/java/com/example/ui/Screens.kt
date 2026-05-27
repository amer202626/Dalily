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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.*
import kotlinx.coroutines.delay
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

    LaunchedEffect(Unit) {
        delay(2000)
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
            verticalArrangement = Arrangement.Center
        ) {
            // App adaptive mockup icon
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isArabic) viewModel.iconLetterAr else viewModel.iconLetterEn,
                        color = Color.White,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "دليلي" else "Dalili",
                        color = secondaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isArabic) viewModel.appNameAr else viewModel.appNameEn,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Translator.getString("subtitle", viewModel.currentLanguage),
                fontSize = 16.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
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
            bottomBar = {
                // Persistent Advertisement Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEEEEEE))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.footerText,
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
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
                onDismiss = { showLoginDialog = false }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category.icon),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (language == "en") category.nameEn else category.nameAr,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Call
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("call_button_${provider.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Call, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translator.getString("call", language))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // WhatsApp
                OutlinedButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("whatsapp_button_${provider.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Chat, null, tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translator.getString("whatsapp", language), color = Color(0xFF4CAF50))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Share
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("share_button_${provider.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translator.getString("share", language))
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
                    if (viewModel.verifyBackdoorPassword(inputPassword)) {
                        Toast.makeText(context, if (isArabic) "تم الدخول بنجاح!" else "Owner authenticated!", Toast.LENGTH_SHORT).show()
                        onAuthenticated()
                    } else {
                        Toast.makeText(context, if (isArabic) "كلمة المرور خاطئة" else "Incorrect password", Toast.LENGTH_SHORT).show()
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
    onDismiss: () -> Unit
) {
    val language = viewModel.currentLanguage
    val isArabic = language == "ar"
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterState by remember { mutableStateOf(false) }

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
                    } else if (isRegisterState) {
                        Translator.getString("signup", language)
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
                        text = if (isArabic) "العضوية: مستخدم نشط" else "Rank: Active Client",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
                    // Enter Login/Signup inputs
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
                            if (viewModel.loginUser(username)) {
                                Toast.makeText(context, if (isArabic) "تم تسجيل الدخول!" else "Logged in!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))),
                        modifier = Modifier.fillMaxWidth().testTag("login_submit_btn")
                    ) {
                        Text(if (isRegisterState) Translator.getString("signup", language) else Translator.getString("enter", language))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { isRegisterState = !isRegisterState }) {
                        Text(
                            text = if (isRegisterState) {
                                if (isArabic) "لديك حساب بالفعل؟ سجل دخول" else "Already have an account? Login"
                            } else {
                                if (isArabic) "لا تملك حساباً؟ أنشئ حساب جديد" else "Don't have an account? Sign Up"
                            }
                        )
                    }

                    TextButton(onClick = {
                        Toast.makeText(context, if (isArabic) "سرية للغاية - اتصل بالمسؤول" else "Top secret - call administrator", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(Translator.getString("forgot_password", language), color = Color.Gray, fontSize = 12.sp)
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

    var activeTab by remember { mutableStateOf(0) } // 0: Categories, 1: Providers, 2: App Branding, 3: Admins

    MainLayout(
        viewModel = viewModel,
        navController = navController,
        showBackButton = true,
        titleContent = {
            Text(
                text = if (isArabic) "لوحة تحكم المالك" else "Owner Dashboard",
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
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text(if (isArabic) "الأقسام" else "Categories", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text(if (isArabic) "مقدمو الخدمات" else "Providers", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text(if (isArabic) "الهوية" else "Brand", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text(if (isArabic) "المشرفون" else "Admins", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }

            // Tab contents rendering
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> DashboardCategoriesTab(categories, viewModel)
                    1 -> DashboardProvidersTab(providers, categories, viewModel)
                    2 -> DashboardBrandingTab(viewModel)
                    3 -> DashboardAdminsTab(viewModel)
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

    LaunchedEffect(showFormDialog, editCategory) {
        if (showFormDialog) {
            nameArInput = editCategory?.nameAr ?: ""
            nameEnInput = editCategory?.nameEn ?: ""
            iconInput = editCategory?.icon ?: "home_repair_service"
            orderIndexInput = (editCategory?.orderIndex ?: (categories.size + 1)).toString()
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
                        Icon(
                            getCategoryIcon(category.icon),
                            null,
                            tint = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)),
                            modifier = Modifier.size(36.dp)
                        )
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = editCategory?.id ?: UUID.randomUUID().toString()
                            val oIndex = orderIndexInput.toIntOrNull() ?: 1
                            if (editCategory == null) {
                                viewModel.addCategory(id, nameArInput, nameEnInput, iconInput, oIndex)
                            } else {
                                viewModel.updateCategory(id, nameArInput, nameEnInput, iconInput, oIndex)
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
                        defLang = defaultLangVal
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
    var inputAdminName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isArabic) "إدارة المشرفين والمدراء" else "Admin Access Credentials Center",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = inputAdminName,
                onValueChange = { inputAdminName = it },
                placeholder = { Text(if (isArabic) "اسم المستخدم للمشرف" else "Admin username") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (inputAdminName.isNotBlank()) {
                        viewModel.addAdminUser(inputAdminName.trim())
                        Toast.makeText(context, if (isArabic) "تمت إضافة المشرف!" else "Admin credential registered!", Toast.LENGTH_SHORT).show()
                        inputAdminName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = parseColor(viewModel.primaryColorHex, Color(0xFF1E88E5)))
            ) {
                Text(if (isArabic) "إضافة" else "Add")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (isArabic) "المشرفون النشطون حالياً:" else "Currently Authorized Admins:",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.adminsList.toList()) { admin ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9F9), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(admin, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    // Prevent deleting master default admins to ensure owner safety
                    if (admin != "maher" && admin != "admin") {
                        IconButton(onClick = {
                            viewModel.removeAdminUser(admin)
                            Toast.makeText(context, if (isArabic) "تم إلغاء صلاحية المشرف" else "Admin credential revoked", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, "Revoke admin badge", tint = Color.Red)
                        }
                    } else {
                        Text(
                            text = if (isArabic) "أساسي" else "Master",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
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
