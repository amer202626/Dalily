package com.dalily.services.data

import kotlinx.serialization.Serializable

@Serializable
data class ServiceProvider(
    val id: String,
    val name: String,
    val category: String,
    val phone: String,
    val whatsapp: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val idCardUrl: String = "",
    val rating: Double = 4.5,
    val reviewsCount: Int = 12,
    val views: Int = 100,
    val isVerified: Boolean = false,
    val tags: List<String> = emptyList(),
    val address: String = "اليمن",
    val residenceRegion: String = "",
    val lat: Double = 15.3694,
    val lng: Double = 44.1910,
    val isAvailable: Boolean = true,
    val isFeatured: Boolean = false,
    val workHours: String = "8:00 AM - 10:00 PM",
    val popularityScore: Int = 0,
    val reviewsList: List<UserReview> = emptyList(),
    val appointments: List<String> = emptyList() // Available times or booked slots
)

@Serializable
data class UserReview(
    val id: String = "",
    val username: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class CustomCategory(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val iconName: String = "Build",
    val colorHex: String = "#3B82F6",
    val isPinned: Boolean = false
)

@Serializable
data class BannerAd(
    val id: String,
    val imageUrl: String,
    val linkUrl: String = "",
    val title: String = "",
    val isActive: Boolean = true,
    val budget: Double = 50.0,
    val durationDays: Int = 7,
    val priorityOrder: Int = 1,
    val creationTime: Long = System.currentTimeMillis()
)

@Serializable
data class AdminProfile(
    val id: String,
    val username: String,
    val email: String = "",
    val passwordHash: String,
    val role: String = "supervisor", // "owner" or "supervisor"
    val is2FAConfigured: Boolean = false,
    val canAddProviders: Boolean = true,
    val canAcceptRequests: Boolean = true,
    val canDeleteProviders: Boolean = true,
    val isActive: Boolean = true
)

@Serializable
data class ServiceRequest(
    val id: String,
    val providerName: String,
    val category: String,
    val phone: String,
    val description: String = "",
    val whatsapp: String = "",
    val profileImageUrl: String = "", // Required photo
    val idCardUrl: String = "", // Optional ID card
    val workAddress: String = "", // Required
    val residenceRegion: String = "", // Required
    val lat: Double = 15.3694,
    val lng: Double = 44.1910,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val submissionDate: Long = System.currentTimeMillis()
)

@Serializable
data class ChatMessage(
    val id: String,
    val providerId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean = true
)

@Serializable
data class Report(
    val id: String,
    val providerId: String,
    val providerName: String,
    val reason: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class FAQItem(
    val id: String,
    val question: String,
    val answer: String
)

@Serializable
data class ActivityLog(
    val id: String = "",
    val supervisorName: String = "",
    val action: String = "",
    val target: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TopBarIconConfig(
    val iconKey: String, // "REFRESH", "LANG", "INFO", "ADMIN", "REGISTER", "BACKDOOR"
    val isVisible: Boolean = true,
    val arabicLabel: String = "",
    val englishLabel: String = ""
)

@Serializable
data class AppConfig(
    val id: String = "global_config",
    val welcomeText: String = "مرحباً بكم في دليل الخدمات المعتمد!",
    val welcomeTextEn: String = "Welcome to the Verified Services Directory!",
    val welcomeFontSize: Int = 16, // small: 12, medium: 16, large: 20
    val welcomeFontColor: String = "Bright White", // Bright White, Light Gold, Vibrant Silver
    val themeColors: String = "Cosmic Slate", // Cosmic Slate, Charcoal Gold, Royal Emerald
    val welcomeImage: String = "", // Simulated brand image
    val footerText: String = "WAM777644670",
    val footerScale: Float = 0.5f, // Size divider (50% small scale)
    val isFooterVisible: Boolean = true,
    val isMaintenanceMode: Boolean = false,
    val aiAssistantPosition: String = "Bottom Right", // Bottom Right, Bottom Left
    val aiAssistantSize: String = "Medium", // Small, Medium, Large
    val aiAssistantColor: String = "#FFD700", // Hex color
    val aiAssistantLabelsEnabled: Boolean = true,
    val twoFactorAuthEnabled: Boolean = false,
    val lowCategoryAlertThreshold: Int = 2,
    val appInfoEmail: String = "support@dalily.com",
    val appInfoPhone: String = "+967 777 644 670",
    val appInfoCheckUpdateUrl: String = "https://example.com/check_updates",
    val appInfoShareUrl: String = "https://example.com/download_dalily",
    val voiceNotesEnabled: Boolean = true,
    val dataSavingMode: Boolean = false,
    val topBarIcons: List<TopBarIconConfig> = listOf(
        TopBarIconConfig("REFRESH", true, "تحديث", "Refresh"),
        TopBarIconConfig("LANG", true, "اللغة", "Language"),
        TopBarIconConfig("INFO", true, "معلومات", "App Info"),
        TopBarIconConfig("ADMIN", true, "الأدمن", "Admin"),
        TopBarIconConfig("REGISTER", true, "تسجيل مهني", "Register"),
        TopBarIconConfig("BACKDOOR", true, "شعار البوابة", "Backdoor")
    )
)
