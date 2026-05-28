package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Custom Task await extension to safely execute suspending Firestore calls
// without depending on external firebase-coroutines modules
suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: RuntimeException("Firestore Task failed"))
        }
    }
}

fun getCurrentTimeIso(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

class Repository(
    private val categoryDao: CategoryDao,
    private val serviceProviderDao: ServiceProviderDao,
    private val reviewDao: ReviewDao
) {
    companion object {
        private const val TAG = "Repository"
    }

    val categories: Flow<List<Category>> = categoryDao.getAllCategories()
    val serviceProviders: Flow<List<ServiceProvider>> = serviceProviderDao.getAllServiceProviders()

    fun getProvidersByCategoryId(categoryId: String): Flow<List<ServiceProvider>> {
        return serviceProviderDao.getProvidersByCategoryId(categoryId)
    }

    fun getReviewsForProvider(providerId: String): Flow<List<Review>> {
        return reviewDao.getReviewsForProvider(providerId)
    }

    private val firestore = FirebaseFirestore.getInstance()

    // Firestore Realtime snapshot synchronization listeners:
    fun listenToFirestoreCategories() {
        firestore.collection("categories")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Categories snapshot listener failed: ${error.message}")
                    return@addSnapshotListener
                }
                snapshots?.let { querySnapshot ->
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Category::class.java)?.apply { id = doc.id }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    Log.d(TAG, "Categories updated from Firestore Snapshot: ${list.size}")
                    CoroutineScope(Dispatchers.IO).launch {
                        categoryDao.clearAll()
                        if (list.isNotEmpty()) {
                            categoryDao.insertCategories(list)
                        }
                    }
                }
            }
    }

    fun listenToFirestoreServiceProviders() {
        firestore.collection("service_providers")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "ServiceProviders snapshot listener failed: ${error.message}")
                    return@addSnapshotListener
                }
                snapshots?.let { querySnapshot ->
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ServiceProvider::class.java)?.apply { id = doc.id }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    Log.d(TAG, "ServiceProviders updated from Firestore Snapshot: ${list.size}")
                    CoroutineScope(Dispatchers.IO).launch {
                        serviceProviderDao.clearAll()
                        if (list.isNotEmpty()) {
                            serviceProviderDao.insertServiceProviders(list)
                        }
                    }
                }
            }
    }

    fun listenToFirestoreReviews() {
        firestore.collection("reviews")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Reviews snapshot listener failed: ${error.message}")
                    return@addSnapshotListener
                }
                snapshots?.let { querySnapshot ->
                    val list = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Review::class.java)?.apply { id = doc.id }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    Log.d(TAG, "Reviews updated from Firestore Snapshot: ${list.size}")
                    CoroutineScope(Dispatchers.IO).launch {
                        reviewDao.clearAll()
                        if (list.isNotEmpty()) {
                            reviewDao.insertReviews(list)
                        }
                    }
                }
            }
    }

    suspend fun syncWithFirestore(settingsManager: SettingsManager) {
        // Seeding databases locally and in Firestore if empty
        try {
            val localCats = categoryDao.getAllCategoriesDirect()
            if (localCats.isEmpty()) {
                val defaultCategories = listOf(
                    Category("cat_1", "صيانة منزلية", "Home Maintenance", "home_repair_service", 1, true, "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_2", "تقنية", "Technology", "laptop_mac", 2, true, "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_3", "تعليم", "Education", "school", 3, true, "https://images.unsplash.com/photo-1427504494785-3a9ca7044f45?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_4", "جمال", "Beauty", "brush", 4, true, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_5", "سيارات", "Cars", "directions_car", 5, true, "https://images.unsplash.com/photo-1486006920555-c77dce18193b?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_6", "خدمات منزلية", "Home Services", "cleaning_services", 6, true, "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_7", "شحن وتوصيل", "Shipping & Delivery", "local_shipping", 7, true, "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_8", "خدمات مهنية", "Professional Services", "work", 8, true, "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_9", "سيارات أجرة", "Taxi", "local_taxi", 9, true, "https://images.unsplash.com/photo-1494959764136-6be9eb3c261e?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_10", "توصيل طلبات", "Order Delivery", "delivery_dining", 10, true, "https://images.unsplash.com/photo-1526367790999-0150786486a9?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_11", "تأجير سيارات", "Car Rental", "car_rental", 11, true, "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_12", "حجوزات شقق وفنادق", "Apartments & Hotels", "hotel", 12, true, "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=300&auto=format&fit=crop&q=60")
                )
                categoryDao.insertCategories(defaultCategories)
                // Seed Firestore too
                for (cat in defaultCategories) {
                    firestore.collection("categories").document(cat.id).set(cat)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fail-safe category seeding failed: ${e.message}")
        }

        try {
            val localProviders = serviceProviderDao.getAllServiceProvidersDirect()
            if (localProviders.isEmpty()) {
                val defaultProviders = listOf(
                    ServiceProvider("prov_1", "المهندس فؤاد - صيانة أجهزة", "Eng. Fouad - Appliance Repair", "771234567", "cat_1", 4.8f, true, "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=200&auto=format&fit=crop&q=60"),
                    ServiceProvider("prov_2", "المحترف للشبكات والكمبيوتر", "The Professional for Networks & PC", "733987654", "cat_2", 4.9f, true, "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=200&auto=format&fit=crop&q=60"),
                    ServiceProvider("prov_3", "أكاديمية النجاح لتقوية الطلاب", "Success Academy for Tutoring", "711223344", "cat_3", 4.7f, true, "https://images.unsplash.com/photo-1427504494785-3a9ca7044f45?w=200&auto=format&fit=crop&q=60"),
                    ServiceProvider("prov_4", "مركز النخبة السريع لصيانة السيارات", "Al-Nokhbah Car Repair Quick Center", "775667788", "cat_5", 4.6f, true, "https://images.unsplash.com/photo-1486006920555-c77dce18193b?w=200&auto=format&fit=crop&q=60"),
                    ServiceProvider("prov_5", "البرق لشحن وتوصيل الطرود", "Al-Barq Shipping & Delivery Express", "777445566", "cat_7", 4.9f, true, "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=200&auto=format&fit=crop&q=60")
                )
                serviceProviderDao.insertServiceProviders(defaultProviders)
                for (prov in defaultProviders) {
                    firestore.collection("service_providers").document(prov.id).set(prov)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fail-safe provider seeding failed: ${e.message}")
        }

        // Seed a default admin if non-existent in Firestore, as a secure fallback
        try {
            val databaseAdmins = getAdmins()
            if (databaseAdmins.isEmpty()) {
                val seedAdmin = Admin("admin", "maher736462", "SuperAdmin", true)
                createAdmin(seedAdmin)
                Log.d(TAG, "Seeded default admin to Firestore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fail-safe admin seeding failed: ${e.message}")
        }
    }

    suspend fun saveCategory(category: Category) {
        categoryDao.insertCategory(category)
        try {
            firestore.collection("categories").document(category.id).set(category).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Category insert failure: ${e.message}")
        }
    }

    suspend fun updateCategory(id: String, nameAr: String, nameEn: String, icon: String, orderIndex: Int, imageUrl: String? = null) {
        val cat = Category(id, nameAr, nameEn, icon, orderIndex, true, imageUrl)
        categoryDao.insertCategory(cat)
        try {
            firestore.collection("categories").document(id).set(cat).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Category update failure: ${e.message}")
        }
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.deleteCategoryById(id)
        try {
            firestore.collection("categories").document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Category delete failure: ${e.message}")
        }
    }

    suspend fun saveServiceProvider(provider: ServiceProvider) {
        serviceProviderDao.insertServiceProvider(provider)
        try {
            firestore.collection("service_providers").document(provider.id).set(provider).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Provider insert failure: ${e.message}")
        }
    }

    suspend fun updateServiceProvider(
        id: String,
        nameAr: String,
        nameEn: String,
        phone: String,
        categoryId: String,
        rating: Float,
        imageUrl: String?
    ) {
        val provider = ServiceProvider(id, nameAr, nameEn, phone, categoryId, rating, true, imageUrl)
        serviceProviderDao.insertServiceProvider(provider)
        try {
            firestore.collection("service_providers").document(id).set(provider).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Provider update failure: ${e.message}")
        }
    }

    suspend fun deleteServiceProvider(id: String) {
        serviceProviderDao.deleteProviderById(id)
        try {
            firestore.collection("service_providers").document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Provider delete failure: ${e.message}")
        }
    }

    suspend fun saveReview(review: Review) {
        reviewDao.insertReview(review)
        try {
            firestore.collection("reviews").document(review.id).set(review).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Review insert failure: ${e.message}")
        }
    }

    suspend fun deleteReview(id: String) {
        reviewDao.deleteReviewById(id)
        try {
            firestore.collection("reviews").document(id).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Review delete failure: ${e.message}")
        }
    }

    suspend fun getAdmins(): List<Admin> {
        return try {
            val snapshot = firestore.collection("admins").get().await()
            val adminsList = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Admin::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            adminsList
        } catch (e: Exception) {
            Log.e(TAG, "Firestore getAdmins failure: ${e.message}")
            emptyList()
        }
    }

    suspend fun createAdmin(admin: Admin) {
        try {
            firestore.collection("admins").document(admin.username).set(admin).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Admin creation failure: ${e.message}")
        }
    }

    suspend fun updateAdminPassword(username: String, newPasswordHash: String) {
        try {
            firestore.collection("admins").document(username).update("passwordHash", newPasswordHash).await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Admin update password failure: ${e.message}")
        }
    }

    suspend fun deleteAdmin(username: String) {
        try {
            firestore.collection("admins").document(username).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore Admin deletion failure: ${e.message}")
        }
    }
}
