package com.yemenservices.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.data.AppConfig
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.GeminiService
import com.yemenservices.app.data.PendingProvider
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.Repository
import com.yemenservices.app.data.ServiceProvider
import com.yemenservices.app.data.Supervisor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Chat message model
data class ChatMessage(
    val sender: String, // "user" or "bot"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AppViewModel : ViewModel() {

    private val repository = Repository()
    private val geminiService = GeminiService()

    // UI Configuration & Language (Arabic is default)
    private val _isArabic = MutableStateFlow(true)
    val isArabic = _isArabic.asStateFlow()

    // Manual Dark Mode override (null follows system, true is dark, false is light)
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode = _isDarkMode.asStateFlow()

    fun toggleDarkMode(systemDark: Boolean) {
        val current = _isDarkMode.value ?: systemDark
        _isDarkMode.value = !current
    }

    // Search and Selected category filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    // Admin & Backdoor State
    private val _currentAdmin = MutableStateFlow<Supervisor?>(null)
    val currentAdmin = _currentAdmin.asStateFlow()

    private val _isOwnerLoggedIn = MutableStateFlow(false)
    val isOwnerLoggedIn = _isOwnerLoggedIn.asStateFlow()

    // Local Chat Messages Session Stream
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    // Flow streams connected directly to Realtime repository
    val categories: StateFlow<List<Category>> = repository.categories
    val rawProviders: StateFlow<List<ServiceProvider>> = repository.providers
    val reviews: StateFlow<List<Review>> = repository.reviews
    val adminsList: StateFlow<List<Supervisor>> = repository.admins
    val pendingProvidersList: StateFlow<List<PendingProvider>> = repository.pendingProviders
    val appConfig: StateFlow<AppConfig> = repository.appConfig

    // Filtered providers based on search query and selected category
    val filteredProviders: StateFlow<List<ServiceProvider>> = combine(
        rawProviders,
        selectedCategoryId,
        searchQuery,
        _isArabic
    ) { providers, catId, query, isAr ->
        var list = providers.filter { it.is_approved }
        if (catId != null) {
            list = list.filter { it.category_id == catId }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.name_ar.contains(query, ignoreCase = true) ||
                it.name_en.contains(query, ignoreCase = true) ||
                it.region_ar.contains(query, ignoreCase = true) ||
                it.region_en.contains(query, ignoreCase = true)
            }
        }
        // Pinned elements first
        list.sortedWith(compareByDescending<ServiceProvider> { it.is_pinned }.thenBy { if (isAr) it.name_ar else it.name_en })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial chat welcome
        clearChatAndGreeting()
    }

    fun clearChatAndGreeting() {
        val welcome = appConfig.value.welcomeMessage
        _chatMessages.value = listOf(
            ChatMessage("bot", welcome)
        )
    }

    // Language Toggle
    fun toggleLanguage() {
        _isArabic.value = !_isArabic.value
    }

    // Refresh command
    fun refresh() {
        repository.refreshData()
    }

    // Filter selection
    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Authentication Actions ---

    // Admin login checked securely
    fun loginAdmin(username: String, password: String): Boolean {
        // Core supervisors credentials: Admin / maher736462
        if (username.equals("Admin", ignoreCase = true) && password == "maher736462") {
            val supervisor = Supervisor("admin-sys", "Admin", "maher736462", is_super_admin = false)
            _currentAdmin.value = supervisor
            _isOwnerLoggedIn.value = false
            return true
        }

        // Search dynamic list of admins loaded online
        val found = adminsList.value.firstOrNull {
            it.username.equals(username, ignoreCase = true) && it.password == password
        }
        if (found != null) {
            _currentAdmin.value = found
            _isOwnerLoggedIn.value = found.is_super_admin
            return true
        }
        return false
    }

    // Backdoor entry credentials checked
    fun entryBackdoorPassword(password: String): Boolean {
        // Owner backdoor password: maher--736462
        if (password == "maher--736462") {
            val masterAdmin = Supervisor("owner-sys", "Owner", "maher--736462", is_super_admin = true)
            _currentAdmin.value = masterAdmin
            _isOwnerLoggedIn.value = true
            return true
        }
        return false
    }

    fun logout() {
        _currentAdmin.value = null
        _isOwnerLoggedIn.value = false
    }

    // --- Gemini Interactive Help ---
    private fun getOfflineAnswer(prompt: String, footerPhone: String, categoriesText: String): String? {
        val p = prompt.trim().lowercase()
        val isAr = _isArabic.value
        return when {
            p.contains("أقسام") || p.contains("اقسام") || p.contains("قسم") || p.contains("categories") || p.contains("category") || p.contains("ماهي الأقسام") || p.contains("ما هي الاقسام") -> {
                if (isAr) {
                    "الأقسام المتاحة حالياً في التطبيق هي: $categoriesText. يمكنك الضغط على أي قسم في الشاشة الرئيسية لعرض المحترفين المسجلين به وتصفية مهاراتهم."
                } else {
                    "The available categories currently are: $categoriesText. You can tap on any category to view its registered professional service providers."
                }
            }
            p.contains("كيف أتصل") || p.contains("كيف اتصل") || p.contains("بالاتصال") || p.contains("تواصل") || p.contains("طريقة الاتصال") || p.contains("اتصال") || p.contains("contact") || p.contains("call") -> {
                if (isAr) {
                    "لتسهيل تواصلك مع مقدمي الخدمات باليمن، تجد داخل صفحة مقدم الخدمة أزراراً سريعة للتواصل بلمسة واحدة:\n📞 زر الاتصال الهاتفي المباشر\n💬 زر مراسلة واتساب الفورية\n✉️ زر الرسائل النصية القصيرة SMS\n🗺️ ومؤخراً تمت إضافة الموقع الجغرافي وخريطة تفاعلية لتسهيل تحديد المكان بدقة عالية."
                } else {
                    "To easily contact any service provider in Yemen, use the direct touch-to-communicate buttons in the provider's profile:\n📞 Direct Phone Call\n💬 Instant WhatsApp Chat\n✉️ SMS Text Messages\n🗺️ We have also integrated dynamic Google Maps view with navigation guidelines."
                }
            }
            p.contains("رقم الدعم") || p.contains("دعم") || p.contains("رقم التواصل") || p.contains("تلفون") || p.contains("هاتف") || p.contains("support") || p.contains("phone") || p.contains("help") -> {
                if (isAr) {
                    "رقم التواصل والدعم الفني المباشر لتطبيق دليل الخدمات هو: $footerPhone. نحن هنا لخدمتكم ومساعدتكم على مدار الساعة."
                } else {
                    "The customer support and help line number for Dalili is: $footerPhone. Contact us anytime if you face issues."
                }
            }
            else -> null
        }
    }

    fun sendMessageToAI(userPrompt: String) {
        if (userPrompt.isBlank()) return
        
        val newList = _chatMessages.value.toMutableList()
        newList.add(ChatMessage("user", userPrompt))
        _chatMessages.value = newList
        _isChatLoading.value = true
 
        viewModelScope.launch {
            val systemContext = """
                أنت مساعد ذكي مدمج في تطبيق "دليل الخدمات في اليمن" (Dalili) المطور بواسطة المهندس والمشرف ماهر.
                هدف المساعد هو توجيه المستخدمين لأفضل المهنيين المتاحين في مختلف المحافظات والمناطق اليمنية وتسهيل التواصل.
                أنت تقدم خدمات التوجيه المهني والنصائح للسباكة، الكهرباء، صيانة التكييف، دهانات والجبس، تطبيقات وبرمجة، الخياطة والتطريز، التعليم الطبي الخ.
                كن ودودًا للغاية، موجزًا، واستخدم اللهجة اليمنية مع الفصحى لتوفير الطمأنينة الكاملة للمستخدم.
                اسم التطبيق الحالي: ${appConfig.value.app_name}
                رقم التواصل والدعم الفني: ${appConfig.value.footer_phone}
                عدد الأقسام المتاحة: ${categories.value.size} أقسام.
            """.trimIndent()
 
            val categoryNames = categories.value.joinToString("، ") { if (_isArabic.value) it.name_ar else it.name_en }
            val localAnswer = getOfflineAnswer(userPrompt, appConfig.value.footer_phone, categoryNames)
            
            val aiResponse = if (localAnswer != null) {
                localAnswer
            } else {
                geminiService.getAssistantResponse(userPrompt, systemContext)
            }
            
            _isChatLoading.value = false
            
            val updatedList = _chatMessages.value.toMutableList()
            updatedList.add(ChatMessage("bot", aiResponse))
            _chatMessages.value = updatedList
        }
    }

    // --- Core Operations (Permissions protected appropriately) ---

    // A. Pending Registration (anyone can submit)
    fun registerPendingRequest(name: String, phone: String, categoryId: String, region: String) {
        val request = PendingProvider(
            id = "",
            name = name,
            phone = phone,
            category_id = categoryId,
            region = region,
            timestamp = System.currentTimeMillis()
        )
        repository.addPendingProvider(request)
    }

    // B. Approval Logic (Owner only can approve or delete pending requests)
    fun approvePendingProvider(pending: PendingProvider, isAr: Boolean) {
        if (!isOwnerLoggedIn.value) return // Block normal supervisor click
        
        val serviceProvider = ServiceProvider(
            id = "",
            category_id = pending.category_id,
            name_ar = pending.name,
            name_en = pending.name, // default english option
            phone = pending.phone,
            whatsapp = "967" + pending.phone.removePrefix("0").removePrefix("967"),
            region_ar = pending.region,
            region_en = pending.region,
            price_range = "medium",
            distance = "medium",
            is_pinned = false,
            is_approved = true,
            image_url = null
        )
        // Add approved service provider
        repository.addProvider(serviceProvider)
        // Remove from pending collection
        repository.deletePendingProvider(pending.id)
    }

    fun rejectPendingProvider(pendingId: String) {
        if (!isOwnerLoggedIn.value) return // Block normal supervisor
        repository.deletePendingProvider(pendingId)
    }

    // C. Categories operations (Admins/Supervisors can manage)
    fun addCategory(category: Category) {
        repository.addCategory(category)
    }

    fun updateCategory(category: Category) {
        repository.updateCategory(category)
    }

    fun deleteCategory(categoryId: String) {
        repository.deleteCategory(categoryId)
    }

    // D. Providers operations (Admins can manage approved list)
    fun addApprovedProvider(provider: ServiceProvider) {
        repository.addProvider(provider)
    }

    fun updateApprovedProvider(provider: ServiceProvider) {
        repository.updateProvider(provider)
    }

    fun deleteApprovedProvider(providerId: String) {
        repository.deleteProvider(providerId)
    }

    // E. Dynamic Supervisors Administration (Owner only can add/delete admins)
    fun addSupervisorAdmin(admin: Supervisor) {
        if (!isOwnerLoggedIn.value) return
        repository.addSupervisor(admin)
    }

    fun deleteSupervisorAdmin(adminId: String) {
        if (!isOwnerLoggedIn.value) return
        repository.deleteSupervisor(adminId)
    }

    // F. Reviews Management
    fun addReview(review: Review) {
        repository.addReview(review)
    }

    fun deleteReview(reviewId: String) {
        repository.deleteReview(reviewId)
    }

    // G. App settings custom configurations (Owner / Backdoor Admin only)
    fun updateSystemConfig(config: AppConfig) {
        if (!isOwnerLoggedIn.value) return
        repository.updateAppConfig(config)
    }

    // H. Login Persistence helpers (Remember login state)
    fun saveLogin(context: Context, username: String, isOwner: Boolean) {
        val prefs = context.getSharedPreferences("dalili_login_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("saved_username", username)
            .putBoolean("saved_is_owner", isOwner)
            .apply()
    }

    fun clearSavedLogin(context: Context) {
        val prefs = context.getSharedPreferences("dalili_login_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun autoLoginIfSaved(context: Context) {
        val prefs = context.getSharedPreferences("dalili_login_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("saved_username", null)
        val isOwner = prefs.getBoolean("saved_is_owner", false)
        if (username != null) {
            val supervisor = Supervisor("saved-session", username, "", is_super_admin = isOwner)
            _currentAdmin.value = supervisor
            _isOwnerLoggedIn.value = isOwner
        }
    }
}
