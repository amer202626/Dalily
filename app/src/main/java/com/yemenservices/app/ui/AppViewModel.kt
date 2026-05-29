package com.yemenservices.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.data.AppConfig
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.GeminiService
import com.yemenservices.app.data.Repository
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.ServiceProvider
import com.yemenservices.app.data.Supervisor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository()
    private val geminiService = GeminiService()
    
    private val sharedPrefs = application.getSharedPreferences("dalili_user_prefs", Context.MODE_PRIVATE)

    // Observables directly bound to Real-Time Firebase Listener
    val categories: StateFlow<List<Category>> = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val providers: StateFlow<List<ServiceProvider>> = repository.getProviders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val reviews: StateFlow<List<Review>> = repository.getReviews()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val supervisors: StateFlow<List<Supervisor>> = repository.getSupervisors()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val appConfig: StateFlow<AppConfig> = repository.getAppConfig()
        .stateIn(viewModelScope, SharingStarted.Lazily, AppConfig())

    // Admin Session State
    private val _currentAdmin = MutableStateFlow<Supervisor?>(null)
    val currentAdmin: StateFlow<Supervisor?> = _currentAdmin.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // UI Interactive States (Dark Mode, AI Chat, Referral System)
    private val _isDarkTheme = MutableStateFlow(sharedPrefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // AI Chat history logs (Pair of content and isUser)
    private val _aiChatLog = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val aiChatLog: StateFlow<List<Pair<String, Boolean>>> = _aiChatLog.asStateFlow()

    private val _aiGenerating = MutableStateFlow(false)
    val aiGenerating: StateFlow<Boolean> = _aiGenerating.asStateFlow()

    // Local user Referral system state
    private val _userInviteCode = MutableStateFlow("")
    val userInviteCode: StateFlow<String> = _userInviteCode.asStateFlow()

    private val _userPoints = MutableStateFlow(sharedPrefs.getInt("referral_points", 0))
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _hasReferred = MutableStateFlow(sharedPrefs.getBoolean("has_referred", false))
    val hasReferred: StateFlow<Boolean> = _hasReferred.asStateFlow()

    // Call tracking
    private val _totalTrackedCalls = MutableStateFlow(sharedPrefs.getInt("total_calls", 15)) // start with nice baseline
    val totalTrackedCalls: StateFlow<Int> = _totalTrackedCalls.asStateFlow()

    init {
        // Initialize Unique Invite Code if not exists
        var code = sharedPrefs.getString("invite_code", "") ?: ""
        if (code.isBlank()) {
            val randomSegment = UUID.randomUUID().toString().take(6).uppercase()
            code = "DALILI-$randomSegment"
            sharedPrefs.edit().putString("invite_code", code).apply()
        }
        _userInviteCode.value = code

        // Add welcome message from AI
        viewModelScope.launch {
            _aiChatLog.value = listOf(
                Pair("أهلاً بك يا غالي في دليل اليمن الشامل ومساعدك الذكي! كيف أستطيع خدمتك اليوم؟ يمكنك الاستفسار عن الأقسام وتفاصيل الاتصال بالدعم الفني للمصمم م/ماهر أحمد وسأجيبك فوراً.", false)
            )
        }
    }

    // Toggle Theme state
    fun toggleTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        sharedPrefs.edit().putBoolean("dark_theme", enabled).apply()
    }

    // Track a Call (Whenever phone button is clicked)
    fun trackCallClicked() {
        val count = _totalTrackedCalls.value + 1
        _totalTrackedCalls.value = count
        sharedPrefs.edit().putInt("total_calls", count).apply()
    }

    // Apply Referral code
    fun applyReferralCode(code: String): Boolean {
        if (_hasReferred.value) return false
        val trimmedCode = code.trim().uppercase()
        if (trimmedCode.isNotBlank() && trimmedCode.startsWith("DALILI-") && trimmedCode != _userInviteCode.value) {
            // Success referral
            val newPoints = _userPoints.value + 50
            _userPoints.value = newPoints
            _hasReferred.value = true
            sharedPrefs.edit()
                .putInt("referral_points", newPoints)
                .putBoolean("has_referred", true)
                .apply()
            return true
        }
        return false
    }

    // Reset referral points (by Admin toggle)
    fun adminResetUserPoints() {
        _userPoints.value = 0
        _hasReferred.value = false
        sharedPrefs.edit()
            .putInt("referral_points", 0)
            .putBoolean("has_referred", false)
            .apply()
    }

    // Chat function
    fun sendChatMessage(msg: String) {
        if (msg.isBlank() || _aiGenerating.value) return
        
        val currentMsgList = _aiChatLog.value.toMutableList()
        currentMsgList.add(Pair(msg, true))
        _aiChatLog.value = currentMsgList
        
        _aiGenerating.value = true
        
        viewModelScope.launch {
            // Pass live category list and appConfig to assistant for offline & system guidance
            val reply = geminiService.getAiReply(msg, categories.value, appConfig.value)
            val updatedMsgList = _aiChatLog.value.toMutableList()
            updatedMsgList.add(Pair(reply, false))
            _aiChatLog.value = updatedMsgList
            _aiGenerating.value = false
        }
    }

    // Clear chat
    fun clearChatHistory() {
        _aiChatLog.value = listOf(
            Pair("تم إعادة تنشيط المساعد الذكي بنجاح. تفضل بطرح أي سؤال!", false)
        )
    }

    // Admin login
    fun loginSupervisor(usernameInput: String, passwordInput: String): Boolean {
        val queryUser = usernameInput.trim()
        val queryPass = passwordInput.trim()
        
        // Match in supervisor collection (includes real-time synced credentials)
        val match = supervisors.value.find { 
            it.username.equals(queryUser, ignoreCase = true) && it.password == queryPass 
        }
        if (match != null) {
            _currentAdmin.value = match
            _loginError.value = null
            return true
        } else {
            _loginError.value = "الاسم أو كلمة المرور غير صحيحة"
            return false
        }
    }

    fun logoutSupervisor() {
        _currentAdmin.value = null
        _loginError.value = null
    }

    // CRUD - Categories
    fun addCategory(category: Category) {
        viewModelScope.launch {
            repository.addCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    // CRUD - Providers
    fun addProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.addProvider(provider)
        }
    }

    fun updateProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.updateProvider(provider)
        }
    }

    fun togglePinProvider(provider: ServiceProvider) {
        updateProvider(provider.copy(is_pinned = !provider.is_pinned))
    }

    fun approveProvider(providerId: String) {
        val prov = providers.value.find { it.id == providerId }
        prov?.let {
            updateProvider(it.copy(is_approved = true))
        }
    }

    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            repository.deleteProvider(providerId)
        }
    }

    // CRUD - Reviews
    fun addReview(review: Review) {
        viewModelScope.launch {
            repository.addReview(review)
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            repository.deleteReview(reviewId)
        }
    }

    // CRUD - Supervisors
    fun addSupervisor(supervisor: Supervisor) {
        viewModelScope.launch {
            repository.addSupervisor(supervisor)
        }
    }

    fun deleteSupervisor(supervisorId: String) {
        viewModelScope.launch {
            repository.deleteSupervisor(supervisorId)
        }
    }

    // CRUD - AppConfig
    fun updateAppConfig(config: AppConfig) {
        viewModelScope.launch {
            repository.updateAppConfig(config)
        }
    }
}
