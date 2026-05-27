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
        // Step 1: Secure localized fallback seeding immediately for rapid UI rendering and total offline reliability
        try {
            val localCats = categoryDao.getAllCategories().first()
            if (localCats.isEmpty()) {
                Log.d(TAG, "Local database has no categories. Pre-seeding locally for instant boot...")
                val defaultCategories = listOf(
                    Category("cat_1", "صيانة منزلية", "Home Maintenance", "home_repair_service", 1, true, "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_2", "تقنية", "Technology", "laptop_mac", 2, true, "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_3", "تعليم", "Education", "school", 3, true, "https://images.unsplash.com/photo-1427504494785-3a9ca7044f45?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_4", "جمال", "Beauty", "brush", 4, true, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_5", "سيارات", "Cars", "directions_car", 5, true, "https://images.unsplash.com/photo-1486006920555-c77dce18193b?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_6", "خدمات منزلية", "Home Services", "cleaning_services", 6, true, "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_7", "شحن وتوصيل", "Shipping & Delivery", "local_shipping", 7, true, "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_8", "خدمات مهنية", "Professional Services", "work", 8, true, "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_9", "سيارات أجرة", "Taxi", "local_taxi", 9, true, "https://images.unsplash.com/photo-1494959764136-6be9eb3c261e?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_10", "توصيل طلبات", "Order Delivery", "delivery_dining", 10, true, "https://images.unsplash.com/photo-1526367790999-0150786486a9?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_11", "تأجير سيارات", "Car Rental", "car_rental", 11, true, "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=300&auto=format&fit=crop&q=60"),
                    Category("cat_12", "حجوزات شقق وفنادق", "Apartments & Hotels", "hotel", 12, true, "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=300&auto=format&fit=crop&q=60")
                )
                categoryDao.insertCategories(defaultCategories)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fail-safe: Failed to seed local categories: ${e.message}")
        }

        try {
            val localProviders = serviceProviderDao.getAllServiceProviders().first()
            if (localProviders.isEmpty()) {
                Log.d(TAG, "Local database has no providers. Pre-seeding locally for instant boot...")
                val defaultProviders = listOf(
                    ServiceProvider("prov_1", "المهندس فؤاد - صيانة أجهزة", "Eng. Fouad - Appliance Repair", "771234567", "cat_1", 4.8f, true, "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=200&auto=format&fit=crop&q=60"),
                    ServiceProvider("prov_2", "المحترف للشبكات والكمبيوتر", "The Professional for Networks & PC", "733987654", "cat_2", 4.9f, true, "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=200&auto=format&fit=crop&q=60"),
                    ServiceProvider("prov_3", "أكاديمية النجاح لتقوية الطلاب", "Success Academy for Tutoring", "711223344", "cat_3", 4.7f, true, "https://images.unsplash.com/photo-1427504494785-3a9ca7044f45?w=200&auto=format&fit=crop&q=60"),
                    ServiceProvider("prov_4", "مركز النخبة السريع لصيانة السيارات", "Al-Nokhbah Car Repair Quick Center", "775667788", "cat_5", 4.6f, true, "https://images.unsplash.com/photo-1486006920555-c77dce18193b?w=200&auto=format&fit=crop&q=60"),
                    ServiceProvider("prov_5", "البرق لشحن وتوصيل الطرود", "Al-Barq Shipping & Delivery Express", "777445566", "cat_7", 4.9f, true, "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=200&auto=format&fit=crop&q=60")
                )
                serviceProviderDao.insertServiceProviders(defaultProviders)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fail-safe: Failed to seed local service providers: ${e.message}")
        }

        // Step 2: Attempt remote fetch and synchronize with Supabase backend
        try {
            Log.d(TAG, "Connecting to Supabase to fetch remote categories...")
            val remoteCats = supabase.getCategories()
            if (remoteCats.isNotEmpty()) {
                categoryDao.clearAll()
                categoryDao.insertCategories(remoteCats)
                Log.d(TAG, "Synced ${remoteCats.size} categories successfully from Supabase.")
            } else {
                // Supabase is empty, publish current local database categories
                val currentLocalCats = categoryDao.getAllCategories().first()
                for (cat in currentLocalCats) {
                    try {
                        supabase.createCategory(cat)
                    } catch (err: Exception) {
                        Log.e(TAG, "Failed publishing category to Supabase: ${err.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from Supabase. Keeping current local database categories intact. Error: ${e.message}")
        }

        try {
            Log.d(TAG, "Connecting to Supabase to fetch remote providers...")
            val remoteProviders = supabase.getServiceProviders()
            if (remoteProviders.isNotEmpty()) {
                serviceProviderDao.clearAll()
                serviceProviderDao.insertServiceProviders(remoteProviders)
                Log.d(TAG, "Synced ${remoteProviders.size} service providers successfully from Supabase.")
            } else {
                // Supabase is empty, publish current local database providers
                val currentLocalProviders = serviceProviderDao.getAllServiceProviders().first()
                for (prov in currentLocalProviders) {
                    try {
                        supabase.createServiceProvider(prov)
                    } catch (err: Exception) {
                        Log.e(TAG, "Failed publishing provider to Supabase: ${err.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from Supabase. Keeping current local database providers intact. Error: ${e.message}")
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
