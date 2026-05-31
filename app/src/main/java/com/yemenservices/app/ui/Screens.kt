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
import com.yemenservices.app.data.YemenService
import com.yemenservices.app.data.ServiceCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val isAr by viewModel.isArabic.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val services by viewModel.filteredServices.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedService by viewModel.selectedServiceForDetails.collectAsState()
    val isAdminAuth by viewModel.isAdminAuthenticated.collectAsState()

    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var adminUsernameInput by remember { mutableStateOf("") }
    var adminPasswordInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    // Backdoor secret entry states
    var showBackdoorDialog by remember { mutableStateOf(false) }
    var backdoorPassInput by remember { mutableStateOf("") }
    var backdoorError by remember { mutableStateOf(false) }
    var logoTapCount by remember { mutableStateOf(0) }

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
                                adminUsernameInput = ""
                                adminPasswordInput = ""
                                loginError = false
                                showAdminLoginDialog = true
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
                    selected = currentScreen == AppScreen.Home || currentScreen == AppScreen.ServicesList,
                    onClick = {
                        viewModel.selectedCategory.value = null
                        viewModel.currentScreen.value = AppScreen.Home
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(if (isAr) "الرئيسية" else "Home") }
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Favorites,
                    onClick = { viewModel.currentScreen.value = AppScreen.Favorites },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                    label = { Text(if (isAr) "المفضلة" else "Favorites") }
                )
                if (isAdminAuth) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.AdminDashboard,
                        onClick = { viewModel.currentScreen.value = AppScreen.AdminDashboard },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Manage") },
                        label = { Text(if (isAr) "إدارة" else "Manage") }
                    )
                }
                NavigationBarItem(
                    selected = currentScreen == AppScreen.About,
                    onClick = { viewModel.currentScreen.value = AppScreen.About },
                    icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                    label = { Text(if (isAr) "عن التطبيق" else "About") }
                )
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
                AppScreen.Favorites -> FavoritesScreen(viewModel = viewModel, isAr = isAr)
                AppScreen.About -> AboutScreen(viewModel = viewModel, isAr = isAr)
                AppScreen.AdminDashboard -> {
                    if (isAdminAuth) {
                        AdminDashboardScreen(viewModel = viewModel, isAr = isAr)
                    } else {
                        viewModel.currentScreen.value = AppScreen.Home
                    }
                }
            }
        }
    }

    // Standard Admin login dialog
    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            title = {
                Text(
                    text = if (isAr) "تسجيل الدخول للإشراف" else "Admin Login",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isAr) "الرجاء إدخال بيانات حساب المشرف المعتمد للتحكم وعمليات الصيانة مباشرة." else "Please enter authorized admin credentials to proceed.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = adminUsernameInput,
                        onValueChange = {
                            adminUsernameInput = it
                            loginError = false
                        },
                        label = { Text(if (isAr) "اسم المستخدم" else "Username") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = {
                            adminPasswordInput = it
                            loginError = false
                        },
                        label = { Text(if (isAr) "كلمة المرور" else "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = loginError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (loginError) {
                        Text(
                            text = if (isAr) "بيانات الدخول خاطئة، يرجى المحاولة مرة أخرى" else "Incorrect credentials, please try again",
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
                        val success = viewModel.authenticateAdmin(adminUsernameInput, adminPasswordInput)
                        if (success) {
                            showAdminLoginDialog = false
                            adminUsernameInput = ""
                            adminPasswordInput = ""
                            viewModel.currentScreen.value = AppScreen.AdminDashboard
                            Toast.makeText(
                                context,
                                if (isAr) "مرحباً بك في لوحة التحكم!" else "Welcome to Admin Dashboard!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            loginError = true
                        }
                    }
                ) {
                    Text(if (isAr) "دخول" else "Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Secret Backdoor dialog (triggered by 5 taps)
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
    val categories = viewModel.categories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome and Intro header
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
                Text(
                    text = if (isAr) "مرحباً بك في دليلك المحلي" else "Welcome to your local guide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAr) 
                        "تصفح وابحث عن أرقام الطوارئ، المستشفيات، البنوك، النقل والخدمات في اليمن بشكل كامل ومباشر مع ميزة المزامنة السحابية الفورية واللحظية لجميع البيانات."
                    else 
                        "Browse and find emergency contacts, medical facilities, banks, travel resources and services in Yemen, instantly synchronized in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 18.sp,
                    textAlign = if (isAr) TextAlign.Right else TextAlign.Left
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
                    Text(
                        text = category.nameEn,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
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
                    Text(
                        text = category.nameAr,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Right
                    )
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
    val category = viewModel.categories.firstOrNull { it.id == categoryId }

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
    var showForm by remember { mutableStateOf(false) }

    // Form inputs state
    var editingServiceId by remember { mutableStateOf<String?>(null) }
    var serviceNameAr by remember { mutableStateOf("") }
    var serviceNameEn by remember { mutableStateOf("") }
    var serviceCategory by remember { mutableStateOf("emergency") }
    var servicePhoneNumber by remember { mutableStateOf("") }
    var serviceWhatsapp by remember { mutableStateOf("") }
    var serviceAddressAr by remember { mutableStateOf("") }
    var serviceAddressEn by remember { mutableStateOf("") }
    var serviceDescAr by remember { mutableStateOf("") }
    var serviceDescEn by remember { mutableStateOf("") }

    val categories = viewModel.categories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isAr) {
                Text(
                    text = "System Administration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        editingServiceId = null
                        serviceNameAr = ""
                        serviceNameEn = ""
                        serviceCategory = "emergency"
                        servicePhoneNumber = ""
                        serviceWhatsapp = ""
                        serviceAddressAr = ""
                        serviceAddressEn = ""
                        serviceDescAr = ""
                        serviceDescEn = ""
                        showForm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            } else {
                Button(
                    onClick = {
                        editingServiceId = null
                        serviceNameAr = ""
                        serviceNameEn = ""
                        serviceCategory = "emergency"
                        servicePhoneNumber = ""
                        serviceWhatsapp = ""
                        serviceAddressAr = ""
                        serviceAddressEn = ""
                        serviceDescAr = ""
                        serviceDescEn = ""
                        showForm = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة خدمة")
                }
                Text(
                    text = "لوحة إدارة النظام",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (editingServiceId == null) 
                            (if (isAr) "إضافة دليل خدمة جديد" else "Create New Service Listing")
                        else 
                            (if (isAr) "تعديل الخدمة المحددة" else "Edit Service Listing"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    OutlinedTextField(
                        value = serviceNameAr,
                        onValueChange = { serviceNameAr = it },
                        label = { Text(if (isAr) "اسم الخدمة باللغة العربية" else "Arabic Service Name") },
                        modifier = Modifier.fillMaxWidth().testTag("form_name_ar")
                    )

                    OutlinedTextField(
                        value = serviceNameEn,
                        onValueChange = { serviceNameEn = it },
                        label = { Text(if (isAr) "اسم الخدمة باللغة الإنجليزية" else "English Service Name") },
                        modifier = Modifier.fillMaxWidth().testTag("form_name_en")
                    )

                    // Category Selector
                    Text(
                        text = if (isAr) "الفئة المحددة:" else "Selected Category:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = serviceCategory == cat.id
                            Button(
                                onClick = { serviceCategory = cat.id },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    contentColor = if (isSelected) Color.White else Color.Black
                                ),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isAr) cat.nameAr.take(5) else cat.id.take(5), fontSize = 10.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = servicePhoneNumber,
                        onValueChange = { servicePhoneNumber = it },
                        label = { Text(if (isAr) "رقم الهاتف والاتصال" else "Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("form_phone")
                    )

                    OutlinedTextField(
                        value = serviceWhatsapp,
                        onValueChange = { serviceWhatsapp = it },
                        label = { Text(if (isAr) "رقم الواتساب (اختياري)" else "WhatsApp Number (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serviceAddressAr,
                        onValueChange = { serviceAddressAr = it },
                        label = { Text(if (isAr) "العنوان بالعربية" else "Arabic Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serviceAddressEn,
                        onValueChange = { serviceAddressEn = it },
                        label = { Text(if (isAr) "العنوان بالإنجليزية" else "English Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serviceDescAr,
                        onValueChange = { serviceDescAr = it },
                        label = { Text(if (isAr) "الوصف بالعربية" else "Arabic Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = serviceDescEn,
                        onValueChange = { serviceDescEn = it },
                        label = { Text(if (isAr) "الوصف بالإنجليزية" else "English Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (serviceNameAr.isBlank() || servicePhoneNumber.isBlank()) {
                                    return@Button
                                }
                                viewModel.saveService(
                                    id = editingServiceId,
                                    nameAr = serviceNameAr,
                                    nameEn = serviceNameEn,
                                    category = serviceCategory,
                                    phone = servicePhoneNumber,
                                    whatsapp = serviceWhatsapp,
                                    addressAr = serviceAddressAr,
                                    addressEn = serviceAddressEn,
                                    descriptionAr = serviceDescAr,
                                    descriptionEn = serviceDescEn
                                )
                                showForm = false
                            },
                            modifier = Modifier.weight(1f).testTag("form_submit_btn")
                        ) {
                            Text(if (isAr) "حفظ والتسجيل" else "Save & Commit")
                        }

                        TextButton(
                            onClick = { showForm = false },
                            modifier = Modifier.weight(0.5f)
                        ) {
                            Text(if (isAr) "إلغاء" else "Cancel")
                        }
                    }
                }
            }
        }

        // List of all services in Admin control
        Text(
            text = if (isAr) "جميع الخدمات المدرجة (${services.size})" else "All listings (${services.size})",
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
                            Text(
                                text = if (isAr) service.nameAr else service.nameEn,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${service.category} | ${service.phoneNumber}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(
                            onClick = {
                                editingServiceId = service.id
                                serviceNameAr = service.nameAr
                                serviceNameEn = service.nameEn
                                serviceCategory = service.category
                                servicePhoneNumber = service.phoneNumber
                                serviceWhatsapp = service.whatsappNumber
                                serviceAddressAr = service.addressAr
                                serviceAddressEn = service.addressEn
                                serviceDescAr = service.descriptionAr
                                serviceDescEn = service.descriptionEn
                                showForm = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                viewModel.deleteService(service.id)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
