package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import com.example.AlarmService
import android.content.Intent
import com.example.data.model.Alarm
import com.example.data.model.UserSettings
import com.example.data.repository.AlarmRepository
import com.example.utils.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<UserSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun addAlarm(hour: Int, minute: Int, label: String, daysOfWeek: Set<Int>, vibrate: Boolean) {
        viewModelScope.launch {
            val newAlarm = Alarm(
                hour = hour,
                minute = minute,
                label = label,
                isEnabled = true,
                monday = daysOfWeek.contains(1),
                tuesday = daysOfWeek.contains(2),
                wednesday = daysOfWeek.contains(3),
                thursday = daysOfWeek.contains(4),
                friday = daysOfWeek.contains(5),
                saturday = daysOfWeek.contains(6),
                sunday = daysOfWeek.contains(7),
                vibrate = vibrate
            )
            val id = repository.insertAlarm(newAlarm)
            val savedAlarm = newAlarm.copy(id = id.toInt())
            scheduler.schedule(savedAlarm)
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
                // If this is the active ringing alarm, stop the service immediately
                if (AlarmService.isRinging && AlarmService.currentlyRingingAlarmId == alarm.id) {
                    try {
                        val stopIntent = Intent(MainApplication.instance, AlarmService::class.java).apply {
                            action = "com.example.ACTION_STOP_ALARM"
                        }
                        MainApplication.instance.startService(stopIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
            // If this is the active ringing alarm, stop the service immediately
            if (AlarmService.isRinging && AlarmService.currentlyRingingAlarmId == alarm.id) {
                try {
                    val stopIntent = Intent(MainApplication.instance, AlarmService::class.java).apply {
                        action = "com.example.ACTION_STOP_ALARM"
                    }
                    MainApplication.instance.startService(stopIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateAlarm(alarm: Alarm, hour: Int, minute: Int, label: String, daysOfWeek: Set<Int>, vibrate: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(
                hour = hour,
                minute = minute,
                label = label,
                isEnabled = true,
                monday = daysOfWeek.contains(1),
                tuesday = daysOfWeek.contains(2),
                wednesday = daysOfWeek.contains(3),
                thursday = daysOfWeek.contains(4),
                friday = daysOfWeek.contains(5),
                saturday = daysOfWeek.contains(6),
                sunday = daysOfWeek.contains(7),
                vibrate = vibrate
            )
            repository.updateAlarm(updated)
            scheduler.schedule(updated)
        }
    }

    fun saveSettings(updatedSettings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(updatedSettings)
        }
    }

    companion object {
        fun provideFactory(
            repository: AlarmRepository,
            scheduler: AlarmScheduler
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AlarmViewModel(repository, scheduler) as T
            }
        }
    }
}
