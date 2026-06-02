package com.yemenservices.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
) : Serializable

data class Category(
    val id: Int = 0,
    val nameAr: String = "",
    val nameEn: String = "",
    val iconName: String = "",
    val isPinned: Boolean = false
) : Serializable

data class ServiceProvider(
    val id: Int = 0,
    val categoryId: Int = 0,
    val nameAr: String = "",
    val nameEn: String = "",
    val phone: String = "",
    val addressAr: String = "",
    val addressEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val workingHours: String = "",
    val isVerified: Boolean = false,
    
    // Custom dynamic fields per provider (Admin can fill these)
    val customField1Value: String = "",
    val customField2Value: String = "",
    val customField3Value: String = "",

    // Advanced fields
    val rating: Float = 4.5f,        // 1 to 5 stars
    val distanceKm: Float = 2.0f,     // Distance (close, medium, far)
    val priceLevel: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val latitude: Double = 15.3547,   // Google Map default (Sana'a)
    val longitude: Double = 44.2012,
    val profilePhoto: String = "",
    val idCardPhoto: String = "",
    val isFeatured: Boolean = false,  // recommended/head of list
    val callCount: Int = 0            // call analytics
) : Serializable

data class RegistrationRequest(
    val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val categoryId: Int = 0,
    val serviceType: String = "",
    val workplaceAddress: String = "",
    val residenceRegion: String = "",
    val profilePhoto: String = "",
    val idCardPhoto: String = "",
    val latitude: Double = 15.3547,
    val longitude: Double = 44.2012,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val timestamp: Long = 0
) : Serializable

data class Supervisor(
    val id: String = "",
    val username: String = "",
    val password: String = "",
    // Permissions
    val canAddProviders: Boolean = true,
    val canApproveRequests: Boolean = true,
    val canChangeSettings: Boolean = false
) : Serializable

data class ProviderReview(
    val id: String = "",
    val providerId: Int = 0,
    val authorName: String = "",
    val rating: Float = 5f,
    val comment: String = "",
    val timestamp: Long = 0
) : Serializable

