package com.dalily.services.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class DbWrapper(
    val providers: List<ServiceProvider> = emptyList(),
    val banners: List<BannerAd> = emptyList(),
    val categories: List<CustomCategory> = emptyList(),
    val admins: List<AdminProfile> = emptyList(),
    val serviceRequests: List<ServiceRequest> = emptyList(),
    val reports: List<Report> = emptyList(),
    val chats: List<ChatMessage> = emptyList(),
    val faqs: List<FAQItem> = emptyList(),
    val logs: List<ActivityLog> = emptyList(),
    val config: AppConfig = AppConfig(),
    val userVisits: Int = 142,
    val callCounts: Int = 54,
    val version: Int = 12
)

object FirebaseSimulator {
    private const val TAG = "FirebaseSimulator"
    private const val LOCAL_FILE_NAME = "dalily_services_db_v2.json"
    
    // Cloud replication bucket for real-time synchronization amongst different devices/emulators
    private const val CLOUD_URL = "https://kvdb.io/KxPcaYJbLwD5M9V9GvW1R/dalily_sync_db_v10"

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val _dbState = MutableStateFlow(DbWrapper())
    val dbState = _dbState.asStateFlow()

    private val _syncingState = MutableStateFlow(false)
    val syncingState = _syncingState.asStateFlow()

    private val _workManagerSyncActive = MutableStateFlow(false)
    val workManagerSyncActive = _workManagerSyncActive.asStateFlow()

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            loadLocalData(context)
            // Trigger automatic initial sync from cloud bucket
            syncWithCloud(context)
        }
    }

    private fun loadLocalData(context: Context) {
        try {
            val file = File(context.filesDir, LOCAL_FILE_NAME)
            if (file.exists()) {
                val jsonText = file.readText()
                val loaded = jsonHelper.decodeFromString<DbWrapper>(jsonText)
                _dbState.value = loaded
                Log.d(TAG, "Local offline-first database loaded. Providers: ${loaded.providers.size}")
            } else {
                val initialDb = getSeedData()
                saveLocalAndPush(context, initialDb)
                Log.d(TAG, "Database seeded with default data.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading offline data: ${e.message}", e)
            _dbState.value = getSeedData()
        }
    }

    fun saveLocalAndPush(context: Context, newDb: DbWrapper) {
        _dbState.value = newDb
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Save locally with 0ms delay inside storage files (Offline-First)
                val file = File(context.filesDir, LOCAL_FILE_NAME)
                val jsonText = jsonHelper.encodeToString(newDb)
                file.writeText(jsonText)
                
                // 2. Trigger asynchronous background syncer simulating WorkManager
                _workManagerSyncActive.value = true
                pushToCloud(jsonText)
                _workManagerSyncActive.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error writing DB offline or pushing: ${e.message}")
                _workManagerSyncActive.value = false
            }
        }
    }

    private fun pushToCloud(jsonText: String) {
        try {
            val url = URL(CLOUD_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Content-Type", "application/json")

            val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
            writer.write(jsonText)
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            Log.d(TAG, "Cloud sync push completed. Status: $responseCode")
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "No connectivity (Offline mode): ${e.message}")
        }
    }

    suspend fun syncWithCloud(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            _syncingState.value = true
            _workManagerSyncActive.value = true
            try {
                val url = URL(CLOUD_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = InputStreamReader(connection.inputStream, "UTF-8")
                    val responseText = reader.readText()
                    reader.close()

                    if (responseText.trim().isNotEmpty()) {
                        val cloudDb = jsonHelper.decodeFromString<DbWrapper>(responseText)
                        
                        // Local merge & upgrade
                        _dbState.value = cloudDb
                        val file = File(context.filesDir, LOCAL_FILE_NAME)
                        file.writeText(responseText)
                        
                        Log.d(TAG, "Synced with remote db successfully. Providers: ${cloudDb.providers.size}")
                        _syncingState.value = false
                        _workManagerSyncActive.value = false
                        connection.disconnect()
                        return@withContext true
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Offline sync check: ${e.message}")
            }
            _syncingState.value = false
            _workManagerSyncActive.value = false
            return@withContext false
        }
    }

    // Increments visitor stats locally & pushes to cloud
    fun recordVisitor(context: Context) {
        val current = _dbState.value
        saveLocalAndPush(context, current.copy(userVisits = current.userVisits + 1))
    }

    fun recordCallEvent(context: Context) {
        val current = _dbState.value
        saveLocalAndPush(context, current.copy(callCounts = current.callCounts + 1))
    }

    // Config controls
    fun updateAppConfig(context: Context, newConfig: AppConfig) {
        val current = _dbState.value
        saveLocalAndPush(context, current.copy(config = newConfig))
    }

    // Providers mechanics
    fun addServiceProvider(context: Context, provider: ServiceProvider, logActor: String? = null) {
        val current = _dbState.value
        val updatedList = current.providers.filter { it.id != provider.id } + provider
        
        var logs = current.logs
        if (logActor != null) {
            logs = logs + ActivityLog(
                id = "log_${System.currentTimeMillis()}",
                supervisorName = logActor,
                action = "إضافة مزود خدمة",
                target = provider.name
            )
        }
        
        saveLocalAndPush(context, current.copy(providers = updatedList, logs = logs))
    }

    fun updateServiceProvider(context: Context, provider: ServiceProvider, logActor: String? = null) {
        addServiceProvider(context, provider, logActor)
    }

    fun deleteServiceProvider(context: Context, id: String, logActor: String? = null) {
        val current = _dbState.value
        val targetName = current.providers.find { it.id == id }?.name ?: id
        val updatedList = current.providers.filter { it.id != id }

        var logs = current.logs
        if (logActor != null) {
            logs = logs + ActivityLog(
                id = "log_${System.currentTimeMillis()}",
                supervisorName = logActor,
                action = "حذف مزود خدمة",
                target = targetName
            )
        }

        saveLocalAndPush(context, current.copy(providers = updatedList, logs = logs))
    }

    // Banner ads modifiers
    fun addBanner(context: Context, banner: BannerAd, logActor: String? = null) {
        val current = _dbState.value
        val updatedList = current.banners.filter { it.id != banner.id } + banner
        
        var logs = current.logs
        if (logActor != null) {
            logs = logs + ActivityLog(
                id = "log_${System.currentTimeMillis()}",
                supervisorName = logActor,
                action = "إضافة لافتة إعلانية ممولة",
                target = banner.title
            )
        }
        saveLocalAndPush(context, current.copy(banners = updatedList, logs = logs))
    }

    fun deleteBanner(context: Context, id: String, logActor: String? = null) {
        val current = _dbState.value
        val title = current.banners.find { it.id == id }?.title ?: id
        val updatedList = current.banners.filter { it.id != id }
        
        var logs = current.logs
        if (logActor != null) {
            logs = logs + ActivityLog(
                id = "log_${System.currentTimeMillis()}",
                supervisorName = logActor,
                action = "حذف لافتة إعلانية",
                target = title
            )
        }
        saveLocalAndPush(context, current.copy(banners = updatedList, logs = logs))
    }

    // Categories modifiers
    fun addCategory(context: Context, category: CustomCategory, logActor: String? = null) {
        val current = _dbState.value
        val updatedList = current.categories.filter { it.id != category.id } + category
        
        var logs = current.logs
        if (logActor != null) {
            logs = logs + ActivityLog(
                id = "log_${System.currentTimeMillis()}",
                supervisorName = logActor,
                action = "إضافة قسم رئيسي",
                target = category.nameAr
            )
        }
        saveLocalAndPush(context, current.copy(categories = updatedList, logs = logs))
    }

    fun deleteCategory(context: Context, id: String, logActor: String? = null) {
        val current = _dbState.value
        val nameAr = current.categories.find { it.id == id }?.nameAr ?: id
        val updatedList = current.categories.filter { it.id != id }
        
        var logs = current.logs
        if (logActor != null) {
            logs = logs + ActivityLog(
                id = "log_${System.currentTimeMillis()}",
                supervisorName = logActor,
                action = "حذف قسم رئيسي",
                target = nameAr
            )
        }
        saveLocalAndPush(context, current.copy(categories = updatedList, logs = logs))
    }

    // Supervisor profiles mechanics
    fun addAdminProfile(context: Context, admin: AdminProfile, logActor: String? = null) {
        val current = _dbState.value
        val updatedList = current.admins.filter { it.username != admin.username } + admin
        
        var logs = current.logs
        if (logActor != null) {
            logs = logs + ActivityLog(
                id = "log_${System.currentTimeMillis()}",
                supervisorName = logActor,
                action = "إضافة أو تعديل مشرف",
                target = admin.username
            )
        }
        saveLocalAndPush(context, current.copy(admins = updatedList, logs = logs))
    }

    fun deleteAdminProfile(context: Context, username: String, logActor: String? = null) {
        val current = _dbState.value
        val updatedList = current.admins.filter { it.username != username }
        
        var logs = current.logs
        if (logActor != null) {
            logs = logs + ActivityLog(
                id = "log_${System.currentTimeMillis()}",
                supervisorName = logActor,
                action = "حذف مشرف من الصلاحيات",
                target = username
            )
        }
        saveLocalAndPush(context, current.copy(admins = updatedList, logs = logs))
    }

    // Service request modifiers (Supervisors approve/reject)
    fun addServiceRequest(context: Context, request: ServiceRequest) {
        val current = _dbState.value
        val updatedList = current.serviceRequests.filter { it.id != request.id } + request
        saveLocalAndPush(context, current.copy(serviceRequests = updatedList))
    }

    fun updateServiceRequestStatus(context: Context, id: String, status: String, supervisorName: String) {
        val current = _dbState.value
        val req = current.serviceRequests.find { id == it.id } ?: return
        
        val updatedRequests = current.serviceRequests.map {
            if (it.id == id) it.copy(status = status) else it
        }

        var newProviders = current.providers
        if (status == "APPROVED") {
            val pId = "p_${System.currentTimeMillis()}"
            val newProvider = ServiceProvider(
                id = pId,
                name = req.providerName,
                category = req.category,
                phone = req.phone,
                whatsapp = req.whatsapp,
                description = req.description,
                address = req.workAddress,
                residenceRegion = req.residenceRegion,
                imageUrl = req.profileImageUrl,
                idCardUrl = req.idCardUrl,
                rating = 4.8,
                reviewsCount = 1,
                isVerified = true,
                lat = req.lat,
                lng = req.lng
            )
            newProviders = newProviders + newProvider
        }

        val logs = current.logs + ActivityLog(
            id = "log_${System.currentTimeMillis()}",
            supervisorName = supervisorName,
            action = if (status == "APPROVED") "قبول وإدراج خدمة" else "رفض طلب خدمة",
            target = req.providerName
        )

        saveLocalAndPush(context, current.copy(
            serviceRequests = updatedRequests,
            providers = newProviders,
            logs = logs
        ))
    }

    // FAQ modifiers
    fun addFAQItem(context: Context, item: FAQItem) {
        val current = _dbState.value
        val updated = current.faqs.filter { it.id != item.id } + item
        saveLocalAndPush(context, current.copy(faqs = updated))
    }

    fun deleteFAQItem(context: Context, id: String) {
        val current = _dbState.value
        val updated = current.faqs.filter { it.id != id }
        saveLocalAndPush(context, current.copy(faqs = updated))
    }

    // Reviews inside Providers
    fun addProviderReview(context: Context, providerId: String, review: UserReview) {
        val current = _dbState.value
        val updated = current.providers.map { provider ->
            if (provider.id == providerId) {
                val newReviews = provider.reviewsList + review
                val averageRating = newReviews.map { it.rating }.average()
                provider.copy(
                    reviewsList = newReviews,
                    reviewsCount = newReviews.size,
                    rating = String.format("%.1f", averageRating).toDoubleOrNull() ?: 5.0
                )
            } else provider
        }
        saveLocalAndPush(context, current.copy(providers = updated))
    }

    // Reports modifiers
    fun addReport(context: Context, report: Report) {
        val current = _dbState.value
        val updatedList = current.reports + report
        saveLocalAndPush(context, current.copy(reports = updatedList))
    }

    // Chats modifiers
    fun addChatMessage(context: Context, message: ChatMessage) {
        val current = _dbState.value
        val updatedList = current.chats + message
        saveLocalAndPush(context, current.copy(chats = updatedList))
    }

    fun deleteChatMessage(context: Context, messageId: String) {
        val current = _dbState.value
        val updatedList = current.chats.filter { it.id != messageId }
        saveLocalAndPush(context, current.copy(chats = updatedList))
    }

    fun clearChatLog(context: Context) {
        val current = _dbState.value
        saveLocalAndPush(context, current.copy(chats = emptyList()))
    }

    private fun getSeedData(): DbWrapper {
        val categories = listOf(
            CustomCategory("plumbing", "سباكة وصيانة", "Plumbing", "Build", "#3B82F6", true),
            CustomCategory("electrical", "كهرباء وصيانة", "Electrical", "Flash", "#10B981", true),
            CustomCategory("cooling", "تكييف وتبريد", "AC & Cooling", "AcUnit", "#EF4444", false),
            CustomCategory("health", "عيادات وصحة", "Clinics & Health", "Favorite", "#8B5CF6", true),
            CustomCategory("delivery", "شحن وتوصيل", "Logistics & Delivery", "LocalShipping", "#F59E0B", false),
            CustomCategory("teaching", "تدريس وتعليم", "Tuition & Teaching", "Book", "#EC4899", false),
            CustomCategory("food", "مأكولات ومطابخ", "Home Kitchens", "Restaurant", "#06B6D4", false)
        )

        val providers = listOf(
            ServiceProvider(
                id = "p1",
                name = "المهندس عادل لخدمات السباكة المتكاملة",
                category = "سباكة وصيانة",
                phone = "771234567",
                whatsapp = "771234567",
                description = "خبرة أكثر من 12 عاماً في اليمن لكشف السباكة، معالجة التسريبات وتركيب الأدوات الصحية بموثوقية.",
                rating = 4.8,
                reviewsCount = 3,
                views = 120,
                isVerified = true,
                tags = listOf("تجهيز", "تسريبات", "سريع"),
                address = "صنعاء - شارع الستين المترب",
                residenceRegion = "الأمانة",
                lat = 15.3694,
                lng = 44.1910,
                reviewsList = listOf(
                    UserReview("r1", "أبو بكر", 5, "عمل ممتاز وسرعة فائقة في الحضور!"),
                    UserReview("r2", "ماجد خالد", 4, "شخص مهذب وخبير جداً في السباكة.")
                )
            ),
            ServiceProvider(
                id = "p2",
                name = "عيادة الدكتور ماجد لطب الأطفال الحديث",
                category = "عيادات وصحة",
                phone = "733445566",
                whatsapp = "733445566",
                description = "رعاية طبية تخصصية متكاملة لحديثي الولادة والأطفال. استشارات تطور ونمو وعلاجات فورية.",
                rating = 4.9,
                reviewsCount = 1,
                views = 3120,
                isVerified = true,
                tags = listOf("أطفال", "عيادة", "صحة"),
                address = "عدن - المنصورة",
                residenceRegion = "عدن",
                lat = 12.8258,
                lng = 44.9749,
                reviewsList = listOf(
                    UserReview("r3", "عبدالله عمر", 5, "أفضل دكتور أطفال في المنطقة عن تجربة!")
                )
            )
        )

        val banners = listOf(
            BannerAd(
                id = "b1",
                imageUrl = "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&q=80&w=800",
                linkUrl = "https://example.com/dalily_guide",
                title = "تمتع بخصومات حصرية 25% مع مزودي الخدمة الموثقين لدينا طوال هذا الأسبوع"
            )
        )

        val admins = listOf(
            AdminProfile(
                id = "adm_super",
                username = "admin",
                email = "admin@dalily.com",
                passwordHash = "maher736462",
                role = "owner"
            ),
            AdminProfile(
                id = "adm_sup1",
                username = "super1",
                email = "supervisor1@dalily.com",
                passwordHash = "123456",
                role = "supervisor",
                canAcceptRequests = true,
                canAddProviders = true
            )
        )

        val faqs = listOf(
            FAQItem("f1", "ما هو تطبيق دليل الخدمات؟", "دليل الخدمات هو المنصة الذكية الأولى في اليمن التي تجمع المواطنين بمقدمي الخدمات والمهن والأطباء لتسهيل الوصول المباشر والآمن مع نظام فلترة دقيق لخدمتك فورياً."),
            FAQItem("f2", "هل التسجيل في دليل الخدمات مجاني؟", "نعم، التسجيل متاح لكافة السباكين والكهربائيين والأطباء وأصحاب المهن مجاناً بالكامل لبناء ثقة أكبر مع العملاء.")
        )

        return DbWrapper(
            providers = providers,
            banners = banners,
            categories = categories,
            admins = admins,
            faqs = faqs,
            config = AppConfig()
        )
    }
}
