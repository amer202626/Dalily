package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    
    val settingsManager = SettingsManager(application)
    val repository = Repository(
        database.categoryDao(),
        database.serviceProviderDao(),
        database.reviewDao()
    )

    companion object {
        private const val TAG = "AppViewModel"
    }

    // Language State
    var language by mutableStateOf(settingsManager.language)
        private set

    fun toggleLanguage() {
        language = if (language == "ar") "en" else "ar"
        settingsManager.language = language
    }

    // Sync States
    var isSyncing by mutableStateOf(false)
        private set
    var lastSyncSuccess by mutableStateOf(true)
        private set

    // Admin State & Backdoor
    var logoTapCount by mutableStateOf(0)
        private set
    var isBackdoorUnlocked by mutableStateOf(false)
        private set
    var isAdminLoggedIn by mutableStateOf(false)
        private set
    var adminUser by mutableStateOf<Admin?>(null)
        private set

    // Local admin list for login matching
    var adminList by mutableStateOf<List<Admin>>(emptyList())
        private set

    // Observe Room data sources
    val categories: StateFlow<List<Category>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val serviceProviders: StateFlow<List<ServiceProvider>> = repository.serviceProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected state
    var selectedCategoryId by mutableStateOf<String?>(null)
    
    // Observed reviews for a provider
    private val _currentProviderReviews = MutableStateFlow<List<Review>>(emptyList())
    val currentProviderReviews: StateFlow<List<Review>> = _currentProviderReviews

    init {
        // Start real-time Firestore synchronization listeners
        Log.d(TAG, "Initializing Firestore Snapshot Listeners...")
        try {
            repository.listenToFirestoreCategories()
            repository.listenToFirestoreServiceProviders()
            repository.listenToFirestoreReviews()
            Log.d(TAG, "Firestore Listeners successfully started.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed launching Firestore snapshot listeners: ${e.message}")
        }

        // Perform initial pull sync and seeding from Firestore
        syncData(force = false)
        loadAdmins()
    }

    fun handleLogoTap() {
        logoTapCount++
        if (logoTapCount >= 5) {
            isBackdoorUnlocked = true
            logoTapCount = 0
            Log.d(TAG, "Backdoor unlocked via logo 5-tap sequence!")
        }
    }

    fun resetBackdoor() {
        isBackdoorUnlocked = false
        logoTapCount = 0
    }

    fun attemptAdminLogin(user: String, pass: String): Boolean {
        // Hardcoded standard credentials as requested
        if (user == "admin" && pass == "maher736462") {
            isAdminLoggedIn = true
            adminUser = Admin("admin", "", "SuperAdmin", true)
            return true
        }
        
        // Also match against fetched DB admins
        val matched = adminList.find { it.username == user && it.passwordHash == pass }
        if (matched != null) {
            isAdminLoggedIn = true
            adminUser = matched
            return true
        }
        return false
    }

    fun logoutAdmin() {
        isAdminLoggedIn = false
        adminUser = null
    }

    fun syncData(force: Boolean) {
        val now = System.currentTimeMillis()
        val lastSync = settingsManager.lastSyncTimestamp
        
        // Anti-throttle buffer (1 minute) unless forced
        if (force || (now - lastSync > 60 * 1000L)) {
            viewModelScope.launch {
                isSyncing = true
                try {
                    repository.syncWithFirestore(settingsManager)
                    settingsManager.lastSyncTimestamp = System.currentTimeMillis()
                    lastSyncSuccess = true
                    Log.d(TAG, "Firestore synchronization successfully executed.")
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore synchronization failed: ${e.message}")
                    lastSyncSuccess = false
                } finally {
                    isSyncing = false
                }
            }
        }
    }

    fun loadAdmins() {
        viewModelScope.launch {
            try {
                val list = repository.getAdmins()
                if (list.isNotEmpty()) {
                    adminList = list
                    settingsManager.saveAdminsLocal(list)
                } else {
                    adminList = settingsManager.getAdminsLocal()
                }
            } catch (e: Exception) {
                adminList = settingsManager.getAdminsLocal()
            }
        }
    }

    fun loadReviewsForProvider(providerId: String) {
        viewModelScope.launch {
            repository.getReviewsForProvider(providerId).collect {
                _currentProviderReviews.value = it
            }
        }
    }

    // --- CRUD Category operations ---
    fun addCategory(id: String, nameAr: String, nameEn: String, icon: String, orderIndex: Int, imageUrl: String?) {
        viewModelScope.launch {
            val cat = Category(id.ifBlank { UUID.randomUUID().toString() }, nameAr, nameEn, icon, orderIndex, true, imageUrl)
            repository.saveCategory(cat)
        }
    }

    fun editCategory(id: String, nameAr: String, nameEn: String, icon: String, orderIndex: Int, imageUrl: String?) {
        viewModelScope.launch {
            repository.updateCategory(id, nameAr, nameEn, icon, orderIndex, imageUrl)
        }
    }

    fun removeCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // --- CRUD Service Provider operations ---
    fun addServiceProvider(nameAr: String, nameEn: String, phone: String, categoryId: String, imageUrl: String?) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val provider = ServiceProvider(id, nameAr, nameEn, phone, categoryId, 5.0f, true, imageUrl)
            repository.saveServiceProvider(provider)
        }
    }

    fun editServiceProvider(id: String, nameAr: String, nameEn: String, phone: String, categoryId: String, rating: Float, imageUrl: String?) {
        viewModelScope.launch {
            repository.updateServiceProvider(id, nameAr, nameEn, phone, categoryId, rating, imageUrl)
        }
    }

    fun removeServiceProvider(id: String) {
        viewModelScope.launch {
            repository.deleteServiceProvider(id)
        }
    }

    // --- Reviews system ---
    fun addReview(providerId: String, userName: String, comment: String, rating: Float) {
        viewModelScope.launch {
            val reviewId = UUID.randomUUID().toString()
            val createdAt = getCurrentTimeIso()
            
            val review = Review(reviewId, providerId, userName, comment, rating, createdAt)
            repository.saveReview(review)
            
            // Recalculate provider overall average rating
            recalculateProviderRating(providerId)
        }
    }

    private suspend fun recalculateProviderRating(providerId: String) {
        try {
            val localReviews = database.reviewDao().getReviewsForProviderDirect(providerId)
            val providers = serviceProviders.value
            val matchedProv = providers.find { it.id == providerId }
            if (matchedProv != null && localReviews.isNotEmpty()) {
                val average = localReviews.map { it.rating }.average().toFloat()
                repository.updateServiceProvider(
                    matchedProv.id,
                    matchedProv.nameAr,
                    matchedProv.nameEn,
                    matchedProv.phone,
                    matchedProv.categoryId,
                    average,
                    matchedProv.imageUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed recalculating provider average rating: ${e.message}")
        }
    }

    // --- Admin DB Management ---
    fun addAdminInDb(username: String, pash: String, role: String) {
        viewModelScope.launch {
            val admin = Admin(username, pash, role, true)
            repository.createAdmin(admin)
            loadAdmins()
        }
    }

    fun editAdminPasswordInDb(username: String, newPash: String) {
        viewModelScope.launch {
            repository.updateAdminPassword(username, newPash)
            loadAdmins()
        }
    }

    fun deleteAdminInDb(username: String) {
        viewModelScope.launch {
            repository.deleteAdmin(username)
            loadAdmins()
        }
    }
}
