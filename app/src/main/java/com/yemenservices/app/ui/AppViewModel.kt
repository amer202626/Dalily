package com.yemenservices.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yemenservices.app.DaliliApplication
import com.yemenservices.app.data.Admin
import com.yemenservices.app.data.Category
import com.yemenservices.app.data.Review
import com.yemenservices.app.data.ServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DaliliApplication).repository
    private val TAG = "AppViewModel"

    // Stream state directly from realtime Firestore Snapshot flows
    val categories: StateFlow<List<Category>> = repository.categoriesFlow
    val serviceProviders: StateFlow<List<ServiceProvider>> = repository.providersFlow
    val reviews: StateFlow<List<Review>> = repository.reviewsFlow
    val admins: StateFlow<List<Admin>> = repository.adminsFlow

    private val _currentAdmin = MutableStateFlow<Admin?>(null)
    val currentAdmin: StateFlow<Admin?> = _currentAdmin.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    init {
        Log.i(TAG, "ViewModel initialized and streaming from Repository.")
        // Pre-create a default admin if none exist (for initial setup)
        viewModelScope.launch {
            repository.adminsFlow.collect { list ->
                if (list.isEmpty()) {
                    // Seed initial admin: username "admin", password "admin123"
                    val defaultAdmin = Admin(
                        username = "admin",
                        password_hash = hashPassword("admin123"),
                        role = "super_admin",
                        is_active = true
                    )
                    repository.saveAdmin(defaultAdmin, {}, {})
                }
            }
        }
    }

    // --- Helpers ---
    fun getCurrentTimeIso(): String {
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            df.timeZone = TimeZone.getTimeZone("UTC")
            df.format(Date())
        } catch (e: Exception) {
            "2026-05-28T21:50:00.000Z"
        }
    }

    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback to plain if digest is missing (unlikely)
        }
    }

    // --- Admin Authentication Operations ---
    fun login(usernameInput: String, passwordInput: String): Boolean {
        _loginError.value = null
        val foundAdmin = admins.value.firstOrNull { it.username.equals(usernameInput, ignoreCase = true) }
        
        if (foundAdmin == null) {
            _loginError.value = "المشرف غير موجود / Admin not found"
            return false
        }
        
        if (!foundAdmin.is_active) {
            _loginError.value = "هذا الحساب معطل / This account is inactive"
            return false
        }

        val inputHash = hashPassword(passwordInput)
        if (foundAdmin.password_hash == inputHash || foundAdmin.password_hash == passwordInput) {
            _currentAdmin.value = foundAdmin
            return true
        } else {
            _loginError.value = "كلمة المرور غير صحيحة / Incorrect password"
            return false
        }
    }

    fun logout() {
        _currentAdmin.value = null
    }

    // --- Category Operations ---
    fun addCategory(id: String, nameAr: String, nameEn: String, icon: String, orderIndex: Int, imageUrl: String?) {
        val category = Category(
            id = id.ifBlank { UUID.randomUUID().toString() },
            name_ar = nameAr,
            name_en = nameEn,
            icon = icon.ifBlank { "work" },
            order_index = orderIndex,
            is_active = true,
            image_url = imageUrl?.ifBlank { null }
        )
        repository.saveCategory(category, {
            Log.d(TAG, "Successfully added category: $id")
        }, {
            Log.e(TAG, "Failed adding category: $id", it)
        })
    }

    fun updateCategory(category: Category) {
        repository.saveCategory(category, {}, {})
    }

    fun deleteCategory(id: String) {
        repository.deleteCategory(id, {}, {})
    }

    // --- Service Provider Operations ---
    fun addServiceProvider(
        id: String,
        nameAr: String,
        nameEn: String,
        phone: String,
        categoryId: String,
        imageUrl: String?,
        rating: Float = 5.0f
    ) {
        val provider = ServiceProvider(
            id = id.ifBlank { UUID.randomUUID().toString() },
            name_ar = nameAr,
            name_en = nameEn,
            phone = phone,
            category_id = categoryId,
            rating = rating,
            is_active = true,
            image_url = imageUrl?.ifBlank { null }
        )
        repository.saveProvider(provider, {
            Log.d(TAG, "Successfully added service provider: $id")
        }, {
            Log.e(TAG, "Failed adding service provider: $id", it)
        })
    }

    fun updateServiceProvider(provider: ServiceProvider) {
        repository.saveProvider(provider, {}, {})
    }

    fun deleteServiceProvider(id: String) {
        repository.deleteProvider(id, {}, {})
    }

    // --- Review Operations ---
    fun addReview(providerId: String, userName: String, comment: String, rating: Float) {
        val reviewId = UUID.randomUUID().toString()
        val createdAt = getCurrentTimeIso()
        val review = Review(
            id = reviewId,
            provider_id = providerId,
            user_name = userName,
            comment = comment,
            rating = rating,
            created_at = createdAt
        )
        repository.saveReview(review, {
            Log.d(TAG, "Successfully added review: $reviewId")
            recalculateProviderRating(providerId)
        }, {
            Log.e(TAG, "Failed adding review: $reviewId", it)
        })
    }

    fun deleteReview(reviewId: String, providerId: String) {
        repository.deleteReview(reviewId, {
            Log.d(TAG, "Successfully deleted review: $reviewId")
            recalculateProviderRating(providerId)
        }, {
            Log.e(TAG, "Failed deleting review: $reviewId", it)
        })
    }

    private fun recalculateProviderRating(providerId: String) {
        // Calculate the average rating from Firestore snapshots
        val providerReviews = reviews.value.filter { it.provider_id == providerId }
        val avgRating = if (providerReviews.isEmpty()) {
            5.0f
        } else {
            providerReviews.map { it.rating }.average().toFloat()
        }
        
        val provider = serviceProviders.value.firstOrNull { it.id == providerId }
        if (provider != null) {
            val updated = provider.copy(rating = avgRating)
            updateServiceProvider(updated)
        }
    }

    // --- Admin Accounts Management (by Super Admin) ---
    fun addNewAdmin(username: String, role: String, isActive: Boolean) {
        val newAdmin = Admin(
            username = username,
            password_hash = hashPassword("admin123"), // Default password
            role = role,
            is_active = isActive
        )
        repository.saveAdmin(newAdmin, {}, {})
    }

    fun toggleAdminStatus(username: String, currentStatus: Boolean) {
        val admin = admins.value.firstOrNull { it.username == username }
        if (admin != null) {
            val updated = admin.copy(is_active = !currentStatus)
            repository.saveAdmin(updated, {}, {})
        }
    }

    fun deleteAdmin(username: String) {
        repository.deleteAdmin(username, {}, {})
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
