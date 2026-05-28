@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import android.content.Intent
import java.util.Locale
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Category
import com.example.data.ServiceProvider
import com.example.data.Review

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(viewModel: AppViewModel) {
    var currentScreen by remember { mutableStateOf("home") }
    var selectedProviderForReviews by remember { mutableStateOf<ServiceProvider?>(null) }
    
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & Backdoor Tap Trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { viewModel.handleLogoTap() }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFD700), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "خدمات",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.language == "ar") "دليلي" else "Dalili",
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Language Switcher
                    Button(
                        onClick = { viewModel.toggleLanguage() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFFFD700)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFFD700)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (viewModel.language == "ar") "English" else "العربية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    // Sync Button
                    IconButton(
                        onClick = { viewModel.syncData(force = true) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color(0xFFFFD700)
                        )
                    }

                    // Admin Area Button
                    IconButton(
                        onClick = {
                            if (viewModel.isAdminLoggedIn) {
                                currentScreen = "admin_dashboard"
                            } else {
                                currentScreen = "admin_login"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (viewModel.isAdminLoggedIn) Icons.Default.Settings else Icons.Default.Lock,
                            contentDescription = "Admin Drawer",
                            tint = Color(0xFFFFD700)
                        )
                    }
                }
            }

            // Main Window Space Classes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentScreen) {
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        onCategorySelect = { catId ->
                            viewModel.selectedCategoryId = catId
                            currentScreen = "providers"
                        }
                    )
                    "providers" -> ProvidersScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "home" },
                        onShowReviews = { provider ->
                            selectedProviderForReviews = provider
                            viewModel.loadReviewsForProvider(provider.id)
                            currentScreen = "reviews"
                        }
                    )
                    "reviews" -> ReviewSectionScreen(
                        viewModel = viewModel,
                        provider = selectedProviderForReviews,
                        onBack = { currentScreen = "providers" }
                    )
                    "admin_login" -> AdminLoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            currentScreen = "admin_dashboard"
                        },
                        onBack = { currentScreen = "home" }
                    )
                    "admin_dashboard" -> {
                        if (viewModel.isAdminLoggedIn) {
                            AdminDashboard(
                                viewModel = viewModel,
                                onLogout = {
                                    viewModel.logoutAdmin()
                                    currentScreen = "home"
                                }
                            )
                        } else {
                            currentScreen = "admin_login"
                        }
                    }
                }
            }

            // Consistent Branded Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MAW 777644670",
                    color = Color(0xFFFFD700),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        // Backdoor Prompt Unlocked Dialog
        if (viewModel.isBackdoorUnlocked) {
            BackdoorAccessDialog(
                onDismiss = { viewModel.resetBackdoor() },
                onUnlock = {
                    viewModel.resetBackdoor()
                    currentScreen = "admin_login"
                },
                context = context
            )
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onCategorySelect: (String) -> Unit
) {
    val categoriesList by viewModel.categories.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredCategories = categoriesList.filter {
        val target = if (viewModel.language == "ar") it.nameAr else it.nameEn
        target.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (viewModel.language == "ar") "ابحث عن الأقسام والخدمات..." else "Search departments & services...",
                    color = Color.Gray
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFFFFD700)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("search_bar"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Banner Alert for Offline / Sync Status
        if (viewModel.isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.language == "ar") "مزامنة البيانات الحية..." else "Syncing real-time Firestore data...",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section Title
        Text(
            text = if (viewModel.language == "ar") "الأقسام المتاحة" else "Available Categories",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (viewModel.language == "ar") "لا يوجد نتائج مطابقة" else "No matching results found",
                    color = Color.LightGray
                )
            }
        } else {
            // Creative Grid Layout for categories
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredCategories) { category ->
                    CategoryCard(
                        category = category,
                        language = viewModel.language,
                        onClick = { onCategorySelect(category.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    language: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
            .testTag("category_card_${category.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image Backdrop representation
            if (!category.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = category.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.35f
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Vector-like generic badge icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFD700).copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, Color(0xFFFFD700), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (category.icon) {
                            "home_repair_service" -> Icons.Default.Home
                            "laptop_mac" -> Icons.Default.Settings
                            "school" -> Icons.Default.List
                            "brush" -> Icons.Default.Star
                            "directions_car" -> Icons.Default.List
                            "cleaning_services" -> Icons.Default.Home
                            "local_shipping" -> Icons.Default.List
                            "work" -> Icons.Default.Person
                            "local_taxi" -> Icons.Default.List
                            "delivery_dining" -> Icons.Default.List
                            "car_rental" -> Icons.Default.List
                            "hotel" -> Icons.Default.Home
                            else -> Icons.Default.Home
                        },
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = if (language == "ar") category.nameAr else category.nameEn,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProvidersScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onShowReviews: (ServiceProvider) -> Unit
) {
    val providersList by viewModel.serviceProviders.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    
    val selectedCategory = categoriesList.find { it.id == viewModel.selectedCategoryId }
    val filteredProviders = providersList.filter { it.categoryId == viewModel.selectedCategoryId }
    
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Back Button & Screen Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color(0xFF1E1E1E), CircleShape)
                    .border(1.dp, Color(0xFFFFD700), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFFFD700)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = selectedCategory?.let { if (viewModel.language == "ar") it.nameAr else it.nameEn } ?: "",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredProviders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (viewModel.language == "ar") "لا يوجد مقدمي خدمات للأمور المصنفة حالياً" else "No service providers found for this category",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProviders) { provider ->
                    ProviderListCard(
                        provider = provider,
                        language = viewModel.language,
                        onCall = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot initiate call", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReviews = { onShowReviews(provider) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderListCard(
    provider: ServiceProvider,
    language: String,
    onCall: () -> Unit,
    onReviews: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
            .testTag("provider_card_${provider.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image or Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!provider.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = provider.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == "ar") provider.nameAr else provider.nameEn,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = provider.phone,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Star Rating Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", provider.rating),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == "ar") "اتصال" else "Call",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onReviews,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E1E),
                        contentColor = Color(0xFFFFD700)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFFD700)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Reviews"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == "ar") "التقييمات" else "Reviews",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSectionScreen(
    viewModel: AppViewModel,
    provider: ServiceProvider?,
    onBack: () -> Unit
) {
    val reviews by viewModel.currentProviderReviews.collectAsState()
    
    var showAddReviewDialog by remember { mutableStateOf(false) }
    var reviewerName by remember { mutableStateOf("") }
    var reviewComment by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color(0xFF1E1E1E), CircleShape)
                        .border(1.dp, Color(0xFFFFD700), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFFFD700)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = provider?.let { if (viewModel.language == "ar") it.nameAr else it.nameEn } ?: "",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (viewModel.language == "ar") "مراجعات العملاء" else "Customer Reviews",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            Button(
                onClick = { showAddReviewDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add review")
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = if (viewModel.language == "ar") "تقييم" else "Rate", fontWeight = FontWeight.Bold)
            }
        }

        if (reviews.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (viewModel.language == "ar") "لا توجد مراجعات حتى الآن. كن أول من يقيم!" else "No reviews yet. Be the first to rate!",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reviews) { review ->
                    ReviewRowCard(review = review)
                }
            }
        }
    }

    if (showAddReviewDialog && provider != null) {
        Dialog(onDismissRequest = { showAddReviewDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                border = BorderStroke(1.dp, Color(0xFFFFD700)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (viewModel.language == "ar") "أضف تقييمك لمقدم الخدمة" else "Add your review",
                        color = Color(0xFFFFD700),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = reviewerName,
                        onValueChange = { reviewerName = it },
                        label = { Text(text = if (viewModel.language == "ar") "اسمك الكريم" else "Your Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text(text = if (viewModel.language == "ar") "اكتب مراجعتك..." else "Comment...") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = Color(0xFF1E1E1E),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Rating Slider selection
                    Text(
                        text = "${if (viewModel.language == "ar") "النجوم:" else "Stars:"} ${selectedRating.toInt()}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = selectedRating,
                        onValueChange = { selectedRating = it },
                        valueRange = 1f..5f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700)
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showAddReviewDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White)
                        ) {
                            Text(text = if (viewModel.language == "ar") "إلغاء" else "Cancel")
                        }

                        Button(
                            onClick = {
                                if (reviewerName.isNotBlank() && reviewComment.isNotBlank()) {
                                    viewModel.addReview(
                                        providerId = provider.id,
                                        userName = reviewerName,
                                        comment = reviewComment,
                                        rating = selectedRating
                                    )
                                    showAddReviewDialog = false
                                    reviewerName = ""
                                    reviewComment = ""
                                    selectedRating = 5f
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                        ) {
                            Text(text = if (viewModel.language == "ar") "إرسال" else "Send", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewRowCard(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    color = Color(0xFFFFD700),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    repeat(review.rating.toInt()) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = review.comment,
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun AdminLoginScreen(
    viewModel: AppViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var userVal by remember { mutableStateOf("") }
    var passVal by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFFFD700), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (viewModel.language == "ar") "دخول المشرفين والمحافظين" else "Admin Access Gate",
            color = Color(0xFFFFD700),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = userVal,
            onValueChange = { userVal = it },
            label = { Text(text = if (viewModel.language == "ar") "اسم المستخدم" else "Username") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = passVal,
            onValueChange = { passVal = it },
            label = { Text(text = if (viewModel.language == "ar") "كلمة المرور" else "Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFFD700),
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E1E1E),
                    contentColor = Color.White
                )
            ) {
                Text(text = if (viewModel.language == "ar") "رجوع" else "Cancel")
            }

            Button(
                onClick = {
                    if (viewModel.attemptAdminLogin(userVal, passVal)) {
                        Toast.makeText(context, "Welcome authorized administrator", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, "Unauthorized Credentials!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                Text(text = if (viewModel.language == "ar") "دخول" else "Login", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminDashboard(
    viewModel: AppViewModel,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf("categories") }
    
    val categories by viewModel.categories.collectAsState()
    val providers by viewModel.serviceProviders.collectAsState()

    var showAddCategory by remember { mutableStateOf(false) }
    var showAddProvider by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (viewModel.language == "ar") "لوحة التحكم الرئيسية" else "Control Panel Dashboard",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Role: ${viewModel.adminUser?.role ?: "Admin"}",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .background(Color(0xFF1E1E1E), CircleShape)
                    .border(1.dp, Color(0xFFFFD700), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Log out",
                    tint = Color(0xFFFFD700)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { currentTab = "categories" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == "categories") Color(0xFFFFD700) else Color(0xFF1E1E1E),
                    contentColor = if (currentTab == "categories") Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = if (viewModel.language == "ar") "الأقسام" else "Categories", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { currentTab = "providers" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentTab == "providers") Color(0xFFFFD700) else Color(0xFF1E1E1E),
                    contentColor = if (currentTab == "providers") Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = if (viewModel.language == "ar") "مقدمي الخدمات" else "Providers", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Box(modifier = Modifier.weight(1f)) {
            if (currentTab == "categories") {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { showAddCategory = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (viewModel.language == "ar") "إضافة قسم جديد" else "Create Category", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categories) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (viewModel.language == "ar") category.nameAr else category.nameEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )

                                IconButton(onClick = { viewModel.removeCategory(category.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { showAddProvider = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (viewModel.language == "ar") "إضافة مقدم خدمة جديد" else "Create Provider", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(providers) { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (viewModel.language == "ar") provider.nameAr else provider.nameEn,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = provider.phone,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(onClick = { viewModel.removeServiceProvider(provider.id) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showAddCategory) {
        AddCategoryDialog(
            viewModel = viewModel,
            onDismiss = { showAddCategory = false }
        )
    }

    if (showAddProvider) {
        AddProviderDialog(
            viewModel = viewModel,
            categories = categories,
            onDismiss = { showAddProvider = false }
        )
    }
}

@Composable
fun AddCategoryDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    var id by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("home_repair_service") }
    var order by remember { mutableStateOf("0") }
    var imgUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            border = BorderStroke(1.dp, Color(0xFFFFD700)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (viewModel.language == "ar") "إضافة قسم جديد" else "Create Category",
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("ID (e.g. cat_99)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFD700))
                )
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("الاسم بالعربية") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFD700))
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Name in English") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFD700))
                )
                OutlinedTextField(
                    value = imgUrl,
                    onValueChange = { imgUrl = it },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFD700))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (nameAr.isNotBlank()) {
                                viewModel.addCategory(id, nameAr, nameEn, icon, order.toIntOrNull() ?: 0, imgUrl.ifBlank { null })
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddProviderDialog(
    viewModel: AppViewModel,
    categories: List<Category>,
    onDismiss: () -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var imgUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            border = BorderStroke(1.dp, Color(0xFFFFD700)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (viewModel.language == "ar") "إضافة مقدم خدمة جديد" else "Create Service Provider",
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("الاسم بالعربية") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFD700))
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Name in English") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFD700))
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (مثل: 777123456)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFD700))
                )
                OutlinedTextField(
                    value = imgUrl,
                    onValueChange = { imgUrl = it },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFFFD700))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Category / القسم:", color = Color.Gray, fontSize = 12.sp)
                // Dropdown mock selection for safety
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            label = { Text(if (viewModel.language == "ar") cat.nameAr else cat.nameEn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD700),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (nameAr.isNotBlank() && phone.isNotBlank() && selectedCatId.isNotBlank()) {
                                viewModel.addServiceProvider(nameAr, nameEn, phone, selectedCatId, imgUrl.ifBlank { null })
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) {
                        Text("حفظ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BackdoorAccessDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    context: android.content.Context
) {
    var passwordInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            border = BorderStroke(1.dp, Color(0xFFFFD700)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "بوابة الوصول السرية للمشرف",
                    color = Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "أدخل الرمز السري للانتقال إلى بوابة الدخول:",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFD700),
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White)
                    ) {
                        Text(text = "إلغاء")
                    }

                    Button(
                        onClick = {
                            if (passwordInput == "dalili2024") {
                                Toast.makeText(context, "Backdoor code matched!", Toast.LENGTH_SHORT).show()
                                onUnlock()
                            } else {
                                Toast.makeText(context, "Wrong Code!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) {
                        Text(text = "تدقيق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
