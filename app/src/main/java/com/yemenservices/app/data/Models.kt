package com.yemenservices.app.data

import kotlinx.serialization.Serializable

@Serializable
data class YemenService(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val category: String = "", // "emergency", "medical", "finance", "transport", "government", "education"
    val phoneNumber: String = "",
    val whatsappNumber: String = "",
    val addressAr: String = "",
    val addressEn: String = "",
    val rating: Float = 4.5f,
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val isPinned: Boolean = false,
    val orderIndex: Int = 0
)

@Serializable
data class ServiceCategory(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val iconName: String = "",
    val isPinned: Boolean = false,
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
    val imageUrl: String = ""
)
