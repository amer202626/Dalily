package com.dalily.services.ui.screens

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dalily.services.data.BannerAd
import com.dalily.services.data.FirebaseSimulator
import com.dalily.services.data.ServiceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToProvider: (String) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToChatsList: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    isGuest: Boolean,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Database state subscriptions
    val providers by FirebaseSimulator.providers.collectAsState()
    val banners by FirebaseSimulator.banners.collectAsState()
    val systemSettings by FirebaseSimulator.systemSettings.collectAsState()
    val blockedProviders by FirebaseSimulator.blockedProviders.collectAsState()
    val blockedUsers by FirebaseSimulator.blockedUsers.collectAsState()

    // UI Search state filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var radiusFilterKm by remember { mutableStateOf<Int?>(null) } // null means all
    var minRatingFilter by remember { mutableStateOf(0.0f) }

    // Loading & infinite scroll simulations
    var isFilteringLoading by remember { mutableStateOf(false) }
    var visibleItemCount by remember { mutableStateOf(4) }
    val isDarkTheme = isSystemInDarkTheme()

    // Tracking section visits for Recommendation algorithm
    val sectionClicks = remember { mutableStateMapOf<String, Int>() }
    var recommendedCategory by remember { mutableStateOf<String?>(null) }

    // Guest login prompt popup dialog
    var showGuestBlockDialog by remember { mutableStateOf(false) }

    // First time Tour states
    var tourStep by remember { mutableStateOf(if (systemSettings.isTourEnabled) 1 else 0) }

    // Location Simulation Sana'a center
    val myLat = 15.3533
    val myLng = 44.2078

    // Voice recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val voiceText = results[0]
                searchQuery = voiceText
                Toast.makeText(context, "البحث عن: $voiceText", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun startVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث باسم الخدمة أو الكلمة المفتاحية...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "البحث الصوتي غير متاح حالياً", Toast.LENGTH_SHORT).show()
        }
    }

    // Dynamic distance calculation helper (Haversine formula in Km)
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Filtered providers mapping with blocklists and filters applied
    val allowedProviders = remember(providers, blockedProviders, searchQuery, selectedCategory, selectedTag, radiusFilterKm, minRatingFilter) {
        providers.filter { p ->
            !blockedProviders.contains(p.id) &&
            (selectedCategory == null || p.category == selectedCategory) &&
            (selectedTag == null || p.tags.contains(selectedTag)) &&
            (p.rating >= minRatingFilter) &&
            (searchQuery.isEmpty() || p.name.contains(searchQuery, ignoreCase = true) || p.description.contains(searchQuery, ignoreCase = true) || p.tags.any { t -> t.contains(searchQuery, ignoreCase = true) }) &&
            (radiusFilterKm == null || calculateDistanceKm(myLat, myLng, p.latitude, p.longitude) <= radiusFilterKm!!)
        }
    }

    // Recommendation logic update: find category user clicked most
    LaunchedEffect(sectionClicks.keys.size) {
        val maxCat = sectionClicks.maxByOrNull { it.value }
        if (maxCat != null && maxCat.value > 0) {
            recommendedCategory = maxCat.key
        }
    }

    // Simulated skeleton trigger upon filter changes
    LaunchedEffect(selectedCategory, selectedTag, radiusFilterKm, searchQuery) {
        isFilteringLoading = true
        delay(600) // 600ms responsive skeleton layout delay
        isFilteringLoading = false
    }

    // Theme Background Gradient
    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(listOf(Color(0xFF090D16), Color(0xFF131A2A)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0)))
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "دليل الخدمات الأول",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Administrative action & guest buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                sectionClicks["الدردشات"] = (sectionClicks["الدردشات"] ?: 0) + 1
                                if (isGuest) {
                                    showGuestBlockDialog = true
                                } else {
                                    onNavigateToChatsList()
                                }
                            },
                            modifier = Modifier.testTag("chats_nav_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "قناة الرسائل المعلقة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = onNavigateToFavorites,
                            modifier = Modifier.testTag("favorites_nav_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "المفضلة",
                                tint = Color.Red
                            )
                        }

                        IconButton(
                            onClick = onNavigateToAdmin,
                            modifier = Modifier.testTag("admin_panel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "لوحة تحكم المشرفين",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isGuest) {
                            Button(
                                onClick = onLoginClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .testTag("home_login_button")
                            ) {
                                Text("تسجيل الدخول", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Welcome Greetings Card with dynamic details
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                val greetingName = if (isGuest) "زائرنا الكريم" else FirebaseSimulator.currentUserName
                                Text(
                                    text = "مرحباً بك، $greetingName 👋",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isGuest) "سجل الآن لتتمكن من حجز المواعيد والدردشة مع الفنيين" else "ابحث عن أفضل مقدمي الخدمات الفنية والرعاية في اليمن",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Banner Advertisements (Dynamic sliding control panels synced with Firebase)
                if (banners.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "خدمات مميزة وعروض رائعة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                            // Interactive scrolling carousels
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val activeBanners = banners.filter { b -> b.isActive }
                                items(activeBanners) { banner ->
                                    Box(
                                        modifier = Modifier
                                            .width(if (banner.size == "Large") 320.dp else if (banner.size == "Small") 200.dp else 260.dp)
                                            .height(if (banner.size == "Large") 130.dp else if (banner.size == "Small") 90.dp else 110.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                if (banner.redirectUrl.isNotEmpty()) {
                                                    scope.launch {
                                                        FirebaseSimulator.logAdminAction(context, "الضغط على لافتة إعلانية مبرمجة للتحويل إلى ${banner.redirectUrl}")
                                                        onNavigateToProvider(banner.redirectUrl)
                                                    }
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = banner.imageUrl,
                                            contentDescription = "لافتة إعلانية",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Badge marker
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (banner.isPinned) "مثبت 📌" else "إعلان",
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Search parameters and Filters toolbar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                    ) {
                        // Search bar input
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("ابحث عن سباك، كهربائي، طبيب...", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_input")
                                    .clip(RoundedCornerShape(12.dp)),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح")
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Custom voice search button
                            IconButton(
                                onClick = { startVoiceSearch() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("voice_search_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "البحث بالصوت", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Advanced Geographic Radius Search ("Near Me" controller)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = "قرب الموقع الجغرافي", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نطاق القرب الجغرافي (على الخريطة):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            // Radius buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(5, 15, 30).filter { it <= systemSettings.maxRadiusSearchKm }.forEach { km ->
                                    val isSelected = radiusFilterKm == km
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { radiusFilterKm = if (isSelected) null else km }
                                            .testTag("radius_${km}km_button"),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$km كم",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Horizontal Categories card view (With Service provider quantity count)
                item {
                    Column {
                        Text(
                            text = "تصفح الأقسام والخدمات الرئيسية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(FirebaseSimulator.categories) { (catKey, catArName) ->
                                val isSelected = selectedCategory == catArName
                                val count = providers.filter { it.category == catArName && !blockedProviders.contains(it.id) }.size

                                val categoryColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedCategory = if (isSelected) null else catArName
                                            // Analytics section visit tracker
                                            sectionClicks[catArName] = (sectionClicks[catArName] ?: 0) + 1
                                        }
                                        .testTag("category_card_$catKey"),
                                    color = categoryColor,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (catKey) {
                                                "S_PLUMBING" -> Icons.Default.WaterDrop
                                                "S_ELECTRICITY" -> Icons.Default.FlashOn
                                                "S_CLINIC" -> Icons.Default.LocalHospital
                                                "S_AC" -> Icons.Default.AcUnit
                                                "S_LOGISTICS" -> Icons.Default.LocalShipping
                                                "S_TUTORING" -> Icons.Default.School
                                                "S_MAINTENANCE" -> Icons.Default.Build
                                                "S_CHEF" -> Icons.Default.Restaurant
                                                else -> Icons.Default.Star
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = catArName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$count خدمة متاحة",
                                                fontSize = 8.sp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Smart RECOMMENDATION section ("Recommended for you" algorithms)
                if (recommendedCategory != null) {
                    val recommendedList = providers.filter {
                        it.category == recommendedCategory &&
                        !blockedProviders.contains(it.id) &&
                        it.rating >= 4.0
                    }.take(3)

                    if (recommendedList.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Icon(Icons.Default.Recommend, contentDescription = "ترشيحات ذكية لك", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "مقترح لك (حسب تصفحك لقسم $recommendedCategory):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(recommendedList) { p ->
                                        Card(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .clickable { onNavigateToProvider(p.id) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                AsyncImage(
                                                    model = p.imageUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(70.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(p.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                                                    Text(" ${p.rating} (${p.reviewsCount} تقييم)", fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Sub-Tags Filters list (fast selection badges like "أسعار اقتصادية", "خدمة سريعة")
                item {
                    val distinctTags = providers.flatMap { it.tags }.distinct()
                    Text(
                        text = "فلترة سريعة بالوسوم:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        distinctTags.forEach { tag ->
                            val isSelected = selectedTag == tag
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedTag = if (isSelected) null else tag }
                                    .testTag("tag_filter_$tag"),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocalActivity, contentDescription = null, size = 11.dp, tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = tag,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Dynamic list representation & Skeleton loader screen
                item {
                    Text(
                        text = "مقدمو الخدمات المتاحة (${allowedProviders.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
                    )
                }

                if (isFilteringLoading) {
                    // Smart Skeleton screen animations
                    items(3) {
                        SkeletonPlaceholderItem()
                    }
                } else if (allowedProviders.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("لم يتم العثور على أي نتائج فنية تتطابق مع معايير البحث والخيارات الفلترية المستخدمةا.", textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                } else {
                    // Show lazy content with Infinite Scroll pagination representation
                    val pagedItems = allowedProviders.take(visibleItemCount)
                    items(pagedItems) { provider ->
                        val distanceVal = calculateDistanceKm(myLat, myLng, provider.latitude, provider.longitude)
                        ProviderItemCard(
                            provider = provider,
                            simulatedDistanceKm = distanceVal,
                            onItemClick = {
                                sectionClicks[provider.category] = (sectionClicks[provider.category] ?: 0) + 1
                                onNavigateToProvider(provider.id)
                            },
                            dataSavingMode = systemSettings.dataSavingMode
                        )
                    }

                    // Infinite Scroll button trigger
                    if (visibleItemCount < allowedProviders.size) {
                        item {
                            Button(
                                onClick = { 
                                    scope.launch {
                                        isFilteringLoading = true
                                        delay(400)
                                        visibleItemCount += 4
                                        isFilteringLoading = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .testTag("load_more_button")
                            ) {
                                Text("عرض المزيد من النتائج (التمرير اللانهائي) 🔄", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // 8. INTERACTIVE TUTORIAL OVERLAY (First time walkthrough highlights)
            if (tourStep in 1..4) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { /* Block underlying touches */ }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (tourStep) {
                                    1 -> Icons.Default.Mic
                                    2 -> Icons.Default.MyLocation
                                    3 -> Icons.Default.Recommend
                                    else -> Icons.Default.AdminPanelSettings
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = when (tourStep) {
                                    1 -> "ميزة البحث بالصوت 🎙️"
                                    2 -> "تصفية النطاق الجغرافي 📍"
                                    3 -> "الترشيحات الذكية 🔮"
                                    else -> "لوحة تحكم المشرفين الكاملة 🔐"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (tourStep) {
                                    1 -> "دليلنا يدعم البحث الصوتي السريع، يمكنك فقط الضغط على الأيقونة الميكروفونية وقول تخصص الخدمة بالعربية اليمني!"
                                    2 -> "اختر النطاق القربي بالكم (٥ كم، ١٥ كم...)، ليقوم النظام بتحديث وعرض الفنيين الذين تنحصر مواقعهم بالقرب لبيتكم!"
                                    3 -> "خوارزمية ذكية مدمجة تدرس تحركاتك ومعدل نقراتك بالأبواب والقطاعات، وتفرز لك توصيات مخصصة ترتقي ببحثك!"
                                    else -> "للآدمن لوحة فخمة متكاملة يمكن من خلالها جدولة تنبيهات FCM، رصد بلاغات مستديمة، تصدير ملفات CSV وترتيب واجهات!"
                                },
                                overflow = TextOverflow.Clip,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { tourStep = 0 },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تخطي كل الرحلة", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { tourStep += 1 },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (tourStep == 4) "بدء التصفح الفعلي 🚀" else "التالي", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Guest popup blocks
            if (showGuestBlockDialog) {
                AlertDialog(
                    onDismissRequest = { showGuestBlockDialog = false },
                    title = { Text("تسجيل الحساب مطلوب 🔏", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = { Text("يسعدنا انضمامك! لكن ميزة التواصل المباشر مع مقدمي الخدمة وحجز مواعيد فنية تقتصر فقط على حسابات المستخدمين الموثقين. نأمل إنشاء حساب فوري.", fontSize = 12.sp, lineHeight = 18.sp) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showGuestBlockDialog = false
                                onLoginClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("تسجيل دخول / إنشاء حساب")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showGuestBlockDialog = false }) {
                            Text("إلغاء والتصفح كزائر")
                        }
                    }
                )
            }
        }
    }
}

// --- COMPOSE SUB-WIDGET COMPONENTS ---

@Composable
fun ProviderItemCard(
    provider: ServiceProvider,
    simulatedDistanceKm: Double,
    onItemClick: () -> Unit,
    dataSavingMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("provider_card_${provider.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (dataSavingMode) 100.dp else 140.dp)
            ) {
                // Compression or layout adjustment in Data Saving Mode
                AsyncImage(
                    model = provider.imageUrl,
                    contentDescription = provider.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top badges strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Online/Offline credibility indicators
                    Surface(
                        color = if (provider.online) Color(0xFF10B981) else Color(0xFF6B7280),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (provider.online) "نشط الآن 🟢" else "غير متصل 🔴",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Popularity point marker
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, size = 8.dp, tint = Color.White)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${provider.views} نقطة شعبية",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // If Verified, add beautiful banner badge overlay
                if (provider.verified) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color(0xFF0D9488), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = "موثوق", size = 10.dp, tint = Color.White)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("موثق بالدليل 🗸", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = provider.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = provider.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = provider.subcategory,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = provider.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${provider.rating} (${provider.reviewsCount} تقييم)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Geographic Distance tags computed in real time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "يبعد حوالي ${String.format("%.1f", simulatedDistanceKm)} كم",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonPlaceholderItem() {
    val infiniteTransition = rememberInfiniteTransition()
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = alphaAnim))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Simulated ImageView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Simulated title field
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(16.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Simulated description field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            )
        }
    }
}

// Icon helper wrapper
@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: Dp, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}
