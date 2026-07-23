package com.example

import android.os.Build
import android.os.Bundle
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AlarmListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import android.content.Intent
import com.example.ui.viewmodel.AlarmViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        
        // Redirect if alarm is actively ringing in the background
        if (AlarmService.isRinging) {
            val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("ALARM_ID", AlarmService.currentlyRingingAlarmId)
                putExtra("ALARM_LABEL", AlarmService.currentlyRingingAlarmLabel)
                putExtra("ALARM_HOUR", AlarmService.currentlyRingingAlarmHour)
                putExtra("ALARM_MINUTE", AlarmService.currentlyRingingAlarmMinute)
            }
            startActivity(ringingIntent)
        }
        
        // Supports full edge-to-edge displays on modern devices
        enableEdgeToEdge()

        // Request notification permission on startup for Android 13+ to avoid missing alarm ringing UI
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    "android.permission.POST_NOTIFICATIONS"
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 101)
            }
        }

        // Fetch application-level manual dependency injector references
        val app = application as MainApplication
        val viewModel = ViewModelProvider(
            this,
            AlarmViewModel.provideFactory(app.repository, app.scheduler)
        )[AlarmViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Set up modern type-safe compile-friendly navigation host routes
                    NavHost(
                        navController = navController,
                        startDestination = "alarm_list"
                    ) {
                        composable("alarm_list") {
                            AlarmListScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AlarmService.isRinging) {
            val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("ALARM_ID", AlarmService.currentlyRingingAlarmId)
                putExtra("ALARM_LABEL", AlarmService.currentlyRingingAlarmLabel)
                putExtra("ALARM_HOUR", AlarmService.currentlyRingingAlarmHour)
                putExtra("ALARM_MINUTE", AlarmService.currentlyRingingAlarmMinute)
            }
            startActivity(ringingIntent)
        }
    }
}
