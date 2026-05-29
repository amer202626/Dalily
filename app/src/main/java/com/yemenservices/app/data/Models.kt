package com.yemenservices.app.data

data class Category(
    var id: String = "",
    var name_ar: String = "",
    var name_en: String = "",
    var icon: String = "",
    var order_index: Int = 0,
    var is_active: Boolean = true,
    var image_url: String? = null
)

data class ServiceProvider(
    var id: String = "",
    var name_ar: String = "",
    var name_en: String = "",
    var phone: String = "",
    var category_id: String = "",
    var rating: Float = 5.0f,
    var is_active: Boolean = true,
    var image_url: String? = null,
    var is_pinned: Boolean = false
)

data class Admin(
    var username: String = "",
    var password_hash: String = "",
    var role: String = "",
    var is_active: Boolean = true
)

data class Review(
    var id: String = "",
    var provider_id: String = "",
    var user_name: String = "",
    var comment: String = "",
    var rating: Float = 5.0f,
    var created_at: String = ""
)

data class AppConfig(
    var id: String = "settings",
    var footer_phone: String = "777644670",
    var app_theme: String = "gold", // "gold", "emerald", "ocean", "purple", "sunset", "classic"
    var welcome_msg_mode: String = "ai", // "ai", "custom"
    var custom_welcome_msg: String = "مرحباً بكم في دليلي للخدمات في اليمن فورياً",
    var owner_name: String = "Maher ahmed",
    var owner_phone: String = "777644670",
    var show_reviews_enabled: Boolean = true,
    var list_sort_mode: String = "date", // "custom", "name", "date"
    var app_logo_icon: String = "work", // "work", "star", "phone", "info"
    var app_name: String = "دليلي للخدمات",
    var app_logo_text: String = "خد"
)

