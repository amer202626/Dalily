package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getAllCategories(): Flow<List<Category>>

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
    @Query("SELECT * FROM service_providers ORDER BY rating DESC, nameAr ASC")
    fun getAllServiceProviders(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE categoryId = :categoryId ORDER BY rating DESC, nameAr ASC")
    fun getProvidersByCategoryId(categoryId: String): Flow<List<ServiceProvider>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceProvider(provider: ServiceProvider)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceProviders(providers: List<ServiceProvider>)

    @Query("DELETE FROM service_providers WHERE id = :id")
    suspend fun deleteProviderById(id: String)

    @Query("DELETE FROM service_providers")
    suspend fun clearAll()
}

@Database(entities = [Category::class, ServiceProvider::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun serviceProviderDao(): ServiceProviderDao
}
