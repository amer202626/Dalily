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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dalily.services.data.Comment
import com.dalily.services.data.FirebaseSimulator
import com.dalily.services.data.ServiceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    providerId: String,
    onBackClick: () -> Unit,
    isGuest: Boolean,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val providers by FirebaseSimulator.providers.collectAsState()
    val provider = remember(providers, providerId) { providers.find { it.id == providerId } }
    val systemSettings by FirebaseSimulator.systemSettings.collectAsState()

    // Screen states
    var isReadingMode by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var selectedBookingSlot by remember { mutableStateOf<String?>(null) }
    var inquiryText by remember { mutableStateOf("") }
    var selectedBookingDate by remember { mutableStateOf("اليوم") }

    // Comment states
    var reviewerName by remember { mutableStateOf("") }
    var ratingValue by remember { mutableStateOf(5) }
    var commentText by remember { mutableStateOf("") }

    // Report states
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    // Simulated Chat states
    var userChatMessageText by remember { mutableStateOf("") }
    val chats by FirebaseSimulator.chats.collectAsState()
    val activeChat = remember(chats, providerId) {
        chats.find { it.userId == FirebaseSimulator.currentUserId && it.providerId == providerId }
    }

    // Guest control popup
    var showGuestDialog by remember { mutableStateOf(false) }

    // Offline synchronization representation state
    var lastSyncTime by remember { mutableStateOf("الآن") }
    var isRecordingSync by remember { mutableStateOf(false) }

    // Increment popularity point views on screen opening
    LaunchedEffect(providerId) {
        FirebaseSimulator.incrementViews(context, providerId)
        // Refresh last sync state
        isRecordingSync = true
        delay(400)
        isRecordingSync = false
    }

    if (provider == null) {
        Scaffold { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("عذراً، مقدم الخدمة المطلوب غير موجود أو تم حظره مؤخراً.", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    // Interactive communications launchers (Native intents)
    fun launchPhoneCall(num: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "فشل في تشغيل مدخل الاتصال الهاتفي", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchWhatsApp(num: String, text: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=$num&text=${Uri.encode(text)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "الواتساب غير مثبت على هذا الهاتف", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSocialShare() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "مشاركة مقدم الخدمة: ${provider.name}")
            val promoText = "ابحث وصِل للفنيين المتميزين مع دليل الخدمات اليمني! شاهد ملف الفني (${provider.name}) المميز في تخصص ${provider.category} عبر الرابط:\n${systemSettings.defaultShareLink}?id=${provider.id}"
            putExtra(Intent.EXTRA_TEXT, promoText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة عبر:"))
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick, modifier = Modifier.testTag("detail_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isReadingMode) "وضع القراءة المريح 📖" else provider.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Reading Mode & Options Controllers
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Offline Sync indicator button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isRecordingSync = true
                                    delay(800)
                                    val timeNow = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                                    lastSyncTime = timeNow
                                    isRecordingSync = false
                                    Toast.makeText(context, "تم تحديث ومزامنة البيانات مع خوادم الدليل", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("sync_data_button")
                        ) {
                            if (isRecordingSync) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudQueue, contentDescription = "تحديث المزامنة الاوفلاين", tint = Color.Gray)
                            }
                        }

                        // Reading mode toggle
                        IconButton(
                            onClick = { isReadingMode = !isReadingMode },
                            modifier = Modifier.testTag("reading_mode_button")
                        ) {
                            Icon(
                                imageVector = if (isReadingMode) Icons.Default.MenuBook else Icons.Default.Book,
                                contentDescription = "تبديل القراءة",
                                tint = if (isReadingMode) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }

                        // Social Media Share Action Button
                        IconButton(
                            onClick = { handleSocialShare() },
                            modifier = Modifier.testTag("share_provider_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة الفني", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Subtitle bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مزامنة الاستعراض الأوفلاين: $lastSyncTime",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    if (provider.verified) {
                        Surface(
                            color = Color(0xFF0D9488),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "علامة توثيق 🗸",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IMAGE SLIDER / HEADER (Suppressed in Reading Mode)
            if (!isReadingMode) {
                item {
                    val imagesList = remember(provider) { listOf(provider.imageUrl) + provider.secondaryImages }
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            // Render active sliding image
                            AsyncImage(
                                model = imagesList[selectedImageIndex],
                                contentDescription = "صورة الملف",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Overlay star Badge indicators
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${provider.rating} (${provider.reviewsCount} تقييم)",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Thumbnail Selectors Row
                        if (imagesList.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(imagesList.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (selectedImageIndex == index) 2.dp else 1.dp,
                                                color = if (selectedImageIndex == index) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedImageIndex = index }
                                    ) {
                                        AsyncImage(
                                            model = imagesList[index],
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CORE PROFILE METRICS & DETAILS Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = provider.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (provider.online) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (provider.online) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (provider.online) "متصل بالدليل" else "غير متصل حالياً",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (provider.online) Color(0xFF10B981) else Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = provider.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "شعبية: ${provider.views} نقطة",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "تفاصيل وتخصص الخدمة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = provider.description,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ساعات الدوام الفني للعمل: ${provider.workingHours}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // DIRECT CONTACT BUTTONS STRIP
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "التواصل الفوري وحجز الاتصال:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Launch Phone Call
                        Button(
                            onClick = { launchPhoneCall(provider.phone) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_call_${provider.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, size = 16.dp, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصل هاتفي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Launch WhatsApp directly
                        if (provider.whatsapp.isNotEmpty()) {
                            Button(
                                onClick = { launchWhatsApp(provider.whatsapp, "مرحباً ${provider.name}، شاهدت ملفك في دليل الخدمات وأود الاستفسار عن:") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("btn_wa_${provider.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Message, contentDescription = null, size = 16.dp, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("واتساب سريع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // DYNAMIC CALENDAR BOOKING SYSTEM
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حجز موعد عمل فني مسبق:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        // Toggle dates
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("اليوم", "غداً", "السبت").forEach { d ->
                                val active = selectedBookingDate == d
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedBookingDate = d },
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = d,
                                        fontSize = 9.sp,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid layout of available booking hours
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        provider.availableSlots.forEach { slot ->
                            val isSelected = selectedBookingSlot == slot
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedBookingSlot = if (isSelected) null else slot }
                                    .testTag("booking_slot_$slot"),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = slot,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Booking Confirmation button
                    if (selectedBookingSlot != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (isGuest) {
                                    showGuestDialog = true
                                } else {
                                    scope.launch {
                                        FirebaseSimulator.logAdminAction(context, "حجز موعد ($selectedBookingSlot) للفني ${provider.name} في يوم $selectedBookingDate")
                                        Toast.makeText(context, "تم تسجيل حجز الموعد بنجاح! تم حفظ تذكير الموعد في Firestore وسيتم تنبيه الفني.", Toast.LENGTH_LONG).show()
                                        selectedBookingSlot = null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("confirm_booking_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تأكيد الموعد المحجوز ($selectedBookingSlot) في يوم $selectedBookingDate 📅", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // INTERACTIVE REAL-TIME IN-APP CHAT (Suppressed when disabled by Admin)
            if (!provider.isChatDisabled) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الدردشة الفورية والرسائل المباشرة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Message boards display
                        if (activeChat != null && activeChat.messages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeChat.messages.forEach { msg ->
                                    val isMe = msg.senderId == FirebaseSimulator.currentUserId
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                    ) {
                                        Surface(
                                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(msg.senderName, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                Text(msg.text, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                "لا توجد رسائل بينك وبين هذا الفني لغاية الآن. أرسل استفسارك وسيقوم بالرد الفوري فور نشاطه.",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Message typewriter controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = userChatMessageText,
                                onValueChange = { userChatMessageText = it },
                                placeholder = { Text("اكتب رسالة سريعة...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (isGuest) {
                                        showGuestDialog = true
                                        return@IconButton
                                    }
                                    if (userChatMessageText.trim().isNotEmpty()) {
                                        FirebaseSimulator.createChatAndSendMessage(context, providerId, userChatMessageText.trim())
                                        val mTemp = userChatMessageText
                                        userChatMessageText = ""
                                        // Auto-reply generator simulator script after 1.5s
                                        scope.launch {
                                            delay(1500)
                                            val simulatedReply = "مرحباً بك! تلقيت استفسارك وسأتصل بك قريباً لمعرفة كافة التفاصيل المطلوبة والباشرة بالعمل."
                                            activeChat?.let {
                                                FirebaseSimulator.sendProviderReply(context, it.id, simulatedReply, providerId, provider.name)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    .testTag("send_chat_message_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "أرسل", tint = Color.White)
                            }
                        }
                    }
                }
            } else {
                item {
                    // Chat is disabled globally or for specific provider
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("عذراً! تم تعطيل نظام المحادثات الفورية لهذا الفني حالياً بواسطة المشرف الرئيسي.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // REPORT PROVIDER BUTTON & COMPONENT
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showReportDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("report_provider_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, size = 14.dp, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الإبلاغ عن إساءة أو محتوى غير لائق لمقدم الخدمه", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // REVIEWS & COMMENTS SECTION FEED
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التقييمات والتعليقات (${provider.comments.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Comment lists scrolling
                    if (provider.comments.isNotEmpty()) {
                        val reviewLogs = provider.comments.sortedByDescending { it.pinned }
                        reviewLogs.forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (log.pinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                                        else Color.Transparent, 
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(log.author, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        if (log.pinned) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                                                Text("مثبت من الإدارة 📌", fontSize = 7.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Row {
                                        repeat(log.rating) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                                Text(
                                    log.text,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Divider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    } else {
                        Text("لا توجد مراجعات مكتوبة حالياً. كن أول من يضيف تقييماً لمقدم الخدمة!", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 10.dp))
                    }

                    // Write comments form
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("إضافة مراجعة فنية:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = reviewerName,
                            onValueChange = { reviewerName = it },
                            placeholder = { Text("اسمك الكريم...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        // Rating selectors row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("التقييم بالنجوم: ", fontSize = 11.sp)
                            repeat(5) { i ->
                                val active = ratingValue > i
                                IconButton(onClick = { ratingValue = i + 1 }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = if (active) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (active) Color(0xFFF59E0B) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Comment text block
                        TextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("اكتب مراجعتك أو تفاصيل التجربة بكل أمانة...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Button(
                            onClick = {
                                if (reviewerName.trim().isNotEmpty() && commentText.trim().isNotEmpty()) {
                                    val c = Comment(
                                        id = "C_${System.currentTimeMillis()}",
                                        author = reviewerName.trim(),
                                        text = commentText.trim(),
                                        rating = ratingValue,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    FirebaseSimulator.addComment(context, providerId, c)
                                    reviewerName = ""
                                    commentText = ""
                                    Toast.makeText(context, "نشكرك على مشاركة تجربتك!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("btn_submit_comment"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("أرسل التقييم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Incident issue warning box popup dialog
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("الإبلاغ عن هذا الفني ⚠️", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column {
                        Text("يرجى قراءة معايير النزاهة وكتابة سبب محدد للبلاغ ليتخذ الآدمن الإجراء المناسب (سيصل تنبيه FCM إداري فوري):", fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        TextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            placeholder = { Text("اكتب سبب الإبلاغ بالتفصيل...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (reportReason.trim().isNotEmpty()) {
                                FirebaseSimulator.submitReport(
                                    context, 
                                    providerId, 
                                    reportReason.trim(), 
                                    if (isGuest) "زائر مجهول" else FirebaseSimulator.currentUserName
                                )
                                showReportDialog = false
                                reportReason = ""
                                Toast.makeText(context, "تم رفع التقرير وجدولة الإشعار الفوري للآدمن عبر FCM بنجاح.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("إرسال التنبيه الفوري")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Guest restriction popup
        if (showGuestDialog) {
            AlertDialog(
                onDismissRequest = { showGuestDialog = false },
                title = { Text("ميزة مقيدة للزوار ⚓", fontWeight = FontWeight.Bold) },
                text = { Text("يقتصر التواصل بالرسائل والدردشات وتأكيد حجوزات الأوقات الفنية على المشتركين المسجلين فقط.", fontSize = 12.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            showGuestDialog = false
                            onLoginClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("الذهاب للتسجيل")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGuestDialog = false }) {
                        Text("تخطي")
                    }
                }
            )
        }
    }
}
