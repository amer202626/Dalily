package com.yemenservices.app

import android.app.Application
import com.google.firebase.FirebaseApp

class DaliliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Throwable) {
            // Prevent crashes if Firebase is missing or play services aren't initialized
        }
    }
}
