package com.yemenservices.app

import android.app.Application
import com.google.firebase.FirebaseApp

class DaliliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: java.lang.Exception) {
            // Prevent crashes in offline preview simulators where play services are not present
        }
    }
}
