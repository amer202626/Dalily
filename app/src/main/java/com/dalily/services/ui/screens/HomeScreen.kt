package com.dalily.services.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dalily.services.UserRole
import com.dalily.services.data.CustomCategory
import com.dalily.services.data.FirebaseSimulator
import com.dalily.services.data.ServiceProvider
import com.dalily.services.data.ServiceRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    
    // Database states
    val dbState by FirebaseSimulator.dbState.collectAsState()
    val isSyncing by FirebaseSimulator.syncingState.collectAsState()

    // UI Interactive States
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    // Backdoor (Owner click listener)
    var backdoorClicks by remember { mutableStateOf(0) }
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var backdoorPasswordInput by remember { mutableStateOf("") }

    // Admin login dialog state
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var adminUsernameInput by remember { mutableStateOf("") }
    var adminPasswordInput by remember { mutableStateOf("") }

    // Create service request dialog (إضافة خدمتي)
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var newServiceName by remember { mutableStateOf("") }
    var newServiceCategory by remember { mutableStateOf("") }
    var newServicePhone by remember { mutableStateOf("") }
    var newServiceWhatsapp by remember { mutableStateOf("") }
    var newServiceDesc by remember { mutableStateOf("") }

    // Category modify state
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf<CustomCategory?>(null) }
    var categoryArInput by remember { mutableStateOf("") }
    var categoryEnInput by remember { mutableStateOf("") }
    var categoryColorInput by remember { mutableStateOf("#3B82F6") }

    // Sliding banners automatic transition
    var activeBannerIndex by remember { mutableStateOf(0) }
    LaunchedEffect(dbState.banners) {
        if (dbState.banners.isNotEmpty()) {
            while (true) {
                delay(4000)
                activeBannerIndex = (activeBannerIndex + 1) % dbState.banners.size
            }
        }
    }

    // Reset backdoor clicks after some timeout inactivity
    LaunchedEffect(backdoorClicks) {
        if (backdoorClicks > 0) {
            delay(3000)
            backdoorClicks = 0
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clickable {
                                backdoorClicks++
                                if (backdoorClicks >= 5) {
                                    backdoorClicks = 0
                                    showBackdoorDialog = true
                                }
                            }
                            .testTag("app_logo_clickable")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "دليلي للخدمات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الدليل الطبي والمهني الأول في اليمن",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Sync Indicator
                    IconButton(
                        onClick = {
                            scope.launch {
                                val success = FirebaseSimulator.syncWithCloud(context)
                                Toast.makeText(
                                    context,
                                    if (success) "تم تحديث البيانات من السحاب بنجاح 🗸" else "فشل الاتصال بالخادم السحابي! يعمل أوفلاين.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.testTag("btn_sync")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "مزامنة السحاب",
                            tint = if (isSyncing) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    // Admin controls or entry button
                    if (currentRole == UserRole.USER) {
                        TextButton(
                            onClick = { showAdminLoginDialog = true },
                            modifier = Modifier.testTag("btn_open_login")
                        ) {
                            Text("دخول المشرف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val roleBadge = if (currentRole == UserRole.OWNER) "المالك 👑" else "مشرف 🛡️"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(roleBadge, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp))
                            IconButton(onClick = { onLogout() }) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "تسجيل خروج", tint = Color.Red)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("favorites") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "المفضلة") },
                    label = { Text("المفضلة", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("chat_list") },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "محادثاتي") },
                    label = { Text("محادثاتي", fontSize = 11.sp) }
                )
                if (currentRole == UserRole.ADMIN || currentRole == UserRole.OWNER) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("admin_dashboard") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "لوحة التحكم") },
                        label = { Text("لوحة التحكم", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("btn_navigation_admin")
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن سباك، عيادة، مهندس...", fontSize = 13.sp) },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Banners Slider
            if (dbState.banners.isNotEmpty()) {
                item {
                    val currentBanner = dbState.banners.getOrNull(activeBannerIndex) ?: dbState.banners.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = currentBanner.imageUrl,
                                contentDescription = currentBanner.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = currentBanner.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Categories Section Title with add button for Admins
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تصفح أقسام الخدمات الموثقة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    if (currentRole == UserRole.ADMIN || currentRole == UserRole.OWNER) {
                        TextButton(
                            onClick = {
                                categoryArInput = ""
                                categoryEnInput = ""
                                categoryColorInput = "#3B82F6"
                                showAddCategoryDialog = true
                            },
                            modifier = Modifier.testTag("btn_add_category")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة قسم", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة قسم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Categories list (Scrollable horizontally)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("الكل", fontSize = 12.sp) }
                        )
                    }
                    items(dbState.categories) { cat ->
                        Box {
                            FilterChip(
                                selected = selectedCategory == cat.nameAr,
                                onClick = { selectedCategory = cat.nameAr },
                                label = { Text(cat.nameAr, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.combinedClickable(
                                    onClick = { selectedCategory = cat.nameAr },
                                    onLongClick = {
                                        if (currentRole == UserRole.ADMIN || currentRole == UserRole.OWNER) {
                                            categoryArInput = cat.nameAr
                                            categoryEnInput = cat.nameEn
                                            categoryColorInput = cat.colorHex
                                            showEditCategoryDialog = cat
                                        }
                                    }
                                )
                            )
                            
                            // Delete chip modifier for logged in admins
                            if (currentRole == UserRole.ADMIN || currentRole == UserRole.OWNER) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .clickable {
                                            FirebaseSimulator.deleteCategory(context, cat.id)
                                            if (selectedCategory == cat.nameAr) selectedCategory = null
                                            Toast.makeText(context, "تم حذف القسم بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف القسم", tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Submit my service card (إضافة خدمتي)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("هل أنت مقدم خدمة مهنية أو طبية؟", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("أضف بياناتك واخدم الآلاف في دليلك الموثوق مجاناً.", fontSize = 10.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                newServiceName = ""
                                newServiceCategory = dbState.categories.firstOrNull()?.nameAr ?: "سباكة وصيانة"
                                newServicePhone = ""
                                newServiceWhatsapp = ""
                                newServiceDesc = ""
                                showAddServiceDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_request_provider")
                        ) {
                            Text("إرسال خدمتي 🗸", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Providers Header
            item {
                Text(
                    text = "مقدمي الخدمات المتوفرين حالياً",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Filtered Service Providers List
            val filteredProviders = dbState.providers.filter { provider ->
                val matchesCategory = selectedCategory == null || provider.category == selectedCategory
                val matchesSearch = provider.name.contains(searchQuery, ignoreCase = true) ||
                        provider.description.contains(searchQuery, ignoreCase = true) ||
                        provider.category.contains(searchQuery, ignoreCase = true) ||
                        provider.tags.any { it.contains(searchQuery, ignoreCase = true) }
                matchesCategory && matchesSearch
            }

            if (filteredProviders.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "لا يوجد بيانات", modifier = Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("عذراً، لم نجد أي مقدم خدمات يطابق بحثك!", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                items(filteredProviders) { provider ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("detail/${provider.id}") }
                            .testTag("provider_card_${provider.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Service Provider Photo
                            Box {
                                val placeholderImage = "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=300"
                                AsyncImage(
                                    model = if (provider.imageUrl.isEmpty()) placeholderImage else provider.imageUrl,
                                    contentDescription = provider.name,
                                    modifier = Modifier
                                        .size(64.dp)
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

                            // Provider Metadata
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.category,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                        Text(" ${provider.views} مشاهدة", fontSize = 9.sp, color = Color.Gray)
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

                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = "تقييم", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                    Text(
                                        text = " ${provider.rating} (${provider.reviewsCount} تقييم)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(Icons.Default.LocationOn, contentDescription = "العنوان", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = provider.address,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Owner backdoor entry modal dialog (بوابة الدخول الخلفية)
    if (showBackdoorDialog) {
        Dialog(onDismissRequest = { showBackdoorDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("بوابة الدخول الخلفية لمالك الدليل 👑", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("الرجاء إدخال كلمة المرور السرية لمالك النظام برتبة أعلى:", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    OutlinedTextField(
                        value = backdoorPasswordInput,
                        onValueChange = { backdoorPasswordInput = it },
                        placeholder = { Text("رمز الدخول الماستر") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backdoor_pwd_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (backdoorPasswordInput == "maher--736462") {
                                    onRoleChanged(UserRole.OWNER, "المالك الماستر")
                                    showBackdoorDialog = false
                                    backdoorPasswordInput = ""
                                    Toast.makeText(context, "مرحباً بك يا مالك الدليل! تم الدخول برتبة مالك.", Toast.LENGTH_SHORT).show()
                                    navController.navigate("admin_dashboard")
                                } else {
                                    Toast.makeText(context, "الرمز السري غير صحيح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_backdoor_submit")
                        ) {
                            Text("دخول سريع")
                        }
                        OutlinedButton(
                            onClick = { showBackdoorDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // Admin/Supervisor Login dialog (خالية من البيانات الافتراضية تلبية للمتطلب)
    if (showAdminLoginDialog) {
        Dialog(onDismissRequest = { showAdminLoginDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("تسجيل دخول المشرفين المعتمدين 🛡️", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("الرجاء كتابة اسم المستخدم وكلمة السر الممنوحة لك:", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    OutlinedTextField(
                        value = adminUsernameInput,
                        onValueChange = { adminUsernameInput = it },
                        label = { Text("اسم مستخدم المشرف") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_username_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        label = { Text("رمز المرور") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val match = dbState.admins.find {
                                    it.username == adminUsernameInput && it.passwordHash == adminPasswordInput && it.isActive
                                }
                                if (match != null) {
                                    onRoleChanged(UserRole.ADMIN, match.username)
                                    showAdminLoginDialog = false
                                    adminUsernameInput = ""
                                    adminPasswordInput = ""
                                    Toast.makeText(context, "مرحباً يا مشرف، تم تسجيل دخولك بنجاح!", Toast.LENGTH_SHORT).show()
                                    navController.navigate("admin_dashboard")
                                } else {
                                    Toast.makeText(context, "بيانات المشرف المدخلة خاطئة أو قد تم تعطيل المشرف!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_admin_submit")
                        ) {
                            Text("دخول المشرف")
                        }
                        OutlinedButton(
                            onClick = { showAdminLoginDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // Submit provider application/request (إضافة خدمتي)
    if (showAddServiceDialog) {
        Dialog(onDismissRequest = { showAddServiceDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("تقديم طلب إدراج خدمة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("سيقوم المشرف بمراجعة طلبك وإضافته للدليل في ثوانٍ", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    OutlinedTextField(
                        value = newServiceName,
                        onValueChange = { newServiceName = it },
                        label = { Text("اسم النشاط أو المهندس") },
                        modifier = Modifier.fillMaxWidth().testTag("req_name_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Simple Dropdown selection for category
                    var showCatDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newServiceCategory,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("القسم الرئيسي") },
                            trailingIcon = {
                                IconButton(onClick = { showCatDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(
                            expanded = showCatDropdown,
                            onDismissRequest = { showCatDropdown = false }
                        ) {
                            dbState.categories.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.nameAr) },
                                    onClick = {
                                        newServiceCategory = it.nameAr
                                        showCatDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newServicePhone,
                        onValueChange = { newServicePhone = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth().testTag("req_phone_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = newServiceWhatsapp,
                        onValueChange = { newServiceWhatsapp = it },
                        label = { Text("رقم الواتساب (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = newServiceDesc,
                        onValueChange = { newServiceDesc = it },
                        label = { Text("تفاصيل الخدمة والخبرات") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newServiceName.isEmpty() || newServicePhone.isEmpty()) {
                                    Toast.makeText(context, "الرجاء تعبئة الحقول الإلزامية الاسم والهاتف!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val req = ServiceRequest(
                                        id = "req_${System.currentTimeMillis()}",
                                        providerName = newServiceName,
                                        category = newServiceCategory,
                                        phone = newServicePhone,
                                        whatsapp = newServiceWhatsapp,
                                        description = newServiceDesc,
                                        status = "PENDING"
                                    )
                                    FirebaseSimulator.addServiceRequest(context, req)
                                    Toast.makeText(context, "تم إرسال طلبك بنجاح! شكراً لتقديمك.", Toast.LENGTH_LONG).show()
                                    showAddServiceDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_req_submit")
                        ) {
                            Text("إرسال الطلب")
                        }
                        OutlinedButton(
                            onClick = { showAddServiceDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
    }

    // Add Section Dialog
    if (showAddCategoryDialog) {
        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إضافة قسم / بورتال جديد 🏷️", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = categoryArInput,
                        onValueChange = { categoryArInput = it },
                        label = { Text("الاسم بالعربية") },
                        modifier = Modifier.fillMaxWidth().testTag("cat_ar_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = categoryEnInput,
                        onValueChange = { categoryEnInput = it },
                        label = { Text("الاسم بالإنجليزية") },
                        modifier = Modifier.fillMaxWidth().testTag("cat_en_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = categoryColorInput,
                        onValueChange = { categoryColorInput = it },
                        label = { Text("لون الواجهة (مثال: #3B82F6)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (categoryArInput.isEmpty()) {
                                    Toast.makeText(context, "الاسم بالعربية مطلوب!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val catId = "cat_${System.currentTimeMillis()}"
                                    val cat = CustomCategory(
                                        id = catId,
                                        nameAr = categoryArInput,
                                        nameEn = categoryEnInput,
                                        colorHex = categoryColorInput
                                    )
                                    FirebaseSimulator.addCategory(context, cat)
                                    showAddCategoryDialog = false
                                    Toast.makeText(context, "تم إضافة القسم الجديد بنجاح مالي!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_cat_add_submit")
                        ) {
                            Text("حفظ")
                        }
                        OutlinedButton(
                            onClick = { showAddCategoryDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // Edit Section Dialog
    if (showEditCategoryDialog != null) {
        val cat = showEditCategoryDialog!!
        Dialog(onDismissRequest = { showEditCategoryDialog = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("تعديل تفاصيل القسم 🏷️", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = categoryArInput,
                        onValueChange = { categoryArInput = it },
                        label = { Text("الاسم بالعربية") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = categoryEnInput,
                        onValueChange = { categoryEnInput = it },
                        label = { Text("الاسم بالإنجليزية") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = categoryColorInput,
                        onValueChange = { categoryColorInput = it },
                        label = { Text("لون الواجهة والسمات") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (categoryArInput.isEmpty()) {
                                    Toast.makeText(context, "الاسم بالعربية مطلوب!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val updatedCat = cat.copy(
                                        nameAr = categoryArInput,
                                        nameEn = categoryEnInput,
                                        colorHex = categoryColorInput
                                    )
                                    FirebaseSimulator.updateCategory(context, updatedCat)
                                    showEditCategoryDialog = null
                                    Toast.makeText(context, "تم تعديل القسم بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تحديث")
                        }
                        OutlinedButton(
                            onClick = { showEditCategoryDialog = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
    }
}
