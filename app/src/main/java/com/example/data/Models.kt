package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "categories")
@JsonClass(generateAdapter = true)
data class Category(
    @PrimaryKey
    @Json(name = "id")
    var id: String = "",
    
    @Json(name = "name_ar")
    var nameAr: String = "",
    
    @Json(name = "name_en")
    var nameEn: String = "",
    
    @Json(name = "icon")
    var icon: String = "",
    
    @Json(name = "order_index")
    var orderIndex: Int = 0,
    
    @Json(name = "is_active")
    var isActive: Boolean = true,
    
    @Json(name = "image_url")
    var imageUrl: String? = null
)

@Entity(tableName = "service_providers")
@JsonClass(generateAdapter = true)
data class ServiceProvider(
    @PrimaryKey
    @Json(name = "id")
    var id: String = "",
    
    @Json(name = "name_ar")
    var nameAr: String = "",
    
    @Json(name = "name_en")
    var nameEn: String = "",
    
    @Json(name = "phone")
    var phone: String = "",
    
    @Json(name = "category_id")
    var categoryId: String = "",
    
    @Json(name = "rating")
    var rating: Float = 5.0f,
    
    @Json(name = "is_active")
    var isActive: Boolean = true,
    
    @Json(name = "image_url")
    var imageUrl: String? = null
)

@Entity(tableName = "admins")
@JsonClass(generateAdapter = true)
data class Admin(
    @PrimaryKey
    @Json(name = "username")
    var username: String = "",
    
    @Json(name = "password_hash")
    var passwordHash: String = "",
    
    @Json(name = "role")
    var role: String = "",
    
    @Json(name = "is_active")
    var isActive: Boolean = true
)

@Entity(tableName = "reviews")
@JsonClass(generateAdapter = true)
data class Review(
    @PrimaryKey
    @Json(name = "id")
    var id: String = "",
    
    @Json(name = "provider_id")
    var providerId: String = "",
    
    @Json(name = "user_name")
    var userName: String = "",
    
    @Json(name = "comment")
    var comment: String = "",
    
    @Json(name = "rating")
    var rating: Float = 5.0f,
    
    @Json(name = "created_at")
    var createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class SyncEvent(
    @Json(name = "id")
    var id: String = "",
    
    @Json(name = "table_name")
    var tableName: String = "",
    
    @Json(name = "record_id")
    var recordId: String = "",
    
    @Json(name = "event_type")
    var eventType: String = "",
    
    @Json(name = "created_at")
    var createdAt: String? = null
)
