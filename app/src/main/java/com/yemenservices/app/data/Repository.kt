package com.yemenservices.app.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Repository {

    private var firestore: FirebaseFirestore? = null

    // Real-time cached state flows
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _serviceProviders = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val serviceProviders: StateFlow<List<ServiceProvider>> = _serviceProviders

    private val _pendingProviders = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val pendingProviders: StateFlow<List<ServiceProvider>> = _pendingProviders

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _appConfig = MutableStateFlow<AppConfig>(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig

    // Listener registrations
    private var categoriesListener: ListenerRegistration? = null
    private var providersListener: ListenerRegistration? = null
    private var pendingListener: ListenerRegistration? = null
    private var reviewsListener: ListenerRegistration? = null
    private var configListener: ListenerRegistration? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
            setupRealtimeListeners()
        } catch (e: Throwable) {
            Log.e("DaliliRepo", "Firebase Firestore unavailable, enabling robust offline mode. ${e.message}")
            setupOfflineMockData()
        }
    }

    private fun setupRealtimeListeners() {
        val fs = firestore ?: return

        // 1. Snapshot Listener for categories
        categoriesListener = fs.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DaliliRepo", "Categories listener failed. Falling back to memory.", error)
                    if (_categories.value.isEmpty()) setupOfflineMockCategories()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Category(
                                id = doc.id,
                                name_ar = doc.getString("name_ar") ?: "",
                                name_en = doc.getString("name_en") ?: "",
                                icon = doc.getString("icon") ?: "tools",
                                image_url = doc.getString("image_url"),
                                parent_id = doc.getString("parent_id")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isEmpty() && snapshot.isEmpty) {
                        seedDefaultCategories()
                    } else {
                        _categories.value = list
                    }
                }
            }

        // 2. Snapshot Listener for service_providers
        providersListener = fs.collection("service_providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DaliliRepo", "Service providers listener failed", error)
                    if (_serviceProviders.value.isEmpty()) setupOfflineMockProviders()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toServiceProviderObject()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isEmpty() && snapshot.isEmpty) {
                        seedDefaultProviders()
                    } else {
                        _serviceProviders.value = list
                    }
                }
            }

        // 3. Snapshot Listener for pending_providers
        pendingListener = fs.collection("pending_providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DaliliRepo", "Pending providers listener failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toServiceProviderObject()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _pendingProviders.value = list
                }
            }

        // 4. Snapshot Listener for reviews
        reviewsListener = fs.collection("reviews")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DaliliRepo", "Reviews listener failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Review(
                                id = doc.id,
                                provider_id = doc.getString("provider_id") ?: "",
                                reviewer_name = doc.getString("reviewer_name") ?: "مستخدم",
                                rating = doc.getDouble("rating")?.toFloat() ?: 5f,
                                comment = doc.getString("comment") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _reviews.value = list
                }
            }

        // 5. Snapshot Listener for appConfig
        configListener = fs.collection("config").document("app")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DaliliRepo", "AppConfig listener failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        _appConfig.value = AppConfig(
                            app_name = snapshot.getString("app_name") ?: "دليل الخدمات",
                            welcomeMessage = snapshot.getString("welcomeMessage") ?: "مرحباً بكم...",
                            welcomeTextSize = snapshot.getLong("welcomeTextSize")?.toInt() ?: 14,
                            welcomeImageUrl = snapshot.getString("welcomeImageUrl"),
                            welcomeType = snapshot.getString("welcomeType") ?: "text",
                            primary_color_hex = snapshot.getString("primary_color_hex") ?: "#2E7D32",
                            secondary_color_hex = snapshot.getString("secondary_color_hex") ?: "#81C784",
                            support_email = snapshot.getString("support_email") ?: "support@yemenservices.app",
                            support_whatsapp = snapshot.getString("support_whatsapp") ?: "+96777777777",
                            footer_phone = snapshot.getString("footer_phone") ?: "777777777",
                            selected_icon_type = snapshot.getString("selected_icon_type") ?: "tools"
                        )
                    } catch (e: Exception) {
                        Log.e("DaliliRepo", "Error parsing AppConfig", e)
                    }
                } else {
                    seedDefaultConfig()
                }
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toServiceProviderObject(): ServiceProvider {
        return ServiceProvider(
            id = id,
            name = getString("name") ?: "",
            phone = getString("phone") ?: "",
            whatsapp = getString("whatsapp") ?: "",
            email = getString("email") ?: "",
            description_ar = getString("description_ar") ?: "",
            description_en = getString("description_en") ?: "",
            category_id = getString("category_id") ?: "",
            rating = getDouble("rating")?.toFloat() ?: 5.0f,
            price_level = getString("price_level") ?: "Average",
            distance = getString("distance") ?: "1",
            address_ar = getString("address_ar") ?: "صنعاء",
            address_en = getString("address_en") ?: "Sanaa",
            is_approved = getBoolean("is_approved") ?: false,
            profileImage = getString("profileImage"),
            rating_count = getLong("rating_count")?.toInt() ?: 1
        )
    }

    fun addCategory(category: Category) {
        val fs = firestore
        if (fs != null) {
            val data = hashMapOf(
                "name_ar" to category.name_ar,
                "name_en" to category.name_en,
                "icon" to category.icon,
                "image_url" to category.image_url,
                "parent_id" to category.parent_id
            )
            fs.collection("categories").add(data)
        } else {
            val randomId = "cat_${System.currentTimeMillis()}"
            _categories.value = _categories.value + category.copy(id = randomId)
        }
    }

    fun deleteCategory(id: String) {
        val fs = firestore
        if (fs != null) {
            fs.collection("categories").document(id).delete()
        } else {
            _categories.value = _categories.value.filter { it.id != id }
        }
    }

    fun addServiceProvider(provider: ServiceProvider, isApproved: Boolean) {
        val fs = firestore
        val collectionName = if (isApproved) "service_providers" else "pending_providers"
        
        val data = hashMapOf(
            "name" to provider.name,
            "phone" to provider.phone,
            "whatsapp" to provider.whatsapp,
            "email" to provider.email,
            "description_ar" to provider.description_ar,
            "description_en" to provider.description_en,
            "category_id" to provider.category_id,
            "rating" to provider.rating,
            "price_level" to provider.price_level,
            "distance" to provider.distance,
            "address_ar" to provider.address_ar,
            "address_en" to provider.address_en,
            "is_approved" to isApproved,
            "profileImage" to provider.profileImage,
            "rating_count" to provider.rating_count
        )

        if (fs != null) {
            fs.collection(collectionName).add(data)
        } else {
            val randomId = "prov_${System.currentTimeMillis()}"
            val newProv = provider.copy(id = randomId, is_approved = isApproved)
            if (isApproved) {
                _serviceProviders.value = _serviceProviders.value + newProv
            } else {
                _pendingProviders.value = _pendingProviders.value + newProv
            }
        }
    }

    fun approveServiceProvider(provider: ServiceProvider) {
        val fs = firestore
        if (fs != null) {
            fs.collection("pending_providers").document(provider.id).delete()
            addServiceProvider(provider.copy(is_approved = true), isApproved = true)
        } else {
            _pendingProviders.value = _pendingProviders.value.filter { it.id != provider.id }
            _serviceProviders.value = _serviceProviders.value + provider.copy(is_approved = true)
        }
    }

    fun deleteServiceProvider(id: String, isApproved: Boolean) {
        val fs = firestore
        val collectionName = if (isApproved) "service_providers" else "pending_providers"
        if (fs != null) {
            fs.collection(collectionName).document(id).delete()
        } else {
            if (isApproved) {
                _serviceProviders.value = _serviceProviders.value.filter { it.id != id }
            } else {
                _pendingProviders.value = _pendingProviders.value.filter { it.id != id }
            }
        }
    }

    fun submitReview(review: Review) {
        val fs = firestore
        if (fs != null) {
            val data = hashMapOf(
                "provider_id" to review.provider_id,
                "reviewer_name" to review.reviewer_name,
                "rating" to review.rating,
                "comment" to review.comment,
                "timestamp" to review.timestamp
            )
            fs.collection("reviews").add(data)
            updateProviderRatingInFirebase(review.provider_id, review.rating)
        } else {
            val id = "rev_${System.currentTimeMillis()}"
            _reviews.value = _reviews.value + review.copy(id = id)
        }
    }

    private fun updateProviderRatingInFirebase(providerId: String, newRating: Float) {
        val fs = firestore ?: return
        val docRef = fs.collection("service_providers").document(providerId)
        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val currentRating = snapshot.getDouble("rating")?.toFloat() ?: 5.0f
                val count = snapshot.getLong("rating_count")?.toInt() ?: 1
                val updatedCount = count + 1
                val updatedRating = ((currentRating * count) + newRating) / updatedCount
                docRef.update(
                    "rating", updatedRating,
                    "rating_count", updatedCount
                )
            }
        }
    }

    fun updateAppConfig(config: AppConfig) {
        val fs = firestore
        if (fs != null) {
            val data = hashMapOf(
                "app_name" to config.app_name,
                "welcomeMessage" to config.welcomeMessage,
                "welcomeTextSize" to config.welcomeTextSize,
                "welcomeImageUrl" to config.welcomeImageUrl,
                "welcomeType" to config.welcomeType,
                "primary_color_hex" to config.primary_color_hex,
                "secondary_color_hex" to config.secondary_color_hex,
                "support_email" to config.support_email,
                "support_whatsapp" to config.support_whatsapp,
                "footer_phone" to config.footer_phone,
                "selected_icon_type" to config.selected_icon_type
            )
            fs.collection("config").document("app").set(data)
        } else {
            _appConfig.value = config
        }
    }

    private fun seedDefaultCategories() {
        val defaults = listOf(
            Category("1", "كهربائي", "Electrician", "electrician"),
            Category("2", "سباك", "Plumber", "plumber"),
            Category("3", "ميكانيكي سيارات", "Auto Mechanic", "car_mechanic"),
            Category("4", "طبيب منزل", "Home Doctor", "doctor"),
            Category("5", "فني تكييف", "AC Technician", "ac_technician"),
            Category("6", "مدرس خصوصي", "Private Teacher", "teacher"),
            Category("7", "مبرمج ومطور", "Software Developer", "programmer"),
            Category("8", "عامل نظافة", "Cleaning Worker", "cleaning"),
            Category("9", "نجار وباب وبناء", "Carpenter & Builder", "carpenter")
        )
        defaults.forEach { addCategory(it) }
    }

    private fun seedDefaultProviders() {
        val defaults = listOf(
            ServiceProvider(
                id = "",
                name = "أحمد الوصابي",
                phone = "771234567",
                whatsapp = "771234567",
                email = "ahmed@dalili.com",
                description_ar = "كهربائي منازل محترف بخبرة تزيد عن 8 سنوات في صنعاء.",
                description_en = "Professional home electrician with 8+ years experience in Sana'a.",
                category_id = "1",
                rating = 4.8f,
                price_level = "Low",
                distance = "1.5",
                address_ar = "صنعاء، شارع حدة",
                address_en = "Sana'a, Hadda Street",
                is_approved = true,
                rating_count = 12
            ),
            ServiceProvider(
                id = "",
                name = "عماد اليماني",
                phone = "776543210",
                whatsapp = "776543210",
                email = "emad@dalili.com",
                description_ar = "سباك تمديدات صحية وصيانة عامة بجودة ممتازة وسعر مناسب.",
                description_en = "Plumbing installation and general maintenance with premium service.",
                category_id = "2",
                rating = 4.9f,
                price_level = "Average",
                distance = "2.3",
                address_ar = "تعز، شارع جمال",
                address_en = "Taiz, Jamal Street",
                is_approved = true,
                rating_count = 18
            )
        )
        defaults.forEach { addServiceProvider(it, isApproved = true) }
    }

    private fun seedDefaultConfig() {
        updateAppConfig(AppConfig())
    }

    private fun setupOfflineMockCategories() {
        _categories.value = listOf(
            Category("1", "كهربائي", "Electrician", "electrician"),
            Category("2", "سباك", "Plumber", "plumber"),
            Category("3", "ميكانيكي سيارات", "Auto Mechanic", "car_mechanic"),
            Category("4", "طبيب منزل", "Home Doctor", "doctor"),
            Category("5", "فني تكييف", "AC Technician", "ac_technician"),
            Category("6", "مدرس خصوصي", "Private Teacher", "teacher"),
            Category("7", "مبرمج ومطور", "Software Developer", "programmer"),
            Category("8", "عامل نظافة", "Cleaning Worker", "cleaning"),
            Category("9", "نجار وباب وبناء", "Carpenter & Builder", "carpenter")
        )
    }

    private fun setupOfflineMockProviders() {
        _serviceProviders.value = listOf(
            ServiceProvider(
                id = "p1",
                name = "أحمد الوصابي",
                phone = "771234567",
                whatsapp = "771234567",
                email = "ahmed@dalili.com",
                description_ar = "كهربائي منازل محترف بخبرة تزيد عن 8 سنوات في صنعاء.",
                description_en = "Professional home electrician with 8+ years experience in Sana'a.",
                category_id = "1",
                rating = 4.8f,
                price_level = "Low",
                distance = "1.5",
                address_ar = "صنعاء، شارع حدة",
                address_en = "Sana'a, Hadda Street",
                is_approved = true,
                rating_count = 12
            ),
            ServiceProvider(
                id = "p2",
                name = "عماد اليماني",
                phone = "776543210",
                whatsapp = "776543210",
                email = "emad@dalili.com",
                description_ar = "سباك تمديدات صحية وصيانة عامة بجودة ممتازة.",
                description_en = "Plumbing installation and general maintenance with premium service.",
                category_id = "2",
                rating = 4.9f,
                price_level = "Average",
                distance = "2.3",
                address_ar = "تعز، شارع جمال",
                address_en = "Taiz, Jamal Street",
                is_approved = true,
                rating_count = 18
            )
        )
    }

    private fun setupOfflineMockData() {
        setupOfflineMockCategories()
        setupOfflineMockProviders()
        _appConfig.value = AppConfig()
    }
}
