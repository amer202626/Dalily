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

    var adminsList by mutableStateOf(settingsManager.admins)
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
            settingsManager.addAdmin(trimmed)
            settingsManager.setAdminPassword(trimmed, passwordInitial)
            adminsList = settingsManager.admins
        }
    }

    fun updateAdminPassword(username: String, newPassword: String) {
        val trimmed = username.trim()
        if (trimmed.isNotBlank()) {
            settingsManager.setAdminPassword(trimmed, newPassword)
        }
    }

    fun getAdminPassword(username: String): String {
        return settingsManager.getAdminPassword(username.trim())
    }

    fun removeAdminUser(username: String) {
        settingsManager.removeAdmin(username)
        adminsList = settingsManager.admins
    }

    // User authentication
    fun loginUser(username: String, passwordEntered: String): String? {
        val trimmed = username.trim()
        if (trimmed.isBlank()) return "Username cannot be blank"
        
        val admins = settingsManager.admins
        if (admins.contains(trimmed)) {
            val correctPwd = settingsManager.getAdminPassword(trimmed)
            if (passwordEntered == correctPwd) {
                loggedInUser = trimmed
                settingsManager.currentUser = trimmed
                if (trimmed == "admin") {
                    isOwnerModeActive = true
                }
                return null // success
            } else {
                return "WrongPassword"
            }
        } else {
            // Standard client user
            loggedInUser = trimmed
            settingsManager.currentUser = trimmed
            return null // success
        }
    }

    fun logoutUser() {
        loggedInUser = null
        settingsManager.currentUser = null
    }

    // Verification of the backdoor password
    fun verifyBackdoorPassword(password: String): Boolean {
        val masterPwd = settingsManager.getAdminPassword("admin")
        if (password == masterPwd || password == "maher736462") {
            loggedInUser = "admin"
            settingsManager.currentUser = "admin"
            isOwnerModeActive = true
            return true
        }
        return false
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
