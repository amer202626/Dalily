package com.yemenservices.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.data.Repository
import com.yemenservices.app.data.YemenService
import com.yemenservices.app.data.ServiceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    Home,
    ServicesList,
    ServiceDetails,
    Favorites,
    About,
    AdminDashboard
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)

    val isArabic = MutableStateFlow(true)
    val currentScreen = MutableStateFlow(AppScreen.Home)
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    
    private val _yemenServices = MutableStateFlow<List<YemenService>>(emptyList())
    val yemenServices: StateFlow<List<YemenService>> = _yemenServices

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    val selectedServiceForDetails = MutableStateFlow<YemenService?>(null)
    val isAdminAuthenticated = MutableStateFlow(false)

    // Filter logic
    val filteredServices: StateFlow<List<YemenService>> = combine(
        _yemenServices,
        searchQuery,
        selectedCategory
    ) { services, query, category ->
        services.filter { service ->
            val matchCategory = category == null || service.category == category
            val matchQuery = query.isBlank() || 
                service.nameAr.contains(query, ignoreCase = true) ||
                service.nameEn.contains(query, ignoreCase = true) ||
                service.phoneNumber.contains(query, ignoreCase = true) ||
                service.descriptionAr.contains(query, ignoreCase = true) ||
                service.descriptionEn.contains(query, ignoreCase = true)
            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: List<ServiceCategory> = repository.getCategories()

    init {
        loadData()
    }

    fun loadData() {
        _yemenServices.value = repository.getServices()
        _favorites.value = repository.getFavorites()
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
            _favorites.value = repository.getFavorites()
        }
    }

    fun isFavorite(id: String): Boolean {
        return _favorites.value.contains(id)
    }

    fun saveService(
        id: String?,
        nameAr: String,
        nameEn: String,
        category: String,
        phone: String,
        whatsapp: String,
        addressAr: String,
        addressEn: String,
        descriptionAr: String,
        descriptionEn: String
    ) {
        val finalId = id ?: UUID.randomUUID().toString()
        val service = YemenService(
            id = finalId,
            nameAr = nameAr,
            nameEn = nameEn,
            category = category,
            phoneNumber = phone,
            whatsappNumber = whatsapp,
            addressAr = addressAr,
            addressEn = addressEn,
            descriptionAr = descriptionAr,
            descriptionEn = descriptionEn,
            rating = 4.5f
        )
        repository.saveService(service)
        loadData()
    }

    fun deleteService(id: String) {
        repository.deleteService(id)
        loadData()
    }

    fun resetToDefaults() {
        repository.resetToDefaults()
        loadData()
    }

    fun authenticateAdmin(code: String): Boolean {
        return if (code.trim() == "ADMIN123" || code.trim() == "12345") {
            isAdminAuthenticated.value = true
            true
        } else {
            false
        }
    }

    fun logOutAdmin() {
        isAdminAuthenticated.value = false
        if (currentScreen.value == AppScreen.AdminDashboard) {
            currentScreen.value = AppScreen.Home
        }
    }
}
