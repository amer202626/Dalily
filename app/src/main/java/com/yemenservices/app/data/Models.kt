package com.yemenservices.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
) : Serializable

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameAr: String,
    val nameEn: String,
    val iconName: String
) : Serializable

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val nameAr: String,
    val nameEn: String,
    val phone: String,
    val addressAr: String,
    val addressEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val workingHours: String,
    val isVerified: Boolean = false,
    
    // Custom dynamic fields per provider (Admin can fill these)
    val customField1Value: String = "",
    val customField2Value: String = "",
    val customField3Value: String = ""
) : Serializable
