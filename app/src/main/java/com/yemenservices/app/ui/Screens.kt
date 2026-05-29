@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.yemenservices.app.ui

import java.util.Locale
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
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
import coil.compose.AsyncImage
import com.yemenservices.app.data.Admin
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.ServiceProvider
import com.yemenservices.app.data.AppConfig

// Raw static color configurations
val DarkBackground = Color(0xFF121212)
val CardBackground = Color(0xFF1E1E1E)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFB0B0B0)

// Helper function to resolve dynamic color mapping instantly
fun getThemeColor(themeName: String): Color {
    return when (themeName.lowercase()) {
        "emerald" -> Color(0xFF5EAAA8)  // Soft Calm Emerald Sage
        "ocean" -> Color(0xFF5199FF)    // Soft Ocean Sky Blue
        "purple" -> Color(0xFF9E77F1)   // Soft Calm Lavender/Lilac
        "sunset" -> Color(0xFFE88A8A)   // Elegant Soft Sunset Coral
        "classic" -> Color(0xFF2196F3)  // Modern Elegant Sky Blue (Matching Image top-bar)
        else -> Color(0xFFCBB89B)       // Soft Luxury Gold (Default)
    }
}

// Convert Android URI to scaled base64 string safely
fun uriToBase64(context: android.content.Context, uri: Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream?.close() ?: return ""
        
        // Scale down image to maximum size (400x400) to optimize Firestore size to keep it extremely fast
        val scaledBitmap = if (bitmap.width > 400 || bitmap.height > 400) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newWidth = if (ratio > 1) 400 else (400 * ratio).toInt()
            val newHeight = if (ratio > 1) (400 / ratio).toInt() else 400
            android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        val outputStream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.e("ScreensImagePicker", "Failed to encode photo", e)
        ""
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var isArabic by remember { mutableStateOf(true) }

    // Stream database elements
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val providers by viewModel.serviceProviders.collectAsStateWithLifecycle(initialValue = emptyList())
    val reviews by viewModel.reviews.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle(initialValue = AppConfig())

    // Dynamic coloring based on Firestore configurations
    val primaryColor = getThemeColor(appConfig.app_theme)

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedProviderId by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf("home") } // "home", "providers", "provider-detail", "admin"
    var searchQuery by remember { mutableStateOf("") }

    // Click tracker for backend backdoor access
    var logoClickCount by remember { mutableStateOf(0) }
    var showBackdoorDialog by remember { mutableStateOf(false) }

    // Overlay chat robot visibility
    var showAiChatSheet by remember { mutableStateOf(false) }

    // Addition Dialog flags
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var showAddReviewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left-side controls (exactly like the user's uploaded image!)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Person/Profile icon
                            IconButton(
                                onClick = {
                                    currentScreen = "admin"
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(22.dp))
                            }

                            // 2. Small Settings Gear icon next to language (the Admin dashboard entrance)
                            IconButton(
                                onClick = {
                                    currentScreen = "admin"
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Admin settings gear logo", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            // 3. Globe/Language switcher icon
                            IconButton(
                                onClick = { isArabic = !isArabic },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Language toggle", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            // 4. Search indicator icon
                            IconButton(
                                onClick = {
                                    currentScreen = "home"
                                    Toast.makeText(context, if (isArabic) "اكتب في مربع البحث الموضح أدناه" else "Type in the search field below", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search shortcut icon in top bar", tint = Color.White, modifier = Modifier.size(20.dp))
                            }

                            // 5. Refresh helper sync icon
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, if (isArabic) "تمت المزامنة الفورية بنجاح!" else "Propagated & synced successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Manual sync indicator", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Right-side main branding (dynamic Title and gorgeous badge overlapping circle logo)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable {
                                logoClickCount++
                                if (logoClickCount >= 5) {
                                    logoClickCount = 0
                                    showBackdoorDialog = true
                                }
                            }
                        ) {
                            // Remote dynamic Brand name (default: دليلي)
                            Text(
                                text = appConfig.app_name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            // White outer circular badge and styled inner theme color circle containing the letters (default: خد)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appConfig.app_logo_text,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    if (currentScreen != "home") {
                        IconButton(onClick = {
                            when (currentScreen) {
                                "providers" -> {
                                    currentScreen = "home"
                                    selectedCategoryId = null
                                }
                                "provider-detail" -> {
                                    currentScreen = "providers"
                                    selectedProviderId = null
                                }
                                "admin" -> {
                                    currentScreen = "home"
                                }
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back Navigation", tint = Color.White)
                        }
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
        ) {
            when (currentScreen) {
                "home" -> {
                    HomeScreen(
                        categories = categories,
                        providers = providers,
                        appConfig = appConfig,
                        isArabic = isArabic,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onCategorySelect = { catId ->
                            selectedCategoryId = catId
                            currentScreen = "providers"
                        },
                        onProviderSelect = { provId ->
                            selectedProviderId = provId
                            currentScreen = "provider-detail"
                        },
                        primaryColor = primaryColor
                    )
                }
                "providers" -> {
                    val category = categories.firstOrNull { it.id == selectedCategoryId }
                    val categoryProviders = providers.filter { it.category_id == selectedCategoryId }
                    
                    // Apply Pinned logic and dynamic sorting filter choices
                    val queryFiltered = categoryProviders.filter {
                        it.name_ar.contains(searchQuery, ignoreCase = true) || it.name_en.contains(searchQuery, ignoreCase = true)
                    }
                    val sortedProviders = queryFiltered.sortedWith(
                        compareByDescending<ServiceProvider> { it.is_pinned }
                            .thenBy {
                                when (appConfig.list_sort_mode.lowercase()) {
                                    "name" -> if (isArabic) it.name_ar else it.name_en
                                    else -> it.id // fallback (date created sort)
                                }
                            }
                    )

                    ProvidersScreen(
                        category = category,
                        providers = sortedProviders,
                        reviews = reviews,
                        appConfig = appConfig,
                        isArabic = isArabic,
                        onProviderSelect = { provId ->
                            selectedProviderId = provId
                            currentScreen = "provider-detail"
                        },
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        currentAdmin = currentAdmin,
                        onAddProviderClick = { showAddProviderDialog = true },
                        onDeleteProvider = { id -> viewModel.deleteServiceProvider(id) },
                        onTogglePinProvider = { provider -> 
                            val updated = provider.copy(is_pinned = !provider.is_pinned)
                            viewModel.updateServiceProvider(updated)
                        },
                        primaryColor = primaryColor
                    )
                }
                "provider-detail" -> {
                    val providerItem = providers.firstOrNull { it.id == selectedProviderId }
                    val providerReviews = reviews.filter { it.provider_id == selectedProviderId }
                    ProviderDetailScreen(
                        provider = providerItem,
                        reviews = providerReviews,
                        isArabic = isArabic,
                        appConfig = appConfig,
                        onAddReviewClick = { showAddReviewDialog = true },
                        onDeleteReview = { reviewId -> viewModel.deleteReview(reviewId, selectedProviderId ?: "") },
                        currentAdmin = currentAdmin,
                        primaryColor = primaryColor
                    )
                }
                "admin" -> {
                    AdminScreen(
                        viewModel = viewModel,
                        isArabic = isArabic,
                        categories = categories,
                        providers = providers,
                        appConfig = appConfig,
                        onAddCategoryClick = { showAddCategoryDialog = true },
                        onDeleteCategory = { id -> viewModel.deleteCategory(id) },
                        onEditCategoryClick = { cat -> selectedCategoryToEdit = cat },
                        primaryColor = primaryColor
                    )
                }
            }

            // Custom perfectly circular extremely compact Floating AI button (Robot emoji with text "خدمات" below)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 20.dp, start = 20.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)) // Slate dark
                    .border(BorderStroke(1.5.dp, primaryColor), CircleShape)
                    .clickable { showAiChatSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "AI Assistant",
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isArabic) "خدمات" else "Services",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Real-time Dialog integrations
            if (showAddCategoryDialog) {
                AddCategoryDialog(
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    onDismiss = { showAddCategoryDialog = false },
                    onConfirm = { nameAr, nameEn, icon, order, imgUrl ->
                        viewModel.addCategory("", nameAr, nameEn, icon, order, imgUrl)
                        showAddCategoryDialog = false
                    }
                )
            }

            if (showAddProviderDialog) {
                AddProviderDialog(
                    isArabic = isArabic,
                    categoryId = selectedCategoryId ?: "",
                    primaryColor = primaryColor,
                    onDismiss = { showAddProviderDialog = false },
                    onConfirm = { nameAr, nameEn, phone, imgUrl, isPinned ->
                        viewModel.addServiceProvider("", nameAr, nameEn, phone, selectedCategoryId ?: "", imgUrl, isPinned = isPinned)
                        showAddProviderDialog = false
                    }
                )
            }

            if (showAddReviewDialog) {
                AddReviewDialog(
                    isArabic = isArabic,
                    primaryColor = primaryColor,
                    onDismiss = { showAddReviewDialog = false },
                    onConfirm = { name, comment, rating ->
                        viewModel.addReview(selectedProviderId ?: "", name, comment, rating)
                        showAddReviewDialog = false
                    }
                )
            }

            // Secret Backdoor Password Dialog Interface
            if (showBackdoorDialog) {
                var backdoorPasswordInput by remember { mutableStateOf("") }
                var backdoorErrorMsg by remember { mutableStateOf<String?>(null) }
                Dialog(onDismissRequest = { showBackdoorDialog = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "تسجيل الدخول الخلفي السري" else "Secret Backdoor Entry",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = backdoorPasswordInput,
                                onValueChange = { backdoorPasswordInput = it },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                label = { Text(if (isArabic) "كلمة المرور السرية" else "Secret Password") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (backdoorErrorMsg != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = backdoorErrorMsg!!, color = Color.Red, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showBackdoorDialog = false }) {
                                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextWhite)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (backdoorPasswordInput.trim() == "maher--736462") {
                                            viewModel.login("Maher Ahmed", "maher--736462")
                                            showBackdoorDialog = false
                                            currentScreen = "admin"
                                            Toast.makeText(context, if (isArabic) "مرحباً يا ماهر! تم الدخول" else "Welcome Maher Ahmed!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            backdoorErrorMsg = if (isArabic) "كلمة المرور غير صحيحة!" else "Incorrect Password!"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                                ) {
                                    Text(if (isArabic) "دخول" else "Login")
                                }
                            }
                        }
                    }
                }
            }

            // Google Gemini Chat Assistant Popup Dialog Sheet
            if (showAiChatSheet) {
                val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
                val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
                var userInputText by remember { mutableStateOf("") }

                Dialog(onDismissRequest = { showAiChatSheet = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(1.5.dp, primaryColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Chat Header
                            Row(
                                modifier = Modifier.fillMaxWidth().background(primaryColor.copy(alpha = 0.1f)).padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "مساعد جوجل الذكي" else "Google AI Assistant",
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Row {
                                    IconButton(onClick = { viewModel.clearAiChat() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Clear", tint = primaryColor)
                                    }
                                    IconButton(onClick = { showAiChatSheet = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Red)
                                    }
                                }
                            }

                            // Chat Messages stream
                            LazyColumn(
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                                reverseLayout = false,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(aiMessages) { msg ->
                                    val isUser = msg.isUser
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isUser) 12.dp else 0.dp,
                                                bottomEnd = if (isUser) 0.dp else 12.dp
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isUser) primaryColor.copy(alpha = 0.2f) else CardBackground.copy(alpha = 0.5f)
                                            ),
                                            border = BorderStroke(
                                                width = 0.5.dp, 
                                                color = if (isUser) primaryColor else TextGray.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.widthIn(max = 260.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = msg.text,
                                                    color = TextWhite,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isAiLoading) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = primaryColor, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isArabic) "جاري التفكير..." else "Thinking...", color = TextGray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // Input row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = userInputText,
                                    onValueChange = { userInputText = it },
                                    placeholder = { Text(if (isArabic) "اكتب سؤالك هنا..." else "Type message...", fontSize = 13.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (userInputText.isNotBlank()) {
                                            viewModel.sendAiMessage(userInputText)
                                            userInputText = ""
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = primaryColor, contentColor = Color.Black),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Centered App footer writing customized details
@Composable
fun AppFooter(appConfig: AppConfig, primaryColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val contactNumber = appConfig.footer_phone.ifBlank { "777644670" }
        Text(
            text = "تطوير Maher ahmed — رقم المبرمج والمصمم: $contactNumber",
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp, // Reduced 15% from 11.sp
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "دليلي للخدمات والمهن باليمن © ٢٠٢٦",
            color = TextGray.copy(alpha = 0.5f),
            fontSize = 8.sp, // Reduced from 9.sp
            textAlign = TextAlign.Center
        )
    }
}

// Modern search bar with premium colors - beautifully compact and center-aligned
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder, color = TextGray, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = primaryColor, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = primaryColor, modifier = Modifier.size(14.dp))
                    }
                }
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextWhite),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(44.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = TextGray.copy(alpha = 0.35f),
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
    }
}

// Convert icons strings accurately to Vectors
fun getIconForCategory(iconKey: String): ImageVector {
    return when (iconKey.lowercase()) {
        "medical", "health", "hospital" -> Icons.Default.Info
        "taxi", "transport", "car" -> Icons.Default.PlayArrow
        "repair", "maintenance", "plumber", "tools", "work" -> Icons.Default.Build
        "restaurant", "food", "eat" -> Icons.Default.Star
        "education", "teacher", "school" -> Icons.Default.Home
        "shopping", "market", "shop" -> Icons.Default.ShoppingCart
        "hotel", "travel", "home" -> Icons.Default.Home
        "electrician", "electricity", "flash" -> Icons.Default.Warning
        "phone", "communication" -> Icons.Default.Phone
        "delivery", "shipping" -> Icons.Default.ShoppingCart
        else -> Icons.Default.Build
    }
}

// ======================== DETAIL SCREEN COMPOSE IMPLEMENTATIONS ========================

@Composable
fun HomeScreen(
    categories: List<Category>,
    providers: List<ServiceProvider>,
    appConfig: AppConfig,
    isArabic: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onProviderSelect: (String) -> Unit,
    primaryColor: Color
) {
    // Standard filtered categories matching query strings
    val filteredCategories = categories.filter {
        it.name_ar.contains(searchQuery, ignoreCase = true) || it.name_en.contains(searchQuery, ignoreCase = true)
    }

    // Global Search logs of Service Providers matching name query strings directly from HomeScreen
    val showGlobalSearchResults = searchQuery.isNotBlank()
    val globalProviderMatches = if (showGlobalSearchResults) {
        providers.filter {
            it.name_ar.contains(searchQuery, ignoreCase = true) || it.name_en.contains(searchQuery, ignoreCase = true)
        }
    } else emptyList()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // -15% Compact welcoming header banner card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "مرحباً بك في تطبيق دليلي" else "Welcome to Dalili",
                    color = primaryColor,
                    fontSize = 16.sp, // Reduced 15% from 20.sp
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val isWelcomeAi = appConfig.welcome_msg_mode.lowercase() == "ai"
                Text(
                    text = if (isWelcomeAi) appConfig.custom_welcome_msg else {
                        if (isArabic) "دليلك السريع للوصول لجميع الخدمات والمهن في اليمن فوراً"
                        else "Your quickest access to services and professionals in Yemen"
                    },
                    color = TextWhite,
                    fontSize = 10.sp, // Reduced 15% from 12.sp
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }

        // -15% Compact search input log
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = if (isArabic) "ابحث عن الأقسام أو الأشخاص أو المهن..." else "Search departments, skills or names...",
            primaryColor = primaryColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Display search results of practitioners/professionals if actively typing query
        if (showGlobalSearchResults) {
            Text(
                text = if (isArabic) "نتائج البحث في مزودي الخدمات والمهن (${globalProviderMatches.size})" else "Service Providers Matches (${globalProviderMatches.size})",
                color = primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (globalProviderMatches.isEmpty()) {
                Text(
                    text = if (isArabic) "لا توجد أسماء مطابقة لبحثك في سجل المهن." else "No service providers found for your query.",
                    color = TextGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(0.4f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(globalProviderMatches) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onProviderSelect(p.id) },
                            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) p.name_ar else p.name_en,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "هاتف: ${p.phone}",
                                        color = TextGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Available categories title
        Text(
            text = if (isArabic) "الأقسام المتاحة" else "All Service Sectors",
            color = primaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "عذراً، لا تتوفر أقسام مطابقة." else "No sectors match your query.",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        } else {
            // LazyVerticalGrid for immersive categorised layouts with Full photo backdrops (exactly like uploaded image!)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).testTag("categories_grid"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCategories, key = { it.id }) { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.28f) // Perfect aspect ratio matching screenshot
                            .clickable { onCategorySelect(cat.id) }
                            .testTag("category_${cat.id}"),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.5.dp, TextWhite.copy(alpha = 0.12f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!cat.image_url.isNullOrBlank()) {
                                AsyncImage(
                                    model = cat.image_url,
                                    contentDescription = cat.name_en,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Overlapping black gradient for gorgeous readability overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                                startY = 0f
                                            )
                                        )
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(CardBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForCategory(cat.icon),
                                        contentDescription = null,
                                        tint = primaryColor.copy(alpha = 0.3f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            // Styled Text overlay aligned bottom right (Arabic standard) or bottom end
                            Text(
                                text = if (isArabic) cat.name_ar else cat.name_en,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Center signature app footer at the bottom of the grid options
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    AppFooter(appConfig, primaryColor)
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
    appConfig: AppConfig,
    isArabic: Boolean,
    onProviderSelect: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    currentAdmin: Admin?,
    onAddProviderClick: () -> Unit,
    onDeleteProvider: (String) -> Unit,
    onTogglePinProvider: (ServiceProvider) -> Unit,
    primaryColor: Color
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isArabic) (category?.name_ar ?: "مزودي الخدمة") else (category?.name_en ?: "Service Providers"),
                    color = primaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isArabic) "انقر للاتصال مباشرة ورؤية التقييمات والتعليقات" else "Tap to call or read visitor opinions",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
            if (currentAdmin != null) {
                IconButton(
                    onClick = onAddProviderClick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = primaryColor, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Provider")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = if (isArabic) "ابحث عن الاسم في هذه الفئة..." else "Search name inside category...",
            primaryColor = primaryColor
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (providers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "عذراً، لم يتم العثور على مقدمي خدمات." else "No service providers available.",
                    color = TextGray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("providers_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(providers, key = { it.id }) { provider ->
                    val providerReviews = reviews.filter { it.provider_id == provider.id }
                    val averageRating = if (providerReviews.isNotEmpty()) {
                        providerReviews.map { it.rating }.average()
                    } else 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onProviderSelect(provider.id) },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(
                            width = if (provider.is_pinned) 1.dp else 0.5.dp, 
                            color = if (provider.is_pinned) primaryColor else TextGray.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!provider.image_url.isNullOrBlank()) {
                                        AsyncImage(
                                            model = provider.image_url,
                                            contentDescription = provider.name_en,
                                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(52.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isArabic) provider.name_ar else provider.name_en,
                                                color = TextWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            // Display Pinned Icon Gold Badge at top of sorting listings
                                            if (provider.is_pinned) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier.background(primaryColor, shape = RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = if (isArabic) "مثبت" else "Pinned",
                                                        color = Color.Black,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "هاتف: ${provider.phone}",
                                            color = TextGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Dynamic call button trigger
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                                context.startActivity(i)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "فشل الاتصال: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = primaryColor.copy(alpha = 0.15f), contentColor = primaryColor)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call")
                                    }

                                    // If admin, expose Pin/Unpin action and Delete action directly
                                    if (currentAdmin != null) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onTogglePinProvider(provider) }
                                        ) {
                                            Icon(
                                                imageVector = if (provider.is_pinned) Icons.Default.Star else Icons.Outlined.Star, 
                                                contentDescription = "Pin", 
                                                tint = if (provider.is_pinned) primaryColor else TextGray
                                            )
                                        }
                                        IconButton(onClick = { onDeleteProvider(provider.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }

                            // Dynamic ratings summary and miniature comment feed inside each practitioner card
                            if (providerReviews.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", averageRating) + "/5" + " (${providerReviews.size} " + (if (isArabic) "تقييمات" else "reviews") + ")",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (appConfig.show_reviews_enabled && providerReviews.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    providerReviews.take(1).forEach { r ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(CardBackground.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = r.user_name.ifBlank { if (isArabic) "زائر مجهول" else "Guest Visitor" },
                                                    color = primaryColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = primaryColor, modifier = Modifier.size(10.dp))
                                                    Text(text = "${r.rating.toInt()}/5", color = TextWhite, fontSize = 9.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = r.comment,
                                                color = TextWhite,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    AppFooter(appConfig, primaryColor)
                }
            }
        }
    }
}

@Composable
fun ProviderDetailScreen(
    provider: ServiceProvider?,
    reviews: List<Review>,
    isArabic: Boolean,
    appConfig: AppConfig,
    onAddReviewClick: () -> Unit,
    onDeleteReview: (String) -> Unit,
    currentAdmin: Admin?,
    primaryColor: Color
) {
    if (provider == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isArabic) "مقدم الخدمة غير متوفر" else "Provider Details Unavailable")
        }
        return
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Professional Header card containing contacts
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!provider.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = provider.image_url,
                        contentDescription = provider.name_ar,
                        modifier = Modifier.size(90.dp).clip(CircleShape).background(DarkBackground),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(90.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor, modifier = Modifier.size(45.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isArabic) provider.name_ar else provider.name_en,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "فشل فتح الاتصال", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(45.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${if (isArabic) "اتصل بمقدم الخدمة" else "Call Provider"}: ${provider.phone}", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Review Header: Option to toggle/hide Add review button based on config
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "التقييمات وآراء الزائرين (${reviews.size})" else "Reviews & Feedbacks (${reviews.size})",
                color = primaryColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Expose the review submission button if configured of admin interest
            if (appConfig.show_reviews_enabled) {
                TextButton(
                    onClick = onAddReviewClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = primaryColor)
                ) {
                    Icon(Icons.Default.Create, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "إضافة تقييم" else "Write opinion", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (reviews.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "لا توجد تقييمات معروضة لهذا الفرد حالياً." else "No reviews available yet.",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reviews) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = r.user_name.ifBlank { if (isArabic) "زائر مجهول" else "Guest Visitor" },
                                    color = primaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${r.rating.toInt()}/5",
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    // Admi supervisor can delete comments directly from detail listing
                                    if (currentAdmin != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { onDeleteReview(r.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Review", tint = Color.Red)
                                        }
                                    }
                   fun AdminScreen(
    viewModel: AppViewModel,
    isArabic: Boolean,
    categories: List<Category>,
    providers: List<ServiceProvider>,
    appConfig: AppConfig,
    onAddCategoryClick: () -> Unit,
    onDeleteCategory: (String) -> Unit,
    onEditCategoryClick: (Category) -> Unit,
    primaryColor: Color
) {
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    var adminUsernameInput by remember { mutableStateOf("") }
    var adminPasswordInput by remember { mutableStateOf("") }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("dalili_prefs", Context.MODE_PRIVATE) }
    var rememberMe by remember { mutableStateOf(sharedPrefs.getBoolean("remember", false)) }

    // On start, if rememberMe is enabled, autofill credentials
    LaunchedEffect(rememberMe) {
        if (rememberMe) {
            adminUsernameInput = sharedPrefs.getString("saved_user", "") ?: ""
            adminPasswordInput = sharedPrefs.getString("saved_pass", "") ?: ""
        }
    }

    if (currentAdmin == null) {
        // Safe normal admin login credential panel
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, primaryColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isArabic) "تسجيل دخول المشرفين" else "Admin Dashboard Access",
                        color = primaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = adminUsernameInput,
                        onValueChange = { adminUsernameInput = it },
                        singleLine = true,
                        label = { Text(if (isArabic) "اسم المستخدم" else "Username") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Remember Me / Save Password option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { ch ->
                                rememberMe = ch
                                sharedPrefs.edit().putBoolean("remember", ch).apply()
                                if (!ch) {
                                    sharedPrefs.edit().remove("saved_user").remove("saved_pass").apply()
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Text(
                            text = if (isArabic) "حفظ كلمة المرور" else "Save Password / Remember Me",
                            color = TextWhite,
                            fontSize = 12.sp
                        )
                    }

                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loginError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (viewModel.login(adminUsernameInput, adminPasswordInput)) {
                                if (rememberMe) {
                                    sharedPrefs.edit()
                                        .putString("saved_user", adminUsernameInput)
                                        .putString("saved_pass", adminPasswordInput)
                                        .apply()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "دخول" else "Login", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Safe access detection based on Owner backdoor Maher Ahmed or super_admin role
        val isSuperAdmin = currentAdmin?.role == "super_admin" || currentAdmin?.username?.trim().equals("Maher Ahmed", ignoreCase = true)
        
        // Define tabs list dynamically - NO access whatsoever to moderator role
        val adminTabs = remember(isSuperAdmin, isArabic) {
            if (isSuperAdmin) {
                listOf(
                    Triple(0, if (isArabic) "الأقسام والمهن" else "Sectors"),
                    Triple(1, if (isArabic) "إدارة المشرفين" else "Supervisors"),
                    Triple(2, if (isArabic) "الإعدادات العامة" else "General Settings")
                )
            } else {
                listOf(
                    Triple(0, if (isArabic) "الأقسام والمهن" else "Sectors"),
                    Triple(2, if (isArabic) "الإعدادات العامة" else "General Settings")
                )
            }
        }

        var activeAdminTab by remember { mutableStateOf(0) }
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Admin Dashboard greeting banner
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isArabic) "المشرف: ${currentAdmin?.username}" else "Admin: ${currentAdmin?.username}",
                        color = primaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "تغييراتك تتزامن مع بقية الأجهزة فوراً!" else "Changes propagate globally instantly!",
                        color = Color.Green,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red)
                ) {
                    Text(if (isArabic) "خروج" else "Logout", fontSize = 12.sp)
                }
            }

            // Tab switches
            TabRow(
                selectedTabIndex = adminTabs.indexOfFirst { it.first == activeAdminTab }.coerceAtLeast(0),
                containerColor = DarkBackground,
                contentColor = primaryColor
            ) {
                adminTabs.forEach { entry ->
                    Tab(
                        selected = activeAdminTab == entry.first,
                        onClick = { activeAdminTab = entry.first },
                        text = { Text(entry.second, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Tab Contents
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)
            ) {
                when (activeAdminTab) {
                    0 -> {
                        // Categories and Quick listing configurations
                        Column(modifier = Modifier.fillMaxSize()) {
                            Button(
                                onClick = onAddCategoryClick,
                                modifier = Modifier.fillMaxWidth().height(45.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isArabic) "إضافة قسم رئيسي جديد" else "Create New Sector", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isArabic) "القائمة الأساسية للأقسام (${categories.size})" else "Active Categories (${categories.size})",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categories) { cat ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(getIconForCategory(cat.icon), contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = if (isArabic) cat.name_ar else cat.name_en,
                                                        color = TextWhite,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text("Index: ${cat.order_index}", color = TextGray, fontSize = 11.sp)
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { onEditCategoryClick(cat) }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Category Section", tint = primaryColor, modifier = Modifier.size(20.dp))
                                                }
                                                IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Safe Super-admin only Supervisors section
                        if (!isSuperAdmin) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (isArabic) "عذراً، هذا القسم خاص بالمالك المسؤول فقط." else "Access Denied: Super Admin Only.", color = Color.Red)
                            }
                        } else {
                            // Supervisors / Advisors administration management
                            val adminList by viewModel.admins.collectAsStateWithLifecycle(initialValue = emptyList())
                            var newModUsername by remember { mutableStateOf("") }
                            var newModPassword by remember { mutableStateOf("") }
                            var newModRole by remember { mutableStateOf("admin") } // "admin", "moderator"

                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = if (isArabic) "إضافة مشرف أو مدير جديد للتطبيق" else "Register App Supervisor / Admin",
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newModUsername,
                                    onValueChange = { newModUsername = it },
                                    singleLine = true,
                                    label = { Text(if (isArabic) "اسم المستخدم الجديد" else "New Mod Username") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = newModPassword,
                                    onValueChange = { newModPassword = it },
                                    singleLine = true,
                                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "الدور الوظيفي للمشرف:" else "Mod Role Type:",
                                        color = TextWhite,
                                        fontSize = 12.sp
                                    )
                                    Row {
                                        Button(
                                            onClick = { newModRole = "admin" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (newModRole == "admin") primaryColor else CardBackground,
                                                contentColor = if (newModRole == "admin") Color.Black else TextWhite
                                            )
                                        ) {
                                            Text("Admin", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = { newModRole = "moderator" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (newModRole == "moderator") primaryColor else CardBackground,
                                                contentColor = if (newModRole == "moderator") Color.Black else TextWhite
                                            )
                                        ) {
                                            Text("Moderator", fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (newModUsername.isNotBlank() && newModPassword.isNotBlank()) {
                                            viewModel.addNewAdminWithPassword(newModUsername, newModPassword, newModRole, true)
                                            newModUsername = ""
                                            newModPassword = ""
                                            Toast.makeText(context, "تمت إضافة المشرف بنجاح!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "يرجى تعبئة كافة الحقول", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text(if (isArabic) "تسجيل المشرف بالبث الفوري" else "Save & Propagate Supervisor")
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isArabic) "المشرفين والمدراء الحاليين" else "All Registered Staff",
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(adminList) { adm ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = CardBackground)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(text = adm.username, color = TextWhite, fontWeight = FontWeight.Bold)
                                                    Text(text = "الدور: ${adm.role} — الحالة: ${if (adm.is_active) "نشط" else "معطل"}", color = TextGray, fontSize = 11.sp)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    TextButton(
                                                        onClick = {
                                                            val updated = adm.copy(is_active = !adm.is_active)
                                                            viewModel.updateAdminDetails(updated)
                                                        }
                                                    ) {
                                                        Text(if (adm.is_active) "تعطيل" else "تفعيل", color = primaryColor, fontSize = 11.sp)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteAdmin(adm.username)
                                                            Toast.makeText(context, "تم حذف المشرف بنجاح!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف المشرف", tint = Color.Red, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Settings customization: app custom dynamic naming, color theme changes, AI welcomes message configuration
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isArabic) "إعدادات الألوان والمظهر والترحيب والتسمية" else "Customize App Branding, Themes & AI Welcomes",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            // 1. External application outer name config
                            var appNameText by remember { mutableStateOf(appConfig.app_name) }
                            OutlinedTextField(
                                value = appNameText,
                                onValueChange = { appNameText = it },
                                label = { Text(if (isArabic) "اسم التطبيق الخارجي" else "Outer App Custom Name") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val updated = appConfig.copy(app_name = appNameText)
                                    viewModel.updateAppConfig(updated)
                                    Toast.makeText(context, "تم حفظ اسم التطبيق بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                            ) {
                                Text(if (isArabic) "حفظ اسم التطبيق" else "Save App Name")
                            }

                            Divider(color = TextGray.copy(alpha = 0.2f))

                            // 2. Dynamic top bar circular letters logo text config
                            var appLogoTextVal by remember { mutableStateOf(appConfig.app_logo_text) }
                            OutlinedTextField(
                                value = appLogoTextVal,
                                onValueChange = { appLogoTextVal = it },
                                label = { Text(if (isArabic) "حروف شعار التطبيق (مثل: دليلي / خد)" else "Circular Logo Text (e.g. DL)") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val updated = appConfig.copy(app_logo_text = appLogoTextVal)
                                    viewModel.updateAppConfig(updated)
                                    Toast.makeText(context, "تم حفظ حروف الشعار بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                            ) {
                                Text(if (isArabic) "حفظ حروف الشعار" else "Save Logo Text")
                            }

                            Divider(color = TextGray.copy(alpha = 0.2f))

                            // 3. Selection row for dynamic accent color themes (adding the requested light "classic" sky blue theme!)
                            Text(if (isArabic) "تغيير لغة التصميم واللون الرئيسي للتطبيق:" else "Choose App Visual Color Theme:", color = TextWhite, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val colorsList = listOf(
                                    Triple("gold", Color(0xFFFFD700), "ذهبي"),
                                    Triple("emerald", Color(0xFF00C9A7), "زمردي"),
                                    Triple("ocean", Color(0xFF008DFF), "محيطي"),
                                    Triple("purple", Color(0xFFBD00FF), "بنفسجي"),
                                    Triple("sunset", Color(0xFFFF5252), "غروبي"),
                                    Triple("classic", Color(0xFF0284C7), "كلاسيكي") // Light calming Sky Blue!
                                )
                                colorsList.forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(col.second)
                                            .clickable {
                                                val updated = appConfig.copy(app_theme = col.first)
                                                viewModel.updateAppConfig(updated)
                                                Toast.makeText(context, "تم تغيير اللون إلى ${col.third}!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (appConfig.app_theme.lowercase() == col.first) {
                                            Box(
                                                modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.Black)
                                            )
                                        }
                                    }
                                }
                            }

                            Divider(color = TextGray.copy(alpha = 0.2f))

                            // 4. Footer contact telephone override
                            var footerPhoneText by remember { mutableStateOf(appConfig.footer_phone) }
                            OutlinedTextField(
                                value = footerPhoneText,
                                onValueChange = { footerPhoneText = it },
                                label = { Text(if (isArabic) "تذييل نهاية الصفحة رقم الهاتف" else "Footer Number override") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val updated = appConfig.copy(footer_phone = footerPhoneText)
                                    viewModel.updateAppConfig(updated)
                                    Toast.makeText(context, "تم حفظ رقم التذييل بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                            ) {
                                Text(if (isArabic) "حفظ رقم التذييل" else "Save Footer Number")
                            }

                            Divider(color = TextGray.copy(alpha = 0.2f))

                            // 5. AI generated welcome note config & modification
                            Text(if (isArabic) "رسالة ترحيب الزوار بالدليل الرئيسي:" else "Home Page Visitor Welcome message Mode:", color = TextWhite, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isArabic) "استخدام وضع الذكاء الاصطناعي (أكثر جاذبية)" else "Use AI Greeting Mode", color = TextWhite, fontSize = 11.sp)
                                Switch(
                                    checked = appConfig.welcome_msg_mode.lowercase() == "ai",
                                    onCheckedChange = { ch ->
                                        val updated = appConfig.copy(welcome_msg_mode = if (ch) "ai" else "custom")
                                        viewModel.updateAppConfig(updated)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                                )
                            }

                            if (appConfig.welcome_msg_mode.lowercase() == "ai") {
                                Button(
                                    onClick = { viewModel.generateWelcomeGreetingFromAi() },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isArabic) "تحديث رسالة الترحيب من الذكاء الاصطناعي" else "Regenerate AI Greeting Note", fontWeight = FontWeight.Bold)
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "الرسالة الحالية: " + appConfig.custom_welcome_msg,
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                var customMessageText by remember { mutableStateOf(appConfig.custom_welcome_msg) }
                                OutlinedTextField(
                                    value = customMessageText,
                                    onValueChange = { customMessageText = it },
                                    label = { Text(if (isArabic) "رسالة ترحيب يدوية مخصصة" else "Custom Welcome Greeting Note") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        val updated = appConfig.copy(custom_welcome_msg = customMessageText)
                                        viewModel.updateAppConfig(updated)
                                        Toast.makeText(context, "تم حفظ الرسالة بنجاح!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                                ) {
                                    Text(if (isArabic) "حفظ الترحيب اليدوي" else "Save Custom Greeting")
                                }
                            }

                            Divider(color = TextGray.copy(alpha = 0.2f))

                            // 6. Toggle comments adding button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isArabic) "عرض زر إضافة التقييمات للزوار الجدد:" else "Show comment submission to visitors", color = TextWhite, fontSize = 12.sp)
                                Switch(
                                    checked = appConfig.show_reviews_enabled,
                                    onCheckedChange = { ch ->
                                        val updated = appConfig.copy(show_reviews_enabled = ch)
                                        viewModel.updateAppConfig(updated)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                                )
                            }

                            Divider(color = TextGray.copy(alpha = 0.2f))

                            // 7. Config sorted index choose choice
                            Text(if (isArabic) "أولويات ترتيب وتصفية القوائم والأسماء:" else "Global Lists sorting preference:", color = TextWhite, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val updated = appConfig.copy(list_sort_mode = "date")
                                        viewModel.updateAppConfig(updated)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (appConfig.list_sort_mode == "date") primaryColor else CardBackground,
                                        contentColor = if (appConfig.list_sort_mode == "date") Color.Black else TextWhite
                                    )
                                ) {
                                    Text(if (isArabic) "حسب تاريخ النشر" else "Publish Date", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        val updated = appConfig.copy(list_sort_mode = "name")
                                        viewModel.updateAppConfig(updated)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (appConfig.list_sort_mode == "name") primaryColor else CardBackground,
                                        contentColor = if (appConfig.list_sort_mode == "name") Color.Black else TextWhite
                                    )
                                ) {
                                    Text(if (isArabic) "حسب الاسم والترتيب الأبجدي" else "Alphabetical Name", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}aryColor else CardBackground,
                                        contentColor = if (appConfig.list_sort_mode == "date") Color.Black else TextWhite
                                    )
                                ) {
                                    Text(if (isArabic) "حسب تاريخ النشر" else "Publish Date", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        val updated = appConfig.copy(list_sort_mode = "name")
                                        viewModel.updateAppConfig(updated)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (appConfig.list_sort_mode == "name") primaryColor else CardBackground,
                                        contentColor = if (appConfig.list_sort_mode == "name") Color.Black else TextWhite
                                    )
                                ) {
                                    Text(if (isArabic) "حسب الاسم والترتيب الأبجدي" else "Alphabetical Name", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ======================== DIALOG BUILDERS SUPPORTING GALLERY IMAGE CAPTURING ========================

@Composable
fun AddCategoryDialog(
    isArabic: Boolean,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("work") }
    var order by remember { mutableStateOf("0") }
    var imgUrl by remember { mutableStateOf("") }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = uriToBase64(context, it)
            if (base64.isNotBlank()) {
                imgUrl = base64
                Toast.makeText(context, "تم رفع الصورة السرية بنجاح!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, primaryColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "إضافة قسم جديد" else "Create New Category",
                    color = primaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "الاسم بالعربية" else "Arabic Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "الاسم بالإنجليزية" else "English Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = iconKey,
                    onValueChange = { iconKey = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "مفتاح الأيقونة (مثال: medical, taxi)" else "Icon Key (e.g. medical, taxi)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "ترتيب العرض (رقم)" else "Order Index (Number)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Image chooser picker toggle from phone gallery storage or internet URL options
                Text(if (isArabic) "صورة القسم الرئيسي:" else "Category Section Image source:", color = TextWhite, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "تحميل من الهاتف" else "From Device", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = imgUrl,
                    onValueChange = { imgUrl = it },
                    label = { Text(if (isArabic) "رابط الصورة أو كود البيانات" else "Or Image online URL / raw code") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )

                if (imgUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "Preview",
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(DarkBackground),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextWhite)) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nameAr.isNotBlank() && nameEn.isNotBlank()) {
                                onConfirm(nameAr, nameEn, iconKey, order.toIntOrNull() ?: 0, imgUrl)
                            } else {
                                Toast.makeText(context, "الرجاء كتمال تعبئة البيانات الأساسية", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "حفظ وإضافة" else "Create")
                    }
                }
            }
        }
    }
}

@Composable
fun EditCategoryDialog(
    category: Category,
    isArabic: Boolean,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var nameAr by remember { mutableStateOf(category.name_ar) }
    var nameEn by remember { mutableStateOf(category.name_en) }
    var iconKey by remember { mutableStateOf(category.icon) }
    var order by remember { mutableStateOf(category.order.toString()) }
    var imgUrl by remember { mutableStateOf(category.image_url ?: "") }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = uriToBase64(context, it)
            if (base64.isNotBlank()) {
                imgUrl = base64
                Toast.makeText(context, "تم رفع وتعديل الصورة بنجاح!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, primaryColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "تعديل القسم / الخدمة" else "Edit Service Category",
                    color = primaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "الاسم بالعربية" else "Arabic Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "الاسم بالإنجليزية" else "English Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = iconKey,
                    onValueChange = { iconKey = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "مفتاح الأيقونة (مثال: medical, taxi)" else "Icon Key (e.g. medical, taxi)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "ترتيب العرض (رقم)" else "Order Index (Number)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(if (isArabic) "تعديل صورة القسم:" else "Edit Category Image source:", color = TextWhite, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "تحميل من الهاتف" else "From Device", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = imgUrl,
                    onValueChange = { imgUrl = it },
                    label = { Text(if (isArabic) "رابط الصورة أو كود البيانات" else "Or Image online URL / raw code") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )

                if (imgUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "Preview",
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(DarkBackground),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextWhite)) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nameAr.isNotBlank() && nameEn.isNotBlank()) {
                                onConfirm(
                                    category.copy(
                                        name_ar = nameAr,
                                        name_en = nameEn,
                                        icon = iconKey,
                                        order = order.toIntOrNull() ?: 0,
                                        image_url = imgUrl
                                    )
                                )
                            } else {
                                Toast.makeText(context, "الرجاء اكمال تعبئة البيانات الأساسية", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "حفظ التعديلات" else "Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun AddProviderDialog(
    isArabic: Boolean,
    categoryId: String,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, Boolean) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imgUrl by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = uriToBase64(context, it)
            if (base64.isNotBlank()) {
                imgUrl = base64
                Toast.makeText(context, "تم رفع الصورة بنجاح!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, primaryColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "إضافة مقدم خدمة جديد" else "Add Service Provider",
                    color = primaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "الاسم (عربي)" else "Name (Arabic)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "الاسم (إنجليزي)" else "Name (English)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text(if (isArabic) "رقم الهاتف" else "Phone Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Custom toggle switch option to Pin provider to the top of listings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isArabic) "تثبيت في رأس القائمة:" else "Pin provider to top:", color = TextWhite, fontSize = 12.sp)
                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Photo load gallery buttons
                Text(if (isArabic) "الصورة الشخصية لمزود المهنة:" else "Professional Profile Picture source:", color = TextWhite, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "اختيار من استوديو الموبايل" else "Select gallery photo", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = imgUrl,
                    onValueChange = { imgUrl = it },
                    label = { Text(if (isArabic) "أو أدخل رابطاً مباشراً من الإنترنت" else "Or enter web image url link") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )

                if (imgUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "Preview",
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(DarkBackground),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextWhite)) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nameAr.isNotBlank() && nameEn.isNotBlank() && phone.isNotBlank()) {
                                onConfirm(nameAr, nameEn, phone, imgUrl.ifBlank { null }, isPinned)
                            } else {
                                Toast.makeText(context, "الرجاء وإدخال كافة المعلومات المطلوبة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "حفظ وتثبيت" else "Save & Broadcast")
                    }
                }
            }
        }
    }
}

@Composable
fun AddReviewDialog(
    isArabic: Boolean,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, primaryColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "قيم الخدمة واترك رأيك الفني" else "Evaluate performance & leave comment",
                    color = primaryColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(if (isArabic) "اسمك" else "Your Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(if (isArabic) "كتب رأيك وتقييمك" else "Comment Details") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = (if (isArabic) "الدرجة المستحقة: " else "Rating Score: ") + "${rating.toInt()} / 5",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = TextGray.copy(alpha = 0.3f),
                        thumbColor = primaryColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextWhite)) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (comment.isNotBlank()) {
                                onConfirm(name, comment, rating)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "نشر التقييم" else "Submit Rating")
                    }
                }
            }
        }
    }
}
