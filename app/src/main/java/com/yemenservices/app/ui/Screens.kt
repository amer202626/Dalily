@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.yemenservices.app.ui

import java.util.Locale
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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

// Color Palette (Premium Dark Slate & Golden Accents)
val DarkBackground = Color(0xFF121212)
val CardBackground = Color(0xFF1E1E1E)
val PrimaryGold = Color(0xFFFFD700)
val AccentGold = Color(0xFFE5C100)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFFB0B0B0)

@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var isArabic by remember { mutableStateOf(true) }

    // Stream state directly from Firestore snapshot listeners
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val providers by viewModel.serviceProviders.collectAsStateWithLifecycle(initialValue = emptyList())
    val reviews by viewModel.reviews.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedProviderId by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf("home") } // "home", "providers", "provider-detail", "admin"
    var searchQuery by remember { mutableStateOf("") }

    // Dialog flags
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
                        Text(
                            text = if (isArabic) "دليلي للخدمات" else "Dalili Services",
                            color = PrimaryGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Language switcher
                            Button(
                                onClick = { isArabic = !isArabic },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = PrimaryGold
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Lang", modifier = Modifier.size(20.dp), tint = PrimaryGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "EN" else "عربي", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = PrimaryGold
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
                            Icon(
                                imageVector = if (isArabic) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryGold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkBackground,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == "home" || currentScreen == "providers" || currentScreen == "provider-detail",
                    onClick = {
                        currentScreen = "home"
                        selectedCategoryId = null
                        selectedProviderId = null
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(if (isArabic) "الرئيسية" else "Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBackground,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = PrimaryGold,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == "admin",
                    onClick = {
                        currentScreen = "admin"
                    },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Admin") },
                    label = { Text(if (isArabic) "لوحة التحكم" else "Admin") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBackground,
                        selectedTextColor = PrimaryGold,
                        indicatorColor = PrimaryGold,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    )
                )
            }
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
                        isArabic = isArabic,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onCategorySelect = { catId ->
                            selectedCategoryId = catId
                            currentScreen = "providers"
                        }
                    )
                }
                "providers" -> {
                    val category = categories.firstOrNull { it.id == selectedCategoryId }
                    val filteredProviders = providers.filter { 
                        it.category_id == selectedCategoryId && 
                        (it.name_ar.contains(searchQuery, ignoreCase = true) || it.name_en.contains(searchQuery, ignoreCase = true))
                    }
                    ProvidersScreen(
                        category = category,
                        providers = filteredProviders,
                        isArabic = isArabic,
                        onProviderSelect = { provId ->
                            selectedProviderId = provId
                            currentScreen = "provider-detail"
                        },
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        currentAdmin = currentAdmin,
                        onAddProviderClick = { showAddProviderDialog = true },
                        onDeleteProvider = { id -> viewModel.deleteServiceProvider(id) }
                    )
                }
                "provider-detail" -> {
                    val provider = providers.firstOrNull { it.id == selectedProviderId }
                    val providerReviews = reviews.filter { it.provider_id == selectedProviderId }
                    ProviderDetailScreen(
                        provider = provider,
                        reviews = providerReviews,
                        isArabic = isArabic,
                        onAddReviewClick = { showAddReviewDialog = true },
                        onDeleteReview = { reviewId -> viewModel.deleteReview(reviewId, selectedProviderId ?: "") },
                        currentAdmin = currentAdmin
                    )
                }
                "admin" -> {
                    AdminScreen(
                        viewModel = viewModel,
                        isArabic = isArabic,
                        categories = categories,
                        providers = providers,
                        onAddCategoryClick = { showAddCategoryDialog = true },
                        onDeleteCategory = { id -> viewModel.deleteCategory(id) }
                    )
                }
            }

            // Real-time Dialog integrations
            if (showAddCategoryDialog) {
                AddCategoryDialog(
                    isArabic = isArabic,
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
                    onDismiss = { showAddProviderDialog = false },
                    onConfirm = { nameAr, nameEn, phone, imgUrl ->
                        viewModel.addServiceProvider("", nameAr, nameEn, phone, selectedCategoryId ?: "", imgUrl)
                        showAddProviderDialog = false
                    }
                )
            }

            if (showAddReviewDialog) {
                AddReviewDialog(
                    isArabic = isArabic,
                    onDismiss = { showAddReviewDialog = false },
                    onConfirm = { name, comment, rating ->
                        viewModel.addReview(selectedProviderId ?: "", name, comment, rating)
                        showAddReviewDialog = false
                    }
                )
            }
        }
    }
}

// ======================== COMMONS ========================

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = TextGray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryGold) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PrimaryGold)
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedBorderColor = PrimaryGold,
            unfocusedBorderColor = TextGray,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

// Convert icon key to appropriate ImageVector
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

// ======================== DETAILED SCREENS ========================

@Composable
fun HomeScreen(
    categories: List<Category>,
    isArabic: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit
) {
    val filteredCategories = categories.filter {
        it.name_ar.contains(searchQuery, ignoreCase = true) || it.name_en.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome Header with premium layout
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "مرحباً بك في دليلي" else "Welcome to Dalili",
                    color = PrimaryGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "دليلك السريع للوصول لجميع الخدمات في اليمن فوراً" else "Your instant directory for all services in Yemen",
                    color = TextWhite,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = if (isArabic) "ابحث عن قسم أو خدمة..." else "Search for categories or services..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isArabic) "الأقسام والخدمات المتوفرة" else "Available Categories",
            color = PrimaryGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "لا توجد نتائج مطابقة لتعبك." else "No categories found.",
                    color = TextGray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .testTag("categories_grid"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCategories, key = { it.id }) { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.1f)
                            .clickable { onCategorySelect(cat.id) }
                            .testTag("category_${cat.id}"),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(0.5.dp, TextGray.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (!cat.image_url.isNullOrBlank()) {
                                AsyncImage(
                                    model = cat.image_url,
                                    contentDescription = cat.name_en,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(PrimaryGold.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForCategory(cat.icon),
                                        contentDescription = null,
                                        tint = PrimaryGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isArabic) cat.name_ar else cat.name_en,
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
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
    isArabic: Boolean,
    onProviderSelect: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    currentAdmin: Admin?,
    onAddProviderClick: () -> Unit,
    onDeleteProvider: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isArabic) (category?.name_ar ?: "مزودي الخدمة") else (category?.name_en ?: "Service Providers"),
                    color = PrimaryGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isArabic) "اختر مقدم خدمة للتواصل معه ورؤية التقييمات" else "Select a service provider to call and view rating",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
            if (currentAdmin != null) {
                IconButton(
                    onClick = onAddProviderClick,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Provider")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            placeholder = if (isArabic) "ابحث عن مقدم الخدمة بالاسم..." else "Search provider by name..."
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (providers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "لا توجد خدمات مضافة حالياً في هذا القسم." else "No service providers added under this category.",
                    color = TextGray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("providers_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(providers, key = { it.id }) { provider ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProviderSelect(provider.id) },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        border = BorderStroke(0.5.dp, TextGray.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!provider.image_url.isNullOrBlank()) {
                                AsyncImage(
                                    model = provider.image_url,
                                    contentDescription = provider.name_en,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryGold.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PrimaryGold,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) provider.name_ar else provider.name_en,
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = PrimaryGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.1f", provider.rating),
                                        color = TextWhite,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Call Trigger (Real Intent)
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${provider.phone}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryGold.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = PrimaryGold)
                                }

                                if (currentAdmin != null) {
                                    IconButton(
                                        onClick = { onDeleteProvider(provider.id) },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Red.copy(alpha = 0.15f))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
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
fun ProviderDetailScreen(
    provider: ServiceProvider?,
    reviews: List<Review>,
    isArabic: Boolean,
    onAddReviewClick: () -> Unit,
    onDeleteReview: (String) -> Unit,
    currentAdmin: Admin?
) {
    val context = LocalContext.current
    if (provider == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper card: Provider Profile Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!provider.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = provider.image_url,
                        contentDescription = provider.name_en,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(50.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(PrimaryGold.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isArabic) provider.name_ar else provider.name_en,
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = PrimaryGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", provider.rating),
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "(${reviews.size} ${if (isArabic) "تقييم" else "reviews"})",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Call Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${provider.phone}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Phone")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (if (isArabic) "اتصال الآن: " else "Call Now: ") + provider.phone,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reviews section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "تقييمات وآراء العملاء" else "Customer Reviews",
                color = PrimaryGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddReviewClick,
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground, contentColor = PrimaryGold),
                border = BorderStroke(0.5.dp, PrimaryGold)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isArabic) "أضف تقييمك" else "Add Review", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (reviews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "لا توجد تقييمات بعد لمقدم الخدمة هذا. كن أول من يقيم!" else "No reviews yet. Be the first to evaluate!",
                    color = TextGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reviews) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
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
                                    text = r.user_name.ifBlank { if (isArabic) "رقم/عميل مجهول" else "Anonymous" },
                                    color = PrimaryGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryGold.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(r.rating.toString(), color = TextWhite, fontSize = 12.sp)
                                        }
                                    }
                                    if (currentAdmin != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { onDeleteReview(r.id) }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = r.comment,
                                color = TextWhite,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================== ADMIN PORTAL ========================

@Composable
fun AdminScreen(
    viewModel: AppViewModel,
    isArabic: Boolean,
    categories: List<Category>,
    providers: List<ServiceProvider>,
    onAddCategoryClick: () -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    val errorMsg by viewModel.loginError.collectAsStateWithLifecycle()

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    if (currentAdmin == null) {
        // Elegant Login Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Login",
                        tint = PrimaryGold,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "تسجيل الدخول للإدارة" else "Admin Authorization Login",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text(if (isArabic) "اسم المستخدم" else "Username") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMsg ?: "",
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.login(usernameInput, passwordInput) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "دخول" else "Login", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Admin Dashboard with realtime items configuration
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isArabic) "مرحباً: ${currentAdmin?.username}" else "Welcome: ${currentAdmin?.username}",
                        color = PrimaryGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) "تعديلاتك تتزامن فوراً مع جميع الأجهزة!" else "Your updates sync instantly with all devices!",
                        color = Color.Green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red)
                ) {
                    Text(if (isArabic) "خروج" else "Logout")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick add category trigger
            Button(
                onClick = onAddCategoryClick,
                modifier = Modifier.fillMaxWidth().height(45.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isArabic) "إضافة قسم رئيسي جديد" else "Add New Category", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isArabic) "إدارة الأقسام الحالية: (${categories.size})" else "Manage Current Categories: (${categories.size})",
                color = PrimaryGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(PrimaryGold.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForCategory(cat.icon),
                                        contentDescription = null,
                                        tint = PrimaryGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) cat.name_ar else cat.name_en,
                                        color = TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Index: ${cat.order_index}",
                                        color = TextGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== MODAL REAL-TIME ADDITION DIALOGS ========================

@Composable
fun AddCategoryDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Int, String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("work") }
    var order by remember { mutableStateOf("0") }
    var imgUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PrimaryGold)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "إضافة قسم جديد" else "Create New Category",
                    color = PrimaryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isArabic) "الاسم بالعربية" else "Arabic Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isArabic) "الاسم بالإنجليزية" else "English Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = iconKey,
                    onValueChange = { iconKey = it },
                    label = { Text(if (isArabic) "مفتاح الأيقونة (مثال: medical, taxi)" else "Icon Key (e.g. medical, taxi)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it },
                    label = { Text(if (isArabic) "ترتيب العرض (رقم)" else "Order Index (Number)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = imgUrl,
                    onValueChange = { imgUrl = it },
                    label = { Text(if (isArabic) "رابط الصورة (اختياري)" else "Image URL (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )

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
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "حفظ وإضافة" else "Save & Broadcast")
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
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imgUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PrimaryGold)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "إضافة مقدم خدمة" else "Add Service Provider",
                    color = PrimaryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isArabic) "اسم مقدم الخدمة (عربي)" else "Provider Name (Arabic)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isArabic) "اسم مقدم الخدمة (إنجليزي)" else "Provider Name (English)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text(if (isArabic) "رقم الهاتف" else "Phone Number") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = imgUrl,
                    onValueChange = { imgUrl = it },
                    label = { Text(if (isArabic) "رابط الصورة الشخصية (اختياري)" else "Profile Image URL (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )

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
                                onConfirm(nameAr, nameEn, phone, imgUrl.ifBlank { null })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "حفظ" else "Save & Broadcast")
                    }
                }
            }
        }
    }
}

@Composable
fun AddReviewDialog(
    isArabic: Boolean,
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
            border = BorderStroke(1.dp, PrimaryGold)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) "قيم الخدمة واترك رأيك" else "Rate service & leave comment",
                    color = PrimaryGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isArabic) "اسمك" else "Your Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(if (isArabic) "التعليق والتقييم" else "Your Comment") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, focusedBorderColor = PrimaryGold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Elegant Rating Star Slider/Selector
                Text(
                    text = (if (isArabic) "التقييم: " else "Rating: ") + "${rating.toInt()} / 5",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        activeTrackColor = PrimaryGold,
                        inactiveTrackColor = TextGray,
                        thumbColor = PrimaryGold
                    )
                )

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
                            if (comment.isNotBlank()) {
                                onConfirm(name, comment, rating)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                    ) {
                        Text(if (isArabic) "نشر التقييم" else "Submit Rating")
                    }
                }
            }
        }
    }
}
