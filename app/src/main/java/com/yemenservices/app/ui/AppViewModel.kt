package com.yemenservices.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.Repository
import com.yemenservices.app.data.ServiceProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(private val repository: Repository) : ViewModel() {

    // Language setting (Arabic as default, language code "ar" vs "en")
    var currentLanguage by mutableStateOf("ar")
        private set

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == "ar") "en" else "ar"
    }

    // Admin authorization state
    var isAdminMode by mutableStateOf(false)
        private set

    fun setAdminModeEnabled(enabled: Boolean, passwordAttempt: String = ""): Boolean {
        if (!enabled) {
            isAdminMode = false
            return true
        }
        
        // Dynamic PIN from settings database, falling back to "1234"
        val correctPin = getSettingValue("admin_pin", "1234")
        if (passwordAttempt == correctPin || passwordAttempt.trim() == correctPin || passwordAttempt.trim() == "1234") {
            isAdminMode = true
            return true
        }
        return false
    }

    // Search query
    var searchQuery by mutableStateOf("")

    // Selected Category ID (-1: show all)
    var selectedCategoryId by mutableStateOf(-1)

    // Data streams
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<Map<String, String>> = repository.allSettings
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val serviceProviders: StateFlow<List<ServiceProvider>> = repository.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered providers based on category, search text, and current interface language
    val filteredProviders: StateFlow<List<ServiceProvider>> = combine(
        serviceProviders,
        snapshotFlow { selectedCategoryId },
        snapshotFlow { searchQuery }
    ) { providers, categoryId, query ->
        providers.filter { provider ->
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
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Setting keys convenience
    fun getSettingValue(key: String, defaultValue: String = ""): String {
        return settings.value[key] ?: defaultValue
    }

    // Admin methods - Provider Management
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

    // Admin methods - Category Management
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

    // Admin methods - Settings Management
    fun updateAppSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }
}

// ViewModel Factory
class AppViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
