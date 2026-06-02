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
    val iconName: String = ""
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
    val customField3Value: String = ""
) : Serializable
