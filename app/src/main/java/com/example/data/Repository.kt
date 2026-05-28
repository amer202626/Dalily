package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import retrofit2.Response

fun getCurrentTimeIso(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

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

    suspend fun logSyncEvent(tableName: String, recordId: String, eventType: String) {
        try {
            val event = SyncEvent(
                tableName = tableName,
                recordId = recordId,
                eventType = eventType
            )
            Log.d(TAG, "Publishing sync event to Supabase: table=$tableName, ID=$recordId, Type=$eventType")
            supabase.createSyncEvent(event)
        } catch (e: Exception) {
            Log.e(TAG, "Failed logging sync_event to Supabase: ${e.message}")
        }
    }

    suspend fun syncWithSupabase(settingsManager: SettingsManager) {
        // Step 1: Secure localized fallback seeding immediately for rapid UI rendering and total offline reliability
        try {
            val localCats = categoryDao.getAllCategoriesDirect()
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
            val localProviders = serviceProviderDao.getAllServiceProvidersDirect()
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

        // Incremental / Event-driven sync system implementation:
        val lastSyncTime = settingsManager.lastSyncTimeIso
        val startTime = getCurrentTimeIso()

        if (lastSyncTime == "1970-01-01T00:00:00.000Z") {
            // First ever sync: Do a complete full sync to align initial data state perfectly
            try {
                Log.d(TAG, "First sync: fetching complete categories list from Supabase...")
                val remoteCats = supabase.getCategories()
                if (remoteCats.isNotEmpty()) {
                    categoryDao.clearAll()
                    categoryDao.insertCategories(remoteCats)
                } else {
                    // Populate Supabase if empty
                    val currentLocalCats = categoryDao.getAllCategoriesDirect()
                    for (cat in currentLocalCats) {
                        try { supabase.createCategory(cat) } catch (err: Exception) { err.printStackTrace() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed full categories sync: ${e.message}")
            }

            try {
                Log.d(TAG, "First sync: fetching complete providers list from Supabase...")
                val remoteProviders = supabase.getServiceProviders()
                if (remoteProviders.isNotEmpty()) {
                    serviceProviderDao.clearAll()
                    serviceProviderDao.insertServiceProviders(remoteProviders)
                } else {
                    // Populate Supabase if empty
                    val currentLocalProviders = serviceProviderDao.getAllServiceProvidersDirect()
                    for (prov in currentLocalProviders) {
                        try { supabase.createServiceProvider(prov) } catch (err: Exception) { err.printStackTrace() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed full providers sync: ${e.message}")
            }

            // Set final lastSyncTime to start offset
            settingsManager.lastSyncTimeIso = startTime
            Log.d(TAG, "First full sync done. Set initial lastSyncTimeIso to $startTime")
        } else {
            // Incremental Event-Driven syncing: Ask for matching events since lastSyncTime (Step 3)
            try {
                Log.d(TAG, "Incremental sync: fetching events newer than $lastSyncTime...")
                val events = supabase.getSyncEvents("gt.$lastSyncTime")
                Log.d(TAG, "Found ${events.size} new sync events.")

                if (events.isNotEmpty()) {
                    for (event in events) {
                        val tableName = event.tableName
                        val recordId = event.recordId
                        val eventType = event.eventType

                        Log.d(TAG, "Processing Event: Table=$tableName, ID=$recordId, Type=$eventType")

                        when (tableName) {
                            "categories" -> {
                                if (eventType == "DELETE") {
                                    categoryDao.deleteCategoryById(recordId)
                                } else {
                                    // INSERT or UPDATE: fetch the actual category row
                                    try {
                                        val rows = supabase.getCategoryById("eq.$recordId")
                                        if (rows.isNotEmpty()) {
                                            categoryDao.insertCategory(rows[0])
                                        } else {
                                            // Row no longer exists
                                            categoryDao.deleteCategoryById(recordId)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed pulling specific category $recordId: ${e.message}")
                                    }
                                }
                            }
                            "service_providers" -> {
                                if (eventType == "DELETE") {
                                    serviceProviderDao.deleteProviderById(recordId)
                                } else {
                                    // INSERT or UPDATE: fetch the actual provider row
                                    try {
                                        val rows = supabase.getServiceProviderById("eq.$recordId")
                                        if (rows.isNotEmpty()) {
                                            serviceProviderDao.insertServiceProvider(rows[0])
                                        } else {
                                            // Row no longer exists
                                            serviceProviderDao.deleteProviderById(recordId)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed pulling specific provider $recordId: ${e.message}")
                                    }
                                }
                            }
                            "admins" -> {
                                // For admins, sync is automatically re-run from the ViewModel if needed, or by getting all
                            }
                        }
                    }

                    // Update lastSyncTime to the created_at of the very last processed event (ensures sequence integrity)
                    val lastEventCreatedAt = events.last().createdAt
                    if (!lastEventCreatedAt.isNullOrBlank()) {
                        settingsManager.lastSyncTimeIso = lastEventCreatedAt
                        Log.d(TAG, "Updated lastSyncTimeIso to last event timestamp: $lastEventCreatedAt")
                    } else {
                        settingsManager.lastSyncTimeIso = startTime
                        Log.d(TAG, "Updated lastSyncTimeIso to query startTime because last event has null timestamp: $startTime")
                    }
                } else {
                    // Update lastSyncTime timestamp to query start time to prevent re-querying empty windows over and over
                    settingsManager.lastSyncTimeIso = startTime
                    Log.d(TAG, "No new events found. Advanced window lastSyncTimeIso to $startTime")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed incremental sync process: ${e.message}")
            }
        }
    }

    suspend fun saveCategory(category: Category) {
        categoryDao.insertCategory(category)
        try {
            val response = supabase.createCategory(category)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed response while saving category to Supabase: ${response.code()}")
            } else {
                logSyncEvent("categories", category.id, "INSERT")
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
            } else {
                logSyncEvent("categories", id, "UPDATE")
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
            } else {
                logSyncEvent("categories", id, "DELETE")
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
            } else {
                logSyncEvent("service_providers", provider.id, "INSERT")
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
            } else {
                logSyncEvent("service_providers", id, "UPDATE")
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
            } else {
                logSyncEvent("service_providers", id, "DELETE")
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
            } else {
                logSyncEvent("admins", admin.username, "INSERT")
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
            } else {
                logSyncEvent("admins", username, "UPDATE")
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
            } else {
                logSyncEvent("admins", username, "DELETE")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error deleting admin from Supabase: ${e.message}")
        }
    }
}
