package com.dalily.services.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import java.io.File

object FirebaseSimulator {
    private const val TAG = "FirebaseSimulator"
    private const val FILE_NAME = "dalily_firebase_db.json"

    // Default predefined systems
    private var isInitialized = false

    // Raw database state
    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers: StateFlow<List<ServiceProvider>> = _providers.asStateFlow()

    private val _banners = MutableStateFlow<List<BannerAd>>(emptyList())
    val banners: StateFlow<List<BannerAd>> = _banners.asStateFlow()

    private val _fcmChannels = MutableStateFlow<List<FcmChannel>>(emptyList())
    val fcmChannels: StateFlow<List<FcmChannel>> = _fcmChannels.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _adminLogs = MutableStateFlow<List<AdminActionLog>>(emptyList())
    val adminLogs: StateFlow<List<AdminActionLog>> = _adminLogs.asStateFlow()

    private val _whitelist = MutableStateFlow<List<DeviceWhitelistEntry>>(emptyList())
    val whitelist: StateFlow<List<DeviceWhitelistEntry>> = _whitelist.asStateFlow()

    private val _widgets = MutableStateFlow<List<DashboardWidget>>(emptyList())
    val widgets: StateFlow<List<DashboardWidget>> = _widgets.asStateFlow()

    private val _notifications = MutableStateFlow<List<ScheduledNotification>>(emptyList())
    val notifications: StateFlow<List<ScheduledNotification>> = _notifications.asStateFlow()

    private val _systemSettings = MutableStateFlow(AppSystemSettings())
    val systemSettings: StateFlow<AppSystemSettings> = _systemSettings.asStateFlow()

    // Blocklists
    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    private val _blockedProviders = MutableStateFlow<Set<String>>(emptySet())
    val blockedProviders: StateFlow<Set<String>> = _blockedProviders.asStateFlow()

    // Prepackaged Categories & Available Icons
    val categories = listOf(
        Pair("S_PLUMBING", "سباكة وصيانة"),
        Pair("S_ELECTRICITY", "كهرباء وصيانة"),
        Pair("S_CLINIC", "عيادات وصحة"),
        Pair("S_AC", "تكييف وتبريد"),
        Pair("S_LOGISTICS", "شحن وتوصيل"),
        Pair("S_TUTORING", "تدريس وتعليم"),
        Pair("S_MAINTENANCE", "صيانة سيارات"),
        Pair("S_CHEF", "مأكولات ومطابخ")
    )

    // Current User Session Sim (Simulated local auth state)
    var currentUserId = "USR_99"
    var currentUserName = "محمد اليماني"
    var currentUserIsAdmin = false

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        val dbFile = File(context.filesDir, FILE_NAME)
        if (dbFile.exists()) {
            try {
                val jsonString = dbFile.readText()
                val wrapper = jsonHelper.decodeFromString<DbWrapper>(jsonString)
                _providers.value = wrapper.providers
                _banners.value = wrapper.banners
                _fcmChannels.value = wrapper.fcmChannels
                _reports.value = wrapper.reports
                _chats.value = wrapper.chats
                _adminLogs.value = wrapper.adminLogs
                _whitelist.value = wrapper.whitelist
                _widgets.value = wrapper.widgets
                _notifications.value = wrapper.notifications
                _systemSettings.value = wrapper.systemSettings
                _blockedUsers.value = wrapper.blockedUsers.toSet()
                _blockedProviders.value = wrapper.blockedProviders.toSet()
                isInitialized = true
                Log.d(TAG, "Successfully restored state from storage. Providers loaded: ${_providers.value.size}")
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading database state, formatting fallback...", e)
            }
        }
        // Build fallback starting state
        buildInitialState()
        saveState(context)
        isInitialized = true
    }

    private fun saveState(context: Context) {
        try {
            val wrapper = DbWrapper(
                providers = _providers.value,
                banners = _banners.value,
                fcmChannels = _fcmChannels.value,
                reports = _reports.value,
                chats = _chats.value,
                adminLogs = _adminLogs.value,
                whitelist = _whitelist.value,
                widgets = _widgets.value,
                notifications = _notifications.value,
                systemSettings = _systemSettings.value,
                blockedUsers = _blockedUsers.value.toList(),
                blockedProviders = _blockedProviders.value.toList()
            )
            val jsonString = jsonHelper.encodeToString(wrapper)
            File(context.filesDir, FILE_NAME).writeText(jsonString)
            Log.d(TAG, "Successfully saved database state.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save state in simulator", e)
        }
    }

    private fun buildInitialState() {
        // Sample Providers in Sana'a/Yemen
        _providers.value = listOf(
            ServiceProvider(
                id = "PRV_01",
                name = "السباك السريع - طه الوجيه",
                category = "سباكة وصيانة",
                subcategory = "تسليك مجاري وتركيب خلاطات",
                description = "خبرة أكثر من ١٥ عاماً في حلول السباكة وصيانة الشبكات ومستلزمات المطابخ والحمامات في العاصمة صنعاء. أسعارنا تنافسية ونلتزم بالمواعيد المحددة.",
                phone = "771002003",
                whatsapp = "967771002003",
                rating = 4.8f,
                reviewsCount = 42,
                imageUrl = "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=500",
                secondaryImages = listOf(
                    "https://images.unsplash.com/photo-1542013936693-8848e5740a7a?w=500",
                    "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=500"
                ),
                verified = true,
                online = true,
                tags = listOf("خدمة سريعة", "أسعار اقتصادية", "يدعم الضمان"),
                workingHours = "08:00 AM - 09:00 PM",
                latitude = 15.3522,
                longitude = 44.2011,
                comments = listOf(
                    Comment(id = "C_01", author = "راشد الصنعاني", text = "خدمة ممتازة وسريعة، قام بصيانة خلاط المطبخ في نصف ساعة.", rating = 5, pinned = true)
                )
            ),
            ServiceProvider(
                id = "PRV_02",
                name = "كهربائي فني - عمار النظاري",
                category = "كهرباء وصيانة",
                subcategory = "تركيب واصلاح منظومات الطاقة الشمسية",
                description = "فني كهرباء منازل ومنظومات ذكية، متخصص في تركيب وصيانة الألواح والبطاريات للطاقة الشمسية وإصلاح التوصيلات المعقدة.",
                phone = "733004005",
                whatsapp = "967733004005",
                rating = 4.9f,
                reviewsCount = 37,
                imageUrl = "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500",
                secondaryImages = listOf(
                    "https://images.unsplash.com/photo-1563770660941-20978e870e26?w=500"
                ),
                verified = true,
                online = false,
                tags = listOf("دقة متناهية", "منظومات ذكية", "يدعم الضمان"),
                workingHours = "07:30 AM - 10:00 PM",
                latitude = 15.3615,
                longitude = 44.2155,
                comments = emptyList()
            ),
            ServiceProvider(
                id = "PRV_03",
                name = "سوبر تكييف - صيانة الهدى",
                category = "تكييف وتبريد",
                subcategory = "صيانة وتعبئة فريون",
                description = "نحن متخصصون في تركيب وفك وصيانة جميع أنواع مكيفات الاسبليت والنافذة. تعبئة فريون أمريكي أصلي وتنظيف شامل للوحدات الداخلية والخارجية.",
                phone = "711005006",
                whatsapp = "967711005006",
                rating = 4.6f,
                reviewsCount = 18,
                imageUrl = "https://images.unsplash.com/photo-1621905252873-7745502a1438?w=500",
                secondaryImages = emptyList(),
                verified = false,
                online = true,
                tags = listOf("أسعار اقتصادية", "تنظيف شامل"),
                workingHours = "09:00 AM - 08:30 PM",
                latitude = 15.3340,
                longitude = 44.1804,
                comments = emptyList()
            ),
            ServiceProvider(
                id = "PRV_04",
                name = "صيدلية وعيادة الشفاء العائلية",
                category = "عيادات وصحة",
                subcategory = "طب عام واستشارات أولية",
                description = "تقديم خدمات الرعاية الطبية الأولية، تشخيص الأمراض البسيطة، قياس الضغط والسكري، متوجدون على مدار ٢٤ ساعة لخدمتكم.",
                phone = "01200300",
                whatsapp = "",
                rating = 4.5f,
                reviewsCount = 12,
                imageUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500",
                secondaryImages = emptyList(),
                verified = true,
                online = true,
                tags = listOf("مستمر 24 ساعة", "فحص متميز"),
                workingHours = "24 Hours / 7 Days",
                latitude = 15.3488,
                longitude = 44.1955,
                comments = emptyList()
            ),
            ServiceProvider(
                id = "PRV_05",
                name = "السبع للشحن والخدمات اللوجستية",
                category = "شحن وتوصيل",
                subcategory = "نقل أثاث وبضائع",
                description = "نوفر ديينات مخصصة ومحمية لنقل الأثاث والبضائع داخل صنعاء وإلى جميع المحافظات اليمنية. عمال مدربون وقدرة عالية على الحفظ والحماية.",
                phone = "775443322",
                whatsapp = "967775443322",
                rating = 4.7f,
                reviewsCount = 29,
                imageUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=500",
                secondaryImages = emptyList(),
                verified = false,
                online = true,
                tags = listOf("خدمة سريعة", "نقل آمن"),
                workingHours = "08:00 AM - 11:50 PM",
                latitude = 15.3211,
                longitude = 44.2201,
                comments = emptyList()
            )
        )

        // Banner Ads at the top
        _banners.value = listOf(
            BannerAd(
                id = "B_01",
                imageUrl = "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800",
                redirectUrl = "PRV_02", // Open Electrician Ammar directly
                displayDuration = 6,
                size = "Medium",
                type = "Promo",
                isPinned = true,
                isActive = true
            ),
            BannerAd(
                id = "B_02",
                imageUrl = "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800",
                redirectUrl = "PRV_01", // Open Plumber Taha
                displayDuration = 4,
                size = "Large",
                type = "Featured",
                isPinned = false,
                isActive = true
            )
        )

        // Default Admin Dashboard Widgets (Rearrange with drag-and-drop state)
        _widgets.value = listOf(
            DashboardWidget("W_REPORTS", "Pending Reports", "البلاغات المعلقة والمراجعة", true, 0),
            DashboardWidget("W_BANNER_MGR", "Banner Management", "إدارة البنرات الإعلانية", true, 1),
            DashboardWidget("W_WIDGETS_CONFIG", "D&D Dashboard Customizer", "تخصيص لوحة القيادة", true, 2),
            DashboardWidget("W_FCM_REGISTRY", "FCM Event Channels", "إدارة قنوات الإشعارات FCM", true, 3),
            DashboardWidget("W_SYSTEM_SETTINGS", "System Tweaks & Parameters", "إعدادات الأنظمة والقيود", true, 4),
            DashboardWidget("W_ACTION_LOGS", "Super Admin Action audit-trail", "سجل نشاط المشرفين", true, 5),
            DashboardWidget("W_SECURITY_WHITELIST", "IP Device Whitelist", "قائمة الأجهزة المصرحة", true, 6),
            DashboardWidget("W_USER_BLOCKLIST", "Blacklist Management", "إدارة الحظر والقيود", true, 7),
            DashboardWidget("W_QUICK_AUTO_CAT", "Auto-category script launcher", "توزيع التصنيفات تلقائياً", true, 8)
        )

        // System Notification Channels
        _fcmChannels.value = listOf(
            FcmChannel("CH_01", "Join Requests", "requests_channel", "إشعارات طلبات انضمام مقدمي الخدمة الجدد", true),
            FcmChannel("CH_02", "Incident Reports", "reports_channel", "بلاغات المستخدمين عن المحتوى غير الملائم", true),
            FcmChannel("CH_03", "Auto Billings", "billing_channel", "تنبيهات وتجديد الاشتراكات الدورية للفواتير", true),
            FcmChannel("CH_04", "Administrative Action", "security_channel", "إشعارات الإدارة وحسابات المشرفين", true)
        )

        // Prepopulate some analytics whitelist logs
        _whitelist.value = listOf(
            DeviceWhitelistEntry("WL_01", "Macbook Pro Admin (Office)", "192.168.1.100", true),
            DeviceWhitelistEntry("WL_02", "Galaxy S24 Admin (Mobile)", "192.168.1.105", true)
        )

        // Basic admin audit trail starter logs
        _adminLogs.value = listOf(
            AdminActionLog("AL_01", "النظام", "البدء الفعلي وجدولة قاعدة البيانات الافتراضية بنجاح", System.currentTimeMillis() - 86400000),
            AdminActionLog("AL_02", "آدمن رئيسي", "توثيق حساب السباك طه الوجيه وترقية موقعه", System.currentTimeMillis() - 36000000)
        )

        _notifications.value = listOf(
            ScheduledNotification("N_01", "تذكير بالخدمات", "لا تنسى فحص منظومات التكيف استعداداً للصيف!", "الكافة", "2026-06-10 10:00", false),
            ScheduledNotification("N_02", "عرض مميز للشحن", "خصم 20% على خدمات الشحن اللوجستي بين المحافظات", "الكافة", "2026-06-15 14:00", false)
        )
    }

    // --- REPOSITORY INTERACTIVE APIS FOR VIEWS ---

    fun incrementViews(context: Context, providerId: String) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) {
                it.copy(views = it.views + 1)
            } else it
        }
        saveState(context)
    }

    fun addComment(context: Context, providerId: String, comment: Comment) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) {
                val updatedComments = it.comments + comment
                val sumRate = updatedComments.sumOf { c -> c.rating }.toFloat()
                val avgRate = if (updatedComments.isNotEmpty()) sumRate / updatedComments.size else 5.0f
                it.copy(
                    comments = updatedComments,
                    rating = avgRate,
                    reviewsCount = updatedComments.size
                )
            } else it
        }
        saveState(context)
    }

    fun deleteComment(context: Context, providerId: String, commentId: String) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) {
                val updatedComments = it.comments.filter { c -> c.id != commentId }
                val sumRate = updatedComments.sumOf { c -> c.rating }.toFloat()
                val avgRate = if (updatedComments.isNotEmpty()) sumRate / updatedComments.size else 5.0f
                it.copy(
                    comments = updatedComments,
                    rating = avgRate,
                    reviewsCount = updatedComments.size
                )
            } else it
        }
        logAdminAction(context, "حذف تعليق غير لائق من المعرف $commentId لمقدم الخدمة $providerId")
        saveState(context)
    }

    fun pinComment(context: Context, providerId: String, commentId: String) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) {
                val updated = it.comments.map { c ->
                    if (c.id == commentId) c.copy(pinned = !c.pinned) else c
                }
                it.copy(comments = updated)
            } else it
        }
        logAdminAction(context, "تغيير حالة تثبيت التعليق $commentId لمقدم الخدمة $providerId")
        saveState(context)
    }

    fun toggleProviderStatus(context: Context, providerId: String) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) {
                it.copy(online = !it.online, lastLogin = if (!it.online) "الآن" else "منذ ساعة")
            } else it
        }
        saveState(context)
    }

    fun submitReport(context: Context, providerId: String, reason: String, reporter: String = "مستخدم زائر") {
        val targetPrv = _providers.value.find { it.id == providerId }
        val report = Report(
            id = "REP_${System.currentTimeMillis()}",
            providerId = providerId,
            providerName = targetPrv?.name ?: "مقدم خدمة",
            reporterName = reporter,
            reason = reason,
            timestamp = System.currentTimeMillis(),
            isPending = true
        )
        _reports.value = _reports.value + report

        // FCM Trigger Sim: If reports count on the same provider exceeds 3, warn admin immediately
        val reportsForPrv = _reports.value.filter { it.providerId == providerId }.size
        if (reportsForPrv >= 3) {
            triggerSystemFcmWarning(
                context, 
                "تحذير بلاغات متكررة!", 
                "تلقى مقدم الخدمة (${targetPrv?.name}) أكثر من $reportsForPrv بلاغات معلقة. برجاء المراجعة الفورية."
            )
        }

        saveState(context)
    }

    fun resolveReport(context: Context, reportId: String) {
        _reports.value = _reports.value.map {
            if (it.id == reportId) it.copy(isPending = false) else it
        }
        logAdminAction(context, "الموافقة وإغلاق البلاغ $reportId")
        saveState(context)
    }

    fun createChatAndSendMessage(context: Context, providerId: String, text: String) {
        val targetPrv = _providers.value.find { it.id == providerId } ?: return
        val existingChat = _chats.value.find { it.userId == currentUserId && it.providerId == providerId }

        val newMessage = Message(
            id = "MSG_${System.currentTimeMillis()}",
            senderId = currentUserId,
            senderName = currentUserName,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        if (existingChat != null) {
            _chats.value = _chats.value.map {
                if (it.id == existingChat.id) {
                    it.copy(
                        messages = it.messages + newMessage,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else it
            }
        } else {
            val newChat = Chat(
                id = "CHT_${System.currentTimeMillis()}",
                userId = currentUserId,
                userName = currentUserName,
                providerId = providerId,
                providerName = targetPrv.name,
                messages = listOf(newMessage),
                lastUpdated = System.currentTimeMillis()
            )
            _chats.value = _chats.value + newChat
        }
        saveState(context)
    }

    fun sendProviderReply(context: Context, chatId: String, text: String, providerId: String, providerName: String) {
        val newMessage = Message(
            id = "MSG_${System.currentTimeMillis()}",
            senderId = providerId,
            senderName = providerName,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        _chats.value = _chats.value.map {
            if (it.id == chatId) {
                it.copy(
                    messages = it.messages + newMessage,
                    lastUpdated = System.currentTimeMillis()
                )
            } else it
        }
        saveState(context)
    }

    fun toggleProviderChatEnabled(context: Context, providerId: String) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) {
                it.copy(isChatDisabled = !it.isChatDisabled)
            } else it
        }
        val isNowDisabled = _providers.value.find { it.id == providerId }?.isChatDisabled == true
        logAdminAction(context, "${if (isNowDisabled) "تعطيل" else "تفعيل"} المحادثة الفورية لمقدم الخدمة $providerId")
        saveState(context)
    }

    fun toggleVerifiedProvider(context: Context, providerId: String) {
        _providers.value = _providers.value.map {
            if (it.id == providerId) {
                it.copy(verified = !it.verified)
            } else it
        }
        val isNowVerified = _providers.value.find { it.id == providerId }?.verified == true
        logAdminAction(context, "${if (isNowVerified) "منح" else "إلغاء"} شارة التوثيق الحسابية للمعرب $providerId")
        saveState(context)
    }

    fun addNewProvider(context: Context, provider: ServiceProvider) {
        _providers.value = _providers.value + provider
        logAdminAction(context, "إضافة مقدم خدمة جديد يدوياً: ${provider.name}")
        saveState(context)
    }

    fun blockUser(context: Context, userId: String) {
        _blockedUsers.value = _blockedUsers.value + userId
        logAdminAction(context, "تمت إضافة المعرف $userId إلى قائمة حظر المستخدمين")
        saveState(context)
    }

    fun unblockUser(context: Context, userId: String) {
        _blockedUsers.value = _blockedUsers.value - userId
        logAdminAction(context, "تمت إزالة المعرف $userId من قائمة حظر المستخدمين")
        saveState(context)
    }

    fun blockProvider(context: Context, providerId: String) {
        _blockedProviders.value = _blockedProviders.value + providerId
        logAdminAction(context, "تم حظر المعرف $providerId من تقديم الخدمات")
        saveState(context)
    }

    fun unblockProvider(context: Context, providerId: String) {
        _blockedProviders.value = _blockedProviders.value - providerId
        logAdminAction(context, "تم إلغاء حظر مقدم الخدمة $providerId")
        saveState(context)
    }

    // Dynamic Banner Ad API
    fun updateBanners(context: Context, updatedList: List<BannerAd>) {
        _banners.value = updatedList
        logAdminAction(context, "تحديث لافتات العرض الإعلانية وقيم تشغيلها في Firestore")
        saveState(context)
    }

    // Dynamic FCM Event Channels API
    fun toggleFcmChannel(context: Context, channelId: String) {
        _fcmChannels.value = _fcmChannels.value.map {
            if (it.id == channelId) {
                it.copy(isEnabled = !it.isEnabled)
            } else it
        }
        val channelObj = _fcmChannels.value.find { it.id == channelId }
        val statusVal = if (channelObj?.isEnabled == true) "تفعيل" else "تعطيل"
        logAdminAction(context, "تم $statusVal مسار إرسال الرابط الإشعاري FCM: ${channelObj?.name}")
        saveState(context)
    }

    fun logAdminAction(context: Context, message: String) {
        val log = AdminActionLog(
            id = "LOG_${System.currentTimeMillis()}",
            adminName = if (currentUserIsAdmin) "آدمن رئيسي" else "مشرف",
            action = message,
            timestamp = System.currentTimeMillis()
        )
        _adminLogs.value = listOf(log) + _adminLogs.value
        saveState(context)
    }

    fun updateSystemSettings(context: Context, settings: AppSystemSettings) {
        _systemSettings.value = settings
        logAdminAction(context, "حفظ وتعديل البيانات المعيارية وتفضيلات توفير البيانات للنظام")
        saveState(context)
    }

    // Drag-and-drop simulated order updating
    fun updateWidgetsState(context: Context, widgetsList: List<DashboardWidget>) {
        _widgets.value = widgetsList
        saveState(context)
    }

    fun schedulePushNotification(context: Context, notification: ScheduledNotification) {
        _notifications.value = _notifications.value + notification
        logAdminAction(context, "تم بنجاح جدولة إرسال دفع تنبيهي مستهدف للفئة ${notification.targetGroup}")
        saveState(context)
    }

    fun triggerScheduledNotificationNow(context: Context, id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) {
                it.copy(isSent = true)
            } else it
        }
        val notification = _notifications.value.find { it.id == id }
        notification?.let {
            triggerSystemFcmWarning(context, it.title, "استهداف فئة ${it.targetGroup}: ${it.body}")
        }
        saveState(context)
    }

    // Security Whitelist Devices
    fun addDeviceToWhitelist(context: Context, dName: String, ipVal: String) {
        val entry = DeviceWhitelistEntry(
            id = "WL_${System.currentTimeMillis()}",
            deviceName = dName,
            ipAddress = ipVal,
            allowed = true
        )
        _whitelist.value = _whitelist.value + entry
        logAdminAction(context, "إضافة جهاز ($dName) ذو أي بي ($ipVal) لقائمة الأجهزة المسموحة")
        saveState(context)
    }

    fun removeDeviceFromWhitelist(context: Context, id: String) {
        _whitelist.value = _whitelist.value.filter { it.id != id }
        logAdminAction(context, "إلغاء الترخيص الخاص بالجهاز ذو المعرف البارز $id")
        saveState(context)
    }

    // Auto-categorize algorithm (Admin approval/trigger script representation)
    fun runAutoCategorizeAnalysis(context: Context): Int {
        var modifiedCount = 0
        Log.d(TAG, "Starting auto-categorize analysis...")
        _providers.value = _providers.value.map { provider ->
            var detectedCategory = provider.category
            val descLower = provider.description.lowercase()

            // Map keywords to standard predefined category names
            if (detectedCategory.isEmpty() || detectedCategory == "غير متوفر") {
                if (descLower.contains("سباك") || descLower.contains("سباكة") || descLower.contains("انابيب")) {
                    detectedCategory = "سباكة وصيانة"
                } else if (descLower.contains("سلك") || descLower.contains("كهرباء") || descLower.contains("طاقة شمسية")) {
                    detectedCategory = "كهرباء وصيانة"
                } else if (descLower.contains("مكيف") || descLower.contains("تبريد") || descLower.contains("فريون")) {
                    detectedCategory = "تكييف وتبريد"
                } else if (descLower.contains("عيادة") || descLower.contains("صحة") || descLower.contains("طبيب") || descLower.contains("علاج")) {
                    detectedCategory = "عيادات وصحة"
                } else if (descLower.contains("شحن") || descLower.contains("نقل") || descLower.contains("توصيل")) {
                    detectedCategory = "شحن وتوصيل"
                } else if (descLower.contains("درس") || descLower.contains("معلم") || descLower.contains("تعليم")) {
                    detectedCategory = "تدريس وتعليم"
                } else if (descLower.contains("مطبخ") || descLower.contains("طعام") || descLower.contains("اكل") || descLower.contains("طبخ")) {
                    detectedCategory = "مأكولات ومطابخ"
                }
            }

            if (detectedCategory != provider.category) {
                modifiedCount++
                provider.copy(category = detectedCategory)
            } else provider
        }
        if (modifiedCount > 0) {
            logAdminAction(context, "تشغيل خوارزمية جرد وتعديل التصنيفات التلقائي لعدد $modifiedCount من الخدمات")
            saveState(context)
        }
        return modifiedCount
    }

    // --- UTILS FOR BROADCAST / NOTIFICATION ALERTS ---
    private fun triggerSystemFcmWarning(context: Context, title: String, content: String) {
        // Log in action logs as SYSTEM event so admin can see
        Log.d(TAG, "FCM Warning: $title - $content")
        val log = AdminActionLog(
            id = "FCM_ALERT_${System.currentTimeMillis()}",
            adminName = "🔔 تنبيه FCM",
            action = "$title -> $content",
            timestamp = System.currentTimeMillis()
        )
        _adminLogs.value = listOf(log) + _adminLogs.value
        saveState(context)
    }
}

@kotlinx.serialization.Serializable
data class DbWrapper(
    val providers: List<ServiceProvider> = emptyList(),
    val banners: List<BannerAd> = emptyList(),
    val fcmChannels: List<FcmChannel> = emptyList(),
    val reports: List<Report> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val adminLogs: List<AdminActionLog> = emptyList(),
    val whitelist: List<DeviceWhitelistEntry> = emptyList(),
    val widgets: List<DashboardWidget> = emptyList(),
    val notifications: List<ScheduledNotification> = emptyList(),
    val systemSettings: AppSystemSettings = AppSystemSettings(),
    val blockedUsers: List<String> = emptyList(),
    val blockedProviders: List<String> = emptyList()
)
