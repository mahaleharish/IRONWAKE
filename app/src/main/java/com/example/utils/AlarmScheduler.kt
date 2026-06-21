package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.AlarmReceiver
import com.example.MainActivity
import com.example.data.model.Alarm
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmScheduler", "Exact alarm permission not granted. Attempting fallback.")
            }
        }

        val triggerTime = calculateNextTriggerTime(alarm)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_ALARM"
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_HOUR", alarm.hour)
            putExtra("ALARM_MINUTE", alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
        Log.d("AlarmScheduler", "Alarm scheduled for ID ${alarm.id} at timestamp $triggerTime")
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
        Log.d("AlarmScheduler", "Alarm canceled for ID ${alarm.id}")
    }

    private fun calculateNextTriggerTime(alarm: Alarm): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = Calendar.getInstance()

        val isRepeating = alarm.monday || alarm.tuesday || alarm.wednesday ||
                alarm.thursday || alarm.friday || alarm.saturday || alarm.sunday

        if (!isRepeating) {
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        } else {
            val todayDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            var daysOffset = -1
            
            for (i in 0..7) {
                val checkIndex = (todayDayOfWeek + i - 1) % 7 + 1
                val isAlarmDayEnabled = when (checkIndex) {
                    Calendar.MONDAY -> alarm.monday
                    Calendar.TUESDAY -> alarm.tuesday
                    Calendar.WEDNESDAY -> alarm.wednesday
                    Calendar.THURSDAY -> alarm.thursday
                    Calendar.FRIDAY -> alarm.friday
                    Calendar.SATURDAY -> alarm.saturday
                    Calendar.SUNDAY -> alarm.sunday
                    else -> false
                }

                if (isAlarmDayEnabled) {
                    if (i == 0) {
                        if (calendar.after(now)) {
                            daysOffset = 0
                            break
                        }
                    } else {
                        daysOffset = i
                        break
                    }
                }
            }

            if (daysOffset == -1) {
                // If somehow no matching day config, set today or tomorrow
                if (calendar.before(now)) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else if (daysOffset > 0) {
                calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
            }
            return calendar.timeInMillis
        }
    }
}
