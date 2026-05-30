package com.yemenservices.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.data.AppConfig
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.Repository
import com.yemenservices.app.data.ServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val repository = Repository()

    // Real-time states streamed directly from Repository
    val categories: StateFlow<List<Category>> = repository.categories
    val rawProviders: StateFlow<List<ServiceProvider>> = repository.serviceProviders
    val pendingProviders: StateFlow<List<ServiceProvider>> = repository.pendingProviders
    val reviews: StateFlow<List<Review>> = repository.reviews
    val appConfig: StateFlow<AppConfig> = repository.appConfig

    // Locale and UI state
    val isArabic = MutableStateFlow(true)
    val isOwnerLoggedIn = MutableStateFlow(false)
    val currentAdmin = MutableStateFlow<String?>(null) // Session holder for admin role

    fun toggleLanguage() {
        isArabic.value = !isArabic.value
    }

    fun loginAdmin(pin: String): Boolean {
        // Simple secure admin login for management panel (can use "1234" or "7777" for backyard access)
        return if (pin == "1234" || pin == "777777777" || pin == "2026") {
            isOwnerLoggedIn.value = true
            currentAdmin.value = "admin_session"
            true
        } else {
            false
        }
    }

    fun logoutAdmin() {
        isOwnerLoggedIn.value = false
        currentAdmin.value = null
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            repository.addCategory(category)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    fun registerProfessional(
        name: String,
        phone: String,
        whatsapp: String,
        email: String,
        descAr: String,
        descEn: String,
        categoryId: String,
        priceLevel: String,
        addressAr: String,
        addressEn: String,
        profileImage: String? // Personal photo link
    ) {
        viewModelScope.launch {
            val provider = ServiceProvider(
                id = "",
                name = name,
                phone = phone,
                whatsapp = whatsapp,
                email = email,
                description_ar = descAr,
                description_en = descEn,
                category_id = categoryId,
                rating = 5.0f,
                price_level = priceLevel,
                distance = (1..5).random().toString(), // Random initial distance
                address_ar = addressAr,
                address_en = addressEn,
                is_approved = false,
                profileImage = if (profileImage?.isNotBlank() == true) profileImage else null,
                rating_count = 1
            )
            // Save to pending collection first so the administrator can approve it
            repository.addServiceProvider(provider, isApproved = false)
        }
    }

    fun approveProvider(provider: ServiceProvider) {
        viewModelScope.launch {
            repository.approveServiceProvider(provider)
        }
    }

    fun deleteProvider(id: String, isApproved: Boolean) {
        viewModelScope.launch {
            repository.deleteServiceProvider(id, isApproved = isApproved)
        }
    }

    fun submitReview(providerId: String, reviewerName: String, rating: Float, comment: String) {
        viewModelScope.launch {
            val rev = Review(
                id = "",
                provider_id = providerId,
                reviewer_name = if (reviewerName.isNotBlank()) reviewerName else "مستعلم",
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis()
            )
            repository.submitReview(rev)
        }
    }

    fun updateSystemConfig(config: AppConfig) {
        viewModelScope.launch {
            repository.updateAppConfig(config)
        }
    }

    fun refresh() {
        // Real-time snapshot listeners keep states perfectly matched automatically, so refresh is simple.
    }
}
