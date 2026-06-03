package com.dalily.services.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dalily.services.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Subs to Firebase Sim data states
    val providers by FirebaseSimulator.providers.collectAsState()
    val banners by FirebaseSimulator.banners.collectAsState()
    val fcmChannels by FirebaseSimulator.fcmChannels.collectAsState()
    val reports by FirebaseSimulator.reports.collectAsState()
    val adminLogs by FirebaseSimulator.adminLogs.collectAsState()
    val whitelist by FirebaseSimulator.whitelist.collectAsState()
    val widgets by FirebaseSimulator.widgets.collectAsState()
    val notifications by FirebaseSimulator.notifications.collectAsState()
    val systemSettings by FirebaseSimulator.systemSettings.collectAsState()
    val blockedProviders by FirebaseSimulator.blockedProviders.collectAsState()
    val blockedUsers by FirebaseSimulator.blockedUsers.collectAsState()

    // 2FA Admin password gates
    var isAuthLocked by remember { mutableStateOf(systemSettings.twoFactorEnabled) }
    var inputOtpCode by remember { mutableStateOf("") }
    var input2fSecret by remember { mutableStateOf(systemSettings.twoFactorSecret) }

    // Forms temp states
    var newBannerImgUrl by remember { mutableStateOf("") }
    var newBannerRedirectUrl by remember { mutableStateOf("") }
    var newBannerDisplayDuration by remember { mutableStateOf("5") }
    var newBannerSize by remember { mutableStateOf("Medium") } // Small, Medium, Large
    var newBannerType by remember { mutableStateOf("Featured") } // Promo, Featured, Welcome

    var newDevName by remember { mutableStateOf("") }
    var newDevIp by remember { mutableStateOf("") }

    var blacklistUserId by remember { mutableStateOf("") }
    var blacklistProviderId by remember { mutableStateOf("") }

    // Scheduled notifications trigger builder
    var schedTitle by remember { mutableStateOf("") }
    var schedBody by remember { mutableStateOf("") }
    var schedTarget by remember { mutableStateOf("الكافة") } // الكافة, مقدمي الخدمات, المستخدمين
    var schedTime by remember { mutableStateOf("10:00 AM") }

    // Widget customizer states (rearrange and manage)
    val orderedWidgets = remember(widgets) {
        widgets.filter { it.isEnabled }.sortedBy { it.order }
    }

    // Function to reorder widgets (Simulated drag and drop up/down displacement)
    fun moveWidgetOrder(id: String, direction: Int) {
        val currentList = widgets.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index == -1) return
        val targetIndex = index + direction
        if (targetIndex >= 0 && targetIndex < currentList.size) {
            // Swap ordering indexes
            val tempOrder = currentList[index].order
            currentList[index] = currentList[index].copy(order = currentList[targetIndex].order)
            currentList[targetIndex] = currentList[targetIndex].copy(order = tempOrder)
            FirebaseSimulator.updateWidgetsState(context, currentList)
        }
    }

    // Export Reports database to formatted CSV file directly saved on Android public documents folder
    fun exportReportsToCsv() {
        try {
            val sb = java.lang.StringBuilder()
            sb.append("ID,ProviderID,ProviderName,Reporter,Reason,Timestamp,Status\n")
            reports.forEach { r ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(r.timestamp))
                sb.append("${r.id},${r.providerId},\"${r.providerName}\",\"${r.reporterName}\",\"${r.reason}\",\"$dateStr\",${if (r.isPending) "Pending" else "Resolved"}\n")
            }

            val filename = "dalily_reports_audit_${System.currentTimeMillis()}.csv"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray())
            }

            FirebaseSimulator.logAdminAction(context, "تصدير البلاغات المعلقة والمكتملة لملف إكسل CSV باسم: $filename")
            Toast.makeText(context, "تم تصدير التقرير الفني بنجاح لملف CSV وحفظه في مجلد التنزيلات:\n$filename", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تصدير الملف: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (isAuthLocked) {
        // Authenticator lock screen UI
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, size = 48.dp, tint = MaterialTheme.colorScheme.primary)
                        Text("التحقق بخطوتين (2FA) المشرف 🔑", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "الرجاء مراجعة تطبيق Google Authenticator التوثيقي وكتابة الرمز الرقمي المولد المكون من 6 أرقام لتسهيل الدخول المجهول للوحة التحكم رئيسية:",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "سري للغاية (Authenticator Seed):\n$input2fSecret",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(6.dp)
                        )

                        TextField(
                            value = inputOtpCode,
                            onValueChange = { inputOtpCode = it },
                            placeholder = { Text("مثال: 123456", textAlign = TextAlign.Center) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("2fa_otp_input")
                                .clip(RoundedCornerShape(10.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        Button(
                            onClick = {
                                // Simulate authenticator checksum validation logic
                                if (inputOtpCode.length == 6) {
                                    isAuthLocked = false
                                    Toast.makeText(context, "تم التحقق الفولاذي الأمني بنجاح! مرحباً بالآدمن.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "الرمز المدخل غير مطابق أو منتهي الصلاحية مسبقاً", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("2fa_submit_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("التحقق وجل الولوج الفوري 🔓", fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = onBackClick) {
                            Text("رجوع للرئيسية كضيف")
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick, modifier = Modifier.testTag("admin_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "لوحة تحكم المشرفين الكاملة 👑",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Transparent)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General metrics tracker banner
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الخدمات المسجلة", fontSize = 10.sp, color = Color.Gray)
                            Text("${providers.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("مستند البلاغات", fontSize = 10.sp, color = Color.Gray)
                            Text("${reports.filter { it.isPending }.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Red)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إعلانات نشطة", fontSize = 10.sp, color = Color.Gray)
                            Text("${banners.filter { it.isActive }.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            // LOOP DYNAMIC DRAG-AND-DROP REORDERABLE WIDGETS LAYOUT
            orderedWidgets.forEach { widget ->
                item(key = widget.id) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("widget_wrapper_${widget.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Section header with ordering controllers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DragHandle, contentDescription = "سحب وإفلات", size = 16.dp, tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = widget.titleAr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Interactive Re-shippers order buttons and deletion controls
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { moveWidgetOrder(widget.id, -1) },
                                        modifier = Modifier.size(24.dp).testTag("btn_order_up_${widget.id}")
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "أعلى", size = 14.dp, tint = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { moveWidgetOrder(widget.id, 1) },
                                        modifier = Modifier.size(24.dp).testTag("btn_order_down_${widget.id}")
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "أسفل", size = 14.dp, tint = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            // Soft deletion of widget UI
                                            val current = widgets.map {
                                                if (it.id == widget.id) it.copy(isEnabled = false) else it
                                            }
                                            FirebaseSimulator.updateWidgetsState(context, current)
                                            Toast.makeText(context, "تم إخفاء القسم بنجاح من شريط التحكم", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp).testTag("btn_delete_widget_${widget.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "إلغاء تشغيل", size = 14.dp, tint = Color.Red)
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // RENDER COMPONENT SOURCE LOGIC CONDITIONALLY BY WIDGET ID
                            when (widget.id) {
                                "W_REPORTS" -> PendingReportsWidget(reports, ::exportReportsToCsv)
                                "W_BANNER_MGR" -> BannerManagerWidget(
                                    banners = banners,
                                    imgUrl = newBannerImgUrl,
                                    onImgChange = { newBannerImgUrl = it },
                                    redirectUrl = newBannerRedirectUrl,
                                    onRedirectChange = { newBannerRedirectUrl = it },
                                    duration = newBannerDisplayDuration,
                                    onDurationChange = { newBannerDisplayDuration = it },
                                    size = newBannerSize,
                                    onSizeChange = { newBannerSize = it },
                                    type = newBannerType,
                                    onTypeChange = { newBannerType = it }
                                )
                                "W_FCM_REGISTRY" -> FcmRegistryWidget(fcmChannels)
                                "W_SYSTEM_SETTINGS" -> SystemSettingsWidget(systemSettings, input2fSecret, { input2fSecret = it })
                                "W_ACTION_LOGS" -> ActionLogsWidget(adminLogs)
                                "W_SECURITY_WHITELIST" -> SecurityWhitelistWidget(
                                    whitelist = whitelist,
                                    devName = newDevName,
                                    onDevNameChange = { newDevName = it },
                                    devIp = newDevIp,
                                    onDevIpChange = { newDevIp = it }
                                )
                                "W_USER_BLOCKLIST" -> UserBlocklistWidget(
                                    providersList = providers,
                                    blockedUsersList = blockedUsers,
                                    blockedProvidersList = blockedProviders,
                                    inputUsrId = blacklistUserId,
                                    onInputUsrChange = { blacklistUserId = it },
                                    inputPrvId = blacklistProviderId,
                                    onInputPrvChange = { blacklistProviderId = it }
                                )
                                "W_QUICK_AUTO_CAT" -> AutoCategoryWidget()
                                else -> Text("مكون إداري مخصص")
                            }
                        }
                    }
                }
            }

            // Hidden Widgets activator drawer toolbar
            val hiddenWidgets = widgets.filter { !it.isEnabled }
            if (hiddenWidgets.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("مكونات مخفية يمكن إعادة تفعيلها بالصفحة:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            hiddenWidgets.forEach { w ->
                                Button(
                                    onClick = {
                                        val current = widgets.map {
                                            if (it.id == w.id) it.copy(isEnabled = true) else it
                                        }
                                        FirebaseSimulator.updateWidgetsState(context, current)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("activate_widget_${w.id}")
                                ) {
                                    Text("+ تشغيل ${w.titleAr}", fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// --- COMPLEX INNER ADMIN COMPONENTS IMPLS ---

@Composable
fun PendingReportsWidget(
    reports: List<Report>,
    onExportCsv: () -> Unit
) {
    val context = LocalContext.current
    val pending = reports.filter { it.isPending }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("البلاغات النشطة المعلقة المرفوعة (${pending.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Button(
                onClick = onExportCsv,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp).testTag("btn_export_reports")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("تصدير CSV ومستند التنزيلات", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (pending.isNotEmpty()) {
            pending.forEach { r ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pending_report_${r.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الفني: ${r.providerName}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            Text("المبلغ: ${r.reporterName}", fontSize = 9.sp)
                        }
                        Text("سبب الإبلاغ: ${r.reason}", fontSize = 10.sp, modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(r.timestamp))
                            Text("توقيت الاستلام: $timeStr", fontSize = 8.sp, color = Color.Gray)
                            Button(
                                onClick = { FirebaseSimulator.resolveReport(context, r.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.height(28.dp).testTag("resolve_report_${r.id}")
                            ) {
                                Text("إغلاق وحل البلاغ 🗸", fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Text("لا توجد أي بلاغات معلقة حالياً في طابور المراجعة.", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun BannerManagerWidget(
    banners: List<BannerAd>,
    imgUrl: String,
    onImgChange: (String) -> Unit,
    redirectUrl: String,
    onRedirectChange: (String) -> Unit,
    duration: String,
    onDurationChange: (String) -> Unit,
    size: String,
    onSizeChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("إضافة / إدارة لافتات العرض الإشهارية (الإنترنت):", fontWeight = FontWeight.Bold, fontSize = 11.sp)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextField(
                value = imgUrl,
                onValueChange = onImgChange,
                placeholder = { Text("رابط الصورة الفنية للبنر...", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )
            TextField(
                value = redirectUrl,
                onValueChange = onRedirectChange,
                placeholder = { Text("معرف الفني للتحويل المباشر (مثال: PRV_01)...", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextField(
                    value = duration,
                    onValueChange = onDurationChange,
                    placeholder = { Text("مدة الظهور (ثواني)", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )

                Button(
                    onClick = {
                        if (imgUrl.trim().isNotEmpty()) {
                            val dur = duration.toIntOrNull() ?: 5
                            val newB = BannerAd(
                                id = "B_${System.currentTimeMillis()}",
                                imageUrl = imgUrl.trim(),
                                redirectUrl = redirectUrl.trim(),
                                displayDuration = dur,
                                size = size,
                                type = type,
                                isActive = true
                            )
                            FirebaseSimulator.updateBanners(context, banners + newB)
                            onImgChange("")
                            onRedirectChange("")
                            Toast.makeText(context, "تم رفع وجدولة لافتة العرض الإشهارية بنجاح بنظام Firestore", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_submit_banner"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إضافة بنر جديد", fontSize = 10.sp)
                }
            }
        }

        // List extant active banners
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        banners.forEach { b ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("نوع البنر: ${b.type} (حجم ${b.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("ثواني: ${b.displayDuration}ث | توجيه: ${b.redirectUrl}", fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row {
                    Checkbox(
                        checked = b.isActive,
                        onCheckedChange = {
                            val updated = banners.map { x -> if (x.id == b.id) x.copy(isActive = !x.isActive) else x }
                            FirebaseSimulator.updateBanners(context, updated)
                        },
                        modifier = Modifier.testTag("chk_banner_${b.id}")
                    )
                    IconButton(
                        onClick = {
                            FirebaseSimulator.updateBanners(context, banners.filter { x -> x.id != b.id })
                        },
                        modifier = Modifier.testTag("del_banner_${b.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", size = 16.dp, tint = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun FcmRegistryWidget(
    channels: List<FcmChannel>
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("التحكم قنوات الإشعارات الفورية (FCM) لكل حدث:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        channels.forEach { ch ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ch.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(ch.description, fontSize = 9.sp, color = Color.Gray)
                }
                Switch(
                    checked = ch.isEnabled,
                    onCheckedChange = { FirebaseSimulator.toggleFcmChannel(context, ch.id) },
                    modifier = Modifier.scale(0.85f).testTag("fcm_switch_${ch.key}")
                )
            }
        }
    }
}

@Composable
fun SystemSettingsWidget(
    settings: AppSystemSettings,
    twoFactorSecret: String,
    onSecretChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("إعدادات وتعديلات لوضع توفير البيانات ومحيط البحث والـ 2FA:", fontWeight = FontWeight.Bold, fontSize = 11.sp)

        // Maintenance Mode System Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("وضعية الصيانة الشاملة للشبكة:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text("يقوم بإغلاق حجز الخدمات مؤقتاً وعرض لوحة صيانة للزبائن", fontSize = 8.sp, color = Color.Gray)
            }
            Switch(
                checked = settings.maintenanceMode,
                onCheckedChange = {
                    FirebaseSimulator.updateSystemSettings(context, settings.copy(maintenanceMode = it))
                },
                modifier = Modifier.testTag("maintenance_mode_switch")
            )
        }

        // Data Saving Mode Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("تفعيل وضع حد توفير البيانات (Data Saving):", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text("يقلل مستويات جودة الصور ويمنع مزامنة الباكجراوند التلقائي", fontSize = 8.sp, color = Color.Gray)
            }
            Switch(
                checked = settings.dataSavingMode,
                onCheckedChange = {
                    FirebaseSimulator.updateSystemSettings(context, settings.copy(dataSavingMode = it))
                },
                modifier = Modifier.testTag("data_saving_mode_switch")
            )
        }

        // Two-Factor Authentication Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("تفعيل التحقق الثنائي بخطوتين (2FA):", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                Text("يطلب فحص رمز المرور في Google Authenticator للدخول", fontSize = 8.sp, color = Color.Gray)
            }
            Switch(
                checked = settings.twoFactorEnabled,
                onCheckedChange = {
                    FirebaseSimulator.updateSystemSettings(context, settings.copy(twoFactorEnabled = it, twoFactorSecret = twoFactorSecret))
                },
                modifier = Modifier.testTag("2fa_toggle_switch")
            )
        }
    }
}

@Composable
fun ActionLogsWidget(logs: List<AdminActionLog>) {
    Column {
        Text("سجل النشاط الفني للمشرف الرئيسي ومسارات التدقيق المالي والتوثيق (Activity Log):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            logs.take(20).forEach { log ->
                val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(log.adminName, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                        Text(timeStr, fontSize = 8.sp, color = Color.Gray)
                    }
                    Text(log.action, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp), lineHeight = 14.sp)
                    Divider(modifier = Modifier.padding(top = 4.dp), color = Color.Gray.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
fun SecurityWhitelistWidget(
    whitelist: List<DeviceWhitelistEntry>,
    devName: String,
    onDevNameChange: (String) -> Unit,
    devIp: String,
    onDevIpChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("سجل الأجهزة ومواقع الـ IP المعتمدة إدارياً (Whitelist):", fontWeight = FontWeight.Bold, fontSize = 11.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextField(
                value = devName,
                onValueChange = onDevNameChange,
                placeholder = { Text("اسم الجهاز (مثال: Macbook)...", fontSize = 10.sp) },
                modifier = Modifier.weight(1.5f).height(40.dp)
            )
            TextField(
                value = devIp,
                onValueChange = onDevIpChange,
                placeholder = { Text("أي بي IP (مثال: 192.168.1.1)...", fontSize = 10.sp) },
                modifier = Modifier.weight(1.5f).height(40.dp)
            )
            Button(
                onClick = {
                    if (devName.trim().isNotEmpty() && devIp.trim().isNotEmpty()) {
                        FirebaseSimulator.addDeviceToWhitelist(context, devName.trim(), devIp.trim())
                        onDevNameChange("")
                        onDevIpChange("")
                    }
                },
                modifier = Modifier.testTag("btn_add_device"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ إضافة", fontSize = 9.sp)
            }
        }

        // whitelist display
        whitelist.forEach { dev ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${dev.deviceName} (${dev.ipAddress})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { FirebaseSimulator.removeDeviceFromWhitelist(context, dev.id) },
                    modifier = Modifier.testTag("del_whitelist_${dev.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "إزالة", size = 14.dp, tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun UserBlocklistWidget(
    providersList: List<ServiceProvider>,
    blockedUsersList: Set<String>,
    blockedProvidersList: Set<String>,
    inputUsrId: String,
    onInputUsrChange: (String) -> Unit,
    inputPrvId: String,
    onInputPrvChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("إدارة الحظر وحجب المستفيدين ومقدمي الخدمات فورا (Block List):", fontWeight = FontWeight.Bold, fontSize = 11.sp)

        // Block Users Panel
        Column {
            Text("حظر معرف مستخدم زبون:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextField(
                    value = inputUsrId,
                    onValueChange = onInputUsrChange,
                    placeholder = { Text("معرف الزبون (مثال: USR_02)", fontSize = 10.sp) },
                    modifier = Modifier.weight(2f).height(40.dp)
                )
                Button(
                    onClick = {
                        if (inputUsrId.trim().isNotEmpty()) {
                            FirebaseSimulator.blockUser(context, inputUsrId.trim())
                            onInputUsrChange("")
                        }
                    },
                    modifier = Modifier.testTag("btn_block_user")
                ) {
                    Text("حظر فوري", fontSize = 9.sp)
                }
            }
            if (blockedUsersList.isNotEmpty()) {
                Text("المعرفات المقيدة: ${blockedUsersList.joinToString()}", fontSize = 9.sp, color = Color.Red)
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

        // Block Providers Panel
        Column {
            Text("إيقاف وحظر حساب مقدم خدمة (فني):", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextField(
                    value = inputPrvId,
                    onValueChange = onInputPrvChange,
                    placeholder = { Text("معرف الفني (مثال: PRV_01)", fontSize = 10.sp) },
                    modifier = Modifier.weight(2f).height(40.dp)
                )
                Button(
                    onClick = {
                        if (inputPrvId.trim().isNotEmpty()) {
                            FirebaseSimulator.blockProvider(context, inputPrvId.trim())
                            onInputPrvChange("")
                        }
                    },
                    modifier = Modifier.testTag("btn_block_provider")
                ) {
                    Text("حظر وإخفاء", fontSize = 9.sp)
                }
            }
            if (blockedProvidersList.isNotEmpty()) {
                Text("المعرفات المحجوبة والنشطة للحجب: ${blockedProvidersList.joinToString()}", fontSize = 9.sp, color = Color.Red)
            }
        }
    }
}

@Composable
fun AutoCategoryWidget() {
    val context = LocalContext.current
    var isAnalyzing by remember { mutableStateOf(false) }
    var detectedCount by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    Column {
        Text("محرر الجرد والتصنيف التلقائي للخدمات (Auto-Categorize Classifier):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("يقوم بتصفح كافة خدمات الفنيين، ويمسح وصفهم بالغة العربية ومفاتيحها ويوزعهم على الثمانية تصانيف الرئيسية المعتمدة دون تدخل برمجي يدوي.", fontSize = 9.sp, color = Color.Gray, lineHeight = 13.sp)

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    isAnalyzing = true
                    delay(1200)
                    detectedCount = FirebaseSimulator.runAutoCategorizeAnalysis(context)
                    isAnalyzing = false
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("btn_run_auto_cat"),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("يجري المسح وإظهار ترشيحات لـ Firestore...", fontSize = 11.sp)
            } else {
                Text("تشغيل خوارزمية جرد وتصحيح التصنيفات 🤖", fontSize = 11.sp)
            }
        }

        detectedCount?.let { count ->
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "التحليل الفني: تم فحص قاعدة البيانات وتصحيح تصنيفات لعدد ($count) من الفنيين بشكل سليم.",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Custom flowRow mapping fallback
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        modifier = modifier,
        content = { content() }
    )
}
