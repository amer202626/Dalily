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

@Database(
    entities = [AppSetting::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingDao(): AppSettingDao
}
