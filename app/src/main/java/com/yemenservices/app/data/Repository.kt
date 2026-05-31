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
        ServiceCategory("emergency", "الطوارئ", "Emergency", "emergency"),
        ServiceCategory("medical", "المستشفيات والطبية", "Hospitals & Medical", "medical"),
        ServiceCategory("finance", "البنوك والخدمات المالية", "Banks & Finance", "finance"),
        ServiceCategory("transport", "النقل والسفر", "Transport & Travel", "transport"),
        ServiceCategory("government", "الخدمات الحكومية العامة", "Government Services", "government")
    )

    private val defaultServices = listOf(
        // Emergency
        YemenService(
            id = "e1",
            nameAr = "الدفاع المدني (الإطفاء)",
            nameEn = "Civil Defense (Fire Department)",
            category = "emergency",
            phoneNumber = "191",
            whatsappNumber = "967191",
            addressAr = "اليمن - جميع المحافظات",
            addressEn = "Yemen - All Governorates",
            rating = 5.0f,
            descriptionAr = "الاتصال المجاني للدفاع المدني والإطفاء لحالات الطوارئ والحرائق.",
            descriptionEn = "Toll-free emergency number for civil defense and fire emergencies."
        ),
        YemenService(
            id = "e2",
            nameAr = "شرطة طوارئ الأمن العام",
            nameEn = "Emergency Operations Police",
            category = "emergency",
            phoneNumber = "199",
            addressAr = "اليمن - جميع المحافظات",
            addressEn = "Yemen - All Governorates",
            rating = 4.8f,
            descriptionAr = "الأرقام المجانية للنجدة والعمليات الشرطية العاجلة.",
            descriptionEn = "Toll-free emergency contact for general safety and emergency police."
        ),
        YemenService(
            id = "e3",
            nameAr = "الإسعاف الطبي العام",
            nameEn = "Public Medical Ambulance",
            category = "emergency",
            phoneNumber = "195",
            addressAr = "اليمن - جميع المحافظات",
            addressEn = "Yemen - All Governorates",
            rating = 4.9f,
            descriptionAr = "خدمة الإسعاف الطبي الطارئ لنقل المصابين والمرضى.",
            descriptionEn = "Public emergency medical ambulance services."
        ),
        // Medical
        YemenService(
            id = "m1",
            nameAr = "هيئة مستشفى الثورة العام - صنعاء",
            nameEn = "Al-Thawra General Hospital Authority - Sana'a",
            category = "medical",
            phoneNumber = "01246975",
            whatsappNumber = "9671246975",
            addressAr = "بابل اليمن، صنعاء",
            addressEn = "Bab Al-Yemen, Sana'a",
            rating = 4.2f,
            descriptionAr = "من أكبر المستشفيات الحكومية المرجعية في اليمن ويقدم خدمات الطوارئ على مدار الساعة.",
            descriptionEn = "One of the largest referral public hospitals in Yemen, operating emergency center 24/7."
        ),
        YemenService(
            id = "m2",
            nameAr = "مستشفى الجمهورية التعليمي - عدن",
            nameEn = "Al-Goumhouria Teaching Hospital - Aden",
            category = "medical",
            phoneNumber = "02238125",
            addressAr = "خور مكسر، عدن",
            addressEn = "Khor Maksar, Aden",
            rating = 4.1f,
            descriptionAr = "المستشفى التعليمي الحكومي الأقدم في عدن لتقديم الرعاية الطبية المتكاملة وطوارئ الإسعاف.",
            descriptionEn = "The top historical public teaching hospital in Aden providing full medical care and trauma help."
        ),
        // Finance
        YemenService(
            id = "f1",
            nameAr = "بنك الكريمي للتمويل الأصغر الإسلامي",
            nameEn = "Kuraimi Islamic Microfinance Bank",
            category = "finance",
            phoneNumber = "01434444",
            whatsappNumber = "967776000300",
            addressAr = "شارع الجزائر، صنعاء - وفروع في جميع المحافظات",
            addressEn = "Algeria Street, Sana'a - With branches across all governorates",
            rating = 4.7f,
            descriptionAr = "الرائد في إرسال واستلام الحوالات المالية وفتح الحسابات المصرفية عبر خدمة الكريمي جوّال.",
            descriptionEn = "The leading Islamic bank in Yemen for remittance transfers and microfinance services."
        ),
        YemenService(
            id = "f2",
            nameAr = "بنك اليمن الدولي",
            nameEn = "International Bank of Yemen (IBY)",
            category = "finance",
            phoneNumber = "01407000",
            addressAr = "شارع الزبيري، صنعاء",
            addressEn = "Zubairy Street, Sana'a",
            rating = 4.5f,
            descriptionAr = "يقدم الخدمات المصرفية الشاملة للأفراد والشركات والعمليات الدولية والمحلية بالعديد من العملات.",
            descriptionEn = "Provides comprehensive corporate and retail banking, international transfers, and investment help."
        ),
        // Transport
        YemenService(
            id = "t1",
            nameAr = "الخطوط الجوية اليمنية",
            nameEn = "Yemenia Airways",
            category = "transport",
            phoneNumber = "01250250",
            addressAr = "شارع الحصبة، صنعاء - وفروع مختلفة بالمحافظات والمطارات",
            addressEn = "Hasaba Street, Sana'a - With offices in airports and main cities",
            rating = 4.3f,
            descriptionAr = "الناقل الوطني الجوي للجمهورية اليمنية، لحجز وإدارة رحلات الطيران الدولية.",
            descriptionEn = "The official national airline of Yemen, dealing with international commercial routes."
        ),
        YemenService(
            id = "t2",
            nameAr = "شركة البراق للنقل البري الدولي",
            nameEn = "Al-Buraq International Land Transport",
            category = "transport",
            phoneNumber = "01262262",
            whatsappNumber = "967775262262",
            addressAr = "جولة الجمنة، صنعاء",
            addressEn = "Al-Jumnah Roundabout, Sana'a",
            rating = 4.4f,
            descriptionAr = "خدمات النقل البري الجماعي المتميز بين المدن اليمنية وإلى المملكة العربية السعودية.",
            descriptionEn = "Premium land transport coaches traveling between Yemeni cities and to Saudi Arabia."
        ),
        // Government
        YemenService(
            id = "g1",
            nameAr = "شكاوي وزارة المياه والمياه والصرف الصحي",
            nameEn = "Water and Sanitation Complaints Office",
            category = "government",
            phoneNumber = "171",
            addressAr = "اليمن - الفروع المحلية للمؤسسين",
            addressEn = "Yemen - Local Offices",
            rating = 4.0f,
            descriptionAr = "الرقم الساخن للتبليغ عن انقطاع المياه بالشبكات الحكومية العامة أو الأعطال الكبرى.",
            descriptionEn = "Primary hotline to notify about local water shortages, grid complaints, or sewage spills."
        ),
        YemenService(
            id = "g2",
            nameAr = "طوارئ أعطال الكهرباء العامة",
            nameEn = "General Electricity Emergency Centre",
            category = "government",
            phoneNumber = "177",
            addressAr = "اليمن",
            addressEn = "Yemen",
            rating = 4.1f,
            descriptionAr = "الرقم الموحد للتبليغ عن مشاكل الكهرباء العامة، انقطاع الكابلات أو محولات التغذية.",
            descriptionEn = "National hotline dedicated to reporting power outages, transformer sparks, or general concerns."
        )
    )

    fun getCategories(): List<ServiceCategory> = defaultCategories

    // Real-time Firestore snapshot listener for categories
    fun listenToCategoriesFlow(): Flow<List<ServiceCategory>> = callbackFlow {
        val listenerRegistration = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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

    // Real-time welcome config listener
    fun listenToWelcomeConfigFlow(): Flow<WelcomeConfig> = callbackFlow {
        val listenerRegistration = firestore.collection("config").document("welcome")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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

    // Real-time service comments listener
    fun listenToCommentsFlow(serviceId: String): Flow<List<ServiceComment>> = callbackFlow {
        val listenerRegistration = firestore.collection("comments")
            .whereEqualTo("serviceId", serviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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

    // Real-time Firestore snapshot listener
    fun listenToServicesFlow(): Flow<List<YemenService>> = callbackFlow {
        val listenerRegistration = firestore.collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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

        // Clear comments
        firestore.collection("comments").get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
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
