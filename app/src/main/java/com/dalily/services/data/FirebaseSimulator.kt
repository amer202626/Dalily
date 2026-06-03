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
    val version: Int = 1
)

object FirebaseSimulator {
    private const val TAG = "FirebaseSimulator"
    private const val LOCAL_FILE_NAME = "dalily_services_db.json"
    
    // Unique shared cloud endpoint for instant real-time synchronization among all devices
    private const val CLOUD_URL = "https://kvdb.io/KxPcaYJbLwD5M9V9GvW1R/dalily_sync_db_v10"

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        coerceInputValues = true
    }

    private val _dbState = MutableStateFlow(DbWrapper())
    val dbState = _dbState.asStateFlow()

    private val _syncingState = MutableStateFlow(false)
    val syncingState = _syncingState.asStateFlow()

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            loadLocalData(context)
            // Trigger initial sync from cloud
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
                Log.d(TAG, "Local database loaded. Providers: ${loaded.providers.size}")
            } else {
                // Initialize seed data
                val initialDb = getSeedData()
                saveLocalAndPush(context, initialDb)
                Log.d(TAG, "Database seeded with initial values.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local data: ${e.message}", e)
            _dbState.value = getSeedData()
        }
    }

    private fun saveLocalAndPush(context: Context, newDb: DbWrapper) {
        _dbState.value = newDb
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Save to local storage for instant offline access
                val file = File(context.filesDir, LOCAL_FILE_NAME)
                val jsonText = jsonHelper.encodeToString(newDb)
                file.writeText(jsonText)
                
                // 2. Sync to cloud in background
                pushToCloud(jsonText)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing config or pushing: ${e.message}")
            }
        }
    }

    // PUSH local JSON representation to our cloud bucket
    private fun pushToCloud(jsonText: String) {
        try {
            val url = URL(CLOUD_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.setRequestProperty("Content-Type", "application/json")

            val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
            writer.write(jsonText)
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            Log.d(TAG, "Cloud sync push completed with response code: $responseCode")
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Could not push to cloud (Offline/Network error): ${e.message}")
        }
    }

    // FETCH state from our cloud bucket and merge/replace
    suspend fun syncWithCloud(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            _syncingState.value = true
            try {
                val url = URL(CLOUD_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = InputStreamReader(connection.inputStream, "UTF-8")
                    val responseText = reader.readText()
                    reader.close()

                    if (responseText.trim().isNotEmpty()) {
                        val cloudDb = jsonHelper.decodeFromString<DbWrapper>(responseText)
                        
                        // Merge or replace depending on what is newer. In this directory scheme,
                        // we'll replace current state with the synchronized cloud database state
                        _dbState.value = cloudDb
                        
                        // Cache it locally too
                        val file = File(context.filesDir, LOCAL_FILE_NAME)
                        file.writeText(responseText)
                        
                        Log.d(TAG, "Successfully synced with cloud storage. Providers count: ${cloudDb.providers.size}")
                        _syncingState.value = false
                        connection.disconnect()
                        return@withContext true
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Cloud sync fetch failed: ${e.message}")
            }
            _syncingState.value = false
            return@withContext false
        }
    }

    // Service providers modifiers
    fun addServiceProvider(context: Context, provider: ServiceProvider) {
        val current = _dbState.value
        val updatedList = current.providers.filter { it.id != provider.id } + provider
        val updated = current.copy(providers = updatedList)
        saveLocalAndPush(context, updated)
    }

    fun updateServiceProvider(context: Context, provider: ServiceProvider) {
        addServiceProvider(context, provider)
    }

    fun deleteServiceProvider(context: Context, id: String) {
        val current = _dbState.value
        val updatedList = current.providers.filter { it.id != id }
        val updated = current.copy(providers = updatedList)
        saveLocalAndPush(context, updated)
    }

    // Sections/Categories modifiers
    fun addCategory(context: Context, category: CustomCategory) {
        val current = _dbState.value
        val updatedList = current.categories.filter { it.id != category.id } + category
        val updated = current.copy(categories = updatedList)
        saveLocalAndPush(context, updated)
    }

    fun updateCategory(context: Context, category: CustomCategory) {
        addCategory(context, category)
    }

    fun deleteCategory(context: Context, id: String) {
        val current = _dbState.value
        // Delete category
        val updatedList = current.categories.filter { it.id != id }
        // Also remove providers in this category or let them be
        val updated = current.copy(categories = updatedList)
        saveLocalAndPush(context, updated)
    }

    // Banner modifiers
    fun addBanner(context: Context, banner: BannerAd) {
        val current = _dbState.value
        val updatedList = current.banners.filter { it.id != banner.id } + banner
        val updated = current.copy(banners = updatedList)
        saveLocalAndPush(context, updated)
    }

    fun deleteBanner(context: Context, id: String) {
        val current = _dbState.value
        val updatedList = current.banners.filter { it.id != id }
        val updated = current.copy(banners = updatedList)
        saveLocalAndPush(context, updated)
    }

    // Admins modifiers
    fun addAdminProfile(context: Context, admin: AdminProfile) {
        val current = _dbState.value
        val updatedList = current.admins.filter { it.username != admin.username } + admin
        val updated = current.copy(admins = updatedList)
        saveLocalAndPush(context, updated)
    }

    fun deleteAdminProfile(context: Context, username: String) {
        val current = _dbState.value
        val updatedList = current.admins.filter { it.username != username }
        val updated = current.copy(admins = updatedList)
        saveLocalAndPush(context, updated)
    }

    // Service requests modifiers (Accepted/Rejected by admins)
    fun addServiceRequest(context: Context, request: ServiceRequest) {
        val current = _dbState.value
        val updatedList = current.serviceRequests.filter { it.id != request.id } + request
        val updated = current.copy(serviceRequests = updatedList)
        saveLocalAndPush(context, updated)
    }

    fun updateServiceRequestStatus(context: Context, id: String, status: String) {
        val current = _dbState.value
        val updatedList = current.serviceRequests.map {
            if (it.id == id) it.copy(status = status) else it
        }
        
        // If approved, create the service provider automatically!
        var newProviders = current.providers
        if (status == "APPROVED") {
            val req = current.serviceRequests.find { it.id == id }
            if (req != null) {
                val newProvider = ServiceProvider(
                    id = "p_${System.currentTimeMillis()}",
                    name = req.providerName,
                    category = req.category,
                    phone = req.phone,
                    whatsapp = req.whatsapp,
                    description = req.description,
                    rating = 5.0,
                    reviewsCount = 1,
                    views = 15,
                    isVerified = true
                )
                newProviders = newProviders + newProvider
            }
        }

        val updated = current.copy(serviceRequests = updatedList, providers = newProviders)
        saveLocalAndPush(context, updated)
    }

    // Reports modifiers
    fun addReport(context: Context, report: Report) {
        val current = _dbState.value
        val updatedList = current.reports + report
        val updated = current.copy(reports = updatedList)
        saveLocalAndPush(context, updated)
    }

    // Chats modifiers
    fun addChatMessage(context: Context, message: ChatMessage) {
        val current = _dbState.value
        val updatedList = current.chats + message
        val updated = current.copy(chats = updatedList)
        saveLocalAndPush(context, updated)
    }

    // Initial seed dataset
    private fun getSeedData(): DbWrapper {
        val categories = listOf(
            CustomCategory("plumbing", "سباكة وصيانة", "Plumbing", "Settings", "#3B82F6"),
            CustomCategory("electrical", "كهرباء وصيانة", "Electrical", "Build", "#10B981"),
            CustomCategory("cooling", "تكييف وتبريد", "AC & Cooling", "AcUnit", "#EF4444"),
            CustomCategory("health", "عيادات وصحة", "Clinics & Health", "Favorite", "#8B5CF6"),
            CustomCategory("delivery", "شحن وتوصيل", "Logistics & Delivery", "LocalShipping", "#F59E0B"),
            CustomCategory("teaching", "تدريس وتعليم", "Tuition & Teaching", "Book", "#EC4899"),
            CustomCategory("food", "مأكولات ومطابخ", "Home Kitchens & Food", "Restaurant", "#06B6D4")
        )

        val providers = listOf(
            ServiceProvider(
                id = "p1",
                name = "المهندس أحمد لخدمات السباكة المتكاملة",
                category = "سباكة وصيانة",
                phone = "771234567",
                whatsapp = "771234567",
                description = "خبرة أكثر من 10 سنوات في أعمال السباكة، كشف تسريبات المياه وأعمال التجديد الصحي.",
                rating = 4.8,
                reviewsCount = 34,
                views = 1420,
                isVerified = true,
                tags = listOf("تجهيز", "تسريبات", "سريع"),
                address = "صنعاء - شارع الستين"
            ),
            ServiceProvider(
                id = "p2",
                name = "أبو ماجد لكهرباء المنازل وصيانة المحركات",
                category = "كهرباء وصيانة",
                phone = "733445566",
                whatsapp = "733445566",
                description = "صيانة وتمديد شبكات الكهرباء المنزلية والإنارة الحديثة بأفضل تقنيات الأمان.",
                rating = 4.9,
                reviewsCount = 18,
                views = 890,
                isVerified = true,
                tags = listOf("إنارة", "تمديد", "فوري"),
                address = "عدن - كريتر"
            ),
            ServiceProvider(
                id = "p3",
                name = "المتحدون لصيانة التكييف والتبريد المركزي",
                category = "تكييف وتبريد",
                phone = "711223344",
                whatsapp = "711223344",
                description = "غسيل وصيانة وتركيب مكيفات سبليت وشباك وأجهزة التبريد وتعبئة غاز التبريد الأصلي.",
                rating = 4.6,
                reviewsCount = 12,
                views = 612,
                isVerified = false,
                tags = listOf("فريون", "مكيفات", "شحن"),
                address = "تعز - شارع جمال"
            ),
            ServiceProvider(
                id = "p4",
                name = "مركز الأمل التخصصي لطب الأسنان",
                category = "عيادات وصحة",
                phone = "770000111",
                whatsapp = "",
                description = "عيادة متخصصة لتركيب وتجميل وزراعة الأسنان، دقة عالية ورعاية طبية ممتازة.",
                rating = 4.7,
                reviewsCount = 45,
                views = 3120,
                isVerified = true,
                tags = listOf("عيادة", "أسنان", "تنظيف"),
                address = "صنعاء - شارع حدة"
            ),
            ServiceProvider(
                id = "p5",
                name = "النجم لخدمات التوصيل البري السريع باليمن",
                category = "شحن وتوصيل",
                phone = "777554433",
                whatsapp = "777554433",
                description = "توصيل طرود، مستندات، أمتعة وهدايا وتوزيع تجاري آمن وسريع لجميع المحافظات.",
                rating = 4.5,
                reviewsCount = 22,
                views = 780,
                isVerified = true,
                tags = listOf("ديلفري", "طرود", "شحن"),
                address = "صنعاء - الحصبة"
            ),
            ServiceProvider(
                id = "p6",
                name = "مطبخ وتكيات ياسمين الشام للأكلات والولائم",
                category = "مأكولات ومطابخ",
                phone = "733990011",
                whatsapp = "733990011",
                description = "تجهيز بوفيهات حفلات، ولائم مناسبات، أطباق يمنية وشامية لجميع العائلات والمكاتب.",
                rating = 4.9,
                reviewsCount = 56,
                views = 2450,
                isVerified = true,
                tags = listOf("غداء", "كبسة", "شيف"),
                address = "الحديدة - شارع صنعاء"
            )
        )

        val banners = listOf(
            BannerAd(
                id = "b1",
                imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&q=80&w=800",
                linkUrl = "https://unsplash.com",
                title = "تخفيضات 20% لكهرباء وصيانة المنازل مع دليل الخدمات!"
            ),
            BannerAd(
                id = "b2",
                imageUrl = "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&q=80&w=800",
                linkUrl = "https://unsplash.com",
                title = "تطبيق دليلي متوفر للجميع الآن - راحة تامة طوال اليوم!"
            )
        )

        val admins = listOf(
            AdminProfile(
                id = "admin_initial",
                username = "admin",
                email = "admin@dalily.com",
                passwordHash = "12345",
                creatorRole = "super_admin"
            )
        )

        return DbWrapper(
            providers = providers,
            banners = banners,
            categories = categories,
            admins = admins
        )
    }
}
