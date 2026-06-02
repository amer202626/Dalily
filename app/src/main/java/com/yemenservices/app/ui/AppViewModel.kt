package com.yemenservices.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.DaliliApplication
import com.yemenservices.app.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as DaliliApplication).repository

    // --- SNAPSHOT DATA FLOWS FROM REPOSITORY ---
    val categories = repository.listenToCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val subCategories = repository.listenToSubCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val yemenServices = repository.listenToServicesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val joinRequests = repository.listenToJoinRequestsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val appConfig = repository.listenToConfigFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppConfig())

    val supervisorAccounts = repository.listenToSupervisorsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- OFFLINE STATE ---
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    // --- DYNAMIC SEARCH & FILTERING STATE ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<Category?>(null)
    val selectedSubCategory = MutableStateFlow<SubCategory?>(null)
    val isArabic = MutableStateFlow(true)

    // Combined filtered services for home search screen
    val filteredServices: StateFlow<List<YemenService>> = combine(
        yemenServices,
        searchQuery,
        selectedCategory,
        selectedSubCategory,
        categories,
        subCategories
    ) { services, query, category, subCat, cats, subs ->
        services.filter { service ->
            val matchCategory = category == null || service.category == category.id
            val matchSubCategory = subCat == null || service.subCategory == subCat.id

            val parentCat = cats.find { it.id == service.category }
            val subCatObj = subs.find { it.id == service.subCategory }

            val matchQuery = query.isBlank() ||
                service.nameAr.contains(query, ignoreCase = true) ||
                service.nameEn.contains(query, ignoreCase = true) ||
                service.descriptionAr.contains(query, ignoreCase = true) ||
                service.descriptionEn.contains(query, ignoreCase = true) ||
                service.addressAr.contains(query, ignoreCase = true) ||
                service.addressEn.contains(query, ignoreCase = true) ||
                service.workPlace.contains(query, ignoreCase = true) ||
                service.residencePlace.contains(query, ignoreCase = true) ||
                (parentCat != null && (parentCat.nameAr.contains(query, ignoreCase = true) || parentCat.nameEn.contains(query, ignoreCase = true))) ||
                (subCatObj != null && (subCatObj.nameAr.contains(query, ignoreCase = true) || subCatObj.nameEn.contains(query, ignoreCase = true)))

            matchCategory && matchSubCategory && matchQuery
        }.sortedWith(compareByDescending<YemenService> { it.isPinned }
            .thenBy { it.orderIndex }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- COMMENTS LOGIC ---
    private val _activeComments = MutableStateFlow<List<Comment>>(emptyList())
    val activeComments: StateFlow<List<Comment>> = _activeComments.asStateFlow()
    private var commentsJob: Job? = null

    // --- SMART CHAT ASSISTANT STATE ---
    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList()) // Pair of Text to IsUser
    val chatHistory: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // --- ADMIN AUTH STATE ---
    val authenticatedSupervisor = MutableStateFlow<SupervisorAccount?>(null)

    init {
        // Load favorite items offline
        _favorites.value = repository.getFavorites()
    }

    // Toggle Favorites
    fun toggleFavorite(serviceId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(serviceId)) {
            current.remove(serviceId)
        } else {
            current.add(serviceId)
        }
        _favorites.value = current
        repository.saveFavorites(current)
    }

    // Load comments dynamically via Snapshot Listener when looking up a specific service
    fun loadComments(serviceId: String) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            repository.listenToCommentsFlow(serviceId).collect {
                _activeComments.value = it
            }
        }
    }

    fun addComment(serviceId: String, author: String, text: String, rating: Float) {
        viewModelScope.launch {
            val comment = Comment(
                id = "",
                serviceId = serviceId,
                userName = author.ifBlank { if (isArabic.value) "زائر مجهول" else "Anonymous Guest" },
                comment = text,
                rating = rating,
                timestamp = System.currentTimeMillis()
            )
            repository.saveComment(comment)

            // Automate update of the service aggregated rating
            val servicesList = yemenServices.value
            val currentService = servicesList.find { it.id == serviceId }
            if (currentService != null) {
                val existingComments = _activeComments.value.toMutableList()
                existingComments.add(comment)
                val avg = if (existingComments.isEmpty()) rating else existingComments.map { it.rating }.average().toFloat()
                repository.saveService(currentService.copy(rating = avg))
            }
        }
    }

    // --- MANAGE APP CONFIGS ---
    fun updateAppConfig(config: AppConfig) {
        viewModelScope.launch {
            repository.saveAppConfig(config)
        }
    }

    // --- ADMIN / SERVICE PROVIDER ACTIONS ---
    fun saveService(service: YemenService) {
        viewModelScope.launch {
            repository.saveService(service)
        }
    }

    fun deleteService(id: String) {
        viewModelScope.launch {
            repository.deleteService(id)
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            repository.saveCategory(category)
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    fun saveSubCategory(subCategory: SubCategory) {
        viewModelScope.launch {
            repository.saveSubCategory(subCategory)
        }
    }

    fun deleteSubCategory(id: String) {
        viewModelScope.launch {
            repository.deleteSubCategory(id)
        }
    }

    // --- JOIN REQUEST ACTIONS ---
    fun submitJoinRequest(request: ProviderJoinRequest) {
        viewModelScope.launch {
            repository.saveJoinRequest(request)
        }
    }

    fun approveJoinRequest(request: ProviderJoinRequest) {
        viewModelScope.launch {
            // First, map to YemenService
            val newService = YemenService(
                id = "", // Will auto generate in firestore
                category = request.category,
                subCategory = request.subCategory,
                nameAr = request.nameAr,
                nameEn = request.nameEn.ifBlank { request.nameAr },
                phoneNumber = request.phone,
                descriptionAr = if (isArabic.value) "مقدم خدمة معتمد في ${request.workPlace}" else "Licensed Service provider",
                descriptionEn = "Registered Service Provider",
                addressAr = request.address,
                addressEn = request.address,
                imageUrl = request.imageUrl,
                rating = 5.0f,
                isPinned = false,
                isRecommended = false,
                orderIndex = 10,
                workPlace = request.workPlace,
                residencePlace = request.residencePlace,
                idCardImageUrl = request.idCardImageUrl
            )
            repository.saveService(newService)
            repository.saveJoinRequest(request.copy(status = "APPROVED"))
        }
    }

    fun rejectJoinRequest(request: ProviderJoinRequest) {
        viewModelScope.launch {
            repository.saveJoinRequest(request.copy(status = "REJECTED"))
        }
    }

    fun deleteJoinRequest(id: String) {
        viewModelScope.launch {
            repository.deleteJoinRequest(id)
        }
    }

    // --- SUPERVISOR MANAGEMENT ---
    fun saveSupervisor(account: SupervisorAccount) {
        viewModelScope.launch {
            repository.saveSupervisor(account)
        }
    }

    fun deleteSupervisor(id: String) {
        viewModelScope.launch {
            repository.deleteSupervisor(id)
        }
    }

    // --- SMART CHAT ASSISTANT LOGIC ---
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val currentHistory = _chatHistory.value.toMutableList()
        currentHistory.add(Pair(text, true)) // Add User turn
        _chatHistory.value = currentHistory

        viewModelScope.launch {
            _isChatLoading.value = true
            val aiResponse = GeminiService.chatWithAi(text, isArabic.value)
            val updatedHistory = _chatHistory.value.toMutableList()
            updatedHistory.add(Pair(aiResponse, false)) // Add System turn
            _chatHistory.value = updatedHistory
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
    }
}
