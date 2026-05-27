package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class Repository(
    private val categoryDao: CategoryDao,
    private val serviceProviderDao: ServiceProviderDao,
    private val supabase: SupabaseService
) {
    companion object {
        private const val TAG = "Repository"
    }

    val categories: Flow<List<Category>> = categoryDao.getAllCategories()
    val serviceProviders: Flow<List<ServiceProvider>> = serviceProviderDao.getAllServiceProviders()

    fun getProvidersByCategoryId(categoryId: String): Flow<List<ServiceProvider>> {
        return serviceProviderDao.getProvidersByCategoryId(categoryId)
    }

    suspend fun syncWithSupabase() {
        try {
            Log.d(TAG, "Syncing categories from Supabase...")
            val remoteCats = supabase.getCategories()
            categoryDao.clearAll()
            if (remoteCats.isNotEmpty()) {
                categoryDao.insertCategories(remoteCats)
                Log.d(TAG, "Synced ${remoteCats.size} categories from Supabase.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync categories with Supabase: ${e.message}")
        }

        try {
            Log.d(TAG, "Syncing service providers from Supabase...")
            val remoteProviders = supabase.getServiceProviders()
            serviceProviderDao.clearAll()
            if (remoteProviders.isNotEmpty()) {
                serviceProviderDao.insertServiceProviders(remoteProviders)
                Log.d(TAG, "Synced ${remoteProviders.size} service providers from Supabase.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync service providers with Supabase: ${e.message}")
        }
    }

    suspend fun saveCategory(category: Category) {
        categoryDao.insertCategory(category)
        try {
            val response = supabase.createCategory(category)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response while saving category to Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error saving category to Supabase: ${e.message}")
        }
    }

    suspend fun updateCategory(id: String, nameAr: String, nameEn: String, icon: String, orderIndex: Int, imageUrl: String? = null) {
        val cat = Category(id, nameAr, nameEn, icon, orderIndex, true, imageUrl)
        categoryDao.insertCategory(cat)
        try {
            val updates = mutableMapOf<String, Any>(
                "name_ar" to nameAr,
                "name_en" to nameEn,
                "icon" to icon,
                "order_index" to orderIndex
            )
            if (imageUrl != null) {
                updates["image_url"] = imageUrl
            }
            val response = supabase.updateCategory("eq.$id", updates)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response updating category in Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error updating category in Supabase: ${e.message}")
        }
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.deleteCategoryById(id)
        try {
            val response = supabase.deleteCategory("eq.$id")
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response deleting category from Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error deleting category from Supabase: ${e.message}")
        }
    }

    suspend fun saveServiceProvider(provider: ServiceProvider) {
        serviceProviderDao.insertServiceProvider(provider)
        try {
            val response = supabase.createServiceProvider(provider)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response saving provider to Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error saving provider to Supabase: ${e.message}")
        }
    }

    suspend fun updateServiceProvider(
        id: String,
        nameAr: String,
        nameEn: String,
        phone: String,
        categoryId: String,
        rating: Float,
        imageUrl: String?
    ) {
        val provider = ServiceProvider(id, nameAr, nameEn, phone, categoryId, rating, true, imageUrl)
        serviceProviderDao.insertServiceProvider(provider)
        try {
            val updates = mutableMapOf<String, Any>(
                "name_ar" to nameAr,
                "name_en" to nameEn,
                "phone" to phone,
                "category_id" to categoryId,
                "rating" to rating
            )
            if (imageUrl != null) {
                updates["image_url"] = imageUrl
            }
            val response = supabase.updateServiceProvider("eq.$id", updates)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response updating provider in Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error updating provider in Supabase: ${e.message}")
        }
    }

    suspend fun deleteServiceProvider(id: String) {
        serviceProviderDao.deleteProviderById(id)
        try {
            val response = supabase.deleteServiceProvider("eq.$id")
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response deleting provider from Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error deleting provider from Supabase: ${e.message}")
        }
    }

    suspend fun getAdmins(): List<Admin> {
        return try {
            supabase.getAdmins()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get admins from Supabase: ${e.message}")
            emptyList()
        }
    }

    suspend fun createAdmin(admin: Admin) {
        try {
            val response = supabase.createAdmin(admin)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response creating admin in Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error creating admin in Supabase: ${e.message}")
        }
    }

    suspend fun updateAdminPassword(username: String, newPasswordHash: String) {
        try {
            val updates = mapOf<String, Any>(
                "password_hash" to newPasswordHash
            )
            val response = supabase.updateAdmin("eq.$username", updates)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response updating admin password in Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error updating admin password in Supabase: ${e.message}")
        }
    }

    suspend fun deleteAdmin(username: String) {
        try {
            val response = supabase.deleteAdmin("eq.$username")
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response deleting admin from Supabase: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error deleting admin from Supabase: ${e.message}")
        }
    }
}
