package com.yemenservices.app

import android.app.Application
import com.yemenservices.app.data.Repository

class DaliliApplication : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
    }
}
