package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("AlarmReceiver", "Received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val database = AppDatabase.getDatabase(context)
            val scheduler = AlarmScheduler(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = database.alarmDao().getAllAlarms().first()
                    for (alarm in alarms) {
                        if (alarm.isEnabled) {
                            scheduler.schedule(alarm)
                        }
                    }
                    Log.d("AlarmReceiver", "Successfully rescheduled alarms on boot.")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed reschedule: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == "com.example.ACTION_TRIGGER_ALARM") {
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Workout Time!"
            val alarmHour = intent.getIntExtra("ALARM_HOUR", 0)
            val alarmMinute = intent.getIntExtra("ALARM_MINUTE", 0)

            val pendingResult = goAsync()
            val database = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (alarmId != -1) {
                        val alarm = database.alarmDao().getAlarmById(alarmId)
                        if (alarm == null || !alarm.isEnabled) {
                            Log.d("AlarmReceiver", "Alarm with ID $alarmId was deleted or disabled. Ignoring trigger.")
                            return@launch
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            putExtra("ALARM_ID", alarmId)
                            putExtra("ALARM_LABEL", alarmLabel)
                            putExtra("ALARM_HOUR", alarmHour)
                            putExtra("ALARM_MINUTE", alarmMinute)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }

                        try {
                            val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra("ALARM_ID", alarmId)
                                putExtra("ALARM_LABEL", alarmLabel)
                                putExtra("ALARM_HOUR", alarmHour)
                                putExtra("ALARM_MINUTE", alarmMinute)
                            }
                            context.startActivity(ringingIntent)
                        } catch (e: Exception) {
                            Log.e("AlarmReceiver", "Direct activity call blocked on background: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to verify alarm status: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
