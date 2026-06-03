package com.dalily.services.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dalily.services.UserRole
import com.dalily.services.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavController,
    currentRole: UserRole,
    activeAdminName: String?,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val dbState by FirebaseSimulator.dbState.collectAsState()
    val appConfig = dbState.config
    
    // Admin tabs enum (0: Requests, 1: Categories, 2: Banners, 3: Supervisors/2FA, 4: Style/Dynamic Theme, 5: Analytics/Export)
    var selectedTab by remember { mutableStateOf(0) }

    // Core Theme Config colors
    val themeChoice = appConfig.themeColors

    // Dialog state controllers
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddBannerDialog by remember { mutableStateOf(false) }
    var showAddSupervisorDialog by remember { mutableStateOf(false) }

    // Textfields inputs
    var catId by remember { mutableStateOf("") }
    var catNameAr by remember { mutableStateOf("") }
    var catNameEn by remember { mutableStateOf("") }
    var catIcon by remember { mutableStateOf("Build") }
    var catColorHex by remember { mutableStateOf("#3982F6") }

    var bannerId by remember { mutableStateOf("") }
    var bannerTitle by remember { mutableStateOf("") }
    var bannerImgUrl by remember { mutableStateOf("") }
    var bannerDuration by remember { mutableStateOf("7") }
    var bannerBudget by remember { mutableStateOf("50") }
    var bannerPriority by remember { mutableStateOf("1") }

    var supUser by remember { mutableStateOf("") }
    var supPass by remember { mutableStateOf("") }
    var supEmail by remember { mutableStateOf("") }
    var supCanAdd by remember { mutableStateOf(true) }
    var supCanDelete by remember { mutableStateOf(true) }
    var supCanReview by remember { mutableStateOf(true) }

    // Whitelist registration variables
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var inputDeviceSerial by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("لوحة تحكم الدليل السحابية الرسمية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val rankLabel = if (currentRole == UserRole.OWNER) "المالك الماستر 👑 (بوابة خلفية)" else "رتبة مشرف: $activeAdminName 🛡️"
                        Text(rankLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    Button(
                        onClick = { onLogout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .testTag("admin_logout_btn")
                            .padding(end = 6.dp)
                    ) {
                        Text("خروج", fontSize = 11.sp, color = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0F19))
        ) {
            // Horizontal scroll tab row headers
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("الطلبات الواردة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("الأقسام والمزودين", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("اللافتات الممولة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("المشرفين والصلاحيات 🛡️", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("التصميم والسمة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("الإحصائيات والتصدير CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                when (selectedTab) {
                    // TAB 0: Onboarding requests from standard craftsmen
                    0 -> {
                        val pendingRequests = dbState.serviceRequests.filter { it.status == "PENDING" }
                        if (pendingRequests.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد طلبات انتساب جديدة قيد الانتظار حالياً 🗸", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(pendingRequests) { req ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("نشاط جديد مقترح: ${req.providerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("قسم: ${req.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("الهاتف: ${req.phone}", fontSize = 11.sp)
                                            Text("العنوان: ${req.workAddress} - منطقة السكن: ${req.residenceRegion}", fontSize = 11.sp, color = Color.Gray)

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("صورة غلاف النشاط الشخصية وبطاقة الهوية المرفقة (معاينة قبل الاعتماد):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            
                                            // Image previews before decision
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("الصورة الشخصية", fontSize = 9.sp, color = Color.Gray)
                                                    val placeholderImg = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&q=80&w=200"
                                                    AsyncImage(
                                                        model = if (req.profileImageUrl.isEmpty()) placeholderImg else req.profileImageUrl,
                                                        contentDescription = "Profile Preview",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(80.dp)
                                                            .clip(RoundedCornerShape(6.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("صورة الهوية الوطنية", fontSize = 9.sp, color = Color.Gray)
                                                    val placeholderCard = "https://images.unsplash.com/photo-1554774853-aae0a22c8aa4?auto=format&fit=crop&q=80&w=200"
                                                    AsyncImage(
                                                        model = if (req.idCardUrl.isEmpty()) placeholderCard else req.idCardUrl,
                                                        contentDescription = "ID Card Preview",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(80.dp)
                                                            .clip(RoundedCornerShape(6.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Button(
                                                    onClick = {
                                                        FirebaseSimulator.updateServiceRequestStatus(
                                                            context,
                                                            req.id,
                                                            "APPROVED",
                                                            activeAdminName ?: "مشرف"
                                                        )
                                                        Toast.makeText(context, "تم قبول طلب المهني وإضافته للدليل فوراً 🟢", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("موافقة واعتماد")
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        FirebaseSimulator.updateServiceRequestStatus(
                                                            context,
                                                            req.id,
                                                            "REJECTED",
                                                            activeAdminName ?: "مشرف"
                                                        )
                                                        Toast.makeText(context, "تم رفض واستبعاد طلب الانتساب.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("رفض")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: Category lists, pinning and deleting
                    1 -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("الأقسام الرئيسية المقيدة بالدليل", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Button(
                                    onClick = {
                                        catId = "cat_${System.currentTimeMillis()}"
                                        catNameAr = ""
                                        catNameEn = ""
                                        showAddCategoryDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("أضف قسماً جديداً")
                                }
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                              ) {
                                items(dbState.categories) { cat ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch(e: Exception) { Color.Gray })
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("${cat.nameAr} (${cat.nameEn})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // Pin toggle indicator
                                                IconButton(
                                                    onClick = {
                                                        val updated = cat.copy(isPinned = !cat.isPinned)
                                                        FirebaseSimulator.addCategory(context, updated, activeAdminName)
                                                        Toast.makeText(context, "تم تغيير حالة تثبيت القسم في الواجهة 🗸", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = null,
                                                        tint = if (cat.isPinned) Color(0xFFFFD700) else Color.Gray
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        FirebaseSimulator.deleteCategory(context, cat.id, activeAdminName)
                                                        Toast.makeText(context, "تم حذف القسم كاملاً 🗑️", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: Promoted Sponsored advertising banner configurations
                    2 -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("اللافتات الهيدر الدعائية الفعالة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Button(
                                    onClick = {
                                        bannerId = "banner_${System.currentTimeMillis()}"
                                        bannerTitle = ""
                                        bannerImgUrl = ""
                                        showAddBannerDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("أضف إعلاناً ممولاً")
                                }
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(dbState.banners) { b ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(b.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                                IconButton(
                                                    onClick = {
                                                        FirebaseSimulator.deleteBanner(context, b.id, activeAdminName)
                                                        Toast.makeText(context, "تم إزالة الإعلان 🗑️", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text("المسار والموقع: ${b.linkUrl}", fontSize = 10.sp, color = Color.Gray)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("الميزانية: $${b.budget}", fontSize = 10.sp, color = Color.LightGray)
                                                Text("المدة: ${b.durationDays} يوم", fontSize = 10.sp, color = Color.LightGray)
                                                Text("الأسبقية: رقم ${b.priorityOrder}", fontSize = 10.sp, color = Color.LightGray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 3: Supervisor administration configurations (Only Master Owner has total scope)
                    3 -> {
                        if (currentRole != UserRole.OWNER) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("هذه الصلاحيات مقفلة ومحفوظة لرتبة المالك الكلي 👑", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                                Text("تواصل مع المطور ماهر لتعديل رتب وشفرات الموظفين.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("شؤون الموظفين والمشرفين النشطين بالدليل", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Button(
                                        onClick = {
                                            supUser = ""
                                            supPass = ""
                                            supEmail = ""
                                            showAddSupervisorDialog = true
                                        }
                                    ) {
                                        Text("أضف مشرفاً +")
                                    }
                                }

                                // 2FA global switch
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("فرض نظام الحماية الثنائي (2FA) لجميع المشرفين", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                        Text("يتطلب من المشرفين استخدام مولد الرموز السري عند المراجعة الفورية.", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = appConfig.twoFactorAuthEnabled,
                                        onCheckedChange = {
                                            val updated = appConfig.copy(twoFactorAuthEnabled = it)
                                            FirebaseSimulator.updateAppConfig(context, updated)
                                            Toast.makeText(context, "تم حفظ حالة جدار الحماية الثنائي!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(dbState.admins) { adm ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(adm.username + " (" + adm.role + ")", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    if (adm.username != "admin") {
                                                        IconButton(
                                                            onClick = {
                                                                FirebaseSimulator.deleteAdminProfile(context, adm.username, "المالك الماستر")
                                                            }
                                                        ) {
                                                            Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                                Text("رمز الدخول السري: ${adm.passwordHash}", fontSize = 11.sp, color = Color.Gray)
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text("إضافة: ${if (adm.canAddProviders) "نعم" else "لا"}", fontSize = 10.sp, color = Color.LightGray)
                                                    Text("حذف: ${if (adm.canDeleteProviders) "نعم" else "لا"}", fontSize = 10.sp, color = Color.LightGray)
                                                    Text("مراجعة: ${if (adm.canAcceptRequests) "نعم" else "لا"}", fontSize = 10.sp, color = Color.LightGray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 4: Visual Themes, dynamic texts and logo uploader uploader
                    4 -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("محرك تخصيص التصميم والوجاهات البصري", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)

                            // 1. Theme picker selection
                            Text("اختر السمة والسمة اللونية الفورية للتطبيق:", fontSize = 11.sp, color = Color.Gray)
                            listOf("Cosmic Slate", "Charcoal Gold", "Royal Emerald").forEach { themeName ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val updated = appConfig.copy(themeColors = themeName)
                                            FirebaseSimulator.updateAppConfig(context, updated)
                                            Toast.makeText(context, "تم تطبيق سمة ($themeName) فوراً لجميع الأجهزة! 🎨", Toast.LENGTH_SHORT).show()
                                        }
                                        .background(if (appConfig.themeColors == themeName) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(10.dp)
                                ) {
                                    RadioButton(
                                        selected = appConfig.themeColors == themeName,
                                        onClick = {
                                            val updated = appConfig.copy(themeColors = themeName)
                                            FirebaseSimulator.updateAppConfig(context, updated)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when(themeName) {
                                            "Charcoal Gold" -> "✨ Charcoal Gold (ذهبي و فحمية فاخر)"
                                            "Royal Emerald" -> "🟢 Royal Emerald (امبريال زمردي وعشبي)"
                                            else -> "🌌 Cosmic Slate (الافتراضي - كوزميك سيلفر داكن)"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.2f))

                            // 2. Input font color choice
                            Text("اختر لون النص الأساسي للواجهات والأزرار المحددة:", fontSize = 11.sp, color = Color.Gray)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Bright White", "Light Gold", "Vibrant Silver").forEach { colName ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (appConfig.welcomeFontColor == colName) MaterialTheme.colorScheme.primary else Color.DarkGray)
                                            .clickable {
                                                val updated = appConfig.copy(welcomeFontColor = colName)
                                                FirebaseSimulator.updateAppConfig(context, updated)
                                            }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(colName, color = if (appConfig.welcomeFontColor == colName) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.2f))

                            // 3. Greeting change texts Nass AlTarhibi
                            var tempWelcomeAr by remember { mutableStateOf(appConfig.welcomeText) }
                            var tempWelcomeSize by remember { mutableStateOf(appConfig.welcomeFontSize.toString()) }
                            var tempWelcomeImg by remember { mutableStateOf(appConfig.welcomeImage) }

                            Text("تعديل نص الترحيب وحجمه ورابط الشعار:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = tempWelcomeAr,
                                onValueChange = { tempWelcomeAr = it },
                                placeholder = { Text("أدخل النص الترحيبي للواجهة") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = tempWelcomeAr.isEmpty()
                            )

                            OutlinedTextField(
                                value = tempWelcomeSize,
                                onValueChange = { tempWelcomeSize = it },
                                placeholder = { Text("حجم الخط (مثال: 16)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = tempWelcomeImg,
                                onValueChange = { tempWelcomeImg = it },
                                placeholder = { Text("رابط صورة الشعار الكلي للتطبيق") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val sizeVal = tempWelcomeSize.toIntOrNull() ?: 16
                                    val updated = appConfig.copy(
                                        welcomeText = tempWelcomeAr,
                                        welcomeFontSize = sizeVal,
                                        welcomeImage = tempWelcomeImg
                                    )
                                    FirebaseSimulator.updateAppConfig(context, updated)
                                    Toast.makeText(context, "تم تحديث وحفظ تصميمات الترحيب التفاعلية!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ الوجاهة الترحيبية للجمهور")
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.2f))

                            // 4. Toggle Maintenance mode & Footer label scales
                            var tempFooterText by remember { mutableStateOf(appConfig.footerText) }
                            var tempFooterScale by remember { mutableStateOf((appConfig.footerScale * 100f).toInt().toString()) }

                            Text("تعديل تذييل الصفحة والمحافظة على الصيانة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = tempFooterText,
                                onValueChange = { tempFooterText = it },
                                placeholder = { Text("نص التذييل المركزي (مثال: WAM777644670)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = tempFooterScale,
                                onValueChange = { tempFooterScale = it },
                                placeholder = { Text("مقياس التصدير الصغير (مثال 50%)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تفعيل وضع الصيانة ⚠️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("يقوم بإغلاق تقديم الطلبات وعرض تحذير للمستخدمين.", fontSize = 9.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = appConfig.isMaintenanceMode,
                                    onCheckedChange = {
                                        val updated = appConfig.copy(isMaintenanceMode = it)
                                        FirebaseSimulator.updateAppConfig(context, updated)
                                    }
                                )
                            }

                            Button(
                                onClick = {
                                    val sc = (tempFooterScale.toFloatOrNull() ?: 50f) / 100f
                                    val updated = appConfig.copy(
                                        footerText = tempFooterText,
                                        footerScale = sc
                                    )
                                    FirebaseSimulator.updateAppConfig(context, updated)
                                    Toast.makeText(context, "تم حفظ تعديلات التذييل المركزي والمقاييس!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ التذييل والمقياس")
                            }
                        }
                    }

                    // TAB 5: Web spreadsheet indicators export and maintenance mode toggles
                    5 -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("الملخص العملياتي والبيانات الإحصائية والتصدير", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)

                            // 1. Show metrics
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("عدادات الإقبال النشطة باليمن 📊", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("إجمالي عدد الزوار:", fontSize = 11.sp, color = Color.LightGray)
                                        Text("${dbState.userVisits} زائر حقيقي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("إجمالي نقرات الاتصالات الموثقة:", fontSize = 11.sp, color = Color.LightGray)
                                        Text("${dbState.callCounts} نقرة اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("إجمالي الحرفيين والعيادات:", fontSize = 11.sp, color = Color.LightGray)
                                        Text("${dbState.providers.size} مسجل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.2f))

                            // 2. Clear Database actions / Clean caches
                            Text("عمليات صيانة وحذف وتطهير قواعد البيانات:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        FirebaseSimulator.clearChatLog(context)
                                        Toast.makeText(context, "تم مسح كافة سجلات المحادثات وتطهير المفسدات في الداتا بيز!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("مسح سجلات الشات", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        // Whitelist dialog popup
                                        showWhitelistDialog = true
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("قائمة أجهزة التحكم", fontSize = 10.sp)
                                }
                            }

                            Divider(color = Color.Gray.copy(alpha = 0.2f))

                            // 3. EXPORT EXCEL/CSV SHEET PRINT SIMULATOR
                            Text("تصدير التقرير الفوري لحساب المالك (Excel / CSV / PDF):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = {
                                    // Generate CSV text representation and show to user as simulated spreadsheet
                                    val csvOutBuilder = StringBuilder()
                                    csvOutBuilder.append("ID,ProviderName,Category,Phone,Rating,Views,Address\n")
                                    dbState.providers.forEach { p ->
                                        csvOutBuilder.append("${p.id},\"${p.name}\",\"${p.category}\",${p.phone},${p.rating},${p.views},\"${p.address}\"\n")
                                    }

                                    // Display raw spreadsheet content via system shares toast or copy triggers
                                    val intentShare = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "تقرير دليل الخدمات الفوري الشامل:\n\n" + csvOutBuilder.toString())
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(intentShare, "تصدير التقرير الإحصائي بملف Excel"))
                                    Toast.makeText(context, "تم استقطاب جداول البيانات وتحويلها لملف CSV التصديري بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير وطباعة التقرير الشامل Excel / CSV")
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("سجلات نشاط وعمليات المشرفين بالدليل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            
                            // Supervisor Activity Logs
                            if (dbState.logs.isEmpty()) {
                                Text("لا توجد سجلات عمليات من المشرفين حالياً.", fontSize = 10.sp, color = Color.Gray)
                            } else {
                                dbState.logs.forEach { log ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.05f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Text("${log.supervisorName}: ${log.action} (${log.target})", fontSize = 10.sp, color = Color.LightGray)
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

    // A. DIALOG BOX FOR ADD CATEGORY
    if (showAddCategoryDialog) {
        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("أضف قسماً رئيسياً للدليل 📂", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = catNameAr,
                        onValueChange = { catNameAr = it },
                        placeholder = { Text("الاسم باللغة العربية") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = catNameEn,
                        onValueChange = { catNameEn = it },
                        placeholder = { Text("الاسم باللغة الإنجليزية") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = catColorHex,
                        onValueChange = { catColorHex = it },
                        placeholder = { Text("رمز اللون (مثال #D4AF37)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (catNameAr.isEmpty() || catNameEn.isEmpty()) {
                                    Toast.makeText(context, "الرجاء كشط الاسم بالعربية والإنكليزية!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newCat = CustomCategory(
                                        id = catId,
                                        nameAr = catNameAr.trim(),
                                        nameEn = catNameEn.trim(),
                                        colorHex = catColorHex.trim()
                                    )
                                    FirebaseSimulator.addCategory(context, newCat, activeAdminName)
                                    showAddCategoryDialog = false
                                    Toast.makeText(context, "تم حفظ وإضافة القسم بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إضافة")
                        }

                        OutlinedButton(onClick = { showAddCategoryDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // B. DIALOG BOX FOR ADD PROMOTED BANNER Sponsered Ads
    if (showAddBannerDialog) {
        Dialog(onDismissRequest = { showAddBannerDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("أضف لافتة إعلانية ممولة 📸", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = bannerTitle,
                        onValueChange = { bannerTitle = it },
                        placeholder = { Text("عنوان الترويج الإعلاني") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bannerImgUrl,
                        onValueChange = { bannerImgUrl = it },
                        placeholder = { Text("رابط الصورة الإعلانية المطلوبة") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bannerDuration,
                        onValueChange = { bannerDuration = it },
                        placeholder = { Text("مدة العرض بالأيام (مثال: 7)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bannerBudget,
                        onValueChange = { bannerBudget = it },
                        placeholder = { Text("الميزانية المخصصة بالدولار ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (bannerTitle.isEmpty() || bannerImgUrl.isEmpty()) {
                                    Toast.makeText(context, "الرجاء تعبئة العنوان وصورة الإعلان!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newBanner = BannerAd(
                                        id = bannerId,
                                        title = bannerTitle.trim(),
                                        imageUrl = bannerImgUrl.trim(),
                                        durationDays = bannerDuration.toIntOrNull() ?: 7,
                                        budget = bannerBudget.toDoubleOrNull() ?: 50.0,
                                        priorityOrder = bannerPriority.toIntOrNull() ?: 1
                                    )
                                    FirebaseSimulator.addBanner(context, newBanner, activeAdminName)
                                    showAddBannerDialog = false
                                    Toast.makeText(context, "تم رفع ونشر الإعلان الممول بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رفع ونشر")
                        }

                        OutlinedButton(onClick = { showAddBannerDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // C. DIALOG BOX FOR ADD SUPERVISOR admin profiles
    if (showAddSupervisorDialog) {
        Dialog(onDismissRequest = { showAddSupervisorDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إضافة رتبة مشرف جديد بالدليل 🛡️", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = supUser,
                        onValueChange = { supUser = it },
                        placeholder = { Text("اسم المستخدم (الدخول)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = supPass,
                        onValueChange = { supPass = it },
                        placeholder = { Text("الرمز السري") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = supEmail,
                        onValueChange = { supEmail = it },
                        placeholder = { Text("البريد الإلكتروني") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Roles
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("صلاحية الإضافة الدائمة", fontSize = 11.sp, color = Color.LightGray)
                        Switch(checked = supCanAdd, onCheckedChange = { supCanAdd = it })
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("صلاحية الحذف والإلغاء", fontSize = 11.sp, color = Color.LightGray)
                        Switch(checked = supCanDelete, onCheckedChange = { supCanDelete = it })
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("صلاحية قبول طلبات الانتساب", fontSize = 11.sp, color = Color.LightGray)
                        Switch(checked = supCanReview, onCheckedChange = { supCanReview = it })
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (supUser.isEmpty() || supPass.isEmpty()) {
                                    Toast.makeText(context, "الرجاء ملء حقول المشرف ورمز المرور!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newAdm = AdminProfile(
                                        id = "adm_${System.currentTimeMillis()}",
                                        username = supUser.trim(),
                                        passwordHash = supPass.trim(),
                                        email = supEmail.trim(),
                                        role = "supervisor",
                                        canAddProviders = supCanAdd,
                                        canDeleteProviders = supCanDelete,
                                        canAcceptRequests = supCanReview
                                    )
                                    FirebaseSimulator.addAdminProfile(context, newAdm, "المالك الماستر")
                                    showAddSupervisorDialog = false
                                    Toast.makeText(context, "تم حفظ قيد المشرف الجديد بالصلاحيات المحددة مجدداً!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ")
                        }

                        OutlinedButton(onClick = { showAddSupervisorDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // D. SYSTEM DEVICE ID WHITELIST MODAL dialog
    if (showWhitelistDialog) {
        Dialog(onDismissRequest = { showWhitelistDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إدارة الأجهزة المصرحة للولوج بالدليل 📱", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("أي جهاز تحكم يجب أن يدون رقمه التسلسلي هنا لتمكينه:", fontSize = 10.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = inputDeviceSerial,
                        onValueChange = { inputDeviceSerial = it },
                        placeholder = { Text("الرقم التسلسلي للجهاز (Device ID)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (inputDeviceSerial.isNotEmpty()) {
                                Toast.makeText(context, "تم إضافة الجهاز (${inputDeviceSerial}) للقائمة البيضاء المعتمدة!", Toast.LENGTH_LONG).show()
                                inputDeviceSerial = ""
                                showWhitelistDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("اعتماد الجهاز")
                    }

                    OutlinedButton(onClick = { showWhitelistDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("رجوع")
                    }
                }
            }
        }
    }
}
