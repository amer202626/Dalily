package com.yemenservices.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.data.Repository
import com.yemenservices.app.data.YemenService
import com.yemenservices.app.data.ServiceCategory
import com.yemenservices.app.data.ServiceSubCategory
import com.yemenservices.app.data.JoinApplication
import com.yemenservices.app.data.GeminiService
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
    SmartAssistant,
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
    val selectedSubCategory = MutableStateFlow<String?>(null)

    private val _yemenServices = MutableStateFlow<List<YemenService>>(emptyList())
    val yemenServices: StateFlow<List<YemenService>> = _yemenServices

    private val _categories = MutableStateFlow<List<ServiceCategory>>(emptyList())
    val categories: StateFlow<List<ServiceCategory>> = combine(_categories, isArabic) { cats, isAr ->
        cats.sortedWith(compareByDescending<ServiceCategory> { it.isPinned }
            .thenBy { it.orderIndex }
            .thenBy { if (isAr) it.nameAr else it.nameEn })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _subCategories = MutableStateFlow<List<ServiceSubCategory>>(emptyList())
    val subCategories: StateFlow<List<ServiceSubCategory>> = combine(_subCategories, isArabic) { subs, isAr ->
        subs.sortedWith(compareBy<ServiceSubCategory> { it.orderIndex }
            .thenBy { if (isAr) it.nameAr else it.nameEn })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _joinApplications = MutableStateFlow<List<JoinApplication>>(emptyList())
    val joinApplications: StateFlow<List<JoinApplication>> = _joinApplications

    val welcomeConfig = MutableStateFlow(com.yemenservices.app.data.WelcomeConfig())

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    val selectedServiceForDetails = MutableStateFlow<YemenService?>(null)
    val activeComments = MutableStateFlow<List<com.yemenservices.app.data.ServiceComment>>(emptyList())
    private var commentsJob: kotlinx.coroutines.Job? = null

    fun selectServiceForDetails(service: YemenService?) {
        selectedServiceForDetails.value = service
        commentsJob?.cancel()
        if (service != null) {
            commentsJob = viewModelScope.launch {
                repository.listenToCommentsFlow(service.id).collect { comments ->
                    activeComments.value = comments
                }
            }
        } else {
            activeComments.value = emptyList()
        }
    }

    val isAdminAuthenticated = MutableStateFlow(false)

    // Filter and Sort logic
    val filteredServices: StateFlow<List<YemenService>> = combine(
        _yemenServices,
        searchQuery,
        selectedCategory,
        selectedSubCategory,
        isArabic
    ) { services, query, category, subCat, isAr ->
        services.filter { service ->
            val matchCategory = category == null || service.category == category
            val matchSubCategory = subCat == null || service.subCategory == subCat
            val matchQuery = query.isBlank() || 
                service.nameAr.contains(query, ignoreCase = true) ||
                service.nameEn.contains(query, ignoreCase = true) ||
                service.phoneNumber.contains(query, ignoreCase = true) ||
                service.descriptionAr.contains(query, ignoreCase = true) ||
                service.descriptionEn.contains(query, ignoreCase = true)
            matchCategory && matchSubCategory && matchQuery
        }.sortedWith(compareByDescending<YemenService> { it.isPinned }
            .thenBy { it.orderIndex }
            .thenBy { if (isAr) it.nameAr else it.nameEn })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect real-time services with Firestore snapshot listener
        viewModelScope.launch {
            repository.listenToServicesFlow().collect { services ->
                _yemenServices.value = services
                // If a service details screen is open, keep the active service in sync with any edits!
                val currentDetails = selectedServiceForDetails.value
                if (currentDetails != null) {
                    val updated = services.find { it.id == currentDetails.id }
                    if (updated != null && updated != currentDetails) {
                        selectedServiceForDetails.value = updated
                    }
                }
            }
        }

        // Collect real-time categories with Firestore snapshot listener
        viewModelScope.launch {
            repository.listenToCategoriesFlow().collect { cats ->
                _categories.value = cats
            }
        }

        // Collect real-time sub-categories
        viewModelScope.launch {
            repository.listenToSubCategoriesFlow().collect { subs ->
                _subCategories.value = subs
            }
        }

        // Collect real-time Welcome configuration
        viewModelScope.launch {
            repository.listenToWelcomeConfigFlow().collect { config ->
                welcomeConfig.value = config
            }
        }

        // Collect real-time Join applications
        viewModelScope.launch {
            repository.listenToJoinApplicationsFlow().collect { apps ->
                _joinApplications.value = apps
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
        subCategory: String,
        phone: String,
        whatsapp: String,
        addressAr: String,
        addressEn: String,
        descriptionAr: String,
        descriptionEn: String,
        imageUrl: String = "",
        isPinned: Boolean = false,
        isRecommended: Boolean = false,
        orderIndex: Int = 0
    ) {
        val finalId = id ?: UUID.randomUUID().toString()
        val service = YemenService(
            id = finalId,
            nameAr = nameAr,
            nameEn = nameEn,
            category = category,
            subCategory = subCategory,
            phoneNumber = phone,
            whatsappNumber = whatsapp,
            addressAr = addressAr,
            addressEn = addressEn,
            descriptionAr = descriptionAr,
            descriptionEn = descriptionEn,
            imageUrl = imageUrl,
            rating = 4.5f,
            isPinned = isPinned,
            isRecommended = isRecommended,
            orderIndex = orderIndex
        )
        repository.saveService(service)
    }

    fun updateServiceRating(serviceId: String, newRating: Float) {
        viewModelScope.launch {
            val s = _yemenServices.value.find { it.id == serviceId } ?: return@launch
            repository.saveService(s.copy(rating = newRating))
        }
    }

    fun deleteService(id: String) {
        repository.deleteService(id)
    }

    fun saveCategory(category: ServiceCategory) {
        repository.saveCategory(category)
    }

    fun deleteCategory(id: String) {
        repository.deleteCategory(id)
    }

    fun saveSubCategory(sub: ServiceSubCategory) {
        repository.saveSubCategory(sub)
    }

    fun deleteSubCategory(id: String) {
        repository.deleteSubCategory(id)
    }

    fun saveWelcomeConfig(config: com.yemenservices.app.data.WelcomeConfig) {
        repository.saveWelcomeConfig(config)
    }

    fun addComment(serviceId: String, authorName: String, text: String, rating: Float) {
        val comment = com.yemenservices.app.data.ServiceComment(
            id = UUID.randomUUID().toString(),
            serviceId = serviceId,
            authorName = authorName,
            commentText = text,
            rating = rating,
            timestamp = System.currentTimeMillis()
        )
        repository.saveComment(comment)
    }

    fun deleteComment(id: String) {
        repository.deleteComment(id)
    }

    fun updateComment(comment: com.yemenservices.app.data.ServiceComment) {
        repository.saveComment(comment)
    }

    // Join requests Submission
    fun submitJoinApplication(
        name: String,
        phone: String,
        region: String,
        categoryId: String,
        subCategoryId: String,
        logoUrl: String
    ) {
        val app = JoinApplication(
            id = UUID.randomUUID().toString(),
            name = name,
            phone = phone,
            region = region,
            categoryId = categoryId,
            subCategoryId = subCategoryId,
            logoUrl = logoUrl,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )
        repository.saveJoinApplication(app)
    }

    // Admin Accept or Reject requests
    fun updateJoinApplicationStatus(appId: String, status: String) {
        viewModelScope.launch {
            val app = _joinApplications.value.find { it.id == appId } ?: return@launch
            val updatedApp = app.copy(status = status)
            repository.saveJoinApplication(updatedApp)

            if (status == "approved") {
                // If approved, create a real active provider automatically!
                val service = YemenService(
                    id = UUID.randomUUID().toString(),
                    nameAr = updatedApp.name,
                    nameEn = updatedApp.name,
                    category = updatedApp.categoryId,
                    subCategory = updatedApp.subCategoryId,
                    phoneNumber = updatedApp.phone,
                    whatsappNumber = "",
                    addressAr = "اليمن - ${updatedApp.region}",
                    addressEn = "Yemen - ${updatedApp.region}",
                    rating = 5.0f,
                    imageUrl = updatedApp.logoUrl.ifBlank { "https://images.unsplash.com/photo-1521791136368-1a46827d0adf?w=400" },
                    descriptionAr = "مزود خدمة يمني معتمد تم قبوله وتفعيله عبر طلب انضمام رسمي.",
                    descriptionEn = "Verified Yemeni service provider approved and listed from join applications.",
                    isPinned = false,
                    isRecommended = true,
                    orderIndex = 0
                )
                repository.saveService(service)
            }
        }
    }

    fun deleteJoinApplication(id: String) {
        repository.deleteJoinApplication(id)
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

    // Gemini Chat Response Call
    suspend fun getGeminiReply(query: String): String {
        // Build a highly rich context representation of all main categories, subcategories and active providers
        val contextBuilder = StringBuilder()
        contextBuilder.append("الدليل يحتوي على الأقسام التالية: ")
        _categories.value.forEach { contextBuilder.append("${it.nameAr} (${it.id}), ") }
        contextBuilder.append("\nالفروع التخصصية: ")
        _subCategories.value.forEach { contextBuilder.append("${it.nameAr} تابع لـ ${it.parentId}, ") }
        contextBuilder.append("\nمزودي الخدمات الفعليين: ")
        _yemenServices.value.forEach {
            contextBuilder.append("${it.nameAr} (رقم الهاتف: ${it.phoneNumber}, الفرع: ${it.subCategory}, التقييم: ${it.rating}), ")
        }
        return GeminiService.getGeminiReply(query, contextBuilder.toString())
    }
}
