package com.dalily.services.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
fun DetailScreen(
    navController: NavController,
    providerId: String,
    currentRole: UserRole,
    activeAdminName: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dbState by FirebaseSimulator.dbState.collectAsState()
    
    // Find provider
    val provider = dbState.providers.find { it.id == providerId }

    // Bookmark/Favorites Local Storage ID Tracker
    val sharedPrefs = remember { context.getSharedPreferences("dalily_favs", Context.MODE_PRIVATE) }
    var favoriteIds by remember {
        mutableStateOf(sharedPrefs.getStringSet("ids", emptySet()) ?: emptySet())
    }
    val isBookmarked = favoriteIds.contains(providerId)

    // Log a simple view increase once on entrance
    LaunchedEffect(providerId) {
        if (provider != null) {
            FirebaseSimulator.updateServiceProvider(
                context,
                provider.copy(views = provider.views + 1)
            )
        }
    }

    if (provider == null) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF0B0F19)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("عذراً، لم يتم العثور على مزود الخدمة المطلوبة بمستودعاتنا!", color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("العودة للواجهة")
                    }
                }
            }
        }
        return
    }

    // Reports and complaint modal states
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReasonInput by remember { mutableStateOf("") }
    var reportDetailsInput by remember { mutableStateOf("") }

    // Chat portal messaging input
    var userChatInput by remember { mutableStateOf("") }
    val chatsHistory = dbState.chats.filter { it.providerId == providerId }

    // Interactive Booking Calendar Simulator states
    val availableDays = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
    val availableSlots = listOf("09:00 AM", "11:00 AM", "01:00 PM", "04:00 PM", "06:00 PM", "08:00 PM")
    var selectedDay by remember { mutableStateOf("السبت") }
    var selectedSlot by remember { mutableStateOf("11:00 AM") }

    // User reviews input text fields
    var reviewerFullName by remember { mutableStateOf("") }
    var reviewRatingSelected by remember { mutableStateOf(5) }
    var reviewCommentBody by remember { mutableStateOf("") }

    // Admin direct editing state indicators
    var showAdminEditDialog by remember { mutableStateOf(false) }
    var edName by remember { mutableStateOf(provider.name) }
    var edCategory by remember { mutableStateOf(provider.category) }
    var edPhone by remember { mutableStateOf(provider.phone) }
    var edWhatsapp by remember { mutableStateOf(provider.whatsapp) }
    var edDesc by remember { mutableStateOf(provider.description) }
    var edImage by remember { mutableStateOf(provider.imageUrl) }
    var edAddress by remember { mutableStateOf(provider.address) }
    var edResidenceRegion by remember { mutableStateOf(provider.residenceRegion) }
    var edAvailable by remember { mutableStateOf(provider.isAvailable) }
    var edFeatured by remember { mutableStateOf(provider.isFeatured) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(provider.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Bookmark toggle icon
                    IconButton(
                        onClick = {
                            val newSet = favoriteIds.toMutableSet()
                            if (isBookmarked) newSet.remove(providerId) else newSet.add(providerId)
                            sharedPrefs.edit().putStringSet("ids", newSet).apply()
                            favoriteIds = newSet
                            Toast.makeText(context, if (isBookmarked) "تم الإزالة من المفضلة" else "تم الحفظ بمفضلتك بنجاح! 🤍", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "مفضلة",
                            tint = if (isBookmarked) Color.Red else Color.White
                        )
                    }

                    // Social Share Button (معلومات مقدم الخدمة ورابط تحميل التطبيق)
                    IconButton(
                        onClick = {
                            val shareBody = "مقدم الخدمة المعتمد: ${provider.name}\n" +
                                            "التخصص: ${provider.category}\n" +
                                            "هاتف التواصل: ${provider.phone}\n" +
                                            "مكان العمل: ${provider.address}\n" +
                                            "حمل تطبيق دليل الخدمات الأول باليمن مجاناً من: https://example.com/dalily_app"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة المهني"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                    }

                    // Direct Edit Controls exclusively for elevated ADMIN / OWNER permissions
                    if (currentRole == UserRole.ADMIN || currentRole == UserRole.OWNER) {
                        IconButton(
                            onClick = {
                                edName = provider.name
                                edCategory = provider.category
                                edPhone = provider.phone
                                edWhatsapp = provider.whatsapp
                                edDesc = provider.description
                                edImage = provider.imageUrl
                                edAddress = provider.address
                                edResidenceRegion = provider.residenceRegion
                                edAvailable = provider.isAvailable
                                edFeatured = provider.isFeatured
                                showAdminEditDialog = true
                            },
                            modifier = Modifier.testTag("admin_speed_edit_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل فوري", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = {
                                FirebaseSimulator.deleteServiceProvider(context, provider.id, activeAdminName)
                                Toast.makeText(context, "تم إلغاء قيد هذا الحرفي وحذفه من الدليل بنجاح 🗑️", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف نهائي", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B0F19)),
            contentPadding = PaddingValues(15.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Hero Identity Banner Photo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.height(200.dp)) {
                        val sampleImg = "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&q=80&w=800"
                        AsyncImage(
                            model = if (provider.imageUrl.isEmpty()) sampleImg else provider.imageUrl,
                            contentDescription = provider.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Availability Status Tag overlying image
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (provider.isAvailable) Color(0xFF22C55E) else Color.Red)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (provider.isAvailable) "متاح ومستعد للعمل 🟢" else "مشغول حالياً 🔴",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Profile Details & Specifications Row
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = provider.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (provider.isVerified) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            Color(0xFF22C55E).copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFF22C55E))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("معتمد رسمياً", fontSize = 9.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Text(" ${provider.rating} (${provider.reviewsCount} تقييم)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Text(" شوهد ${provider.views} مرة", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(provider.description, fontSize = 12.sp, color = Color.LightGray)

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color.Gray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Business Metadata Address & working indices
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("عنوان المركز: ${provider.address}", fontSize = 11.sp, color = Color.LightGray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("أوقات العمل المعتادة: ${provider.workHours}", fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }

            // 3. Google Maps Coordinates Pin and Directions Button
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("خارطة وموقع مزود الخدمة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            
                            // Directions Trigger to external Google Maps
                            Button(
                                onClick = {
                                    val directionsIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("google.navigation:q=${provider.lat},${provider.lng}")
                                    )
                                    directionsIntent.apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    try {
                                        context.startActivity(directionsIntent)
                                    } catch (e: Exception) {
                                        // Fallback browser URL map launcher
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${provider.lat},${provider.lng}"))
                                        context.startActivity(webIntent)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الاتجاهات السير بالخريطة", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Coordinate indicator drawing box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                Text("إحداثيات خط العرض: ${provider.lat}, خط الطول: ${provider.lng}", fontSize = 9.sp, color = Color.White)
                                Text("انقر للحصول على المسار والاتجاهات من موقعك فوراً 🚗", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 4. Interactive Appointments Calendar Slots (جدولة المواعيد المباشرة)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("حجز موعد عمل واستشارة فورية 📅", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        
                        // Day Selection Horizontal Row
                        Text("اختر تاريخ اليوم المفضل:", fontSize = 11.sp, color = Color.Gray)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableDays) { day ->
                                FilterChip(
                                    selected = selectedDay == day,
                                    onClick = { selectedDay = day },
                                    label = { Text(day, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Slots Selection Horizontal Row
                        Text("اختر فترة وتوقيت الحضور:", fontSize = 11.sp, color = Color.Gray)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableSlots) { slot ->
                                FilterChip(
                                    selected = selectedSlot == slot,
                                    onClick = { selectedSlot = slot },
                                    label = { Text(slot, fontSize = 10.sp) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "تم جدولة موعدك ليوم ($selectedDay $selectedSlot) بنجاح! تواصل مع المهني لتأكيده.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تأكيد وحجز الموعد الفوري الحالي")
                        }
                    }
                }
            }

            // 5. Actions / Direct communication Links
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            val callUri = "tel:${provider.phone}"
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(callUri)))
                            FirebaseSimulator.recordCallEvent(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اتصال مباشر")
                    }

                    if (provider.whatsapp.isNotEmpty()) {
                        Button(
                            onClick = {
                                val cleanNum = provider.whatsapp.replace(" ", "")
                                val whatsUri = "https://api.whatsapp.com/send?phone=$cleanNum&text=مرحباً، أود الاستفسار عن كشاف خدماتك من تطبيق دليل الخدمات اليمني."
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsUri))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "لم نجد تطبيق واتساب مثبت! الرقم: ${provider.whatsapp}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("واتساب فوري")
                        }
                    }
                }
            }

            // 6. Real-time Customer to Provider Chat Portal (محادثة فورية)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نافذة مراسلة ومحادثة الحرفي الفورية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("مؤمنة 🔒", fontSize = 9.sp, color = Color.Gray)
                        }

                        // Chat Logs
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 130.dp)
                                .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (chatsHistory.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("لا توجد رسائل بينك وبين المهني بعد. أرسل استفسارك بالأسفل!", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                                }
                            } else {
                                chatsHistory.forEach { chat ->
                                    val position = if (chat.isFromUser) Alignment.End else Alignment.Start
                                    val bubbleCol = if (chat.isFromUser) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray
                                    val bubbleTextCol = if (chat.isFromUser) Color.Black else Color.White
                                    
                                    Box(modifier = Modifier.fillMaxWidth().wrapContentWidth(position)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = bubbleCol
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text(chat.senderName + ": " + chat.text, fontSize = 11.sp, color = bubbleTextCol)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Send input msg row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = userChatInput,
                                onValueChange = { userChatInput = it },
                                placeholder = { Text("اكتب رسالتك الفورية للمهني هنا...", fontSize = 11.sp) },
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
                                    if (userChatInput.trim().isNotEmpty()) {
                                        val newMsg = ChatMessage(
                                            id = "chat_${System.currentTimeMillis()}",
                                            providerId = provider.id,
                                            senderName = if (currentRole != UserRole.USER) "مشرف الدليل" else "مواطن",
                                            text = userChatInput.trim(),
                                            isFromUser = true
                                        )
                                        FirebaseSimulator.addChatMessage(context, newMsg)
                                        userChatInput = ""
                                        
                                        // Simulate auto provider reply mock
                                        scope.launch {
                                            delay(1500)
                                            val autoReply = ChatMessage(
                                                id = "chat_${System.currentTimeMillis() + 1}",
                                                providerId = provider.id,
                                                senderName = provider.name.take(15),
                                                text = "مرحباً بك! تلقيت استفسارك وسأتصل بك خلال لحظات لتأكيد العمل والتجهيز.",
                                                isFromUser = false
                                            )
                                            FirebaseSimulator.addChatMessage(context, autoReply)
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

            // 7. Reviews and Ratings list
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("التقييمات وآراء العملاء الموثقة ⭐", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        if (provider.reviewsList.isEmpty()) {
                            Text("كن أول من يقيم هذا المهني باليمن لبناء الثقة!", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                provider.reviewsList.forEach { rev ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text(rev.username, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                                Row {
                                                    repeat(rev.rating) {
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(rev.comment, fontSize = 11.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        // Submit review form
                        Text("أضف تقييمك الخاص:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = reviewerFullName,
                            onValueChange = { reviewerFullName = it },
                            placeholder = { Text("اسمك الكامل") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = reviewCommentBody,
                            onValueChange = { reviewCommentBody = it },
                            placeholder = { Text("اكتب تعليقك ورأيك في جودة الخدمة...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        // Rating stars selection row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("التقييم بالنجوم: ", fontSize = 11.sp, color = Color.Gray)
                            listOf(1, 2, 3, 4, 5).forEach { star ->
                                IconButton(
                                    onClick = { reviewRatingSelected = star },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (star <= reviewRatingSelected) Color(0xFFFFD700) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (reviewerFullName.trim().isEmpty() || reviewCommentBody.trim().isEmpty()) {
                                    Toast.makeText(context, "الرجاء كشط حقول الاسم والتعليق أولاً!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newReview = UserReview(
                                        id = "rev_${System.currentTimeMillis()}",
                                        username = reviewerFullName.trim(),
                                        rating = reviewRatingSelected,
                                        comment = reviewCommentBody.trim()
                                    )
                                    FirebaseSimulator.addProviderReview(context, provider.id, newReview)
                                    Toast.makeText(context, "شكراً لك! تم نشر تقييمك المعتمد فوراً.", Toast.LENGTH_SHORT).show()
                                    reviewerFullName = ""
                                    reviewCommentBody = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("نشر التقييم")
                        }
                    }
                }
            }

            // 8. Safety Report complaint button
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { showReportDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الإبلاغ عن إساءة استخدام أو خدمة خاطئة ⚠️")
                    }
                }
            }
        }
    }

    // A. DIRECT SUBMIT REPORT COMPLAINT DIALOG BOX
    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
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
                    Text("تقديم بلاغ شكوى عن مقدم الخدمة ⚠️", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("سيتم مراجعة البلاغ وحظر أو تعليق مقدم الخدمة فوراً عند ثبوت المخالفة:", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    OutlinedTextField(
                        value = reportReasonInput,
                        onValueChange = { reportReasonInput = it },
                        placeholder = { Text("نوع المخالفة (مثال: أسعار مبالغة، عدم التزام)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = reportDetailsInput,
                        onValueChange = { reportDetailsInput = it },
                        placeholder = { Text("تفاصيل الشكوى والوقوف على مسببات المشكلة...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
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
                                if (reportReasonInput.trim().isEmpty() || reportDetailsInput.trim().isEmpty()) {
                                    Toast.makeText(context, "الرجاء تعبئة حقول البلاغ بالكامل!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val r = Report(
                                        id = "rep_${System.currentTimeMillis()}",
                                        providerId = provider.id,
                                        providerName = provider.name,
                                        reason = reportReasonInput,
                                        details = reportDetailsInput
                                    )
                                    FirebaseSimulator.addReport(context, r)
                                    showReportDialog = false
                                    // Simulated immediate trigger alert on multiple reports
                                    val countReportsForThis = dbState.reports.count { it.providerId == provider.id } + 1
                                    if (countReportsForThis >= 2) {
                                        Toast.makeText(context, "تنبيه للأدمن: تم تسجيل بلاغ ثان عن نفس مزود الخدمة. جاري تفعيل المراجعة الجنائية للتعليق!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "تم إحالة البلاغ لجهة المشرفين للتدقيق بنجاح.", Toast.LENGTH_SHORT).show()
                                    }
                                    reportReasonInput = ""
                                    reportDetailsInput = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("إرسال الشكوى")
                        }

                        OutlinedButton(onClick = { showReportDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
    }

    // B. ADMIN SPEED MODIFY DIALOG (تعديل كامل البيانات والمميزات)
    if (showAdminEditDialog) {
        Dialog(onDismissRequest = { showAdminEditDialog = false }) {
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("تعديل كامل بيانات ومعلومات الصفحة ⚙️", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = edName,
                        onValueChange = { edName = it },
                        placeholder = { Text("الاسم الكامل الحرفي") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edCategory,
                        onValueChange = { edCategory = it },
                        placeholder = { Text("القسم الرئيسي") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edPhone,
                        onValueChange = { edPhone = it },
                        placeholder = { Text("هاتف الاتصال") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edWhatsapp,
                        onValueChange = { edWhatsapp = it },
                        placeholder = { Text("رقم الواتساب") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edDesc,
                        onValueChange = { edDesc = it },
                        placeholder = { Text("الوصف والتفاصيل والمؤهلات") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edImage,
                        onValueChange = { edImage = it },
                        placeholder = { Text("رابط صورة الغلاف") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edAddress,
                        onValueChange = { edAddress = it },
                        placeholder = { Text("عنوان النشاط الحالي") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edResidenceRegion,
                        onValueChange = { edResidenceRegion = it },
                        placeholder = { Text("منطقة السكن") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Switches
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("المهني متاح للعمل لجميع الساعات", fontSize = 11.sp, color = Color.LightGray)
                        Switch(checked = edAvailable, onCheckedChange = { edAvailable = it })
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تثبيت التوصية (موصى به في الأعلى) 🔥", fontSize = 11.sp, color = Color.LightGray)
                        Switch(checked = edFeatured, onCheckedChange = { edFeatured = it })
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val updated = provider.copy(
                                    name = edName,
                                    category = edCategory,
                                    phone = edPhone,
                                    whatsapp = edWhatsapp,
                                    description = edDesc,
                                    imageUrl = edImage,
                                    address = edAddress,
                                    residenceRegion = edResidenceRegion,
                                    isAvailable = edAvailable,
                                    isFeatured = edFeatured
                                )
                                FirebaseSimulator.updateServiceProvider(context, updated, activeAdminName)
                                showAdminEditDialog = false
                                Toast.makeText(context, "تم حفظ كافة تعديلات الأدمن ومزامنتها بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ التغييرات")
                        }

                        OutlinedButton(onClick = { showAdminEditDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("رجوع")
                        }
                    }
                }
            }
        }
    }
}
