package com.dalily.services.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.dalily.services.UserRole
import com.dalily.services.data.AdminProfile
import com.dalily.services.data.FirebaseSimulator
import com.dalily.services.data.ServiceProvider

@Composable
fun AdminScreen(
    navController: NavController,
    currentRole: UserRole,
    activeAdminName: String?,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val dbState by FirebaseSimulator.dbState.collectAsState()
    
    // Admin tabs enum
    var selectedTab by remember { mutableStateOf(0) } // 0: Requests, 1: Providers, 2: Admins, 3: Reports

    // Add Provider Form (No restrictions, customizable fully)
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var applyName by remember { mutableStateOf("") }
    var applyCategory by remember { mutableStateOf("") }
    var applyPhone by remember { mutableStateOf("") }
    var applyWhatsapp by remember { mutableStateOf("") }
    var applyDesc by remember { mutableStateOf("") }
    var applyImage by remember { mutableStateOf("") }
    var applyRating by remember { mutableStateOf("4.5") }
    var applyViews by remember { mutableStateOf("15") }
    var applyVerified by remember { mutableStateOf(true) }
    var applyAddress by remember { mutableStateOf("اليمن") }

    // Add Admin Form (Restricted to OWNER backdoor only)
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var adminUserField by remember { mutableStateOf("") }
    var adminPassField by remember { mutableStateOf("") }
    var adminEmailField by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text("لوحة تحكم الدليل السحابية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val roleString = if (currentRole == UserRole.OWNER) "المالك الماستر 👑" else "المشرف: $activeAdminName 🛡️"
                        Text(roleString, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
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
                        modifier = Modifier.testTag("admin_logout_btn")
                    ) {
                        Text("خروج للمستخدمين", fontSize = 11.sp, color = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            // Tab row headers
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
                    text = { Text("المزودين والكشاف", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("المشرفين والأدمنز", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("شكاوى وبلاغات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: Onboarding service Requests (Only active for ADMIN as requested)
                        if (currentRole == UserRole.OWNER) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("صلاحية خاصة بالمشرفين الفنيين!", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                                Text("حسب توجيهات المالك الماستر، المراجعات والقبول تدار من رتبة مشرف.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        } else {
                            val requests = dbState.serviceRequests.filter { it.status == "PENDING" }
                            if (requests.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("لا توجد طلبات إدراج قيد الانتظار حالياً 🗸", color = Color.Gray, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(requests) { req ->
                                        Card(modifier = Modifier.fillMaxWidth().testTag("req_item_${req.id}")) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("نشاط جديد مقترح: ${req.providerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("القسم المقترح: ${req.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                Text("رقم التواصل والاتصال: ${req.phone}", fontSize = 12.sp)
                                                if (req.whatsapp.isNotEmpty()) {
                                                    Text("واتساب: ${req.whatsapp}", fontSize = 12.sp)
                                                }
                                                if (req.description.isNotEmpty()) {
                                                    Text("تفاصيل النشاط: ${req.description}", fontSize = 11.sp, color = Color.Gray)
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            FirebaseSimulator.updateServiceRequestStatus(context, req.id, "APPROVED")
                                                            Toast.makeText(context, "تم قبول الطلب بامتياز وإضافته فوراً! 💓", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                                        modifier = Modifier.weight(1f).testTag("btn_accept_req")
                                                    ) {
                                                        Text("قبول وإدراج")
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            FirebaseSimulator.updateServiceRequestStatus(context, req.id, "REJECTED")
                                                            Toast.makeText(context, "تم رفض الطلب واستبعاده.", Toast.LENGTH_SHORT).show()
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
                    }

                    1 -> {
                        // TAB 1: Service providers directory view, creation with ZERO constraints (mandatory/optional bypassed)
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قائمة مقدمي الخدمة بالدليل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Button(
                                    onClick = {
                                        applyName = ""
                                        applyCategory = dbState.categories.firstOrNull()?.nameAr ?: "سباكة وصيانة"
                                        applyPhone = ""
                                        applyWhatsapp = ""
                                        applyDesc = ""
                                        applyImage = ""
                                        applyAddress = "اليمن"
                                        applyRating = "5.0"
                                        applyViews = "100"
                                        showAddProviderDialog = true
                                    },
                                    modifier = Modifier.testTag("admin_add_provider_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة بدون قيود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(dbState.providers) { p ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navController.navigate("detail/${p.id}") }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("القسم: ${p.category} | الهاتف: ${p.phone}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Icon(
                                                Icons.Default.ArrowForward,
                                                contentDescription = "تفاصيل وتعديل",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Administrators list and management (Owner backdrop only)
                        if (currentRole == UserRole.ADMIN) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("خاص بمالك الدليل الماستر فقط! 👑", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                                Text("المشرفين المعتمدين لا يمكنهم تعديل أو إضافة مشرفين آخرين.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        } else {
                            // OWNER ACCESS ONLY: Can add, search and sync administrators
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("إدارة المشرفين المعتمدين", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Button(
                                        onClick = {
                                            adminUserField = ""
                                            adminPassField = ""
                                            adminEmailField = ""
                                            showAddAdminDialog = true
                                        },
                                        modifier = Modifier.testTag("owner_add_admin_btn")
                                    ) {
                                        Text("إضافة مشرف جديد", fontSize = 11.sp)
                                    }
                                }

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(dbState.admins) { adm ->
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("اسم المستخدم: ${adm.username}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("كلمة المرور: ${adm.passwordHash}", fontSize = 11.sp, color = Color.Gray)
                                                    Text("الرتبة: ${adm.creatorRole}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                                
                                                if (adm.username != "admin") { // Base initial admin protected from deletion
                                                    IconButton(
                                                        onClick = {
                                                            FirebaseSimulator.deleteAdminProfile(context, adm.username)
                                                            Toast.makeText(context, "تم إلغاء صلاحية المشرف بنجاح!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف المشرف", tint = Color.Red)
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
                        // TAB 3: Reports registered by user
                        val reports = dbState.reports
                        if (reports.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد بلاغات مسجلة حالياً! التطبيق يسير ممتازاً.", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(reports) { rep ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("بلاغ ضد: ${rep.providerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                                            Text("السبب المذكور: ${rep.reason}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            if (rep.details.isNotEmpty()) {
                                                Text("التفاصيل: ${rep.details}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Text("تاريخ البلاغ: ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm").format(java.util.Date(rep.timestamp))}", fontSize = 10.sp, color = Color.Gray)
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

    // Add Provider (No constraints / optional mandatory bypass) Dialog
    if (showAddProviderDialog) {
        Dialog(onDismissRequest = { showAddProviderDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("إضافة فني / عيادة بدون قيود شروط 🛡️", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("صلاحية تامة للمشرف بإدراج أي بيانات مباشرة بدون إجبارية الحقول", fontSize = 10.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = applyName,
                        onValueChange = { applyName = it },
                        placeholder = { Text("الاسم الكامل فني/منشأة (يفضل)") },
                        label = { Text("الاسم") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_field_name"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    var showCatDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = applyCategory,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("القسم") },
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
                                        applyCategory = it.nameAr
                                        showCatDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = applyPhone,
                        onValueChange = { applyPhone = it },
                        placeholder = { Text("رقم هاتف فني/منشأة (يفضل)") },
                        label = { Text("رقم الهاتق") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_field_phone"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = applyWhatsapp,
                        onValueChange = { applyWhatsapp = it },
                        label = { Text("رقم الواتساب (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = applyDesc,
                        onValueChange = { applyDesc = it },
                        label = { Text("الخبرات والتفاصيل (اختياري)") },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = applyImage,
                        onValueChange = { applyImage = it },
                        label = { Text("رابط الصورة الفنية (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = applyAddress,
                        onValueChange = { applyAddress = it },
                        label = { Text("العنوان") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("توثيق النشاط بالعلامة الزرقاء:", fontSize = 11.sp)
                        Switch(checked = applyVerified, onCheckedChange = { applyVerified = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Relaxed constraints: Even if both name and phone are empty,
                                // we can assign fallback placeholders, bypassing mandatory requirements!
                                val finalName = applyName.ifEmpty { "مزود غير مسجّل الاسم" }
                                val finalPhone = applyPhone.ifEmpty { "000000000" }
                                
                                val provider = ServiceProvider(
                                    id = "p_${System.currentTimeMillis()}",
                                    name = finalName,
                                    category = applyCategory,
                                    phone = finalPhone,
                                    whatsapp = applyWhatsapp,
                                    description = applyDesc,
                                    imageUrl = applyImage,
                                    rating = applyRating.toDoubleOrNull() ?: 5.0,
                                    views = applyViews.toIntOrNull() ?: 20,
                                    isVerified = applyVerified,
                                    address = applyAddress
                                )
                                FirebaseSimulator.addServiceProvider(context, provider)
                                showAddProviderDialog = false
                                Toast.makeText(context, "تم إدراج ومزامنة الفني بالدليل بنجاح تام! 🗸", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("admin_field_submit")
                        ) {
                            Text("إدراج ومزامنة")
                        }
                        OutlinedButton(
                            onClick = { showAddProviderDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
    }

    // Owner Add Admin Dialog
    if (showAddAdminDialog) {
        Dialog(onDismissRequest = { showAddAdminDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("إضافة مشرف جديد للدليل 🛡️", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = adminUserField,
                        onValueChange = { adminUserField = it },
                        label = { Text("اسم المستخدم") },
                        modifier = Modifier.fillMaxWidth().testTag("add_admin_username"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = adminPassField,
                        onValueChange = { adminPassField = it },
                        label = { Text("رمز المرور السري للمشرف") },
                        modifier = Modifier.fillMaxWidth().testTag("add_admin_password"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = adminEmailField,
                        onValueChange = { adminEmailField = it },
                        label = { Text("البريد الإلكتروني") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (adminUserField.isEmpty() || adminPassField.isEmpty()) {
                                    Toast.makeText(context, "اسم المستخدم وكلمة المرور مطلوبان لحفظ المشرف السحابي!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newAdmin = AdminProfile(
                                        id = "adm_${System.currentTimeMillis()}",
                                        username = adminUserField,
                                        passwordHash = adminPassField,
                                        email = adminEmailField,
                                        creatorRole = "supervisor",
                                        isActive = true
                                    )
                                    FirebaseSimulator.addAdminProfile(context, newAdmin)
                                    showAddAdminDialog = false
                                    Toast.makeText(context, "تم إضافة المشرف ومزامنة صلاحياته بنجاح! 🗸", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("owner_add_admin_submit")
                        ) {
                            Text("إدراج")
                        }
                        OutlinedButton(
                            onClick = { showAddAdminDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }
}
