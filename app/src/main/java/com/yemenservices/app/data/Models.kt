package com.yemenservices.app.data

data class Category(
    val id: String = "",
    val name_ar: String = "",
    val name_en: String = "",
    val icon: String = "build",
    val order_index: Int = 0,
    val image_url: String? = null
)

data class ServiceProvider(
    val id: String = "",
    val category_id: String = "",
    val name_ar: String = "",
    val name_en: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val sms: String = "",
    val image_url: String? = null,
    val is_pinned: Boolean = false,
    val is_approved: Boolean = true,
    val price_range: String = "medium", // "low", "medium", "high"
    val distance: String = "medium",    // "close", "medium", "far"
    val latitude: Double = 15.3694,    // Standard Sana'a coords as default
    val longitude: Double = 44.1910
)

data class Review(
    val id: String = "",
    val provider_id: String = "",
    val user_name: String = "",
    val comment: String = "",
    val rating: Double = 5.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class AppConfig(
    val id: String = "singleton",
    val welcome_msg_ar: String = "مرحباً بكم في تطبيق دليلي للخدمات والمهن في اليمن!",
    val welcome_msg_en: String = "Welcome to Dalili, the top Service Directory in Yemen!",
    val welcome_msg_mode: String = "ai", // "ai" or "custom"
    val welcome_developer: String = "Maher ahmed",
    val footer_phone: String = "777644670",
    val support_email: String = "sub@dalili-yemen.com",
    val support_whatsapp: String = "777644670",
    val apk_download_url: String = "https://dalili-yemen.com/download/dalili.apk",
    val newest_apk_version: String = "2.0",
    val show_maps_enabled: Boolean = true,
    val show_reviews_enabled: Boolean = true,
    val invite_codes_enabled: Boolean = true,
    val list_sort_mode: String = "name", // "name" (alphabetical), "date" (publish order/id)
    val primary_color_hex: String = "#1B5E20",
    val secondary_color_hex: String = "#FFC107"
)

data class Supervisor(
    val id: String = "",
    val username: String = "",
    val password: String = "",
    val role: String = "moderator" // "admin" or "moderator"
)
