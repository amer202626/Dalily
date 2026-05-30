package com.yemenservices.app

import android.app.Application

class DaliliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase and Supabase have been completely removed as per user intent.
    }
}
