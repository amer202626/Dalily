package com.yemenservices.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name_ar: String = "",
    val name_en: String = "",
    val icon: String = "",
    val image_url: String? = null
)

@Serializable
data class ServiceProvider(
    val id: String = "",
    val category_id: String = "",
    val name_ar: String = "",
    val name_en: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val region_ar: String = "",
    val region_en: String = "",
    val price_range: String = "medium", // "low", "medium", "high"
    val distance: String = "medium", // "close", "medium", "far"
    val is_pinned: Boolean = false,
    val is_approved: Boolean = true,
    val image_url: String? = null
)

@Serializable
data class Review(
    val id: String = "",
    val provider_id: String = "",
    val reviewer_name: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class Supervisor(
    val id: String = "",
    val username: String = "",
    val password: String = "",
    val is_super_admin: Boolean = false // If true, can update administrators and pending requests
)

@Serializable
data class AppConfig(
    val app_name: String = "دليل الخدمات",
    val primary_color_hex: String = "#1B5E20",
    val secondary_color_hex: String = "#FFC107",
    val bg_color_hex: String = "#F5F5F5",
    val text_main_hex: String = "#212121",
    val welcomeMessage: String = "مرحباً بكم في التطبيق الوحيد في اليمن الذي يجمع كل المهن والخدمات بين يديك، قريبين منك، ويسهل عليك التواصل معهم.",
    val footer_text: String = "MAW 777644670",
    val footer_phone: String = "777644670",
    val support_whatsapp: String = "967777644670",
    val support_email: String = "support@dalili.ye",
    val newest_apk_version: String = "1.0",
    val apk_download_url: String = "https://example.com/dalili.apk",
    val list_sort_mode: String = "name", // "name" or "id"
    val selected_icon_type: String = "tools"
)

@Serializable
data class PendingProvider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val category_id: String = "",
    val region: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
