package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    suspend fun getAllCategoriesDirect(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface ServiceProviderDao {
    @Query("SELECT * FROM service_providers ORDER BY rating DESC")
    fun getAllServiceProviders(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers ORDER BY rating DESC")
    suspend fun getAllServiceProvidersDirect(): List<ServiceProvider>

    @Query("SELECT * FROM service_providers WHERE categoryId = :categoryId ORDER BY rating DESC")
    fun getProvidersByCategoryId(categoryId: String): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE categoryId = :categoryId ORDER BY rating DESC")
    suspend fun getProvidersByCategoryIdDirect(categoryId: String): List<ServiceProvider>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceProvider(provider: ServiceProvider)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceProviders(providers: List<ServiceProvider>)

    @Query("DELETE FROM service_providers WHERE id = :id")
    suspend fun deleteProviderById(id: String)

    @Query("DELETE FROM service_providers")
    suspend fun clearAll()
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun getReviewsForProvider(providerId: String): Flow<List<Review>>

    @Query("SELECT * FROM reviews WHERE providerId = :providerId ORDER BY createdAt DESC")
    suspend fun getReviewsForProviderDirect(providerId: String): List<Review>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<Review>)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReviewById(id: String)

    @Query("DELETE FROM reviews")
    suspend fun clearAll()
}

@Database(entities = [Category::class, ServiceProvider::class, Review::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun serviceProviderDao(): ServiceProviderDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dalili_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
