package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class DaliliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("DaliliApplication", "Initializing Application & Firebase...")
        try {
            FirebaseApp.initializeApp(this)
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.d("DaliliApplication", "Firebase Firestore initialized with persistence.")
        } catch (e: Exception) {
            Log.e("DaliliApplication", "Failed to initialize Firebase: ${e.message}")
        }
    }
}
