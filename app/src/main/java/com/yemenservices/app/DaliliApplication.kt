package com.yemenservices.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.yemenservices.app.data.Repository

class DaliliApplication : Application() {
    
    // Maintain a single instance of the repository throughout the app's life
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        repository = Repository()
    }
}
