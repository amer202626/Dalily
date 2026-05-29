package com.yemenservices.app.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class Repository {

    // Primary observable states
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _providers = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providers = _providers.asStateFlow()

    private val _admins = MutableStateFlow<List<Supervisor>>(emptyList())
    val admins = _admins.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews = _reviews.asStateFlow()

    private val _pendingProviders = MutableStateFlow<List<PendingProvider>>(emptyList())
    val pendingProviders = _pendingProviders.asStateFlow()

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig = _appConfig.asStateFlow()

    // Firestore Reference
    private var firestore: FirebaseFirestore? = null

    // Fallbacks state holders for off-line/local execution
    private val localCategoriesMap = mutableMapOf<String, Category>()
    private val localProvidersMap = mutableMapOf<String, ServiceProvider>()
    private val localAdminsMap = mutableMapOf<String, Supervisor>()
    private val localReviewsMap = mutableMapOf<String, Review>()
    private val localPendingMap = mutableMapOf<String, PendingProvider>()

    init {
        // Attempt firebase loading safely
        try {
            firestore = FirebaseFirestore.getInstance()
            setupRealtimeListeners()
        } catch (e: Exception) {
            Log.e("DaliliRepo", "Firebase not initialized, defaulting to high-fidelity offline mockup database. ${e.message}")
            setupOfflineMockData()
        }
    }

    private fun setupOfflineMockData() {
        // Core 11 Categories
        val defaultCats = listOf(
            Category("c1", "خدمات منزلية", "Home Services", "home_work"),
            Category("c2", "تقنية", "Technology", "computer"),
            Category("c3", "مهندسين", "Engineers", "engineering"),
            Category("c4", "صيانة منزلية", "Home Maintenance", "handyman"),
            Category("c5", "جمال", "Beauty & Care", "face"),
            Category("c6", "نقل وأجرة", "Transport & Taxi", "local_shipping"),
            Category("c7", "خياطة وتطريز", "Sewing & Embroidery", "content_cut"),
            Category("c8", "كهرباء منازل", "Home Electricity", "bolt"),
            Category("c9", "سباكة وصحي", "Plumbing & Sanitary", "water_drop"),
            Category("c10", "صيانة هواتف", "Phone Repair", "smartphone"),
            Category("c11", "صيانة تكييف", "AC Maintenance", "ac_unit")
        )

        defaultCats.forEach { localCategoriesMap[it.id] = it }
        _categories.value = localCategoriesMap.values.toList()

        // Default Supervisors
        val defaultAdmins = listOf(
            Supervisor("admin1", "Admin", "maher736462", is_super_admin = false),
            Supervisor("admin2", "owner", "maher--736462", is_super_admin = true)
        )
        defaultAdmins.forEach { localAdminsMap[it.id] = it }
        _admins.value = localAdminsMap.values.toList()

        // Default Providers
        val defaultProviders = listOf(
            ServiceProvider("p1", "c1", "المهندس أحمد صالح", "Eng. Ahmed Saleh", "775511223", "967775511223", "صنعاء", "Sanaa", "low", "close", true, true),
            ServiceProvider("p2", "c2", "المعلم فؤاد الوصابي", "Fouad Al-Wasabi", "771122334", "96771122334", "عدن", "Aden", "medium", "medium", false, true),
            ServiceProvider("p3", "c3", "مركز الرواد للصيانة", "Al-Ruwad Repair Center", "733445566", "967733445566", "تعز", "Taiz", "high", "far", true, true)
        )
        defaultProviders.forEach { localProvidersMap[it.id] = it }
        _providers.value = localProvidersMap.values.toList()

        // Default Reviews
        val defaultReviews = listOf(
            Review("r1", "p1", "ماجد الهمداني", 5.0, "خدمة سريعة وممتازة كثر خيركم"),
            Review("r2", "p2", "عمر اليافعي", 4.0, "شغل طيب وسعر مناسب")
        )
        defaultReviews.forEach { localReviewsMap[it.id] = it }
        _reviews.value = localReviewsMap.values.toList()

        // Default Settings
        _appConfig.value = AppConfig()
    }

    private fun setupRealtimeListeners() {
        val db = firestore ?: return

        // 1. Categories
        db.collection("categories").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("DaliliRepo", "Categories listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val cat = doc.toObject(Category::class.java)
                    cat?.copy(id = doc.id)
                }
                if (list.isEmpty()) {
                    // Populate defaults to firestore
                    setupDefaultCategoriesInFirestore()
                } else {
                    _categories.value = list
                }
            }
        }

        // 2. Service Providers
        db.collection("service_providers").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val prov = doc.toObject(ServiceProvider::class.java)
                    prov?.copy(id = doc.id)
                }
                _providers.value = list
            }
        }

        // 3. Admins / Supervisors
        db.collection("admins").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val admin = doc.toObject(Supervisor::class.java)
                    admin?.copy(id = doc.id)
                }
                if (list.isEmpty()) {
                    setupDefaultAdminsInFirestore()
                } else {
                    _admins.value = list
                }
            }
        }

        // 4. Reviews
        db.collection("reviews").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val rev = doc.toObject(Review::class.java)
                    rev?.copy(id = doc.id)
                }
                _reviews.value = list
            }
        }

        // 5. Pending Providers
        db.collection("pending_providers").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val pending = doc.toObject(PendingProvider::class.java)
                    pending?.copy(id = doc.id)
                }
                _pendingProviders.value = list
            }
        }

        // 6. Settings
        db.collection("settings").document("app_settings").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val config = snapshot.toObject(AppConfig::class.java)
                if (config != null) {
                    _appConfig.value = config
                }
            } else {
                setupDefaultSettingsInFirestore()
            }
        }
    }

    private fun setupDefaultCategoriesInFirestore() {
        val db = firestore ?: return
        val defaultCats = listOf(
            Category("c1", "خدمات منزلية", "Home Services", "home_work"),
            Category("c2", "تقنية", "Technology", "computer"),
            Category("c3", "مهندسين", "Engineers", "engineering"),
            Category("c4", "صيانة منزلية", "Home Maintenance", "handyman"),
            Category("c5", "جمال", "Beauty & Care", "face"),
            Category("c6", "نقل وأجرة", "Transport & Taxi", "local_shipping"),
            Category("c7", "خياطة وتطريز", "Sewing & Embroidery", "content_cut"),
            Category("c8", "كهرباء منازل", "Home Electricity", "bolt"),
            Category("c9", "سباكة وصحي", "Plumbing & Sanitary", "water_drop"),
            Category("c10", "صيانة هواتف", "Phone Repair", "smartphone"),
            Category("c11", "صيانة تكييف", "AC Maintenance", "ac_unit")
        )
        for (cat in defaultCats) {
            db.collection("categories").document(cat.id).set(cat)
        }
    }

    private fun setupDefaultAdminsInFirestore() {
        val db = firestore ?: return
        val defaultAdmins = listOf(
            Supervisor("admin1", "Admin", "maher736462", is_super_admin = false),
            Supervisor("admin2", "owner", "maher--736462", is_super_admin = true)
        )
        for (admin in defaultAdmins) {
            db.collection("admins").document(admin.id).set(admin)
        }
    }

    private fun setupDefaultSettingsInFirestore() {
        val db = firestore ?: return
        db.collection("settings").document("app_settings").set(AppConfig())
    }

    // --- CRUD OPERATIONAL METHODS ---

    // 1. Categories
    fun addCategory(category: Category) {
        val db = firestore
        val newId = if (category.id.isBlank()) UUID.randomUUID().toString() else category.id
        val finalCat = category.copy(id = newId)

        if (db != null) {
            db.collection("categories").document(newId).set(finalCat)
        } else {
            localCategoriesMap[newId] = finalCat
            _categories.value = localCategoriesMap.values.toList()
        }
    }

    fun updateCategory(category: Category) {
        val db = firestore
        if (db != null) {
            db.collection("categories").document(category.id).set(category)
        } else {
            localCategoriesMap[category.id] = category
            _categories.value = localCategoriesMap.values.toList()
        }
    }

    fun deleteCategory(categoryId: String) {
        val db = firestore
        if (db != null) {
            db.collection("categories").document(categoryId).delete()
        } else {
            localCategoriesMap.remove(categoryId)
            _categories.value = localCategoriesMap.values.toList()
        }
    }

    // 2. Service Providers
    fun addProvider(provider: ServiceProvider) {
        val db = firestore
        val newId = if (provider.id.isBlank()) UUID.randomUUID().toString() else provider.id
        val finalProv = provider.copy(id = newId)

        if (db != null) {
            db.collection("service_providers").document(newId).set(finalProv)
        } else {
            localProvidersMap[newId] = finalProv
            _providers.value = localProvidersMap.values.toList()
        }
    }

    fun updateProvider(provider: ServiceProvider) {
        val db = firestore
        if (db != null) {
            db.collection("service_providers").document(provider.id).set(provider)
        } else {
            localProvidersMap[provider.id] = provider
            _providers.value = localProvidersMap.values.toList()
        }
    }

    fun deleteProvider(providerId: String) {
        val db = firestore
        if (db != null) {
            db.collection("service_providers").document(providerId).delete()
        } else {
            localProvidersMap.remove(providerId)
            _providers.value = localProvidersMap.values.toList()
        }
    }

    // 3. Pending Providers
    fun addPendingProvider(pending: PendingProvider) {
        val db = firestore
        val newId = if (pending.id.isBlank()) UUID.randomUUID().toString() else pending.id
        val finalPending = pending.copy(id = newId)

        if (db != null) {
            db.collection("pending_providers").document(newId).set(finalPending)
        } else {
            localPendingMap[newId] = finalPending
            _pendingProviders.value = localPendingMap.values.toList()
        }
    }

    fun deletePendingProvider(id: String) {
        val db = firestore
        if (db != null) {
            db.collection("pending_providers").document(id).delete()
        } else {
            localPendingMap.remove(id)
            _pendingProviders.value = localPendingMap.values.toList()
        }
    }

    // 4. Supervisors / Admins
    fun addSupervisor(supervisor: Supervisor) {
        val db = firestore
        val newId = if (supervisor.id.isBlank()) UUID.randomUUID().toString() else supervisor.id
        val finalSupervisor = supervisor.copy(id = newId)

        if (db != null) {
            db.collection("admins").document(newId).set(finalSupervisor)
        } else {
            localAdminsMap[newId] = finalSupervisor
            _admins.value = localAdminsMap.values.toList()
        }
    }

    fun deleteSupervisor(id: String) {
        val db = firestore
        if (db != null) {
            db.collection("admins").document(id).delete()
        } else {
            localAdminsMap.remove(id)
            _admins.value = localAdminsMap.values.toList()
        }
    }

    // 5. Reviews
    fun addReview(review: Review) {
        val db = firestore
        val newId = if (review.id.isBlank()) UUID.randomUUID().toString() else review.id
        val finalReview = review.copy(id = newId)

        if (db != null) {
            db.collection("reviews").document(newId).set(finalReview)
        } else {
            localReviewsMap[newId] = finalReview
            _reviews.value = localReviewsMap.values.toList()
        }
    }

    fun deleteReview(id: String) {
        val db = firestore
        if (db != null) {
            db.collection("reviews").document(id).delete()
        } else {
            localReviewsMap.remove(id)
            _reviews.value = localReviewsMap.values.toList()
        }
    }

    // 6. Settings / AppConfig
    fun updateAppConfig(config: AppConfig) {
        val db = firestore
        if (db != null) {
            db.collection("settings").document("app_settings").set(config)
        } else {
            _appConfig.value = config
        }
    }

    // Helpers to access standard observables
    fun getCategories(): Flow<List<Category>> = categories
    fun getProviders(): Flow<List<ServiceProvider>> = providers
    fun getAdmins(): Flow<List<Supervisor>> = admins
    fun getReviews(): Flow<List<Review>> = reviews
    fun getPendingProviders(): Flow<List<PendingProvider>> = pendingProviders
    fun getAppConfig(): Flow<AppConfig> = appConfig

    // Refresh manually (as requested by top bar refresh 🔄)
    fun refreshData() {
        if (firestore != null) {
            setupRealtimeListeners()
        } else {
            // Recalculate lists to emit new updates
            _categories.value = localCategoriesMap.values.toList()
            _providers.value = localProvidersMap.values.toList()
            _admins.value = localAdminsMap.values.toList()
            _reviews.value = localReviewsMap.values.toList()
            _pendingProviders.value = localPendingMap.values.toList()
        }
    }
}
