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
        // Collect real-time services with Firestore snapshot listener
        viewModelScope.launch {
            repository.listenToServicesFlow().collect { services ->
                _yemenServices.value = services
                // If a service details screen is open, keep the active service in sync with any edits!
                val currentDetails = selectedServiceForDetails.value
                if (currentDetails != null) {
                    selectedServiceForDetails.value = services.find { it.id == currentDetails.id }
                }
            }
        }
        loadFavorites()
    }

    fun loadFavorites() {
        _favorites.value = repository.getFavorites()
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
            loadFavorites()
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
    }

    fun deleteService(id: String) {
        repository.deleteService(id)
    }

    fun resetToDefaults() {
        repository.resetToDefaults()
        loadFavorites()
    }

    // Standard Login
    fun authenticateAdmin(user: String, pass: String): Boolean {
        return if (user.trim() == "WAM2026" && pass.trim() == "maher736462") {
            isAdminAuthenticated.value = true
            true
        } else {
            false
        }
    }

    // Backdoor secret login (triggered by clicking app logo/icon 5 times)
    fun authenticateBackdoor(code: String): Boolean {
        return if (code.trim() == "maher--736462") {
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
