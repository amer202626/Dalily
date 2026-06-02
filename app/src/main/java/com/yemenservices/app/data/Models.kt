package com.yemenservices.app.data

import kotlinx.serialization.Serializable

@Serializable
data class YemenService(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val category: String = "", // parent category id (e.g., "emergency", "medical")
    val subCategory: String = "", // sub-category id (e.g., "dentist", "pharmacy")
    val phoneNumber: String = "",
    val whatsappNumber: String = "",
    val addressAr: String = "",
    val addressEn: String = "",
    val rating: Float = 4.5f,
    val imageUrl: String = "", // profile or avatar photo URL
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false, // recommended status
    val orderIndex: Int = 0,
    val latLngString: String = ""
)

@Serializable
data class ServiceCategory(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val iconName: String = "", // stores Emoji icon
    val isPinned: Boolean = false,
    val orderIndex: Int = 0
)

@Serializable
data class ServiceSubCategory(
    val id: String = "",
    val parentId: String = "", // Parent category ID
    val nameAr: String = "",
    val nameEn: String = "",
    val iconEmoji: String = "", // subcategory emoji icon
    val orderIndex: Int = 0
)

@Serializable
data class ServiceComment(
    val id: String = "",
    val serviceId: String = "",
    val authorName: String = "",
    val commentText: String = "",
    val rating: Float = 5f,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class WelcomeConfig(
    val id: String = "welcome",
    val titleAr: String = "مرحباً بك في دليلك المحلي",
    val titleEn: String = "Welcome to your local guide",
    val bodyAr: String = "تصفح وابحث عن أرقام الطوارئ، المستشفيات، البنوك، النقل والخدمات في اليمن بشكل كامل ومباشر مع ميزة المزامنة السحابية الفورية واللحظية لجميع البيانات.",
    val bodyEn: String = "Browse and find emergency contacts, medical facilities, banks, travel resources and services in Yemen, instantly synchronized in real-time.",
    val imageUrl: String = "",
    val globalTheme: String = "cosmic_slate", // cosmic_slate, charcoal_gold, royal_emerald, red_black, royal_indigo, emerald_green, slate_silver, ocean_teal, beige_cream
    val fontColor: String = "bright_white", // bright_white, light_gold, vibrant_silver
    val footerText: String = "MAW 777644670",
    val showFooter: Boolean = true,
    val assistantBtnSize: Int = 48,
    val assistantBtnColor: String = "#10B981",
    val assistantBtnPosition: String = "bottom_right", // bottom_right, bottom_left
    val supportPhone: String = "736462000",
    val supportWhatsapp: String = "967736462000",
    val supportEmail: String = "support@daliliyemen.com",
    val assistantGreetingAr: String = "مرحباً! أنا مساعدك الذكي لجميع الخدمات وأصحاب المهن في اليمن. سأساعدك في العثور على أرقام الطوارئ، الأقسام، المستشفيات، والأشخاص المهنيين.",
    val assistantGreetingEn: String = "Welcome! I am your Smart Assistant for all services in Yemen. I will help you look up categories, services, hospitals, doctors and professional names.",
    val bannerExtUrl: String = "", // URL link for external navigation
    // Configurable toolbar icons (visibility and text underneath)
    val showRefreshIcon: Boolean = true,
    val showLangIcon: Boolean = true,
    val showThemeIcon: Boolean = true,
    val showAdminIcon: Boolean = true,
    val showJoinIcon: Boolean = true,
    val showHomeIcon: Boolean = true,
    val refreshIconTextAr: String = "تحديث",
    val refreshIconTextEn: String = "Refresh",
    val langIconTextAr: String = "اللغة",
    val langIconTextEn: String = "Language",
    val themeIconTextAr: String = "المظهر",
    val themeIconTextEn: String = "Theme",
    val adminIconTextAr: String = "المشرفون",
    val adminIconTextEn: String = "Admins",
    val joinIconTextAr: String = "التسجيل",
    val joinIconTextEn: String = "Register",
    val homeIconTextAr: String = "الرئيسية",
    val homeIconTextEn: String = "Home",
    // Configurable sub-admins list
    val supervisorsJson: String = "[]"
)

@Serializable
data class JoinApplication(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val region: String = "", // governorate / region in Yemen
    val categoryId: String = "",
    val subCategoryId: String = "",
    val logoUrl: String = "",
    val status: String = "pending", // pending, approved, rejected
    val timestamp: Long = System.currentTimeMillis(),
    val workspaceAddress: String = "",
    val idCardUrl: String = "",
    val latLngString: String = ""
)

@Serializable
data class SupervisorAccount(
    val username: String = "",
    val password: String = "",
    val canAddProvider: Boolean = true,
    val canManageJoins: Boolean = true
)
