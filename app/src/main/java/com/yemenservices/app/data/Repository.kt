package com.yemenservices.app.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Repository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirestoreRepository"

    private val _categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    val categoriesFlow: StateFlow<List<Category>> = _categoriesFlow.asStateFlow()

    private val _providersFlow = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val providersFlow: StateFlow<List<ServiceProvider>> = _providersFlow.asStateFlow()

    private val _reviewsFlow = MutableStateFlow<List<Review>>(emptyList())
    val reviewsFlow: StateFlow<List<Review>> = _reviewsFlow.asStateFlow()

    private val _adminsFlow = MutableStateFlow<List<Admin>>(emptyList())
    val adminsFlow: StateFlow<List<Admin>> = _adminsFlow.asStateFlow()

    private val _configFlow = MutableStateFlow<AppConfig>(AppConfig())
    val configFlow: StateFlow<AppConfig> = _configFlow.asStateFlow()

    private var categoriesListener: ListenerRegistration? = null
    private var providersListener: ListenerRegistration? = null
    private var reviewsListener: ListenerRegistration? = null
    private var adminsListener: ListenerRegistration? = null
    private var configListener: ListenerRegistration? = null

    init {
        startRealtimeListening()
    }

    private fun startRealtimeListening() {
        Log.i(TAG, "Starting direct real-time Firestore listeners.")

        // Listen for AppConfig
        configListener = firestore.collection("app_config").document("settings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Config snapshot listener failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                snapshot?.let { doc ->
                    if (doc.exists()) {
                        try {
                            doc.toObject(AppConfig::class.java)?.let {
                                _configFlow.value = it
                                Log.d(TAG, "Realtime update: Loaded AppConfig with theme ${it.app_theme}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse AppConfig document", e)
                        }
                    } else {
                        // Create default config
                        val defaultConfig = AppConfig()
                        saveConfig(defaultConfig, {}, {})
                    }
                }
            }

        // Listen for Categories
        categoriesListener = firestore.collection("categories")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Categories real-time snapshot listener failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                snapshots?.let { querySnapshot ->
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Category::class.java)?.apply { id = doc.id }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse Category document: ${doc.id}", e)
                            null
                        }
                    }.sortedBy { it.order_index }
                    _categoriesFlow.value = list
                    Log.d(TAG, "Realtime update: Loaded ${list.size} categories.")
                }
            }

        // Listen for Service Providers
        providersListener = firestore.collection("service_providers")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Providers real-time snapshot listener failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                snapshots?.let { querySnapshot ->
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ServiceProvider::class.java)?.apply { id = doc.id }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse ServiceProvider document: ${doc.id}", e)
                            null
                        }
                    }
                    _providersFlow.value = list
                    Log.d(TAG, "Realtime update: Loaded ${list.size} providers.")
                }
            }

        // Listen for Reviews
        reviewsListener = firestore.collection("reviews")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Reviews real-time snapshot listener failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                snapshots?.let { querySnapshot ->
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Review::class.java)?.apply { id = doc.id }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse Review document: ${doc.id}", e)
                            null
                        }
                    }.sortedByDescending { it.created_at }
                    _reviewsFlow.value = list
                    Log.d(TAG, "Realtime update: Loaded ${list.size} reviews.")
                }
            }

        // Listen for Admins
        adminsListener = firestore.collection("admins")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Admins real-time snapshot listener failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                snapshots?.let { querySnapshot ->
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Admin::class.java)?.apply { username = doc.id }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse Admin document: ${doc.id}", e)
                            null
                        }
                    }
                    _adminsFlow.value = list
                    Log.d(TAG, "Realtime update: Loaded ${list.size} admins.")
                }
            }
    }

    // Direct Firestore mutations
    fun saveCategory(category: Category, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("categories").document(category.id).set(category)
            .addOnSuccessListener {
                Log.d(TAG, "Category saved to Firestore: ${category.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save category to Firestore: ${category.id}", e)
                onFailure(e)
            }
    }

    fun deleteCategory(categoryId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("categories").document(categoryId).delete()
            .addOnSuccessListener {
                Log.d(TAG, "Category deleted from Firestore: $categoryId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete category from Firestore: $categoryId", e)
                onFailure(e)
            }
    }

    fun saveProvider(provider: ServiceProvider, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("service_providers").document(provider.id).set(provider)
            .addOnSuccessListener {
                Log.d(TAG, "Provider saved to Firestore: ${provider.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save provider to Firestore: ${provider.id}", e)
                onFailure(e)
            }
    }

    fun deleteProvider(providerId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("service_providers").document(providerId).delete()
            .addOnSuccessListener {
                Log.d(TAG, "Provider deleted from Firestore: $providerId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete provider from Firestore: $providerId", e)
                onFailure(e)
            }
    }

    fun saveReview(review: Review, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("reviews").document(review.id).set(review)
            .addOnSuccessListener {
                Log.d(TAG, "Review saved to Firestore: ${review.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save review to Firestore: ${review.id}", e)
                onFailure(e)
            }
    }

    fun deleteReview(reviewId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("reviews").document(reviewId).delete()
            .addOnSuccessListener {
                Log.d(TAG, "Review deleted from Firestore: $reviewId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete review from Firestore: $reviewId", e)
                onFailure(e)
            }
    }

    fun saveAdmin(admin: Admin, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("admins").document(admin.username).set(admin)
            .addOnSuccessListener {
                Log.d(TAG, "Admin saved to Firestore: ${admin.username}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save admin to Firestore: ${admin.username}", e)
                onFailure(e)
            }
    }

    fun deleteAdmin(username: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("admins").document(username).delete()
            .addOnSuccessListener {
                Log.d(TAG, "Admin deleted from Firestore: $username")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete admin from Firestore: $username", e)
                onFailure(e)
            }
    }

    fun saveConfig(config: AppConfig, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("app_config").document("settings").set(config)
            .addOnSuccessListener {
                Log.d(TAG, "Config saved to Firestore.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save config to Firestore.", e)
                onFailure(e)
            }
    }

    fun cleanup() {
        categoriesListener?.remove()
        providersListener?.remove()
        reviewsListener?.remove()
        adminsListener?.remove()
        configListener?.remove()
    }
}
