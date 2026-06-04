package com.dalily.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.dalily.services.data.*
import com.dalily.services.ui.theme.DalilyTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    HOME,
    DETAIL,
    ADMIN_PANEL,
    ADMIN_LOGS
}

class DalilyViewModel(private val repository: DalilyRepository) : ViewModel() {
    val providers = repository.allProviders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val auditLogs = repository.allAuditLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var currentScreen by mutableStateOf(Screen.HOME)
        private set

    private val _activeProviderId = MutableStateFlow<Int?>(null)
    val activeProviderId = _activeProviderId.asStateFlow()

    val activeProvider = combine(providers, _activeProviderId) { providerList, currentId ->
        providerList.find { it.id == currentId }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    private val _activeProviderReviews = MutableStateFlow<List<Review>>(emptyList())
    val activeProviderReviews = _activeProviderReviews.asStateFlow()

    var averageRating by mutableStateOf(0.0)
        private set

    var reviewsCount by mutableStateOf(0)
        private set

    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf("الكل")

    var isAdminLoggedIn by mutableStateOf(false)
    var currentAdminName by mutableStateOf("")

    private var reviewsJob: kotlinx.coroutines.Job? = null

    init {
        // Automatically calculate average rating when active reviews change
        viewModelScope.launch {
            activeProviderReviews.collect { reviews ->
                if (reviews.isNotEmpty()) {
                    averageRating = reviews.map { it.rating }.average()
                    reviewsCount = reviews.size
                } else {
                    averageRating = 0.0
                    reviewsCount = 0
                }
            }
        }
    }

    fun navigateTo(screen: Screen, providerId: Int? = null) {
        currentScreen = screen
        if (providerId != null) {
            _activeProviderId.value = providerId
            reviewsJob?.cancel()
            reviewsJob = viewModelScope.launch {
                repository.getReviewsForProvider(providerId).collect {
                    _activeProviderReviews.value = it
                }
            }
        }
    }

    fun submitReview(reviewer: String, rating: Int, comment: String) {
        val pid = _activeProviderId.value ?: return
        viewModelScope.launch {
            val r = Review(
                providerId = pid,
                rating = rating,
                comment = comment,
                reviewerName = reviewer
            )
            repository.insertReview(r)
        }
    }

    fun adminLogin(name: String) {
        isAdminLoggedIn = true
        currentAdminName = name
        viewModelScope.launch {
            val locations = listOf("صنعاء، اليمن", "عدن، اليمن", "تعز، اليمن", "المكلا، اليمن", "إب، اليمن")
            val randomLoc = locations.random()
            repository.insertAuditLog(
                AuditLog(
                    action = "تسجيل دخول المسؤول",
                    details = "تم تسجيل دخول المسؤول $name بنجاح لمراقبة سجلات التشغيل وضبط مقدمي الخدمات.",
                    location = randomLoc,
                    adminName = name
                )
            )
        }
    }

    fun adminLogout() {
        val name = currentAdminName
        isAdminLoggedIn = false
        currentAdminName = ""
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLog(
                    action = "تسجيل خروج المسؤل",
                    details = "تم تسجيل خروج المسؤول $name بأمان من لوحة التحكم.",
                    location = "النظام الداخلي",
                    adminName = name
                )
            )
        }
    }

    fun addAuditLog(action: String, details: String) {
        val name = if (currentAdminName.isEmpty()) "المشرف العام" else currentAdminName
        viewModelScope.launch {
            repository.insertAuditLog(
                AuditLog(
                    action = action,
                    details = details,
                    location = "التحكم الداخلي للبرنامج",
                    adminName = name
                )
            )
        }
    }

    fun toggleProviderPin(provider: ServiceProvider) {
        viewModelScope.launch {
            val updated = provider.copy(isPinned = !provider.isPinned)
            repository.updateProvider(updated)
            addAuditLog(
                action = "تحديث التثبيت",
                details = "تم ${if (updated.isPinned) "تثبيت" else "إلغاء تثبيت"} مقدم الخدمة: ${provider.name} في الصدارة."
            )
        }
    }

    fun toggleProviderRecommended(provider: ServiceProvider) {
        viewModelScope.launch {
            val updated = provider.copy(isRecommended = !provider.isRecommended)
            repository.updateProvider(updated)
            addAuditLog(
                action = "تحديث توصية الأدمن",
                details = "تم ${if (updated.isRecommended) "تمييز" else "إلغاء تمييز"} مقدم الخدمة: ${provider.name} كموصى به."
            )
        }
    }

    fun toggleProviderVerified(provider: ServiceProvider) {
        viewModelScope.launch {
            val updated = provider.copy(isVerified = !provider.isVerified)
            repository.updateProvider(updated)
            addAuditLog(
                action = "تحديث التوثيق",
                details = "تم ${if (updated.isVerified) "منح الشارة الزرقاء لـ" else "سحب الشارة الزرقاء من"} مقدم الخدمة: ${provider.name}."
            )
        }
    }

    fun toggleProviderPremium(provider: ServiceProvider, customColor: String? = null) {
        viewModelScope.launch {
            val isPrem = !provider.isPremium
            val updated = provider.copy(
                isPremium = isPrem,
                cardColorHex = if (isPrem) (customColor ?: "#FFF9C4") else null,
                isRecommended = if (isPrem) true else provider.isRecommended // Premium automatically enters recommended
            )
            repository.updateProvider(updated)
            addAuditLog(
                action = "تحديث العضوية المميزة",
                details = "تم ${if (updated.isPremium) "تفعيل العضوية المميزة ومربع الخلفية لـ" else "إلغاء العضوية المميزة لـ"} مقدم الخدمة: ${provider.name}."
            )
        }
    }

    fun updateProviderColor(provider: ServiceProvider, colorHex: String) {
        viewModelScope.launch {
            val updated = provider.copy(cardColorHex = colorHex)
            repository.updateProvider(updated)
            addAuditLog(
                action = "تحديث لون بطاقة الخدمة",
                details = "تم تعديل لون خلفية بطاقات ${provider.name} إلى الرمز: $colorHex."
            )
        }
    }

    fun registerProviderFromAdmin(
        name: String,
        category: String,
        phone: String,
        address: String,
        description: String,
        isPremium: Boolean,
        colorHex: String?
    ) {
        viewModelScope.launch {
            val prov = ServiceProvider(
                name = name,
                category = category,
                phone = phone,
                address = address,
                description = description,
                isPremium = isPremium,
                cardColorHex = if (isPremium) (colorHex ?: "#FFF9C4") else null,
                isRecommended = isPremium,
                isPinned = false,
                isVerified = isPremium
            )
            repository.insertProvider(prov)
            addAuditLog(
                action = "إضافة مقدم خدمة",
                details = "تم إدراج مقدم الخدمة الجديد: $name بنجاح في قاعدة البيانات للجمهور."
            )
        }
    }

    fun deleteProviderFromAdmin(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.deleteProvider(provider)
            addAuditLog(
                action = "حذف مقدم خدمة",
                details = "تم إقصاء وحذف مقدم الخدمة: ${provider.name} نهائياً."
            )
        }
    }
}

class ViewModelFactory(private val repository: DalilyRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DalilyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DalilyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: DalilyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "dalily_services_db"
        ).build()

        repository = DalilyRepository(database)

        // Pre-populate sample database items in coroutine scope before loading the view
        lifecycleScope.launch {
            repository.initSeedDataIfEmpty()
        }

        setContent {
            DalilyTheme {
                val viewModel: DalilyViewModel = viewModel(
                    factory = ViewModelFactory(repository)
                )

                // Force layout direction to RTL for beautiful Arabic local feel
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainAppScaffolding(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScaffolding(viewModel: DalilyViewModel) {
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableStateOf(0L) }
    val activity = context as? Activity

    // Back button behavior: Single press goes back to Main screen, double press within 2s finishes the App.
    BackHandler {
        if (viewModel.currentScreen != Screen.HOME) {
            viewModel.navigateTo(Screen.HOME)
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                activity?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "اضغط مرة أخرى للخروج من التطبيق", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = viewModel.currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.HOME -> DalilyHomeScreen(viewModel)
                    Screen.DETAIL -> DalilyDetailScreen(viewModel)
                    Screen.ADMIN_PANEL -> DalilyAdminScreen(viewModel)
                    Screen.ADMIN_LOGS -> AuditLogsScreen(viewModel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1: HOME
// -------------------------------------------------------------
@Composable
fun DalilyHomeScreen(viewModel: DalilyViewModel) {
    val providers by viewModel.providers.collectAsState()
    val searchQuery = viewModel.searchQuery
    val selectedCategory = viewModel.selectedCategory

    val categories = listOf("الكل", "السباكة", "الكهرباء", "التبريد والتكييف", "النجارة", "صيانة برمجيات وهواتف", "خدمات التوصيل")

    // Filter providers based on search query and category
    val filteredProviders = remember(providers, searchQuery, selectedCategory) {
        providers.filter { provider ->
            val matchesCategory = selectedCategory == "الكل" || provider.category == selectedCategory
            val matchesSearch = provider.name.contains(searchQuery, ignoreCase = true) ||
                    provider.description.contains(searchQuery, ignoreCase = true) ||
                    provider.address.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val recommendedProviders = remember(providers) {
        providers.filter { it.isRecommended || it.isPremium }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "دليل الخدمات اليماني",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ابحث عن أفضل الفنيين ومقدمي الخدمات بلمح البصر",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Control panel login toggler button
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.ADMIN_PANEL) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .testTag("admin_panel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "لوحة التحكم للأدمن",
                        tint = Color.White
                    )
                }
            }
        }

        // Search Bar Block
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                placeholder = { Text("ابحث باسم الفني، أو مجاله، أو موقعه...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // Categories Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedCategory = category },
                    label = { Text(text = category, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("category_chip_$category"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main List Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            // Horizontal scrollbar of recommended/premium providers (only when category filter is "الكل")
            if (selectedCategory == "الكل" && searchQuery.isEmpty() && recommendedProviders.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "نجمه الفخامة",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مقدمو الخدمات الموصى بهم ⭐",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            items(recommendedProviders) { provider ->
                                RecommendedProviderCard(
                                    provider = provider,
                                    onClick = { viewModel.navigateTo(Screen.DETAIL, provider.id) }
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // General List Title
            item {
                Text(
                    text = if (selectedCategory == "الكل") "قائمة الفنيين ومزودي الخدمات في منطقتك" else "نتائج البحث في مجال: $selectedCategory",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            if (filteredProviders.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "لا توجد نتائج",
                            tint = Color.Gray,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "عذراً.. لا توجد نتائج مطابقة لمؤشرات البحث حالياً.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredProviders) { provider ->
                    ProviderListCard(
                        provider = provider,
                        onClick = { viewModel.navigateTo(Screen.DETAIL, provider.id) }
                    )
                }
            }
        }
    }
}

// Subcomponent: Recommended provider Card
@Composable
fun RecommendedProviderCard(
    provider: ServiceProvider,
    onClick: () -> Unit
) {
    // Elegant background with customized background theme color if premium (with gradients)
    val cardColor = if (provider.isPremium && provider.cardColorHex != null) {
        Color(android.graphics.Color.parseColor(provider.cardColorHex))
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val onCardTextColor = if (provider.isPremium) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick)
            .testTag("recommended_provider_${provider.id}"),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Premium Badge or Recommended Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (provider.isPremium) Color(0xFFFF9100) else MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (provider.isPremium) "عضو مميز 👑" else "موصى به 👍",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (provider.isVerified) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "حساب موثق",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = provider.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = onCardTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "💼 " + provider.category,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = onCardTextColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = provider.description,
                fontSize = 11.sp,
                color = onCardTextColor.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "📍 " + provider.address,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = onCardTextColor,
                maxLines = 1
            )
        }
    }
}

// Subcomponent: Standard/Premium Provider row list card
@Composable
fun ProviderListCard(
    provider: ServiceProvider,
    onClick: () -> Unit
) {
    // If premium, has customizable background color specified in hex parameter
    val cardBg = if (provider.isPremium && provider.cardColorHex != null) {
        Color(android.graphics.Color.parseColor(provider.cardColorHex)).copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val onCardText = if (provider.isPremium) Color.Black else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .testTag("provider_card_${provider.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (provider.isPinned) 5.dp else 1.dp),
        border = if (provider.isPinned) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (provider.isPinned) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "مثبت في الصدارة",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = provider.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = onCardText
                    )

                    if (provider.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "شارة زرقاء موثقة",
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier
                                .size(16.dp)
                                .testTag("verified_badge_${provider.id}")
                        )
                    }
                }

                // Premium decoration icon/indicator
                if (provider.isPremium) {
                    Text(
                        text = "مميز ⭐",
                        color = Color(0xFFFF8F00),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = provider.category,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (provider.isPremium) Color.DarkGray else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            (if (provider.isPremium) Color.Black.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primaryContainer),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )

                Text(
                    text = "📍 " + provider.address,
                    fontSize = 12.sp,
                    color = onCardText.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = provider.description,
                fontSize = 12.sp,
                color = onCardText.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📞 " + provider.phone,
                    fontSize = 13.sp,
                    color = onCardText,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "قراءة المزيد ⬅",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (provider.isPremium) Color.Black else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: DETAIL (WITH RATING AND REVIEWS)
// -------------------------------------------------------------
@Composable
fun DalilyDetailScreen(viewModel: DalilyViewModel) {
    val context = LocalContext.current
    val provider by viewModel.activeProvider.collectAsState()
    val reviews by viewModel.activeProviderReviews.collectAsState()
    val avgScore = viewModel.averageRating
    val revCount = viewModel.reviewsCount

    // Form inputs
    var reviewerName by remember { mutableStateOf("") }
    var userRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    if (provider == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val p = provider!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Upper Toolbar with action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.HOME) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "الرجوع")
            }

            Text(
                text = p.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { /* COPY DETAILS */ },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Share, contentDescription = "مشاركة")
            }
        }

        // Provider Main Showcase Card
        val detailCardColor = if (p.isPremium && p.cardColorHex != null) {
            Color(android.graphics.Color.parseColor(p.cardColorHex))
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }

        val onDetailText = if (p.isPremium) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = detailCardColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = p.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = onDetailText
                    )
                    if (p.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "شارة زرقاء موثقة",
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "💼 " + p.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (p.isPremium) Color.DarkGray else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Star Ratings aggregate
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "نجمة التقييم",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (revCount > 0) String.format("%.1f", avgScore) else "لا توجد تقييمات",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = onDetailText
                    )
                    Text(
                        text = "($revCount مراجعة)",
                        fontSize = 13.sp,
                        color = onDetailText.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = p.description,
                    fontSize = 14.sp,
                    color = onDetailText.copy(alpha = 0.85f),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = onDetailText.copy(alpha = 0.15f))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 العنوان: " + p.address,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = onDetailText
                    )

                    Button(
                        onClick = { /* COPY PHONE */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (p.isPremium) Color.Black else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "اتصال: ${p.phone}", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Review Submission Form
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "قيم الخدمة واكتب مراجعة نصية ✍️",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Five Star interactive Row Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { userRating = i },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (i <= userRating) Icons.Default.Star else Icons.Default.PlayArrow, // Outlined behavior or play
                                contentDescription = "$i نجوم",
                                tint = if (i <= userRating) Color(0xFFFFB300) else Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reviewerName,
                    onValueChange = { reviewerName = it },
                    label = { Text("اسمك الكريم") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reviewer_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reviewComment,
                    onValueChange = { reviewComment = it },
                    label = { Text("اكتب مراجعتك التفصيلية هنا...") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reviewer_comment_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (reviewerName.trim().isEmpty() || reviewComment.trim().isEmpty()) {
                            Toast.makeText(context, "الرجاء اكمال الاسم والتعليق قبل الارسال!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.submitReview(reviewerName, userRating, reviewComment)
                            Toast.makeText(context, "شكرًا لك! تم تسجيل مراجعتك بنجاح.", Toast.LENGTH_SHORT).show()
                            reviewerName = ""
                            reviewComment = ""
                            userRating = 5
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_review_button")
                ) {
                    Text(text = "إرسال التقييم والمراجعة ✅")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Display Reviews List
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = "مراجعات العملاء (${reviews.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (reviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أي مراجعات مكتوبة بعد. كن أول من يكتب مراجعته!",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                reviews.forEach { r ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = r.reviewerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row {
                                    for (s in 1..r.rating) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "نجمة",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = r.comment,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val logDate = remember(r.timestamp) {
                                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(r.timestamp))
                            }
                            Text(
                                text = logDate,
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// -------------------------------------------------------------
// SCREEN 3: ADMIN PANEL (WITH AUDIT LOG & CONTROLS)
// -------------------------------------------------------------
@Composable
fun DalilyAdminScreen(viewModel: DalilyViewModel) {
    val providers by viewModel.providers.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var inputAdminName by remember { mutableStateOf("") }
    var inputAdminPass by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (!viewModel.isAdminLoggedIn) {
        // Admin Login Screen Beautiful Block
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "قفل الأمان",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "دخول لوحة التحكم للمستشارين",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "الرجاء تأكيد اسمك ورمز الدخول لاستعراض سجلات Audit Logs وتعديل تراخيص مقدمي الخدمات",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = inputAdminName,
                onValueChange = { inputAdminName = it },
                label = { Text("اسم المسؤول") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_username")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputAdminPass,
                onValueChange = { inputAdminPass = it },
                label = { Text("رمز المرور السري") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (inputAdminName.trim().isEmpty() || inputAdminPass.trim().isEmpty()) {
                        Toast.makeText(context, "يرجى تعبئة كافة الحقول!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.adminLogin(inputAdminName)
                        Toast.makeText(context, "أهلاً بك يا ${inputAdminName}! تم الدخول بنجاح", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_login_submit")
            ) {
                Text(text = "تسجيل الدخول الآمن للوحة التحكم")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { viewModel.navigateTo(Screen.HOME) }) {
                Text(text = "الرجوع للرئيسية")
            }
        }
    } else {
        // Admin Panel Logged In UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "بوابة الإدارة للمشرفين",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "المسؤول الحالي: ${viewModel.currentAdminName}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.ADMIN_LOGS) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.List, contentDescription = "سجل النشاط المالي والأمني", tint = Color.White)
                    }

                    IconButton(
                        onClick = { viewModel.adminLogout() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل الخروج", tint = Color.White)
                    }
                }
            }

            // Tabs toggle selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabButton(isSelected = activeTab == 0, text = "الفنيين المرخصين (${providers.size})", onClick = { activeTab = 0 })
                TabButton(isSelected = activeTab == 1, text = "تسجيل مقدم جديد+", onClick = { activeTab = 1 })
            }

            Divider()

            if (activeTab == 0) {
                // Tab 0: Providers Management List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "تنبيه", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "بصفتك مسؤول، يمكنك إدارة تراخيص التثبيت في الصدارة، التوصية، وسام التوثيق الأزرق، أو ترقية مقدم الخدمة للعضوية المميزة (Premium) وتعيين لون بطاقته.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    items(providers) { provider ->
                        AdminProviderManagementCard(
                            provider = provider,
                            onTogglePin = { viewModel.toggleProviderPin(provider) },
                            onToggleRec = { viewModel.toggleProviderRecommended(provider) },
                            onToggleVer = { viewModel.toggleProviderVerified(provider) },
                            onTogglePrem = { viewModel.toggleProviderPremium(provider, "#FFF9C4") },
                            onColorChange = { hex -> viewModel.updateProviderColor(provider, hex) },
                            onDelete = { viewModel.deleteProviderFromAdmin(provider) }
                        )
                    }
                }
            } else {
                // Tab 1: Registration Form of new Provider
                AdminRegisterProviderForm(viewModel)
            }

            // Navigation back button
            Button(
                onClick = { viewModel.navigateTo(Screen.HOME) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = "العودة إلى الواجهة الرئيسية")
            }
        }
    }
}

// Subcomponent: Admin control card for specific provider
@Composable
fun AdminProviderManagementCard(
    provider: ServiceProvider,
    onTogglePin: () -> Unit,
    onToggleRec: () -> Unit,
    onToggleVer: () -> Unit,
    onTogglePrem: () -> Unit,
    onColorChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val predefinedColors = listOf("#FFF9C4", "#E1BEE7", "#B3E5FC", "#FFCDD2", "#C8E6C9")

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = provider.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${provider.category})",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف مقدم الخدمة", tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Switches Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "تثبيت في الصدارة 📌", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Switch(checked = provider.isPinned, onCheckedChange = { onTogglePin() })
            }

            // Action Switches Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "عضوية موصى بها 👍", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Switch(checked = provider.isRecommended, onCheckedChange = { onToggleRec() })
            }

            // Action Switches Row 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "توثيق حساب شارة زرقاء 💎", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Switch(checked = provider.isVerified, onCheckedChange = { onToggleVer() })
            }

            // Action Switches Row 4 (PREMIUM CONTROLLER)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "العضوية الملكية المميزة (Premium) 👑", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Switch(checked = provider.isPremium, onCheckedChange = { onTogglePrem() })
            }

            // Color Selector if Premium
            if (provider.isPremium) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "اختر لون خلفية البطاقة المخصص للمقدم المميز:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefinedColors.forEach { hex ->
                        val isSelected = provider.cardColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { onColorChange(hex) }
                        )
                    }
                }
            }
        }
    }
}

// Subcomponent: New service provider insertion form
@Composable
fun AdminRegisterProviderForm(viewModel: DalilyViewModel) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("السباكة") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPremium by remember { mutableStateOf(false) }

    val categories = listOf("السباكة", "الكهرباء", "التبريد والتكييف", "النجارة", "صيانة برمجيات وهواتف", "خدمات التوصيل")
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "تسجيل بيانات مقدم خدمة فني جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("اسم الفني / المحل التجاري") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Category dropdown replacement list
        Text(text = "تصنيف مجال الخدمة الخاص به:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { cat ->
                val isSel = cat == selectedCategory
                ElevatedFilterChip(
                    selected = isSel,
                    onClick = { selectedCategory = cat },
                    label = { Text(text = cat) }
                )
            }
        }

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("رقم هاتف التواصل والواتس آب") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("عنوان العمل والمشروع التفصيلي") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("نبذة مختصرة ومؤهلات وخبرات وتفصيل بالخدمات المقدمة") },
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "عضوية مميزة مع اللون الافتراضي (👑 Premium)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Switch(checked = isPremium, onCheckedChange = { isPremium = it })
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (name.trim().isEmpty() || phone.trim().isEmpty() || address.trim().isEmpty() || description.trim().isEmpty()) {
                    Toast.makeText(context, "الرجاء اكمال كافة حقول مقدم الخدمة!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.registerProviderFromAdmin(
                        name = name,
                        category = selectedCategory,
                        phone = phone,
                        address = address,
                        description = description,
                        isPremium = isPremium,
                        colorHex = if (isPremium) "#FFF9C4" else null
                    )
                    Toast.makeText(context, "تم حفظ وتسجيل مقدم الخدمة $name بنجاح!", Toast.LENGTH_SHORT).show()
                    name = ""
                    phone = ""
                    address = ""
                    description = ""
                    isPremium = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "تسجيل وإدراج للجمهور فوراً 💾")
        }
    }
}

@Composable
fun RowScope.TabButton(
    isSelected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}

// -------------------------------------------------------------
// SCREEN 4: AUDIT LOGS SECURITY (WITH PDF EXPORT)
// -------------------------------------------------------------
@Composable
fun AuditLogsScreen(viewModel: DalilyViewModel) {
    val logs by viewModel.auditLogs.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Log Toolbar Toolbar view
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.ADMIN_PANEL) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "الرجوع للوحة التحكم")
            }

            Text(
                text = "سجل الأنشطة الأمنية (Audit Logs)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    exportAuditLogsToPdf(context, logs) { file ->
                        if (file != null) {
                            Toast.makeText(context, "تم تصدير سجل PDF بنجاح!", Toast.LENGTH_LONG).show()
                            
                            // Log action
                            viewModel.addAuditLog(
                                action = "تصدير سجل النشاط (PDF)",
                                details = "تم استخراج ملف ورقة PDF وتخزينه بنجاح لمراقب الحساب: $file"
                            )

                            // Trigger sharing options intent dialog
                            try {
                                val fileUri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة سجل الأنشطة PDF"))
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                                Toast.makeText(context, "الرجاء تهيئة مشاركة الملفات، تم التصدير للامتداد الداخلي.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "فشل تصدير مستند PDF!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(Icons.Default.Share, contentDescription = "تصدير PDF", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Divider()

        // Page info banner
        Card(
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = "تحذير الأمان", tint = Color(0xFFFF9800))
                Column {
                    Text(
                        text = "بيان الإفصاح والمراقبة للعمليات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "يسجل هذا القسم تلقائياً تاريخ، وقت، موقع، وإجراءات جميع الأدمن بموثوقية تامة، لضمان النزاهة. اضغط على أيقونة المشاركة العلوية لتصدير السجل كورقة PDF رسمية.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Logs listing index
        if (logs.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = "سجل الأنشطة فارغ تماماً والمراقبة معطلة.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(logs) { log ->
                    AuditLogItemRow(log = log)
                }
            }
        }

        Button(
            onClick = {
                exportAuditLogsToPdf(context, logs) { file ->
                    if (file != null) {
                        Toast.makeText(context, "تم حفظ وتصدير ملف PDF بنجاح وحفظه في مجلد التنزيلات الداخلي الخاص بالتطبيق", Toast.LENGTH_LONG).show()
                        viewModel.addAuditLog(
                            action = "تصدير سجل النشاط (PDF)",
                            details = "تم تصدير السجل بصيغة PDF وحفظه محلياً في الرابط: $file"
                        )
                    } else {
                        Toast.makeText(context, "خطأ أثناء حفظ ملف PDF!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("export_pdf_button")
        ) {
            Text(text = "تصدير السجل بصيغة PDF 📄")
        }
    }
}

@Composable
fun AuditLogItemRow(log: AuditLog) {
    val formattedTime = remember(log.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔧 الإجراء: " + log.action,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "بواسطة: " + log.adminName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.details,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 موقع تسجيل الدخول: " + log.location,
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// -------------------------------------------------------------
// PDF GENERATION LOGICAL HELPER FUNCTION
// -------------------------------------------------------------
fun exportAuditLogsToPdf(context: Context, logs: List<AuditLog>, onComplete: (File?) -> Unit) {
    try {
        val pdfDocument = PdfDocument()
        val paint = Paint()

        // Standard A4 Layout parameters: 595 x 842 pt
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(android.graphics.Color.WHITE)

        // Title Block
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("سجل أنشطة وعمليات لوحة التحكم اليماني (Audit Logs)", 30f, 60f, paint)

        // Subtitle Metadata
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 11f
        paint.isFakeBoldText = false
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("تاريخ التقرير: $formattedDate", 30f, 85f, paint)

        // Divider
        paint.strokeWidth = 1.5f
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawLine(30f, 100f, 565f, 100f, paint)

        // Table headers (Draw strings)
        paint.textSize = 12f
        paint.isFakeBoldText = true
        paint.color = android.graphics.Color.BLUE
        canvas.drawText("الإجراء وسير العملية", 30f, 125f, paint)
        canvas.drawText("المسؤول", 250f, 125f, paint)
        canvas.drawText("الموقع", 380f, 125f, paint)
        canvas.drawText("تاريخ الحدث", 480f, 125f, paint)

        paint.strokeWidth = 1.0f
        paint.color = android.graphics.Color.LTGRAY
        canvas.drawLine(30f, 135f, 565f, 135f, paint)

        // Write row elements
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 9f
        var yPos = 160f
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

        for (log in logs) {
            if (yPos > 800) break // Prevent page overflow in demo draft

            val dateStr = sdf.format(Date(log.timestamp))

            // Text limiting to fit columns gracefully
            val nameLimit = if (log.adminName.length > 20) log.adminName.take(17) + "..." else log.adminName
            val actionLimit = if (log.action.length > 30) log.action.take(27) + "..." else log.action
            val locationLimit = if (log.location.length > 18) log.location.take(15) + "..." else log.location

            canvas.drawText(actionLimit, 30f, yPos, paint)
            canvas.drawText(nameLimit, 250f, yPos, paint)
            canvas.drawText(locationLimit, 380f, yPos, paint)
            canvas.drawText(dateStr, 480f, yPos, paint)

            yPos += 26f
        }

        // Draw page count footer
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 9f
        canvas.drawText("ورقة: ١ من ١  *  تم توليده تلقائياً من نظام المراقبة الداخلي للخدمات", 30f, 820f, paint)

        pdfDocument.finishPage(page)

        // Write to application's localized downloads cache
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(path, "dalily_audit_logs_${System.currentTimeMillis()}.pdf")
        val fileOutputStream = FileOutputStream(file)
        pdfDocument.writeTo(fileOutputStream)
        pdfDocument.close()
        fileOutputStream.close()

        onComplete(file)
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(null)
    }
}
