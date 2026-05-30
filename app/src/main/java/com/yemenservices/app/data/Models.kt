package com.yemenservices.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name_ar: String = "",
    val name_en: String = "",
    val icon: String = "",
    val image_url: String? = null,
    val parent_id: String? = null
)

@Serializable
data class ServiceProvider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val description_ar: String = "",
    val description_en: String = "",
    val category_id: String = "",
    val rating: Float = 5.0f,
    val price_level: String = "Average", // Low, Average, High
    val distance: String = "1", // in km
    val address_ar: String = "صنعاء",
    val address_en: String = "Sanaa",
    val is_approved: Boolean = false,
    val profileImage: String? = null, // Custom personal photo link input by professional upon registration
    val rating_count: Int = 1
)

@Serializable
data class Review(
    val id: String = "",
    val provider_id: String = "",
    val reviewer_name: String = "مستخدم",
    val rating: Float = 5.0f,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AppConfig(
    val app_name: String = "دليل الخدمات",
    val welcomeMessage: String = "مرحباً بكم في التطبيق الذي يجمع كل المهن والخدمات بين يديك، قريبين منك، ويسهل عليك التواصل معهم.",
    val welcomeTextSize: Int = 14,
    val welcomeImageUrl: String? = null,
    val welcomeType: String = "text", // "text", "image", "both"
    val primary_color_hex: String = "#2E7D32",
    val secondary_color_hex: String = "#81C784",
    val support_email: String = "support@yemenservices.app",
    val support_whatsapp: String = "+96777777777",
    val footer_phone: String = "777777777",
    val selected_icon_type: String = "tools"
)
