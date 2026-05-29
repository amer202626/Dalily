package com.yemenservices.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class Repository {
    private val firestore = FirebaseFirestore.getInstance()

    // Base fallback configurations to populate if remote database is empty
    private val initialCategories = listOf(
        Category("cat_1", "سباكة وصحي", "Plumbing & Sanitary", "plumbing", 1, "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=500&q=80"),
        Category("cat_2", "كهرباء منازل", "Home Electricity", "bolt", 2, "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&q=80"),
        Category("cat_3", "صيانة تكييف", "AC Repair & Maintenance", "ac_unit", 3, "https://images.unsplash.com/photo-1621905252507-b354bc25edac?w=500&q=80"),
        Category("cat_4", "صيانة هواتف", "Mobile Maintenance", "smartphone", 4, "https://images.unsplash.com/photo-1597740985671-2a8a3b80f017?w=500&q=80"),
        Category("cat_5", "خياطة وتطريز", "Tailoring & Fashion", "architecture", 5, "https://images.unsplash.com/photo-1544816155-12df9643f363?w=500&q=80"),
        Category("cat_6", "نقل وأجرة", "Transport & Taxi", "local_shipping", 6, "https://images.unsplash.com/photo-1516574187841-cb9cc2ca948b?w=500&q=80")
    )

    private val initialProviders = listOf(
        ServiceProvider("p_1", "cat_1", "المهندس عادل السباك", "Engineer Adel Plumber", "777123456", "777123456", "777123456", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80", is_pinned = true, is_approved = true, "low", "close", 15.3522, 44.2078),
        ServiceProvider("p_2", "cat_1", "سباكة السلام الفنية", "Al-Salam Technical Plumbers", "733987654", "733987654", "733987654", null, is_pinned = false, is_approved = true, "medium", "medium", 15.3694, 44.1910),
        ServiceProvider("p_3", "cat_2", "كهربائي يمني محترف", "Professional Yemeni Electrician", "711223344", "711223344", "711223344", "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=200&q=80", is_pinned = true, is_approved = true, "medium", "close", 15.3812, 44.1802),
        ServiceProvider("p_4", "cat_4", "تكنو فون لصيانة الموبايل", "Techno Phone Repair", "770112233", "770112233", "770112233", "https://images.unsplash.com/photo-1597740985671-2a8a3b80f017?w=200&q=80", is_pinned = false, is_approved = true, "high", "far", 15.3400, 44.2200),
        ServiceProvider("p_5", "cat_3", "برق الشمال للتكييف", "Northern Lightning for AC", "775664422", "775664422", "", null, is_pinned = false, is_approved = true, "high", "medium", 15.3650, 44.1980)
    )

    private val initialReviews = listOf(
        Review("r_1", "p_1", "أحمد السعيدي", "خدمة سريعة وممتازة وسعر مناسب جداً أنصح بالتعامل معه", 5.0),
        Review("r_2", "p_1", "Amr Ali", "Excellent plumber, fixed my bathroom leakage efficiently", 5.0),
        Review("r_3", "p_3", "خالد الكبسي", "مهندس ممتاز جدا ومحاضر وخبير", 4.0)
    )

    // Listen to Categories Flow
    fun getCategories(): Flow<List<Category>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(initialCategories)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Category::class.java)
                    if (list.isEmpty()) {
                        // Populate remote with default categories if empty
                        initialCategories.forEach { addCategory(it) }
                        trySend(initialCategories)
                    } else {
                        trySend(list.sortedBy { it.order_index })
                    }
                } else {
                    trySend(initialCategories)
                }
            }
        awaitClose { listener.remove() }
    }

    // Listen to Providers Flow
    fun getProviders(): Flow<List<ServiceProvider>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("providers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(initialProviders)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(ServiceProvider::class.java)
                    if (list.isEmpty()) {
                        initialProviders.forEach { addProvider(it) }
                        trySend(initialProviders)
                    } else {
                        trySend(list)
                    }
                } else {
                    trySend(initialProviders)
                }
            }
        awaitClose { listener.remove() }
    }

    // Listen to Reviews Flow
    fun getReviews(): Flow<List<Review>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("reviews")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(initialReviews)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Review::class.java)
                    if (list.isEmpty()) {
                        initialReviews.forEach { addReview(it) }
                        trySend(initialReviews)
                    } else {
                        trySend(list)
                    }
                } else {
                    trySend(initialReviews)
                }
            }
        awaitClose { listener.remove() }
    }

    // Listen to AppConfig Flow
    fun getAppConfig(): Flow<AppConfig> = callbackFlow {
        val docRef = firestore.collection("config").document("singleton")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppConfig())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val config = snapshot.toObject(AppConfig::class.java)
                trySend(config ?: AppConfig())
            } else {
                // Prepopulate
                val defaultConfig = AppConfig()
                docRef.set(defaultConfig)
                trySend(defaultConfig)
            }
        }
        awaitClose { listener.remove() }
    }

    // Listen to Supervisors Flow
    fun getSupervisors(): Flow<List<Supervisor>> = callbackFlow {
        val listener = firestore.collection("supervisors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(listOf(Supervisor("admin_1", "maher", "736462", "admin")))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Supervisor::class.java)
                    if (list.isEmpty()) {
                        val initSuper = Supervisor("admin_1", "maher", "736462", "admin")
                        addSupervisor(initSuper)
                        trySend(listOf(initSuper))
                    } else {
                        trySend(list)
                    }
                } else {
                    trySend(listOf(Supervisor("admin_1", "maher", "736462", "admin")))
                }
            }
        awaitClose { listener.remove() }
    }

    // --- Write/Update Operations ---

    fun addCategory(category: Category) {
        val docId = category.id.ifBlank { UUID.randomUUID().toString() }
        val finalCategory = category.copy(id = docId)
        firestore.collection("categories").document(docId).set(finalCategory)
    }

    fun updateCategory(category: Category) {
        firestore.collection("categories").document(category.id).set(category)
    }

    fun deleteCategory(categoryId: String) {
        firestore.collection("categories").document(categoryId).delete()
    }

    fun addProvider(provider: ServiceProvider) {
        val docId = provider.id.ifBlank { UUID.randomUUID().toString() }
        val finalProvider = provider.copy(id = docId)
        firestore.collection("providers").document(docId).set(finalProvider)
    }

    fun updateProvider(provider: ServiceProvider) {
        firestore.collection("providers").document(provider.id).set(provider)
    }

    fun deleteProvider(providerId: String) {
        firestore.collection("providers").document(providerId).delete()
    }

    fun addReview(review: Review) {
        val docId = review.id.ifBlank { UUID.randomUUID().toString() }
        val finalReview = review.copy(id = docId)
        firestore.collection("reviews").document(docId).set(finalReview)
    }

    fun deleteReview(reviewId: String) {
        firestore.collection("reviews").document(reviewId).delete()
    }

    fun addSupervisor(supervisor: Supervisor) {
        val docId = supervisor.id.ifBlank { UUID.randomUUID().toString() }
        val finalSup = supervisor.copy(id = docId)
        firestore.collection("supervisors").document(docId).set(finalSup)
    }

    fun deleteSupervisor(supervisorId: String) {
        firestore.collection("supervisors").document(supervisorId).delete()
    }

    fun updateAppConfig(config: AppConfig) {
        firestore.collection("config").document("singleton").set(config)
    }
}
