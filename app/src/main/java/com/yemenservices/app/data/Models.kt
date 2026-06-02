package com.yemenservices.app.data

data class Category(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val icon: String = "", // Can be custom emoji or image url
    val orderIndex: Int = 0
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", 0)
}

data class SubCategory(
    val id: String = "",
    val categoryId: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val orderIndex: Int = 0
) {
    constructor() : this("", "", "", "", 0)
}

data class YemenService(
    val id: String = "",
    val category: String = "",          // Category ID
    val subCategory: String = "",       // Subcategory ID
    val nameAr: String = "",
    val nameEn: String = "",
    val phoneNumber: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val addressAr: String = "",
    val addressEn: String = "",
    val imageUrl: String = "",
    val rating: Float = 5.0f,
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false,
    val orderIndex: Int = 0,
    val workPlace: String = "",         // مكان العمل
    val residencePlace: String = "",    // مكان السكن
    val idCardImageUrl: String = ""     // صورة الهوية الشخصية (اختياري)
) {
    constructor() : this("", "", "", "", "", "", "", "", "", "", "", 5.0f, false, false, 0, "", "", "")
}

data class ProviderJoinRequest(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val phone: String = "",
    val category: String = "",
    val subCategory: String = "",
    val imageUrl: String = "",
    val address: String = "",
    val latitude: Double = 15.3694,    // Default coordinates in Sana'a
    val longitude: Double = 44.1910,
    val workPlace: String = "",         // مكان العمل
    val residencePlace: String = "",    // السكن
    val idCardImageUrl: String = "",     // صورة الهوية الشخصية (Optionally uploaded/linked)
    val status: String = "PENDING",     // PENDING, APPROVED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", "", "", "", 15.3694, 44.1910, "", "", "", "PENDING", System.currentTimeMillis())
}

data class Comment(
    val id: String = "",
    val serviceId: String = "",
    val userName: String = "",
    val comment: String = "",
    val rating: Float = 5.0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", 5.0f, System.currentTimeMillis())
}

data class AppConfig(
    val globalTheme: String = "cosmic_slate", // cosmic_slate, charcoal_gold, royal_emerald, red_black, slate_silver, ocean_teal
    val footerContactPhone: String = "+967777777777",
    val showAiAssistant: Boolean = true,
    val aiAssistantSizeDp: Int = 40,
    val aiAssistantIcon: String = "💬", // Can be emoji or URL
    val aiAssistantPosition: String = "FOOTER", // FOOTER, FLOATING
    val aiAssistantYOffset: Int = 0,
    val showFooterInfo: Boolean = true,
    val showFooterPhone: Boolean = true,
    val showFooterAi: Boolean = true,
    val footerOrder: String = "INFO,AI,PHONE" // Comma separated items to re-order the elements dynamically!
) {
    constructor() : this("cosmic_slate", "+967777777777", true, 40, "💬", "FOOTER", 0, true, true, true, "INFO,AI,PHONE")
}

data class SupervisorAccount(
    val id: String = "",
    val username: String = "",
    val passwordPlain: String = "",
    val role: String = "SUPERVISOR", // SUPERVISOR, ADMIN
    val isEnabled: Boolean = true
) {
    constructor() : this("", "", "", "SUPERVISOR", true)
}
