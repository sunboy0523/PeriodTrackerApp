package com.periodtracker

import android.app.Application
import com.periodtracker.data.AppDatabase

class PeriodTrackerApplication : Application() {
    
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: PeriodTrackerApplication
            private set
    }
}
