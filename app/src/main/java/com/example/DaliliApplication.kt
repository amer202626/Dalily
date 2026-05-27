package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.Repository
import com.example.data.SettingsManager
import com.example.data.SupabaseClient

class DaliliApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: Repository
    lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "dalili_db"
        ).fallbackToDestructiveMigration().build()

        settingsManager = SettingsManager(applicationContext)
        
        repository = Repository(
            database.categoryDao(),
            database.serviceProviderDao(),
            SupabaseClient.service
        )
    }
}
