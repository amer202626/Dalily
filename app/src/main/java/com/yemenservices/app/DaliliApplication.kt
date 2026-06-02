package com.yemenservices.app

import android.app.Application
import androidx.room.Room
import com.yemenservices.app.data.AppDatabase
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.Repository
import com.yemenservices.app.data.ServiceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class DaliliApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Ensure Firebase is initialized
        FirebaseApp.initializeApp(this)
        
        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "dalili_services_database"
        ).build()

        repository = Repository(database)

        // Seed initial data asynchronously if the database is empty
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        // Checking if local room settings are empty, and seed them
        val settings = database.appSettingDao().getAllSettings().first()
        if (settings.isEmpty()) {
            seedLocalSettings()
        }
        
        // Check and seed Firestore collections
        seedFirestoreIfNeeded()
    }

    private suspend fun seedLocalSettings() {
        val defaultSettings = mapOf(
            "app_title_ar" to "دليل الخدمات اليمني",
            "app_title_en" to "Yemen Service Directory",
            "app_desc_ar" to "دليلك الشامل والشخصي لجميع الخدمات الطبية، التعليمية، والمهنية في مختلف المحافظات اليمنية.",
            "app_desc_en" to "Your comprehensive and personal directory for medical, educational, and professional services across Yemeni governorates.",
            "contact_phone" to "+96777777777",
            "contact_email" to "support@yemenservices.app",
            "app_rules_ar" to "1. يمكن لجميع المستخدمين تصفح الخدمات والبحث عنها والاتصال بمقدمي الخدمات.\n2. يمكن للمستخدم إرسال طلب إضافة مقدم خدمة جديد مجاناً.\n3. يتم التحقق من مقدمي الخدمات من قبل الإدارة لضمان جودة ومصداقية الدليل.",
            "app_rules_en" to "1. All users can browse, search services, and call service providers directly.\n2. Users can submit requests to add new service providers for free.\n3. Service providers are verified by administration to ensure guide quality and credibility.",
            
            // Labels for custom fields that admin can change
            "custom_field_1_label_ar" to "سنة التأسيس / الخبرة",
            "custom_field_1_label_en" to "Experience / Est. Year",
            "custom_field_2_label_ar" to "طريقة الدفع المقبولة",
            "custom_field_2_label_en" to "Accepted Payment Method",
            "custom_field_3_label_ar" to "ملاحظات إضافية",
            "custom_field_3_label_en" to "Additional Notes"
        )
        
        for ((key, value) in defaultSettings) {
            database.appSettingDao().insertSetting(com.yemenservices.app.data.AppSetting(key, value))
        }
    }

    private suspend fun seedFirestoreIfNeeded() {
        val firestore = FirebaseFirestore.getInstance()
        
        try {
            // Seed categories if empty
            val categoriesRef = firestore.collection("categories")
            val catSnapshot = categoriesRef.limit(1).get().await()
            if (catSnapshot.isEmpty) {
                val categories = listOf(
                    Category(id = 1, nameAr = "طوارئ وإسعاف", nameEn = "Emergency & Ambulance", iconName = "medical_services"),
                    Category(id = 2, nameAr = "مستشفيات وعيادات", nameEn = "Hospitals & Clinics", iconName = "local_hospital"),
                    Category(id = 3, nameAr = "كهرباء وسباكة", nameEn = "Electrical & Plumbing", iconName = "build"),
                    Category(id = 4, nameAr = "تعليم ومدارس", nameEn = "Education & Schools", iconName = "school"),
                    Category(id = 5, nameAr = "صيانة سيارات", nameEn = "Car Maintenance", iconName = "directions_car")
                )
                for (category in categories) {
                    categoriesRef.document(category.id.toString()).set(category).await()
                }
            }

            // Seed providers if empty
            val providersRef = firestore.collection("service_providers")
            val provSnapshot = providersRef.limit(1).get().await()
            if (provSnapshot.isEmpty) {
                val providers = listOf(
                    ServiceProvider(
                        id = 1,
                        categoryId = 1,
                        nameAr = "مركز الإسعاف والطوارئ بصنعاء",
                        nameEn = "Sana'a Ambulance & Emergency Center",
                        phone = "191",
                        addressAr = "صنعاء - باب اليمن",
                        addressEn = "Sana'a - Bab Al-Yaman",
                        descriptionAr = "المركز الرئيسي للخدمات الطارئة والإسعاف لخدمة جميع المواطنين على مدار 24 ساعة.",
                        descriptionEn = "Principal center for emergency services and ambulances serving citizens 24/7.",
                        workingHours = "24/7",
                        isVerified = true,
                        customField1Value = "1994",
                        customField2Value = "مجاني (Free)",
                        customField3Value = "رقم الطوارئ السريع والوطني في أمانة العاصمة"
                    ),
                    ServiceProvider(
                        id = 2,
                        categoryId = 2,
                        nameAr = "مستشفى الثورة العام بصنعاء",
                        nameEn = "Al-Thawra General Hospital Sana'a",
                        phone = "+9671250225",
                        addressAr = "صنعاء - شارع تعز (قرب باب اليمن)",
                        addressEn = "Sana'a - Taiz Street (Near Bab Al-Yaman)",
                        descriptionAr = "أكبر مستشفى عام وأكاديمي في اليمن، يحتوي على كافة التخصصات الطبية ومركز الطوارئ المتكامل.",
                        descriptionEn = "The largest general and academic hospital in Yemen, featuring all medical specialties and a comprehensive emergency center.",
                        workingHours = "24/7",
                        isVerified = true,
                        customField1Value = "1968",
                        customField2Value = "نقدي / تأمين صحي",
                        customField3Value = "يقدم خدمات الرعاية العامة والعيادات التخصصية"
                    ),
                    ServiceProvider(
                        id = 3,
                        categoryId = 2,
                        nameAr = "مستشفى مأرب العام",
                        nameEn = "Marib General Hospital",
                        phone = "+9676302220",
                        addressAr = "مأرب - الشارع العام",
                        addressEn = "Marib - Main Street",
                        descriptionAr = "مستشفى حكومي عام يقدم خدمات الطوارئ، الجراحة، والأمومة والطفولة في محافظة مأرب.",
                        descriptionEn = "Official general hospital offering emergency, surgeries, and maternity services in Marib Governorate.",
                        workingHours = "24/7",
                        isVerified = true,
                        customField1Value = "15+ سنة خبرة",
                        customField2Value = "نقدي كاش",
                        customField3Value = "يحتوي على أفضل كادر للأمراض الباطنية والجراحة العامة"
                    ),
                    ServiceProvider(
                        id = 4,
                        categoryId = 3,
                        nameAr = "مؤسسة يمن تك للتمديدات والشبكات الكهربائية",
                        nameEn = "Yemen-Tech for Electrical Extensions & Networks",
                        phone = "+96771234567",
                        addressAr = "عدن - الشيخ عثمان",
                        addressEn = "Aden - Sheikh Othman",
                        descriptionAr = "فنيون متطوعون ومهندسون مؤهلون لصيانة وإصلاح التمديدات المنزلية وأنظمة الطاقة الشمسية بجودة عالية.",
                        descriptionEn = "Qualified technicians and engineers for maintaining home extensions and solar energy systems with premium quality.",
                        workingHours = "8:00 AM - 9:00 PM",
                        isVerified = true,
                        customField1Value = "12 سنة",
                        customField2Value = "نقدي / حوالة موبايل (Kuraimi)",
                        customField3Value = "تخصص متميز في أنظمة الطاقة الشمسية والبطاريات"
                    ),
                    ServiceProvider(
                        id = 5,
                        categoryId = 4,
                        nameAr = "ثانوية جمال عبد الناصر للمتفوقين",
                        nameEn = "Gamal Abdel Nasser High School for Gifted Students",
                        phone = "+9671274112",
                        addressAr = "صنعاء - التحرير",
                        addressEn = "Sana'a - Al-Tahrir",
                        descriptionAr = "أعرق ثانوية حكومية للمتفوقين في اليمن، تدرس مناهج علمية متطورة ومجهزة بأحدث المختبرات الشاملة.",
                        descriptionEn = "The most prestigious public high school for gifted students in Yemen, teaching advanced scientific curricula and equipped with laboratories.",
                        workingHours = "8:00 AM - 1:30 PM",
                        isVerified = true,
                        customField1Value = "تأسست 1962",
                        customField2Value = "مجاني (شروط قبول خاصة)",
                        customField3Value = "القبول يعتمد على اختبار ذكاء واختبار تحصيلي سنوي"
                    ),
                    ServiceProvider(
                        id = 6,
                        categoryId = 5,
                        nameAr = "مركز حضرموت لصيانة وفحص السيارات بالكمبيوتر",
                        nameEn = "Hadramout Center for Car Maintenance & Computer Diagnosis",
                        phone = "+967770987654",
                        addressAr = "المكلا - روكب",
                        addressEn = "Mukalla - Rokob",
                        descriptionAr = "فنيون مؤهلون لفحص متكامل لجميع أنواع السيارات بالكمبيوتر وصيانة المحركات وبرمجة المنظومات الحديثة.",
                        descriptionEn = "Full computer diagnosis for all automotive brands, engine overhaul, and software configuration for modern vehicles.",
                        workingHours = "8:00 AM - 12:00 PM | 4:00 PM - 8:30 PM",
                        isVerified = false,
                        customField1Value = "8 سنوات",
                        customField2Value = "نقدي كاش",
                        customField3Value = "يتوفر فحص قبل الشراء"
                    )
                )
                for (provider in providers) {
                    providersRef.document(provider.id.toString()).set(provider).await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
