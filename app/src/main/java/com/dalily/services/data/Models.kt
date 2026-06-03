package com.dalily.services.data

import androidx.compose.ui.graphics.Color
import java.io.Serializable

data class Category(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val icon: String = "Build", // Icon name like Build, Call, Health, etc.
    val colorHex: String = "#0D9488",
    val isPinned: Boolean = false,
    val orderIndex: Int = 0
) : Serializable

data class SubCategory(
    val id: String = "",
    val parentId: String = "",
    val nameAr: String = "",
    val nameEn: String = ""
) : Serializable

data class ServiceProvider(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val subCategory: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val address: String = "",
    val residenceRegion: String = "",
    val lat: Double = 15.3694,
    val lng: Double = 44.1910,
    val isAvailable: Boolean = true,
    val isPinned: Boolean = false,
    val isRecommended: Boolean = false,
    val rating: Double = 5.0,
    val reviewsCount: Int = 0,
    val views: Int = 0,
    val workHours: String = "08:00 AM - 10:00 PM"
) : Serializable

data class PendingProvider(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val category: String = "",
    val address: String = "",
    val residenceRegion: String = "",
    val gpsCoordinates: String = "",
    val profileImageUrl: String = "",
    val idCardUrl: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String = ""
) : Serializable

data class AdminAccount(
    val username: String = "",
    val passwordHash: String = "",
    val role: String = "ADMIN" // OWNER, ADMIN
) : Serializable

data class AppConfig(
    val appName: String = "دليل الخدمات",
    val primaryColorHex: String = "#0D9488",
    val secondaryColorHex: String = "#0F766E",
    val launcherIconUrl: String = "",
    val footerText: String = "MAW 777644670",
    val welcomeText: String = "مرحباً بكم في دليل الخدمات اليمني المعتمد لجميع المحافظات",
    val supportPhone: String = "777644670",
    val supportEmail: String = "maher@dalily.com",
    val masterAdminPassword: String = "maher736462", // This allows dynamic change
    
    // Smart Assistant dynamic properties
    val showAssistant: Boolean = true,
    val assistantIsDeleted: Boolean = false,
    val assistantPosition: String = "BottomRight", // "BottomRight", "BottomLeft", "TopRight", "TopLeft"
    val assistantColorHex: String = "#0D9488",
    val assistantIconName: String = "SmartToy", // "SmartToy", "SupportAgent", "Chat", "Help"
    val assistantWelcomeMsg: String = "مرحباً بكم! أنا مساعدكم الذكي، كيف يمكنني خدمتكم اليوم؟",
    val assistantHasWelcomeMsg: Boolean = true,
    val assistantWelcomeImageBase64: String = ""
) : Serializable

data class BannerAd(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val linkUrl: String = "", // Deep link or contact number
    val durationSeconds: Int = 5,
    val priorityOrder: Int = 1,
    val durationDays: Int = 7,
    val budget: Double = 10.0
) : Serializable

data class UserReview(
    val id: String = "",
    val providerId: String = "",
    val username: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

data class ChatMessage(
    val id: String = "",
    val providerId: String = "",
    val senderName: String = "",
    val text: String = "",
    val isFromUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

data class Report(
    val id: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val reason: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
