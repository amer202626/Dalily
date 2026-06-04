package com.dalily.services.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DalilyDao {

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Int)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // --- Service Providers ---
    @Query("SELECT * FROM service_providers ORDER BY isPinned DESC, id DESC")
    fun getServiceProviders(): Flow<List<ServiceProvider>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceProvider(provider: ServiceProvider)

    @Query("DELETE FROM service_providers WHERE id = :id")
    suspend fun deleteServiceProvider(id: Int)

    @Query("UPDATE service_providers SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: Int, isPinned: Boolean)

    @Query("UPDATE service_providers SET isRecommended = :isRecommended WHERE id = :id")
    suspend fun updateRecommendStatus(id: Int, isRecommended: Boolean)

    @Query("UPDATE service_providers SET isVerified = :isVerified WHERE id = :id")
    suspend fun updateVerificationStatus(id: Int, isVerified: Boolean)

    @Query("UPDATE service_providers SET isApproved = :approved, rejectionReason = :reason WHERE id = :id")
    suspend fun updateApprovalStatus(id: Int, approved: Boolean, reason: String)

    @Query("UPDATE service_providers SET isBlocked = :blocked WHERE id = :id")
    suspend fun updateBlockStatus(id: Int, blocked: Boolean)

    @Query("UPDATE service_providers SET loyaltyPoints = loyaltyPoints + :points WHERE id = :id")
    suspend fun addLoyaltyPoints(id: Int, points: Int)

    @Query("UPDATE service_providers SET subscriptionActive = :active, subscriptionVerified = :verified WHERE id = :id")
    suspend fun updateSubscriptionStatus(id: Int, active: Boolean, verified: Boolean)

    @Query("UPDATE service_providers SET averageRating = :avgRating, reviewCount = :count WHERE id = :id")
    suspend fun updateRating(id: Int, avgRating: Double, count: Int)

    // --- Reviews ---
    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY timestamp DESC")
    fun getReviewsForProvider(providerId: Int): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)

    @Query("DELETE FROM reviews WHERE providerId = :id")
    suspend fun deleteReviewsByProvider(id: Int)

    // --- Banners ---
    @Query("SELECT * FROM banners ORDER BY id DESC")
    fun getAllBanners(): Flow<List<Banner>>

    @Query("SELECT * FROM banners WHERE isActive = 1")
    fun getActiveBanners(): Flow<List<Banner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: Banner)

    @Query("DELETE FROM banners WHERE id = :id")
    suspend fun deleteBanner(id: Int)

    // --- App Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("DELETE FROM audit_logs WHERE timestamp < :olderThan")
    suspend fun clearAuditLogs(olderThan: Long)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllAuditLogs()

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(msg: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE timestamp < :olderThan")
    suspend fun clearChatMessages(olderThan: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages()

    // --- Authorized Devices ---
    @Query("SELECT * FROM authorized_devices")
    fun getAuthorizedDevices(): Flow<List<AuthorizedDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthorizedDevice(device: AuthorizedDevice)

    @Query("DELETE FROM authorized_devices WHERE id = :id")
    suspend fun deleteAuthorizedDevice(id: Int)

    // --- User Service Requests ---
    @Query("SELECT * FROM user_service_requests ORDER BY requestDate DESC")
    fun getUserServiceRequests(): Flow<List<UserServiceRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceRequest(request: UserServiceRequest)
}

@Database(
    entities = [
        Category::class,
        ServiceProvider::class,
        Review::class,
        Banner::class,
        AuditLog::class,
        ChatMessage::class,
        AppSettings::class,
        AuthorizedDevice::class,
        UserServiceRequest::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dalilyDao(): DalilyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dalily_services_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
