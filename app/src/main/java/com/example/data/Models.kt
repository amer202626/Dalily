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
    val id: String,
    
    @Json(name = "name_ar")
    val nameAr: String,
    
    @Json(name = "name_en")
    val nameEn: String,
    
    @Json(name = "icon")
    val icon: String,
    
    @Json(name = "order_index")
    val orderIndex: Int,
    
    @Json(name = "is_active")
    val isActive: Boolean = true,
    
    @Json(name = "image_url")
    val imageUrl: String? = null
)

@Entity(tableName = "service_providers")
@JsonClass(generateAdapter = true)
data class ServiceProvider(
    @PrimaryKey
    @Json(name = "id")
    val id: String,
    
    @Json(name = "name_ar")
    val nameAr: String,
    
    @Json(name = "name_en")
    val nameEn: String,
    
    @Json(name = "phone")
    val phone: String,
    
    @Json(name = "category_id")
    val categoryId: String,
    
    @Json(name = "rating")
    val rating: Float = 0.0f,
    
    @Json(name = "is_active")
    val isActive: Boolean = true,
    
    @Json(name = "image_url")
    val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class Admin(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "username")
    val username: String,
    
    @Json(name = "password_hash")
    val passwordHash: String,
    
    @Json(name = "role")
    val role: String,
    
    @Json(name = "is_active")
    val isActive: Boolean = true
)

