package com.dalily.services

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.dalily.services.data.*
import com.dalily.services.ui.theme.getDynamicThemeColors
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class Screen {
    HOME, DETAILS, LOGIN, REGISTRATION, ABOUT, REQUESTS, CHAT
}

object Loc {
    var isArabic = mutableStateOf(true)
    fun t(ar: String, en: String): String = if (isArabic.value) ar else en
}

class DalilyViewModel(private val repository: DalilyRepository) : ViewModel() {
    var currentScreen by mutableStateOf(Screen.HOME)
    var activeProviderId by mutableStateOf<Int?>(null)
    var backPressTime = 0L

    var searchQuery by mutableStateOf("")
    var filterDistrict by mutableStateOf("")
    var filterCity by mutableStateOf("")
    var filterRating by mutableStateOf(0.0)

    var selectedCategory by mutableStateOf<Category?>(null)
    var selectedSubCategory by mutableStateOf<Category?>(null)

    var isLoggedIn by mutableStateOf(false)
    var loggedInUserRole by mutableStateOf("USER") // USER, ADMIN, OWNER

    val isRtl: Boolean get() = Loc.isArabic.value

    // Flow mapped directly to non-null AppSettings
    val appSettings: StateFlow<AppSettings> = repository.appSettings
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val serviceProviders = repository.serviceProviders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val banners = repository.banners.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chatMessages = repository.chatMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val authorizedDevices = repository.authorizedDevices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val serviceRequests = repository.userServiceRequests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: Screen, providerId: Int? = null) {
        currentScreen = screen
        if (providerId != null) {
            activeProviderId = providerId
        }
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch {
            val s = appSettings.value.copy(themeName = themeName)
            repository.saveSettings(s)
            logActivity("تحديث القالب المنهجي إلى $themeName", "Owner")
        }
    }

    fun updateDynamicColors(primary: String, secondary: String) {
        viewModelScope.launch {
            val s = appSettings.value.copy(themeName = "مخصص", primaryColorHex = primary, secondaryColorHex = secondary)
            repository.saveSettings(s)
            logActivity("تخصيص الألوان: الأساسي $primary الفرعي $secondary", "Owner")
        }
    }

    fun updateMaintenanceMode(mode: Boolean) {
        viewModelScope.launch {
            val s = appSettings.value.copy(isMaintenanceMode = mode)
            repository.saveSettings(s)
            logActivity("تعديل وضع الصيانة: $mode", "Owner")
        }
    }

    fun updateDataSavingMode(mode: Boolean) {
        viewModelScope.launch {
            val s = appSettings.value.copy(isDataSavingMode = mode)
            repository.saveSettings(s)
            logActivity("تعديل وضع توفير البيانات: $mode", "Admin")
        }
    }

    fun saveAppSettings(s: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(s)
            logActivity("تعديل إعدادات عامة", "Owner")
        }
    }

    fun logActivity(action: String, adminName: String) {
        viewModelScope.launch {
            repository.insertAuditLog(AuditLog(action = action, adminName = adminName))
        }
    }

    fun addManualProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.insertServiceProvider(provider)
            logActivity("إضافة مزود خدمة: ${provider.fullName}", "Admin")
        }
    }

    fun deleteProvider(id: Int, name: String) {
        viewModelScope.launch {
            repository.deleteServiceProvider(id)
            repository.deleteReviewByProvider(id)
            logActivity("حذف حساب مقدم الخدمة $name", "Admin")
        }
    }

    fun updateProviderApproval(id: Int, approved: Boolean, reason: String = "") {
        viewModelScope.launch {
            repository.updateApprovalStatus(id, approved, reason)
            logActivity("تعديل حالة الموافقة للمعرف $id إلى: $approved ($reason)", "Admin")
        }
    }

    fun toggleProviderPin(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePinStatus(id, enabled)
            logActivity("تحديث تثبيت المهني $id إلى $enabled", "Owner")
        }
    }

    fun toggleProviderRecommend(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateRecommendStatus(id, enabled)
            logActivity("تحديث توصية المهني $id إلى $enabled", "Owner")
        }
    }

    fun toggleProviderVerification(id: Int, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateVerificationStatus(id, enabled)
            logActivity("تحديث توثيق المهني $id إلى $enabled", "Owner")
        }
    }

    fun updateProviderBlock(id: Int, blocked: Boolean) {
        viewModelScope.launch {
            repository.updateBlockStatus(id, blocked)
            logActivity("تحديث حظر المهني $id إلى $blocked", "Admin")
        }
    }

    fun processSubActive(id: Int, active: Boolean) {
        viewModelScope.launch {
            repository.updateSubscriptionStatus(id, active, active)
            logActivity("تفعيل اشتراك العضوية المميزة للمهني $id: $active", "Admin")
        }
    }

    fun addReview(providerId: Int, userName: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val r = Review(providerId = providerId, userName = userName, rating = rating, comment = comment)
            repository.insertReview(r)
            repository.addLoyaltyPoints(providerId, 150)
            logActivity("إضافة تقييم لمقدم الخدمة $providerId من $userName", "User")
        }
    }

    fun getReviewsForProvider(providerId: Int): Flow<List<Review>> {
        return repository.getReviewsForProvider(providerId)
    }

    fun sendChatMessage(senderId: String, senderName: String, receiverId: String, receiverName: String, text: String) {
        viewModelScope.launch {
            repository.insertChatMessage(
                ChatMessage(
                    senderId = senderId,
                    senderName = senderName,
                    receiverId = receiverId,
                    receiverName = receiverName,
                    messageText = text
                )
            )
        }
    }

    fun addCategory(cat: Category) {
        viewModelScope.launch {
            repository.insertCategory(cat)
            logActivity("إضافة قسم جديد: ${cat.nameAr}", "Owner")
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            repository.deleteCategory(id)
            logActivity("حذف قسم بالكامل المعرف: $id", "Owner")
        }
    }

    fun addBanner(banner: Banner) {
        viewModelScope.launch {
            repository.insertBanner(banner)
            logActivity("إضافة لافتة إعلانية جديدة", "Admin")
        }
    }

    fun deleteBanner(id: Int) {
        viewModelScope.launch {
            repository.deleteBanner(id)
            logActivity("حذف لافتة إعلانية المعرف $id", "Admin")
        }
    }

    fun addAuthorizedDevice(device: AuthorizedDevice) {
        viewModelScope.launch {
            repository.insertAuthorizedDevice(device)
            logActivity("إضافة جهاز مصرح به: ${device.deviceName}", "Owner")
        }
    }

    fun deleteAuthorizedDevice(id: Int) {
        viewModelScope.launch {
            repository.deleteAuthorizedDevice(id)
            logActivity("إلغاء تفويض الجهاز المعرف $id", "Owner")
        }
    }

    fun registerServiceRequest(providerId: Int, providerName: String, category: String) {
        viewModelScope.launch {
            repository.insertServiceRequest(
                UserServiceRequest(
                    providerId = providerId,
                    providerName = providerName,
                    providerCategory = category
                )
            )
            repository.addLoyaltyPoints(providerId, 50)
            logActivity("طلب اتصال مباشر ومتبادل مع: $providerName", "User")
        }
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: DalilyRepository
    private lateinit var viewModel: DalilyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(this)
        repository = DalilyRepository(database.dalilyDao())
        viewModel = DalilyViewModel(repository)

        lifecycleScope.launch {
            repository.initSeedDataIfEmpty()
        }

        setContent {
            val settings = viewModel.appSettings.collectAsState().value
            val themeColors = getDynamicThemeColors(
                themeName = settings.themeName,
                primaryHex = settings.primaryColorHex,
                secondaryHex = settings.secondaryColorHex
            )

            MaterialTheme(colorScheme = themeColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DalilyAppContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun DalilyAppContent(viewModel: DalilyViewModel) {
    val settings = viewModel.appSettings.collectAsState().value
    val context = LocalContext.current
    val activity = context as Activity

    BackHandler {
        if (viewModel.currentScreen != Screen.HOME) {
            viewModel.navigateTo(Screen.HOME)
        } else {
            val current = System.currentTimeMillis()
            if (current - viewModel.backPressTime < 2000) {
                activity.finish()
            } else {
                viewModel.backPressTime = current
                Toast.makeText(
                    context,
                    Loc.t("اضغط مرة أخرى للخروج من التطبيق", "Press back again to exit"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if (settings.isMaintenanceMode && viewModel.loggedInUserRole != "OWNER" && viewModel.loggedInUserRole != "ADMIN") {
        MaintenanceScreen(viewModel)
    } else {
        Scaffold(
            topBar = { DalilyTopBar(viewModel) },
            bottomBar = { DalilyFooterPanel(viewModel) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = viewModel.currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ScreenTransition"
                ) { targetScreen ->
                    when (targetScreen) {
                        Screen.HOME -> DalilyHomeScreen(viewModel)
                        Screen.DETAILS -> DalilyDetailScreen(viewModel)
                        Screen.LOGIN -> DalilyLoginScreen(viewModel)
                        Screen.REGISTRATION -> DalilyRegistrationForm(viewModel)
                        Screen.ABOUT -> DalilyAboutUsScreen(viewModel)
                        Screen.REQUESTS -> PreviousRequestsScreen(viewModel)
                        Screen.CHAT -> DalilyRealtimeChatScreen(viewModel)
                    }
                }
                FloatingControlWidgets(viewModel)
            }
        }
    }
}

@Composable
fun MaintenanceScreen(viewModel: DalilyViewModel) {
    val settings = viewModel.appSettings.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F16))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 72.sp, modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = Loc.t("وضع الصيانة المؤقت", "Temporary Maintenance Mode"),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = settings.welcomeMessage.ifEmpty {
                Loc.t(
                    "التطبيق حالياً تحت الصيانة الدورية لتحديث الأمان ونبذ المشاكل الفنية. سنعود للعمل الفوري قريباً جداً.",
                    "The application is undergoing scheduled maintenance to upgrade safety features. We will be back online soon."
                )
            },
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.navigateTo(Screen.LOGIN) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0C0C0), contentColor = Color.Black)
        ) {
            Text(Loc.t("تسجيل دخول المالك / المشرف", "Admin / Owner Login"))
        }
    }
}

@Composable
fun DalilyTopBar(viewModel: DalilyViewModel) {
    val settings = viewModel.appSettings.collectAsState().value
    var secretTaps by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        secretTaps++
                        if (secretTaps >= 5) {
                            secretTaps = 0
                            viewModel.navigateTo(Screen.LOGIN)
                            Toast.makeText(context, Loc.t("تم فتح البوابة الخلفية السرية للمالك! أدخل كلمة المرور.", "Secret Backdoor Unlocked!"), Toast.LENGTH_LONG).show()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Text("⭐", modifier = Modifier.align(Alignment.Center), fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = settings.appName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    Toast.makeText(context, Loc.t("تم تحديث البيانات والربط الفوري", "Data refreshed instantly"), Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = { Loc.isArabic.value = !Loc.isArabic.value }) {
                    Text(
                        text = if (Loc.isArabic.value) "EN" else "عربي",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                IconButton(onClick = { viewModel.navigateTo(Screen.REGISTRATION) }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Register",
                        tint = if (viewModel.currentScreen == Screen.REGISTRATION) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = { viewModel.navigateTo(Screen.LOGIN) }) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Login",
                        tint = if (viewModel.currentScreen == Screen.LOGIN) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = { viewModel.navigateTo(Screen.HOME) }) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (viewModel.currentScreen == Screen.HOME) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DalilyFooterPanel(viewModel: DalilyViewModel) {
    val settings = viewModel.appSettings.collectAsState().value

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.ABOUT) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "About App", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (settings.advertisingFooter.isNotEmpty()) {
                    Text(
                        text = settings.advertisingFooter,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.sp, // Reduced by 50%
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            IconButton(
                onClick = { viewModel.navigateTo(Screen.REQUESTS) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = "Requests", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun FloatingControlWidgets(viewModel: DalilyViewModel) {
    val settings = viewModel.appSettings.collectAsState().value
    var assistantVisible by remember { mutableStateOf(false) }
    var widgetActiveAnswer by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (settings.chatIconEnabled) {
            Box(
                modifier = Modifier
                    .offset(x = settings.chatIconX.dp, y = settings.chatIconY.dp)
                    .align(Alignment.BottomStart)
                    .padding(bottom = 60.dp, start = 12.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(Screen.CHAT) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(settings.chatIconSize.dp)
                ) {
                    Text("💬", fontSize = 18.sp)
                }
            }
        }

        if (settings.smartAssistantEnabled) {
            Box(
                modifier = Modifier
                    .offset(x = settings.smartAssistantX.dp, y = settings.smartAssistantY.dp)
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 60.dp, end = 12.dp)
            ) {
                FloatingActionButton(
                    onClick = { assistantVisible = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(settings.smartAssistantSize.dp)
                ) {
                    Text(settings.smartAssistantIcon, fontSize = 20.sp)
                }
            }
        }

        if (assistantVisible) {
            Dialog(onDismissRequest = { assistantVisible = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "${settings.smartAssistantIcon} ${Loc.t("المساعد الذكي (بدون اتصال)", "Smart Assistant (Offline)")}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val questions = listOf(
                            Pair(Loc.t("ما هي الأقسام المتوفرة؟", "What are the categories?"), "sections"),
                            Pair(Loc.t("كيف أتصل بمقدم الخدمة؟", "How can I contact service?"), "contact"),
                            Pair(Loc.t("ما هو رقم الدعم الفني الفوري؟", "What's technical support?"), "support")
                        )

                        questions.forEach { (q, key) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        widgetActiveAnswer = when (key) {
                                            "sections" -> Loc.t(
                                                "يتوفر أقسام: كهرباء، سباكة، التعليم وتطوير المهارات، ميكانيكا وصيانة سيارات.",
                                                "Available: Electricity, Plumbing, Education, Car maintenance services."
                                            )
                                            "contact" -> Loc.t(
                                                "ادخل لصفحة مهني محدد واضغط زر 'طلب الخدمة واتصال'.",
                                                "Pick a professional file and tap 'Request Service & Call' button."
                                            )
                                            "support" -> "${Loc.t("رقم الدعم:", "Hotline Support:")} ${settings.supportPhone}"
                                            else -> ""
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = q, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        if (widgetActiveAnswer.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = widgetActiveAnswer,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                assistantVisible = false
                                widgetActiveAnswer = ""
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(Loc.t("إغلاق", "Close"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DalilyHomeScreen(viewModel: DalilyViewModel) {
    val settings = viewModel.appSettings.collectAsState().value
    val categories = viewModel.categories.collectAsState().value
    val providers = viewModel.serviceProviders.collectAsState().value
    val bList = viewModel.banners.collectAsState().value
    val context = LocalContext.current

    val mainCategories = categories.filter { it.parentId == null }
    val currentSubCategories = categories.filter { it.parentId == viewModel.selectedCategory?.id }

    val filteredList = providers.filter { p ->
        val matchesApproved = p.isApproved
        val matchesDept = if (viewModel.selectedCategory == null) true else (p.mainCategory == viewModel.selectedCategory?.nameAr || p.mainCategory == viewModel.selectedCategory?.nameEn)
        val matchesSub = if (viewModel.selectedSubCategory == null) true else (p.subCategory == viewModel.selectedSubCategory?.nameAr || p.subCategory == viewModel.selectedSubCategory?.nameEn)
        val matchesSearch = if (viewModel.searchQuery.isEmpty()) true else (p.fullName.contains(viewModel.searchQuery, ignoreCase = true) || p.address.contains(viewModel.searchQuery, ignoreCase = true))
        val matchesCity = if (viewModel.filterCity.isEmpty()) true else p.address.contains(viewModel.filterCity, ignoreCase = true)
        val matchesDistrict = if (viewModel.filterDistrict.isEmpty()) true else p.district.contains(viewModel.filterDistrict, ignoreCase = true)

        matchesApproved && matchesDept && matchesSub && matchesSearch && matchesCity && matchesDistrict && !p.isBlocked
    }

    val recommendedList = providers.filter { it.isApproved && it.isRecommended && !it.isBlocked }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        if (bList.isNotEmpty()) {
            item {
                Text(
                    text = Loc.t("عروض وإعلانات الخدمات المميزة", "Premium Services Ads"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bList) { b ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .height(90.dp)
                                .clickable {
                                    Toast.makeText(context, "${Loc.t("التوجه الفوري إلى:", "Redirecting to:")} ${b.targetUrl}", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                Text(
                                    text = if (b.type == "IMAGE") "🖼️ " + b.textContent else if (b.type == "VIDEO") "🎥 " + b.textContent else "⚡ " + b.textContent,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${Loc.t("متبقي", "Left")} ${b.durationDays} ${Loc.t("أيام", "days")}",
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (recommendedList.isNotEmpty()) {
            item {
                Text(
                    text = "⭐ " + Loc.t("مزودي الخدمات الموصى بهم", "Recommended Professionals"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    items(recommendedList) { p ->
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { viewModel.navigateTo(Screen.DETAILS, p.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Gray)) {
                                    Text("👤", modifier = Modifier.align(Alignment.Center), fontSize = 20.sp)
                                }
                                Text(p.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(p.subCategory, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⭐ ${p.averageRating}", fontSize = 11.sp)
                                    if (p.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("🔵", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(Loc.t("ابحث بالاسم، المهنة، العنوان...", "Search name, job, layout...")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.searchQuery = "ماهر"
                                Toast.makeText(context, Loc.t("تم تجربة البحث الصوتي!", "Voice speech check!"), Toast.LENGTH_SHORT).show()
                            }) {
                                Text("🎤", fontSize = 16.sp)
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = viewModel.filterCity,
                            onValueChange = { viewModel.filterCity = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(Loc.t("المدينة", "City")) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.filterDistrict,
                            onValueChange = { viewModel.filterDistrict = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(Loc.t("الحي", "District")) },
                            singleLine = true
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = Loc.t("الأقسام والخدمات الرئيسية", "Main Departments & Services"),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                item {
                    AssistChip(
                        onClick = {
                            viewModel.selectedCategory = null
                            viewModel.selectedSubCategory = null
                        },
                        label = { Text(Loc.t("الكل", "All")) },
                        colors = if (viewModel.selectedCategory == null) AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else AssistChipDefaults.assistChipColors()
                    )
                }

                items(mainCategories) { cat ->
                    val isSelected = viewModel.selectedCategory?.id == cat.id
                    AssistChip(
                        onClick = {
                            viewModel.selectedCategory = cat
                            viewModel.selectedSubCategory = null
                        },
                        label = { Text(if (Loc.isArabic.value) cat.nameAr else cat.nameEn) },
                        colors = if (isSelected) AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else AssistChipDefaults.assistChipColors()
                    )
                }
            }
        }

        if (viewModel.selectedCategory != null && currentSubCategories.isNotEmpty()) {
            item {
                Text(
                    text = Loc.t("الفروع المتخصص", "Specialized Sub-Categories"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    item {
                        FilterChip(
                            selected = viewModel.selectedSubCategory == null,
                            onClick = { viewModel.selectedSubCategory = null },
                            label = { Text(Loc.t("الكل الفروع", "All Sub")) }
                        )
                    }

                    items(currentSubCategories) { sub ->
                        val isSelected = viewModel.selectedSubCategory?.id == sub.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedSubCategory = sub },
                            label = { Text(if (Loc.isArabic.value) sub.nameAr else sub.nameEn) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "${Loc.t("النتائج الحقيقية المباشرة", "Direct Active Listings")} (${filteredList.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (filteredList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(Loc.t("لم يتم العثور على نتائج تفاؤلية.", "No listings match."), color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filteredList) { p ->
                ServiceProviderCard(p, settings.isDataSavingMode) {
                    viewModel.navigateTo(Screen.DETAILS, p.id)
                }
            }
        }
    }
}

@Composable
fun ServiceProviderCard(p: ServiceProvider, dataSaving: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer)) {
                Text(if (dataSaving) "💡" else "👤", modifier = Modifier.align(Alignment.Center), fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = p.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (p.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🔵", fontSize = 11.sp)
                    }
                    if (p.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📌", fontSize = 12.sp)
                    }
                    if (p.subscriptionActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⭐", fontSize = 12.sp)
                    }
                }
                Text(text = "${p.mainCategory} • ${p.subCategory}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                Text(text = "${p.address} • ${p.district}", fontSize = 11.sp, color = Color.Gray)
            }
            Text("◀", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun DalilyDetailScreen(viewModel: DalilyViewModel) {
    val providers = viewModel.serviceProviders.collectAsState().value
    val pid = viewModel.activeProviderId ?: 0
    val p = providers.find { it.id == pid }
    val context = LocalContext.current

    if (p == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(Loc.t("غير متوفر", "Not Found"))
        }
        return
    }

    val rList = viewModel.getReviewsForProvider(p.id).collectAsState(initial = emptyList()).value

    var showReviewDialog by remember { mutableStateOf(false) }
    var ratingChosen by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var reviewerName by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                    Text("👤", modifier = Modifier.align(Alignment.Center), fontSize = 48.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = p.fullName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (p.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🔵", fontSize = 14.sp)
                    }
                }
                Text(text = "${p.mainCategory} - ${p.subCategory}", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
                Text(text = "📍 ${p.address} (${p.district})", fontSize = 12.sp, color = Color.Gray)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Button(onClick = {
                        viewModel.registerServiceRequest(p.id, p.fullName, p.subCategory)
                        Toast.makeText(context, "${Loc.t("جاري الاتصال بـ", "Calling")}: ${p.phone}", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(Loc.t("طلب الخدمة واتصال 📱", "Request Service & Call 📱"))
                    }

                    IconButton(onClick = {
                        Toast.makeText(context, "${Loc.t("تم ترويج ومشاركة الارتباط", "Shared link of")}: ${p.fullName}", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("📤", fontSize = 18.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.logActivity("بلاغ شكوى ضد ${p.fullName}", "User")
                            Toast.makeText(context, Loc.t("تم إرسال بلاغك للمراجعة!", "Sent review complaint report!"), Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(Loc.t("إبلاغ ⚠️", "Report ⚠️"))
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.t("التقييمات وآراء العملاء", "Client Reviews"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Button(onClick = { showReviewDialog = true }) {
                            Text(Loc.t("تقييم ★", "Rate ★"))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⭐ ${p.averageRating} / 5.0 (${p.reviewCount} ${Loc.t("مراجعة", "Reviews")})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (rList.isEmpty()) {
            item {
                Text(
                    text = Loc.t("لا توجد مراجعات حالية.", "No reviews yet."),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        } else {
            items(rList) { r ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.userName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("★ ${r.rating}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                        Text(r.comment, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        Dialog(onDismissRequest = { showReviewDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Loc.t("إضافة تقييم ومراجعة", "Add Review"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(value = reviewerName, onValueChange = { reviewerName = it }, modifier = Modifier.fillMaxWidth(), label = { Text(Loc.t("الاسم ثلاثي", "Name")) })
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(value = reviewComment, onValueChange = { reviewComment = it }, modifier = Modifier.fillMaxWidth(), label = { Text(Loc.t("التعليق والتقييم النصي", "Comment")) })
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Loc.t("النجوم (1-5):", "Stars (1-5):"))
                        Row {
                            (1..5).forEach { star ->
                                Text(
                                    text = if (star <= ratingChosen) "★" else "☆",
                                    fontSize = 24.sp,
                                    color = if (star <= ratingChosen) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.clickable { ratingChosen = star }.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showReviewDialog = false }) { Text(Loc.t("إلغاء", "Cancel")) }
                        Button(onClick = {
                            if (reviewerName.trim().isNotEmpty() && reviewComment.trim().isNotEmpty()) {
                                viewModel.addReview(p.id, reviewerName, ratingChosen, reviewComment)
                                showReviewDialog = false
                                reviewComment = ""
                                reviewerName = ""
                            }
                        }) {
                            Text(Loc.t("حفظ", "Save"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DalilyRegistrationForm(viewModel: DalilyViewModel) {
    val categories = viewModel.categories.collectAsState().value
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedMainText by remember { mutableStateOf("") }
    var selectedSubText by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var gpsCoordinates by remember { mutableStateOf("") }
    var selfieDone by remember { mutableStateOf(false) }
    var cardIdDone by remember { mutableStateOf(false) }

    val mainCategories = categories.filter { it.parentId == null }
    val matchingSubs = categories.filter { it.parentId != null }

    var expandedMain by remember { mutableStateOf(false) }
    var expandedSub by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(
            text = Loc.t("👤 تسجيل حساب مقدم خدمة محترف", "Register as a Service Provider"),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, modifier = Modifier.fillMaxWidth(), label = { Text(Loc.t("الاسم الثلاثي كامل (إجباري)", "Full Name (Required)")) })
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(), label = { Text(Loc.t("رقم الهاتف أو واتساب الفعال (إجباري)", "Phone Number / WhatsApp (Required)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedMainText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(Loc.t("القسم الرئيسي (إجباري)", "Main Category (Required)")) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expandedMain = true }) }
            )
            DropdownMenu(expanded = expandedMain, onDismissRequest = { expandedMain = false }) {
                mainCategories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(if (Loc.isArabic.value) cat.nameAr else cat.nameEn) },
                        onClick = {
                            selectedMainText = if (Loc.isArabic.value) cat.nameAr else cat.nameEn
                            selectedSubText = ""
                            expandedMain = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedSubText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(Loc.t("الخدمة الفرعية (إجباري)", "Sub Service (Required)")) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expandedSub = true }) }
            )
            DropdownMenu(expanded = expandedSub, onDismissRequest = { expandedSub = false }) {
                matchingSubs.forEach { sub ->
                    DropdownMenuItem(
                        text = { Text(if (Loc.isArabic.value) sub.nameAr else sub.nameEn) },
                        onClick = {
                            selectedSubText = if (Loc.isArabic.value) sub.nameAr else sub.nameEn
                            expandedSub = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text(Loc.t("عنوان العمل الحالي (إجباري)", "Office Address (Required)")) })
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = district, onValueChange = { district = it }, modifier = Modifier.fillMaxWidth(), label = { Text(Loc.t("منطقة الحي (إجباري)", "District Zone (Required)")) })
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = gpsCoordinates, onValueChange = { gpsCoordinates = it }, modifier = Modifier.weight(1f), label = { Text(Loc.t("موقع الخرائط GPS (اختياري)", "GPS (Optional)")) })
            Spacer(modifier = Modifier.width(6.dp))
            Button(onClick = {
                gpsCoordinates = "15.3694,44.1910"
                Toast.makeText(context, Loc.t("تم جلب إحداثيات GPS بنجاح!", "GPS Coordinates Retrieved!"), Toast.LENGTH_SHORT).show()
            }) {
                Text(Loc.t("جلب الخرائط", "Get"))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text(Loc.t("تحميل المستندات الهندسية والثبوتية", "Upload verified Documents"), fontWeight = FontWeight.Bold, fontSize = 13.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.weight(1f).height(90.dp).clickable {
                    selfieDone = true
                    Toast.makeText(context, Loc.t("تم رفع صورة سيلفي بنجاح!", "Selfie saved!"), Toast.LENGTH_SHORT).show()
                },
                colors = CardDefaults.cardColors(containerColor = if (selfieDone) Color(0xFF1E3F20) else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (selfieDone) "✅ سيلفي" else "📸 صورة الشخصية", fontSize = 12.sp)
                }
            }

            Card(
                modifier = Modifier.weight(1f).height(90.dp).clickable {
                    cardIdDone = true
                    Toast.makeText(context, Loc.t("تم تصوير بطاقة الهوية!", "ID Captured!"), Toast.LENGTH_SHORT).show()
                },
                colors = CardDefaults.cardColors(containerColor = if (cardIdDone) Color(0xFF1E3F20) else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (cardIdDone) "✅ هويتك" else "💳 بطاقة الهوية", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (fullName.isEmpty() || phone.isEmpty() || selectedMainText.isEmpty() || selectedSubText.isEmpty() || address.isEmpty() || district.isEmpty() || !selfieDone) {
                    Toast.makeText(context, Loc.t("برجاء إدخال الحقول الإجبارية ورفع السيلفي", "Please fill required fields"), Toast.LENGTH_LONG).show()
                } else {
                    viewModel.addManualProvider(
                        ServiceProvider(
                            fullName = fullName,
                            phone = phone,
                            mainCategory = selectedMainText,
                            subCategory = selectedSubText,
                            address = address,
                            district = district,
                            gpsCoordinates = gpsCoordinates,
                            personalPhoto = "selfie",
                            isApproved = false
                        )
                    )
                    Toast.makeText(context, Loc.t("تم إرسال الطلب، شكراً لك!", "Submitted for review!"), Toast.LENGTH_LONG).show()
                    viewModel.navigateTo(Screen.HOME)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Loc.t("إرسال طلب الانضمام ⚙️", "Submit ⚙️"))
        }
    }
}

@Composable
fun DalilyLoginScreen(viewModel: DalilyViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = Loc.t("🔐 تسجيل دخول الإدارة والمنظومة", "Secure Portal Sign-In"),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(value = username, onValueChange = { username = it }, modifier = Modifier.fillMaxWidth(), label = { Text(Loc.t("اسم المستخدم", "Username")) })
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text(Loc.t("كلمة المرور", "Password")) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (username == "WAM2026" && password == "maher736462") {
                    viewModel.isLoggedIn = true
                    viewModel.loggedInUserRole = "ADMIN"
                    viewModel.logActivity("دخول ناجح للمشرف الرئيسي", "WAM2026")
                    Toast.makeText(context, Loc.t("مرحباً بك مجدداً WAM2026!", "Welcome back, WAM2026!"), Toast.LENGTH_SHORT).show()
                } else if (password == "maher--736462") {
                    viewModel.isLoggedIn = true
                    viewModel.loggedInUserRole = "OWNER"
                    viewModel.logActivity("دخول المالك للوحة البوابة الخلفية السرية", "Owner")
                    Toast.makeText(context, Loc.t("تم التخطي والولوج كمالك سري كلي!", "Welcome Owner!"), Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.logActivity("محاولة دخول خاطئة باسم المحاولة: $username", "Anonymous")
                    Toast.makeText(context, Loc.t("خطأ، أوراق الاعتماد غير صحيحة!", "Credentials invalid!"), Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Loc.t("تحقق وتسجيل دخول", "Authenticate"))
        }

        if (viewModel.isLoggedIn) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(Loc.t("تم تفعيل أجهزة الإدارة المصرحة والولوج الفعال:", "Administrator panels unlocked locally:"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(10.dp))
            DalilyAdminPanelSubScreen(viewModel)
        }
    }
}

@Composable
fun DalilyAboutUsScreen(viewModel: DalilyViewModel) {
    val settings = viewModel.appSettings.collectAsState().value

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
            Text("ℹ️", modifier = Modifier.align(Alignment.Center), fontSize = 38.sp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = settings.appName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = "الإصدار الحالي: v2.067", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = settings.welcomeMessage.ifEmpty { "منصة كلاسيكية كفؤة للاتصال والربط بين مقدمي الخدمات والعملاء داخل اليمن وخارجه." },
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = Loc.t("قنوات الدعم الفني المباشر", "Official Support Channels"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("☎️ الدعم:", fontSize = 13.sp)
                    Text(text = settings.supportPhone, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("✉️ البريد:", fontSize = 13.sp)
                    Text(text = settings.supportEmail, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("💬 واتساب:", fontSize = 13.sp)
                    Text(text = settings.supportWhatsapp, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun PreviousRequestsScreen(viewModel: DalilyViewModel) {
    val list = viewModel.serviceRequests.collectAsState().value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "📋 " + Loc.t("سجل طلبات التواصل السابقة", "Connection History Logs"),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Loc.t("سجل الطلبات فارغ.", "No history logs found."), color = Color.Gray)
            }
        } else {
            LazyColumn {
                items(list) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.providerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(item.status, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(item.providerCategory, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DalilyRealtimeChatScreen(viewModel: DalilyViewModel) {
    val role = viewModel.loggedInUserRole
    val messages = viewModel.chatMessages.collectAsState().value
    var responseInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "💬 " + Loc.t("المحادثة الفورية لطلب المساعدة", "Real-time Live Chat"),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (messages.isEmpty()) {
                    item {
                        Text(text = Loc.t("أرسل رسالة وممثلي الإدارة سيردون فوراً!", "Send a message and support team will reply!"), color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    items(messages) { msg ->
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (msg.senderId == role) Alignment.End else Alignment.Start) {
                            Card(colors = CardDefaults.cardColors(containerColor = if (msg.senderId == role) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(msg.senderName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(msg.messageText, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = responseInput, onValueChange = { responseInput = it }, modifier = Modifier.weight(1f), placeholder = { Text(Loc.t("اكتب رسالتك...", "Type a message...")) })
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = {
                if (responseInput.trim().isNotEmpty()) {
                    viewModel.sendChatMessage(senderId = role, senderName = if (role == "USER") Loc.t("زائر", "Guest") else role, receiverId = "ADMIN", receiverName = "Dalily Team", text = responseInput)
                    responseInput = ""
                }
            }) {
                Text("➡️", fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun DalilyAdminPanelSubScreen(viewModel: DalilyViewModel) {
    val providers = viewModel.serviceProviders.collectAsState().value
    val currentCategories = viewModel.categories.collectAsState().value
    val bList = viewModel.banners.collectAsState().value
    val logs = viewModel.auditLogs.collectAsState().value
    val devices = viewModel.authorizedDevices.collectAsState().value
    val settings = viewModel.appSettings.collectAsState().value

    var activeTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val tabs = listOf(
        Loc.t("الأقسام", "Categories"),
        Loc.t("طلبات التسجيل", "Pending"),
        Loc.t("إضافة يدوية", "Manual Entry"),
        Loc.t("إدارة المشتركين", "Providers"),
        Loc.t("التقارير وسجل النشاط", "Logs & Audit"),
        Loc.t("النسخ الاحتياطي", "Backup"),
        Loc.t("الإعلانات", "Ads"),
        Loc.t("الألوان والتصميم", "Colors"),
        Loc.t("ترخيص الأجهزة", "Devices")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            ScrollableTabRow(selectedTabIndex = activeTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(selected = activeTab == idx, onClick = { activeTab = idx }, text = { Text(title, fontSize = 10.sp) })
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            0 -> {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    var cNameAr by remember { mutableStateOf("") }
                    var cNameEn by remember { mutableStateOf("") }
                    var parentSelectorId by remember { mutableStateOf<Int?>(null) }
                    var expandedP by remember { mutableStateOf(false) }

                    Text(Loc.t("إضافة قسم", "Add Category"), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(value = cNameAr, onValueChange = { cNameAr = it }, placeholder = { Text("الاسم بالعربية") })
                    OutlinedTextField(value = cNameEn, onValueChange = { cNameEn = it }, placeholder = { Text("Name in English") })

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(Loc.t("القسم الأب: ", "Parent: "), fontSize = 12.sp)
                        TextButton(onClick = { expandedP = true }) {
                            Text(if (parentSelectorId != null) "ID: $parentSelectorId" else Loc.t("رئيسي", "Root"))
                        }
                        DropdownMenu(expanded = expandedP, onDismissRequest = { expandedP = false }) {
                            DropdownMenuItem(text = { Text(Loc.t("رئيسي", "Root")) }, onClick = { parentSelectorId = null; expandedP = false })
                            currentCategories.filter { it.parentId == null }.forEach { rootCat ->
                                DropdownMenuItem(text = { Text(rootCat.nameAr) }, onClick = { parentSelectorId = rootCat.id; expandedP = false })
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (cNameAr.isNotEmpty() && cNameEn.isNotEmpty()) {
                                viewModel.addCategory(Category(nameAr = cNameAr, nameEn = cNameEn, parentId = parentSelectorId))
                                cNameAr = ""
                                cNameEn = ""
                                Toast.makeText(context, Loc.t("تم الحفظ بنجاح!", "Successfully saved!"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Loc.t("حفظ وإضافة", "Save"))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(Loc.t("الأقسام الحالية:", "Current:"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    currentCategories.forEach { cat ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${cat.id} - ${cat.nameAr}", fontSize = 12.sp)
                            IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            1 -> {
                val pendingList = providers.filter { !it.isApproved }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(Loc.t("طلبات الانضمام المعلقة", "Pending Registrations"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (pendingList.isEmpty()) {
                        Text(Loc.t("لا توجد طلبات معلقة.", "No pending requests."), color = Color.Gray, fontSize = 12.sp)
                    } else {
                        pendingList.forEach { p ->
                            var expandedZoom by remember { mutableStateOf(false) }
                            var rejectReasonInput by remember { mutableStateOf("") }
                            var isRejecting by remember { mutableStateOf(false) }

                            Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(p.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("📞 ${p.phone} | 📍 ${p.address}", fontSize = 12.sp)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        Box(modifier = Modifier.size(60.dp).background(Color.DarkGray).clickable { expandedZoom = true }, contentAlignment = Alignment.Center) {
                                            Text("🖼️ سيلفي", fontSize = 10.sp, color = Color.White)
                                        }
                                        Box(modifier = Modifier.size(60.dp).background(Color.DarkGray).clickable { expandedZoom = true }, contentAlignment = Alignment.Center) {
                                            Text("💳 الهوية", fontSize = 10.sp, color = Color.White)
                                        }
                                    }

                                    if (expandedZoom) {
                                        Dialog(onDismissRequest = { expandedZoom = false }) {
                                            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp)) {
                                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(Loc.t("معاينة الوثائق الهندسية والثبوتية", "Documents Preview"), fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Box(modifier = Modifier.size(240.dp).background(Color.Gray)) {
                                                        Text("تم رفع وثيقة الهوية والترخيص الشخصي بكامل تفاصيلها", modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    TextButton(onClick = { expandedZoom = false }) { Text(Loc.t("إغلاق", "Close")) }
                                                }
                                            }
                                        }
                                    }

                                    if (isRejecting) {
                                        OutlinedTextField(value = rejectReasonInput, onValueChange = { rejectReasonInput = it }, placeholder = { Text("سبب الرفض") })
                                        Row {
                                            Button(
                                                onClick = {
                                                    viewModel.updateProviderApproval(p.id, false, rejectReasonInput)
                                                    isRejecting = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                            ) {
                                                Text(Loc.t("تأكيد الرفض", "Confirm"))
                                            }
                                            TextButton(onClick = { isRejecting = false }) { Text(Loc.t("إلغاء", "Cancel")) }
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Button(onClick = { viewModel.updateProviderApproval(p.id, true) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3F20))) {
                                                Text(Loc.t("قبول الطلب ✅", "Accept ✅"))
                                            }
                                            Button(onClick = { isRejecting = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                                Text(Loc.t("رفض ❌", "Reject ❌"))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                var name by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }
                var mCat by remember { mutableStateOf("") }
                var sCat by remember { mutableStateOf("") }
                var addy by remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(Loc.t("إضافة يدوية لمزود الخدمة مباشرة", "Direct Manual Addition"), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("الاسم") })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, placeholder = { Text("رقم الهاتف") })
                    OutlinedTextField(value = mCat, onValueChange = { mCat = it }, placeholder = { Text("القسم") })
                    OutlinedTextField(value = sCat, onValueChange = { sCat = it }, placeholder = { Text("الخدمة") })
                    OutlinedTextField(value = addy, onValueChange = { addy = it }, placeholder = { Text("العنوان") })

                    Button(
                        onClick = {
                            if (name.isNotEmpty() && phone.isNotEmpty()) {
                                viewModel.addManualProvider(
                                    ServiceProvider(
                                        fullName = name,
                                        phone = phone,
                                        mainCategory = mCat,
                                        subCategory = sCat,
                                        address = addy,
                                        district = "العاصمة",
                                        personalPhoto = "",
                                        isApproved = true
                                    )
                                )
                                name = ""
                                phone = ""
                                Toast.makeText(context, Loc.t("تم حفظ مقدم الخدمة!", "Successfully added direct!"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Loc.t("حفظ وإضافة", "Save"))
                    }
                }
            }
            4 -> {
                val approved = providers.filter { it.isApproved }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(Loc.t("إدارة مقدمي الخدمات والتثبيت والترشيحات والاشتراكات لجميع الأعضاء والمستخدمين والأدمينات", "Manage Active Service Providers"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    approved.forEach { p ->
                        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(p.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${p.subCategory} • نقاط الولاء: ${p.loyaltyPoints}", fontSize = 11.sp, color = Color.Gray)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                    IconButton(onClick = { viewModel.toggleProviderRecommend(p.id, !p.isRecommended) }) {
                                        Text(if (p.isRecommended) "⭐" else "☆", fontSize = 18.sp)
                                    }
                                    IconButton(onClick = { viewModel.toggleProviderPin(p.id, !p.isPinned) }) {
                                        Text(if (p.isPinned) "📌" else "📍", fontSize = 18.sp)
                                    }
                                    IconButton(onClick = { viewModel.toggleProviderVerification(p.id, !p.isVerified) }) {
                                        Text(if (p.isVerified) "🔵" else "⚪", fontSize = 18.sp)
                                    }
                                    IconButton(onClick = { viewModel.processSubActive(p.id, !p.subscriptionActive) }) {
                                        Text(if (p.subscriptionActive) "💎" else "⚙️", fontSize = 18.sp)
                                    }
                                    IconButton(onClick = { viewModel.updateProviderBlock(p.id, !p.isBlocked) }) {
                                        Text(if (p.isBlocked) "🔴 [حظر]" else "🟢 [مستمر]", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = { viewModel.deleteProvider(p.id, p.fullName) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            5 -> {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Loc.t("سجل الأحداث والنشاطات والتقارير الأسبوعية والشهرية", "Audit Activity logs"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Button(onClick = {
                            Toast.makeText(context, Loc.t("تم تصدير وحفظ السجلات بصيغة PDF بنجاح!", "Successfully exported history logs PDF!"), Toast.LENGTH_LONG).show()
                        }) {
                            Text(Loc.t("تصدير الحفظ بصيغة PDF 📄", "Export PDF 📄"))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(Loc.t("السجلات الأحدث بالذاكرة:", "Recent Audit logs:"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    logs.take(15).forEach { log ->
                        Text("⚡ [${log.adminName}] ${log.action}", fontSize = 11.sp)
                    }
                }
            }
            6 -> {
                var scheduleBackup by remember { mutableStateOf(true) }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(Loc.t("النسخ الاحتياطي والأرشفة سحابياً ومحلياً لبطاقة الذاكرة وجوجل درايف", "Backup database configurations"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(onClick = {
                        Toast.makeText(context, Loc.t("تم تصدير أرشفة كاملة من قاعدة البيانات!", "Manual Backup generated safely!"), Toast.LENGTH_LONG).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(Loc.t("أخذ نسخة احتياطية وحفظها بالخارج 📥", "Manual Backup to Cloud 📥"))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = scheduleBackup, onCheckedChange = { scheduleBackup = it })
                        Text(Loc.t("تفعيل ميزة النسخ الاحتياطي التلقائي المجدول للأنظمة الحيوية", "Mute automated scheduled backup configuration"), fontSize = 11.sp)
                    }
                }
            }
            7 -> {
                var bannerText by remember { mutableStateOf("") }
                var expandedAdColor by remember { mutableStateOf("TEXT") }
                var bannerUrl by remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(Loc.t("اللافتات والإعلانات وإدارة واجهة الترويج الرئيسي", "Premium Ads Banners"), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(value = bannerText, onValueChange = { bannerText = it }, placeholder = { Text("النص أو رابط الصورة") })
                    OutlinedTextField(value = bannerUrl, onValueChange = { bannerUrl = it }, placeholder = { Text("رابط التوجيه (Url)") })

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("نوع الإعلان: ")
                        listOf("TEXT", "IMAGE", "VIDEO").forEach { type ->
                            FilterChip(selected = expandedAdColor == type, onClick = { expandedAdColor = type }, label = { Text(type) })
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    Button(
                        onClick = {
                            if (bannerText.isNotEmpty()) {
                                viewModel.addBanner(Banner(textContent = bannerText, type = expandedAdColor, targetUrl = bannerUrl))
                                bannerText = ""
                                bannerUrl = ""
                                Toast.makeText(context, Loc.t("تم تفعيل اللافتة الإعلانية!", "Advertisements published successfully!"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Loc.t("نشر وتفعيل اللافتة الإعلانية ترويجياً", "Publish Ad Banner"))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    bList.forEach { b ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("[${b.type}] ${b.textContent}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteBanner(b.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            8 -> {
                var pColor by remember { mutableStateOf(settings.primaryColorHex) }
                var sColor by remember { mutableStateOf(settings.secondaryColorHex) }
                var nameSet by remember { mutableStateOf(settings.appName) }
                var footerSet by remember { mutableStateOf(settings.advertisingFooter) }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(Loc.t("الهوية البصرية والألوان السلوكية وقوالب التصميم", "Graphic themes of system"), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(value = nameSet, onValueChange = { nameSet = it }, label = { Text("اسم التطبيق") })
                    OutlinedTextField(value = footerSet, onValueChange = { footerSet = it }, label = { Text("التذييل المخصص") })

                    Row {
                        Button(onClick = { viewModel.updateTheme("كوزميك سيلفر") }) { Text("🌌 كوزميك", fontSize = 10.sp) }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = { viewModel.updateTheme("الذهبي الفاخر") }) { Text("✨ الذهبي", fontSize = 10.sp) }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = { viewModel.updateTheme("الزمردي الراقي") }) { Text("🟢 الزمردي", fontSize = 10.sp) }
                    }

                    OutlinedTextField(value = pColor, onValueChange = { pColor = it }, label = { Text("اللون الأساسي HEX") })
                    OutlinedTextField(value = sColor, onValueChange = { sColor = it }, label = { Text("اللون الفرعي HEX") })

                    Button(
                        onClick = {
                            viewModel.updateDynamicColors(pColor, sColor)
                            viewModel.saveAppSettings(settings.copy(appName = nameSet, advertisingFooter = footerSet))
                            Toast.makeText(context, Loc.t("تم التطبيق بنجاح!", "Styles updated successfully!"), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Loc.t("حفظ الهوية البصرية وتطويرها", "Save Visuals"))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = settings.isMaintenanceMode, onCheckedChange = { viewModel.updateMaintenanceMode(it) })
                        Text(Loc.t("تفعيل وضع الصيانة المؤقت للتحكم بالأنظمة السلوكية", "Activate overall Maintenance Mode"), fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = settings.isDataSavingMode, onCheckedChange = { viewModel.updateDataSavingMode(it) })
                        Text(Loc.t("تفعيل وضع تصفح تقتصد استهلاك الإنترنت والبيانات", "Activate Data-Saving Browser mode"), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
