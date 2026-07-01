package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.database.AppDatabase
import com.example.data.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentAlarmId: Int = -1
    private var currentAlarmLabel: String = "Wake Up & Train!"
    private var currentAlarmHour: Int = 6
    private var currentAlarmMinute: Int = 0

    companion object {
        private const val CHANNEL_ID = "IronWakeAlarmChannel"
        private const val NOTIFICATION_ID = 881
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.example.ACTION_STOP_ALARM") {
            cleanupMediaAndVibration()
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == "com.example.ACTION_ACTIVITY_FOREGROUND") {
            updateNotificationForForeground()
            return START_STICKY
        }

        val alarmLabel = intent?.getStringExtra("ALARM_LABEL") ?: "Wake Up & Train!"
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val alarmHour = intent?.getIntExtra("ALARM_HOUR", 6) ?: 6
        val alarmMinute = intent?.getIntExtra("ALARM_MINUTE", 0) ?: 0

        currentAlarmId = alarmId
        currentAlarmLabel = alarmLabel
        currentAlarmHour = alarmHour
        currentAlarmMinute = alarmMinute

        // 1. Build and show the foreground notification immediately
        val notification = buildForegroundNotification(alarmId, alarmLabel, alarmHour, alarmMinute)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 2. Fetch database settings dynamically and trigger Audio & Vibe routines
        serviceScope.launch {
            try {
                val settings = AppDatabase.getDatabase(applicationContext).userSettingsDao().getSettings() 
                    ?: UserSettings()
                
                if (isActive) {
                    triggerAlarmMedia(settings)
                }
            } catch (e: Exception) {
                Log.e("AlarmService", "Error reading settings: ${e.message}")
                if (isActive) {
                    // Fallback run with defaults
                    triggerAlarmMedia(UserSettings())
                }
            }
        }

        return START_STICKY
    }

    private fun triggerAlarmMedia(settings: UserSettings) {
        // Clean up any existing player/vibrator first to prevent multiple instances playing concurrently
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error releasing prior mediaPlayer: ${e.message}")
        }
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error canceling prior vibrator: ${e.message}")
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        // Determine target volume level based on user config
        val targetVolumeFraction = when (settings.volumeLevel) {
            "Low" -> 0.25f
            "Mid" -> 0.50f
            "High" -> 0.75f
            "Superhigh" -> 1.00f
            else -> 0.50f
        }
        val targetVolume = (maxVolume * targetVolumeFraction).toInt().coerceAtLeast(1)

        // Force volume to limit for Superhigh blast mode
        if (settings.volumeLevel == "Superhigh") {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
        }

        // Initialize Media Player
        try {
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri!!)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            
            // Handle Progressive Volume Increase if toggled
            if (settings.progressivelyIncreaseVolume) {
                val startVolume = 1.coerceAtMost(targetVolume / 4)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, startVolume, 0)
                
                volumeJob?.cancel()
                volumeJob = serviceScope.launch {
                    var currentVol = startVolume
                    while (currentVol < targetVolume) {
                        delay(3000) // Increase every 3 seconds
                        currentVol += 1
                        val finalVol = currentVol.coerceAtMost(targetVolume)
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, finalVol, 0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to setup audio playback: ${e.message}")
        }

        // Initialize Haptic Vibration
        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 700, 400) // Vibrate 700ms, pause 400ms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to start vibration: ${e.message}")
        }
    }

    private fun buildForegroundNotification(alarmId: Int, label: String, hour: Int, minute: Int): Notification {
        val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            putExtra("ALARM_HOUR", hour)
            putExtra("ALARM_MINUTE", minute)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            992 + (if (alarmId != -1) alarmId else 0),
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IRONWAKE ALARM ACTIVE")
            .setContentText(label.uppercase())
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotificationForForeground() {
        val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ALARM_ID", currentAlarmId)
            putExtra("ALARM_LABEL", currentAlarmLabel)
            putExtra("ALARM_HOUR", currentAlarmHour)
            putExtra("ALARM_MINUTE", currentAlarmMinute)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            992 + (if (currentAlarmId != -1) currentAlarmId else 0),
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IRONWAKE ALARM ACTIVE")
            .setContentText(currentAlarmLabel.uppercase())
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Suppress/retract any heads-up banner overlay
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IronWake Active Alarm Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification service channel for active morning alarms."
                setBypassDnd(true)
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun cleanupMediaAndVibration() {
        try {
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.e("AlarmService", "Error cancelling scope: ${e.message}")
        }
        volumeJob?.cancel()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping player: ${e.message}")
        }
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping vibrator: ${e.message}")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping foreground status: ${e.message}")
        }
    }

    override fun onDestroy() {
        cleanupMediaAndVibration()
        super.onDestroy()
    }
}
