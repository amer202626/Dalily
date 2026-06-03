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
    val rating: Double = 4.5,
    val reviewsCount: Int = 12,
    val views: Int = 100,
    val isVerified: Boolean = false,
    val tags: List<String> = emptyList(),
    val address: String = "اليمن",
    val lat: Double = 15.3694,
    val lng: Double = 44.1910,
    val isAvailable: Boolean = true,
    val isFeatured: Boolean = false
)

@Serializable
data class CustomCategory(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val iconName: String = "Build",
    val colorHex: String = "#3B82F6"
)

@Serializable
data class BannerAd(
    val id: String,
    val imageUrl: String,
    val linkUrl: String = "",
    val title: String = "",
    val isActive: Boolean = true
)

@Serializable
data class AdminProfile(
    val id: String,
    val username: String,
    val email: String = "",
    val passwordHash: String,
    val creatorRole: String = "owner", // owner/backdoor or super_admin
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
