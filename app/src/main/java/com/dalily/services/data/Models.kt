package com.dalily.services.data

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String = "",
    val author: String = "",
    val text: String = "",
    val rating: Int = 5,
    val pinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ServiceProvider(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val subcategory: String = "",
    val description: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val rating: Float = 5.0f,
    val reviewsCount: Int = 0,
    val imageUrl: String = "",
    val secondaryImages: List<String> = emptyList(),
    val verified: Boolean = false,
    val online: Boolean = false,
    val lastLogin: String = "الآن",
    val views: Int = 0,
    val latitude: Double = 15.35 // Sana'a default latitude
    , val longitude: Double = 44.20 // Sana'a default longitude
    , val tags: List<String> = emptyList(),
    val workingHours: String = "08:00 AM - 10:00 PM",
    val availableSlots: List<String> = listOf("09:00 AM", "11:00 AM", "02:00 PM", "04:00 PM", "06:00 PM"),
    val comments: List<Comment> = emptyList(),
    val isChatDisabled: Boolean = false
)

@Serializable
data class BannerAd(
    val id: String = "",
    val imageUrl: String = "",
    val redirectUrl: String = "",
    val displayDuration: Int = 5, // in seconds
    val size: String = "Medium", // Small, Medium, Large
    val type: String = "Promo",   // Promo, Featured, Welcome
    val isPinned: Boolean = false,
    val isActive: Boolean = true
)

@Serializable
data class FcmChannel(
    val id: String = "",
    val name: String = "",
    val key: String = "",
    val description: String = "",
    val isEnabled: Boolean = true
)

@Serializable
data class Report(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = true
)

@Serializable
data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class Chat(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val messages: List<Message> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class AdminActionLog(
    val id: String = "",
    val adminName: String = "آدمن رئيسي",
    val action: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DeviceWhitelistEntry(
    val id: String = "",
    val deviceName: String = "",
    val ipAddress: String = "",
    val allowed: Boolean = true
)

@Serializable
data class DashboardWidget(
    val id: String = "",
    val title: String = "",
    val titleAr: String = "",
    val isEnabled: Boolean = true,
    val order: Int = 0
)

@Serializable
data class ScheduledNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val targetGroup: String = "الكافة", // "الكافة" (All), "مقدمي الخدمات" (Providers), "المستخدمين" (Users)
    val scheduledTime: String = "",
    val isSent: Boolean = false
)

@Serializable
data class AppSystemSettings(
    val maintenanceMode: Boolean = false,
    val dataSavingMode: Boolean = false,
    val imageCompression: Float = 0.5f, // 0.1 to 1.0 (or Off, Low, Med, High)
    val maxRadiusSearchKm: Int = 50,
    val defaultShareLink: String = "https://dalili-services.com/download",
    val isTourEnabled: Boolean = true,
    val twoFactorEnabled: Boolean = false,
    val twoFactorSecret: String = "JBSWY3DPEHPK3PXP"
)
