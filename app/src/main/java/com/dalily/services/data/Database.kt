package com.dalily.services.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceProviderDao {
    @Query("SELECT * FROM service_providers ORDER BY isPinned DESC, id DESC")
    fun getAllProvidersFlow(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE id = :id")
    suspend fun getProviderById(id: Int): ServiceProvider?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ServiceProvider): Long

    @Update
    suspend fun updateProvider(provider: ServiceProvider)

    @Delete
    suspend fun deleteProvider(provider: ServiceProvider)

    @Query("SELECT COUNT(*) FROM service_providers")
    suspend fun getCount(): Int
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY timestamp DESC")
    fun getReviewsForProviderFlow(providerId: Int): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review): Long

    @Query("SELECT AVG(rating) FROM reviews WHERE providerId = :providerId")
    suspend fun getAverageRatingForProvider(providerId: Int): Double?

    @Query("SELECT COUNT(*) FROM reviews WHERE providerId = :providerId")
    suspend fun getReviewsCountForProvider(providerId: Int): Int
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long
}

@Database(entities = [ServiceProvider::class, Review::class, AuditLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serviceProviderDao(): ServiceProviderDao
    abstract fun reviewDao(): ReviewDao
    abstract fun auditLogDao(): AuditLogDao
}
