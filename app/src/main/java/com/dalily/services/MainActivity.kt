package com.dalily.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.dalily.services.data.*
import com.dalily.services.ui.theme.DalilyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Offline / Realtime config
        FirebaseRepository.initialize(applicationContext)

        setContent {
            DalilyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppViewport()
                }
            }
        }
    }
}

// Global Localization translations
object Trans {
    var isArabic = mutableStateOf(true)

    fun t(ar: String, en: String): String {
        return if (isArabic.value) ar else en
    }
}

// Navigation Screens enum
enum class AppScreen {
    HOME,
    DETAIL,
    JOIN,
    ADMIN
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppViewport() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Core database flows from FirebaseRepository
    val configState by FirebaseRepository.appConfig.collectAsState()
    val categoriesState by FirebaseRepository.categories.collectAsState()
    val subCategoriesState by FirebaseRepository.subCategories.collectAsState()
    val providersState by FirebaseRepository.serviceProviders.collectAsState()
    val pendingState by FirebaseRepository.pendingProviders.collectAsState()
    val bannersState by FirebaseRepository.banners.collectAsState()
    val reviewsMapState by FirebaseRepository.reviewsByProvider.collectAsState()
    val messagesMapState by FirebaseRepository.chatMessagesByProvider.collectAsState()

    // Screen navigation state
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedProvider by remember { mutableStateOf<ServiceProvider?>(null) }

    // Click tracking for Backdoor
    var backdoorTapsCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var showBackdoorPasswordDialog by remember { mutableStateOf(false) }
    var showBackdoorPanelDialog by remember { mutableStateOf(false) }

    // SharedPreferences for saving admin / user login status
    val prefs = remember { context.getSharedPreferences("dalily_prefs", Context.MODE_PRIVATE) }
    var rememberLogin by remember { mutableStateOf(prefs.getBoolean("remember_login", false)) }
    
    // Admin authorization states
    var loggedInAdmin by remember { 
        mutableStateOf<String?>(
            if (prefs.getBoolean("remember_login", false)) {
                prefs.getString("saved_admin", null)
            } else {
                null
            }
        ) 
    }
    
    // Direct authenticated users immediately
    LaunchedEffect(loggedInAdmin) {
        if (loggedInAdmin != null && currentScreen == AppScreen.HOME) {
            currentScreen = AppScreen.ADMIN
        }
    }
    
    var showLoginDialog by remember { mutableStateOf(false) }

    // Search and Filters
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String>("") }

    // Visual indicators
    var isManualSyncing by remember { mutableStateOf(false) }

    // Language selector state
    var showLangSelector by remember { mutableStateOf(false) }

    // Backdoor inputs state
    var backdoorPwdInput by remember { mutableStateOf("") }

    // Backdoor secret settings edit states
    var editAppName by remember { mutableStateOf("") }
    var editPrimaryColor by remember { mutableStateOf("") }
    var editSecondaryColor by remember { mutableStateOf("") }
    var editWelcomeText by remember { mutableStateOf("") }
    var editFooterText by remember { mutableStateOf("") }
    var editSupportPhone by remember { mutableStateOf("") }
    var editSupportEmail by remember { mutableStateOf("") }
    var editAdminPasswordByOwner by remember { mutableStateOf("") }

    // Trigger action when tapping logo or home icon
    val triggerBackdoorTap: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < 1500) {
            backdoorTapsCount += 1
        } else {
            backdoorTapsCount = 1
        }
        lastTapTime = now
        
        if (backdoorTapsCount >= 5) {
            backdoorTapsCount = 0
            showBackdoorPasswordDialog = true
            backdoorPwdInput = ""
        }
    }

    // Compose Main layout structure
    Scaffold(
        topBar = {
            // BRAND NEW CUSTOM TOP APP BAR REQUIRED ON ALL SCREENS
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Brand Side (Logo & Interactive App Name)
                    Row(
                        modifier = Modifier
                            .clickable { triggerBackdoorTap() }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Handyman,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = configState.appName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Action Icons Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. 🏠 Home Icon Button
                        IconButton(
                            onClick = {
                                backdoorTapsCount++
                                triggerBackdoorTap()
                                currentScreen = AppScreen.HOME
                            },
                            modifier = Modifier.testTag("nav_home_btn")
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home",
                                tint = if (currentScreen == AppScreen.HOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. 🔐 Key Login Icon Button
                        IconButton(
                            onClick = {
                                if (loggedInAdmin != null) {
                                    currentScreen = AppScreen.ADMIN
                                } else {
                                    showLoginDialog = true
                                }
                            },
                            modifier = Modifier.testTag("nav_login_btn")
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = "Admin Area",
                                tint = if (loggedInAdmin != null || currentScreen == AppScreen.ADMIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. 👤 User Plus Join Application
                        IconButton(
                            onClick = { currentScreen = AppScreen.JOIN },
                            modifier = Modifier.testTag("nav_join_btn")
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Join Service Providers",
                                tint = if (currentScreen == AppScreen.JOIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 4. 🌐 Globe Language
                        IconButton(
                            onClick = { showLangSelector = true },
                            modifier = Modifier.testTag("nav_lang_btn")
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = "Change Language",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 5. 🔄 Refresh Button
                        IconButton(
                            onClick = {
                                isManualSyncing = true
                                FirebaseRepository.forceSync {
                                    isManualSyncing = false
                                    Toast.makeText(context, Trans.t("تم تحديث البيانات فوراً", "Data synced in real-time"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("nav_refresh_btn")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Realtime Feed",
                                tint = if (isManualSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Background visual ambient glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                )

                // Animated Screen transitions
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    },
                    label = "screen_transition"
                ) { target ->
                    when (target) {
                        AppScreen.HOME -> {
                            HomeScreen(
                                banners = bannersState,
                                categories = categoriesState,
                                providers = providersState,
                                reviewsMap = reviewsMapState,
                                selectedCategoryId = selectedCategoryId,
                                searchQuery = searchQuery,
                                onSelectCategory = { selectedCategoryId = it },
                                onSearchQueryChange = { searchQuery = it },
                                onProviderClicked = {
                                    selectedProvider = it
                                    currentScreen = AppScreen.DETAIL
                                },
                                welcomeText = configState.welcomeText,
                                footerText = configState.footerText
                            )
                        }
                        AppScreen.DETAIL -> {
                            selectedProvider?.let { provider ->
                                DetailScreen(
                                    provider = provider,
                                    reviews = reviewsMapState[provider.id] ?: emptyList(),
                                    chatMessages = messagesMapState[provider.id] ?: emptyList(),
                                    onBackSelected = { currentScreen = AppScreen.HOME },
                                    onAddReview = { username, rating, comment ->
                                        val newReview = UserReview(
                                            providerId = provider.id,
                                            username = username,
                                            rating = rating,
                                            comment = comment
                                        )
                                        FirebaseRepository.addReview(newReview) {
                                            Toast.makeText(context, Trans.t("شكراً لتقييمك!", "Thank you for your rating!"), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSendMessage = { messageText ->
                                        val newMsg = ChatMessage(
                                            providerId = provider.id,
                                            senderName = Trans.t("مستخدم", "User"),
                                            text = messageText,
                                            isFromUser = true
                                        )
                                        FirebaseRepository.sendMessage(newMsg) {
                                            Toast.makeText(context, Trans.t("تم إرسال الرسالة", "Message sent"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } ?: run {
                                currentScreen = AppScreen.HOME
                            }
                        }
                        AppScreen.JOIN -> {
                            JoinScreen(
                                categories = categoriesState,
                                onBackSelected = { currentScreen = AppScreen.HOME },
                                onSubmitRequest = { request ->
                                    FirebaseRepository.addPendingProvider(request, {
                                        Toast.makeText(context, Trans.t("تم تقديم طلبك بنجاح للمراجعة للمراجعة الفورية!", "Joined successfully! Your request is pending immediate review."), Toast.LENGTH_LONG).show()
                                        currentScreen = AppScreen.HOME
                                    }, {
                                        Toast.makeText(context, "${Trans.t("خطأ:", "Error:")} ${it.message}", Toast.LENGTH_SHORT).show()
                                    })
                                }
                            )
                        }
                        AppScreen.ADMIN -> {
                            if (loggedInAdmin == null) {
                                currentScreen = AppScreen.HOME
                            } else {
                                AdminDashboardScreen(
                                    categories = categoriesState,
                                    subCategories = subCategoriesState,
                                    pendingRequests = pendingState,
                                    providersList = providersState,
                                    banners = bannersState,
                                    config = configState,
                                    onLogout = {
                                        loggedInAdmin = null
                                        prefs.edit().apply {
                                            putBoolean("remember_login", false)
                                            putString("saved_admin", null)
                                            apply()
                                        }
                                        currentScreen = AppScreen.HOME
                                        Toast.makeText(context, Trans.t("تم تسجيل الخروج", "Logged out"), Toast.LENGTH_SHORT).show()
                                    },
                                    onAddCategory = { cat ->
                                        FirebaseRepository.addCategory(cat, {
                                            Toast.makeText(context, Trans.t("تمت إضافة القسم الرئيسي!", "Main category added successfully!"), Toast.LENGTH_SHORT).show()
                                        }, {})
                                    },
                                    onAddSubCategory = { sub ->
                                        FirebaseRepository.addSubCategory(sub, {
                                            Toast.makeText(context, Trans.t("تمت إضافة القسم الفرعي!", "Subcategory added successfully!"), Toast.LENGTH_SHORT).show()
                                        }, {})
                                    },
                                    onAddProviderManual = { prov ->
                                        FirebaseRepository.addServiceProvider(prov, {
                                            Toast.makeText(context, Trans.t("تمت إضافة الخدمي يدوياً وبدون مراجعة!", "Service provider added manually!"), Toast.LENGTH_SHORT).show()
                                        }, {})
                                    },
                                    onReviewRequest = { requestId, isApprove, reason ->
                                        FirebaseRepository.reviewPendingProvider(requestId, isApprove, reason) {
                                            val msg = if (isApprove) Trans.t("تمت الموافقة ونقل البيانات لمديرية الخدمات", "Approved request!") else Trans.t("تم رفض الطلب بنجاح", "Rejected request")
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onUpdateProviderPin = { id, pinned, recommended ->
                                        FirebaseRepository.updateProviderPinStatus(id, pinned, recommended) {
                                            Toast.makeText(context, Trans.t("تمت عملية التحديث", "Status updated"), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteProvider = { id ->
                                        FirebaseRepository.deleteServiceProvider(id) {
                                            Toast.makeText(context, Trans.t("تم الحذف بنجاح", "Deleted successfully"), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onAddBannerAd = { ban ->
                                        FirebaseRepository.addBanner(ban) {
                                            Toast.makeText(context, Trans.t("تمت إضافة اللافتة بنجاح", "Banner ad registered successfully"), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteBanner = { id ->
                                        FirebaseRepository.deleteBanner(id) {
                                            Toast.makeText(context, Trans.t("تم حذف اللافتة الإعلانية", "Banner advertisement deleted"), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onUpdateConfig = { updated ->
                                        FirebaseRepository.updateAppConfig(updated) {
                                            Toast.makeText(context, Trans.t("تم التحديث والمزامنة", "Config updated successfully"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    // A. Password Backdoor Verification Dialog
    if (showBackdoorPasswordDialog) {
        Dialog(onDismissRequest = { showBackdoorPasswordDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Trans.t("البوابة الخلفية السرية", "Secret Admin Backdoor"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = backdoorPwdInput,
                        onValueChange = { backdoorPwdInput = it },
                        label = { Text(Trans.t("كلمة المرور الخلفية", "Backdoor Code")) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("backdoor_pwd_field")
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showBackdoorPasswordDialog = false }) {
                            Text(Trans.t("إلغاء", "Cancel"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (backdoorPwdInput == "maher--736462") {
                                    showBackdoorPasswordDialog = false
                                    // Initialize backdoor states
                                    editAppName = configState.appName
                                    editPrimaryColor = configState.primaryColorHex
                                    editSecondaryColor = configState.secondaryColorHex
                                    editWelcomeText = configState.welcomeText
                                    editFooterText = configState.footerText
                                    editSupportPhone = configState.supportPhone
                                    editSupportEmail = configState.supportEmail
                                    editAdminPasswordByOwner = configState.masterAdminPassword

                                    showBackdoorPanelDialog = true
                                } else {
                                    Toast.makeText(context, Trans.t("كلمة مرور خاطئة!", "Incorrect password!"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("backdoor_submit_btn")
                        ) {
                            Text(Trans.t("تأكيد", "Verify"))
                        }
                    }
                }
            }
        }
    }

    // B. Secret Settings Backdoor Configuration panel (للمالك فقط)
    if (showBackdoorPanelDialog) {
        Dialog(onDismissRequest = { showBackdoorPanelDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = Trans.t("⚙️ الإعدادات المتقدمة السرية (المالك)", "⚙️ Owner Secret Core Panel"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Field 1: Change App Name
                    Text(Trans.t("اسم التطبيق الرئيسي", "Main Application Name"), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editAppName,
                        onValueChange = { editAppName = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Field 2: Change Colors
                    Text(Trans.t("ألوان السمات (Hex)", "Theme Hex Colors"), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editPrimaryColor,
                            onValueChange = { editPrimaryColor = it },
                            label = { Text(Trans.t("اللون الأساسي", "Primary Color")) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editSecondaryColor,
                            onValueChange = { editSecondaryColor = it },
                            label = { Text(Trans.t("اللون الثانوي", "Secondary Color")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Field 3: Change Welcome Text
                    Text(Trans.t("رسالة الترحيب في الهيدر", "Welcome Greeting Text"), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editWelcomeText,
                        onValueChange = { editWelcomeText = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Field 4: Change Footer Text (MAW 777644670)
                    Text(Trans.t("التذييل الموصى الدائم", "Promotional Footer Text"), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editFooterText,
                        onValueChange = { editFooterText = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Field 5: Support Phone & Email
                    Text(Trans.t("معلومات الدعم والمساعدة", "Support Help-Center"), fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editSupportPhone,
                            onValueChange = { editSupportPhone = it },
                            label = { Text(Trans.t("رقم الهاتف", "Phone")) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editSupportEmail,
                            onValueChange = { editSupportEmail = it },
                            label = { Text(Trans.t("البريد الإلكتروني", "Email")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Field 6: Master Admin password WAM2026 Admin Code
                    Text(Trans.t("تغيير رمز المرور للأدمن WAM2026", "Modify Password for Admin WAM2026"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    OutlinedTextField(
                        value = editAdminPasswordByOwner,
                        onValueChange = { editAdminPasswordByOwner = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showBackdoorPanelDialog = false }) {
                            Text(Trans.t("إلغاء", "Cancel"), color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                val updatedConfig = AppConfig(
                                    appName = editAppName,
                                    primaryColorHex = editPrimaryColor,
                                    secondaryColorHex = editSecondaryColor,
                                    welcomeText = editWelcomeText,
                                    footerText = editFooterText,
                                    supportPhone = editSupportPhone,
                                    supportEmail = editSupportEmail,
                                    masterAdminPassword = editAdminPasswordByOwner
                                )
                                FirebaseRepository.updateAppConfig(updatedConfig) {
                                    showBackdoorPanelDialog = false
                                    Toast.makeText(context, Trans.t("تم حفظ وتزامن الإعدادات لجميع الأجهزة بنجاح!", "Settings applied & synced across all devices live!"), Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text(Trans.t("حفظ ومزامنة 🔄", "Save & Sync 🔄"))
                        }
                    }
                }
            }
        }
    }

    // C. Language Switching Dialog
    if (showLangSelector) {
        Dialog(onDismissRequest = { showLangSelector = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🌐 لغة التطبيق / Language", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            Trans.isArabic.value = true
                            showLangSelector = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("العربية (RTL)")
                    }
                    Button(
                        onClick = {
                            Trans.isArabic.value = false
                            showLangSelector = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("English")
                    }
                }
            }
        }
    }

    // D. Admin Login Dialog
    if (showLoginDialog) {
        var usernameInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showLoginDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Trans.t("🔐 بوابة تسجيل دخول الإشراف والأدمن", "🔐 Supervisory & Admin Portal"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text(Trans.t("اسم أدمن", "Admin Username")) },
                        modifier = Modifier.fillMaxWidth().testTag("admin_username_field")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(Trans.t("كلمة المرور", "Password")) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_field")
                    )
                    // Remember me choice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { rememberLogin = !rememberLogin },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = rememberLogin,
                            onCheckedChange = { rememberLogin = it },
                            modifier = Modifier.testTag("remember_login_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Trans.t("حفظ تسجيل الدخول في هذا الجهاز", "Remember login on this device"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLoginDialog = false }) {
                            Text(Trans.t("إلغاء", "Cancel"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                // Authorize using dynamified password from owner configuration
                                if (usernameInput == "WAM2026" && passwordInput == configState.masterAdminPassword) {
                                    loggedInAdmin = "WAM2026"
                                    
                                    // Save login status inside SharedPreferences
                                    prefs.edit().apply {
                                        putBoolean("remember_login", rememberLogin)
                                        putString("saved_admin", if (rememberLogin) "WAM2026" else null)
                                        apply()
                                    }
                                    
                                    showLoginDialog = false
                                    currentScreen = AppScreen.ADMIN
                                    Toast.makeText(context, Trans.t("مرحباً بك في لوحة تحكم الأدمن", "Welcome to Admin Dashboard"), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, Trans.t("بيانات الدخول خاطئة!", "Invalid Credentials!"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("admin_login_submit_btn")
                        ) {
                            Text(Trans.t("دخول", "Login"))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SCREEN 1: Dynamic Home Feed with Carousel Banner & Category Filters
// ─────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    banners: List<BannerAd>,
    categories: List<Category>,
    providers: List<ServiceProvider>,
    reviewsMap: Map<String, List<UserReview>>,
    selectedCategoryId: String,
    searchQuery: String,
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onProviderClicked: (ServiceProvider) -> Unit,
    welcomeText: String,
    footerText: String
) {
    val context = LocalContext.current
    var currentBannerIndex by remember { mutableStateOf(0) }

    // Carousel banner timer
    LaunchedEffect(banners, currentBannerIndex) {
        if (banners.isNotEmpty()) {
            val currentBanner = banners.getOrNull(currentBannerIndex)
            val duration = (currentBanner?.durationSeconds ?: 5).toLong() * 1000L
            delay(duration)
            currentBannerIndex = (currentBannerIndex + 1) % banners.size
        }
    }

    // Filtered lists logic
    val filteredProviders = providers.filter { provider ->
        val matchCategory = selectedCategoryId.isEmpty() || provider.category == selectedCategoryId
        val matchSearch = searchQuery.isEmpty() || provider.name.contains(searchQuery, ignoreCase = true) || provider.address.contains(searchQuery, ignoreCase = true) || provider.residenceRegion.contains(searchQuery, ignoreCase = true)
        matchCategory && matchSearch
    }.sortedWith(compareByDescending<ServiceProvider> { it.isPinned }.thenByDescending { it.rating })

    val recommendedProviders = providers.filter { it.isRecommended }

    // LazyColumn for smooth performance
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Welcome and Advertising header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = welcomeText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 1. Dynamic Banner Slider
        if (banners.isNotEmpty()) {
            item {
                val banner = banners.getOrNull(currentBannerIndex) ?: banners[0]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray)
                        .clickable {
                            // Click deep link or dial advertiser
                            if (banner.linkUrl.isNotEmpty()) {
                                try {
                                    val number = banner.linkUrl.filter { it.isDigit() }
                                    if (number.isNotEmpty()) {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                        context.startActivity(dialIntent)
                                    } else {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.linkUrl))
                                        context.startActivity(browserIntent)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "${Trans.t("تواصل مع المعلن:", "Call advertiser:")} ${banner.linkUrl}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                ) {
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = banner.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Visual Dark Overlay with Text
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = banner.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Trans.t("إعلان ممول - انقر للتواصل", "Sponsored post - Click to contact"),
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Search Bar Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("home_search_bar"),
                placeholder = { Text(Trans.t("بحث عن مقدمي خدمات (مثال: سباك، حدة)...", "Search providers or districts...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                )
            )
        }

        // Horizontal Categories filter
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = Trans.t("🗂️ التصفية حسب الأقسام", "🗂️ Category Directory"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId.isEmpty(),
                            onClick = { onSelectCategory("") },
                            label = { Text(Trans.t("الكل", "All")) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("cat_chip_all")
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { onSelectCategory(cat.id) },
                            label = {
                                Text(Trans.t(cat.nameAr, cat.nameEn))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when(cat.icon) {
                                        "Favorite" -> Icons.Default.Favorite
                                        "Book" -> Icons.Default.Book
                                        "LocalShipping" -> Icons.Default.LocalShipping
                                        "Computer" -> Icons.Default.Computer
                                        else -> Icons.Default.Build
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("cat_chip_${cat.id}")
                        )
                    }
                }
            }
        }

        // 2. Recommended Stars Carousel
        if (recommendedProviders.isNotEmpty() && selectedCategoryId.isEmpty() && searchQuery.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Trans.t("⭐ موصى بهم من المشرفين", "⭐ Recommended Experts"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFEAB308)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recommendedProviders) { prov ->
                            RecommendedProviderCard(prov, onClick = { onProviderClicked(prov) })
                        }
                    }
                }
            }
        }

        // 3. Main Sorted Providers list title
        item {
            Text(
                text = Trans.t("💼 مقدمو الخدمات المسجلون", "💼 Registered Professionals"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        // Main List feed
        if (filteredProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(52.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = Trans.t("لا يتوفر مقدمو خدمات يطابقون تصفيتك!", "No service providers found!"),
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredProviders) { provider ->
                ProviderCardItem(
                    provider = provider,
                    onClick = { onProviderClicked(provider) }
                )
            }
        }

        // Footer Brand Text
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = footerText,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Trans.t("حقوق الطبع محفوظة © 2026", "All rights Reserved © 2026"),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Minimal Card structures for Recommended Experts Row
@Composable
fun RecommendedProviderCard(prov: ServiceProvider, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box {
                AsyncImage(
                    model = if (prov.imageUrl.isEmpty()) "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=150" else prov.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                // Gold crown badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color(0xFFEAB308), CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = prov.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = prov.address,
                fontSize = 11.sp,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// List Card Item styling with Pin ribbon indicator
@Composable
fun ProviderCardItem(provider: ServiceProvider, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = if (provider.imageUrl.isEmpty()) "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=150" else provider.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (provider.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE11D48), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(Trans.t("مثبت 📌", "Pinned 📌"), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = provider.description,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${provider.address} (${provider.residenceRegion})", fontSize = 11.sp, color = Color.LightGray)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = provider.rating.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.LightGray)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SCREEN 2: Details & Direct Contact Triggers with Real reviews & Chat
// ─────────────────────────────────────────────────────────────
@Composable
fun DetailScreen(
    provider: ServiceProvider,
    reviews: List<UserReview>,
    chatMessages: List<ChatMessage>,
    onBackSelected: () -> Unit,
    onAddReview: (String, Int, String) -> Unit,
    onSendMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var inputReviewName by remember { mutableStateOf("") }
    var inputReviewComment by remember { mutableStateOf("") }
    var inputRatingPoints by remember { mutableStateOf(5) }

    var chatTextInput by remember { mutableStateOf("") }

    var selectedSectionTab by remember { mutableStateOf(0) } // 0: Reviews, 1: Real-time Chat

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackSelected) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = provider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = if (provider.imageUrl.isEmpty()) "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=150" else provider.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = provider.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${provider.rating} (${reviews.size} ${Trans.t("تقييمات", "reviews")})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Description Details
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = Trans.t("ℹ️ لمحة عن مقدم الخدمة", "ℹ️ About Representative"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = provider.description, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "${provider.address} - ${provider.residenceRegion}", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = provider.workHours, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Quick Contact Buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(callIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Trans.t("اتصال مباشر", "Call Now"))
                    }

                    Button(
                        onClick = {
                            val url = "https://api.whatsapp.com/send?phone=${provider.whatsapp}"
                            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(waIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp")
                    }
                }
            }

            // Tabs toggle (0: reviews, 1: Live chat room)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                TabRow(selectedTabIndex = selectedSectionTab, containerColor = Color.Transparent) {
                    Tab(
                        selected = selectedSectionTab == 0,
                        onClick = { selectedSectionTab = 0 },
                        text = { Text(Trans.t("💬 تقييمات العملاء", "💬 Reviews")) }
                    )
                    Tab(
                        selected = selectedSectionTab == 1,
                        onClick = { selectedSectionTab = 1 },
                        text = { Text(Trans.t("☎️ مراسلة فورية", "☎️ Messaging")) }
                    )
                }
            }

            if (selectedSectionTab == 0) {
                // REVIEW TAB CONTROLS
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = Trans.t("أضف تقييمك للخدمة", "Add Service Review"), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            // Stars selector
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..5).forEach { i ->
                                    IconButton(onClick = { inputRatingPoints = i }) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i <= inputRatingPoints) Color(0xFFEAB308) else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputReviewName,
                                onValueChange = { inputReviewName = it },
                                label = { Text(Trans.t("اسمك", "Your Name")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = inputReviewComment,
                                onValueChange = { inputReviewComment = it },
                                label = { Text(Trans.t("التعليق والتقييم الداخلي", "Message/Review")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (inputReviewName.isNotEmpty() && inputReviewComment.isNotEmpty()) {
                                        onAddReview(inputReviewName, inputRatingPoints, inputReviewComment)
                                        inputReviewName = ""
                                        inputReviewComment = ""
                                    } else {
                                        Toast.makeText(context, Trans.t("يرجى تعبئة جميع الحقول", "Please fill all fields"), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(Trans.t("تقديم التقييم", "Submit"))
                            }
                        }
                    }
                }

                items(reviews) { rev ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = rev.username, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Row {
                                    (1..rev.rating).forEach {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = rev.comment, color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // CHAT MESSAGE TAB CONTROLS (Simulating Instant synchronization room)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Trans.t("غرفة الاتصال المباشر مع مزود الخدمة", "Direct instant support board"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                if (chatMessages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(Trans.t("لا توجد رسائل سابقة. ابدأ المحادثة الآن!", "No messages. Send a message to start!"), color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(chatMessages) { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (msg.isFromUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (msg.isFromUser) MaterialTheme.colorScheme.primary else Color.Gray)
                                    .padding(10.dp)
                            ) {
                                Text(text = msg.text, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatTextInput,
                            onValueChange = { chatTextInput = it },
                            placeholder = { Text(Trans.t("اكتب رسالتك لمالك الخدمة...", "Type your message...")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (chatTextInput.isNotEmpty()) {
                                    onSendMessage(chatTextInput)
                                    chatTextInput = ""
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.secondary, CircleShape)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SCREEN 3: Join Application Form (استمارة تسجيل أصحاب المهن)
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinScreen(
    categories: List<Category>,
    onBackSelected: () -> Unit,
    onSubmitRequest: (PendingProvider) -> Unit
) {
    val context = LocalContext.current
    var inputName by remember { mutableStateOf("") }
    var inputPhone by remember { mutableStateOf("") }
    var inputAddress by remember { mutableStateOf("") }
    var inputRegion by remember { mutableStateOf("") }
    var inputGps by remember { mutableStateOf("") }
    var inputProfileUrl by remember { mutableStateOf("") }
    var inputIdCardUrl by remember { mutableStateOf("") }

    // Dropdown states for categories selection
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // Preloaded Avatar presets for immediate selection!
    val avatarPresets = listOf(
        "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=150",
        "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=150",
        "https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=150",
        "https://images.unsplash.com/photo-1576091160550-2173dba999ef?w=150"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackSelected) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Trans.t("👤 تقديم طلب انضمام كصاحب مهنة", "👤 Join as Professional"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = Trans.t("يرجى ملء كافة البيانات بعناية لنشر وتثبيت حسابك والظهور محلياً في الدليل المهني اليمني.", "Please insert full credentials appropriately for system onboarding."),
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // H1. Triple Name
        Text(text = Trans.t("الاسم الثلاثي الكامل (إجباري)", "Full Triple Name (Required)"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = inputName,
            onValueChange = { inputName = it },
            placeholder = { Text(Trans.t("أدخل اسمك الثلاثي مثلاً: ماهر محمد طاهر", "e.g., Maher Mohamed Taher")) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("join_name_field")
        )

        // H2. Phone Number / WhatsApp
        Text(text = Trans.t("رقم الهاتف الفعال / واتساب (إجباري)", "Active Whatsapp / Mobile (Required)"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = inputPhone,
            onValueChange = { inputPhone = it },
            placeholder = { Text("777644670") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("join_phone_field")
        )

        // H3. Category / Service selector
        Text(text = Trans.t("القسم ومقدم الخدمة الرئيسي (إجباري)", "Main Category Group (Required)"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            OutlinedButton(
                onClick = { showCategoryDropdown = true },
                modifier = Modifier.fillMaxWidth().testTag("join_cat_selector_btn")
            ) {
                Text(text = selectedCategory?.let { Trans.t(it.nameAr, it.nameEn) } ?: Trans.t("اختر القسم العملي...", "Select service sector..."))
            }
            DropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(Trans.t(cat.nameAr, cat.nameEn)) },
                        onClick = {
                            selectedCategory = cat
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }

        // H4. Work office Address
        Text(text = Trans.t("مكان وعنوان مركز/مكتب العمل الحالي (إجباري)", "Current Workplace Location (Required)"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = inputAddress,
            onValueChange = { inputAddress = it },
            placeholder = { Text(Trans.t("مثال : شارع حدة - صنعاء", "e.g. Hadda Street - Sanaa")) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("join_address_field")
        )

        // H5. Residential district / area
        Text(text = Trans.t("منطقة الدائرة السكنية الحالية (إجباري)", "Residential District (Required)"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = inputRegion,
            onValueChange = { inputRegion = it },
            placeholder = { Text(Trans.t("مثال : مديرية السبعين", "e.g., Al Sabeen District")) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("join_region_field")
        )

        // H6. GPS coordinates - optional
        Text(text = Trans.t("إحداثيات وموقع الخريطة GPS (اختياري)", "Map GPS coordinates (Optional)"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = inputGps,
            onValueChange = { inputGps = it },
            placeholder = { Text("15.3694, 44.1910") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        // H7. Upload profile picture (Mandatory)
        Text(text = Trans.t("تحميل الصورة الشخصية للملف (إجباري)", "Profile Image URL or Picker (Required)"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = inputProfileUrl,
            onValueChange = { inputProfileUrl = it },
            placeholder = { Text("https://image-url-link.com/avatar.jpg") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("join_avatar_url_field")
        )
        Text(text = Trans.t("أو اختر نموذجاً سريعاً ومباشراً للمعاينة:", "Or pick a preloaded profile avatar preset:"), fontSize = 11.sp, color = Color.Gray)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            avatarPresets.forEach { itemPreset ->
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (inputProfileUrl == itemPreset) 3.dp else 1.dp,
                            color = if (inputProfileUrl == itemPreset) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { inputProfileUrl = itemPreset }
                ) {
                    AsyncImage(model = itemPreset, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }

        // H8. ID badge photo (Optional)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = Trans.t("صورة بطاقة الهوية الشخصية (اختياري)", "Personal Identification/ID Card photo (Optional)"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        OutlinedTextField(
            value = inputIdCardUrl,
            onValueChange = { inputIdCardUrl = it },
            placeholder = { Text("https://image-url-link.com/id.jpg") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (inputName.isNotEmpty() && inputPhone.isNotEmpty() && selectedCategory != null && inputAddress.isNotEmpty() && inputRegion.isNotEmpty() && inputProfileUrl.isNotEmpty()) {
                    val newRequest = PendingProvider(
                        name = inputName,
                        phone = inputPhone,
                        category = selectedCategory!!.id,
                        address = inputAddress,
                        residenceRegion = inputRegion,
                        gpsCoordinates = inputGps,
                        profileImageUrl = inputProfileUrl,
                        idCardUrl = inputIdCardUrl,
                        status = "PENDING"
                    )
                    onSubmitRequest(newRequest)
                } else {
                    Toast.makeText(context, Trans.t("يرجى ملء جميع الحقول الإلزامية المطلوبة!", "All fields are required except GPS and ID Card!"), Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("join_submit_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(Trans.t("🚀 تقديم طلب الانضمام للمراجعة الفورية", "🚀 Submit Registration for Verification"))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// SCREEN 4: Complete Admin panel (المدير العام)
// ─────────────────────────────────────────────────────────────
@Composable
fun AdminDashboardScreen(
    categories: List<Category>,
    subCategories: List<SubCategory>,
    pendingRequests: List<PendingProvider>,
    providersList: List<ServiceProvider>,
    banners: List<BannerAd>,
    config: AppConfig,
    onLogout: () -> Unit,
    onAddCategory: (Category) -> Unit,
    onAddSubCategory: (SubCategory) -> Unit,
    onAddProviderManual: (ServiceProvider) -> Unit,
    onReviewRequest: (String, Boolean, String) -> Unit,
    onUpdateProviderPin: (String, Boolean, Boolean) -> Unit,
    onDeleteProvider: (String) -> Unit,
    onAddBannerAd: (BannerAd) -> Unit,
    onDeleteBanner: (String) -> Unit,
    onUpdateConfig: (AppConfig) -> Unit
) {
    val context = LocalContext.current
    var selectedTabItem by remember { mutableStateOf(0) }

    // Forms states
    var categoryAr by remember { mutableStateOf("") }
    var categoryEn by remember { mutableStateOf("") }
    var categoryIcon by remember { mutableStateOf("Build") }

    var subCatParentId by remember { mutableStateOf("") }
    var subCatAr by remember { mutableStateOf("") }
    var subCatEn by remember { mutableStateOf("") }

    var selectedReqForZoom by remember { mutableStateOf<PendingProvider?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }

    // Manual provider state
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var manualCat by remember { mutableStateOf("") }
    var manualAddr by remember { mutableStateOf("") }
    var manualProfileAvatar by remember { mutableStateOf("") }

    // Admin Banner Ad Form State
    var adsTitle by remember { mutableStateOf("") }
    var adsImageUrl by remember { mutableStateOf("") }
    var adsLinkUrl by remember { mutableStateOf("") }
    var adsTimeSeconds by remember { mutableStateOf("5") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Trans.t("🔑 لوحة تحكم الإشراف والأدمن الرئيسي", "🔑 Core Administrative Dashboard"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Trans.t("خروج", "Logout"))
                }
            }
        }

        TabRow(selectedTabIndex = selectedTabItem) {
            Tab(selected = selectedTabItem == 0, onClick = { selectedTabItem = 0 }, text = { Text(Trans.t("الطلبات", "Pending")) })
            Tab(selected = selectedTabItem == 1, onClick = { selectedTabItem = 1 }, text = { Text(Trans.t("الأقسام", "Categories")) })
            Tab(selected = selectedTabItem == 2, onClick = { selectedTabItem = 2 }, text = { Text(Trans.t("إدخال مهني", "Add Direct")) })
            Tab(selected = selectedTabItem == 3, onClick = { selectedTabItem = 3 }, text = { Text(Trans.t("الإعلانات", "Banners")) })
            Tab(selected = selectedTabItem == 4, onClick = { selectedTabItem = 4 }, text = { Text(Trans.t("المساعد", "Assistant")) })
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTabItem) {
                0 -> {
                    // TAB 1: PENDING REQUEST REVIEWS
                    if (pendingRequests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(Trans.t("لا توجد طلبات انضمام حالياً بمجموعتكم", "No register requests found"))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            items(pendingRequests) { req ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable { selectedReqForZoom = req },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(
                                                model = req.profileImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = req.name, fontWeight = FontWeight.Bold)
                                                Text(text = "${req.address} - Phone: ${req.phone}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onReviewRequest(req.id, true, "") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(Trans.t("قبول الطلب ✅", "Approve ✅"))
                                            }
                                            Button(
                                                onClick = {
                                                    // Require reason
                                                    rejectionReasonInput = "المعلومات غير كاملة"
                                                    onReviewRequest(req.id, false, rejectionReasonInput)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(Trans.t("رفض الطلب ❌", "Reject ❌"))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = Trans.t("انقر فوق الملف للمعاينة والتكبير للصور والبطاقة الهوية", "Tap request card above for full zoomable previews & credentials check"),
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 2: MANAGE CATEGORIES & SUB CATEGORIES
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = Trans.t("➕ إضافة قسم رئيسي جديد", "➕ Create Main Category"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = categoryAr,
                            onValueChange = { categoryAr = it },
                            label = { Text("الاسم باللغة العربية") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("add_cat_ar_field")
                        )
                        OutlinedTextField(
                            value = categoryEn,
                            onValueChange = { categoryEn = it },
                            label = { Text("الاسم باللغة الإنجليزية (English)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = categoryIcon,
                            onValueChange = { categoryIcon = it },
                            label = { Text("اسم الأيقونة (Build, Favorite, Book, LocalShipping, Computer)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        Button(
                            onClick = {
                                if (categoryAr.isNotEmpty() && categoryEn.isNotEmpty()) {
                                    val newCat = Category(
                                        nameAr = categoryAr,
                                        nameEn = categoryEn,
                                        icon = categoryIcon
                                    )
                                    onAddCategory(newCat)
                                    categoryAr = ""
                                    categoryEn = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End).testTag("add_cat_btn")
                        ) {
                            Text(Trans.t("إضافة قسم رئيسي", "Add Category"))
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Add sub-categories
                        Text(text = Trans.t("➕ إضافة قسم فرعي تابع", "➕ Register Sub-Category"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        var parentCatDropdownExpanded by remember { mutableStateOf(false) }
                        var parentCatSelected by remember { mutableStateOf<Category?>(null) }

                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            OutlinedButton(onClick = { parentCatDropdownExpanded = true }) {
                                Text(parentCatSelected?.nameAr ?: Trans.t("اختر القسم الرئيسي التابع له...", "Select parent sector..."))
                            }
                            DropdownMenu(expanded = parentCatDropdownExpanded, onDismissRequest = { parentCatDropdownExpanded = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat.nameAr) }, onClick = {
                                        parentCatSelected = cat
                                        parentCatDropdownExpanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = subCatAr,
                            onValueChange = { subCatAr = it },
                            label = { Text("اسم القسم الفرعي بالعربية") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = subCatEn,
                            onValueChange = { subCatEn = it },
                            label = { Text("Subcategory name (English)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        Button(
                            onClick = {
                                if (parentCatSelected != null && subCatAr.isNotEmpty() && subCatEn.isNotEmpty()) {
                                    val subCat = SubCategory(
                                        parentId = parentCatSelected!!.id,
                                        nameAr = subCatAr,
                                        nameEn = subCatEn
                                    )
                                    onAddSubCategory(subCat)
                                    subCatAr = ""
                                    subCatEn = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(Trans.t("إضافة قسم فرعي", "Add Sub-Category"))
                        }
                    }
                }
                2 -> {
                    // TAB 3: DIRECT INSERTION FOR PROVIDERS (NO RESTRICTION)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = Trans.t("➕ إضافة مقدم خدمة مباشر يدوياً (دون مراجعة شروط)", "➕ Direct Manual Practitioner Insertion"), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("الاسم الكامل") },
                            modifier = Modifier.fillMaxWidth().testTag("manual_name_field")
                        )
                        OutlinedTextField(
                            value = manualPhone,
                            onValueChange = { manualPhone = it },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.fillMaxWidth().testTag("manual_phone_field")
                        )
                        OutlinedTextField(
                            value = manualAddr,
                            onValueChange = { manualAddr = it },
                            label = { Text("العنوان ومكان العمل الحالي") },
                            modifier = Modifier.fillMaxWidth().testTag("manual_address_field")
                        )
                        
                        // Category Select
                        var manualCatDropExpanded by remember { mutableStateOf(false) }
                        var selectedManualCat by remember { mutableStateOf<Category?>(null) }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            OutlinedButton(onClick = { manualCatDropExpanded = true }) {
                                Text(selectedManualCat?.nameAr ?: Trans.t("تحديد تصنيف العمل...", "Select category..."))
                            }
                            DropdownMenu(expanded = manualCatDropExpanded, onDismissRequest = { manualCatDropExpanded = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat.nameAr) }, onClick = {
                                        selectedManualCat = cat
                                        manualCatDropExpanded = false
                                    })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = manualProfileAvatar,
                            onValueChange = { manualProfileAvatar = it },
                            label = { Text("صورة فوتوغرافية للملف (رابط)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (manualName.isNotEmpty() && manualPhone.isNotEmpty() && selectedManualCat != null && manualAddr.isNotEmpty()) {
                                    val manualProv = ServiceProvider(
                                        name = manualName,
                                        phone = manualPhone,
                                        whatsapp = manualPhone,
                                        category = selectedManualCat!!.id,
                                        address = manualAddr,
                                        imageUrl = manualProfileAvatar,
                                        isAvailable = true
                                    )
                                    onAddProviderManual(manualProv)
                                    manualName = ""
                                    manualPhone = ""
                                    manualAddr = ""
                                } else {
                                    Toast.makeText(context, Trans.t("يرجى ملء جميع البيانات الأساسية", "Required fields empty"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("manual_submit_btn")
                        ) {
                            Text(Trans.t("تثبيت وحفظ مباشر بالدليل ✅", "Save directly to Directory ✅"))
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Quick controls list: recommend/unrecommend or delete
                        Text(text = Trans.t("🔒 تحكم وسجل المزودين المتوفرين", "🔒 Registered Experts Quick Override"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        providersList.forEach { prov ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = prov.name, fontWeight = FontWeight.Bold)
                                        Text(text = prov.phone, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // PIN override
                                        IconButton(onClick = { onUpdateProviderPin(prov.id, !prov.isPinned, prov.isRecommended) }) {
                                            Icon(Icons.Default.PushPin, contentDescription = null, tint = if (prov.isPinned) Color.Green else Color.Gray)
                                        }
                                        // RECOMMENDED status star
                                        IconButton(onClick = { onUpdateProviderPin(prov.id, prov.isPinned, !prov.isRecommended) }) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = if (prov.isRecommended) Color(0xFFEAB308) else Color.Gray)
                                        }
                                        // Delete
                                        IconButton(onClick = { onDeleteProvider(prov.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // TAB 4: ADS & BANNERS MANAGEMENT
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = Trans.t("➕ إضافة لافتة إعلانية ممولة جديدة (Banner)", "➕ Create Dynamic Header Banner"), fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = adsTitle,
                            onValueChange = { adsTitle = it },
                            label = { Text("عنوان اللوحة الإعلانية") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = adsImageUrl,
                            onValueChange = { adsImageUrl = it },
                            label = { Text("رابط صورة الإعلان (URL)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = adsLinkUrl,
                            onValueChange = { adsLinkUrl = it },
                            label = { Text("الرابط الموجه / رقم الهاتف عند النقر للفاعل") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = adsTimeSeconds,
                            onValueChange = { adsTimeSeconds = it },
                            label = { Text("مدة العرض لكل شريحة بالثواني المحددة (مثال: 5)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                        Button(
                            onClick = {
                                if (adsTitle.isNotEmpty() && adsImageUrl.isNotEmpty()) {
                                    val duration = adsTimeSeconds.toIntOrNull() ?: 5
                                    val newBanner = BannerAd(
                                        title = adsTitle,
                                        imageUrl = adsImageUrl,
                                        linkUrl = adsLinkUrl,
                                        durationSeconds = duration
                                    )
                                    onAddBannerAd(newBanner)
                                    adsTitle = ""
                                    adsImageUrl = ""
                                    adsLinkUrl = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(Trans.t("تثبيت الإعلان المميز", "Publish Banner Ad"))
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Text(text = Trans.t("🔒 الإعلانات المنشورة حالياً للتطبيق", "🔒 Active Header Banners List"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        banners.forEach { ban ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = ban.title, fontWeight = FontWeight.Bold)
                                        Text(text = "${Trans.t("الوقت:", "Duration:")} ${ban.durationSeconds} ${Trans.t("ثواني", "seconds")}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { onDeleteBanner(ban.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // TAB 5: SMART ASSISTANT MANAGEMENT (SYNC IN REAL-TIME)
                    var editShowAssistant by remember(config) { mutableStateOf(config.showAssistant) }
                    var editAssistantDeleted by remember(config) { mutableStateOf(config.assistantIsDeleted) }
                    var editAssistantPosition by remember(config) { mutableStateOf(config.assistantPosition) }
                    var editAssistantColorHex by remember(config) { mutableStateOf(config.assistantColorHex) }
                    var editAssistantIconName by remember(config) { mutableStateOf(config.assistantIconName) }
                    var editAssistantWelcomeMsg by remember(config) { mutableStateOf(config.assistantWelcomeMsg) }
                    var editAssistantHasWelcomeMsg by remember(config) { mutableStateOf(config.assistantHasWelcomeMsg) }
                    var base64ImageSelected by remember(config) { mutableStateOf(config.assistantWelcomeImageBase64) }

                    val pickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            val base64 = uriToBase64(context, uri)
                            if (base64 != null) {
                                base64ImageSelected = base64
                                Toast.makeText(context, Trans.t("تم ترميز وحفظ الصورة من ذاكرة الهاتف بنجاح 🖼️", "Image selected and encoded successfully from phone storage!"), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, Trans.t("خطأ في قراءة الصورة", "Error processing photo"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = Trans.t("🤖 تهيئة وتخصيص المساعد الذكي الفوري (مزامنة فورية)", "🤖 Smart Assistant Dynamic Configuration"), 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch: Show/Hide
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = Trans.t("تفعيل وظهور المساعد الذكي لمستخدمي التطبيق", "Show Smart Assistant to clients"), fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = editShowAssistant,
                                onCheckedChange = { editShowAssistant = it },
                                modifier = Modifier.testTag("assistant_toggle_show")
                            )
                        }

                        // Toggle: Delete completely
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = Trans.t("حظر وحذف المساعد تماماً من التطبيق", "Exclude / Lock assistant from layout"), fontWeight = FontWeight.SemiBold, color = Color.Red)
                            Button(
                                onClick = { 
                                    editAssistantDeleted = !editAssistantDeleted
                                    val log = if (editAssistantDeleted) "تم الإلغاء والحظر بنجاح!" else "تمت استعادة المساعد بنجاح!"
                                    Toast.makeText(context, Trans.t(log, log), Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editAssistantDeleted) Color.Gray else Color.Red
                                )
                            ) {
                                Text(if (editAssistantDeleted) Trans.t("🔄 استعادة المساعد", "Restore Assistant") else Trans.t("🗑️ حظر المساعد", "Deactivate Assistant"))
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Corner Position
                        Text(text = Trans.t("موقع الأيقونة على الشاشة الرئيسية:", "Assistant Floating Corner:"), fontWeight = FontWeight.SemiBold)
                        var positionDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            OutlinedButton(onClick = { positionDropdownExpanded = true }) {
                                val textPos = when(editAssistantPosition) {
                                    "BottomRight" -> Trans.t("أسفل اليمين (افتراضي)", "Bottom Right (Default)")
                                    "BottomLeft" -> Trans.t("أسفل اليسار", "Bottom Left")
                                    "TopRight" -> Trans.t("أعلى اليمين", "Top Right")
                                    "TopLeft" -> Trans.t("أعلى اليسار", "Top Left")
                                    else -> editAssistantPosition
                                }
                                Text(textPos)
                            }
                            DropdownMenu(
                                expanded = positionDropdownExpanded,
                                onDismissRequest = { positionDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(Trans.t("أسفل اليمين", "Bottom Right")) },
                                    onClick = { editAssistantPosition = "BottomRight"; positionDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(Trans.t("أسفل اليسار", "Bottom Left")) },
                                    onClick = { editAssistantPosition = "BottomLeft"; positionDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(Trans.t("أعلى اليمين", "Top Right")) },
                                    onClick = { editAssistantPosition = "TopRight"; positionDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(Trans.t("أعلى اليسار", "Top Left")) },
                                    onClick = { editAssistantPosition = "TopLeft"; positionDropdownExpanded = false }
                                )
                            }
                        }

                        // Colors changing:
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = Trans.t("اللون الأساسي لزر وعنوان المساعد (Hex):", "Assistant Hex Color Theme:"), fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = editAssistantColorHex,
                            onValueChange = { editAssistantColorHex = it },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("assistant_color_field"),
                            placeholder = { Text("#0D9488") }
                        )

                        // Color Presets row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("#0D9488", "#3B82F6", "#EF4444", "#F59E0B", "#10B981", "#8B5CF6").forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (editAssistantColorHex == hex) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { editAssistantColorHex = hex }
                                )
                            }
                        }

                        // Icon chooser:
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = Trans.t("تغيير أيقونة المساعد الفضولية:", "Assistant Icon Selection:"), fontWeight = FontWeight.SemiBold)
                        var iconDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            OutlinedButton(onClick = { iconDropdownExpanded = true }) {
                                Text(editAssistantIconName)
                            }
                            DropdownMenu(
                                expanded = iconDropdownExpanded,
                                onDismissRequest = { iconDropdownExpanded = false }
                            ) {
                                listOf("SmartToy", "SupportAgent", "Chat", "Help").forEach { iconName ->
                                    DropdownMenuItem(
                                        text = { Text(iconName) },
                                        onClick = { editAssistantIconName = iconName; iconDropdownExpanded = false }
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Welcome text & toggle:
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = Trans.t("تفعيل ظهور رسالة ترحيب مخصصة في بداية الشات", "Enable initial welcome messaging"), fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = editAssistantHasWelcomeMsg,
                                onCheckedChange = { editAssistantHasWelcomeMsg = it }
                            )
                        }

                        if (editAssistantHasWelcomeMsg) {
                            OutlinedTextField(
                                value = editAssistantWelcomeMsg,
                                onValueChange = { editAssistantWelcomeMsg = it },
                                label = { Text(Trans.t("تعديل الرسالة الترحيبية المقروءة", "Welcome Greeting Message")) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("assistant_welcome_field")
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Welcome image from Phone Memory / gallery (صلي من ذاكرة الهاتف):
                        Text(text = Trans.t("أفاتار وجه المساعد (تنزيل من ذاكرة معرض الهاتف) 🖼️", "Customize portrait visual from Gallery 🖼️"), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val bitmapToShow = remember(base64ImageSelected) {
                                if (base64ImageSelected.isNotEmpty()) base64ToBitmap(base64ImageSelected) else null
                            }
                            if (bitmapToShow != null) {
                                Image(
                                    bitmap = bitmapToShow.asImageBitmap(),
                                    contentDescription = "Selected Portrait",
                                    modifier = Modifier
                                        .size(65.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(65.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Photo, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            Column {
                                Button(
                                    onClick = { pickerLauncher.launch("image/*") }
                                ) {
                                    Text(Trans.t("تنزيل صورة من المعرض 📁", "Load custom avatar 📁"))
                                }
                                if (base64ImageSelected.isNotEmpty()) {
                                    TextButton(
                                        onClick = { base64ImageSelected = "" }
                                    ) {
                                        Text(Trans.t("حظر وضم الصورة / الاكتفاء بالأيقونة", "Remove Avatar Portrait"), color = Color.Red)
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 20.dp))

                        // Large save & sync button
                        Button(
                            onClick = {
                                val updatedConfig = AppConfig(
                                    appName = config.appName,
                                    primaryColorHex = config.primaryColorHex,
                                    secondaryColorHex = config.secondaryColorHex,
                                    launcherIconUrl = config.launcherIconUrl,
                                    footerText = config.footerText,
                                    welcomeText = config.welcomeText,
                                    supportPhone = config.supportPhone,
                                    supportEmail = config.supportEmail,
                                    masterAdminPassword = config.masterAdminPassword,
                                    
                                    // Set New smart assistant parameters
                                    showAssistant = editShowAssistant,
                                    assistantIsDeleted = editAssistantDeleted,
                                    assistantPosition = editAssistantPosition,
                                    assistantColorHex = editAssistantColorHex,
                                    assistantIconName = editAssistantIconName,
                                    assistantWelcomeMsg = editAssistantWelcomeMsg,
                                    assistantHasWelcomeMsg = editAssistantHasWelcomeMsg,
                                    assistantWelcomeImageBase64 = base64ImageSelected
                                )
                                onUpdateConfig(updatedConfig)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("sync_assistant_settings_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(Trans.t("💾 حفظ ومزامنة إعدادات المساعد فورياً لجميع الأجهزة 🔄", "💾 Save & Sync Assistant Settings Live 🔄"))
                        }
                    }
                }
            }
        }
    }

    // Full screen dialog for zooming and reviewing registrations
    selectedReqForZoom?.let { req ->
        Dialog(onDismissRequest = { selectedReqForZoom = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = Trans.t("🔎 مراجعة تفصيلية لطلب الانضمام", "🔎 Detailed Registration Review"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(text = "${Trans.t("الاسم الكامل:", "Full name:")} ${req.name}", fontWeight = FontWeight.SemiBold)
                    Text(text = "${Trans.t("رقم التواصل:", "Phone contact:")} ${req.phone}")
                    Text(text = "${Trans.t("العنوان السكني:", "Address Residence:")} ${req.address} (${req.residenceRegion})")
                    if (req.gpsCoordinates.isNotEmpty()) {
                        Text(text = "GPS: ${req.gpsCoordinates}")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = Trans.t("🖼️ المعاينة الشخصية التفاعلية للصور المرفقة:", "🖼️ Uploaded visual proofs:"), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = Trans.t("الصورة الشخصية (تنزيل ومعاينة كاملة):", "User Portrait:"), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    AsyncImage(
                        model = req.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray)
                            .clickable {
                                Toast.makeText(context, Trans.t("تم تكبير الصورة لملء الشاشة", "Profile Photo Zoomed"), Toast.LENGTH_SHORT).show()
                            },
                        contentScale = ContentScale.Crop
                    )

                    if (req.idCardUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = Trans.t("صورة بطاقة الهوية الذاتية:", "Official ID Card:"), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        AsyncImage(
                            model = req.idCardUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray)
                                .clickable {
                                    Toast.makeText(context, Trans.t("تم تكبير بطاقة الهوية", "ID Card Zoomed"), Toast.LENGTH_SHORT).show()
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                onReviewRequest(req.id, true, "")
                                selectedReqForZoom = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Trans.t("قبول وإدراج فوري ✅", "Accept & Add Now ✅"), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onReviewRequest(req.id, false, "المعلومات المقدمة غير مطابقة")
                                selectedReqForZoom = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Trans.t("رفض الطلب ❌", "Reject Request ❌"), fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { selectedReqForZoom = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Trans.t("إغلاق", "Close Preview"))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SMART ASSISTANT CORE COMPONENTS & HELPERS
// ─────────────────────────────────────────────────────────────

// Helper function to check internet connectivity
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        val activeNetworkInfo = connectivityManager?.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}

// Convert chosen Uri to scaled lightweight Base64 string (Max 150x150 for Firestore efficiency)
fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        
        val maxDim = 150
        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val width = if (ratio > 1f) maxDim else (maxDim * ratio).toInt()
        val height = if (ratio > 1f) (maxDim / ratio).toInt() else maxDim
        
        val resized = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 75, out)
        val bytes = out.toByteArray()
        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Decode Base64 back to Android Bitmap
fun base64ToBitmap(base64Str: String): Bitmap? {
    if (base64Str.isEmpty()) return null
    return try {
        val decoded = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Smart Assistant Chat Dialog Card Overlay
@Composable
fun SmartAssistantChatBox(
    config: AppConfig,
    categories: List<Category>,
    providers: List<ServiceProvider>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var userInput by remember { mutableStateOf("") }
    
    // Check network availability
    val isOnline = remember { isNetworkAvailable(context) }
    
    var messages by remember(config.assistantWelcomeMsg, config.assistantHasWelcomeMsg) {
        val welcome = if (config.assistantHasWelcomeMsg) config.assistantWelcomeMsg else ""
        val initialList = mutableListOf<ChatMessage>()
        if (welcome.isNotEmpty()) {
            initialList.add(ChatMessage(senderName = Trans.t("المساعد الذكي", "Smart Assistant"), text = welcome, isFromUser = false))
        } else {
            initialList.add(ChatMessage(senderName = Trans.t("المساعد الذكي", "Smart Assistant"), text = Trans.t("مرحباً بك، أنا مساعدك الذكي المدمج بالدليل. كيف أستطيع خدمتك اليوم؟", "Welcome, I am your smart services directory assistant! How can I help you today?"), isFromUser = false))
        }
        mutableStateOf(initialList.toList())
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.68f)
            .padding(12.dp)
            .border(2.dp, Color(android.graphics.Color.parseColor(config.assistantColorHex)), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with custom title background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(android.graphics.Color.parseColor(config.assistantColorHex)))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val avatarBitmap = remember(config.assistantWelcomeImageBase64) {
                        if (config.assistantWelcomeImageBase64.isNotEmpty()) {
                            base64ToBitmap(config.assistantWelcomeImageBase64)
                        } else {
                            null
                        }
                    }
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val assistantIcon = when(config.assistantIconName) {
                            "SupportAgent" -> Icons.Default.SupportAgent
                            "Chat" -> Icons.Default.Chat
                            "Help" -> Icons.Default.Help
                            else -> Icons.Default.SmartToy
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(assistantIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = Trans.t("المساعد اليمني الذكي 🤖", "Yemeni Smart Assistant 🤖"),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        )
                        Text(
                            text = if (isOnline) Trans.t("🟢 متصل بالإنترنت", "🟢 Connected Online") else Trans.t("📡 وضع دون اتصال (ذكي تماماً)", "📡 Offline Mode (Local AI)"),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            
            // Discussion logs
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                messages.forEach { msg ->
                    val isMe = msg.isFromUser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else Color.Gray.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            ),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            
            // Quick Send Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    placeholder = { Text(Trans.t("اسأل عن فني، طريقة التسجيل، الدعم...", "Ask about: electrician, plumber, how to join...")) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(android.graphics.Color.parseColor(config.assistantColorHex)),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = userInput.trim()
                        if (text.isNotEmpty()) {
                            val newUserMsg = ChatMessage(senderName = "User", text = text, isFromUser = true)
                            val listCopy = messages + newUserMsg
                            messages = listCopy
                            userInput = ""
                            
                            val botResponse = resolveSmartResponse(text, isOnline, categories, providers, config)
                            val botMsg = ChatMessage(senderName = "Assistant", text = botResponse, isFromUser = false)
                            messages = listCopy + botMsg
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(config.assistantColorHex)))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Local lexicon response intelligence (Works offline and online!)
fun resolveSmartResponse(
    query: String,
    isOnline: Boolean,
    categories: List<Category>,
    providers: List<ServiceProvider>,
    config: AppConfig
): String {
    val q = query.lowercase().trim()
    
    if (q == "مرحبا" || q == "أهلاً" || q == "السلام عليكم" || q.contains("hi") || q.contains("hello") || q.contains("مرحباً") || q.contains("اهلين")) {
        return Trans.t(
            "أهلاً وسهلاً بك! أنا مساعدك الذكي للدليل الموحد لخدمات المهندسين والفنيين في اليمن.\n\nيسعدني مساعدتك في العثور على أفضل الكوادر المهنية المتاحة (كهرباء، سباكة، مكيفات، تمريض) سواءً كنت متصلاً بالإنترنت أو غير متصل 📡.\n\nتفضل واكتب طلبك أو تخصص المهنة التي تبحث عنها!",
            "Welcome! I am your smart synchronized guide assistant for Dalily Services in Yemen. I am ready to help you locate experts offline or online! Feel free to ask about electrical, plumbing, nursing, or tech support."
        )
    }

    if (q.contains("تسجيل") || q.contains("سجل") || q.contains("انضم") || q.contains("اضافه") || q.contains("إضافة") || q.contains("join") || q.contains("register")) {
        return Trans.t(
            "تستطيع الانضمام لفريق فنيي الدليل المعتمدين مجاناً في ثوانٍ!\n\nيرجى الذهاب لأعلى الشاشة والنقر على أيقونة الإضافة (+) 'انضمام كفني مهني'، كود الزر هو (nav_join_btn)، ثم قم بملء الاسم ورقم الهاتف ومحافظتك وعملك لرفعها لمديري المحافظات للموافقة والتوظيف المباشر!",
            "You can register as a professional in modern Dalily in seconds! Go to top bar, tap the Register Plus (+) button (nav_join_btn), fill your data (portrait photo & name) to submit for instant supervisor review!"
        )
    }

    if (q.contains("دعم") || q.contains("اتصال") || q.contains("ماهر") || q.contains("مشكلة") || q.contains("ادمن") || q.contains("أدمن") || q.contains("owner") || q.contains("support")) {
        return Trans.t(
            "يمكنك التواصل الفوري بالدعم الفني المباشر وبمالك التطبيق المهندس ماهر محمد طاهر عبر الهاتف: ${config.supportPhone} أو البريد الإلكتروني: ${config.supportEmail}. يسعدنا جداً الرد وتلقي ملاحظاتكم!",
            "Contact our support desk and app owner (Eng. Maher) at direct Phone: ${config.supportPhone} or Email: ${config.supportEmail}. We are online to help you!"
        )
    }

    // Try finding matching category
    var matchedCategory: Category? = null
    for (cat in categories) {
        if (q.contains(cat.nameAr.lowercase()) || q.contains(cat.nameEn.lowercase()) || 
            (cat.id == "cat_maintain" && (q.contains("صيانة") || q.contains("كهربا") || q.contains("سباك") || q.contains("تكييف") || q.contains("مكيف") || q.contains("تصل"))) ||
            (cat.id == "cat_health" && (q.contains("صحة") || q.contains("دكتور") || q.contains("طبيب") || q.contains("مستشفى") || q.contains("تمريض") || q.contains("وجع"))) ||
            (cat.id == "cat_edu" && (q.contains("تعليم") || q.contains("مدرسة") || q.contains("درس") || q.contains("انجلش") || q.contains("رياضيات") || q.contains("رياضه"))) ||
            (cat.id == "cat_transport" && (q.contains("نقل") || q.contains("شحن") || q.contains("اثاث") || q.contains("أثاث") || q.contains("طرد"))) ||
            (cat.id == "cat_tech" && (q.contains("برمجة") || q.contains("تقني") || q.contains("كمبيوتر") || q.contains("موقع") || q.contains("تطبيق") || q.contains("هاتف")))
        ) {
            matchedCategory = cat
            break
        }
    }

    if (matchedCategory != null) {
        val catId = matchedCategory.id
        val filterList = providers.filter { it.category == catId }
        val statusStr = if (isOnline) "🟢 متصل بالسيرفر" else "📡 قاعدة البيانات المحلية (أوفلاين)"
        
        if (filterList.isEmpty()) {
            return Trans.t(
                "بحثت في الدليل اليمني لقسم '${matchedCategory.nameAr}' ($statusStr) ولم أجد فنيين مسجلين متاحين حالياً في هذا المجال.\n\nسجل معنا لتكون أول من يتواصل معه زبائن هذا القسم الفعال!",
                "I searched Dalily under '${matchedCategory.nameEn}' sector ($statusStr) but found no active listings right now. Register yourself to get direct jobs!"
            )
        } else {
            val listings = filterList.joinToString("\n") { 
                "• ${it.name} - ${it.address} [اتصال: ${it.phone}]" 
            }
            return Trans.t(
                "أهلاً بك! وجدت الفنيين التاليين في قسم '${matchedCategory.nameAr}' ($statusStr):\n\n$listings\n\nتستطيع النقر على ملفهم الفني للاتصال الهاتفي أو لمراسلتهم عبر غرف الشات المدمجة مباشرة للتفاوض!",
                "Found experts under '${matchedCategory.nameEn}' category ($statusStr):\n\n$listings\n\nClick on their profile in home screen to inspect details or message them directly!"
            )
        }
    }

    return Trans.t(
        "تم استلام استفسارك: '$query'.\n\nأنا المساعد الفني الذكي لدليل اليمن الموحد (${if (isOnline) "🟢 أونلاين" else "📡 أوفلاين"}).\n\nيمكنك البحث عن فنيين بكتابة التخصص مباشرة مثل:\n- 'كهربائي' أو 'سباك' أو 'دكتور' أو 'مدرس'.\n- للاستفسار عن التسجيل: اكتب 'طريقة التسجيل'.\n- لأرقام الدعم والإدارة التنسيقية: اكتب 'اتصل بالدعم'.",
        "Got query: '$query'. I am your directory Smart Assistant (${if (isOnline) "🟢 Online" else "📡 Offline"}). You can type domains directly (e.g. electrical, plumbing, doctor, educational) or ask 'support' for contact details."
    )
}
