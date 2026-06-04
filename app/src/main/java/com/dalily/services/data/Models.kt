package com.dalily.services.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "categories")
@Serializable
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String,
    val nameEn: String,
    val imageUrl: String = "",
    val parentId: Int? = null, // null for Main category, specific Id for Sub category
    val sortOrder: Int = 0
)

@Entity(tableName = "service_providers")
@Serializable
data class ServiceProvider(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val phone: String,
    val mainCategory: String,
    val subCategory: String,
    val address: String,
    val district: String,
    val gpsCoordinates: String = "",
    val personalPhoto: String, // Base64 or local filepath URI
    val idCardPhoto: String = "", // Base64 or local filepath URI
    val isApproved: Boolean = false, // false is pending_provider, true is approved
    val rejectionReason: String = "",
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false,
    val isVerified: Boolean = false, // Blue badge
    val isBlocked: Boolean = false,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val loyaltyPoints: Int = 0,
    val subscriptionActive: Boolean = false,
    val subscriptionVerified: Boolean = false, // verification pending by Admin
    val registrationDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
@Serializable
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val userName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "banners")
@Serializable
data class Banner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUrl: String = "",
    val type: String = "IMAGE", // IMAGE, VIDEO, TEXT
    val textContent: String = "",
    val durationDays: Int = 7,
    val targetUrl: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "audit_logs")
@Serializable
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val adminName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
@Serializable
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val receiverName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
@Serializable
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val appName: String = "دليل الخدمات",
    val themeName: String = "كوزميك سيلفر", // كوزميك سيلفر, الذهبي الفاخر, الزمردي الراقي, مخصص
    val primaryColorHex: String = "#708090", // cosmic slate grey
    val secondaryColorHex: String = "#A9A9A9",
    val fontColorHex: String = "#FFFFFF", // Off-white bold text default
    val fontType: String = "Standard",
    val advertisingFooter: String = "MAW 777644670",
    val welcomeMessage: String = "مرحباً بكم في دليل الخدمات الطبيعية والمهنية",
    val supportPhone: String = "777644670",
    val supportEmail: String = "support@dalily.services",
    val supportWhatsapp: String = "777644670",
    val adminPassword: String = "maher736462",
    val enable2FA: Boolean = false,
    val isMaintenanceMode: Boolean = false,
    val isDataSavingMode: Boolean = false,
    val maxSearchRadius: Int = 25,
    val smartAssistantEnabled: Boolean = true,
    val smartAssistantIcon: String = "🤖",
    val smartAssistantSize: Int = 56,
    val smartAssistantX: Int = 16,
    val smartAssistantY: Int = 16,
    val chatIconEnabled: Boolean = true,
    val chatIconSize: Int = 56,
    val chatIconX: Int = 16,
    val chatIconY: Int = 80
)

@Entity(tableName = "authorized_devices")
@Serializable
data class AuthorizedDevice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String,
    val deviceName: String,
    val isApproved: Boolean = true
)

@Entity(tableName = "user_service_requests")
@Serializable
data class UserServiceRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val providerName: String,
    val providerCategory: String,
    val requestDate: Long = System.currentTimeMillis(),
    val status: String = "جاري الاتصال"
)
