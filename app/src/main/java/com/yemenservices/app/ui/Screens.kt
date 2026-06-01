package com.yemenservices.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextStyle
import java.util.UUID
import com.yemenservices.app.data.YemenService
import com.yemenservices.app.data.ServiceCategory
import com.yemenservices.app.data.ServiceSubCategory
import com.yemenservices.app.data.ServiceComment
import com.yemenservices.app.data.WelcomeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val isAr by viewModel.isArabic.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val services by viewModel.filteredServices.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedCategoryByState by viewModel.selectedCategory.collectAsState()
    val selectedService by viewModel.selectedServiceForDetails.collectAsState()
    val isAdminAuth by viewModel.isAdminAuthenticated.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val subCategoriesList by viewModel.subCategories.collectAsState()

    var showJoinDialog by remember { mutableStateOf(false) }
    var logoTapCount by remember { mutableStateOf(0) }
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var backdoorPassInput by remember { mutableStateOf("") }
    var backdoorError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                logoTapCount++
                                if (logoTapCount >= 5) {
                                    logoTapCount = 0
                                    showBackdoorDialog = true
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = if (isAr) "دليل الخدمات اليمني" else "Yemen Services Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    // Quick Language Switch Toggle Button
                    IconButton(
                        onClick = { viewModel.isArabic.value = !isAr },
                        modifier = Modifier.testTag("lang_toggle")
                    ) {
                        Text(
                            text = if (isAr) "EN" else "عربي",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Admin Login/Logout Button
                    IconButton(
                        onClick = {
                            if (isAdminAuth) {
                                viewModel.logOutAdmin()
                                Toast.makeText(
                                    context,
                                    if (isAr) "تم تسجيل الخروج" else "Logged out from Admin",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.currentScreen.value = AppScreen.AdminDashboard
                            }
                        },
                        modifier = Modifier.testTag("admin_action_btn")
                    ) {
                        Icon(
                            imageVector = if (isAdminAuth) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Admin Area"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Home || currentScreen == AppScreen.ServicesList || currentScreen == AppScreen.ServiceDetails,
                    onClick = {
                        viewModel.selectedCategory.value = null
                        viewModel.selectedSubCategory.value = null
                        viewModel.currentScreen.value = AppScreen.Home
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(if (isAr) "الرئيسية" else "Home") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SmartAssistant,
                    onClick = { viewModel.currentScreen.value = AppScreen.SmartAssistant },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Assistant") },
                    label = { Text(if (isAr) "المساعد الذكي" else "AI Assistant") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.About,
                    onClick = { viewModel.currentScreen.value = AppScreen.About },
                    icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                    label = { Text(if (isAr) "عن دليلي" else "App Info") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.AdminDashboard,
                    onClick = { viewModel.currentScreen.value = AppScreen.AdminDashboard },
                    icon = { Icon(Icons.Default.ManageAccounts, contentDescription = "Admin") },
                    label = { Text(if (isAr) "لوحة التحكم" else "Admin Panel") }
                )
            }
        },
        floatingActionButton = {
            if (currentScreen == AppScreen.Home) {
                FloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("join_provider_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Join")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAr) "انضم كمزود خدمة" else "Join as Provider",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                AppScreen.Home -> HomeScreen(viewModel = viewModel, isAr = isAr)
                AppScreen.ServicesList -> ServicesListScreen(viewModel = viewModel, isAr = isAr)
                AppScreen.ServiceDetails -> ServiceDetailsScreen(
                    service = selectedService,
                    viewModel = viewModel,
                    isAr = isAr,
                    onBack = {
                        viewModel.currentScreen.value = if (viewModel.selectedCategory.value != null) {
                            AppScreen.ServicesList
                        } else {
                            AppScreen.Home
                        }
                    }
                )
                AppScreen.SmartAssistant -> SmartAssistantPage(viewModel = viewModel, isAr = isAr)
                AppScreen.Favorites -> FavoritesScreen(viewModel = viewModel, isAr = isAr)
                AppScreen.About -> AboutScreen(viewModel = viewModel, isAr = isAr)
                AppScreen.AdminDashboard -> {
                    if (isAdminAuth) {
                        AdminDashboardScreen(viewModel = viewModel, isAr = isAr)
                    } else {
                        AdminLoginScreen(viewModel = viewModel, isAr = isAr)
                    }
                }
            }
        }
    }

    // Modal dialogue to registration request (Join Application as a Service Provider)
    if (showJoinDialog) {
        var joinName by remember { mutableStateOf("") }
        var joinPhone by remember { mutableStateOf("") }
        var joinRegion by remember { mutableStateOf("") }
        var joinLogoUrl by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableStateOf(categoriesList.firstOrNull()?.id ?: "") }
        
        val filteredSubcategoriesByParent = subCategoriesList.filter { it.parentId == selectedCatId }
        var selectedSubCatId by remember(selectedCatId) { 
            mutableStateOf(filteredSubcategoriesByParent.firstOrNull()?.id ?: "") 
        }

        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = {
                Text(
                    text = if (isAr) "طلب انضمام كمزود خدمة" else "Join as a Service Provider",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isAr) "أدخل بياناتك وسيتم مراجعتها من قبل المشرف وتفعيل حسابك." else "Enter your information. Admin will verify before listing you.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = joinName,
                        onValueChange = { joinName = it },
                        label = { Text(if (isAr) "الاسم التجاري أو المهني" else "Business/Professional Name") },
                        modifier = Modifier.fillMaxWidth().testTag("join_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C2C),
                            unfocusedContainerColor = Color(0xFF2C2C2C)
                        ),
                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )

                    OutlinedTextField(
                        value = joinPhone,
                        onValueChange = { joinPhone = it },
                        label = { Text(if (isAr) "رقم الهاتف للاتصال" else "Contact Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("join_phone_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C2C),
                            unfocusedContainerColor = Color(0xFF2C2C2C)
                        ),
                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )

                    OutlinedTextField(
                        value = joinRegion,
                        onValueChange = { joinRegion = it },
                        label = { Text(if (isAr) "المحافظة / المديرية" else "Governorate / Region") },
                        placeholder = { Text(if (isAr) "صنعاء، عدن، تعز، حضرموت..." else "Sanaa, Aden, Taiz...") },
                        modifier = Modifier.fillMaxWidth().testTag("join_region_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C2C),
                            unfocusedContainerColor = Color(0xFF2C2C2C)
                        ),
                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )

                    OutlinedTextField(
                        value = joinLogoUrl,
                        onValueChange = { joinLogoUrl = it },
                        label = { Text(if (isAr) "رابط الصورة الشخصية أو الشعار" else "Photo or Logo URL") },
                        placeholder = { Text("https://example.com/myimage.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C2C),
                            unfocusedContainerColor = Color(0xFF2C2C2C)
                        ),
                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )

                    // Category Select
                    Text(text = if (isAr) "اختر فئة الخدمة:" else "Select service category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categoriesList.take(3).forEach { cat ->
                            val isSelected = selectedCatId == cat.id
                            Button(
                                onClick = { selectedCatId = cat.id },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    contentColor = if (isSelected) Color.White else Color.Black
                                ),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isAr) cat.nameAr.take(8) else cat.nameEn.take(8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Subcategory Select if available
                    if (filteredSubcategoriesByParent.isNotEmpty()) {
                        Text(text = if (isAr) "التخصص الدقيق الفرعي:" else "Select Subspecialist:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredSubcategoriesByParent.take(3).forEach { sub ->
                                val isSelected = selectedSubCatId == sub.id
                                Button(
                                    onClick = { selectedSubCatId = sub.id },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        contentColor = if (isSelected) Color.White else Color.Black
                                    ),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (isAr) sub.nameAr.take(8) else sub.nameEn.take(8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinName.isBlank() || joinPhone.isBlank() || joinRegion.isBlank()) {
                            Toast.makeText(context, if (isAr) "يرجى ملء جميع الحقول المطلوبة!" else "Please fill required fields!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.submitJoinApplication(
                            name = joinName,
                            phone = joinPhone,
                            region = joinRegion,
                            categoryId = selectedCatId,
                            subCategoryId = selectedSubCatId,
                            logoUrl = joinLogoUrl
                        )
                        showJoinDialog = false
                        Toast.makeText(context, if (isAr) "تم إرسال طلب انضمامك للمشرف بنجاح!" else "Join request submitted to Admin!", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Text(if (isAr) "تقديم الطلب الآن" else "Submit Application")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Secret Backdoor dialog
    if (showBackdoorDialog) {
        AlertDialog(
            onDismissRequest = { showBackdoorDialog = false },
            title = {
                Text(
                    text = if (isAr) "بوابة الدخول الخلفية للتحكم" else "Backdoor Portal Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isAr) "أدخل الرمز السري المطور للولوج الفوري والسريع للوحة المشرف دون قيود." else "Enter backdoor developer code to access admin settings instantly.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = backdoorPassInput,
                        onValueChange = {
                            backdoorPassInput = it
                            backdoorError = false
                        },
                        label = { Text(if (isAr) "الرمز السري الخلفي" else "Backdoor Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = backdoorError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (backdoorError) {
                        Text(
                            text = if (isAr) "الرمز غير صحيح!" else "Passcode incorrect!",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.authenticateBackdoor(backdoorPassInput)
                        if (success) {
                            showBackdoorDialog = false
                            backdoorPassInput = ""
                            viewModel.currentScreen.value = AppScreen.AdminDashboard
                            Toast.makeText(
                                context,
                                if (isAr) "تم المصادقة عبر الموثق المباشر!" else "Authenticated via developer portal!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            backdoorError = true
                        }
                    }
                ) {
                    Text(if (isAr) "تأكيد" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackdoorDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun HomeScreen(viewModel: AppViewModel, isAr: Boolean) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val welcomeConfig by viewModel.welcomeConfig.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome and Intro header (Admin customizable & real-time synchronized)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
            ) {
                if (welcomeConfig.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = welcomeConfig.imageUrl,
                        contentDescription = "Welcome Banner Picture",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(bottom = 8.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Text(
                    text = if (isAr) welcomeConfig.titleAr else welcomeConfig.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAr) welcomeConfig.bodyAr else welcomeConfig.bodyEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 18.sp,
                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Beautiful Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text(if (isAr) "ابحث عن خدمة، جهة، رقم هاتف..." else "Search by service, phone, keyword...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .testTag("home_search_bar"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        val activeFilteredServices by viewModel.filteredServices.collectAsState()

        if (searchQuery.isNotBlank()) {
            // Display matching search results directly
            Text(
                text = if (isAr) "نتائج البحث (${activeFilteredServices.size})" else "Search Results (${activeFilteredServices.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .align(if (isAr) Alignment.End else Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            if (activeFilteredServices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAr) "عذراً، لم يتم العثور على نتائج للبحث." else "No results found for your search.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeFilteredServices) { service ->
                        ServiceItemCard(service = service, viewModel = viewModel, isAr = isAr)
                    }
                }
            }
        } else {
            // Categories display
            Text(
                text = if (isAr) "تصفح حسب الفئة" else "Browse by Category",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(if (isAr) Alignment.End else Alignment.Start)
                    .padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { category ->
                    CategoryItemCard(category = category, isAr = isAr) {
                        viewModel.selectedCategory.value = category.id
                        viewModel.currentScreen.value = AppScreen.ServicesList
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItemCard(category: ServiceCategory, isAr: Boolean, onClick: () -> Unit) {
    val icon = when (category.iconName) {
        "emergency" -> Icons.Default.Warning
        "medical" -> Icons.Default.HealthAndSafety
        "finance" -> Icons.Default.Payments
        "transport" -> Icons.Default.TimeToLeave
        "government" -> Icons.Default.AccountBalance
        else -> Icons.Default.Category
    }

    val tintColor = when (category.iconName) {
        "emergency" -> Color(0xFFD32F2F)
        "medical" -> Color(0xFF00796B)
        "finance" -> Color(0xFF388E3C)
        "transport" -> Color(0xFF1976D2)
        "government" -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("cat_card_${category.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start
        ) {
            if (!isAr) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (category.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = category.nameEn,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open",
                    tint = Color.Gray
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "افتح",
                    tint = Color.Gray
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                        Text(
                            text = category.nameAr,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Right
                        )
                        if (category.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "مثبت",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun ServicesListScreen(viewModel: AppViewModel, isAr: Boolean) {
    val categoryId by viewModel.selectedCategory.collectAsState()
    val services by viewModel.filteredServices.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val category = categoriesList.firstOrNull { it.id == categoryId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start
        ) {
            if (!isAr) {
                IconButton(onClick = { viewModel.currentScreen.value = AppScreen.Home }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = category?.nameEn ?: "Services",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Text(
                    text = category?.nameAr ?: "الخدمات المتاحة",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.currentScreen.value = AppScreen.Home }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
            }
        }

        // Local Search filter bar
        var localQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = localQuery,
            onValueChange = {
                localQuery = it
                viewModel.searchQuery.value = it
            },
            placeholder = { Text(if (isAr) "ابحث في هذه الفئة..." else "Search in this category...") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filter") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        if (services.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isAr) "لا توجد خدمات مضافة في هذه الفئة." else "No services added in this category.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(services) { service ->
                    ServiceItemCard(service = service, viewModel = viewModel, isAr = isAr)
                }
            }
        }
    }
}

@Composable
fun ServiceItemCard(service: YemenService, viewModel: AppViewModel, isAr: Boolean) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsState()
    val isFav = favorites.contains(service.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.selectedServiceForDetails.value = service
                viewModel.currentScreen.value = AppScreen.ServiceDetails
            }
            .testTag("service_card_${service.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start
            ) {
                if (!isAr) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = service.nameEn,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = service.addressEn.ifBlank { "Yemen" },
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleFavorite(service.id) },
                        modifier = Modifier.testTag("fav_toggle_${service.id}")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Fav",
                            tint = if (isFav) Color.Red else Color.LightGray
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(service.id) },
                        modifier = Modifier.testTag("fav_toggle_${service.id}")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "مفضلة",
                            tint = if (isFav) Color.Red else Color.LightGray
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = service.nameAr,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = service.addressAr.ifBlank { "اليمن" },
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Phone action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phoneNumber}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot dial numbers on this device", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAr) "اتصال" else "Call", fontSize = 11.sp)
                }

                // WhatsApp Button
                if (service.whatsappNumber.isNotBlank()) {
                    Button(
                        onClick = {
                            var formattedNumber = service.whatsappNumber.trim()
                            if (!formattedNumber.startsWith("+") && !formattedNumber.startsWith("967")) {
                                if (formattedNumber.startsWith("0")) {
                                    formattedNumber = "967" + formattedNumber.substring(1)
                                } else if (formattedNumber.length == 9) {
                                    formattedNumber = "967$formattedNumber"
                                }
                            }
                            try {
                                val url = "https://api.whatsapp.com/send?phone=$formattedNumber"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAr) "واتساب" else "WhatsApp", fontSize = 11.sp, color = Color.White)
                    }
                }

                // SMS Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${service.phoneNumber}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot send SMS", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isAr) "رسالة" else "SMS", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ServiceDetailsScreen(service: YemenService?, viewModel: AppViewModel, isAr: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    if (service == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isAr) "لم يتم العثور على تفاصيل الخدمة." else "Service details not found.")
        }
        return
    }

    val favorites by viewModel.favorites.collectAsState()
    val isFav = favorites.contains(service.id)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start
        ) {
            if (!isAr) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "التفاصيل الكاملة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!isAr) {
                                Text(
                                    text = service.category.uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                IconButton(onClick = { viewModel.toggleFavorite(service.id) }) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFav) Color.Red else Color.LightGray
                                    )
                                }
                            } else {
                                IconButton(onClick = { viewModel.toggleFavorite(service.id) }) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "مفضلة",
                                        tint = if (isFav) Color.Red else Color.LightGray
                                    )
                                }
                                Text(
                                    text = service.category.uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isAr) service.nameAr else service.nameEn,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Secondary Details card
                        val desc = if (isAr) service.descriptionAr else service.descriptionEn
                        if (desc.isNotBlank()) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 24.sp,
                                textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = if (isAr) "لا يتوفر وصف تفصيلي حالياً لهذه الخدمة." else "No description available for this service.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        // Address Row
                        val addr = if (isAr) service.addressAr else service.addressEn
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isAr) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = addr.ifBlank { "Yemen" }, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(text = addr.ifBlank { "اليمن" }, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray)
                            }
                        }

                        // Phone details
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isAr) {
                                Icon(Icons.Default.PhoneEnabled, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = service.phoneNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            } else {
                                Text(text = service.phoneNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.PhoneEnabled, contentDescription = null, tint = Color.Gray)
                            }
                        }

                        // WhatsApp info
                        if (service.whatsappNumber.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isAr) Arrangement.End else Arrangement.Start
                            ) {
                                if (!isAr) {
                                    Icon(Icons.Default.Message, contentDescription = null, tint = Color(0xFF25D366))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = service.whatsappNumber, style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Text(text = service.whatsappNumber, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Message, contentDescription = null, tint = Color(0xFF25D366))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phoneNumber}"))
                                context.startActivity(intent)
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().testTag("detail_call_fab")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Call, contentDescription = "Call")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isAr) "اتصل الآن" else "Call Now")
                            }
                        }
                    }

                    if (service.whatsappNumber.isNotBlank()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    var formattedNumber = service.whatsappNumber.trim()
                                    if (!formattedNumber.startsWith("+") && !formattedNumber.startsWith("967")) {
                                        if (formattedNumber.startsWith("0")) {
                                            formattedNumber = "967" + formattedNumber.substring(1)
                                        } else if (formattedNumber.length == 9) {
                                            formattedNumber = "967$formattedNumber"
                                        }
                                    }
                                    val url = "https://api.whatsapp.com/send?phone=$formattedNumber"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                containerColor = Color(0xFFE8F5E9),
                                contentColor = Color(0xFF2E7D32),
                                modifier = Modifier.fillMaxWidth().testTag("detail_whatsapp_fab")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = "WhatsApp")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isAr) "مراسلة واتساب" else "WhatsApp")
                                }
                            }
                        }
                    }
                }
            }

            // Visitor Comments & Reviews Section
            item {
                val comments by viewModel.activeComments.collectAsState()
                val isAdminAuth by viewModel.isAdminAuthenticated.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = if (isAr) "تعليقات وآراء الزوار (${comments.size})" else "Visitor Reviews (${comments.size})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Add Review Form
                        var authorName by remember { mutableStateOf("") }
                        var commentText by remember { mutableStateOf("") }
                        var ratingValue by remember { mutableStateOf(5) }
                        var submittalSuccess by remember { mutableStateOf(false) }

                        if (submittalSuccess) {
                            Text(
                                text = if (isAr) "تم إضافة تعليقك بنجاح! شكراً لك." else "Comment added successfully! Thank you.",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = if (isAr) "أضف تقييمك ورأيك" else "Leave a Review",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Dynamic Star Picker
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    (1..5).forEach { starIndex ->
                                        val isFilled = starIndex <= ratingValue
                                        Icon(
                                            imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = if (isFilled) Color(0xFFFFB300) else Color.LightGray,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable { ratingValue = starIndex }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = authorName,
                                    onValueChange = { authorName = it },
                                    label = { Text(if (isAr) "الاسم" else "Name") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    label = { Text(if (isAr) "صيغة التعليق" else "Comment text") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    maxLines = 3
                                )

                                Button(
                                    onClick = {
                                        if (authorName.isNotBlank() && commentText.isNotBlank()) {
                                            viewModel.addComment(
                                                serviceId = service.id,
                                                authorName = authorName.trim(),
                                                text = commentText.trim(),
                                                rating = ratingValue.toFloat()
                                            )
                                            authorName = ""
                                            commentText = ""
                                            ratingValue = 5
                                            submittalSuccess = true
                                        }
                                    },
                                    modifier = Modifier.align(if (isAr) Alignment.Start else Alignment.End)
                                ) {
                                    Text(if (isAr) "إرسال التعليق" else "Submit comment")
                                }
                            }
                        }

                        // 2. Reviews lists
                        if (comments.isEmpty()) {
                            Text(
                                text = if (isAr) "لا توجد تعليقات بعد لهذا مقدم الخدمة. كن الأول لتقييمه!" else "No comments yet for this service. Be first to rate!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            var showEditCommentId by remember { mutableStateOf<String?>(null) }
                            var editCommentText by remember { mutableStateOf("") }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                comments.forEach { comment ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (isAr) {
                                                    // Rating stars (Ar)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        (1..5).forEach { star ->
                                                            Icon(
                                                                imageVector = if (star <= comment.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                                contentDescription = null,
                                                                tint = Color(0xFFFFB300),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = comment.authorName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                } else {
                                                    Text(
                                                        text = comment.authorName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    // Rating stars (En)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        (1..5).forEach { star ->
                                                            Icon(
                                                                imageVector = if (star <= comment.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                                contentDescription = null,
                                                                tint = Color(0xFFFFB300),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = comment.commentText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Formatting timestamp
                                            val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(comment.timestamp))
                                            Text(
                                                text = date,
                                                fontSize = 10.sp,
                                                color = Color.LightGray,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                textAlign = if (isAr) TextAlign.Left else TextAlign.Right
                                            )

                                            // Admin Controls: Edit / Delete
                                            if (isAdminAuth) {
                                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.deleteComment(comment.id)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = if (isAr) "حذف للآدمن" else "Admin Delete",
                                                            color = Color.Red,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            showEditCommentId = comment.id
                                                            editCommentText = comment.commentText
                                                        }
                                                    ) {
                                                        Text(
                                                            text = if (isAr) "تعديل للآدمن" else "Admin Edit",
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Interactive comment editor popup for Admin
                            if (showEditCommentId != null) {
                                AlertDialog(
                                    onDismissRequest = { showEditCommentId = null },
                                    title = { Text(if (isAr) "تعديل تعليق الزائر كمسؤول" else "Edit Visitor Review") },
                                    text = {
                                        OutlinedTextField(
                                            value = editCommentText,
                                            onValueChange = { editCommentText = it },
                                            label = { Text(if (isAr) "التعليق المعدل" else "Modified comment") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                val activeEditId = showEditCommentId
                                                val originalComment = comments.find { it.id == activeEditId }
                                                if (originalComment != null && editCommentText.isNotBlank()) {
                                                    viewModel.updateComment(originalComment.copy(commentText = editCommentText.trim()))
                                                }
                                                showEditCommentId = null
                                            }
                                        ) {
                                            Text(if (isAr) "حفظ التعديل" else "Save Changes")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showEditCommentId = null }) {
                                            Text(if (isAr) "إلغاء الأمر" else "Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: AppViewModel, isAr: Boolean) {
    val favorites by viewModel.favorites.collectAsState()
    val allServices by viewModel.yemenServices.collectAsState()
    val favoriteServices = allServices.filter { favorites.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (isAr) "قائمة المفضلة (${favoriteServices.size})" else "My Favorites (${favoriteServices.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(if (isAr) Alignment.End else Alignment.Start)
                .padding(bottom = 16.dp)
        )

        if (favoriteServices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAr) 
                        "لم تقم بإضافة أي خدمة لقائمة المفضلة حتى الآن." 
                    else 
                        "You haven't added any services to favorites yet.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favoriteServices) { service ->
                    ServiceItemCard(service = service, viewModel = viewModel, isAr = isAr)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(viewModel: AppViewModel, isAr: Boolean) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
        }

        item {
            Text(
                text = if (isAr) "دليل الخدمات اليمني السحابي" else "Yemeni Cloud Services Directory",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Version 2.0.0 (Firebase Cloud Sync Edition)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = if (isAr) "مهمتنا وأهدافنا" else "Our Mission",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) 
                            "نهدف إلى تسهيل الوصول المجانية والكاملة لأرقام الهواتف، العناوين، وقنوات الاتصال بالطوارئ والخدمات الطبية والتجارية والمالية والحكومية الهامة داخل الجمهورية اليمنية وتزامنها بشكل فوري ولحظي بين جميع المستخدمين المتصلين بالشبكة."
                        else 
                            "This application aims to facilitate complete, free access to phone directories, emergency hotlines, banks, transport, and government infrastructure services in Yemen, securely and instantly synchronized in real-time.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = if (isAr) "تزامن البيانات السحابي الفوري" else "Real-time Cloud Synchronization",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) 
                            "يعتمد التطبيق على قاعدة بيانات سحابية آمنة ومفتوحة بالكامل باستخدام Firebase Firestore لتأمين خدمات متزامنة وفورية لجميع الأجهزة والبيانات في نفس اللحظة وبأعلى معايير الأداء والسرعة."
                        else 
                            "The application leverages a secure, highly-optimized Firebase Firestore cloud database, enabling instant real-time synchronization and low-latency updates across all devices seamlessly.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        textAlign = if (isAr) TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.resetToDefaults()
                    Toast.makeText(
                        context,
                        if (isAr) "تم إعادة تعيين البيانات للمصنع الافتراضي" else "Successfully reset directory to defaults",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isAr) "إعادة تعيين البيانات الافتراضية" else "Reset to Factory Defaults")
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(viewModel: AppViewModel, isAr: Boolean) {
    val services by viewModel.yemenServices.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val subCategoriesList by viewModel.subCategories.collectAsState()
    val welcomeConfig by viewModel.welcomeConfig.collectAsState()
    val joinRequests by viewModel.joinApplications.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Listings, 1: Categories & Subs, 2: Applications Review, 3: System settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (isAr) "لوحة التحكم وإدارة النظام" else "System Admin Console",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(if (isAr) Alignment.End else Alignment.Start)
                .padding(bottom = 12.dp)
        )

        // Custom M3 Tab Layout with 4 key panels
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (isAr) "الموفرين" else "Providers", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isAr) "الأقسام والفرعية" else "Categories Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { 
                    val pendingCount = joinRequests.count { it.status == "pending" }
                    val badgeText = if (pendingCount > 0) " ($pendingCount)" else ""
                    Text((if (isAr) "طلبات الانضمام" else "Join Requests") + badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold) 
                }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(if (isAr) "الإعدادات العامة" else "Global Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Providers Directory Tab
                var showForm by remember { mutableStateOf(false) }
                var editingServiceId by remember { mutableStateOf<String?>(null) }
                var serviceNameAr by remember { mutableStateOf("") }
                var serviceNameEn by remember { mutableStateOf("") }
                var serviceCategory by remember { mutableStateOf(categoriesList.firstOrNull()?.id ?: "emergency") }
                var serviceSubCategory by remember { mutableStateOf("") }
                var servicePhoneNumber by remember { mutableStateOf("") }
                var serviceWhatsapp by remember { mutableStateOf("") }
                var serviceAddressAr by remember { mutableStateOf("") }
                var serviceAddressEn by remember { mutableStateOf("") }
                var serviceDescAr by remember { mutableStateOf("") }
                var serviceDescEn by remember { mutableStateOf("") }
                var serviceImageUrl by remember { mutableStateOf("") }
                var serviceIsPinned by remember { mutableStateOf(false) }
                var serviceIsRecommended by remember { mutableStateOf(false) }
                var serviceOrderIndex by remember { mutableStateOf("0") }

                if (showForm) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = if (editingServiceId == null) {
                                    if (isAr) "إضافة دليل خدمة جديد" else "Create New Service Listing"
                                } else {
                                    if (isAr) "تعديل الخدمة المحددة" else "Edit Service Listing"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = serviceNameAr,
                                onValueChange = { serviceNameAr = it },
                                label = { Text(if (isAr) "اسم الخدمة / المهني باللغة العربية" else "Arabic Service Name") },
                                modifier = Modifier.fillMaxWidth().testTag("form_name_ar"),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            OutlinedTextField(
                                value = serviceNameEn,
                                onValueChange = { serviceNameEn = it },
                                label = { Text(if (isAr) "اسم الخدمة / المهني باللغة الإنجليزية" else "English Service Name") },
                                modifier = Modifier.fillMaxWidth().testTag("form_name_en"),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            // Select category
                            Text(text = if (isAr) "اختر فئة الدليل الرئيسية:" else "Primary Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                categoriesList.take(3).forEach { cat ->
                                    val isSel = serviceCategory == cat.id
                                    Button(
                                        onClick = { 
                                            serviceCategory = cat.id
                                            serviceSubCategory = ""
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            contentColor = if (isSel) Color.White else Color.Black
                                        ),
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (isAr) cat.nameAr.take(8) else cat.nameEn.take(8), fontSize = 10.sp)
                                    }
                                }
                            }

                            // Select subcategory if parent exists
                            val subsForCat = subCategoriesList.filter { it.parentId == serviceCategory }
                            if (subsForCat.isNotEmpty()) {
                                Text(text = if (isAr) "اختر التخصص الفرعي الدقيق:" else "Select Subcategory:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    subsForCat.take(3).forEach { sub ->
                                        val isSel = serviceSubCategory == sub.id
                                        Button(
                                            onClick = { serviceSubCategory = sub.id },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                contentColor = if (isSel) Color.White else Color.Black
                                            ),
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(if (isAr) sub.nameAr.take(8) else sub.nameEn.take(8), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = servicePhoneNumber,
                                onValueChange = { servicePhoneNumber = it },
                                label = { Text(if (isAr) "رقم الهاتف والاتصال" else "Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth().testTag("form_phone"),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            OutlinedTextField(
                                value = serviceWhatsapp,
                                onValueChange = { serviceWhatsapp = it },
                                label = { Text(if (isAr) "رقم الواتساب (اختياري)" else "WhatsApp Number (Optional)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            OutlinedTextField(
                                value = serviceAddressAr,
                                onValueChange = { serviceAddressAr = it },
                                label = { Text(if (isAr) "العنوان بالعربية" else "Arabic Address") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            OutlinedTextField(
                                value = serviceAddressEn,
                                onValueChange = { serviceAddressEn = it },
                                label = { Text(if (isAr) "العنوان بالإنجليزية" else "English Address") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            OutlinedTextField(
                                value = serviceImageUrl,
                                onValueChange = { serviceImageUrl = it },
                                label = { Text(if (isAr) "رابط الصورة الشخصية" else "Profile Pic / Photo URL") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            OutlinedTextField(
                                value = serviceDescAr,
                                onValueChange = { serviceDescAr = it },
                                label = { Text(if (isAr) "الوصف بالعربية" else "Arabic Description") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            OutlinedTextField(
                                value = serviceDescEn,
                                onValueChange = { serviceDescEn = it },
                                label = { Text(if (isAr) "الوصف بالإنجليزية" else "English Description") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = serviceIsPinned, onCheckedChange = { serviceIsPinned = it })
                                Text(text = if (isAr) "تثبيت في أعلى القائمة وبداية البحث" else "Pin to top of search results", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = serviceIsRecommended, onCheckedChange = { serviceIsRecommended = it })
                                Text(text = if (isAr) "تمييز كـ خدمة موصى بها من قِبلنا" else "Recommend listing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = serviceOrderIndex,
                                onValueChange = { serviceOrderIndex = it },
                                label = { Text(if (isAr) "رقم الترتيب الهرمي (الأكبر يظهر أولاً)" else "Sorting rank index") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (serviceNameAr.isBlank() || servicePhoneNumber.isBlank()) return@Button
                                        val idxVal = serviceOrderIndex.toIntOrNull() ?: 0
                                        viewModel.saveService(
                                            id = editingServiceId,
                                            nameAr = serviceNameAr,
                                            nameEn = serviceNameEn,
                                            category = serviceCategory,
                                            subCategory = serviceSubCategory,
                                            phone = servicePhoneNumber,
                                            whatsapp = serviceWhatsapp,
                                            addressAr = serviceAddressAr,
                                            addressEn = serviceAddressEn,
                                            descriptionAr = serviceDescAr,
                                            descriptionEn = serviceDescEn,
                                            imageUrl = serviceImageUrl,
                                            isPinned = serviceIsPinned,
                                            isRecommended = serviceIsRecommended,
                                            orderIndex = idxVal
                                        )
                                        showForm = false
                                    },
                                    modifier = Modifier.weight(1f).testTag("form_submit_btn")
                                ) {
                                    Text(if (isAr) "حفظ وتسجيل" else "Save & Commit")
                                }

                                TextButton(onClick = { showForm = false }, modifier = Modifier.weight(0.5f)) {
                                    Text(if (isAr) "إلغاء" else "Cancel")
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            editingServiceId = null
                            serviceNameAr = ""
                            serviceNameEn = ""
                            serviceCategory = categoriesList.firstOrNull()?.id ?: "emergency"
                            serviceSubCategory = ""
                            servicePhoneNumber = ""
                            serviceWhatsapp = ""
                            serviceAddressAr = ""
                            serviceAddressEn = ""
                            serviceDescAr = ""
                            serviceDescEn = ""
                            serviceImageUrl = ""
                            serviceIsPinned = false
                            serviceIsRecommended = false
                            serviceOrderIndex = "0"
                            showForm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAr) "إضافة مزود خدمة أو مهني جديد" else "Add New Professional Listing")
                    }
                }

                Text(
                    text = if (isAr) "جميع الخدمات المدرجة للتحكم والفلترة (${services.size})" else "All Listings Database (${services.size})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(services) { service ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isAr) service.nameAr else service.nameEn,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (service.isPinned) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                        if (service.isRecommended) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Star, contentDescription = "Recommended", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = "القسم: ${service.category} / فرعي: ${service.subCategory.ifBlank { "بلا" }}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "الهاتف: ${service.phoneNumber} | ترتيب هرمي: ${service.orderIndex}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        editingServiceId = service.id
                                        serviceNameAr = service.nameAr
                                        serviceNameEn = service.nameEn
                                        serviceCategory = service.category
                                        serviceSubCategory = service.subCategory
                                        servicePhoneNumber = service.phoneNumber
                                        serviceWhatsapp = service.whatsappNumber
                                        serviceAddressAr = service.addressAr
                                        serviceAddressEn = service.addressEn
                                        serviceDescAr = service.descriptionAr
                                        serviceDescEn = service.descriptionEn
                                        serviceImageUrl = service.imageUrl
                                        serviceIsPinned = service.isPinned
                                        serviceIsRecommended = service.isRecommended
                                        serviceOrderIndex = service.orderIndex.toString()
                                        showForm = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }

                                IconButton(onClick = { viewModel.deleteService(service.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Categories & Subcategories Manager Tab
                var categoryNameAr by remember { mutableStateOf("") }
                var categoryNameEn by remember { mutableStateOf("") }
                var categoryEmoji by remember { mutableStateOf("") }
                var categoryIdInput by remember { mutableStateOf("") }
                var catFormOpen by remember { mutableStateOf(false) }

                var subNameAr by remember { mutableStateOf("") }
                var subNameEn by remember { mutableStateOf("") }
                var subParentId by remember { mutableStateOf(categoriesList.firstOrNull()?.id ?: "") }
                var subFormOpen by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(if (isAr) "التحكم بالأقسام الرئيسية (Categories Manager)" else "Main Categories Control", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (catFormOpen) {
                            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = categoryIdInput,
                                        onValueChange = { categoryIdInput = it },
                                        label = { Text("المعرف الفريد (ID)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                                    )
                                    OutlinedTextField(
                                        value = categoryNameAr,
                                        onValueChange = { categoryNameAr = it },
                                        label = { Text("الاسم بالعربية") },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                                    )
                                    OutlinedTextField(
                                        value = categoryNameEn,
                                        onValueChange = { categoryNameEn = it },
                                        label = { Text("الاسم بالإنجليزية") },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                                    )
                                    OutlinedTextField(
                                        value = categoryEmoji,
                                        onValueChange = { categoryEmoji = it },
                                        label = { Text("الأيقونة التعبيرية Emoji") },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            if (categoryIdInput.isBlank() || categoryNameAr.isBlank()) return@Button
                                            val cat = ServiceCategory(
                                                id = categoryIdInput,
                                                nameAr = categoryNameAr,
                                                nameEn = categoryNameEn,
                                                iconName = categoryEmoji,
                                                isPinned = false,
                                                orderIndex = 0
                                            )
                                            viewModel.saveCategory(cat)
                                            catFormOpen = false
                                            categoryIdInput = ""
                                            categoryNameAr = ""
                                            categoryNameEn = ""
                                            categoryEmoji = ""
                                        }, modifier = Modifier.weight(1f)) {
                                            Text(if (isAr) "تسجيل وحفظ" else "Save Category")
                                        }
                                        TextButton(onClick = { catFormOpen = false }) { Text(if (isAr) "إلغاء" else "Cancel") }
                                    }
                                }
                            }
                        } else {
                            Button(onClick = { catFormOpen = true }, modifier = Modifier.padding(top = 8.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAr) "إضافة قسم رئيسي جديد" else "Add Main Category")
                            }
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(if (isAr) "قائمة الأقسام المتوفرة:" else "Active Main Categories:", fontWeight = FontWeight.Bold)
                                categoriesList.forEach { cat ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${cat.iconName} ${cat.nameAr} (${cat.id})", fontSize = 13.sp)
                                        IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(if (isAr) "التحكم بالتخصصات الفرعية (SubCategories Manager)" else "Subcategories Specialist Control", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (subFormOpen) {
                            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = subNameAr,
                                        onValueChange = { subNameAr = it },
                                        label = { Text("اسم التخصص بالعربية") },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                                    )
                                    OutlinedTextField(
                                        value = subNameEn,
                                        onValueChange = { subNameEn = it },
                                        label = { Text("اسم التخصص بالإنجليزية") },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                                    )

                                    Text("الفئة الأب المرتبطة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        categoriesList.take(3).forEach { cat ->
                                            val isSel = subParentId == cat.id
                                            Button(
                                                onClick = { subParentId = cat.id },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.LightGray),
                                                modifier = Modifier.weight(1f).height(30.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(cat.nameAr, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                                        Button(onClick = {
                                            if (subNameAr.isBlank() || subParentId.isBlank()) return@Button
                                            val sub = ServiceSubCategory(
                                                id = UUID.randomUUID().toString(),
                                                parentId = subParentId,
                                                nameAr = subNameAr,
                                                nameEn = subNameEn,
                                                iconEmoji = "⚙️",
                                                orderIndex = 0
                                            )
                                            viewModel.saveSubCategory(sub)
                                            subFormOpen = false
                                            subNameAr = ""
                                            subNameEn = ""
                                        }, modifier = Modifier.weight(1f)) {
                                            Text(if (isAr) "تسجيل وحفظ" else "Save SubCategory")
                                        }
                                        TextButton(onClick = { subFormOpen = false }) { Text(if (isAr) "إلغاء" else "Cancel") }
                                    }
                                }
                            }
                        } else {
                            Button(onClick = { subFormOpen = true }, modifier = Modifier.padding(top = 8.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAr) "إضافة فئة فرعية تخصصية جديدة" else "Add Subspecialty Category")
                            }
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(if (isAr) "التخصصات الفرعية النشطة:" else "Active Subcategories:", fontWeight = FontWeight.Bold)
                                subCategoriesList.forEach { sub ->
                                    val parentName = categoriesList.find { it.id == sub.parentId }?.nameAr ?: sub.parentId
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${sub.nameAr} ➔ [${parentName}]", fontSize = 12.sp)
                                        IconButton(onClick = { viewModel.deleteSubCategory(sub.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Applications Review Tab
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = if (isAr) "مراجعة وفحص طلبات الانضمام المعلقة (${joinRequests.size})" else "Join Applications Inbox (${joinRequests.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAr) "القبول يحول المهني تلقائياً ومباشرة كـ مزود معتمد وخدمة نشطة على الدليل للمستخدمين." else "Approving instantly generates an active listing in the directory.",
                            fontSize = 12.sp, color = Color.Gray
                        )
                    }

                    if (joinRequests.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                Text(if (isAr) "لا توجد طلبات انضمام معلقة حالياً." else "No pending registration requests currently.", color = Color.Gray)
                            }
                        }
                    }

                    items(joinRequests) { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = app.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    val badgeColor = when (app.status) {
                                        "approved" -> Color(0xFF2E7D32)
                                        "rejected" -> Color(0xFFC62828)
                                        else -> Color(0xFFEF6C00)
                                    }
                                    Text(
                                        text = if (isAr) {
                                            when (app.status) {
                                                "approved" -> "تم القبول"
                                                "rejected" -> "مرفوض"
                                                else -> "قيد المراجعة"
                                            }
                                        } else app.status.uppercase(),
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(text = "رقم التواصل: ${app.phone}", fontSize = 13.sp)
                                Text(text = "المنطقة / المحافظة: ${app.region}", fontSize = 13.sp)
                                
                                val pCat = categoriesList.find { it.id == app.categoryId }?.nameAr ?: app.categoryId
                                val sCat = subCategoriesList.find { it.id == app.subCategoryId }?.nameAr ?: app.subCategoryId
                                Text(text = "الفئة: $pCat | تخطيط فرعي: $sCat", fontSize = 13.sp)

                                if (app.logoUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "صورة الطلب المرفقة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    AsyncImage(
                                        model = app.logoUrl,
                                        contentDescription = "App logo",
                                        modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (app.status == "pending") {
                                        Button(
                                            onClick = { viewModel.updateJoinApplicationStatus(app.id, "approved") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (isAr) "موافقة وقبول فوري" else "Approve & Publish")
                                        }

                                        Button(
                                            onClick = { viewModel.updateJoinApplicationStatus(app.id, "rejected") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (isAr) "رفض الطلب" else "Reject")
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.deleteJoinApplication(app.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isAr) "حذف السجل التاريخي" else "Delete Archive Log")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Live Configurator Tab
                var titleArInput by remember { mutableStateOf(welcomeConfig.titleAr) }
                var titleEnInput by remember { mutableStateOf(welcomeConfig.titleEn) }
                var bodyArInput by remember { mutableStateOf(welcomeConfig.bodyAr) }
                var bodyEnInput by remember { mutableStateOf(welcomeConfig.bodyEn) }
                var imageUrlInput by remember { mutableStateOf(welcomeConfig.imageUrl) }
                var themeInput by remember { mutableStateOf(welcomeConfig.globalTheme) }
                
                var phoneSupport by remember { mutableStateOf(welcomeConfig.supportPhone) }
                var whatsappSupport by remember { mutableStateOf(welcomeConfig.supportWhatsapp) }
                var emailSupport by remember { mutableStateOf(welcomeConfig.supportEmail) }
                var bannerExtInput by remember { mutableStateOf(welcomeConfig.bannerExtUrl) }
                var greetingInputAr by remember { mutableStateOf(welcomeConfig.assistantGreetingAr) }
                var greetingInputEn by remember { mutableStateOf(welcomeConfig.assistantGreetingEn) }

                var savedNotifier by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = if (isAr) "تخصيص المظهر وتعميم النصوص الإعلانية والترحيبية" else "Global System Settings Block",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAr) "جميع التعديلات هنا تُبث وتسجل لحظياً (Real-time Sync) للشبكة." else "Modifications are pushed instantly across all connected nodes.",
                            fontSize = 11.sp, color = Color.Gray
                        )
                    }

                    item {
                        if (savedNotifier) {
                            Text(
                                text = if (isAr) "تم الحفظ والتعميم الفوري بنجاح! 🎉" else "Saved and broadcasted successfully! 🎉",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Theme select selector
                        Text("اختر المظهر والخامة العامة للتطبيق (Dynamic Themes):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val themeOptions = listOf(
                            Pair("red_black", if (isAr) "الأحمر والأسود الافتراضي (Dark)" else "Red & Black (Dark)"),
                            Pair("royal_indigo", if (isAr) "الأزرق الملكي الفاخر (Dark)" else "Royal Indigo (Dark)"),
                            Pair("emerald_green", if (isAr) "الأخضر الزمردي المريح (Dark)" else "Emerald Green (Dark)"),
                            Pair("slate_silver", if (isAr) "الفضي الكلاسيكي اللامع (Dark)" else "Slate Silver (Dark)"),
                            Pair("ocean_teal", if (isAr) "التركواز المحيطي المهدئ (Dark)" else "Ocean Teal (Dark)"),
                            Pair("beige_cream", if (isAr) "البيج الكريمي الدافئ (Light)" else "Warm Beige (Light)")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            themeOptions.forEach { opt ->
                                val isSelected = themeInput == opt.first
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { themeInput = opt.first }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = { themeInput = opt.first })
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(opt.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = titleArInput,
                            onValueChange = { titleArInput = it; savedNotifier = false },
                            label = { Text("أبرز عناوين الترحيب (العربية)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = titleEnInput,
                            onValueChange = { titleEnInput = it; savedNotifier = false },
                            label = { Text("أبرز عناوين الترحيب (الإنجليزية)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bodyArInput,
                            onValueChange = { bodyArInput = it; savedNotifier = false },
                            label = { Text("مضمون الترحيب (العربية)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bodyEnInput,
                            onValueChange = { bodyEnInput = it; savedNotifier = false },
                            label = { Text("مضمون الترحيب (الإنجليزية)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = imageUrlInput,
                            onValueChange = { imageUrlInput = it; savedNotifier = false },
                            label = { Text("رابط الصورة الترحيبية (أية صيغة JPG / PNG / GIF )") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bannerExtInput,
                            onValueChange = { bannerExtInput = it; savedNotifier = false },
                            label = { Text("رابط البن الإعلاني الخارجي (موقع إلكتروني)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = phoneSupport,
                            onValueChange = { phoneSupport = it; savedNotifier = false },
                            label = { Text("رقم هاتف الدعم الفني للدليل") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = whatsappSupport,
                            onValueChange = { whatsappSupport = it; savedNotifier = false },
                            label = { Text("رقم واتساب المشرفين للدعم") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = emailSupport,
                            onValueChange = { emailSupport = it; savedNotifier = false },
                            label = { Text("البريد الإلكتروني للشكاوى") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = greetingInputAr,
                            onValueChange = { greetingInputAr = it; savedNotifier = false },
                            label = { Text("رسالة ترحيب المساعد الذكي (العربية)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = greetingInputEn,
                            onValueChange = { greetingInputEn = it; savedNotifier = false },
                            label = { Text("رسالة ترحيب المساعد الذكي (الإنجليزية)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0xFF2C2C2C), unfocusedContainerColor = Color(0xFF2C2C2C))
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                val conf = welcomeConfig.copy(
                                    globalTheme = themeInput,
                                    titleAr = titleArInput,
                                    titleEn = titleEnInput,
                                    bodyAr = bodyArInput,
                                    bodyEn = bodyEnInput,
                                    imageUrl = imageUrlInput,
                                    supportPhone = phoneSupport,
                                    supportWhatsapp = whatsappSupport,
                                    supportEmail = emailSupport,
                                    bannerExtUrl = bannerExtInput,
                                    assistantGreetingAr = greetingInputAr,
                                    assistantGreetingEn = greetingInputEn
                                )
                                viewModel.saveWelcomeConfig(conf)
                                savedNotifier = true
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(if (isAr) "حفظ وتعميم التكوينات الآن" else "Sync Application Properties")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLoginScreen(viewModel: AppViewModel, isAr: Boolean) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isAr) "بوابة حوكمة دليلي للأمن العالي" else "Admin High Security Gate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isAr) "الرجاء تسجيل الدخول ببيانات المشرف للتحكم" else "Please authenticate to manage directory databases.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )

                if (isError) {
                    Text(
                        text = if (isAr) "خطأ: اسم المستخدم أو كلمة المرور غير صحيحة!" else "Error: Invalid username or password",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        isError = false
                    },
                    label = { Text(if (isAr) "اسم المشرف" else "Admin Username") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_user_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C2C),
                        unfocusedContainerColor = Color(0xFF2C2C2C)
                    ),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = { Text(if (isAr) "كلمة المرور" else "Security Password") },
                    modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C2C),
                        unfocusedContainerColor = Color(0xFF2C2C2C)
                    ),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val authenticated = viewModel.authenticateAdmin(username, password)
                        if (!authenticated) {
                            isError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_login_submit_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isAr) "أكّد هويّتي ودخول" else "Authenticate & Authenticate", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SmartAssistantPage(viewModel: AppViewModel, isAr: Boolean) {
    val categoriesList by viewModel.categories.collectAsState()
    val servicesList by viewModel.yemenServices.collectAsState()
    val subCategoriesList by viewModel.subCategories.collectAsState()
    val welcomeConfig by viewModel.welcomeConfig.collectAsState()

    var userMessage by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf<Pair<Boolean, String>>(
            Pair(false, if (isAr) welcomeConfig.assistantGreetingAr else welcomeConfig.assistantGreetingEn)
        )
    }
    var isGenerating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Futuristic Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAr) "المساعد الذكي لدليلي اليمني" else "Dalili AI Smart Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val keyConfigured = com.yemenservices.app.BuildConfig.GEMINI_API_KEY.isNotBlank() && com.yemenservices.app.BuildConfig.GEMINI_API_KEY != "GEMINI_API_KEY_DEFAULT_VALUE"
                    val statusText = if (keyConfigured) {
                        if (isAr) "متصل بـ Gemini 2.5-Flash API 🟢" else "Online Gemini 2.5-Flash API 🟢"
                    } else {
                        if (isAr) "وضع الخبير المحلي (أوفلاين) ⚡" else "Offline Expert Mode ⚡"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat messages box
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.first
                val text = msg.second
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 0.dp,
                            bottomEnd = if (isUser) 0.dp else 16.dp
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = if (isAr) TextAlign.Right else TextAlign.Left
                            )
                        }
                    }
                }
            }
        }

        if (isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAr) "جاري التفكير والتوليد من دليلك الذكي..." else "Thinking & Generating...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Input Row containing custom white custom text field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text(if (isAr) "اسأل دليلك عن الأطباء، الأسعار والتمريض والسباكة..." else "Ask about doctors, rates, filters...") },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("assistant_input_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF2C2C2C),
                    unfocusedContainerColor = Color(0xFF2C2C2C)
                ),
                textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (userMessage.isBlank() || isGenerating) return@IconButton
                    val promptText = userMessage
                    chatMessages.add(Pair(true, promptText))
                    userMessage = ""
                    isGenerating = true

                    val coroutineScope = kotlinx.coroutines.MainScope()
                    coroutineScope.launch {
                        val keyConfigured = com.yemenservices.app.BuildConfig.GEMINI_API_KEY.isNotBlank() && com.yemenservices.app.BuildConfig.GEMINI_API_KEY != "GEMINI_API_KEY_DEFAULT_VALUE"
                        var finalReply = ""
                        if (keyConfigured) {
                            try {
                                finalReply = viewModel.getGeminiReply(promptText)
                            } catch (e: Exception) {
                                finalReply = ""
                            }
                        }

                        // Fallback to local expert mode if empty or offline
                        if (finalReply.isBlank() || finalReply.startsWith("Error")) {
                            val norm = promptText.lowercase().trim()
                            val servicesMatches = servicesList.filter { s ->
                                s.nameAr.lowercase().contains(norm) ||
                                s.nameEn.lowercase().contains(norm) ||
                                s.descriptionAr.lowercase().contains(norm) ||
                                s.descriptionEn.lowercase().contains(norm) ||
                                s.category.lowercase().contains(norm) ||
                                s.subCategory.lowercase().contains(norm)
                            }
                            
                            val rep = StringBuilder()
                            if (servicesMatches.isNotEmpty()) {
                                if (isAr) {
                                    rep.append("نتائج محرك البحث المحلي (وضع غير متصل بالإنترنت) ⚡:\n\n")
                                    servicesMatches.take(5).forEach { s ->
                                        val catName = categoriesList.find { it.id == s.category }?.nameAr ?: s.category
                                        val subCatName = subCategoriesList.find { it.id == s.subCategory }?.nameAr ?: s.subCategory
                                        rep.append("• *${s.nameAr}*\n  القسم: $catName | فرع: $subCatName\n  الهاتف للتواصل المباشر: ${s.phoneNumber}\n  التقييم: ⭐ ${s.rating}\n\n")
                                    }
                                } else {
                                    rep.append("Local Offline Match Results ⚡:\n\n")
                                    servicesMatches.take(5).forEach { s ->
                                        val catName = categoriesList.find { it.id == s.category }?.nameEn ?: s.category
                                        val subCatName = subCategoriesList.find { it.id == s.subCategory }?.nameEn ?: s.subCategory
                                        rep.append("• *${s.nameEn}*\n  Category: $catName | SubCat: $subCatName\n  Phone: ${s.phoneNumber}\n  Rating: ⭐ ${s.rating}\n\n")
                                    }
                                }
                            } else {
                                if (isAr) {
                                    rep.append("معذرةً، لم تعثر قاعدة البيانات المحلية (أوفلاين) على دقة محددة لاستعلامك.\n\nيمكنك الاتصال هاتفياً بالدعم أو فحص شبكة الإنترنت للوصول إلى تفويضات Gemini الذكي.")
                                } else {
                                    rep.append("Your local expert mode couldn't find matches.\n\nPlease check network capabilities to connect with the Cloud Gemini API.")
                                }
                            }
                            finalReply = rep.toString()
                        }

                        chatMessages.add(Pair(false, finalReply))
                        isGenerating = false
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                enabled = !isGenerating
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
