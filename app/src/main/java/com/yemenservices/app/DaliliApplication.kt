package com.yemenservices.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class DaliliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:963621716942:android:375a24068863dbd490a19f")
                .setApiKey("AIzaSyCpxwXZZKrN4h2AmyuEkzyat0K4LOUAXD8")
                .setProjectId("yemenservices-fd56c")
                .setStorageBucket("yemenservices-fd56c.firebasestorage.app")
                .setGcmSenderId("963621716942")
                .build()

            FirebaseApp.initializeApp(this, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
