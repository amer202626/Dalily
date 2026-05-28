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
    var image_url: String? = null
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
