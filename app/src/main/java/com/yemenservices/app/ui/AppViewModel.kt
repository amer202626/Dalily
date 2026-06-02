package com.yemenservices.app.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.Repository
import com.yemenservices.app.data.ServiceProvider
import com.yemenservices.app.data.RegistrationRequest
import com.yemenservices.app.data.Supervisor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AppViewModel(application: Application, private val repository: Repository) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("dalili_prefs", Context.MODE_PRIVATE)

    // Font scaling settings
    var fontSizeScale by mutableStateOf(sharedPrefs.getFloat("app_font_size_scale", 1.0f))
        private set

    fun adjustFontSize(increment: Boolean) {
        val current = fontSizeScale
        val next = if (increment) current + 0.1f else current - 0.1f
        fontSizeScale = next.coerceIn(0.7f, 2.0f)
        sharedPrefs.edit().putFloat("app_font_size_scale", fontSizeScale).apply()
    }

    // Unique user identification for Firebase wishlist backup
    val userUid: String = sharedPrefs.getString("user_uid", null) ?: run {
        val nextUid = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
        sharedPrefs.edit().putString("user_uid", nextUid).apply()
        nextUid
    }

    // Wishlist filters
    var showOnlyFavorites by mutableStateOf(false)

    // Language setting (Arabic as default, language code "ar" vs "en")
    var currentLanguage by mutableStateOf(sharedPrefs.getString("lang", "ar") ?: "ar")
        private set

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == "ar") "en" else "ar"
        sharedPrefs.edit().putString("lang", currentLanguage).apply()
    }

    // Role-based authorization states
    var isAdminMode by mutableStateOf(false)
        private set

    var loggedInSupervisor by mutableStateOf<Supervisor?>(null)
        private set

    var saveLoginOption by mutableStateOf(sharedPrefs.getBoolean("save_login", false))

    init {
        // Auto sign-in supervisor if session is saved
        val savedSupId = sharedPrefs.getString("saved_sup_id", "") ?: ""
        if (saveLoginOption && savedSupId.isNotBlank()) {
            viewModelScope.launch {
                repository.allSupervisors.firstOrNull()?.find { it.id == savedSupId }?.let {
                    loggedInSupervisor = it
                }
            }
        }
        
        // Auto-increment app launch users count
        viewModelScope.launch {
            repository.incrementUsersCount()
        }
    }

    fun performLogin(usernameAttempt: String, passwordAttempt: String): Boolean {
        val trimmedUser = usernameAttempt.trim()
        val trimmedPass = passwordAttempt.trim()

        // 1. Check Admin Credentials
        if (trimmedUser == "admin" && trimmedPass == "maher736462") {
            isAdminMode = true
            loggedInSupervisor = null
            if (saveLoginOption) {
                sharedPrefs.edit()
                    .putBoolean("save_login", true)
                    .putString("saved_sup_id", "admin_saved")
                    .apply()
            }
            return true
        }

        // 2. Check Supervisor Credentials
        val supervisors = supervisors.value
        val supervisor = supervisors.find { 
            it.username.equals(trimmedUser, ignoreCase = true) && it.password == trimmedPass 
        }
        if (supervisor != null) {
            loggedInSupervisor = supervisor
            isAdminMode = false
            if (saveLoginOption) {
                sharedPrefs.edit()
                    .putBoolean("save_login", true)
                    .putString("saved_sup_id", supervisor.id)
                    .apply()
            } else {
                sharedPrefs.edit().remove("saved_sup_id").apply()
            }
            return true
        }

        return false
    }

    fun performBackdoorLogin() {
        isAdminMode = true
        loggedInSupervisor = null
        if (saveLoginOption) {
            sharedPrefs.edit()
                .putBoolean("save_login", true)
                .putString("saved_sup_id", "admin_saved")
                .apply()
        }
    }

    fun performLogout() {
        isAdminMode = false
        loggedInSupervisor = null
        sharedPrefs.edit()
            .remove("saved_sup_id")
            .putBoolean("save_login", false)
            .apply()
        saveLoginOption = false
    }

    fun toggleSaveLogin() {
        saveLoginOption = !saveLoginOption
        sharedPrefs.edit().putBoolean("save_login", saveLoginOption).apply()
    }

    // Search query & Advanced Filter states
    var searchQuery by mutableStateOf("")
    var selectedCategoryId by mutableStateOf(-1)

    var ratingFilter by mutableStateOf<Int?>(null) // null: list all, or 1..5
    var distanceFilter by mutableStateOf<String?>(null) // null: any, CLOSE, MEDIUM, FAR
    var priceFilter by mutableStateOf<String?>(null) // null: any, LOW, MEDIUM, HIGH
    var governorateFilter by mutableStateOf<String?>(null) // null: any, or selected Governorate name
    var districtFilter by mutableStateOf<String?>(null) // null: any, or typed district name

    fun clearFilters() {
        ratingFilter = null
        distanceFilter = null
        priceFilter = null
        searchQuery = ""
        governorateFilter = null
        districtFilter = null
    }

    // Real-time Data streams from Repository
    val categories: StateFlow<List<Category>> = repository.allCategories
        .map { list -> list.sortedByDescending { it.isPinned } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<Map<String, String>> = repository.allSettings
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val serviceProviders: StateFlow<List<ServiceProvider>> = repository.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supervisors: StateFlow<List<Supervisor>> = repository.allSupervisors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registrationRequests: StateFlow<List<RegistrationRequest>> = repository.allRegistrationRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalAnalytics: StateFlow<Map<String, Long>> = repository.getGlobalAnalytics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), mapOf("usersCount" to 100L, "totalCalls" to 0L))

    // Real-time Favorite service providers list flow
    val favoriteProviderIds: StateFlow<List<Int>> = repository.getFavoritesForUser(userUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(providerId: Int) {
        viewModelScope.launch {
            val currentList = favoriteProviderIds.value
            val isCurrentlyFav = currentList.contains(providerId)
            repository.toggleFavorite(userUid, providerId, !isCurrentlyFav)
        }
    }

    // Advanced sorting/filtering combined StateFlow
    val filteredProviders: StateFlow<List<ServiceProvider>> = combine(
        serviceProviders,
        favoriteProviderIds,
        snapshotFlow { selectedCategoryId },
        snapshotFlow { searchQuery },
        snapshotFlow { ratingFilter },
        snapshotFlow { distanceFilter },
        snapshotFlow { priceFilter },
        snapshotFlow { showOnlyFavorites },
        snapshotFlow { governorateFilter },
        snapshotFlow { districtFilter }
    ) { array ->
        val providers = array[0] as List<ServiceProvider>
        val favorites = array[1] as List<Int>
        val categoryId = array[2] as Int
        val query = array[3] as String
        val rating = array[4] as Int?
        val distance = array[5] as String?
        val price = array[6] as String?
        val onlyFavorites = array[7] as Boolean
        val gov = array[8] as String?
        val dist = array[9] as String?

        providers.filter { provider ->
            val matchesFavorites = !onlyFavorites || favorites.contains(provider.id)
            val matchesCategory = categoryId == -1 || provider.categoryId == categoryId
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                provider.nameAr.contains(query, ignoreCase = true) ||
                provider.nameEn.contains(query, ignoreCase = true) ||
                provider.descriptionAr.contains(query, ignoreCase = true) ||
                provider.descriptionEn.contains(query, ignoreCase = true) ||
                provider.phone.contains(query, ignoreCase = true) ||
                provider.addressAr.contains(query, ignoreCase = true) ||
                provider.addressEn.contains(query, ignoreCase = true)
            }
            val matchesRating = rating == null || provider.rating >= rating
            val matchesDistance = when (distance) {
                "CLOSE" -> provider.distanceKm <= 1.5f
                "MEDIUM" -> provider.distanceKm > 1.5f && provider.distanceKm <= 4.0f
                "FAR" -> provider.distanceKm > 4.0f
                else -> true
            }
            val matchesPrice = price == null || provider.priceLevel.equals(price, ignoreCase = true)
            
            val matchesGov = gov.isNullOrBlank() || 
                    provider.addressAr.contains(gov, ignoreCase = true) || 
                    provider.addressEn.contains(gov, ignoreCase = true)
                    
            val matchesDist = dist.isNullOrBlank() || 
                    provider.addressAr.contains(dist, ignoreCase = true) || 
                    provider.addressEn.contains(dist, ignoreCase = true) || 
                    provider.descriptionAr.contains(dist, ignoreCase = true) || 
                    provider.descriptionEn.contains(dist, ignoreCase = true)

            matchesFavorites && matchesCategory && matchesSearch && matchesRating && matchesDistance && matchesPrice && matchesGov && matchesDist
        }.sortedWith(
            compareByDescending<ServiceProvider> { it.isFeatured }
                .thenByDescending { it.isVerified }
                .thenByDescending { it.rating }
                .thenByDescending { it.id }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reviews Management
    fun getReviewsForProvider(providerId: Int): Flow<List<com.yemenservices.app.data.ProviderReview>> {
        return repository.getReviewsForProvider(providerId)
    }

    fun addProviderReview(review: com.yemenservices.app.data.ProviderReview) {
        viewModelScope.launch {
            repository.insertReview(review)
        }
    }

    fun updateProviderReview(review: com.yemenservices.app.data.ProviderReview) {
        viewModelScope.launch {
            repository.updateReview(review)
        }
    }

    fun deleteProviderReview(review: com.yemenservices.app.data.ProviderReview) {
        viewModelScope.launch {
            repository.deleteReview(review)
        }
    }

    // Setting keys convenience
    fun getSettingValue(key: String, defaultValue: String = ""): String {
        return settings.value[key] ?: defaultValue
    }

    // Role Permissions check helper
    fun canPerformAction(actionType: String): Boolean {
        if (isAdminMode) return true
        val supervisor = loggedInSupervisor ?: return false
        return when (actionType) {
            "ADD_PROVIDER" -> supervisor.canAddProviders
            "APPROVE_REQUESTS" -> supervisor.canApproveRequests
            "CHANGE_SETTINGS" -> supervisor.canChangeSettings
            else -> false
        }
    }

    // Providers Management
    fun addServiceProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.insertProvider(provider)
        }
    }

    fun updateServiceProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.updateProvider(provider)
        }
    }

    fun deleteServiceProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.deleteProvider(provider)
        }
    }

    fun triggerCall(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.incrementCallCount(provider.id)
        }
    }

    // Category Management
    fun addCategory(nameAr: String, nameEn: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(nameAr = nameAr, nameEn = nameEn, iconName = iconName))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Settings Management
    fun updateAppSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }

    // Supervisor Management
    fun addSupervisor(supervisor: Supervisor) {
        viewModelScope.launch {
            repository.insertSupervisor(supervisor)
        }
    }

    fun deleteSupervisor(supervisor: Supervisor) {
        viewModelScope.launch {
            repository.deleteSupervisor(supervisor.id)
        }
    }

    // Registration Requests Flow & Operations
    fun submitRegistrationRequest(request: RegistrationRequest) {
        viewModelScope.launch {
            repository.insertRegistrationRequest(request)
        }
    }

    fun approveRegistrationRequest(request: RegistrationRequest) {
        viewModelScope.launch {
            // Create ServiceProvider and insert
            val verifiedProvider = ServiceProvider(
                id = (System.currentTimeMillis() % 10000000).toInt(),
                categoryId = request.categoryId,
                nameAr = request.fullName,
                nameEn = request.fullName, // simple English default
                phone = request.phone,
                addressAr = request.workplaceAddress,
                addressEn = request.workplaceAddress,
                descriptionAr = request.serviceType,
                descriptionEn = request.serviceType,
                workingHours = "8:00 AM - 5:00 PM",
                isVerified = true,
                latitude = request.latitude,
                longitude = request.longitude,
                profilePhoto = request.profilePhoto,
                idCardPhoto = request.idCardPhoto,
                customField1Value = request.residenceRegion // store residency region in extra 1
            )
            repository.insertProvider(verifiedProvider)
            repository.updateRegistrationRequestStatus(request.id, "APPROVED")
        }
    }

    fun rejectRegistrationRequest(request: RegistrationRequest) {
        viewModelScope.launch {
            repository.updateRegistrationRequestStatus(request.id, "REJECTED")
        }
    }

    fun deleteRegistrationRequest(requestId: String) {
        viewModelScope.launch {
            repository.deleteRegistrationRequest(requestId)
        }
    }

    // Smart AI Assistant Gemini Q&A REST request
    var aiAnswerState by mutableStateOf("")
        private set
    var aiLoadingState by mutableStateOf(false)
        private set

    fun askGeminiAssistant(question: String) {
        aiLoadingState = true
        aiAnswerState = ""
        viewModelScope.launch {
            val answer = callGeminiREST(question)
            aiAnswerState = answer
            aiLoadingState = false
        }
    }

    private suspend fun callGeminiREST(prompt: String): String = withContext(Dispatchers.IO) {
        // Safe key check
        val apiKey = "" // API Keys can be fetched from a custom buildconfig or empty string to trigger helpful offline responder
        if (apiKey.isBlank()) {
            return@withContext getOfflineSmartResponse(prompt)
        }

        try {
            val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true

            val systemInstruction = "أنت مساعد ذكي لتطبيق دليل الخدمات اليمني. تجيب بلطف وبإيجاز شديد عن الخدمات الطبية والمهنية والتعليمية المتوفرة في اليمن، وتساعد بإرشادات حكيمة."
            val requestBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(response.toString())
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                parts.getJSONObject(0).getString("text")
            } else {
                getOfflineSmartResponse(prompt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getOfflineSmartResponse(prompt)
        }
    }

    private fun getOfflineSmartResponse(prompt: String): String {
        val lower = prompt.toLowerCase()
        return when {
            lower.contains("طوارئ") || lower.contains("اسعاف") || lower.contains("emergency") -> {
                "🚨 في حالات الطوارئ الطبية باليمن، يمكنك الاتصال بالإسعاف السريع وطنيًا (191) أو التوجه فورًا لمستشفى الثورة العام بصنعاء. كما يوفر دليلنا تصنيف 'طوارئ وإسعاف' لشرائح الاتصال السريع بمراكز الطوارئ المعتمدة."
            }
            lower.contains("كهرباء") || lower.contains("سباكة") || lower.contains("solar") || lower.contains("طاقة") -> {
                "🛠️ يتوفر في الدليل تصنيف متخصص لـ 'كهرباء وسباكة'، بما في ذلك فنيي صيانة الطاقة الشمسية والبطاريات المنزلية في عدن وصنعاء ومختلف المحافظات."
            }
            lower.contains("تعليم") || lower.contains("مدرسة") || lower.contains("جامعة") || lower.contains("school") -> {
                "🎓 للخدمات التعليمية، يضم تصنيف 'تعليم ومدارس' عتبة هامة مثل ثانوية جمال عبد الناصر للممتلكين الموهوبين بصنعاء وغيرها من المعاهد المهنية."
            }
            lower.contains("مشرف") || lower.contains("supervisor") || lower.contains("ادمن") -> {
                "⚙️ لوحة المشرفين تتيح تسجيل مقدمي الخدمات ومراجعة طلبات الانضمام المعلقة تمهيدًا للقبول أو الرفض بالاتساق مع صلاحيات الآدمن."
            }
            lower.contains("عن تطبيق") || lower.contains("ماهو") || lower.contains("دليلي") -> {
                "ℹ️ دليل الخدمات اليمني باللغتين العربية والإنجليزية يهدف لتسهيل الوصول المجاني والمباشر لكافة الخدمات المهنية، الطبية، التعليمية وصيانة السيارات في المحافظات اليمنية."
            }
            else -> {
                "مرحباً بك في دليل الخدمات اليمني! 🇾🇪\nأنا مساعدك الذكي لمختلف الأقسام (طوارئ 🚨، عيادات ومستشفيات 🏥، كهرباء وسباكة 🛠️، تعليم 🎓، سيارات 🚗).\nكيف يمكنني مساعدتك اليوم؟ يمكنك كتابة استفسارك للبحث السريع."
            }
        }
    }
}

// ViewModel Factory
class AppViewModelFactory(private val application: Application, private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
