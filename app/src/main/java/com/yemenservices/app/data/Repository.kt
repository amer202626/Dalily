package com.yemenservices.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class Repository(private val db: AppDatabase) {

    private val settingDao = db.appSettingDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val settingsCollection = firestore.collection("app_settings")
    private val categoriesCollection = firestore.collection("categories")
    private val providersCollection = firestore.collection("service_providers")
    private val supervisorsCollection = firestore.collection("supervisors")
    private val requestsCollection = firestore.collection("registration_requests")
    private val analyticsCollection = firestore.collection("analytics")
    private val wishlistCollection = firestore.collection("wishlist")
    private val reviewsCollection = firestore.collection("provider_reviews")

    init {
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Settings (Firestore snapshot listener for real-time synchronization across all devices !)
    val allSettings: Flow<List<AppSetting>> = callbackFlow {
        val listenerRegistration = settingsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val key = doc.id
                    val value = doc.get("value")?.toString() ?: ""
                    AppSetting(key, value)
                }
                trySend(list)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun saveSetting(key: String, value: String) {
        settingsCollection.document(key).set(mapOf("value" to value)).await()
        try {
            settingDao.insertSetting(AppSetting(key, value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getSettingByKey(key: String): String? {
        return try {
            val doc = settingsCollection.document(key).get().await()
            if (doc != null && doc.exists()) {
                doc.get("value")?.toString()
            } else {
                settingDao.getSettingByKey(key)?.value
            }
        } catch (e: Exception) {
            settingDao.getSettingByKey(key)?.value
        }
    }

    // Categories
    val allCategories: Flow<List<Category>> = callbackFlow {
        val listenerRegistration = categoriesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                }.sortedWith(compareByDescending<Category> { it.isPinned }.thenBy { it.id })
                trySend(list)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun insertCategory(category: Category) {
        val uniqueId = if (category.id == 0) {
            (System.currentTimeMillis() % 10000000).toInt()
        } else {
            category.id
        }
        val finalCategory = category.copy(id = uniqueId)
        categoriesCollection.document(uniqueId.toString()).set(finalCategory).await()
    }

    suspend fun updateCategory(category: Category) {
        categoriesCollection.document(category.id.toString()).set(category).await()
    }

    suspend fun deleteCategory(category: Category) {
        categoriesCollection.document(category.id.toString()).delete().await()
    }

    // Service Providers
    val allProviders: Flow<List<ServiceProvider>> = callbackFlow {
        val listenerRegistration = providersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ServiceProvider::class.java)
                }.sortedWith(
                    compareByDescending<ServiceProvider> { it.isFeatured }
                        .thenByDescending { it.isVerified }
                        .thenByDescending { it.id }
                )
                trySend(list)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    fun getProvidersByCategoryId(categoryId: Int): Flow<List<ServiceProvider>> = callbackFlow {
        val query = providersCollection.whereEqualTo("categoryId", categoryId)
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ServiceProvider::class.java)
                }.sortedWith(
                    compareByDescending<ServiceProvider> { it.isFeatured }
                        .thenByDescending { it.isVerified }
                        .thenByDescending { it.id }
                )
                trySend(list)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    fun getProviderById(id: Int): Flow<ServiceProvider?> = callbackFlow {
        val listenerRegistration = providersCollection.document(id.toString()).addSnapshotListener { doc, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                trySend(doc.toObject(ServiceProvider::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun insertProvider(provider: ServiceProvider) {
        val uniqueId = if (provider.id == 0) {
            (System.currentTimeMillis() % 10000000).toInt()
        } else {
            provider.id
        }
        val finalProvider = provider.copy(id = uniqueId)
        providersCollection.document(uniqueId.toString()).set(finalProvider).await()
    }

    suspend fun updateProvider(provider: ServiceProvider) {
        providersCollection.document(provider.id.toString()).set(provider).await()
    }

    suspend fun deleteProvider(provider: ServiceProvider) {
        providersCollection.document(provider.id.toString()).delete().await()
    }

    suspend fun incrementCallCount(providerId: Int) {
        try {
            val docRef = providersCollection.document(providerId.toString())
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentCalls = snapshot.getLong("callCount") ?: 0L
                transaction.update(docRef, "callCount", currentCalls + 1)
            }.await()
            incrementGlobalCalls()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun incrementGlobalCalls() {
        try {
            val globalRef = analyticsCollection.document("global")
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(globalRef)
                val currentCalls = snapshot.getLong("totalCalls") ?: 0L
                transaction.update(globalRef, "totalCalls", currentCalls + 1)
            }.await()
        } catch (e: Exception) {
            try {
                analyticsCollection.document("global").set(mapOf("totalCalls" to 1L), com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (ex: Exception) {}
        }
    }

    suspend fun incrementUsersCount() {
        try {
            val globalRef = analyticsCollection.document("global")
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(globalRef)
                val currentUsers = snapshot.getLong("usersCount") ?: 100L
                transaction.update(globalRef, "usersCount", currentUsers + 1)
            }.await()
        } catch (e: Exception) {
            try {
                analyticsCollection.document("global").set(mapOf("usersCount" to 101L), com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (ex: Exception) {}
        }
    }

    // Supervisors Management Flow
    val allSupervisors: Flow<List<Supervisor>> = callbackFlow {
        val listenerRegistration = supervisorsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Supervisor::class.java)
                }
                trySend(list)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun insertSupervisor(supervisor: Supervisor) {
        val uniqueId = supervisor.id.ifBlank { supervisorsCollection.document().id }
        val finalSupervisor = supervisor.copy(id = uniqueId)
        supervisorsCollection.document(uniqueId).set(finalSupervisor).await()
    }

    suspend fun deleteSupervisor(supervisorId: String) {
        supervisorsCollection.document(supervisorId).delete().await()
    }

    // Service Provider registration requests Flow
    val allRegistrationRequests: Flow<List<RegistrationRequest>> = callbackFlow {
        val listenerRegistration = requestsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(RegistrationRequest::class.java)
                }.sortedByDescending { it.timestamp }
                trySend(list)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun insertRegistrationRequest(request: RegistrationRequest) {
        val uniqueId = request.id.ifBlank { requestsCollection.document().id }
        val finalRequest = request.copy(id = uniqueId, timestamp = System.currentTimeMillis())
        requestsCollection.document(uniqueId).set(finalRequest).await()
    }

    suspend fun updateRegistrationRequestStatus(requestId: String, status: String) {
        requestsCollection.document(requestId).update("status", status).await()
    }

    suspend fun deleteRegistrationRequest(requestId: String) {
        requestsCollection.document(requestId).delete().await()
    }

    // Document Snapshot helper for Global Analytics Stats
    fun getGlobalAnalytics(): Flow<Map<String, Long>> = callbackFlow {
        val listenerRegistration = analyticsCollection.document("global").addSnapshotListener { doc, error ->
            if (error != null) return@addSnapshotListener
            if (doc != null && doc.exists()) {
                val map = mapOf(
                    "usersCount" to (doc.getLong("usersCount") ?: 100L),
                    "totalCalls" to (doc.getLong("totalCalls") ?: 0L)
                )
                trySend(map)
            } else {
                trySend(mapOf("usersCount" to 100L, "totalCalls" to 0L))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    // Wishlist Favorites Functions
    fun getFavoritesForUser(userId: String): Flow<List<Int>> = callbackFlow {
        val listenerRegistration = wishlistCollection.whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.getLong("providerId")?.toInt()
                    }
                    trySend(list)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun toggleFavorite(userId: String, providerId: Int, isFav: Boolean) {
        val docId = "${userId}_${providerId}"
        if (isFav) {
            wishlistCollection.document(docId).set(mapOf("userId" to userId, "providerId" to providerId)).await()
        } else {
            wishlistCollection.document(docId).delete().await()
        }
    }

    // Provider Reviews
    fun getReviewsForProvider(providerId: Int): Flow<List<ProviderReview>> = callbackFlow {
        val query = reviewsCollection.whereEqualTo("providerId", providerId)
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ProviderReview::class.java)
                }.sortedByDescending { it.timestamp }
                trySend(list)
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun insertReview(review: ProviderReview) {
        val docRef = reviewsCollection.document()
        val finalReview = review.copy(id = docRef.id, timestamp = System.currentTimeMillis())
        docRef.set(finalReview).await()
        recalculateProviderAverageRating(review.providerId)
    }

    suspend fun updateReview(review: ProviderReview) {
        reviewsCollection.document(review.id).set(review).await()
        recalculateProviderAverageRating(review.providerId)
    }

    suspend fun deleteReview(review: ProviderReview) {
        reviewsCollection.document(review.id).delete().await()
        recalculateProviderAverageRating(review.providerId)
    }

    private suspend fun recalculateProviderAverageRating(providerId: Int) {
        try {
            val snapshot = reviewsCollection.whereEqualTo("providerId", providerId).get().await()
            val reviews = snapshot.documents.mapNotNull { it.toObject(ProviderReview::class.java) }
            if (reviews.isNotEmpty()) {
                val avg = reviews.map { it.rating }.average().toFloat()
                // Round rating to 1 decimal place
                val rounded = Math.round(avg * 10f) / 10f
                providersCollection.document(providerId.toString()).update("rating", rounded).await()
            } else {
                providersCollection.document(providerId.toString()).update("rating", 5.0f).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
