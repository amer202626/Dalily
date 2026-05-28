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

    var welcomeMessageAr by mutableStateOf(settingsManager.welcomeMessageAr)
        private set

    var welcomeMessageEn by mutableStateOf(settingsManager.welcomeMessageEn)
        private set

    var showWelcomeMessageInsteadOfLogo by mutableStateOf(settingsManager.showWelcomeMessageInsteadOfLogo)
        private set

    var customWelcomeLogoUrl by mutableStateOf(settingsManager.customWelcomeLogoUrl)
        private set

    var geminiApiKeySetting by mutableStateOf(settingsManager.geminiApiKeySetting)
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
        // Pre-populate state from local SharedPreferences so supervisors are instantly available offline on launch
        val localCached = settingsManager.getAdminsLocal()
        if (localCached.isNotEmpty() && remoteAdmins.isEmpty()) {
            val nonConfig = localCached.filter { it.username != "app_config" }
            remoteAdmins = nonConfig
            adminsList = nonConfig.map { it.username }.toSet()
        }

        try {
            var fetched = repository.getAdmins()
            
            // Check if admin is present. If not, generate and insert default admin
            if (fetched.none { it.username == "admin" }) {
                val defaultAdmin = Admin(
                    id = java.util.UUID.randomUUID().toString(),
                    username = "admin",
                    passwordHash = settingsManager.adminPasswordOverride ?: "maher736462",
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
            
            // Check for system app wide custom configuration inside Supabase Database
            val configRecord = fetched.find { it.username == "app_config" }
            if (configRecord != null) {
                val data = configRecord.passwordHash
                if (data.startsWith("CONFIG_V2||")) {
                    val parts = data.split("||")
                    if (parts.size >= 14) {
                        appNameAr = parts[1]
                        settingsManager.appNameAr = parts[1]
                        appNameEn = parts[2]
                        settingsManager.appNameEn = parts[2]
                        primaryColorHex = parts[3]
                        settingsManager.primaryColor = parts[3]
                        secondaryColorHex = parts[4]
                        settingsManager.secondaryColor = parts[4]
                        iconLetterAr = parts[5]
                        settingsManager.iconLetterAr = parts[5]
                        iconLetterEn = parts[6]
                        settingsManager.iconLetterEn = parts[6]
                        footerText = parts[7]
                        settingsManager.footerText = parts[7]
                        defaultLanguage = parts[8]
                        settingsManager.defaultLanguage = parts[8]
                        
                        welcomeMessageAr = parts[9]
                        settingsManager.welcomeMessageAr = parts[9]
                        welcomeMessageEn = parts[10]
                        settingsManager.welcomeMessageEn = parts[10]
                        val showInstead = parts[11].toBoolean()
                        showWelcomeMessageInsteadOfLogo = showInstead
                        settingsManager.showWelcomeMessageInsteadOfLogo = showInstead
                        customWelcomeLogoUrl = parts[12]
                        settingsManager.customWelcomeLogoUrl = parts[12]
                        geminiApiKeySetting = parts[13]
                        settingsManager.geminiApiKeySetting = parts[13]
                    }
                } else if (data.startsWith("CONFIG_V1||")) {
                    val parts = data.split("||")
                    if (parts.size >= 9) {
                        appNameAr = parts[1]
                        settingsManager.appNameAr = parts[1]
                        appNameEn = parts[2]
                        settingsManager.appNameEn = parts[2]
                        primaryColorHex = parts[3]
                        settingsManager.primaryColor = parts[3]
                        secondaryColorHex = parts[4]
                        settingsManager.secondaryColor = parts[4]
                        iconLetterAr = parts[5]
                        settingsManager.iconLetterAr = parts[5]
                        iconLetterEn = parts[6]
                        settingsManager.iconLetterEn = parts[6]
                        footerText = parts[7]
                        settingsManager.footerText = parts[7]
                        defaultLanguage = parts[8]
                        settingsManager.defaultLanguage = parts[8]
                    }
                }
            }
            
            val mergedList = fetched.toMutableList()
            // Merge local admins that might not have synchronized yet to make them 100% resilient
            localCached.forEach { local ->
                if (mergedList.none { it.username.trim() == local.username.trim() }) {
                    mergedList.add(local)
                }
            }

            // Apply master password override locally to prevent remote databases from reverting it
            val overridePwd = settingsManager.adminPasswordOverride
            val finalFetched = mergedList.map { adminRecord ->
                if (adminRecord.username == "admin" && overridePwd != null) {
                    adminRecord.copy(passwordHash = overridePwd)
                } else {
                    adminRecord
                }
            }

            // Exclude system config record from regular admins list displayed in UI
            val realAdminsList = finalFetched.filter { it.username != "app_config" }
            remoteAdmins = realAdminsList
            adminsList = realAdminsList.map { it.username }.toSet()

            // Update local cache
            settingsManager.saveAdminsLocal(finalFetched)
        } catch (e: Exception) {
            e.printStackTrace()
            // Ensure local fallback list of admins is never empty
            if (remoteAdmins.isEmpty()) {
                val localAdmin = Admin(
                    id = "local_admin_id",
                    username = "admin",
                    passwordHash = settingsManager.adminPasswordOverride ?: "maher736462",
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
        defLang: String,
        welcomeAr: String = "كل الخدمات في تطبيق واحد",
        welcomeEn: String = "All services in one app",
        showInstead: Boolean = false,
        logoUrl: String = "",
        gKey: String = ""
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

        welcomeMessageAr = welcomeAr
        settingsManager.welcomeMessageAr = welcomeAr

        welcomeMessageEn = welcomeEn
        settingsManager.welcomeMessageEn = welcomeEn

        showWelcomeMessageInsteadOfLogo = showInstead
        settingsManager.showWelcomeMessageInsteadOfLogo = showInstead

        customWelcomeLogoUrl = logoUrl
        settingsManager.customWelcomeLogoUrl = logoUrl

        geminiApiKeySetting = gKey
        settingsManager.geminiApiKeySetting = gKey

        // Save to remote Supabase database so it syncs with all other active device clients!
        viewModelScope.launch {
            try {
                val configData = "CONFIG_V2||$arName||$enName||$pColor||$sColor||$iconAr||$iconEn||$footer||$defLang||$welcomeAr||$welcomeEn||$showInstead||$logoUrl||$gKey"
                val response = repository.getAdmins()
                val existing = response.find { it.username == "app_config" }
                if (existing != null) {
                    repository.updateAdminPassword("app_config", configData)
                } else {
                    val configAdmin = Admin(
                        id = java.util.UUID.randomUUID().toString(),
                        username = "app_config",
                        passwordHash = configData,
                        role = "config",
                        isActive = true
                    )
                    repository.createAdmin(configAdmin)
                }
                // Sync admins after update to align state perfectly
                performSyncAdmins()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                
                // 1. Instant local cache update to guarantee offline availability
                val localList = settingsManager.getAdminsLocal().filterNot { it.username == trimmed } + newAdmin
                settingsManager.saveAdminsLocal(localList)

                // 2. Instant UI update (Optimistic Update)
                val updatedList = remoteAdmins.filterNot { it.username == trimmed } + newAdmin
                remoteAdmins = updatedList
                adminsList = updatedList.map { it.username }.toSet()
                
                // 3. Persist to Supabase
                try {
                    repository.createAdmin(newAdmin)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // 4. Re-sync to verify and align with database state
                performSyncAdmins()
            }
        }
    }

    fun updateAdminPassword(username: String, newPassword: String) {
        val trimmed = username.trim()
        if (trimmed == "admin" && (loggedInUser != "admin" && !isOwnerModeActive)) {
            // High Security Barrier: Block unauthorized clients/users from tampering with the master password
            return
        }
        if (trimmed.isNotBlank()) {
            if (trimmed == "admin") {
                settingsManager.adminPasswordOverride = newPassword
            }
            viewModelScope.launch {
                // 1. Instant local cache update
                val localList = settingsManager.getAdminsLocal().map {
                    if (it.username == trimmed) it.copy(passwordHash = newPassword) else it
                }
                settingsManager.saveAdminsLocal(localList)

                // 2. Instant UI update (Optimistic Update)
                val updatedList = remoteAdmins.map {
                    if (it.username == trimmed) {
                        it.copy(passwordHash = newPassword)
                    } else {
                        it
                    }
                }
                remoteAdmins = updatedList
                
                // 3. Persist to Supabase
                try {
                    repository.updateAdminPassword(trimmed, newPassword)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // 4. Re-sync
                performSyncAdmins()
            }
        }
    }

    fun getAdminPassword(username: String): String {
        return remoteAdmins.find { it.username.trim() == username.trim() }?.passwordHash ?: ""
    }

    fun removeAdminUser(username: String) {
        val trimmed = username.trim()
        if (trimmed == "admin" || trimmed == "app_config") {
            // Prevent removing the master owner account or the remote configuration container entry
            return
        }
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                // 1. Instant local cache update
                val localList = settingsManager.getAdminsLocal().filterNot { it.username == trimmed }
                settingsManager.saveAdminsLocal(localList)

                // 2. Instant UI update (Optimistic Update)
                val updatedList = remoteAdmins.filterNot { it.username == trimmed }
                remoteAdmins = updatedList
                adminsList = updatedList.map { it.username }.toSet()
                
                // 3. Persist to Supabase
                try {
                    repository.deleteAdmin(trimmed)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // 4. Re-sync
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
