package com.dalily.services.data

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object FirebaseRepository {
    private const val TAG = "FirebaseRepository"
    private lateinit var db: FirebaseFirestore
    private val scope = CoroutineScope(Dispatchers.IO)

    // StateFlows that Compose UI can directly collect for instant reactions
    val categories = MutableStateFlow<List<Category>>(emptyList())
    val subCategories = MutableStateFlow<List<SubCategory>>(emptyList())
    val serviceProviders = MutableStateFlow<List<ServiceProvider>>(emptyList())
    val pendingProviders = MutableStateFlow<List<PendingProvider>>(emptyList())
    val banners = MutableStateFlow<List<BannerAd>>(emptyList())
    val appConfig = MutableStateFlow<AppConfig>(AppConfig())
    val chatMessagesByProvider = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val reviewsByProvider = MutableStateFlow<Map<String, List<UserReview>>>(emptyMap())

    // Tracks if database is successfully initialized with Firestore
    private var isRealFirebaseActive = false

    fun initialize(context: Context) {
        try {
            // Enable offline persistence using standard Java API
            db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
            isRealFirebaseActive = true
            Log.d(TAG, "Real Firebase Firestore successfully configured with local persistence")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase. Falling back to Simulated cloud-native sync", e)
            isRealFirebaseActive = false
        }

        // Initialize listeners
        setupRealtimeListeners()
    }

    private fun setupRealtimeListeners() {
        if (isRealFirebaseActive) {
            setupFirestoreListeners()
        } else {
            setupSimulatedCloudListeners()
        }
    }

    private fun setupFirestoreListeners() {
        // 1. Categories
        db.collection("categories").orderBy("orderIndex").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Categories snapshot error, using simulated backup", e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(Category::class.java)?.copy(id = it.id) }
                if (list.isEmpty()) {
                    seedDefaultCategories()
                } else {
                    categories.value = list
                }
            }
        }

        // 2. SubCategories
        db.collection("subcategories").addSnapshotListener { snapshot, e ->
            if (e == null && snapshot != null) {
                subCategories.value = snapshot.documents.mapNotNull { it.toObject(SubCategory::class.java)?.copy(id = it.id) }
            }
        }

        // 3. Service Providers
        db.collection("service_providers").addSnapshotListener { snapshot, e ->
            if (e == null && snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(ServiceProvider::class.java)?.copy(id = it.id) }
                if (list.isEmpty() && categories.value.isNotEmpty()) {
                    seedDefaultProviders()
                } else {
                    serviceProviders.value = list
                }
            }
        }

        // 4. Pending Providers
        db.collection("pending_providers").addSnapshotListener { snapshot, e ->
            if (e == null && snapshot != null) {
                pendingProviders.value = snapshot.documents.mapNotNull { it.toObject(PendingProvider::class.java)?.copy(id = it.id) }
            }
        }

        // 5. Banners
        db.collection("banners").addSnapshotListener { snapshot, e ->
            if (e == null && snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(BannerAd::class.java)?.copy(id = it.id) }
                if (list.isEmpty()) {
                    seedDefaultBanners()
                } else {
                    banners.value = list
                }
            }
        }

        // 6. Application Configuration
        db.collection("config").document("app_config").addSnapshotListener { doc, e ->
            if (e == null && doc != null && doc.exists()) {
                val config = doc.toObject(AppConfig::class.java)
                if (config != null) {
                    appConfig.value = config
                }
            } else {
                // If configuration doesn't exist, seed it
                seedDefaultConfig()
            }
        }

        // 7. Core Reviews & Messages
        db.collection("reviews").addSnapshotListener { snapshot, e ->
            if (e == null && snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(UserReview::class.java)?.copy(id = it.id) }
                reviewsByProvider.value = list.groupBy { it.providerId }
            }
        }

        db.collection("messages").addSnapshotListener { snapshot, e ->
            if (e == null && snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java)?.copy(id = it.id) }
                chatMessagesByProvider.value = list.groupBy { it.providerId }
            }
        }
    }

    private fun setupSimulatedCloudListeners() {
        // Fallback or Sandbox Environment if firebase is not fully online on build environment
        // Seed default dataset
        seedDefaultCategories()
        seedDefaultBanners()
        seedDefaultConfig()
        seedDefaultProviders()
    }

    // Seeding default sandbox values
    private fun seedDefaultCategories() {
        val list = listOf(
            Category("cat_maintain", "صيانة منزلية", "Home Maintenance", "Build", "#0D9488", true, 0),
            Category("cat_health", "صحة ورعاية", "Health & Care", "Favorite", "#D946EF", true, 1),
            Category("cat_edu", "تعليم وتدريب", "Education & Training", "Book", "#3B82F6", false, 2),
            Category("cat_transport", "نقل وخدمات", "Transportation & Logistics", "LocalShipping", "#F59E0B", false, 3),
            Category("cat_tech", "خدمات تقنية", "Tech & Coding", "Computer", "#10B981", false, 4)
        )
        if (isRealFirebaseActive) {
            for (cat in list) {
                db.collection("categories").document(cat.id).set(cat)
            }
        } else {
            categories.value = list
        }

        // Seed subcategories
        val subList = listOf(
            SubCategory("sub_plumber", "cat_maintain", "سباكة وتركيب صحي", "Plumbing"),
            SubCategory("sub_electrician", "cat_maintain", "كهرباء وتمديدات", "Electrical"),
            SubCategory("sub_ac", "cat_maintain", "تكييف وتبريد", "AC Repair"),
            SubCategory("sub_doctor", "cat_health", "استشارة طبية", "Medical Consultation"),
            SubCategory("sub_nurse", "cat_health", "تمريض رعاية منزلية", "Home Nursing"),
            SubCategory("sub_maths", "cat_edu", "رياضيات وفيزياء", "Maths & Physics"),
            SubCategory("sub_english", "cat_edu", "لغة إنجليزية", "English Language"),
            SubCategory("sub_furniture", "cat_transport", "نقل أثاث وشحن", "Furniture Moving")
        )
        if (isRealFirebaseActive) {
            for (sub in subList) {
                db.collection("subcategories").document(sub.id).set(sub)
            }
        } else {
            subCategories.value = subList
        }
    }

    private fun seedDefaultProviders() {
        val list = listOf(
            ServiceProvider(
                id = "prov_maher",
                name = "المهندس ماهر محمد طاهر",
                category = "cat_maintain",
                subCategory = "sub_electrician",
                phone = "777644670",
                whatsapp = "777644670",
                description = "فني كهرباء منازل وتمديدات حديثة خبرة 10 سنوات في صيانة وتركيب لوحات التوزيع الذكية ومستلزمات الإنارة الحديثة بفريق متكامل.",
                imageUrl = "https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=150",
                address = "شارع الستين - صنعاء",
                residenceRegion = "مديرية السبعين",
                isPinned = true,
                isRecommended = true,
                rating = 4.9,
                reviewsCount = 4
            ),
            ServiceProvider(
                id = "prov_yaser",
                name = "أكرم الحميري لصيانة المكيفات",
                category = "cat_maintain",
                subCategory = "sub_ac",
                phone = "777123456",
                whatsapp = "777123456",
                description = "صيانة وتركيب جميع أنواع المكيفات (سبليت، دولابي، مركزي) وتعبئة الفريون وتصليح جميع الأعطال بسرعة وجودة عالية.",
                imageUrl = "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=150",
                address = "شارع الجزائر - صنعاء",
                residenceRegion = "مديرية الوحدة",
                isPinned = false,
                isRecommended = true,
                rating = 4.8,
                reviewsCount = 2
            ),
            ServiceProvider(
                id = "prov_dr_amjad",
                name = "الدكتور أمجد القدسي",
                category = "cat_health",
                subCategory = "sub_doctor",
                phone = "770112233",
                whatsapp = "770112233",
                description = "استشاري طب الأسرة والطب الباطني، رعاية طبية شاملة لجميع أفراد الأسرة وزيارات منزلية للحالات المستعصية وكبار السن.",
                imageUrl = "https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=150",
                address = "شارع حدة - صنعاء",
                residenceRegion = "مديرية السبعين",
                isPinned = true,
                isRecommended = false,
                rating = 5.0,
                reviewsCount = 3
            )
        )
        if (isRealFirebaseActive) {
            for (prov in list) {
                db.collection("service_providers").document(prov.id).set(prov)
            }
        } else {
            serviceProviders.value = list
        }

        // Seed some reviews
        val reviewList = listOf(
            UserReview("rev1", "prov_maher", "أحمد الوادعي", 5, "شخص خلوق جداً وسريع في الكشف عن الالتماس وإصلاحه بأمانة عالية وسعر معقول للغاية! рекомендую."),
            UserReview("rev2", "prov_maher", "سليم الحاشدي", 5, "ممتاز وملتزم بالوقت، خدمة رائعة ومهندس ذكي يعرف عمله بالتفصيل."),
            UserReview("rev3", "prov_dr_amjad", "أم خالد", 5, "أفضل طبيب استشرناه، دقيق جداً في التشخيص ولديه سعة صدر في الاستماع لتفاصيل الأعراض.")
        )
        if (isRealFirebaseActive) {
            for (rev in reviewList) {
                db.collection("reviews").document(rev.id).set(rev)
            }
        } else {
            reviewsByProvider.value = reviewList.groupBy { it.providerId }
        }
    }

    private fun seedDefaultBanners() {
        val list = listOf(
            BannerAd(
                id = "ban1",
                title = "أكبر صيانة صيفية للمنازل",
                imageUrl = "https://images.unsplash.com/photo-1581092921461-eab62e97a780?w=600",
                linkUrl = "777644670",
                durationSeconds = 6,
                priorityOrder = 1
            ),
            BannerAd(
                id = "ban2",
                title = "رعاية طبية تخصصية متميزة لمنزلك",
                imageUrl = "https://images.unsplash.com/photo-1576091160550-2173dba999ef?w=600",
                linkUrl = "777123456",
                durationSeconds = 4,
                priorityOrder = 2
            ),
            BannerAd(
                id = "ban3",
                title = "خصومات حصرية على نقل الأثاث المضمون",
                imageUrl = "https://images.unsplash.com/photo-1603796846097-bee99e4a60c9?w=600",
                linkUrl = "770112233",
                durationSeconds = 5,
                priorityOrder = 3
            )
        )
        if (isRealFirebaseActive) {
            for (ban in list) {
                db.collection("banners").document(ban.id).set(ban)
            }
        } else {
            banners.value = list
        }
    }

    private fun seedDefaultConfig() {
        val config = AppConfig()
        if (isRealFirebaseActive) {
            db.collection("config").document("app_config").set(config)
        } else {
            appConfig.value = config
        }
    }

    // Methods to write/modify data

    // CATEGORIES
    fun addCategory(category: Category, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docId = if (category.id.isEmpty()) db.collection("categories").document().id else category.id
        val finalCat = category.copy(id = docId, orderIndex = categories.value.size)
        if (isRealFirebaseActive) {
            db.collection("categories").document(docId).set(finalCat)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        } else {
            val newList = categories.value.filter { it.id != docId } + finalCat
            categories.value = newList.sortedBy { it.orderIndex }
            onSuccess()
        }
    }

    fun addSubCategory(sub: SubCategory, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docId = if (sub.id.isEmpty()) db.collection("subcategories").document().id else sub.id
        val finalSub = sub.copy(id = docId)
        if (isRealFirebaseActive) {
            db.collection("subcategories").document(docId).set(finalSub)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        } else {
            subCategories.value = subCategories.value.filter { it.id != docId } + finalSub
            onSuccess()
        }
    }

    // SERVICE PROVIDERS
    fun addServiceProvider(provider: ServiceProvider, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docId = if (provider.id.isEmpty()) db.collection("service_providers").document().id else provider.id
        val finalProv = provider.copy(id = docId)
        if (isRealFirebaseActive) {
            db.collection("service_providers").document(docId).set(finalProv)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        } else {
            serviceProviders.value = serviceProviders.value.filter { it.id != docId } + finalProv
            onSuccess()
        }
    }

    fun updateProviderPinStatus(id: String, isPinned: Boolean, isRecommended: Boolean, onSuccess: () -> Unit) {
        if (isRealFirebaseActive) {
            db.collection("service_providers").document(id)
                .update("pinned", isPinned, "recommended", isRecommended)
                .addOnSuccessListener { onSuccess() }
        } else {
            serviceProviders.value = serviceProviders.value.map {
                if (it.id == id) it.copy(isPinned = isPinned, isRecommended = isRecommended) else it
            }
            onSuccess()
        }
    }

    fun deleteServiceProvider(id: String, onSuccess: () -> Unit) {
        if (isRealFirebaseActive) {
            db.collection("service_providers").document(id).delete()
                .addOnSuccessListener { onSuccess() }
        } else {
            serviceProviders.value = serviceProviders.value.filter { it.id != id }
            onSuccess()
        }
    }

    // PENDING PROVIDERS (Registration Requests)
    fun addPendingProvider(pending: PendingProvider, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docId = if (pending.id.isEmpty()) db.collection("pending_providers").document().id else pending.id
        val finalPend = pending.copy(id = docId)
        if (isRealFirebaseActive) {
            db.collection("pending_providers").document(docId).set(finalPend)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        } else {
            pendingProviders.value = pendingProviders.value.filter { it.id != docId } + finalPend
            onSuccess()
        }
    }

    fun reviewPendingProvider(id: String, approve: Boolean, reason: String = "", onSuccess: () -> Unit) {
        val request = pendingProviders.value.find { it.id == id } ?: return
        if (approve) {
            // Transfer to real service provider
            val newProvider = ServiceProvider(
                id = db.collection("service_providers").document().id,
                name = request.name,
                category = request.category,
                phone = request.phone,
                whatsapp = request.phone,
                imageUrl = request.profileImageUrl,
                address = request.address,
                residenceRegion = request.residenceRegion
            )
            addServiceProvider(newProvider, {
                // Delete request or mark as approved
                if (isRealFirebaseActive) {
                    db.collection("pending_providers").document(id).delete()
                        .addOnSuccessListener { onSuccess() }
                } else {
                    pendingProviders.value = pendingProviders.value.filter { it.id != id }
                    onSuccess()
                }
            }, {
                Log.e(TAG, "Failed transferring registration to service providers", it)
            })
        } else {
            // Update or Delete to Rejected
            if (isRealFirebaseActive) {
                db.collection("pending_providers").document(id).update("status", "REJECTED", "rejectionReason", reason)
                    .addOnSuccessListener { onSuccess() }
            } else {
                pendingProviders.value = pendingProviders.value.map {
                    if (it.id == id) it.copy(status = "REJECTED", rejectionReason = reason) else it
                }
                onSuccess()
            }
        }
    }

    // BANNERS
    fun addBanner(banner: BannerAd, onSuccess: () -> Unit) {
        val docId = if (banner.id.isEmpty()) db.collection("banners").document().id else banner.id
        val finalBan = banner.copy(id = docId)
        if (isRealFirebaseActive) {
            db.collection("banners").document(docId).set(finalBan)
                .addOnSuccessListener { onSuccess() }
        } else {
            banners.value = banners.value.filter { it.id != docId } + finalBan
            onSuccess()
        }
    }

    fun deleteBanner(id: String, onSuccess: () -> Unit) {
        if (isRealFirebaseActive) {
            db.collection("banners").document(id).delete()
                .addOnSuccessListener { onSuccess() }
        } else {
            banners.value = banners.value.filter { it.id != id }
            onSuccess()
        }
    }

    // CONFIG
    fun updateAppConfig(config: AppConfig, onSuccess: () -> Unit) {
        if (isRealFirebaseActive) {
            db.collection("config").document("app_config").set(config)
                .addOnSuccessListener { onSuccess() }
        } else {
            appConfig.value = config
            onSuccess()
        }
    }

    // REVIEWS
    fun addReview(review: UserReview, onSuccess: () -> Unit) {
        val docId = db.collection("reviews").document().id
        val finalReview = review.copy(id = docId)
        if (isRealFirebaseActive) {
            db.collection("reviews").document(docId).set(finalReview)
                .addOnSuccessListener { onSuccess() }
        } else {
            val list = (reviewsByProvider.value[review.providerId] ?: emptyList()) + finalReview
            val updated = reviewsByProvider.value.toMutableMap()
            updated[review.providerId] = list
            reviewsByProvider.value = updated
            onSuccess()
        }
    }

    // CHAT MESSAGES
    fun sendMessage(msg: ChatMessage, onSuccess: () -> Unit) {
        val docId = db.collection("messages").document().id
        val finalMsg = msg.copy(id = docId)
        if (isRealFirebaseActive) {
            db.collection("messages").document(docId).set(finalMsg)
                .addOnSuccessListener { onSuccess() }
        } else {
            val list = (chatMessagesByProvider.value[msg.providerId] ?: emptyList()) + finalMsg
            val updated = chatMessagesByProvider.value.toMutableMap()
            updated[msg.providerId] = list
            chatMessagesByProvider.value = updated
            onSuccess()
        }
    }

    // General Manual Force Refresh triggered on demand by users 🔄
    fun forceSync(onComplete: () -> Unit) {
        setupRealtimeListeners()
        onComplete()
    }
}
