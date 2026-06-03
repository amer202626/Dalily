package com.dalily.services.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dalily.services.UserRole
import com.dalily.services.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    currentRole: UserRole,
    onLogout: () -> Unit,
    onRoleChanged: (UserRole, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Core database collect from our local reactive state
    val dbState by FirebaseSimulator.dbState.collectAsState()
    val isSyncing by FirebaseSimulator.syncingState.collectAsState()
    val isWorkManagerActive by FirebaseSimulator.workManagerSyncActive.collectAsState()
    val appConfig = dbState.config

    // Record visitor index
    LaunchedEffect(Unit) {
        FirebaseSimulator.recordVisitor(context)
    }

    // App language selection (Arabic or English)
    var isArabic by remember { mutableStateOf(true) }

    // Navigation and drawer variables
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showBackdoorLoginDialog by remember { mutableStateOf(false) }
    var showProviderRegisterDialog by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }
    
    // Welcome Section components
    val welcomeColor = when (appConfig.welcomeFontColor) {
        "Light Gold" -> Color(0xFFFFDF00)
        "Vibrant Silver" -> Color(0xFFE2E8F0)
        else -> Color.White // Bright White
    }

    // Advanced Filtering States
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryAr by remember { mutableStateOf<String?>(null) }
    var selectedDistrictFilter by remember { mutableStateOf("") }
    var selectedRadiusFilter by remember { mutableStateOf("الكل") } // الكل, 5 كم, 10 كم, 20 كم
    var sliderMaxPriceFilter by remember { mutableStateOf(100f) }

    // Backdoor 5 clicks listener
    var backdoorClicks by remember { mutableStateOf(0) }
    var backdoorPasswordInput by remember { mutableStateOf("") }

    // Simple Input fields for credentials
    var adminUsernameInput by remember { mutableStateOf("") }
    var adminPasswordInput by remember { mutableStateOf("") }

    // Service provider onboarding variables
    var reqFullName by remember { mutableStateOf("") }
    var reqPhone by remember { mutableStateOf("") }
    var reqCategoryAr by remember { mutableStateOf("") }
    var reqPersonalPhotoUrl by remember { mutableStateOf("") }
    var reqWorkAddress by remember { mutableStateOf("") }
    var reqResidenceRegion by remember { mutableStateOf("") }
    var reqLocationOnMap by remember { mutableStateOf(false) }
    var reqIdCardPhotoUrl by remember { mutableStateOf("") }
    var showOnboardingDropdown by remember { mutableStateOf(false) }

    // Infinite list loading state simulation
    var listLimit by remember { mutableStateOf(10) }
    val listScrollState = rememberScrollState()

    // Dynamic banner slider counter
    var activeBannerIndex by remember { mutableStateOf(0) }
    LaunchedEffect(dbState.banners) {
        if (dbState.banners.isNotEmpty()) {
            while (true) {
                delay(5000)
                activeBannerIndex = (activeBannerIndex + 1) % dbState.banners.size
            }
        }
    }

    // Reset backdoor clicks
    LaunchedEffect(backdoorClicks) {
        if (backdoorClicks > 0) {
            delay(3000)
            backdoorClicks = 0
        }
    }

    // Visual detail card popup modal
    var clickedBusinessCard by remember { mutableStateOf<ServiceProvider?>(null) }

    // Web Speech Mic simulation state
    var isMicRecordingSim by remember { mutableStateOf(false) }
    var simulatedSpeechText by remember { mutableStateOf("") }
    var micRecordingCompleted by remember { mutableStateOf(false) }

    // Coordinates for simple distance calculations
    // Base simulation coordinates (Sanaa, Yemen)
    val baseLat = 15.3694
    val baseLng = 44.1910

    fun calculateProximityKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Filter list
    val filteredProviders = dbState.providers.filter { provider ->
        val matchesCategory = selectedCategoryAr == null || provider.category == selectedCategoryAr
        val matchesSearch = provider.name.contains(searchQuery, ignoreCase = true) ||
                provider.description.contains(searchQuery, ignoreCase = true) ||
                provider.phone.contains(searchQuery) ||
                provider.address.contains(searchQuery, ignoreCase = true)

        val matchesDistrict = selectedDistrictFilter.isEmpty() || 
                provider.residenceRegion.contains(selectedDistrictFilter, ignoreCase = true) ||
                provider.address.contains(selectedDistrictFilter, ignoreCase = true)

        val matchesRadius = when (selectedRadiusFilter) {
            "5 كم" -> calculateProximityKm(baseLat, baseLng, provider.lat, provider.lng) <= 5.0
            "10 كم" -> calculateProximityKm(baseLat, baseLng, provider.lat, provider.lng) <= 10.0
            "20 كم" -> calculateProximityKm(baseLat, baseLng, provider.lat, provider.lng) <= 20.0
            else -> true
        }

        matchesCategory && matchesSearch && matchesDistrict && matchesRadius
    }.sortedByDescending { it.isFeatured } // Pinned (Premium) on top

    // Main scaffold container
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                // Top Custom Title Row
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Logo and Title block - click 5 times for backdoor entry
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clickable {
                                        backdoorClicks++
                                        if (backdoorClicks >= 5) {
                                            backdoorClicks = 0
                                            showBackdoorLoginDialog = true
                                        }
                                    }
                                    .testTag("app_title_logo")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (isArabic) "دليل الخدمات" else "Services Guide",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isArabic) "الدليل الأول المتكامل في اليمن" else "The Unified Yemen Services Directory",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Horizontal alignment of dynamic Top Bar Action Icons
                            // Right to Left Order (1: Sync, 2: Lang, 3: AppInfo, 4: AdminLogin, 5: ProviderRegister, 6: BackdoorInfo)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 1. Refresh / Cloud Sync Button
                                val syncIconCnf = appConfig.topBarIcons.find { it.iconKey == "REFRESH" }
                                if (syncIconCnf?.isVisible == true) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val res = FirebaseSimulator.syncWithCloud(context)
                                                Toast.makeText(
                                                    context,
                                                    if (res) "تم تحديث كافة حقول الدليل ومزامنة البيانات سحابياً 🗸" else "لم يتم العثور على تغطية إنترنت سريعة. تعمل دون إنترنت (Offline)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = if (isArabic) syncIconCnf.arabicLabel else syncIconCnf.englishLabel,
                                            tint = if (isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // 2. Language Switch Button
                                val langIconCnf = appConfig.topBarIcons.find { it.iconKey == "LANG" }
                                if (langIconCnf?.isVisible == true) {
                                    IconButton(onClick = { isArabic = !isArabic }) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = if (isArabic) langIconCnf.arabicLabel else langIconCnf.englishLabel,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // 3. Info (عن التطبيق) App Information
                                val infoIconCnf = appConfig.topBarIcons.find { it.iconKey == "INFO" }
                                if (infoIconCnf?.isVisible == true) {
                                    IconButton(onClick = { showInfoDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = if (isArabic) infoIconCnf.arabicLabel else infoIconCnf.englishLabel
                                        )
                                    }
                                }

                                // 4. Admin Gear Control
                                val adminIconCnf = appConfig.topBarIcons.find { it.iconKey == "ADMIN" }
                                if (adminIconCnf?.isVisible == true) {
                                    IconButton(
                                        onClick = {
                                            if (currentRole == UserRole.USER) {
                                                adminUsernameInput = ""
                                                adminPasswordInput = ""
                                                showAdminLoginDialog = true
                                            } else {
                                                navController.navigate("admin_dashboard")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = if (isArabic) adminIconCnf.arabicLabel else adminIconCnf.englishLabel,
                                            tint = if (currentRole != UserRole.USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // 5. User Onboarding Icon (سجل معنا)
                                val regIconCnf = appConfig.topBarIcons.find { it.iconKey == "REGISTER" }
                                if (regIconCnf?.isVisible == true) {
                                    IconButton(onClick = { showProviderRegisterDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = if (isArabic) regIconCnf.arabicLabel else regIconCnf.englishLabel
                                        )
                                    }
                                }

                                // 6. App icon status click indicator
                                val backdoorIconCnf = appConfig.topBarIcons.find { it.iconKey == "BACKDOOR" }
                                if (backdoorIconCnf?.isVisible == true) {
                                    IconButton(onClick = {
                                        Toast.makeText(context, "اضغط 5 مرات متتالية على الشعار لفتح البوابة الخلفية", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "الرمز السرّي",
                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // Syncing status light showing Offline-first state and WorkManager syncs
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSyncing || isWorkManagerActive) Color.Green else Color(0xFF3B82F6))
                            )
                            Text(
                                text = if (isWorkManagerActive) "مزامنة سحابية نشطة بالخلفية (WorkManager).."
                                       else "تخزين محلي مؤمن وملفاتك تعمل في 0 جزء من الثانية Offline-First 🗸",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Scaffold Nav Bottom Bar
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                        label = { Text(if (isArabic) "الرئيسية" else "Home", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("favorites") },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "المفضلة") },
                        label = { Text(if (isArabic) "المفضلة" else "Favorites", fontSize = 10.sp) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("chat_list") },
                        icon = { Icon(Icons.Default.MailOutline, contentDescription = "محادثاتي") },
                        label = { Text(if (isArabic) "محادثاتي" else "Chats", fontSize = 10.sp) }
                    )
                    if (currentRole != UserRole.USER) {
                        NavigationBarItem(
                            selected = false,
                            onClick = { navController.navigate("admin_dashboard") },
                            icon = { Icon(Icons.Default.Build, contentDescription = "الإدارة") },
                            label = { Text(if (isArabic) "الأقسام" else "Admin", fontSize = 10.sp) },
                            modifier = Modifier.testTag("btn_navigation_admin_back")
                        )
                    }
                }

                // Custom dynamic Branding Shrunk scale footer at the very bottom
                if (appConfig.isFooterVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appConfig.footerText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            modifier = Modifier.scale(appConfig.footerScale)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Dynamic Floating button for AI Smart Assistant (المساعد الذكي)
            val assistantColor = try {
                Color(android.graphics.Color.parseColor(appConfig.aiAssistantColor))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.tertiary
            }

            val alignmentOffset = if (appConfig.aiAssistantPosition == "Bottom Left") {
                Modifier.padding(end = 260.dp) // shift to left
            } else Modifier

            ExtendedFloatingActionButton(
                onClick = { showAiAssistantDialog = true },
                containerColor = assistantColor,
                contentColor = Color.Black,
                modifier = alignmentOffset.testTag("ai_assistant_fab"),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Star, contentDescription = "خدمات", modifier = Modifier.size(16.dp))
                if (appConfig.aiAssistantLabelsEnabled) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "خدمات المساعد" else "AI Assistant",
                        fontSize = if (appConfig.aiAssistantSize == "Small") 9.sp else if (appConfig.aiAssistantSize == "Large") 13.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        
        // Main Screen Body with beautiful dark accents
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0F19)), // Enforced dark relaxed background
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Maintenance mode warning sheet
            if (appConfig.isMaintenanceMode) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("وضع الصيانة نشط حالياً ⚠️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("عذراً، يتم تحديث النظام والبيانات الجوهرية الآن. قد يتم إيقاف استقبال الطلبات الفورية مؤقتاً.", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                        }
                    }
                }
            }

            // WELCOME BANNER CARD WITH DYNAMIC SIZES AND CUSTOM COLORS SET BY ADMIN
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Ambient gradient background
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (appConfig.welcomeImage.isNotEmpty()) {
                                AsyncImage(
                                    model = appConfig.welcomeImage,
                                    contentDescription = "Welcome Logo",
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Default stylish app abstract image
                                Image(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .align(Alignment.CenterHorizontally),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
                                )
                            }

                            Text(
                                text = if (isArabic) appConfig.welcomeText else appConfig.welcomeTextEn,
                                fontSize = appConfig.welcomeFontSize.sp,
                                color = welcomeColor,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = if (isArabic) "اكتشف أفضل السباكين والكهربائيين واختصاصيي التبريد الموثقين القريبين منك في اليمن" 
                                       else "Discover verified local technicians, clinics and developers in Yemen easily",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ADVANCED FILTERING CARD
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isArabic) "البحث الدقيق والفلترة المتقدمة" else "Advanced Filter & Search Tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Voice Integration and Mic button next to standard text search
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(if (isArabic) "ابحث بالاسم، التخصص أو المحافظة..." else "Search by name, tags, cities...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("advanced_search_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                )
                            )

                            // Browser Speech simulated microphone API
                            if (appConfig.voiceNotesEnabled) {
                                IconButton(
                                    onClick = {
                                        if (!isMicRecordingSim) {
                                            isMicRecordingSim = true
                                            simulatedSpeechText = "جاري الاستماع لصوتك..."
                                            scope.launch {
                                                delay(2500)
                                                simulatedSpeechText = "أحتاج إلى سباك مياه في صنعاء فوراً"
                                                delay(1000)
                                                searchQuery = "سباك"
                                                selectedDistrictFilter = "صنعاء"
                                                isMicRecordingSim = false
                                                micRecordingCompleted = true
                                                Toast.makeText(context, "تم تحويل الصوت لكلمات وتصنيفه بنجاح!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isMicRecordingSim) Color.Red else MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "تسجيل صوتي",
                                        tint = if (isMicRecordingSim) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Simulated transcription results
                        if (isMicRecordingSim || micRecordingCompleted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = simulatedSpeechText,
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        // Advanced District/City selection and Geolocated distance selection
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedDistrictFilter,
                                onValueChange = { selectedDistrictFilter = it },
                                placeholder = { Text("المحافظة/الحي", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White
                                )
                            )

                            // Geofencing radius circular selector
                            var showRadiusDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = "نطاق: $selectedRadiusFilter",
                                    onValueChange = {},
                                    readOnly = true,
                                    textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                                    trailingIcon = {
                                        IconButton(onClick = { showRadiusDropdown = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = showRadiusDropdown,
                                    onDismissRequest = { showRadiusDropdown = false }
                                ) {
                                    listOf("الكل", "5 كم", "10 كم", "20 كم").forEach { rad ->
                                        DropdownMenuItem(
                                            text = { Text(rad) },
                                            onClick = {
                                                selectedRadiusFilter = rad
                                                showRadiusDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PROMOTED PREMIUM BANNER CAROUSEL (مدار بواسطة الأدمن)
            if (dbState.banners.isNotEmpty()) {
                item {
                    val activeBanner = dbState.banners.getOrNull(activeBannerIndex) ?: dbState.banners.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = activeBanner.imageUrl,
                                contentDescription = activeBanner.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Elegant overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "إعلان مميز 🔥" else "Sponsored Ad",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = activeBanner.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // HORIZONTAL CATEGORIES BAR WITH COUNTERS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "تصفح الأقسام الرئيسية المعتمدة" else "Browse Certified Divisions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryAr == null,
                                onClick = { selectedCategoryAr = null },
                                label = { Text(if (isArabic) "كافة الأقسام" else "All Sectors") },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }

                        items(dbState.categories) { cat ->
                            val providersInCat = dbState.providers.count { it.category == cat.nameAr }
                            FilterChip(
                                selected = selectedCategoryAr == cat.nameAr,
                                onClick = { selectedCategoryAr = cat.nameAr },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isArabic) cat.nameAr else cat.nameEn)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text("$providersInCat", fontSize = 9.sp)
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ThumbUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // VECHILE LOCAL VECTOR MAP PREVIEW (الخارطة الجغرافية)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("خارطة الحِرف والعيادات القريبة باليمن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("متصلة 🌐", fontSize = 9.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Vector Map Simulator Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Map abstract coordinates grid drawing
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw stylized circular grid waves
                                drawCircle(
                                    color = Color.LightGray.copy(alpha = 0.05f),
                                    radius = size.width / 4,
                                    center = center
                                )
                                drawCircle(
                                    color = Color.LightGray.copy(alpha = 0.05f),
                                    radius = size.width / 2,
                                    center = center
                                )
                            }
                            
                            // Draw markers
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                Text("موقعك الحالي (أمانة العاصمة)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // Dynamic Provider markers representation
                            filteredProviders.take(3).forEachIndexed { index, provider ->
                                val offsetMultiplierX = if (index % 2 == 0) 1 else -1
                                val offsetMultiplierY = if (index > 1) 1 else -1
                                Box(
                                    modifier = Modifier
                                        .offset(x = (60 * index * offsetMultiplierX).dp, y = (40 * index * offsetMultiplierY).dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(8.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(provider.name.take(12) + "..", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تحديد البعد ونطاق الدائرة يحسن النتائج المباشرة", fontSize = 9.sp, color = Color.Gray)
                            Button(
                                onClick = {
                                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$baseLat,$baseLng?q=سباك+صنعاء"))
                                    context.startActivity(mapIntent)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("افتح خرائط Google", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // PROVIDERS HEADER BLOCK
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic) "مقدمو الخدمات المتوفرون حالياً" else "Available Service Technicians",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${filteredProviders.size} ${if (isArabic) "حالة معتمدة" else "Verified List"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // SEARCH RESULTS WITH SKELETON LOADERS & REUSABLE CARDS (INFINITE SCROLL)
            if (filteredProviders.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Text("عذراً، لم يتم العثور على مزودين موثوقين يطابقون تصفيتك!", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(filteredProviders.take(listLimit)) { provider ->
                    
                    // REUSABLE BUSINESS CARD COMPONENT (بطاقة النشاط التجاري)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clickedBusinessCard = provider }
                            .testTag("provider_card_${provider.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = if (provider.isFeatured) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar and Verification Badges
                            Box {
                                val placeholderImage = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&q=80&w=300"
                                AsyncImage(
                                    model = if (provider.imageUrl.isEmpty()) placeholderImage else provider.imageUrl,
                                    contentDescription = provider.name,
                                    modifier = Modifier
                                        .size(65.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                if (provider.isVerified) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 4.dp, y = 4.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "موثوق", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = provider.category,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    
                                    if (provider.isFeatured) {
                                        Text(
                                            text = "موصى به 🔥",
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(Color(0xFFFFD700), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = provider.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = provider.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Rating",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = " ${provider.rating} (${provider.reviewsCount})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Address",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = provider.address.take(15) + "..",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Simulate Infinite loading bottom trigger element
                item {
                    if (filteredProviders.size > listLimit) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { listLimit += 5 }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "اضغط لتحميل المزيد من مزودي الخدمات 📂" else "Click to load more items",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. BUSINESS DETAIL DIALOG POPUP MODAL (نافذة تفاصيل النشاط عند النقر)
    if (clickedBusinessCard != null) {
        val provider = clickedBusinessCard!!
        Dialog(onDismissRequest = { clickedBusinessCard = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = provider.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { clickedBusinessCard = null }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    // Photo Carousel/Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        val sampleImg = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&q=80&w=600"
                        AsyncImage(
                            model = if (provider.imageUrl.isEmpty()) sampleImg else provider.imageUrl,
                            contentDescription = provider.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Specifications
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ساعات العمل: " + provider.workHours,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        )
                        Text(
                            text = "المنطقة: " + provider.residenceRegion,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        )
                    }

                    Text(
                        text = provider.description,
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    // Navigation Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                clickedBusinessCard = null
                                navController.navigate("detail/${provider.id}")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تصفح كامل الملف")
                        }

                        IconButton(
                            onClick = {
                                val u = "tel:${provider.phone}"
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(u)))
                                FirebaseSimulator.recordCallEvent(context)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // 2. INFORMATION DIALOG (عن التطبيق)
    if (showInfoDialog) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("عن دليل الخدمات المعتمد ℹ️", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "منصة متكاملة تسهل لك الوصول الفوري لجميع الاختصاصيين والمهن الطبية والحرفية في اليمن. يعمل التطبيق بكفاءة (Offline-First) لسرعة استجابة فائقة.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.LightGray
                    )

                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    // Contact metadata editable by admin
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("البريد الإلكتروني للدعم: ${appConfig.appInfoEmail}", fontSize = 11.sp, color = Color.White)
                        Text("هاتف التواصل المباشر: ${appConfig.appInfoPhone}", fontSize = 11.sp, color = Color.White)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appConfig.appInfoCheckUpdateUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("البحث عن تحديثات", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "قم بتحميل تطبيق دليل الخدمات اليمني من الرابط: ${appConfig.appInfoShareUrl}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("مشاركة الرابط", fontSize = 10.sp)
                        }
                    }

                    OutlinedButton(onClick = { showInfoDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("رجوع")
                    }
                }
            }
        }
    }

    // 3. ADMIN LOGIN DIALOG- BOXES CLEAN BY DEFAULT
    if (showAdminLoginDialog) {
        Dialog(onDismissRequest = { showAdminLoginDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("تسجيل دخول المشرفين المعتمدين ⚙️", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("الرجاء تعبئة حقول الدخول المدونة مسبقاً:", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    // Name Input
                    OutlinedTextField(
                        value = adminUsernameInput,
                        onValueChange = { adminUsernameInput = it },
                        placeholder = { Text("اسم مستخدم المشرف") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_user_input_box"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Password Input
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        placeholder = { Text("الرمز السري الخاص بك") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_pass_input_box"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val found = dbState.admins.find {
                                    it.username == adminUsernameInput && it.passwordHash == adminPasswordInput && it.isActive
                                }
                                if (found != null) {
                                    val r = if (found.role == "owner") UserRole.OWNER else UserRole.ADMIN
                                    onRoleChanged(r, found.username)
                                    showAdminLoginDialog = false
                                    Toast.makeText(context, "مرحباً يا ${found.username}، تم تسجيل الدخول بنجاح!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("admin_dashboard")
                                } else {
                                    Toast.makeText(context, "حساب المشرف غير متوفر أو كلمة السر غير مطابقة!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تسجيل الدخول", fontSize = 11.sp)
                        }

                        OutlinedButton(onClick = { showAdminLoginDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 4. BACKDOOR CODE LOGIN DIALOG (الدخول الخفي)
    if (showBackdoorLoginDialog) {
        Dialog(onDismissRequest = { showBackdoorLoginDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("بوابة الدخول السرية للمطور والمالك 👑", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("رمز الدخول السري الماستر:", fontSize = 11.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = backdoorPasswordInput,
                        onValueChange = { backdoorPasswordInput = it },
                        placeholder = { Text("رمز الدخول الخفي") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backdoor_input_box"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (backdoorPasswordInput == "maher--736462") {
                                    onRoleChanged(UserRole.OWNER, "المطور ماهر")
                                    showBackdoorLoginDialog = false
                                    backdoorPasswordInput = ""
                                    Toast.makeText(context, "تم الولوج بنجاح برتبة المالك الكلي!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("admin_dashboard")
                                } else {
                                    Toast.makeText(context, "خطأ بالرقم السري الماستر!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("أدخل")
                        }

                        OutlinedButton(onClick = { showBackdoorLoginDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // 5. REGISTRATION ONBOARDING REQUEST FOR CUSTOMERS/TECHNICIANS
    if (showProviderRegisterDialog) {
        Dialog(onDismissRequest = { showProviderRegisterDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("تقديم طلب إدراج مهني بالدليل 📝", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("عبئ الحقول المحددة التالية للتسجيل الفوري:", fontSize = 11.sp, color = Color.Gray)

                    // الاسم الثلاثي - اجباري
                    OutlinedTextField(
                        value = reqFullName,
                        onValueChange = { reqFullName = it },
                        placeholder = { Text("الاسم الثلاثي (إجباري) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // رقم الهاتف - اجباري
                    OutlinedTextField(
                        value = reqPhone,
                        onValueChange = { reqPhone = it },
                        placeholder = { Text("رقم الهاتف (إجباري) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // اختيار نوع القسم - اجباري
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (reqCategoryAr.isEmpty()) "اختر القسم الرئيسي (إجباري) *" else reqCategoryAr,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = TextStyle(color = Color.White),
                            trailingIcon = {
                                IconButton(onClick = { showOnboardingDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = showOnboardingDropdown,
                            onDismissRequest = { showOnboardingDropdown = false }
                        ) {
                            dbState.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.nameAr) },
                                    onClick = {
                                        reqCategoryAr = cat.nameAr
                                        showOnboardingDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // الصورة الشخصية - اجباري
                    OutlinedTextField(
                        value = reqPersonalPhotoUrl,
                        onValueChange = { reqPersonalPhotoUrl = it },
                        placeholder = { Text("رابط الصورة الشخصية (إجباري) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // مكان العمل الحالي - اجباري
                    OutlinedTextField(
                        value = reqWorkAddress,
                        onValueChange = { reqWorkAddress = it },
                        placeholder = { Text("عنوان مركز العمل الحالي (إجباري) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // منطقة الإقامة - اجباري
                    OutlinedTextField(
                        value = reqResidenceRegion,
                        onValueChange = { reqResidenceRegion = it },
                        placeholder = { Text("منطقة السكن والإقامة (إجباري) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // خريطة موقعة - اختياري
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ربط إحداثيات الخريطة (اختياري)", fontSize = 11.sp, color = Color.LightGray)
                        Switch(
                            checked = reqLocationOnMap,
                            onCheckedChange = { reqLocationOnMap = it }
                        )
                    }

                    // بطاقة الهوية - اختياري
                    OutlinedTextField(
                        value = reqIdCardPhotoUrl,
                        onValueChange = { reqIdCardPhotoUrl = it },
                        placeholder = { Text("رابط صورة بطاقة الهوية (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (reqFullName.isEmpty() || reqPhone.isEmpty() || reqCategoryAr.isEmpty() || 
                                    reqPersonalPhotoUrl.isEmpty() || reqWorkAddress.isEmpty() || reqResidenceRegion.isEmpty()) {
                                    Toast.makeText(context, "الرجاء ملء كافة الحقول الإجبارية المؤشرة بنجمة!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newReq = ServiceRequest(
                                        id = "req_${System.currentTimeMillis()}",
                                        providerName = reqFullName,
                                        category = reqCategoryAr,
                                        phone = reqPhone,
                                        description = "مقدم خدمة مسجل بالنموذج المهني الموحد للتطبيق.",
                                        profileImageUrl = reqPersonalPhotoUrl,
                                        idCardUrl = reqIdCardPhotoUrl,
                                        workAddress = reqWorkAddress,
                                        residenceRegion = reqResidenceRegion,
                                        lat = baseLat + (Math.random() - 0.5) / 10.0,
                                        lng = baseLng + (Math.random() - 0.5) / 10.0
                                    )
                                    FirebaseSimulator.addServiceRequest(context, newReq)
                                    showProviderRegisterDialog = false
                                    Toast.makeText(context, "تم تقديم طلبك بنجاح. سيتم مراجعته من المشرفين في لحظات!", Toast.LENGTH_LONG).show()
                                    
                                    // Reset fields
                                    reqFullName = ""
                                    reqPhone = ""
                                    reqCategoryAr = ""
                                    reqPersonalPhotoUrl = ""
                                    reqWorkAddress = ""
                                    reqResidenceRegion = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رفع الطلب")
                        }

                        OutlinedButton(onClick = { showProviderRegisterDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
    }

    // 6. AI SUPPORT CHAT DIALOGUE MODAL (المساعد الذكي)
    if (showAiAssistantDialog) {
        var assistantInputMsg by remember { mutableStateOf("") }
        val promptLogs = remember { mutableStateListOf<Pair<String, Boolean>>() } // message to isFromUser
        
        if (promptLogs.isEmpty()) {
            promptLogs.add(Pair("مرحباً بك في المساعد الذكي لمؤسسات اليمن المعتمدة! كيف أستطيع خدمتك اليوم؟ يمكنك سؤالي عن السباكين المتوفرين أو الكشف السريع.", false))
        }

        Dialog(onDismissRequest = { showAiAssistantDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .height(380.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مساعدك الذكي الفوري 🤖", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        IconButton(onClick = { showAiAssistantDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    // Message history list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(promptLogs) { log ->
                            val align = if (log.second) Alignment.End else Alignment.Start
                            val containerCol = if (log.second) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray
                            val textCol = if (log.second) Color.Black else Color.White
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(align)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = containerCol,
                                    modifier = Modifier.widthIn(max = 210.dp)
                                ) {
                                    Text(
                                        text = log.first,
                                        fontSize = 11.sp,
                                        color = textCol,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input message row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = assistantInputMsg,
                            onValueChange = { assistantInputMsg = it },
                            placeholder = { Text("اسأل المساعد عن أي تخصص المهني...", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        IconButton(
                            onClick = {
                                if (assistantInputMsg.trim().isNotEmpty()) {
                                    val input = assistantInputMsg
                                    promptLogs.add(Pair(input, true))
                                    assistantInputMsg = ""
                                    
                                    // Intelligent trigger keywords responses (Works fully offline)
                                    scope.launch {
                                        delay(1000)
                                        val responseText = when {
                                            input.contains("سباك") || input.contains("سباكة") -> 
                                                "لدينا السباك عادل في صنعاء شارع الستين الموثوق، تواصل معه عبر الرقم: 771234567"
                                            input.contains("طبيب") || input.contains("عيادة") || input.contains("أطفال") -> 
                                                "يتوفر حالياً عيادة الدكتور ماجد لطب الأطفال في عدن المنصورة، للمراسلة: 733445566"
                                            input.contains("دليلي") || input.contains("خدمات") -> 
                                                "أنا أساعدك في العثور الفوري وتصفية الأقسام للخدمات وتحديد مواقعهم بدقة!"
                                            else -> "عذراً، لم أفهم استفسارك تماماً. يمكنك البحث عن سباك أو كهربائي أو دكتور باستخدام محرك البحث المتقدم أيضاً!"
                                        }
                                        promptLogs.add(Pair(responseText, false))
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
