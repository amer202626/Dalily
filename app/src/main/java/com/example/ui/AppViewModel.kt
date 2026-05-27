package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DaliliApplication
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DaliliApplication
    private val repository = app.repository
    private val settingsManager = app.settingsManager

    // Dynamic Live Theme Settings
    var currentLanguage by mutableStateOf(settingsManager.language)
        private set

    var appNameAr by mutableStateOf(settingsManager.appNameAr)
        private set

    var appNameEn by mutableStateOf(settingsManager.appNameEn)
        private set

    var primaryColorHex by mutableStateOf(settingsManager.primaryColor)
        private set

    var secondaryColorHex by mutableStateOf(settingsManager.secondaryColor)
        private set

    var iconLetterAr by mutableStateOf(settingsManager.iconLetterAr)
        private set

    var iconLetterEn by mutableStateOf(settingsManager.iconLetterEn)
        private set

    var footerText by mutableStateOf(settingsManager.footerText)
        private set

    var defaultLanguage by mutableStateOf(settingsManager.defaultLanguage)
        private set

    var remoteAdmins by mutableStateOf<List<Admin>>(emptyList())
        private set

    var adminsList by mutableStateOf<Set<String>>(emptySet())
        private set

    var loggedInUser by mutableStateOf(settingsManager.currentUser)
        private set

    // Backdoor Authentication State
    var isOwnerModeActive by mutableStateOf(false)
        private set

    // DB Flows
    val categoriesList: StateFlow<List<Category>> = repository.categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val serviceProvidersList: StateFlow<List<ServiceProvider>> = repository.serviceProviders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Trigger background initial internet sync
        viewModelScope.launch {
            repository.syncWithSupabase()
            syncAdmins()
        }
    }

    fun syncAdmins() {
        viewModelScope.launch {
            performSyncAdmins()
        }
    }

    suspend fun performSyncAdmins() {
        try {
            var fetched = repository.getAdmins()
            
            // Check if admin is present. If not, generate and insert default admin
            if (fetched.none { it.username == "admin" }) {
                val defaultAdmin = Admin(
                    id = java.util.UUID.randomUUID().toString(),
                    username = "admin",
                    passwordHash = "maher736462",
                    role = "super_admin",
                    isActive = true
                )
                try {
                    repository.createAdmin(defaultAdmin)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Fetch again to pick up the newly created admin
                val subFetched = repository.getAdmins()
                fetched = if (subFetched.any { it.username == "admin" }) {
                    subFetched
                } else {
                    fetched + defaultAdmin
                }
            }
            
            // Filter out empty usernames
            fetched = fetched.filter { it.username.isNotBlank() }
            
            remoteAdmins = fetched
            adminsList = fetched.map { it.username }.toSet()
        } catch (e: Exception) {
            e.printStackTrace()
            // Ensure local fallback list of admins is never empty
            if (remoteAdmins.isEmpty()) {
                val localAdmin = Admin(
                    id = "local_admin_id",
                    username = "admin",
                    passwordHash = "maher736462",
                    role = "super_admin",
                    isActive = true
                )
                remoteAdmins = listOf(localAdmin)
                adminsList = setOf("admin")
            }
        }
    }

    // Toggle Language
    fun toggleLanguage() {
        val nextLanguage = if (currentLanguage == "ar") "en" else "ar"
        currentLanguage = nextLanguage
        settingsManager.language = nextLanguage
    }

    // Dynamic configuration updates from Owner Dashboard
    fun updateAppConfig(
        arName: String,
        enName: String,
        pColor: String,
        sColor: String,
        iconAr: String,
        iconEn: String,
        footer: String,
        defLang: String
    ) {
        appNameAr = arName
        settingsManager.appNameAr = arName

        appNameEn = enName
        settingsManager.appNameEn = enName

        primaryColorHex = pColor
        settingsManager.primaryColor = pColor

        secondaryColorHex = sColor
        settingsManager.secondaryColor = sColor

        iconLetterAr = iconAr
        settingsManager.iconLetterAr = iconAr

        iconLetterEn = iconEn
        settingsManager.iconLetterEn = iconEn

        footerText = footer
        settingsManager.footerText = footer

        defaultLanguage = defLang
        settingsManager.defaultLanguage = defLang
    }

    // Admin user control
    fun addAdminUser(username: String, passwordInitial: String) {
        val trimmed = username.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                val newAdmin = Admin(
                    id = java.util.UUID.randomUUID().toString(),
                    username = trimmed,
                    passwordHash = passwordInitial,
                    role = if (trimmed == "admin" || trimmed == "general_manager") "super_admin" else "admin",
                    isActive = true
                )
                
                // 1. Instant UI update (Optimistic Update)
                val updatedList = remoteAdmins.filterNot { it.username == trimmed } + newAdmin
                remoteAdmins = updatedList
                adminsList = updatedList.map { it.username }.toSet()
                
                // 2. Persist to Supabase
                try {
                    repository.createAdmin(newAdmin)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // 3. Re-sync to verify and align with database state
                performSyncAdmins()
            }
        }
    }

    fun updateAdminPassword(username: String, newPassword: String) {
        val trimmed = username.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                // 1. Instant UI update (Optimistic Update)
                val updatedList = remoteAdmins.map {
                    if (it.username == trimmed) {
                        it.copy(passwordHash = newPassword)
                    } else {
                        it
                    }
                }
                remoteAdmins = updatedList
                
                // 2. Persist to Supabase
                try {
                    repository.updateAdminPassword(trimmed, newPassword)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // 3. Re-sync
                performSyncAdmins()
            }
        }
    }

    fun getAdminPassword(username: String): String {
        return remoteAdmins.find { it.username.trim() == username.trim() }?.passwordHash ?: ""
    }

    fun removeAdminUser(username: String) {
        val trimmed = username.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                // 1. Instant UI update (Optimistic Update)
                val updatedList = remoteAdmins.filterNot { it.username == trimmed }
                remoteAdmins = updatedList
                adminsList = updatedList.map { it.username }.toSet()
                
                // 2. Persist to Supabase
                try {
                    repository.deleteAdmin(trimmed)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // 3. Re-sync
                performSyncAdmins()
            }
        }
    }

    // User authentication with callback matching design requirements
    fun loginUser(username: String, passwordEntered: String, onResult: (String?) -> Unit) {
        val trimmed = username.trim()
        val isArabic = currentLanguage == "ar"
        if (trimmed.isBlank()) {
            onResult(if (isArabic) "اسم المستخدم لا يمكن أن يكون فارغاً" else "Username cannot be blank")
            return
        }
        
        viewModelScope.launch {
            try {
                // Try to perform a fast sync first to get latest admins from database
                try {
                    performSyncAdmins()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                
                var currentAdmins = remoteAdmins
                if (currentAdmins.none { it.username == "admin" }) {
                    val defaultAdmin = Admin(
                        id = java.util.UUID.randomUUID().toString(),
                        username = "admin",
                        passwordHash = "maher736462",
                        role = "super_admin",
                        isActive = true
                    )
                    try {
                        repository.createAdmin(defaultAdmin)
                        performSyncAdmins()
                        currentAdmins = remoteAdmins
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Master super admin backdoor override check
                if (trimmed == "admin" && passwordEntered == "maher736462") {
                    loggedInUser = "admin"
                    settingsManager.currentUser = "admin"
                    isOwnerModeActive = true
                    
                    // Make sure the general manager admin is present with the correct password hash in DB
                    val mainAdmin = currentAdmins.find { it.username == "admin" }
                    if (mainAdmin == null) {
                        val defaultAdmin = Admin(
                            id = java.util.UUID.randomUUID().toString(),
                            username = "admin",
                            passwordHash = "maher736462",
                            role = "super_admin",
                            isActive = true
                        )
                        viewModelScope.launch {
                            try {
                                repository.createAdmin(defaultAdmin)
                                performSyncAdmins()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    } else if (mainAdmin.passwordHash != "maher736462") {
                        // Reset/update password in Supabase if it differs from the expected super admin key
                        viewModelScope.launch {
                            try {
                                repository.updateAdminPassword("admin", "maher736462")
                                performSyncAdmins()
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                    onResult(null) // success
                    return@launch
                }
                
                val foundAdmin = currentAdmins.find { it.username == trimmed }
                if (foundAdmin != null && foundAdmin.passwordHash == passwordEntered) {
                    loggedInUser = trimmed
                    settingsManager.currentUser = trimmed
                    if (trimmed == "admin") {
                        isOwnerModeActive = true
                    }
                    onResult(null) // success
                } else {
                    onResult(if (isArabic) "خطأ في بيانات الدخول" else "Incorrect username or password")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Emergency fallback if completely offline but they typed admin maher736462
                if (trimmed == "admin" && passwordEntered == "maher736462") {
                    loggedInUser = "admin"
                    settingsManager.currentUser = "admin"
                    isOwnerModeActive = true
                    onResult(null)
                } else {
                    onResult(if (isArabic) "فشل الاتصال. تحقق من الاتصال بالإنترنت." else "Connection failed. Check your internet connection.")
                }
            }
        }
    }

    fun logoutUser() {
        loggedInUser = null
        settingsManager.currentUser = null
    }

    // Verification of the backdoor password with callback
    fun verifyBackdoorPassword(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val admins = repository.getAdmins()
                val mainAdmin = admins.find { it.username == "admin" }
                val correctPwd = mainAdmin?.passwordHash ?: "maher736462"
                
                if (password == correctPwd || password == "maher736462") {
                    loggedInUser = "admin"
                    settingsManager.currentUser = "admin"
                    isOwnerModeActive = true
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                // local fallback if offline
                if (password == "maher736462") {
                    loggedInUser = "admin"
                    settingsManager.currentUser = "admin"
                    isOwnerModeActive = true
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
        }
    }

    fun exitOwnerMode() {
        isOwnerModeActive = false
    }

    // Category CRUD
    fun addCategory(id: String, nameAr: String, nameEn: String, icon: String, orderIndex: Int, imageUrl: String? = null) {
        viewModelScope.launch {
            val category = Category(id, nameAr, nameEn, icon, orderIndex, true, imageUrl)
            repository.saveCategory(category)
        }
    }

    fun updateCategory(id: String, nameAr: String, nameEn: String, icon: String, orderIndex: Int, imageUrl: String? = null) {
        viewModelScope.launch {
            repository.updateCategory(id, nameAr, nameEn, icon, orderIndex, imageUrl)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // ServiceProvider CRUD
    fun addServiceProvider(id: String, nameAr: String, nameEn: String, phone: String, categoryId: String, rating: Float, imageUrl: String?) {
        viewModelScope.launch {
            val provider = ServiceProvider(id, nameAr, nameEn, phone, categoryId, rating, true, imageUrl)
            repository.saveServiceProvider(provider)
        }
    }

    fun updateServiceProvider(id: String, nameAr: String, nameEn: String, phone: String, categoryId: String, rating: Float, imageUrl: String?) {
        viewModelScope.launch {
            repository.updateServiceProvider(id, nameAr, nameEn, phone, categoryId, rating, imageUrl)
        }
    }

    fun deleteServiceProvider(id: String) {
        viewModelScope.launch {
            repository.deleteServiceProvider(id)
        }
    }
}
