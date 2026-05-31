package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.ScanRepository

class ScanApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ScanRepository(database.scanDao()) }

    override fun onCreate() {
        super.onCreate()
        com.example.config.PerformanceSettings.initialize(this)
    }
}
