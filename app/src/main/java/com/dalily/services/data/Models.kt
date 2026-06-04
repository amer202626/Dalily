package com.dalily.services.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val description: String,
    val phone: String,
    val address: String,
    val isPremium: Boolean = false,
    val cardColorHex: String? = null, // Custom background color hex if premium
    val isRecommended: Boolean = false, // Recommended status (Admin controlled)
    val isPinned: Boolean = false, // Pinned at top (Admin controlled)
    val isVerified: Boolean = false, // Verified with blue badge (Admin controlled)
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val rating: Int, // 1 to 5 stars
    val comment: String,
    val reviewerName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val details: String,
    val location: String,
    val adminName: String
)
