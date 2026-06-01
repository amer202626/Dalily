package com.yemenservices.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class Repository(context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("dalili_prefs", Context.MODE_PRIVATE)

    private val defaultCategories = listOf(
        ServiceCategory("emergency", "الطوارئ والإنقاذ", "Emergency & Rescue", "🚨", true, 1),
        ServiceCategory("medical", "المستشفيات والطبية", "Hospitals & Medical", "🏥", true, 2),
        ServiceCategory("finance", "البنوك والخدمات المالية", "Banks & Finance", "💼", false, 3),
        ServiceCategory("transport", "النقل والسفر", "Transport & Travel", "🚌", false, 4),
        ServiceCategory("government", "الخدمات الحكومية العامة", "Government Services", "🏛️", false, 5)
    )

    private val defaultSubCategories = listOf(
        ServiceSubCategory("dentist", "medical", "أطباء أسنان", "Dentists", "🦷", 1),
        ServiceSubCategory("pharmacy", "medical", "صيدليات مناوبة", "Pharmacies", "💊", 2),
        ServiceSubCategory("clinic", "medical", "عيادات تخصصية", "Specialty Clinics", "🩺", 3),
        ServiceSubCategory("fire", "emergency", "إطفاء وحرائق", "Fire & Rescue", "🚒", 1),
        ServiceSubCategory("red_crescent", "emergency", "الهلال الأحمر الإسعافي", "Red Crescent", "🚑", 2),
        ServiceSubCategory("banks", "finance", "بنوك ومصارف", "Banks & Banking", "🏦", 1),
        ServiceSubCategory("exchanges", "finance", "صرافة وحوالات", "Money Exchanges", "💸", 2),
        ServiceSubCategory("flights", "transport", "حجز تكت طيران", "Air Travel", "✈️", 1),
        ServiceSubCategory("land_coaches", "transport", "نقل جماعي بري", "Land Transport", "🚌", 2),
        ServiceSubCategory("utilities", "government", "شكاوى وخدمات عامة", "Public Utilities", "📞", 1)
    )

    private val defaultServices = listOf(
        // Emergency
        YemenService(
            id = "e1",
            nameAr = "الدفاع المدني (الإطفاء لحالات الحريق)",
            nameEn = "Civil Defense (Fire Emergency)",
            category = "emergency",
            subCategory = "fire",
            phoneNumber = "191",
            whatsappNumber = "967191",
            addressAr = "اليمن - جميع المحافظات",
            addressEn = "Yemen - All Governorates",
            rating = 5.0f,
            imageUrl = "https://images.unsplash.com/photo-1513530534585-c7b1394c6d51?w=400",
            descriptionAr = "الاتصال المجاني للدفاع المدني والإطفاء لحالات الطوارئ والحرائق في اليمن.",
            descriptionEn = "Toll-free emergency number for civil defense and fire emergencies.",
            isPinned = true,
            isRecommended = true,
            orderIndex = 1
        ),
        YemenService(
            id = "e2",
            nameAr = "شرطة طوارئ النجدة والأمن العام",
            nameEn = "Emergency Operations Police",
            category = "emergency",
            subCategory = "red_crescent",
            phoneNumber = "199",
            whatsappNumber = "",
            addressAr = "اليمن - جميع المحافظات",
            addressEn = "Yemen - All Governorates",
            rating = 4.8f,
            imageUrl = "https://images.unsplash.com/photo-1513530534585-c7b1394c6d51?w=400",
            descriptionAr = "الأرقام المجانية للنجدة والعمليات الشرطية العاجلة لحفظ الأمن العام.",
            descriptionEn = "Toll-free emergency contact for general safety and emergency police.",
            isPinned = false,
            isRecommended = false,
            orderIndex = 2
        ),
        YemenService(
            id = "e3",
            nameAr = "الإسعاف الطبي العام وسيارات الإنقاذ",
            nameEn = "Public Medical Ambulance",
            category = "emergency",
            subCategory = "red_crescent",
            phoneNumber = "195",
            whatsappNumber = "",
            addressAr = "اليمن - جميع المحافظات",
            addressEn = "Yemen - All Governorates",
            rating = 4.9f,
            imageUrl = "https://images.unsplash.com/photo-1516550893923-42d28e5677af?w=400",
            descriptionAr = "خدمة الإسعاف الطبي الطارئ لنقل المصابين والمرضى للمستشفيات مجاناً.",
            descriptionEn = "Public emergency medical ambulance services for urgent transport.",
            isPinned = true,
            isRecommended = true,
            orderIndex = 3
        ),
        // Medical
        YemenService(
            id = "m1",
            nameAr = "هيئة مستشفى الثورة العام - صنعاء",
            nameEn = "Al-Thawra General Hospital Authority - Sana'a",
            category = "medical",
            subCategory = "clinic",
            phoneNumber = "01246975",
            whatsappNumber = "9671246975",
            addressAr = "بابل اليمن، صنعاء",
            addressEn = "Bab Al-Yemen, Sana'a",
            rating = 4.7f,
            imageUrl = "https://images.unsplash.com/photo-1586015555751-63bb77f4322a?w=400",
            descriptionAr = "من أكبر المستشفيات الحكومية المرجعية في اليمن ويقدم خدمات الطوارئ على مدار الساعة.",
            descriptionEn = "One of the largest referral public hospitals in Yemen, operating emergency center 24/7.",
            isPinned = true,
            isRecommended = true,
            orderIndex = 1
        ),
        YemenService(
            id = "m2",
            nameAr = "مستشفى الجمهورية التعليمي - عدن",
            nameEn = "Al-Goumhouria Teaching Hospital - Aden",
            category = "medical",
            subCategory = "clinic",
            phoneNumber = "02238125",
            whatsappNumber = "",
            addressAr = "خور مكسر، عدن",
            addressEn = "Khor Maksar, Aden",
            rating = 4.5f,
            imageUrl = "https://images.unsplash.com/photo-1516549655169-df83a0774514?w=400",
            descriptionAr = "المستشفى التعليمي الحكومي الأقدم في عدن لتقديم الرعاية الطبية المتكاملة وطوارئ الإسعاف.",
            descriptionEn = "The top historical public teaching hospital in Aden providing full medical care and trauma help.",
            isPinned = false,
            isRecommended = true,
            orderIndex = 2
        ),
        // Finance
        YemenService(
            id = "f1",
            nameAr = "بنك الكريمي للتمويل الأصغر الإسلامي",
            nameEn = "Kuraimi Islamic Microfinance Bank",
            category = "finance",
            subCategory = "banks",
            phoneNumber = "01434444",
            whatsappNumber = "967776000300",
            addressAr = "شارع الجزائر، صنعاء - وفروع في جميع المحافظات",
            addressEn = "Algeria Street, Sana'a - With branches across all governorates",
            rating = 4.9f,
            imageUrl = "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=400",
            descriptionAr = "الرائد في إرسال واستلام الحوالات المالية وفتح الحسابات المصرفية عبر خدمة الكريمي جوّال.",
            descriptionEn = "The leading Islamic bank in Yemen for remittance transfers and microfinance services.",
            isPinned = true,
            isRecommended = true,
            orderIndex = 1
        ),
        YemenService(
            id = "f2",
            nameAr = "بنك اليمن الدولي والمصرفية المعتمدة",
            nameEn = "International Bank of Yemen (IBY)",
            category = "finance",
            subCategory = "banks",
            phoneNumber = "01407000",
            whatsappNumber = "",
            addressAr = "شارع الزبيري، صنعاء",
            addressEn = "Zubairy Street, Sana'a",
            rating = 4.5f,
            imageUrl = "https://images.unsplash.com/photo-1601597111158-2fceff270190?w=400",
            descriptionAr = "يقدم الخدمات المصرفية الشاملة للأفراد والشركات والعمليات الدولية والمحلية بالعديد من العملات.",
            descriptionEn = "Provides comprehensive corporate and retail banking, international transfers, and investment help.",
            isPinned = false,
            isRecommended = false,
            orderIndex = 2
        ),
        // Transport
        YemenService(
            id = "t1",
            nameAr = "الخطوط الجوية اليمنية (المبيعات المركزية)",
            nameEn = "Yemenia Airways",
            category = "transport",
            subCategory = "flights",
            phoneNumber = "01250250",
            whatsappNumber = "",
            addressAr = "شارع الحصبة، صنعاء - وفروع مختلفة بالمحافظات والمطارات",
            addressEn = "Hasaba Street, Sana'a - With offices in airports and main cities",
            rating = 4.3f,
            imageUrl = "https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=400",
            descriptionAr = "الناقل الوطني الجوي للجمهورية اليمنية، لحجز وإدارة رحلات الطيران الدولية.",
            descriptionEn = "The official national airline of Yemen, dealing with international commercial routes.",
            isPinned = true,
            isRecommended = true,
            orderIndex = 1
        ),
        YemenService(
            id = "t2",
            nameAr = "شركة البراق للنقل البري الدولي المشترك",
            nameEn = "Al-Buraq International Land Transport",
            category = "transport",
            subCategory = "land_coaches",
            phoneNumber = "01262262",
            whatsappNumber = "967775262262",
            addressAr = "جولة الجمنة، صنعاء",
            addressEn = "Al-Jumnah Roundabout, Sana'a",
            rating = 4.4f,
            imageUrl = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=400",
            descriptionAr = "خدمات النقل البري الجماعي المتميز بين المدن اليمنية وإلى المملكة العربية السعودية.",
            descriptionEn = "Premium land transport coaches traveling between Yemeni cities and to Saudi Arabia.",
            isPinned = false,
            isRecommended = false,
            orderIndex = 2
        ),
        // Government
        YemenService(
            id = "g1",
            nameAr = "شكاوي وزارة المياه والمياه والصرف الصحي",
            nameEn = "Water and Sanitation Complaints Office",
            category = "government",
            subCategory = "utilities",
            phoneNumber = "171",
            whatsappNumber = "",
            addressAr = "اليمن - الفروع المحلية للمؤسسين",
            addressEn = "Yemen - Local Offices",
            rating = 4.0f,
            imageUrl = "https://images.unsplash.com/photo-1542013936693-8848e5742383?w=400",
            descriptionAr = "الرقم الساخن للتبليغ عن انقطاع المياه بالشبكات الحكومية العامة أو الأعطال الكبرى.",
            descriptionEn = "Primary hotline to notify about local water shortages, grid complaints, or sewage spills.",
            isPinned = true,
            isRecommended = false,
            orderIndex = 1
        ),
        YemenService(
            id = "g2",
            nameAr = "طوارئ أعطال الكهرباء العامة والشبكة القومية",
            nameEn = "General Electricity Emergency Centre",
            category = "government",
            subCategory = "utilities",
            phoneNumber = "177",
            whatsappNumber = "",
            addressAr = "اليمن",
            addressEn = "Yemen",
            rating = 4.1f,
            imageUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=400",
            descriptionAr = "الرقم الموحد للتبليغ عن مشاكل الكهرباء العامة، انقطاع الكابلات أو محولات التغذية.",
            descriptionEn = "National hotline dedicated to reporting power outages, transformer sparks, or general concerns.",
            isPinned = false,
            isRecommended = false,
            orderIndex = 2
        )
    )

    fun getCategories(): List<ServiceCategory> = defaultCategories
    fun getSubCategories(): List<ServiceSubCategory> = defaultSubCategories
    fun getDefaultServices(): List<YemenService> = defaultServices

    // Real-time Firestore snapshot listener for categories
    fun listenToCategoriesFlow(): Flow<List<ServiceCategory>> = callbackFlow {
        val listenerRegistration = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        // Seed default categories
                        for (cat in defaultCategories) {
                            firestore.collection("categories").document(cat.id).set(cat)
                        }
                    } else {
                        val categoriesList = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ServiceCategory::class.java)?.copy(id = doc.id)
                        }
                        trySend(categoriesList)
                    }
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun saveCategory(category: ServiceCategory) {
        val docId = if (category.id.isBlank()) firestore.collection("categories").document().id else category.id
        val finalCat = category.copy(id = docId)
        firestore.collection("categories").document(docId).set(finalCat)
    }

    fun deleteCategory(id: String) {
        firestore.collection("categories").document(id).delete()
    }

    // Real-time subcategories listener
    fun listenToSubCategoriesFlow(): Flow<List<ServiceSubCategory>> = callbackFlow {
        val listenerRegistration = firestore.collection("subcategories")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        for (sub in defaultSubCategories) {
                            firestore.collection("subcategories").document(sub.id).set(sub)
                        }
                    } else {
                        val subList = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ServiceSubCategory::class.java)?.copy(id = doc.id)
                        }
                        trySend(subList)
                    }
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun saveSubCategory(sub: ServiceSubCategory) {
        val docId = if (sub.id.isBlank()) firestore.collection("subcategories").document().id else sub.id
        val finalSub = sub.copy(id = docId)
        firestore.collection("subcategories").document(docId).set(finalSub)
    }

    fun deleteSubCategory(id: String) {
        firestore.collection("subcategories").document(id).delete()
    }

    // Real-time welcome config listener
    fun listenToWelcomeConfigFlow(): Flow<WelcomeConfig> = callbackFlow {
        val listenerRegistration = firestore.collection("config").document("welcome")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    if (snapshot.exists()) {
                        val config = snapshot.toObject(WelcomeConfig::class.java)
                        if (config != null) {
                            trySend(config)
                        } else {
                            trySend(WelcomeConfig())
                        }
                    } else {
                        // Seed config
                        val defaultConfig = WelcomeConfig()
                        firestore.collection("config").document("welcome").set(defaultConfig)
                        trySend(defaultConfig)
                    }
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun saveWelcomeConfig(config: WelcomeConfig) {
        firestore.collection("config").document("welcome").set(config)
    }

    // Real-time service comments/reviews listener
    fun listenToCommentsFlow(serviceId: String): Flow<List<ServiceComment>> = callbackFlow {
        val listenerRegistration = firestore.collection("comments")
            .whereEqualTo("serviceId", serviceId)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val commentsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ServiceComment::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(commentsList)
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun saveComment(comment: ServiceComment) {
        val docId = if (comment.id.isBlank()) firestore.collection("comments").document().id else comment.id
        val finalComment = comment.copy(id = docId)
        firestore.collection("comments").document(docId).set(finalComment)
    }

    fun deleteComment(id: String) {
        firestore.collection("comments").document(id).delete()
    }

    // Real-time Join applications listing
    fun listenToJoinApplicationsFlow(): Flow<List<JoinApplication>> = callbackFlow {
        val listenerRegistration = firestore.collection("join_applications")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(JoinApplication::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    trySend(list)
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    fun saveJoinApplication(app: JoinApplication) {
        val docId = if (app.id.isBlank()) firestore.collection("join_applications").document().id else app.id
        val finalApp = app.copy(id = docId)
        firestore.collection("join_applications").document(docId).set(finalApp)
    }

    fun deleteJoinApplication(id: String) {
        firestore.collection("join_applications").document(id).delete()
    }

    // Real-time Firestore snapshot listener for services
    fun listenToServicesFlow(): Flow<List<YemenService>> = callbackFlow {
        val listenerRegistration = firestore.collection("services")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        // Seed database with defaults if empty
                        initializeDefaultServicesInFirestore()
                    } else {
                        val servicesList = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(YemenService::class.java)?.copy(id = doc.id)
                        }
                        trySend(servicesList)
                    }
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    private fun initializeDefaultServicesInFirestore() {
        for (service in defaultServices) {
            firestore.collection("services").document(service.id).set(service)
        }
    }

    fun saveService(service: YemenService) {
        val docId = if (service.id.isBlank()) firestore.collection("services").document().id else service.id
        val finalService = service.copy(id = docId)
        firestore.collection("services").document(docId).set(finalService)
    }

    fun deleteService(id: String) {
        firestore.collection("services").document(id).delete()
    }

    fun getFavorites(): Set<String> {
        return sharedPrefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(id: String) {
        val current = getFavorites().toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        sharedPrefs.edit().putStringSet("favorites", current).apply()
    }

    fun resetToDefaults() {
        sharedPrefs.edit()
            .putStringSet("favorites", emptySet())
            .apply()

        // Clean and reset configs
        val defaultConfig = WelcomeConfig()
        firestore.collection("config").document("welcome").set(defaultConfig)

        // Clear join applications
        firestore.collection("join_applications").get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
        }

        // Clear comments
        firestore.collection("comments").get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
        }

        // Wipe subcategories and reseed
        firestore.collection("subcategories").get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
            for (sub in defaultSubCategories) {
                firestore.collection("subcategories").document(sub.id).set(sub)
            }
        }

        // Wipe categories and reseed
        firestore.collection("categories").get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
            for (cat in defaultCategories) {
                firestore.collection("categories").document(cat.id).set(cat)
            }
        }

        // Wipe firestore collection and re-seed services
        firestore.collection("services").get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
            initializeDefaultServicesInFirestore()
        }
    }
}
