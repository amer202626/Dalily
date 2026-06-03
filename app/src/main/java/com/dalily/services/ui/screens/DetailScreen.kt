package com.dalily.services.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.dalily.services.data.ChatMessage
import com.dalily.services.data.FirebaseSimulator
import com.dalily.services.data.Report
import com.dalily.services.data.ServiceProvider

@Composable
fun DetailScreen(
    navController: NavController,
    providerId: String,
    currentRole: UserRole
) {
    val context = LocalContext.current
    val dbState by FirebaseSimulator.dbState.collectAsState()
    
    // Find matching provider
    val provider = dbState.providers.find { it.id == providerId }

    // Increment view count on open
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
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("عذراً، لم يتم العثور على مقدم الخدمة!", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("العودة للرئيسية")
                    }
                }
            }
        }
        return
    }

    // Comment and report states
    var commentText by remember { mutableStateOf("") }
    var reviewerName by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportDetails by remember { mutableStateOf("") }

    // Admin direct editing states
    var showEditProviderDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(provider.name) }
    var editCategory by remember { mutableStateOf(provider.category) }
    var editPhone by remember { mutableStateOf(provider.phone) }
    var editWhatsapp by remember { mutableStateOf(provider.whatsapp) }
    var editDesc by remember { mutableStateOf(provider.description) }
    var editImage by remember { mutableStateOf(provider.imageUrl) }
    var editRating by remember { mutableStateOf(provider.rating.toString()) }
    var editViews by remember { mutableStateOf(provider.views.toString()) }
    var editVerified by remember { mutableStateOf(provider.isVerified) }
    var editAddress by remember { mutableStateOf(provider.address) }

    // Messaging states
    var chatMessageText by remember { mutableStateOf("") }
    val chatsList = dbState.chats.filter { it.providerId == providerId }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(provider.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Edit and delete icons if role is ADMIN or OWNER (اعطاء صلاحيه للتعديل الكامل)
                    if (currentRole == UserRole.ADMIN || currentRole == UserRole.OWNER) {
                        IconButton(
                            onClick = {
                                editName = provider.name
                                editCategory = provider.category
                                editPhone = provider.phone
                                editWhatsapp = provider.whatsapp
                                editDesc = provider.description
                                editImage = provider.imageUrl
                                editRating = provider.rating.toString()
                                editViews = provider.views.toString()
                                editVerified = provider.isVerified
                                editAddress = provider.address
                                showEditProviderDialog = true
                            },
                            modifier = Modifier.testTag("btn_edit_provider")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل مقدم الخدمة", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        IconButton(
                            onClick = {
                                FirebaseSimulator.deleteServiceProvider(context, provider.id)
                                Toast.makeText(context, "تم حذف مقدم الخدمة بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            modifier = Modifier.testTag("btn_delete_provider")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف مقدم الخدمة", tint = Color.Red)
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
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Core Identity header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        val placeholderImage = "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&q=80&w=600"
                        AsyncImage(
                            model = if (provider.imageUrl.isEmpty()) placeholderImage else provider.imageUrl,
                            contentDescription = provider.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = provider.category,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                    Text(" ${provider.rating} (${provider.reviewsCount} تقييم)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = provider.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (provider.isVerified) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "موثوق", tint = Color(0xFF22C55E), modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(provider.description, fontSize = 13.sp, color = Color.Gray, lineHeight = 20.sp)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = "موقع", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(provider.address, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Quick Contact Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${provider.phone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_call_provider"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اتصال فوري", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (provider.whatsapp.isNotEmpty()) {
                        Button(
                            onClick = {
                                val url = "https://api.whatsapp.com/send?phone=${provider.whatsapp}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_whatsapp_provider"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Report button
            item {
                OutlinedButton(
                    onClick = { showReportDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_report_provider"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("أبلغ عن وجود مشكلة أو بيانات خاطئة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Chat Box
            item {
                Text("المحادثة المباشرة الفورية مع المزود", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                        ) {
                            if (chatsList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("ابدأ المحادثة الآن واكتب استفسارك!", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(chatsList) { msg ->
                                        val alignment = if (msg.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
                                        val bubbleColor = if (msg.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Surface(
                                                color = bubbleColor,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.align(alignment)
                                            ) {
                                                Text(msg.text, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = chatMessageText,
                                onValueChange = { chatMessageText = it },
                                placeholder = { Text("اكتب رسالتك لطلب الخدمة...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("chat_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (chatMessageText.isNotEmpty()) {
                                        val msg = ChatMessage(
                                            id = "msg_${System.currentTimeMillis()}",
                                            providerId = providerId,
                                            senderName = "أنا",
                                            text = chatMessageText,
                                            isFromUser = true
                                        )
                                        FirebaseSimulator.addChatMessage(context, msg)
                                        chatMessageText = ""
                                        Toast.makeText(context, "تم إرسال رسالتك لمزود الخدمة!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(50.dp).testTag("btn_send_chat")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "إرسال")
                            }
                        }
                    }
                }
            }

            // Reviews and Comments
            item {
                Text("المراجعات وآراء العملاء", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أضف رأيك كعميل لهذا النشاط", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = reviewerName,
                            onValueChange = { reviewerName = it },
                            placeholder = { Text("اسمك الكريم") },
                            modifier = Modifier.fillMaxWidth().testTag("rev_name_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("تكلم عن جودة العمل، السعر، والالتزام بالوقت...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .testTag("rev_text_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                if (reviewerName.isEmpty() || commentText.isEmpty()) {
                                    Toast.makeText(context, "الرجاء كتابة الاسم والرأي!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "شكراً لإضافة رأيك ومساعدتنا لتقييم النشاط!", Toast.LENGTH_LONG).show()
                                    // Trigger incremental review metadata simulation
                                    val newReviewsCount = provider.reviewsCount + 1
                                    val currentRating = provider.rating
                                    // Add a slight rating bump simulation
                                    val newRating = minOf(5.0, ((currentRating * provider.reviewsCount + 5.0) / newReviewsCount))
                                    FirebaseSimulator.updateServiceProvider(
                                        context,
                                        provider.copy(reviewsCount = newReviewsCount, rating = newRating)
                                    )
                                    reviewerName = ""
                                    commentText = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("btn_submit_review")
                        ) {
                            Text("نشر التقييم")
                        }
                    }
                }
            }
        }
    }

    // Submit Report Dialog
    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("الإبلاغ عن بيانات خاطئة ⚠️", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("سبب الإبلاغ (مثال: هاتف خاطئ، السعر مرتفع)") },
                        modifier = Modifier.fillMaxWidth().testTag("report_reason_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        placeholder = { Text("تفاصيل البلاغ الإضافية ومقترحاتك للتعديل") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (reportReason.isNotEmpty()) {
                                    val report = Report(
                                        id = "rep_${System.currentTimeMillis()}",
                                        providerId = providerId,
                                        providerName = provider.name,
                                        reason = reportReason,
                                        details = reportDetails
                                    )
                                    FirebaseSimulator.addReport(context, report)
                                    showReportDialog = false
                                    reportReason = ""
                                    reportDetails = ""
                                    Toast.makeText(context, "نشكر مساهمتك! تم جدولة البلاغ وبدء مراجعته.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_report_submit")
                        ) {
                            Text("إرسال البلاغ")
                        }
                        OutlinedButton(
                            onClick = { showReportDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // Full Provider Edit (صلاحيات الأدمن الكاملة للمزامنة)
    if (showEditProviderDialog) {
        Dialog(onDismissRequest = { showEditProviderDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("تعديل كامل البيانات (رتبة مشرف) 🛡️", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم النشاط ومزود الخدمة") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_provider_name"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("القسم المعتمد") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("رقم الاتصال") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editWhatsapp,
                        onValueChange = { editWhatsapp = it },
                        label = { Text("رقم الواتساب") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("الوصف والخدمات المقدمة") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editImage,
                        onValueChange = { editImage = it },
                        label = { Text("رابط الصورة الفنية") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("العنوان بالتحديد") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editRating,
                        onValueChange = { editRating = it },
                        label = { Text("التقييم الأصلي للنشاط (0.0 - 5.0)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editViews,
                        onValueChange = { editViews = it },
                        label = { Text("عدد المشاهدات (نقطة الشعبية)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("توثيق مزود الخدمة (علامة الزرقاء):", fontSize = 12.sp)
                        Switch(
                            checked = editVerified,
                            onCheckedChange = { editVerified = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (editName.isEmpty() || editPhone.isEmpty()) {
                                    Toast.makeText(context, "الحقول الأساسية الاسم والهاتف مطلوبة!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val r = editRating.toDoubleOrNull() ?: provider.rating
                                    val v = editViews.toIntOrNull() ?: provider.views
                                    val updatedPro = provider.copy(
                                        name = editName,
                                        category = editCategory,
                                        phone = editPhone,
                                        whatsapp = editWhatsapp,
                                        description = editDesc,
                                        imageUrl = editImage,
                                        rating = r,
                                        views = v,
                                        isVerified = editVerified,
                                        address = editAddress
                                    )
                                    FirebaseSimulator.updateServiceProvider(context, updatedPro)
                                    showEditProviderDialog = false
                                    Toast.makeText(context, "تم تحديث ومزامنة بيانات المزود بنجاح! 📣", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_edit_provider_submit")
                        ) {
                            Text("تزامن البيانات الكلي")
                        }
                        OutlinedButton(
                            onClick = { showEditProviderDialog = false },
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
