package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.AppDatabase
import com.example.data.model.Alarm
import com.example.data.model.UserSettings
import com.example.ui.components.PatternTraceGrid
import com.example.ui.theme.*
import com.example.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.BorderStroke

class AlarmRingingActivity : ComponentActivity() {

    private var alarmId: Int = -1
    private var alarmLabel: String = "Morning Workout"
    private var alarmHour: Int = 6
    private var alarmMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parse extras
        alarmId = intent.getIntExtra("ALARM_ID", -1)
        alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up & Train!"
        alarmHour = intent.getIntExtra("ALARM_HOUR", 6)
        alarmMinute = intent.getIntExtra("ALARM_MINUTE", 0)

        // 1. Force screen-on, keyguard dismissal, and over-lockscreen presence
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Reject back-button bypass attempts. Users must trace the pattern!
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Ignore. Tracing is mandatory to stop the alarm.
                val explanation = "TRACING PATTERN MANDATORY TO SNOOZE/DISMISS"
                Toast.makeText(this@AlarmRingingActivity, explanation, Toast.LENGTH_SHORT).show()
            }
        })

        // Pick a random index matching our 10 gym illustrations
        val illustrationIndex = (0..9).random()

        setContent {
            MyApplicationTheme {
                RingingScreenContent(
                    label = alarmLabel,
                    illustrationIndex = illustrationIndex,
                    onUnlocked = { isSnooze ->
                        handleUnlockResult(isSnooze)
                    }
                )
            }
        }
    }

    private fun handleUnlockResult(isSnooze: Boolean) {
        val database = AppDatabase.getDatabase(applicationContext)
        val scheduler = AlarmScheduler(applicationContext)

        // Stop the alarm audio and haptics immediately
        val stopServiceIntent = Intent(applicationContext, AlarmService::class.java)
        stopService(stopServiceIntent)

        CoroutineScope(Dispatchers.IO).launch {
            val settings = database.userSettingsDao().getSettings() ?: UserSettings()
            
            if (isSnooze) {
                // Schedule snooze alarm (exact and single-use)
                val snoozeMin = settings.snoozeDurationMinutes
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, snoozeMin)
                }
                
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMin = calendar.get(Calendar.MINUTE)

                // Insert a temporary one-off alarm in our DB to trigger
                val snoozeAlarm = Alarm(
                    id = 9999 + (1..100).random(), // temporary snooze ID range
                    hour = currentHour,
                    minute = currentMin,
                    label = "$alarmLabel (Snoozed)",
                    isEnabled = true,
                    monday = false, tuesday = false, wednesday = false, 
                    thursday = false, friday = false, saturday = false, sunday = false,
                    vibrate = true
                )
                
                scheduler.schedule(snoozeAlarm)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AlarmRingingActivity,
                        "SNOOZED FOR $snoozeMin MINUTES. DONT GO BACK TO SLEEP!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                // Full dismiss, reschedule repeating rules for tomorrow if it is repeating
                if (alarmId != -1) {
                    val activeAlarm = database.alarmDao().getAlarmById(alarmId)
                    if (activeAlarm != null && activeAlarm.isEnabled) {
                        // Reschedule normal trigger next week/day
                        scheduler.schedule(activeAlarm)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AlarmRingingActivity, "SUCCESSFULLY DISMISSED! LETS CRUSH YOUR WORKOUT!", Toast.LENGTH_LONG).show()
                }
            }

            // Close ringing activity
            finish()
        }
    }
}

@Composable
fun RingingScreenContent(
    label: String,
    illustrationIndex: Int,
    onUnlocked: (isSnooze: Boolean) -> Unit
) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    var settings by remember { mutableStateOf<UserSettings?>(null) }
    var patternMatched by remember { mutableStateOf(false) }

    // Live Clock State
    var currentTimeString by remember { mutableStateOf("05:00") }
    var currentDateString by remember { mutableStateOf("TUESDAY, MAY 21") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            settings = database.userSettingsDao().getSettings() ?: UserSettings()
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            val cal = Calendar.getInstance()
            currentTimeString = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            
            val dayName = when(cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "MONDAY"
                Calendar.TUESDAY -> "TUESDAY"
                Calendar.WEDNESDAY -> "WEDNESDAY"
                Calendar.THURSDAY -> "THURSDAY"
                Calendar.FRIDAY -> "FRIDAY"
                Calendar.SATURDAY -> "SATURDAY"
                Calendar.SUNDAY -> "SUNDAY"
                else -> "TODAY"
            }
            val monthName = when(cal.get(Calendar.MONTH)) {
                Calendar.JANUARY -> "JANUARY"
                Calendar.FEBRUARY -> "FEBRUARY"
                Calendar.MARCH -> "MARCH"
                Calendar.APRIL -> "APRIL"
                Calendar.MAY -> "MAY"
                Calendar.JUNE -> "JUNE"
                Calendar.JULY -> "JULY"
                Calendar.AUGUST -> "AUGUST"
                Calendar.SEPTEMBER -> "SEPTEMBER"
                Calendar.OCTOBER -> "OCTOBER"
                Calendar.NOVEMBER -> "NOVEMBER"
                Calendar.DECEMBER -> "DECEMBER"
                else -> ""
            }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            currentDateString = "$dayName, $monthName $dayOfMonth"
            kotlinx.coroutines.delay(1000)
        }
    }

    val finalSettings = settings ?: UserSettings()
    val gridSize = if (finalSettings.patternDifficulty == "4x4") 4 else 3
    // Correct target connection path for match recognition standard (e.g. at least 4 nodes)
    val requiredLength = 4

    // Pulse animation for max volume indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val indicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // 1. Sleek Background Image Placeholder (High quality motivational gym context in grayscale layout)
        AsyncImage(
            model = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80",
            contentDescription = "Gym Background",
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.33f, 0.59f, 0.11f, 0F, 0F,
                        0.33f, 0.59f, 0.11f, 0F, 0F,
                        0.33f, 0.59f, 0.11f, 0F, 0F,
                        0F,    0F,    0F,    1F, 0F
                    )
                )
            ),
            alpha = 0.45f,
            modifier = Modifier.fillMaxSize()
        )

        // Elegant Dark Gradient overlay to make trace grids very clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black
                        )
                    )
                )
        )

        // Dynamic gym background canvas lines (behind grid)
        GymMotivationalCanvas(index = illustrationIndex)

        // 2. Volume indicator bar on Left Edge
        val volumeFraction = when (finalSettings.volumeLevel) {
            "Low" -> 0.25f
            "Mid" -> 0.50f
            "High" -> 0.75f
            "Superhigh" -> 1.00f
            else -> 0.50f
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .width(4.dp)
                .height(280.dp)
                .background(HardcoreSteel, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(volumeFraction)
                    .align(Alignment.BottomStart)
                    .background(NeonGreen, RoundedCornerShape(2.dp))
            )
        }

        // Main layout items
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Top Bar / Clock Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Text(
                    text = "WAKE UP THE BEAST",
                    color = NeonGreen,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = currentTimeString,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 86.sp,
                        color = Color.White,
                        shadow = Shadow(
                            color = NeonGreen.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 24f
                        )
                    )
                )
                
                Text(
                    text = currentDateString,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Pattern Trace Grid (Center Section)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = "REQUIRED TRACE: STRENGTH PATTERN",
                    color = NeonCyan,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Box(
                    modifier = Modifier
                        .size(290.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                        .padding(12.dp)
                ) {
                    PatternTraceGrid(
                        gridSize = gridSize,
                        patternColor = if (patternMatched) NeonCyan else NeonGreen,
                        onPatternComplete = { trace ->
                            if (trace.size >= requiredLength) {
                                patternMatched = true
                            } else {
                                patternMatched = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom controls with beautiful dynamic enabled/disabled styling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Motivational Quote
                Text(
                    text = "\"The only bad workout is the one that didn't happen.\"",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Controls row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SNOOZE control
                    Button(
                        onClick = {
                            if (patternMatched) {
                                onUnlocked(true)
                            } else {
                                Toast.makeText(context, "TRACE PATTERN TO UNLOCK SNOOZE", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (patternMatched) HardcoreSteel else MetalGray.copy(alpha = 0.5f),
                            contentColor = if (patternMatched) Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        border = if (patternMatched) BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "SNOOZE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            )
                            if (!patternMatched) {
                                Text(
                                    " (LOCKED)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // DISMISS control
                    Button(
                        onClick = {
                            if (patternMatched) {
                                onUnlocked(false)
                            } else {
                                Toast.makeText(context, "TRACE PATTERN TO UNLOCK DISMISS BUTTON", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (patternMatched) NeonGreen else Color.DarkGray.copy(alpha = 0.3f),
                            contentColor = if (patternMatched) TrueBlack else Color.Gray
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(
                            text = if (patternMatched) "DISMISS ALARM" else "🔒 TRACE FIRST",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        )
                    }
                }

                // Max Volume Pulse Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = BrightRed.copy(alpha = indicatorAlpha),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAX VOLUME ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp
                        ),
                        color = BrightRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


// Draw dynamic creative background elements instead of loading blank static drawables!
@Composable
fun GymMotivationalCanvas(index: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (index) {
            0, 3 -> { // Circular metallic barbell structure representation (Male Theme)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w / 2, h / 2),
                        radius = w * 0.75f
                    )
                )
                // Draw 45 LBS plates silhouettes
                drawCircle(
                    color = MetalGray,
                    radius = w * 0.25f,
                    center = Offset(w / 2, h * 0.35f),
                    style = Stroke(width = 30.dp.toPx())
                )
                drawCircle(
                    color = NeonGreen,
                    radius = w * 0.30f,
                    center = Offset(w / 2, h * 0.35f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            1, 4 -> { // Biceps flex arm drawing silhouette simulation (Female Theme)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(w / 2, h / 3),
                        radius = w
                    )
                )
                // Draw dynamic neon bars
                drawLine(
                    color = NeonCyan,
                    start = Offset(w * 0.15f, h * 0.3f),
                    end = Offset(w * 0.85f, h * 0.3f),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            2, 5 -> { // Dumbbell cross barbell stand (Mixed Theme)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BrightRed.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(w / 2, h / 2),
                        radius = w * 0.9f
                    )
                )
                // Geometric workout cross lines
                drawLine(
                    color = ShinySteel,
                    start = Offset(w * 0.2f, h * 0.2f),
                    end = Offset(w * 0.8f, h * 0.5f),
                    strokeWidth = 12.dp.toPx()
                )
                drawLine(
                    color = ShinySteel,
                    start = Offset(w * 0.8f, h * 0.2f),
                    end = Offset(w * 0.2f, h * 0.5f),
                    strokeWidth = 12.dp.toPx()
                )
            }
            else -> { // Standard metallic gradient structure
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(w / 2, h / 2),
                        radius = w * 0.8f
                    )
                )
            }
        }
    }
}
