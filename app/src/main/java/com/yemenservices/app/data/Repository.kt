package com.yemenservices.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class Repository(private val db: AppDatabase) {

    private val settingDao = db.appSettingDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val categoriesCollection = firestore.collection("categories")
    private val providersCollection = firestore.collection("service_providers")

    // Settings (remain local/Room-based)
    val allSettings: Flow<List<AppSetting>> = settingDao.getAllSettings()

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(AppSetting(key, value))
    }

    suspend fun getSettingByKey(key: String): String? {
        return settingDao.getSettingByKey(key)?.value
    }

    // Categories
    val allCategories: Flow<List<Category>> = callbackFlow {
        val listenerRegistration = categoriesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Ignore or log error
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                }.sortedBy { it.id }
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
                }.sortedWith(compareByDescending<ServiceProvider> { it.isVerified }.thenByDescending { it.id })
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
                }.sortedWith(compareByDescending<ServiceProvider> { it.isVerified }.thenByDescending { it.id })
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
}
