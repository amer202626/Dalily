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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yemenservices.app.data.*
import kotlinx.coroutines.launch

// Screen Enumeration
enum class ScreenRoute {
    HOME,
    JOIN_FORM,
    ADMIN_LOGIN,
    ADMIN_DASHBOARD,
    SERVICE_DETAILS
}

@Composable
fun MainNavigationSystem(viewModel: AppViewModel) {
    var currentScreen by remember { mutableStateOf(ScreenRoute.HOME) }
    var selectedServiceForDetails by remember { mutableStateOf<YemenService?>(null) }
    val isAr by viewModel.isArabic.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                ScreenRoute.HOME -> HomeScreen(
                    viewModel = viewModel,
                    isAr = isAr,
                    onNavigateToJoin = { currentScreen = ScreenRoute.JOIN_FORM },
                    onNavigateToAdmin = {
                        val auth = viewModel.authenticatedSupervisor.value
                        currentScreen = if (auth != null) ScreenRoute.ADMIN_DASHBOARD else ScreenRoute.ADMIN_LOGIN
                    },
                    onSelectService = { service ->
                        selectedServiceForDetails = service
                        viewModel.loadComments(service.id)
                        currentScreen = ScreenRoute.SERVICE_DETAILS
                    }
                )
                ScreenRoute.JOIN_FORM -> ProviderJoinScreen(
                    viewModel = viewModel,
                    isAr = isAr,
                    onBack = { currentScreen = ScreenRoute.HOME }
                )
                ScreenRoute.ADMIN_LOGIN -> AdminLoginScreen(
                    viewModel = viewModel,
                    isAr = isAr,
                    onLoginSuccess = { currentScreen = ScreenRoute.ADMIN_DASHBOARD },
                    onBack = { currentScreen = ScreenRoute.HOME }
                )
                ScreenRoute.ADMIN_DASHBOARD -> AdminDashboardScreen(
                    viewModel = viewModel,
                    isAr = isAr,
                    onBackToHome = { currentScreen = ScreenRoute.HOME }
                )
                ScreenRoute.SERVICE_DETAILS -> ServiceDetailsScreen(
                    service = selectedServiceForDetails ?: YemenService(),
                    viewModel = viewModel,
                    isAr = isAr,
                    onBack = { currentScreen = ScreenRoute.HOME }
                )
            }
        }

        // Global Overlay Floating Smart AI Assistant (Only shown if enabled and position is set to FLOATING)
        val appConfig by viewModel.appConfig.collectAsState()
        var showChatDialog by remember { mutableStateOf(false) }

        if (appConfig.showAiAssistant && appConfig.aiAssistantPosition == "FLOATING") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp, end = 16.dp, start = 16.dp),
                contentAlignment = if (isAr) Alignment.BottomStart else Alignment.BottomEnd
            ) {
                FloatingAiAssistantBubble(
                    config = appConfig,
                    onClick = { showChatDialog = true }
                )
            }
        }

        if (showChatDialog) {
            AiAssistantChatDialog(
                viewModel = viewModel,
                isAr = isAr,
                onDismiss = { showChatDialog = false }
            )
        }
    }
}

// ==================== WIDGETS & SUB-COMPONENTS ====================

@Composable
fun FloatingAiAssistantBubble(config: AppConfig, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(config.aiAssistantSizeDp.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE94560), Color(0xFF0F3460))
                )
            )
            .clickable { onClick() }
            .testTag("ai_assistant_floating"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = config.aiAssistantIcon.ifEmpty { "💬" },
                fontSize = (config.aiAssistantSizeDp / 2.2).sp
            )
        }
    }
}

@Composable
fun ServiceRatingRow(rating: Float, isAr: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        (1..5).forEach { starIdx ->
            val fillFraction = (rating - (starIdx - 1)).coerceIn(0f, 1f)
            val starTint = if (fillFraction >= 0.5f) Color(0xFFFFB300) else Color.LightGray.copy(alpha = 0.5f)
            Icon(
                imageVector = if (fillFraction >= 0.5f) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = starTint,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = String.format(java.util.Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ServiceBadgesRow(service: YemenService, isAr: Boolean) {
    if (service.isPinned || service.isRecommended) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start,
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (service.isPinned) {
                Surface(
                    color = Color.Red.copy(alpha = 0.12f),
                    contentColor = Color.Red,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isAr) "مثبت" else "Pinned", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (service.isRecommended) {
                Surface(
                    color = Color(0xFFFFB300).copy(alpha = 0.12f),
                    contentColor = Color(0xFFE65100),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isAr) "موصى به" else "Recommended", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicFooterBar(
    viewModel: AppViewModel,
    isAr: Boolean,
    onShowInfo: () -> Unit,
    onOpenAi: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.appConfig.collectAsState()

    Surface(
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Parse ordering of elements
            val orderList = config.footerOrder.split(",").map { it.trim().uppercase() }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                orderList.forEach { elem ->
                    when (elem) {
                        "INFO" -> {
                            if (config.showFooterInfo) {
                                IconButton(
                                    onClick = onShowInfo,
                                    modifier = Modifier.testTag("footer_info")
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "App Info",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            if (isAr) "عن التطبيق" else "About Us",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        "AI" -> {
                            if (config.showFooterAi && config.aiAssistantPosition == "FOOTER") {
                                IconButton(
                                    onClick = onOpenAi,
                                    modifier = Modifier
                                        .size(config.aiAssistantSizeDp.dp)
                                        .testTag("footer_ai_chat")
                                ) {
                                    Card(
                                        shape = CircleShape,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = config.aiAssistantIcon.ifEmpty { "💬" },
                                                fontSize = (config.aiAssistantSizeDp / 2.2).sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "PHONE" -> {
                            if (config.showFooterPhone) {
                                val telUri = "tel:${config.footerContactPhone}"
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse(telUri))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("footer_contact")
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.SupportAgent,
                                            contentDescription = "Contact Line",
                                            tint = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            if (isAr) "اتصل بنا" else "Support",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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

// ==================== SCREEN: MAIN DIRECTORY ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    isAr: Boolean,
    onNavigateToJoin: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSelectService: (YemenService) -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val filteredServices by viewModel.filteredServices.collectAsState()
    val config by viewModel.appConfig.collectAsState()
    
    val selectedCat by viewModel.selectedCategory.collectAsState()
    val selectedSub by viewModel.selectedSubCategory.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showAiChatFromFooter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // High quality Yemen smile icon requested:
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE94560)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("😊", fontSize = 20.sp)
                        }
                        Text(
                            text = if (isAr) "دليل خدمات اليمن" else "Yemen Services Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Language Switch Button
                    TextButton(onClick = { viewModel.isArabic.value = !isAr }) {
                        Text(if (isAr) "English" else "العربية", fontWeight = FontWeight.Bold)
                    }
                    // Admin Access Key Button
                    IconButton(onClick = onNavigateToAdmin, modifier = Modifier.testTag("admin_panel_trigger")) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            DynamicFooterBar(
                viewModel = viewModel,
                isAr = isAr,
                onShowInfo = { showAppInfoDialog = true },
                onOpenAi = { showAiChatFromFooter = true }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onNavigateToJoin,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .testTag("join_service_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = "Join Service")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "انضم كمزود خدمة" else "Join as Provider",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Yemen Welcome Area Highlight Block
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = if (isAr) Alignment.End else Alignment.Start) {
                        Text(
                            text = if (isAr) "مرحباً بكم في الدليل الشامل الخدمي 🇾🇪" else "Welcome to Yemen Service Hub 🇾🇪",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAr) "ابحث عن المهندسين، الأطباء، الفنيين وخدمات الطوارئ مع تحديث ومزامنة فورية." else "Locate top engineers, clinics, technicians, and local utilities with instant dynamic sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                        )
                    }
                }
            }

            // Search Filter text input
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = {
                        Text(if (isAr) "ابحث بالاسم، السكن، العمل، القسم..." else "Search by name, category, place...")
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .testTag("search_field"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Categories horizontal selection chips
            item {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = if (isAr) "الأقسام الرئيسية" else "Core Categories",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCat == null,
                                onClick = {
                                    viewModel.selectedCategory.value = null
                                    viewModel.selectedSubCategory.value = null
                                },
                                label = { Text(if (isAr) "الكل" else "All") }
                            )
                        }

                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCat?.id == cat.id,
                                onClick = {
                                    viewModel.selectedCategory.value = cat
                                    viewModel.selectedSubCategory.value = null // Reset subclass when category switches
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(cat.icon, modifier = Modifier.padding(end = 4.dp))
                                        Text(if (isAr) cat.nameAr else cat.nameEn)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Subcategories Horizontal List corresponding to selected Category
            if (selectedCat != null) {
                val matchingSubs = subCategories.filter { it.categoryId == selectedCat!!.id }
                if (matchingSubs.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Text(
                                text = if (isAr) "التصنيفات الفرعية" else "Sub Categories",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedSub == null,
                                        onClick = { viewModel.selectedSubCategory.value = null },
                                        label = { Text(if (isAr) "عرض الكل" else "Show All") }
                                    )
                                }
                                items(matchingSubs) { sub ->
                                    FilterChip(
                                        selected = selectedSub?.id == sub.id,
                                        onClick = { viewModel.selectedSubCategory.value = sub },
                                        label = { Text(if (isAr) sub.nameAr else sub.nameEn) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active list of services
            if (filteredServices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isAr) "لا توجد نتائج مطابقة لمصطلحات البحث" else "No matching services found",
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredServices) { service ->
                    ServiceItemCard(
                        service = service,
                        viewModel = viewModel,
                        isAr = isAr,
                        onSelect = { onSelectService(service) }
                    )
                }
            }
        }
    }

    // App Info Dialog
    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showAppInfoDialog = false }) {
                    Text(if (isAr) "إغلاق" else "Close")
                }
            },
            title = {
                Text(
                    text = if (isAr) "معلومات التطبيق 🇾🇪" else "Application info 🇾🇪",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isAr)
                            "دليل خدمات اليمن هو تطبيق خدمي استرشادي بريادة وطنية يتيح تصفح والبحث ومزامنة الأطباء والمهندسين والكهربائيين وغيرهم على جميع الأجهزة مع ميزة المحادثة الذكية وتغيير الخلفيات ديناميكياً."
                        else
                            "Yemen Services Directory is a local search application to coordinate, discover, and instantly synchronize professionals with conversational support, interactive reviews, and live color skin adaptations.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "${if (isAr) "رقم التواصل بالدعم:" else "Support Line:"} ${config.footerContactPhone}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }

    if (showAiChatFromFooter) {
        AiAssistantChatDialog(
            viewModel = viewModel,
            isAr = isAr,
            onDismiss = { showAiChatFromFooter = false }
        )
    }
}

@Composable
fun ServiceItemCard(
    service: YemenService,
    viewModel: AppViewModel,
    isAr: Boolean,
    onSelect: () -> Unit
) {
    val favs by viewModel.favorites.collectAsState()
    val isFav = favs.contains(service.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable { onSelect() }
            .testTag("service_card_${service.id}"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // If English, Image is Left. Arabic, Image is Right.
                if (!isAr) {
                    // Image thumbnail
                    AsyncImage(
                        model = service.imageUrl.ifBlank { "https://images.unsplash.com/photo-1521791136368-1a46827d0adf?w=120" },
                        contentDescription = "Service Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = service.nameEn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = service.addressEn,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ServiceRatingRow(rating = service.rating, isAr = isAr)
                        ServiceBadgesRow(service = service, isAr = isAr)
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavorite(service.id) },
                        modifier = Modifier.testTag("fav_heart_${service.id}")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color.Red else Color.Gray
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(service.id) },
                        modifier = Modifier.testTag("fav_heart_${service.id}")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) Color.Red else Color.Gray
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = service.nameAr,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = service.addressAr,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ServiceRatingRow(rating = service.rating, isAr = isAr)
                        ServiceBadgesRow(service = service, isAr = isAr)
                    }

                    AsyncImage(
                        model = service.imageUrl.ifBlank { "https://images.unsplash.com/photo-1521791136368-1a46827d0adf?w=120" },
                        contentDescription = "Service Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(6.dp))

            // Work and Residence place indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAr) 
                        "العمل: ${service.workPlace.ifBlank { "غير محدد" }} | السكن: ${service.residencePlace.ifBlank { "غير محدد" }}"
                    else 
                        "Work: ${service.workPlace.ifBlank { "N/A" }} | Residence: ${service.residencePlace.ifBlank { "N/A" }}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== SCREEN: REGISTER / JOIN FORMS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderJoinScreen(
    viewModel: AppViewModel,
    isAr: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()

    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var selectedSub by remember { mutableStateOf<SubCategory?>(null) }
    var address by remember { mutableStateOf("") }
    var workPlace by remember { mutableStateOf("") }
    var residencePlace by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var idCardUrl by remember { mutableStateOf("") } // Optional 

    // Lat/Long interactive maps simulation coordinates
    var latitude by remember { mutableStateOf(15.3694) }
    var longitude by remember { mutableStateOf(44.1910) }

    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAr) "تقديم طلب انضمام كفني/مقدم خدمة" else "Join as Service Provider") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isAr) "يرجى تعبئة الحقول المطلوبة للانضمام إلى الدليل والموافقة من الإدارة" else "Fill custom fields to list your skills inside the verified Yemen Services Directory",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = { Text(if (isAr) "الاسم بالكامل (بالعربية) *" else "Full Name (Arabic) *") },
                modifier = Modifier.fillMaxWidth()
            )

            // Let English name be populated
            OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text(if (isAr) "الاسم بالإنجليزية (اختياري)" else "Full Name (English) (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(if (isAr) "رقم الهاتف / الواتساب *" else "Phone / Whatsapp *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            // Select Category
            var catExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCat?.let { if (isAr) it.nameAr else it.nameEn } ?: "",
                    onValueChange = {},
                    label = { Text(if (isAr) "اختر القسم والتصنيف الرئيسي *" else "Select Major Category *") },
                    readOnly = true,
                    trailingIcon = { IconButton(onClick = { catExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(if (isAr) cat.nameAr else cat.nameEn) },
                            onClick = {
                                selectedCat = cat
                                selectedSub = null
                                catExpanded = false
                            }
                        )
                    }
                }
            }

            // Select Subcategory corresponding to major parent
            if (selectedCat != null) {
                var subExpanded by remember { mutableStateOf(false) }
                val options = subCategories.filter { it.categoryId == selectedCat!!.id }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSub?.let { if (isAr) it.nameAr else it.nameEn } ?: "",
                        onValueChange = {},
                        label = { Text(if (isAr) "اختر التصنيف الفرعي المخصص *" else "Select Subcategory *") },
                        readOnly = true,
                        trailingIcon = { IconButton(onClick = { subExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = subExpanded, onDismissRequest = { subExpanded = false }) {
                        options.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(if (isAr) sub.nameAr else sub.nameEn) },
                                onClick = {
                                    selectedSub = sub
                                    subExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(if (isAr) "العنوان التفصيلي *" else "Detailed Address *") },
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Workplace & Residence (مكان العمل ومكان السكن)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = workPlace,
                    onValueChange = { workPlace = it },
                    label = { Text(if (isAr) "مكان العمل الحالي *" else "Place of Work *") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = residencePlace,
                    onValueChange = { residencePlace = it },
                    label = { Text(if (isAr) "مكان السكن وحي الإقامة *" else "Place of Residence *") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text(if (isAr) "رابط الصورة الخاصة بك / الخدمة" else "Your Photo / Logo URL") },
                placeholder = { Text("https://example.com/item.jpg") },
                modifier = Modifier.fillMaxWidth()
            )

            // Optional National ID photo link
            OutlinedTextField(
                value = idCardUrl,
                onValueChange = { idCardUrl = it },
                placeholder = { Text("https://example.com/id.jpg") },
                label = { Text(if (isAr) "رابط صورة الهوية الشخصية (اختياري)" else "National ID Card Image Link (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // GPS/Map coordinate Picker simulator
            Text(
                text = if (isAr) "تحديد الموقع الجغرافي على الخريطة (محاكاة دقيقة)" else "GPS Coordinates Selection (Simulated)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lWidth = this.size.width
                    val lHeight = this.size.height
                    
                    // Draw horizontal & vertical grid lines to look like map grid lines
                    for (i in 1..8) {
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(0f, lHeight * i / 8),
                            end = androidx.compose.ui.geometry.Offset(lWidth, lHeight * i / 8)
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.08f),
                            start = androidx.compose.ui.geometry.Offset(lWidth * i / 8, 0f),
                            end = androidx.compose.ui.geometry.Offset(lWidth * i / 8, lHeight)
                        )
                    }
                }
                Text(
                    text = "Map Simulation\nLat: $latitude, Long: $longitude",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(4.dp)
                )
                // Simulated GPS Pin Click buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { latitude = 15.3694; longitude = 44.1910 },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Sana'a صنعاء", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { latitude = 12.7855; longitude = 44.9754 },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Aden عدن", fontSize = 10.sp)
                    }
                }
            }

            Button(
                onClick = {
                    if (nameAr.isBlank() || phone.isBlank() || selectedCat == null || address.isBlank() || workPlace.isBlank() || residencePlace.isBlank()) {
                        Toast.makeText(context, if (isAr) "يرجى ملء جميع الحقول المطلوبة ذات العلامة (*)" else "Please fill all mandatory fields marked with (*)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    viewModel.submitJoinRequest(
                        ProviderJoinRequest(
                            id = "",
                            nameAr = nameAr,
                            nameEn = nameEn,
                            phone = phone,
                            category = selectedCat!!.id,
                            subCategory = selectedSub?.id ?: "",
                            imageUrl = imageUrl,
                            address = address,
                            latitude = latitude,
                            longitude = longitude,
                            workPlace = workPlace,
                            residencePlace = residencePlace,
                            idCardImageUrl = idCardUrl,
                            status = "PENDING"
                        )
                    )
                    Toast.makeText(context, if (isAr) "تم إرسال طلبك بنجاح وسيتصل بك أحد المشرفين قريباً" else "Your request has been successfully submitted for review", Toast.LENGTH_LONG).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_join_request_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (isAr) "إرسال الطلب للمراجعة" else "Submit Application",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ==================== SCREEN: DETAIL VIEW ====================

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(
    service: YemenService,
    viewModel: AppViewModel,
    isAr: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val comments by viewModel.activeComments.collectAsState()
    
    var writerName by remember { mutableStateOf("") }
    var writerComment by remember { mutableStateOf("") }
    var writerRating by remember { mutableStateOf(5f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAr) service.nameAr else service.nameEn) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Service Image Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                ) {
                    AsyncImage(
                        model = service.imageUrl.ifBlank { "https://images.unsplash.com/photo-1521791136368-1a46827d0adf?w=500" },
                        contentDescription = "Service Banner Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Badges overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        ServiceBadgesRow(service = service, isAr = false)
                    }
                }
            }

            // Names & Ratings Title
            item {
                Column(horizontalAlignment = if (isAr) Alignment.End else Alignment.Start) {
                    Text(
                        text = if (isAr) service.nameAr else service.nameEn,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ServiceRatingRow(rating = service.rating, isAr = isAr)
                }
            }

            // Contact Actions Group Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Phone Call Dial
                        Button(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phoneNumber}"))
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAr) "اتصال" else "Call")
                        }

                        // WhatsApp Chat Link
                        Button(
                            onClick = {
                                try {
                                    // Strip formatting
                                    val cleanedPhone = service.phoneNumber.replace("+", "").replace(" ", "")
                                    val waUrl = "https://api.whatsapp.com/send?phone=$cleanedPhone"
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No WhatsApp application found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Icon(Icons.Default.Textsms, contentDescription = "WhatsApp")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp")
                        }
                    }
                }
            }

            // Detailed Location & Descriptions
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isAr) "تفاصيل مقدم الخدمة وموقع التواجد" else "Provider specifications",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider()
                        
                        Text(
                            text = "${if (isAr) "العنوان:" else "Address:"} ${if (isAr) service.addressAr else service.addressEn}",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${if (isAr) "مكان العمل الرئيسي:" else "Work Territory:"} ${service.workPlace.ifBlank { if (isAr) "جميع المناطق" else "All areas" }}",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${if (isAr) "حي الإقامة / السكن:" else "Residence Neighborhood:"} ${service.residencePlace.ifBlank { "N/A" }}",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAr) service.descriptionAr else service.descriptionEn,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Add reviews block
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isAr) "إضافة تقييم ورأي حول مزود الخدمة" else "Add star review rating",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        // Interactive Star selector
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                IconButton(onClick = { writerRating = star.toFloat() }) {
                                    Icon(
                                        imageVector = if (writerRating >= star) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (writerRating >= star) Color(0xFFFFB300) else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = writerName,
                            onValueChange = { writerName = it },
                            label = { Text(if (isAr) "الاسم الكريم" else "Your Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = writerComment,
                            onValueChange = { writerComment = it },
                            label = { Text(if (isAr) "رأيك بالتفصيل" else "Your Review feedback") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (writerComment.isBlank()) return@Button
                                viewModel.addComment(
                                    serviceId = service.id,
                                    author = writerName,
                                    text = writerComment,
                                    rating = writerRating
                                )
                                writerComment = ""
                                Toast.makeText(context, if (isAr) "شكرًا لتقييمك الكريم!" else "Review added successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isAr) "إضافة التقييم" else "Submit Review")
                        }
                    }
                }
            }

            // Visitor reviews list sync
            item {
                Text(
                    text = if (isAr) "آراء زوار الدليل ومستخدميه (${comments.size})" else "User Ratings & Reviews (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (comments.isEmpty()) {
                item {
                    Text(
                        text = if (isAr) "لا توجد تقييمات حتى الآن لهذا المزود. كن الأول لتقييمه!" else "No comments yet. Write the first review!",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else {
                items(comments) { comment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = comment.userName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(text = comment.rating.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = comment.comment,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== SCREEN: ADMIN PANEL LOGIN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    viewModel: AppViewModel,
    isAr: Boolean,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val supervisorAccounts by viewModel.supervisorAccounts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAr) "تسجيل دخول المشرفين / الإدارة" else "Supervisor / Admin Login") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingVals ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Area",
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = if (isAr) "بوابة لوحة التحكم والتحكم الفوري بالإعدادات" else "Verified Admin Access Console",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = Alignment.CenterHorizontally.let { TextAlign.Center }
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(if (isAr) "اسم المستخدم المشرف" else "Supervisor Username") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isAr) "كلمة المرور" else "Plain Text Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val lowerUser = username.lowercase().trim()
                            val found = supervisorAccounts.find { it.username == lowerUser && it.passwordPlain == password && it.isEnabled }
                            
                            if (found != null) {
                                viewModel.authenticatedSupervisor.value = found
                                Toast.makeText(context, if (isAr) "مرحباً بك مجدداً ${found.username}!" else "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, if (isAr) "الاسم أو كلمة المرور غير صحيحة أو منتهية الصلاحية" else "Invalid supervisor credit records", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_login_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (isAr) "تسجيل الدخول الآمن" else "Secure Admin Login", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== SCREEN: MASTER ADMIN PANEL DASHBOARD ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    isAr: Boolean,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val yemenServices by viewModel.yemenServices.collectAsState()
    val joinRequests by viewModel.joinRequests.collectAsState()
    val supervisors by viewModel.supervisorAccounts.collectAsState()
    val config by viewModel.appConfig.collectAsState()
    
    val currentAdmin = viewModel.authenticatedSupervisor.value

    var activeTabIdx by remember { mutableIntStateOf(0) }
    val tabTitles = if (isAr) {
        listOf("طلبات الانضمام", "الأقسام والخدمات", "ثيم التطبيق وشريط التذييل", "إدارة المشرفين")
    } else {
        listOf("Join Requests", "Structure & Listings", "Dynamic Theme & Footer", "Supervisors Console")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAr) "لوحة تصرف وتعديل الإدارة: ${currentAdmin?.username}" else "Admin dashboard: ${currentAdmin?.username}") },
                actions = {
                    IconButton(onClick = {
                        viewModel.authenticatedSupervisor.value = null
                        onBackToHome()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Exit Admin Panels")
                    }
                }
            )
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
        ) {
            ScrollableTabRow(selectedTabIndex = activeTabIdx) {
                tabTitles.forEachIndexed { idx, txt ->
                    Tab(
                        selected = activeTabIdx == idx,
                        onClick = { activeTabIdx = idx },
                        text = { Text(txt, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                when (activeTabIdx) {
                    0 -> JoinRequestsTab(joinRequests, viewModel, isAr)
                    1 -> StructureListingsTab(categories, subCategories, yemenServices, viewModel, isAr)
                    2 -> ThemeFooterControllerTab(config, viewModel, isAr)
                    3 -> SupervisorsConsoleTab(supervisors, currentAdmin, viewModel, isAr)
                }
            }
        }
    }
}

// Sub-Tab 1: List join requests & toggle verification instantly
@Composable
fun JoinRequestsTab(
    requests: List<ProviderJoinRequest>,
    viewModel: AppViewModel,
    isAr: Boolean
) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isAr) "لا توجد طلبات انضمام حالياً" else "No active provider join applications available")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(requests) { req ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.nameAr, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            
                            val labelColor = when (req.status) {
                                "PENDING" -> Color(0xFFFF9800)
                                "APPROVED" -> Color(0xFF4CAF50)
                                else -> Color.Red
                            }
                            Surface(
                                color = labelColor.copy(alpha = 0.12f),
                                contentColor = labelColor,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(req.status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("${if (isAr) "الهاتف:" else "Phone:"} ${req.phone}", fontSize = 13.sp)
                        Text("${if (isAr) "العنوان:" else "Address:"} ${req.address}", fontSize = 13.sp)
                        Text("${if (isAr) "مكان السكن:" else "Residence:"} ${req.residencePlace} | ${if (isAr) "مكان العمل:" else "Work segment:"} ${req.workPlace}", fontSize = 12.sp)
                        
                        if (req.idCardImageUrl.isNotBlank()) {
                            Text(
                                text = if (isAr) "الهوية مرفقة: متوفرة" else "National Id Attachment: Attached",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        if (req.status == "PENDING") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.rejectJoinRequest(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(if (isAr) "رفض الطلب" else "Reject")
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { viewModel.approveJoinRequest(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(if (isAr) "موافقة ونشر" else "Approve & Publish")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.deleteJoinRequest(req.id) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Request", tint = Color.Red)
                                    Text(if (isAr) "حذف أرشيف الطلب" else "Delete Record", color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-Tab 2: Add major Category classifications or specific service providers
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StructureListingsTab(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    services: List<YemenService>,
    viewModel: AppViewModel,
    isAr: Boolean
) {
    var showAddCatDialog by remember { mutableStateOf(false) }
    var catNameAr by remember { mutableStateOf("") }
    var catNameEn by remember { mutableStateOf("") }
    var catEmoji by remember { mutableStateOf("🛠️") }

    var showAddServiceDialog by remember { mutableStateOf(false) }
    var sNameAr by remember { mutableStateOf("") }
    var sNameEn by remember { mutableStateOf("") }
    var sPhone by remember { mutableStateOf("") }
    var sDescAr by remember { mutableStateOf("") }
    var sDescEn by remember { mutableStateOf("") }
    var sAddrAr by remember { mutableStateOf("") }
    var sAddrEn by remember { mutableStateOf("") }
    var sWorkArea by remember { mutableStateOf("") }
    var sSkenArea by remember { mutableStateOf("") }
    var sImageUrl by remember { mutableStateOf("") }
    var sRatingSelected by remember { mutableStateOf(5f) }
    var sIsPinned by remember { mutableStateOf(false) }
    var sIsRecommended by remember { mutableStateOf(false) }
    var sSelectedCatId by remember { mutableStateOf("") }
    var sSelectedSubId by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "التصنيفات (${categories.size})" else "Major Categories (${categories.size})",
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = { showAddCatDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(if (isAr) "إضافة قسم رئيسي" else "Add Category")
                    }
                }
            }
        }

        items(categories) { cat ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(cat.nameAr, fontWeight = FontWeight.Bold)
                            Text(cat.nameEn, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        }

        // Services administration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) "مقدمي الخدمات النشطين (${services.size})" else "Active Services (${services.size})",
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = { 
                        if (categories.isNotEmpty()) {
                            sSelectedCatId = categories.first().id
                            showAddServiceDialog = true
                        } else {
                            Toast.makeText(viewModel.getApplication(), "Create a category first", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Text(if (isAr) "إضافة مقدم خدمة مباشر" else "Add Provider")
                    }
                }
            }
        }

        items(services) { serv ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(serv.nameAr, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteService(serv.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete provider item", tint = Color.Red)
                        }
                    }
                    Text(
                        text = "${if (isAr) "مكان العمل:" else "Work:"} ${serv.workPlace} | ${if (isAr) "السكن:" else "Residence:"} ${serv.residencePlace}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    // Add Major Category Dialog popup
    if (showAddCatDialog) {
        AlertDialog(
            onDismissRequest = { showAddCatDialog = false },
            confirmButton = {
                Button(onClick = {
                    if (catNameAr.isBlank()) return@Button
                    val cid = "cat_" + System.currentTimeMillis()
                    viewModel.saveCategory(Category(cid, catNameAr, catNameEn.ifBlank { catNameAr }, catEmoji, categories.size + 1))
                    showAddCatDialog = false
                    catNameAr = ""
                    catNameEn = ""
                }) {
                    Text(if (isAr) "حفظ والتزامن" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCatDialog = false }) { Text("Cancel") }
            },
            title = { Text(if (isAr) "إضافة تصنيف عام جديد" else "Create Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = catNameAr, onValueChange = { catNameAr = it }, label = { Text("الاسم بالعربية *") })
                    OutlinedTextField(value = catNameEn, onValueChange = { catNameEn = it }, label = { Text("الاسم بالإنجليزية") })
                    OutlinedTextField(value = catEmoji, onValueChange = { catEmoji = it }, label = { Text("رمز تعبيري / آيقونة (Emoji)") })
                }
            }
        )
    }

    // Add service provider popup form manual
    if (showAddServiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddServiceDialog = false },
            confirmButton = {
                Button(onClick = {
                    if (sNameAr.isBlank() || sPhone.isBlank() || sAddrAr.isBlank() || sWorkArea.isBlank() || sSkenArea.isBlank()) {
                        return@Button
                    }
                    val servId = "srv_" + System.currentTimeMillis()
                    viewModel.saveService(
                        YemenService(
                            id = servId,
                            category = sSelectedCatId,
                            subCategory = sSelectedSubId,
                            nameAr = sNameAr,
                            nameEn = sNameEn.ifBlank { sNameAr },
                            phoneNumber = sPhone,
                            descriptionAr = sDescAr,
                            descriptionEn = sDescEn.ifBlank { sDescAr },
                            addressAr = sAddrAr,
                            addressEn = sAddrEn.ifBlank { sAddrAr },
                            imageUrl = sImageUrl,
                            rating = sRatingSelected,
                            isPinned = sIsPinned,
                            isRecommended = sIsRecommended,
                            workPlace = sWorkArea,
                            residencePlace = sSkenArea
                        )
                    )
                    showAddServiceDialog = false
                    // Reset
                    sNameAr = ""; sNameEn = ""; sPhone = ""; sDescAr = ""; sDescEn = ""; sAddrAr = ""; sAddrEn = ""; sWorkArea = ""; sSkenArea = ""; sImageUrl = ""
                }) {
                    Text(if (isAr) "حفظ وإضافة" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServiceDialog = false }) { Text("Cancel") }
            },
            title = { Text(if (isAr) "إضافة مزود خدمة مباشر" else "Add New Provider Record") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = sNameAr, onValueChange = { sNameAr = it }, label = { Text("اسم مقدم الخدمة (عربي) *") })
                    OutlinedTextField(value = sNameEn, onValueChange = { sNameEn = it }, label = { Text("اسم مقدم الخدمة (إنجليزي)") })
                    OutlinedTextField(value = sPhone, onValueChange = { sPhone = it }, label = { Text("رقم الاتصال الدائم *") })
                    OutlinedTextField(value = sDescAr, onValueChange = { sDescAr = it }, label = { Text("شرح الخدمات (عربي)") })
                    OutlinedTextField(value = sAddrAr, onValueChange = { sAddrAr = it }, label = { Text("العنوان التقريبي (صنعاء، عدن...) *") })
                    
                    OutlinedTextField(value = sWorkArea, onValueChange = { sWorkArea = it }, label = { Text("مكان ونطاق العمل التفصيلي *") })
                    OutlinedTextField(value = sSkenArea, onValueChange = { sSkenArea = it }, label = { Text("مكان السكن والحي السكني *") })
                    
                    OutlinedTextField(value = sImageUrl, onValueChange = { sImageUrl = it }, label = { Text("رابط صورة رمزية") })

                    // Category mapping
                    var expCategory by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { expCategory = true }) {
                            Text("Category parent: " + (categories.find { it.id == sSelectedCatId }?.nameAr ?: "Select"))
                        }
                        DropdownMenu(expanded = expCategory, onDismissRequest = { expCategory = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.nameAr) },
                                    onClick = {
                                        sSelectedCatId = cat.id
                                        expCategory = false
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = sIsPinned, onCheckedChange = { sIsPinned = it })
                        Text("مقدم خدمة مثبت (Pin to Top)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = sIsRecommended, onCheckedChange = { sIsRecommended = it })
                        Text("موصى به (Recommended Tag)")
                    }
                }
            }
        )
    }
}

// Sub-Tab 3: Skin and visual configurations controller
@Composable
fun ThemeFooterControllerTab(
    appConfig: AppConfig,
    viewModel: AppViewModel,
    isAr: Boolean
) {
    var footerPhoneInput by remember { mutableStateOf(appConfig.footerContactPhone) }
    
    val themeOptionsList = listOf(
        Pair("cosmic_slate", if (isAr) "كوزميك سيلفر الداكن الأنيق" else "Cosmic Slate Silver"),
        Pair("charcoal_gold" , if (isAr) "الفحم الحجري والذهب الفاخر" else "Vibrant Gold Charcoal"),
        Pair("royal_emerald", if (isAr) "الزمرد الملكي البراق" else "Royal Emerald Green"),
        Pair("red_black", if (isAr) "القرمزي والأسود المطفي" else "Crimson Torch Red"),
        Pair("slate_silver", if (isAr) "الفضي الكلاسيكي" else "Industrial Silver"),
        Pair("ocean_teal", if (isAr) "التركواز المحيطي الهادئ" else "Relax Tealing Ocean"),
        Pair("beige_cream", if (isAr) "البيج الكريمي الدافئ (ثيم فاتح)" else "Warm Beige Cream (Light)")
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Theme Selector
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isAr) "تخصيص ألوان وثيم التطبيق (مزامنة فورية لكل الهواتف)" else "Central Theme Skin Orchestrator (Synchronized Live)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    themeOptionsList.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateAppConfig(appConfig.copy(globalTheme = p.first))
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appConfig.globalTheme == p.first,
                                onClick = {
                                    viewModel.updateAppConfig(appConfig.copy(globalTheme = p.first))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(p.second, fontWeight = if (appConfig.globalTheme == p.first) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }

        // Assistant size, visibility and placements
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAr) "لوحة تعديل أيقونة المساعد الذكي العائم" else "AI Chat Assistant controls",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) "تفعيل ظهور المساعد الذكي" else "Enable Gemini Assistant Bubble")
                        Switch(
                            checked = appConfig.showAiAssistant,
                            onCheckedChange = { viewModel.updateAppConfig(appConfig.copy(showAiAssistant = it)) }
                        )
                    }

                    // Positioning (FLOATING vs FOOTER placement!)
                    Text(if (isAr) "توطين مكان زر المساعد الذكي" else "AI Assistant positioning placement")
                    Row {
                        FilterChip(
                            selected = appConfig.aiAssistantPosition == "FLOATING",
                            onClick = { viewModel.updateAppConfig(appConfig.copy(aiAssistantPosition = "FLOATING")) },
                            label = { Text(if (isAr) "عائم على الشاشة" else "Floating overlay") }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        FilterChip(
                            selected = appConfig.aiAssistantPosition == "FOOTER",
                            onClick = { viewModel.updateAppConfig(appConfig.copy(aiAssistantPosition = "FOOTER")) },
                            label = { Text(if (isAr) "تنزيله مدمجاً بشريط التذييل" else "Integrated in footer bar") }
                        )
                    }

                    // Custom avatar representation
                    OutlinedTextField(
                        value = appConfig.aiAssistantIcon,
                        onValueChange = { viewModel.updateAppConfig(appConfig.copy(aiAssistantIcon = it)) },
                        label = { Text(if (isAr) "رمز أو رمز التعبير لتغيير أيقونة المساعد" else "Custom Emoji icon representation") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Size controller slider
                    Column {
                        Text("${if (isAr) "التحكم في حجم قطر الأيقونة:" else "Adjust Assistant Bubble Diameter:"} ${appConfig.aiAssistantSizeDp} dp")
                        Slider(
                            value = appConfig.aiAssistantSizeDp.toFloat(),
                            onValueChange = { viewModel.updateAppConfig(appConfig.copy(aiAssistantSizeDp = it.toInt())) },
                            valueRange = 30f..80f
                        )
                    }
                }
            }
        }

        // Footer elements control and arrangement re-ordering
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isAr) "عناصر شريط التذييل (إظهار، إخفاء، أو إعادة ترتيب المسار)" else "Footer Toolbar elements configuration and order",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) "إظهار أيقونة معلومات التطبيق" else "Show About Us info icon")
                        Checkbox(checked = appConfig.showFooterInfo, onCheckedChange = { viewModel.updateAppConfig(appConfig.copy(showFooterInfo = it)) })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) "إظهار زر الاتصال بالدعم" else "Show Call Support icon")
                        Checkbox(checked = appConfig.showFooterPhone, onCheckedChange = { viewModel.updateAppConfig(appConfig.copy(showFooterPhone = it)) })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isAr) "إظهار المساعد ضمن الشريط (في حال تفعيله)" else "Show AI icon (if integrated in footer)")
                        Checkbox(checked = appConfig.showFooterAi, onCheckedChange = { viewModel.updateAppConfig(appConfig.copy(showFooterAi = it)) })
                    }

                    // Reordering dynamic paths
                    OutlinedTextField(
                        value = appConfig.footerOrder,
                        onValueChange = { viewModel.updateAppConfig(appConfig.copy(footerOrder = it)) },
                        label = { Text(if (isAr) "ترتيب العناصر (مفصولة بفاصلة: INFO, AI, PHONE)" else "Arrangement schema (Separated by comma: INFO, AI, PHONE)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Contacts edit
                    OutlinedTextField(
                        value = footerPhoneInput,
                        onValueChange = {
                            footerPhoneInput = it
                            viewModel.updateAppConfig(appConfig.copy(footerContactPhone = it))
                        },
                        label = { Text(if (isAr) "تعديل رقم تواصل الدعم الفني بالدليل" else "Modify Support contact phone line") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Sub-Tab 4: Admin Supervisor console Accounts Management
@Composable
fun SupervisorsConsoleTab(
    supervisors: List<SupervisorAccount>,
    currentAdmin: SupervisorAccount?,
    viewModel: AppViewModel,
    isAr: Boolean
) {
    val context = LocalContext.current
    var inputUser by remember { mutableStateOf("") }
    var inputPass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isAr) "تسجيل حساب مشرف جديد بالدليل" else "Register new system supervisor",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = inputUser,
                    onValueChange = { inputUser = it },
                    label = { Text(if (isAr) "اسم المستخدم المشرف" else "Supervisor username") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inputPass,
                    onValueChange = { inputPass = it },
                    label = { Text(if (isAr) "كلمة المرور البدئية للمشرف" else "Initial Password") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (inputUser.isBlank() || inputPass.isBlank()) return@Button
                        viewModel.saveSupervisor(
                            SupervisorAccount(
                                id = inputUser.lowercase().trim(),
                                username = inputUser.lowercase().trim(),
                                passwordPlain = inputPass,
                                role = "SUPERVISOR",
                                isEnabled = true
                            )
                        )
                        inputUser = ""
                        inputPass = ""
                        Toast.makeText(context, if (isAr) "تمت إضافة المشرف للدليل بنجاح وتفعيل حسابه" else "Supervisor created and mapped successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isAr) "إنشاء وتفعيل الحساب" else "Create Supervisor")
                }
            }
        }

        Text(
            text = if (isAr) "المشرفين ومدراء السيرفر النشطين حالياً" else "Active directory supervisors",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(supervisors) { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(s.username, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Role: ${s.role} | Password Plain: ${s.passwordPlain}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        
                        // Prevent deleting self
                        if (s.username != currentAdmin?.username) {
                            IconButton(onClick = { viewModel.deleteSupervisor(s.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Supervisor Account", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== WIDGET: ASSISTANT CHAT DIALOG CONTAINER ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantChatDialog(
    viewModel: AppViewModel,
    isAr: Boolean,
    onDismiss: () -> Unit
) {
    var userPromptText by remember { mutableStateOf("") }
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()

    val currentConfig by viewModel.appConfig.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "أغلق المحادثة" else "Close Assistant")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(currentConfig.aiAssistantIcon, fontSize = 24.sp)
                Column {
                    Text(
                        text = if (isAr) "المساعد اليمني الذكي دليل" else "Smart Yemen AI Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Powered by Gemini 3.5 Flash",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                // Messages List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(8.dp)
                ) {
                    if (chatHistory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAr)
                                    "مرحباً! اسألني عن أفضل المصلحين، الأقسام، أو مقدمي خدمات التقنية وسأجيبك فوراً."
                                else
                                    "Ask me for recommendation recommendations on local services or mechanics, and I will resolve it instantly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            reverseLayout = false,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatHistory) { entry ->
                                val turnIsUser = entry.second
                                val msgTxt = entry.first
                                val backgroundColored = if (turnIsUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                val alignOffset = if (turnIsUser) Alignment.CenterEnd else Alignment.CenterStart
                                
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = alignOffset
                                ) {
                                    Surface(
                                        color = backgroundColored,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = msgTxt,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
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
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Bar Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = userPromptText,
                        onValueChange = { userPromptText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (isAr) "اسأل المساعد الذكي..." else "Ask Assistant...") },
                        maxLines = 2,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    IconButton(
                        onClick = {
                            if (userPromptText.isNotBlank()) {
                                viewModel.sendMessage(userPromptText)
                                userPromptText = ""
                            }
                        },
                        modifier = Modifier.testTag("ai_send_message_button"),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}
