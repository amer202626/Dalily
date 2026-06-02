package com.yemenservices.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class Repository(private val context: Context) {

    private val firestore: FirebaseFirestore
    private val rtdb: FirebaseDatabase?

    init {
        // Enforce safe initialization
        val app = try {
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:963621716942:android:375a24068863dbd490a19f")
                    .setProjectId("yemenservices-fd56c")
                    .setApiKey("AIzaSyCpxwXZZKrN4h2AmyuEkzyat0K4LOUAXD8")
                    .setStorageBucket("yemenservices-fd56c.firebasestorage.app")
                    .setDatabaseUrl("https://yemenservices-fd56c-default-rtdb.firebaseio.com")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } catch (initEx: Exception) {
                Log.e("Repository", "Firebase manual init fallthrough: ${initEx.message}")
                null
            }
        }

        firestore = FirebaseFirestore.getInstance()
        rtdb = try {
            FirebaseDatabase.getInstance()
        } catch (dbEx: Exception) {
            Log.e("Repository", "Realtime Database init failed: ${dbEx.message}")
            null
        }

        // Initialize default mock/seed data if Firestore collections are empty to ensure a good first-run experience
        seedInitialCategories()
    }

    private fun seedInitialCategories() {
        firestore.collection("categories").get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                val demoCats = listOf(
                    Category("1", "الهندسة والصيانة", "Engineering & Maintenance", "🛠️", 1),
                    Category("2", "خدمات الاتصالات", "Telecom & Network Services", "📱", 2),
                    Category("3", "سيارات ونقل", "Cars & Transportation", "🚕", 3),
                    Category("4", "الطب والتمريض والخدمات الوقائية", "Medicine & Healthcare", "🩺", 4),
                    Category("5", "التعليم والتدريب", "Education & Training", "📚", 5)
                )
                demoCats.forEach { firestore.collection("categories").document(it.id).set(it) }

                val demoSubs = listOf(
                    SubCategory("1_1", "1", "صيانة هواتف", "Phone Repair", 1),
                    SubCategory("1_2", "1", "صيانة حواسيب", "PC Repair", 2),
                    SubCategory("1_3", "1", "تركيب كاميرات", "CCTV Installation", 3),
                    SubCategory("2_1", "2", "شبكات لاسلكية", "Wi-Fi Networks", 1),
                    SubCategory("2_2", "2", "برمجة سيرفرات", "Server Configuration", 2),
                    SubCategory("3_1", "3", "توصيل طلبات", "Delivery", 1),
                    SubCategory("3_2", "3", "ميكانيكي سيارات", "Car Mechanic", 2),
                    SubCategory("4_1", "4", "تمريض منزلي", "Home Nursing", 1),
                    SubCategory("4_2", "4", "طبيب عام استشارات", "Consultations", 2)
                )
                demoSubs.forEach { firestore.collection("subcategories").document(it.id).set(it) }

                val demoServices = listOf(
                    YemenService(
                        id = "serv1",
                        category = "1",
                        subCategory = "1_1",
                        nameAr = "صيانة الآيفون الاحترافية والسامسونج",
                        nameEn = "Professional iPhone & Samsung Repair",
                        phoneNumber = "777123456",
                        descriptionAr = "أفضل مركز صيانة هواتف ذكية بأحدث الأجهزة وتوفير قطع الغيار الأصلية مع ضمان حقيقي.",
                        descriptionEn = "Outstanding smartphone repair center with direct support, original parts, and true repair warranty.",
                        addressAr = "صنعاء - شارع صخر",
                        addressEn = "Sana'a - Sakhar Street",
                        imageUrl = "https://images.unsplash.com/photo-1597740985671-2a8a3b80502e?w=500",
                        rating = 4.8f,
                        isPinned = true,
                        isRecommended = true,
                        orderIndex = 1,
                        workPlace = "أمانة العاصمة",
                        residencePlace = "صنعاء الحصبة"
                    ),
                    YemenService(
                        id = "serv2",
                        category = "1",
                        subCategory = "1_2",
                        nameAr = "مكتب جيل التقنية لصيانة اللابتوب والشبكات",
                        nameEn = "Tech Generation PC Maintenance",
                        phoneNumber = "733221100",
                        descriptionAr = "متخصصون في حلول الشبكات وصيانة الحواسيب المحمولة والأجهزة اللوحية وتثبيت كروت الواي فاي.",
                        descriptionEn = "Specialized in computer hardware fixes, server cabling, backup systems, and enterprise routers.",
                        addressAr = "عدن - المعلا الرئيسي",
                        addressEn = "Aden - Main Mualla Street",
                        imageUrl = "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=500",
                        rating = 4.6f,
                        isPinned = false,
                        isRecommended = true,
                        orderIndex = 2,
                        workPlace = "عدن وخورمكسر",
                        residencePlace = "عدن المعلا"
                    )
                )
                demoServices.forEach { firestore.collection("services").document(it.id).set(it) }
            }
        }
    }

    // --- REALTIME SYNC PATTERNS (SNAPSHOT LISTENERS) ---

    // 1. AppConfig Realtime Sync (Tries RTDB, fallbacks to Firestore)
    fun listenToConfigFlow(): Flow<AppConfig> = callbackFlow {
        if (rtdb != null) {
            val configRef = rtdb.getReference("app_config")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val config = snapshot.getValue(AppConfig::class.java)
                    if (config != null) {
                        trySend(config)
                    } else {
                        // Seed default configuration if empty
                        val defaultC = AppConfig()
                        configRef.setValue(defaultC)
                        trySend(defaultC)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Repository", "RTDB Config cancelled: ${error.message}")
                }
            }
            configRef.addValueEventListener(listener)
            awaitClose { configRef.removeEventListener(listener) }
        } else {
            // Firestore Fallback
            val docRef = firestore.collection("config").document("app_settings")
            val registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val config = snapshot.toObject(AppConfig::class.java)
                    if (config != null) trySend(config)
                } else {
                    val defaultC = AppConfig()
                    docRef.set(defaultC)
                    trySend(defaultC)
                }
            }
            awaitClose { registration.remove() }
        }
    }

    suspend fun saveAppConfig(config: AppConfig) {
        if (rtdb != null) {
            try {
                rtdb.getReference("app_config").setValue(config).await()
            } catch (e: Exception) {
                Log.e("Repository", "Failed saving config to RTDB, writing to Firestore: ${e.message}")
                firestore.collection("config").document("app_settings").set(config).await()
            }
        } else {
            firestore.collection("config").document("app_settings").set(config).await()
        }
    }

    // 2. Services Realtime Snapshot Listener
    fun listenToServicesFlow(): Flow<List<YemenService>> = callbackFlow {
        val registration = firestore.collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Repository", "Listen to services failed: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(YemenService::class.java)?.copy(id = it.id) }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveService(service: YemenService) {
        val docId = service.id.ifBlank { firestore.collection("services").document().id }
        val updated = service.copy(id = docId)
        firestore.collection("services").document(docId).set(updated).await()
    }

    suspend fun deleteService(id: String) {
        firestore.collection("services").document(id).delete().await()
    }

    // 3. Categories Snapshot Listener
    fun listenToCategoriesFlow(): Flow<List<Category>> = callbackFlow {
        val registration = firestore.collection("categories")
            .orderBy("orderIndex")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(Category::class.java)?.copy(id = it.id) }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveCategory(category: Category) {
        val docId = category.id.ifBlank { firestore.collection("categories").document().id }
        val updated = category.copy(id = docId)
        firestore.collection("categories").document(docId).set(updated).await()
    }

    suspend fun deleteCategory(id: String) {
        firestore.collection("categories").document(id).delete().await()
    }

    // 4. Subcategories Snapshot Listener
    fun listenToSubCategoriesFlow(): Flow<List<SubCategory>> = callbackFlow {
        val registration = firestore.collection("subcategories")
            .orderBy("orderIndex")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(SubCategory::class.java)?.copy(id = it.id) }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveSubCategory(subCategory: SubCategory) {
        val docId = subCategory.id.ifBlank { firestore.collection("subcategories").document().id }
        val updated = subCategory.copy(id = docId)
        firestore.collection("subcategories").document(docId).set(updated).await()
    }

    suspend fun deleteSubCategory(id: String) {
        firestore.collection("subcategories").document(id).delete().await()
    }

    // 5. Providers Joining Requests
    fun listenToJoinRequestsFlow(): Flow<List<ProviderJoinRequest>> = callbackFlow {
        val registration = firestore.collection("join_requests")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(ProviderJoinRequest::class.java)?.copy(id = it.id) }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveJoinRequest(request: ProviderJoinRequest) {
        val docId = request.id.ifBlank { firestore.collection("join_requests").document().id }
        val updated = request.copy(id = docId)
        firestore.collection("join_requests").document(docId).set(updated).await()
    }

    suspend fun deleteJoinRequest(id: String) {
        firestore.collection("join_requests").document(id).delete().await()
    }

    // 6. Comments & Ratings Snapshot Listener
    fun listenToCommentsFlow(serviceId: String): Flow<List<Comment>> = callbackFlow {
        val registration = firestore.collection("comments")
            .whereEqualTo("serviceId", serviceId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Repository", "Comments sync error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(Comment::class.java)?.copy(id = it.id) }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveComment(comment: Comment) {
        val docId = comment.id.ifBlank { firestore.collection("comments").document().id }
        val updated = comment.copy(id = docId)
        firestore.collection("comments").document(docId).set(updated).await()
    }

    // 7. Supervisors System RTDB/Firestore Synchronizer
    fun listenToSupervisorsFlow(): Flow<List<SupervisorAccount>> = callbackFlow {
        if (rtdb != null) {
            val supervisorsRef = rtdb.getReference("supervisors")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<SupervisorAccount>()
                    snapshot.children.forEach { child ->
                        child.getValue(SupervisorAccount::class.java)?.copy(id = child.key ?: "")?.let {
                            items.add(it)
                        }
                    }
                    if (items.isEmpty()) {
                        // Seed initial superadmin
                        val admin = SupervisorAccount("admin", "admin", "admin123", "ADMIN", true)
                        supervisorsRef.child("admin").setValue(admin)
                        items.add(admin)
                    }
                    trySend(items)
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            supervisorsRef.addValueEventListener(listener)
            awaitClose { supervisorsRef.removeEventListener(listener) }
        } else {
            val registration = firestore.collection("supervisors")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val items = snapshot.documents.mapNotNull { it.toObject(SupervisorAccount::class.java)?.copy(id = it.id) }
                        if (items.isEmpty()) {
                            val admin = SupervisorAccount("admin", "admin", "admin123", "ADMIN", true)
                            firestore.collection("supervisors").document("admin").set(admin)
                            trySend(listOf(admin))
                        } else {
                            trySend(items)
                        }
                    }
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun saveSupervisor(account: SupervisorAccount) {
        val keyId = account.username.lowercase().trim()
        val updated = account.copy(id = keyId, username = keyId)
        if (rtdb != null) {
            rtdb.getReference("supervisors").child(keyId).setValue(updated).await()
        } else {
            firestore.collection("supervisors").document(keyId).set(updated).await()
        }
    }

    suspend fun deleteSupervisor(id: String) {
        val keyId = id.lowercase().trim()
        if (rtdb != null) {
            rtdb.getReference("supervisors").child(keyId).removeValue().await()
        } else {
            firestore.collection("supervisors").document(keyId).delete().await()
        }
    }

    // --- FAVORITES (LOCAL PERSISTENCE VIA SECURE SHARED PREFERENCES) ---
    private val sharedPrefs = context.getSharedPreferences("dalili_prefs", Context.MODE_PRIVATE)

    fun getFavorites(): Set<String> {
        return sharedPrefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    fun saveFavorites(favorites: Set<String>) {
        sharedPrefs.edit().putStringSet("favorites", favorites).apply()
    }
}
