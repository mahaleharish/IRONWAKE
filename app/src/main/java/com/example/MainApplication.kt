package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.AlarmRepository
import com.example.utils.AlarmScheduler

class MainApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao(), database.userSettingsDao()) }
    val scheduler by lazy { AlarmScheduler(this) }

    companion object {
        lateinit var instance: MainApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
