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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.ServiceProvider

// Cosmic Slate Theme Colors
val DarkCanvas = Color(0xFF0D1B2A) // Let's use clean high-contrast colors
val DeepSlate = Color(0xFF0F1A24)
val CardBackground = Color(0xFF162534)
val AccentPink = Color(0xFFE91E63)
val BorderCyan = Color(0xFF00BCD4)
val TextLight = Color(0xFFECEFF1)
val TextGray = Color(0xFF90A4AE)

fun planetaryDark() = 0xFF0D1B2A

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("home") } // "home", "add", "about"
    
    val lang = viewModel.currentLanguage
    val isAr = lang == "ar"
    
    val appTitleAr = viewModel.getSettingValue("app_title_ar", "دليل الخدمات اليمني")
    val appTitleEn = viewModel.getSettingValue("app_title_en", "Yemen Service Directory")
    val appTitle = if (isAr) appTitleAr else appTitleEn

    var showAdminDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = AccentPink.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = AccentPink,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Text(
                            text = appTitle,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Admin mode action indicator
                    IconButton(
                        onClick = {
                            if (viewModel.isAdminMode) {
                                viewModel.setAdminModeEnabled(false)
                                Toast.makeText(context, if (isAr) "تم الخروج من وضع الإدارة" else "Admin mode disabled", Toast.LENGTH_SHORT).show()
                            } else {
                                showAdminDialog = true
                            }
                        },
                        modifier = Modifier.testTag("admin_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (viewModel.isAdminMode) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                            contentDescription = "Admin Area",
                            tint = if (viewModel.isAdminMode) BorderCyan else TextGray
                        )
                    }
                    
                    // Language toggle button
                    TextButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("language_toggle_button")
                    ) {
                        Text(
                            text = if (isAr) "EN" else "عربي",
                            color = AccentPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepSlate,
                    titleContentColor = TextLight
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepSlate,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(if (isAr) "الرئيسية" else "Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPink,
                        selectedTextColor = AccentPink,
                        indicatorColor = AccentPink.copy(alpha = 0.15f),
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == "add",
                    onClick = { currentTab = "add" },
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                    label = { Text(if (isAr) "إضافة خدمة" else "Add") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPink,
                        selectedTextColor = AccentPink,
                        indicatorColor = AccentPink.copy(alpha = 0.15f),
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    ),
                    modifier = Modifier.testTag("nav_add")
                )
                NavigationBarItem(
                    selected = currentTab == "about",
                    onClick = { currentTab = "about" },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text(if (isAr) "عن الدليل" else "About") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentPink,
                        selectedTextColor = AccentPink,
                        indicatorColor = AccentPink.copy(alpha = 0.15f),
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray
                    ),
                    modifier = Modifier.testTag("nav_about")
                )
            }
        },
        containerColor = Color(planetaryDark())
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepSlate, Color(planetaryDark()))
                    )
                )
        ) {
            when (currentTab) {
                "home" -> HomeScreenContent(viewModel = viewModel)
                "add" -> AddProviderScreen(viewModel = viewModel, onNavigateToHome = { currentTab = "home" })
                "about" -> AboutAppScreen(viewModel = viewModel)
            }
            
            // Admin authentication prompt
            if (showAdminDialog) {
                Dialog(onDismissRequest = {
                    showAdminDialog = false
                    pinInput = ""
                }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, BorderCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (isAr) "تسجيل دخول الإدارة" else "Admin Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextLight
                            )
                            
                            Text(
                                text = if (isAr) "أدخل رمز المرور الخاص بالإدارة للدخول لوضع التعديل والاستقصاء (الافتراضي: 1234)" 
                                       else "Enter admin PIN to unlock customization & dashboard permissions (Default: 1234)",
                                fontSize = 12.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                            
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { pinInput = it },
                                label = { Text(if (isAr) "رمز الإدارة (PIN)" else "Admin PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPink,
                                    unfocusedBorderColor = TextGray,
                                    focusedLabelColor = AccentPink,
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_pin_input")
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showAdminDialog = false
                                        pinInput = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                                ) {
                                    Text(if (isAr) "إلغاء" else "Cancel")
                                }
                                
                                Button(
                                    onClick = {
                                        val success = viewModel.setAdminModeEnabled(true, pinInput)
                                        if (success) {
                                            Toast.makeText(context, if (isAr) "مرحباً بك يا مسؤول" else "Welcome Admin", Toast.LENGTH_SHORT).show()
                                            showAdminDialog = false
                                        } else {
                                            Toast.makeText(context, if (isAr) "رمز مرور خاطئ!" else "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                                        }
                                        pinInput = ""
                                    },
                                    modifier = Modifier.weight(1f).testTag("admin_login_submit"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPink)
                                ) {
                                    Text(if (isAr) "دخول" else "Login")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(viewModel: AppViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredProviders by viewModel.filteredProviders.collectAsStateWithLifecycle()
    
    val lang = viewModel.currentLanguage
    val isAr = lang == "ar"
    
    var editingProvider by remember { mutableStateOf<ServiceProvider?>(null) }
    
    // Admin category creation state
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatAr by remember { mutableStateOf("") }
    var newCatEn by remember { mutableStateOf("") }
    var newCatIcon by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { 
                Text(
                    text = if (isAr) "ابحث عن مشفى، فني كهرباء، معهد ..." else "Search hospital, electrician, school ...",
                    fontSize = 14.sp
                ) 
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
            trailingIcon = {
                if (viewModel.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = TextGray)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedBorderColor = AccentPink,
                unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                cursorColor = AccentPink
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("search_bar")
        )

        // Categories section with header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAr) "التصنيفات المتاحة" else "Available Categories",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextLight
            )
            
            if (viewModel.isAdminMode) {
                IconButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category",
                        tint = BorderCyan
                    )
                }
            }
        }
        
        // Horizontal Categories Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // "Show All" item
            item {
                FilterChip(
                    selected = viewModel.selectedCategoryId == -1,
                    onClick = { viewModel.selectedCategoryId = -1 },
                    label = { Text(if (isAr) "الكل 📁" else "All 📁") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentPink,
                        selectedLabelColor = Color.White,
                        containerColor = CardBackground,
                        labelColor = TextGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = viewModel.selectedCategoryId == -1,
                        borderColor = TextGray.copy(alpha = 0.3f),
                        selectedBorderColor = AccentPink
                    )
                )
            }
            
            items(categories) { category ->
                val catName = if (isAr) category.nameAr else category.nameEn
                
                FilterChip(
                    selected = viewModel.selectedCategoryId == category.id,
                    onClick = { viewModel.selectedCategoryId = category.id },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = getIconByName(category.iconName),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (viewModel.selectedCategoryId == category.id) Color.White else BorderCyan
                            )
                            Text(catName)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentPink,
                        selectedLabelColor = Color.White,
                        containerColor = CardBackground,
                        labelColor = TextLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = viewModel.selectedCategoryId == category.id,
                        borderColor = TextGray.copy(alpha = 0.3f),
                        selectedBorderColor = AccentPink
                    )
                )
            }
        }

        // Providers list or empty state
        if (filteredProviders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (isAr) "لا توجد خدمات مطابقة لبحثك" else "No matching services found",
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("providers_list")
            ) {
                items(filteredProviders) { provider ->
                    ProviderCard(
                        provider = provider,
                        viewModel = viewModel,
                        onEditClick = { editingProvider = provider },
                        onDeleteClick = {
                            viewModel.deleteServiceProvider(provider)
                            Toast.makeText(context, if (isAr) "تم الحذف بنجاح" else "Deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Edit Provider Modal Dialog
    if (editingProvider != null) {
        val provider = editingProvider!!
        var editNameAr by remember { mutableStateOf(provider.nameAr) }
        var editNameEn by remember { mutableStateOf(provider.nameEn) }
        var editPhone by remember { mutableStateOf(provider.phone) }
        var editAddressAr by remember { mutableStateOf(provider.addressAr) }
        var editAddressEn by remember { mutableStateOf(provider.addressEn) }
        var editDescAr by remember { mutableStateOf(provider.descriptionAr) }
        var editDescEn by remember { mutableStateOf(provider.descriptionEn) }
        var editHours by remember { mutableStateOf(provider.workingHours) }
        var editIsVerified by remember { mutableStateOf(provider.isVerified) }
        
        var editCustom1 by remember { mutableStateOf(provider.customField1Value) }
        var editCustom2 by remember { mutableStateOf(provider.customField2Value) }
        var editCustom3 by remember { mutableStateOf(provider.customField3Value) }

        val field1LabelAr = viewModel.getSettingValue("custom_field_1_label_ar", "حقل إضافي 1")
        val field1LabelEn = viewModel.getSettingValue("custom_field_1_label_en", "Extra Field 1")
        val field2LabelAr = viewModel.getSettingValue("custom_field_2_label_ar", "حقل إضافي 2")
        val field2LabelEn = viewModel.getSettingValue("custom_field_2_label_en", "Extra Field 2")
        val field3LabelAr = viewModel.getSettingValue("custom_field_3_label_ar", "حقل إضافي 3")
        val field3LabelEn = viewModel.getSettingValue("custom_field_3_label_en", "Extra Field 3")

        val f1Label = if (isAr) field1LabelAr else field1LabelEn
        val f2Label = if (isAr) field2LabelAr else field2LabelEn
        val f3Label = if (isAr) field3LabelAr else field3LabelEn

        Dialog(onDismissRequest = { editingProvider = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .border(1.dp, BorderCyan, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAr) "تعديل تفاصيل الخدمة" else "Edit Provider Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BorderCyan,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = editNameAr,
                        onValueChange = { editNameAr = it },
                        label = { Text(if (isAr) "اسم الخدمة بالأولى (عربي)" else "Service Name (Arabic)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editNameEn,
                        onValueChange = { editNameEn = it },
                        label = { Text(if (isAr) "الاسم بالإنجليزية (مستحسن)" else "Service Name (English)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(if (isAr) "رقم الهاتف / الاتصال" else "Phone/Contact Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAddressAr,
                        onValueChange = { editAddressAr = it },
                        label = { Text(if (isAr) "العنوان (عربي)" else "Address (Arabic)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAddressEn,
                        onValueChange = { editAddressEn = it },
                        label = { Text(if (isAr) "العنوان (بالإنجليزي)" else "Address (English)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editHours,
                        onValueChange = { editHours = it },
                        label = { Text(if (isAr) "ساعات الدوام والعمل" else "Working/Service Hours") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDescAr,
                        onValueChange = { editDescAr = it },
                        label = { Text(if (isAr) "الوصف والتفاصيل (عربي)" else "Description (Arabic)") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDescEn,
                        onValueChange = { editDescEn = it },
                        label = { Text(if (isAr) "الوصف (بالإنجليزي)" else "Description (English)") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dynamic Admin Settings fields
                    Divider(color = TextGray.copy(alpha = 0.3f))
                    Text(
                        text = if (isAr) "الخانات الإضافية القابلة للتعديل" else "Customizable Additional Fields",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentPink
                    )

                    OutlinedTextField(
                        value = editCustom1,
                        onValueChange = { editCustom1 = it },
                        label = { Text(f1Label) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = AccentPink),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCustom2,
                        onValueChange = { editCustom2 = it },
                        label = { Text(f2Label) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = AccentPink),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCustom3,
                        onValueChange = { editCustom3 = it },
                        label = { Text(f3Label) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = AccentPink),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = editIsVerified,
                            onCheckedChange = { editIsVerified = it },
                            colors = CheckboxDefaults.colors(checkedColor = BorderCyan)
                        )
                        Text(
                            text = if (isAr) "مقدم خدمة معتمد وموثوق ✔" else "Verified & Trusted Service ✔",
                            color = TextLight,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingProvider = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                        ) {
                            Text(if (isAr) "إلغاء المراجعة" else "Cancel")
                        }

                        Button(
                            onClick = {
                                if (editNameAr.isBlank() || editPhone.isBlank()) {
                                    Toast.makeText(context, if (isAr) "الرجاء تعبئة على الأقل الاسم والهاتف" else "Please fill Name and Phone", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.updateServiceProvider(
                                    provider.copy(
                                        nameAr = editNameAr,
                                        nameEn = editNameEn,
                                        phone = editPhone,
                                        addressAr = editAddressAr,
                                        addressEn = editAddressEn,
                                        workingHours = editHours,
                                        descriptionAr = editDescAr,
                                        descriptionEn = editDescEn,
                                        isVerified = editIsVerified,
                                        customField1Value = editCustom1,
                                        customField2Value = editCustom2,
                                        customField3Value = editCustom3
                                    )
                                )
                                Toast.makeText(context, if (isAr) "تم الحفظ والتحديث بنجاح" else "Updated successfully", Toast.LENGTH_SHORT).show()
                                editingProvider = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BorderCyan)
                        ) {
                            Text(if (isAr) "حفظ التعديلات" else "Save Changes")
                        }
                    }
                }
            }
        }
    }

    // Add Category Dialog for Admin
    if (showAddCategoryDialog) {
        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, BorderCyan, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAr) "إضافة تصنيف جديد" else "Add New Category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BorderCyan
                    )

                    OutlinedTextField(
                        value = newCatAr,
                        onValueChange = { newCatAr = it },
                        label = { Text("الاسم بالعربية (أبي)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                    )

                    OutlinedTextField(
                        value = newCatEn,
                        onValueChange = { newCatEn = it },
                        label = { Text("Name in English") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                    )

                    OutlinedTextField(
                        value = newCatIcon,
                        onValueChange = { newCatIcon = it },
                        label = { Text("اسم الأيقونة (مثال: build, school, medical)") },
                        placeholder = { Text("build") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showAddCategoryDialog = false
                                newCatAr = ""
                                newCatEn = ""
                                newCatIcon = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "إلغاء" else "Cancel")
                        }

                        Button(
                            onClick = {
                                if (newCatAr.isBlank() || newCatEn.isBlank()) {
                                    Toast.makeText(context, if (isAr) "الاسم مطلوب" else "Name is required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val icon = if (newCatIcon.isBlank()) "star" else newCatIcon.trim()
                                viewModel.addCategory(newCatAr, newCatEn, icon)
                                Toast.makeText(context, if (isAr) "تمت إضافة التصنيف" else "Category added", Toast.LENGTH_SHORT).show()
                                showAddCategoryDialog = false
                                newCatAr = ""
                                newCatEn = ""
                                newCatIcon = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BorderCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "إضافة" else "Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderCard(
    provider: ServiceProvider,
    viewModel: AppViewModel,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val lang = viewModel.currentLanguage
    val isAr = lang == "ar"
    
    val name = if (isAr) provider.nameAr else provider.nameEn
    val address = if (isAr) provider.addressAr else provider.addressEn
    val desc = if (isAr) provider.descriptionAr else provider.descriptionEn
    
    val field1LabelAr = viewModel.getSettingValue("custom_field_1_label_ar", "حقل 1")
    val field1LabelEn = viewModel.getSettingValue("custom_field_1_label_en", "Field 1")
    val field2LabelAr = viewModel.getSettingValue("custom_field_2_label_ar", "حقل 2")
    val field2LabelEn = viewModel.getSettingValue("custom_field_2_label_en", "Field 2")
    val field3LabelAr = viewModel.getSettingValue("custom_field_3_label_ar", "حقل 3")
    val field3LabelEn = viewModel.getSettingValue("custom_field_3_label_en", "Field 3")

    val f1L = if (isAr) field1LabelAr else field1LabelEn
    val f2L = if (isAr) field2LabelAr else field2LabelEn
    val f3L = if (isAr) field3LabelAr else field3LabelEn

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (provider.isVerified) BorderCyan.copy(alpha = 0.4f) else TextGray.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("provider_card_${provider.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            modifier = Modifier.testTag("provider_name_${provider.id}")
                        )
                        if (provider.isVerified) {
                            Surface(
                                color = BorderCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, BorderCyan),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = if (isAr) "موثق ✔" else "Verified ✔",
                                    color = BorderCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                // Admin Actions Block
                if (viewModel.isAdminMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Provider",
                                tint = BorderCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Provider",
                                tint = AccentPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Description
            if (desc.isNotBlank()) {
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = TextGray,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Divider(color = TextGray.copy(alpha = 0.1f))

            // Phone and Address Grid Icons
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Phone row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                                context.startActivity(callIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "ERROR: Phone call launcher failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                    Text(
                        text = provider.phone,
                        fontSize = 13.sp,
                        color = AccentPink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Dial Phone",
                        tint = AccentPink,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Address row
                if (address.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BorderCyan, modifier = Modifier.size(16.dp))
                        Text(
                            text = address,
                            fontSize = 13.sp,
                            color = TextLight
                        )
                    }
                }

                // Working Hours row
                if (provider.workingHours.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
                        Text(
                            text = provider.workingHours,
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }
            }

            // Optional Dynamic Extra Fields Display
            val showCustom = provider.customField1Value.isNotBlank() || 
                             provider.customField2Value.isNotBlank() || 
                             provider.customField3Value.isNotBlank()
            
            if (showCustom) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepSlate.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (provider.customField1Value.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "$f1L: ", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                            Text(text = provider.customField1Value, fontSize = 11.sp, color = TextLight)
                        }
                    }
                    if (provider.customField2Value.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "$f2L: ", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                            Text(text = provider.customField2Value, fontSize = 11.sp, color = TextLight)
                        }
                    }
                    if (provider.customField3Value.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "$f3L: ", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                            Text(text = provider.customField3Value, fontSize = 11.sp, color = TextLight)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderScreen(
    viewModel: AppViewModel,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    
    val lang = viewModel.currentLanguage
    val isAr = lang == "ar"

    // Inputs States
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var nameAr by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var addressAr by remember { mutableStateOf("") }
    var addressEn by remember { mutableStateOf("") }
    var descriptionAr by remember { mutableStateOf("") }
    var descriptionEn by remember { mutableStateOf("") }
    var workingHours by remember { mutableStateOf("") }
    
    // Custom labels/placeholders
    val field1LabelAr = viewModel.getSettingValue("custom_field_1_label_ar", "سنة التأسيس")
    val field1LabelEn = viewModel.getSettingValue("custom_field_1_label_en", "Est. Year")
    val field2LabelAr = viewModel.getSettingValue("custom_field_2_label_ar", "طريقة الدفع")
    val field2LabelEn = viewModel.getSettingValue("custom_field_2_label_en", "Payment")
    val field3LabelAr = viewModel.getSettingValue("custom_field_3_label_ar", "ملاحظات")
    val field3LabelEn = viewModel.getSettingValue("custom_field_3_label_en", "Notes")

    val f1L = if (isAr) field1LabelAr else field1LabelEn
    val f2L = if (isAr) field2LabelAr else field2LabelEn
    val f3L = if (isAr) field3LabelAr else field3LabelEn

    var customF1Val by remember { mutableStateOf("") }
    var customF2Val by remember { mutableStateOf("") }
    var customF3Val by remember { mutableStateOf("") }

    var dropdownExpanded by remember { mutableStateOf(false) }

    // UNIVERSAL SCROLLABLE LAYER - Fixed clipping, fits perfectly on any display size
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isAr) "طلب إضافة مقدم خدمة جديد" else "Submit New Service Request",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPink,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                
                Text(
                    text = if (isAr) 
                        "سيتم إدراج الخدمة فور تعبئتها. إذا كنت تسجل كمسؤول فستظهر الخدمة مباشرة معتمدة." 
                        else "Fill out details to submit. If logged as Admin, it is instantly approved.",
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
        }

        // 1. Category Selection
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (isAr) "اختر تصنيف الخدمة *" else "Select Service Category *",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { dropdownExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_dropdown_trigger"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                    border = BorderStroke(1.dp, TextGray.copy(alpha = 0.5f))
                ) {
                    val label = if (selectedCategory != null) {
                        if (isAr) selectedCategory!!.nameAr else selectedCategory!!.nameEn
                    } else {
                        if (isAr) "اضغط لاختيار التصنيف" else "Press to pick category"
                    }
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = BorderCyan
                    )
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(CardBackground)
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = if (isAr) category.nameAr else category.nameEn,
                                    color = TextLight
                                ) 
                            },
                            onClick = {
                                selectedCategory = category
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 2. Main Visual Fields
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Service Name Ar
            OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = { Text(if (isAr) "الاسم باللغة العربية *" else "Service Name (Arabic) *") },
                singleLine = true,
                isError = nameAr.isBlank() && phone.isNotBlank(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = BorderCyan,
                    focusedLabelColor = BorderCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_name_ar")
            )

            // Service Name En
            OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text(if (isAr) "الاسم باللغة الإنجليزية" else "Service Name (English)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = BorderCyan,
                    focusedLabelColor = BorderCyan
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Telephone Number
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(if (isAr) "رقم الهاتف / الاتصال المباشر *" else "Direct Contact Phone *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = BorderCyan,
                    focusedLabelColor = BorderCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_phone")
            )

            // Location/Address Ar
            OutlinedTextField(
                value = addressAr,
                onValueChange = { addressAr = it },
                label = { Text(if (isAr) "العنوان التفصيلي (عربي)" else "Address Details (Arabic)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = BorderCyan,
                    focusedLabelColor = BorderCyan
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Location/Address En
            OutlinedTextField(
                value = addressEn,
                onValueChange = { addressEn = it },
                label = { Text(if (isAr) "العنوان التفصيلي (إنجليزي)" else "Address Details (English)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = BorderCyan,
                    focusedLabelColor = BorderCyan
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Working times
            OutlinedTextField(
                value = workingHours,
                onValueChange = { workingHours = it },
                label = { Text(if (isAr) "ساعات العمل الرسمية (مثل: 8ص - 2ظ)" else "Official Hours (e.g., 24/7, 8am-2pm)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = BorderCyan,
                    focusedLabelColor = BorderCyan
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Description Arabic
            OutlinedTextField(
                value = descriptionAr,
                onValueChange = { descriptionAr = it },
                label = { Text(if (isAr) "تفاصيل وشرح عن الخدمة (عربي)" else "Service Description (Arabic)") },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = BorderCyan,
                    focusedLabelColor = BorderCyan
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Description English
            OutlinedTextField(
                value = descriptionEn,
                onValueChange = { descriptionEn = it },
                label = { Text(if (isAr) "شرح عن الخدمة (بالإنجليزي)" else "Service Description (English)") },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = BorderCyan,
                    focusedLabelColor = BorderCyan
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. Dynamic Custom Admin Properties
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isAr) "الخانات الإضافية القابلة للتخصيص" else "Customizable Dynamic Inputs",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPink
            )
            
            Text(
                text = if (isAr) 
                    "يمكن للمسؤول تغيير مسميات هذه الحقول الـ 3 من صفحة الإعدادات لتناسب مختلف الأنشطة." 
                    else "The designation of these 3 fields is globally configurable from Settings.",
                fontSize = 10.sp,
                color = TextGray
            )

            OutlinedTextField(
                value = customF1Val,
                onValueChange = { customF1Val = it },
                label = { Text(f1L) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = AccentPink,
                    focusedLabelColor = AccentPink
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = customF2Val,
                onValueChange = { customF2Val = it },
                label = { Text(f2L) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = AccentPink,
                    focusedLabelColor = AccentPink
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = customF3Val,
                onValueChange = { customF3Val = it },
                label = { Text(f3L) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = AccentPink,
                    focusedLabelColor = AccentPink
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Submit Action Button
        Button(
            onClick = {
                if (selectedCategory == null) {
                    Toast.makeText(context, if (isAr) "يجب اختيار تصنيف الخدمة أولاً!" else "Please select category first!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (nameAr.isBlank()) {
                    Toast.makeText(context, if (isAr) "اسم الخدمة بالعربية مطلوب!" else "Service name in Arabic is required!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (phone.isBlank()) {
                    Toast.makeText(context, if (isAr) "رقم جهة الاتصال مطلوب!" else "Contact phone is required!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                viewModel.addServiceProvider(
                    ServiceProvider(
                        categoryId = selectedCategory!!.id,
                        nameAr = nameAr.trim(),
                        nameEn = nameEn.trim().ifBlank { nameAr.trim() },
                        phone = phone.trim(),
                        addressAr = addressAr.trim(),
                        addressEn = addressEn.trim().ifBlank { addressAr.trim() },
                        descriptionAr = descriptionAr.trim(),
                        descriptionEn = descriptionEn.trim(),
                        workingHours = workingHours.trim(),
                        isVerified = viewModel.isAdminMode, // Auto verify if admin is adding
                        customField1Value = customF1Val.trim(),
                        customField2Value = customF2Val.trim(),
                        customField3Value = customF3Val.trim()
                    )
                )

                Toast.makeText(context, if (isAr) "تم إدراج الخدمة بنجاح!" else "Added successfully!", Toast.LENGTH_LONG).show()
                
                // Clear state
                nameAr = ""
                nameEn = ""
                phone = ""
                addressAr = ""
                addressEn = ""
                descriptionAr = ""
                descriptionEn = ""
                workingHours = ""
                customF1Val = ""
                customF2Val = ""
                customF3Val = ""
                selectedCategory = null

                onNavigateToHome()
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_provider_btn")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAr) "إضافة وحفظ في الدليل المحلي" else "Save Locally in Yemen Directory",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AboutAppScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    val lang = viewModel.currentLanguage
    val isAr = lang == "ar"

    val titleAr = settings["app_title_ar"] ?: "دليل الخدمات اليمني"
    val titleEn = settings["app_title_en"] ?: "Yemen Service Directory"
    val descAr = settings["app_desc_ar"] ?: "دليلك الشامل لجميع الأنشطة والخدمات الطبية والتعليمية والمهنية."
    val descEn = settings["app_desc_en"] ?: "Your absolute guide for medical, educational, and professional directories."
    val email = settings["contact_email"] ?: "support@yemenservices.app"
    val phone = settings["contact_phone"] ?: "+96777777777"
    val rulesAr = settings["app_rules_ar"] ?: "1. تصفح وتواصل مباشرة."
    val rulesEn = settings["app_rules_en"] ?: "1. Direct contact with providers."

    // Customize mode inside About screen if Admin is active
    var editAboutMode by remember { mutableStateOf(false) }

    // Admin Inputs for Settings customization
    var editTitleAr by remember { mutableStateOf(titleAr) }
    var editTitleEn by remember { mutableStateOf(titleEn) }
    var editDescAr by remember { mutableStateOf(descAr) }
    var editDescEn by remember { mutableStateOf(descEn) }
    var editContactEmail by remember { mutableStateOf(email) }
    var editContactPhone by remember { mutableStateOf(phone) }
    var editRulesAr by remember { mutableStateOf(rulesAr) }
    var editRulesEn by remember { mutableStateOf(rulesEn) }

    var customF1LabelAr by remember { mutableStateOf(settings["custom_field_1_label_ar"] ?: "") }
    var customF1LabelEn by remember { mutableStateOf(settings["custom_field_1_label_en"] ?: "") }
    var customF2LabelAr by remember { mutableStateOf(settings["custom_field_2_label_ar"] ?: "") }
    var customF2LabelEn by remember { mutableStateOf(settings["custom_field_2_label_en"] ?: "") }
    var customF3LabelAr by remember { mutableStateOf(settings["custom_field_3_label_ar"] ?: "") }
    var customF3LabelEn by remember { mutableStateOf(settings["custom_field_3_label_en"] ?: "") }

    // Sync state when entering or settings update
    LaunchedEffect(settings, editAboutMode) {
        if (!editAboutMode) {
            editTitleAr = titleAr
            editTitleEn = titleEn
            editDescAr = descAr
            editDescEn = descEn
            editContactEmail = email
            editContactPhone = phone
            editRulesAr = rulesAr
            editRulesEn = rulesEn
            customF1LabelAr = settings["custom_field_1_label_ar"] ?: "سنة التأسيس"
            customF1LabelEn = settings["custom_field_1_label_en"] ?: "Est. Year"
            customF2LabelAr = settings["custom_field_2_label_ar"] ?: "طريقة الدفع"
            customF2LabelEn = settings["custom_field_2_label_en"] ?: "Payment"
            customF3LabelAr = settings["custom_field_3_label_ar"] ?: "ملاحظات"
            customF3LabelEn = settings["custom_field_3_label_en"] ?: "Notes"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Identity Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Vector App Logo Placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AccentPink.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, AccentPink, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TravelExplore,
                        contentDescription = null,
                        tint = AccentPink,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (isAr) titleAr else titleEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextLight,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isAr) descAr else descEn,
                    fontSize = 13.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (viewModel.isAdminMode) {
                    Divider(color = TextGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { editAboutMode = !editAboutMode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editAboutMode) BorderCyan else AccentPink
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (editAboutMode) Icons.Default.Visibility else Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (editAboutMode) {
                                    if (isAr) "عرض الصفحة النهائية" else "View Live Page"
                                } else {
                                    if (isAr) "تعديل محتويات وتسميات الصفحة ⚙" else "Edit Page Content & Labels ⚙"
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // View Mode vs Dynamic Edit Mode
        if (editAboutMode && viewModel.isAdminMode) {
            // ADMIN SETTINGS DYNAMIC CUSTOMIZER FORM
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderCyan, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAr) "لوحة تعديل وتعديل كل شيء بالتطبيق" else "Full App Customization Controller",
                        fontWeight = FontWeight.Bold,
                        color = BorderCyan,
                        fontSize = 15.sp
                    )

                    // Titles Editor
                    OutlinedTextField(
                        value = editTitleAr,
                        onValueChange = { editTitleAr = it },
                        label = { Text("عنوان التطبيق بالعربية") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTitleEn,
                        onValueChange = { editTitleEn = it },
                        label = { Text("App Title (English)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Description Editor
                    OutlinedTextField(
                        value = editDescAr,
                        onValueChange = { editDescAr = it },
                        label = { Text("الوصف بالعربية") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDescEn,
                        onValueChange = { editDescEn = it },
                        label = { Text("Description (English)") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight, focusedLabelColor = BorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dynamic Universal Inputs/Labels Customizer (واعطاء صلاحيه تغيير الخانات)
                    Divider(color = TextGray.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.DynamicForm, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isAr) "تخصيص مسميات خانات مقدم الخدمة" else "Customize Provider Labels",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPink
                        )
                    }

                    // Field 1 Customization
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "الخانة الأولى (Field 1 Name):", fontSize = 11.sp, color = TextGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customF1LabelAr,
                                onValueChange = { customF1LabelAr = it },
                                label = { Text("عربي") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                            )
                            OutlinedTextField(
                                value = customF1LabelEn,
                                onValueChange = { customF1LabelEn = it },
                                label = { Text("English") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                            )
                        }
                    }

                    // Field 2 Customization
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "الخانة الثانية (Field 2 Name):", fontSize = 11.sp, color = TextGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customF2LabelAr,
                                onValueChange = { customF2LabelAr = it },
                                label = { Text("عربي") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                            )
                            OutlinedTextField(
                                value = customF2LabelEn,
                                onValueChange = { customF2LabelEn = it },
                                label = { Text("English") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                            )
                        }
                    }

                    // Field 3 Customization
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "الخانة الثالثة (Field 3 Name):", fontSize = 11.sp, color = TextGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customF3LabelAr,
                                onValueChange = { customF3LabelAr = it },
                                label = { Text("عربي") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                            )
                            OutlinedTextField(
                                value = customF3LabelEn,
                                onValueChange = { customF3LabelEn = it },
                                label = { Text("English") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight)
                            )
                        }
                    }

                    Divider(color = TextGray.copy(alpha = 0.2f))

                    // Contacts Editor
                    OutlinedTextField(
                        value = editContactPhone,
                        onValueChange = { editContactPhone = it },
                        label = { Text("رقم هاتف الاتصال بالدليل") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editContactEmail,
                        onValueChange = { editContactEmail = it },
                        label = { Text("بريد الدعم والمراسلة الإلكتروني") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Rules Editor
                    OutlinedTextField(
                        value = editRulesAr,
                        onValueChange = { editRulesAr = it },
                        label = { Text("شروط وسياسات الدليل (عربي)") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editRulesEn,
                        onValueChange = { editRulesEn = it },
                        label = { Text("Regulations of Guide (English)") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextLight, unfocusedTextColor = TextLight),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Submission actions inside admin form
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editAboutMode = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "إلغاء التغييرات" else "Cancel")
                        }

                        Button(
                            onClick = {
                                viewModel.updateAppSetting("app_title_ar", editTitleAr.trim())
                                viewModel.updateAppSetting("app_title_en", editTitleEn.trim())
                                viewModel.updateAppSetting("app_desc_ar", editDescAr.trim())
                                viewModel.updateAppSetting("app_desc_en", editDescEn.trim())
                                viewModel.updateAppSetting("contact_email", editContactEmail.trim())
                                viewModel.updateAppSetting("contact_phone", editContactPhone.trim())
                                viewModel.updateAppSetting("app_rules_ar", editRulesAr.trim())
                                viewModel.updateAppSetting("app_rules_en", editRulesEn.trim())
                                
                                viewModel.updateAppSetting("custom_field_1_label_ar", customF1LabelAr.trim())
                                viewModel.updateAppSetting("custom_field_1_label_en", customF1LabelEn.trim())
                                viewModel.updateAppSetting("custom_field_2_label_ar", customF2LabelAr.trim())
                                viewModel.updateAppSetting("custom_field_2_label_en", customF2LabelEn.trim())
                                viewModel.updateAppSetting("custom_field_3_label_ar", customF3LabelAr.trim())
                                viewModel.updateAppSetting("custom_field_3_label_en", customF3LabelEn.trim())

                                Toast.makeText(context, if (isAr) "تم تحديث كافة الخانات والتطبيق بنجاح" else "App settings configured successfully!", Toast.LENGTH_SHORT).show()
                                editAboutMode = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BorderCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "حفظ وحفظ التغييرات" else "Save Settings")
                        }
                    }
                }
            }
        } else {
            // Live Public View mode
            // 1. Direct Contact Cards
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAr) "الاتصال والدعم الفني" else "Contact Support Desk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AccessColor()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "ERROR: Phone dialing options unavailable", Toast.LENGTH_SHORT).show()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = AccentPink, modifier = Modifier.size(18.dp))
                        Text(text = phone, color = AccentPink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                    context.startActivity(mailIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "ERROR: Email options unavailable", Toast.LENGTH_SHORT).show()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.MailOutline, contentDescription = null, tint = BorderCyan, modifier = Modifier.size(18.dp))
                        Text(text = email, color = BorderCyan, fontSize = 14.sp)
                    }
                }
            }

            // 2. Rules and Terms of directory usage
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isAr) "شروط وضوابط الاستخدام" else "Guidelines & Regulations",
                        fontWeight = FontWeight.Bold,
                        color = AccessColor(),
                        fontSize = 15.sp
                    )

                    val rules = if (isAr) rulesAr else rulesEn
                    Text(
                        text = rules,
                        fontSize = 13.sp,
                        color = TextLight,
                        lineHeight = 18.sp
                    )
                }
            }
            
            // Helpful message showing you are logged in
            if (viewModel.isAdminMode) {
                Surface(
                    color = BorderCyan.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAr) 
                            "المطوّر: أنت الآن في وضع الإدارة الكامل للداتا ليفيل والمزامنة المحلية." 
                            else "Developer Note: You are logged into full administrator mode.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 11.sp,
                        color = BorderCyan,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AccessColor(): Color {
    return BorderCyan
}

// Icon mapper helper
fun getIconByName(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "medical_services" -> Icons.Default.MedicalServices
        "local_hospital" -> Icons.Default.LocalHospital
        "build" -> Icons.Default.Build
        "school" -> Icons.Default.School
        "directions_car" -> Icons.Default.DirectionsCar
        "star" -> Icons.Default.Star
        "phone" -> Icons.Default.Phone
        "location" -> Icons.Default.LocationOn
        else -> Icons.Default.Category
    }
}
