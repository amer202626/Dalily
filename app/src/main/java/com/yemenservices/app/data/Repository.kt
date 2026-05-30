package com.yemenservices.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Repository(context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("dalili_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

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

    init {
        // Load defaults if empty
        if (!sharedPrefs.contains("is_initialized")) {
            sharedPrefs.edit()
                .putString("services", json.encodeToString(defaultServices))
                .putBoolean("is_initialized", true)
                .apply()
        }
    }

    fun getCategories(): List<ServiceCategory> = defaultCategories

    fun getServices(): List<YemenService> {
        val jsonStr = sharedPrefs.getString("services", null) ?: return defaultServices
        return try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            defaultServices
        }
    }

    fun saveService(service: YemenService) {
        val current = getServices().toMutableList()
        val index = current.indexOfFirst { it.id == service.id }
        if (index >= 0) {
            current[index] = service
        } else {
            current.add(service)
        }
        sharedPrefs.edit().putString("services", json.encodeToString(current)).apply()
    }

    fun deleteService(id: String) {
        val current = getServices().filterNot { it.id == id }
        sharedPrefs.edit().putString("services", json.encodeToString(current)).apply()
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
            .putString("services", json.encodeToString(defaultServices))
            .putStringSet("favorites", emptySet())
            .apply()
    }
}
