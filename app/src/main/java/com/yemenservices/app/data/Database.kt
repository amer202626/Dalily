package com.yemenservices.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): AppSetting?

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface ServiceProviderDao {
    @Query("SELECT * FROM service_providers ORDER BY isVerified DESC, id DESC")
    fun getAllProviders(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE categoryId = :categoryId ORDER BY isVerified DESC, id DESC")
    fun getProvidersByCategoryId(categoryId: Int): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE id = :id LIMIT 1")
    fun getProviderById(id: Int): Flow<ServiceProvider?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ServiceProvider)

    @Update
    suspend fun updateProvider(provider: ServiceProvider)

    @Delete
    suspend fun deleteProvider(provider: ServiceProvider)
}

@Database(
    entities = [AppSetting::class, Category::class, ServiceProvider::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingDao(): AppSettingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun serviceProviderDao(): ServiceProviderDao
}
