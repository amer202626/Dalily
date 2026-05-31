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
    val descriptionEn: String = ""
)

@Serializable
data class ServiceCategory(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val iconName: String = ""
)
