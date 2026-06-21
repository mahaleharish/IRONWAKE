package com.example.data.repository

import com.example.data.database.AlarmDao
import com.example.data.database.UserSettingsDao
import com.example.data.model.Alarm
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val userSettingsDao: UserSettingsDao
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    
    val settingsFlow: Flow<UserSettings> = userSettingsDao.getSettingsFlow()
        .map { it ?: UserSettings() }

    suspend fun getAlarmById(id: Int): Alarm? {
        return alarmDao.getAlarmById(id)
    }

    suspend fun insertAlarm(alarm: Alarm): Long {
        return alarmDao.insertAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun getSettings(): UserSettings {
        return userSettingsDao.getSettings() ?: UserSettings()
    }

    suspend fun saveSettings(settings: UserSettings) {
        userSettingsDao.saveSettings(settings)
    }
}
