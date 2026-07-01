package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

class AlarmRingingActivity : ComponentActivity() {

    private var alarmId: Int = -1
    private var alarmLabel: String = "Morning Workout"
    private var alarmHour: Int = 6
    private var alarmMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Force screen-on and over-lockscreen presence BEFORE super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Supports full edge-to-edge displays on modern devices (e.g. Android 15+)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // Parse extras
        alarmId = intent.getIntExtra("ALARM_ID", -1)
        alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up & Train!"
        alarmHour = intent.getIntExtra("ALARM_HOUR", 6)
        alarmMinute = intent.getIntExtra("ALARM_MINUTE", 0)

        // 2. Reject back-button bypass attempts. Users must trace the pattern!
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Ignore. Tracing is mandatory to stop the alarm.
                val explanation = "TRACING PATTERN MANDATORY TO SNOOZE/STOP ALARM"
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmId = intent.getIntExtra("ALARM_ID", -1)
        alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up & Train!"
        alarmHour = intent.getIntExtra("ALARM_HOUR", 6)
        alarmMinute = intent.getIntExtra("ALARM_MINUTE", 0)
    }

    private fun handleUnlockResult(isSnooze: Boolean) {
        val database = AppDatabase.getDatabase(applicationContext)
        val scheduler = AlarmScheduler(applicationContext)

        // Stop the alarm audio and haptics immediately with multi-layer redundancy
        val stopServiceIntent = Intent(applicationContext, AlarmService::class.java).apply {
            action = "com.example.ACTION_STOP_ALARM"
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(stopServiceIntent)
            } else {
                startService(stopServiceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmRingingActivity", "Error calling startService with stop action: ${e.message}")
        }
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
                    id = if (alarmId != -1) (100000 + alarmId) else (9999 + (1..100).random()), // temporary snooze ID range
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
                    Toast.makeText(this@AlarmRingingActivity, "SUCCESSFULLY STOPPED! LETS CRUSH YOUR WORKOUT!", Toast.LENGTH_LONG).show()
                }
            }

            // Close ringing activity
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Notify the service that the activity is in the foreground, suppressing the heads-up banner overlay
        val updateIntent = Intent(applicationContext, AlarmService::class.java).apply {
            action = "com.example.ACTION_ACTIVITY_FOREGROUND"
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(updateIntent)
            } else {
                startService(updateIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmRingingActivity", "Error updating notification: ${e.message}")
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

    // Three premium dark gym background options
    val backgroundUrls = remember {
        listOf(
            "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=80"
        )
    }

    // Sequentially rotate through the 3 backgrounds every time the alarm screen displays
    val selectedBgUrl = remember {
        val prefs = context.getSharedPreferences("ironwake_alarm_prefs", Context.MODE_PRIVATE)
        val lastIndex = prefs.getInt("last_bg_index", 0)
        val nextIndex = (lastIndex + 1) % backgroundUrls.size
        prefs.edit().putInt("last_bg_index", nextIndex).apply()
        backgroundUrls[lastIndex]
    }

    var settings by remember { mutableStateOf<UserSettings?>(null) }
    var patternMatched by remember { mutableStateOf(false) }
    var activeMode by remember { mutableStateOf("SNOOZE") } // "SNOOZE" or "STOP"
    var snoozePattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var stopPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isDraggingPattern by remember { mutableStateOf(false) }
    
    val selectedQuote = remember {
        listOf(
            "\"The only bad workout is the one that didn't happen.\"",
            "\"No alarm has power over a spirit waiting to conquer.\"",
            "\"Pain is temporary. Pride is forever. Get up and grind.\"",
            "\"Defeat the bed, conquer the day. Rise and build the shield.\"",
            "\"Your dream body and life aren't built in your sleep.\"",
            "\"Iron does not lie. It tells you exactly how hard you worked.\"",
            "\"The sweat of today is the strength of tomorrow.\"",
            "\"Excuses don't build muscles. Action does.\"",
            "\"Comfort is the enemy of progress. Step outside the zone.\"",
            "\"Success is rented, and rent is due every single morning.\""
        ).random()
    }

    // Live Clock State
    var currentTimeString by remember { mutableStateOf("05:00") }
    var currentDateString by remember { mutableStateOf("TUESDAY, MAY 21") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            settings = database.userSettingsDao().getSettings() ?: UserSettings()
        }
    }

    LaunchedEffect(settings) {
        val finalSettings = settings ?: UserSettings()
        val finalGridSize = when (finalSettings.patternDifficulty) {
            "5x5" -> 5
            "4x4" -> 4
            else -> 3
        }
        val snoozeLen = when (finalGridSize) {
            5 -> 6
            4 -> 5
            else -> 4
        }
        val stopLen = when (finalGridSize) {
            5 -> 10
            4 -> 8
            else -> 6
        }
        snoozePattern = generateRandomPattern(finalGridSize, snoozeLen)
        stopPattern = generateRandomPattern(finalGridSize, stopLen)
    }

    // Reset patternMatched when activeMode changes!
    LaunchedEffect(activeMode) {
        patternMatched = false
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
    val gridSize = when (finalSettings.patternDifficulty) {
        "5x5" -> 5
        "4x4" -> 4
        else -> 3
    }

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
            model = selectedBgUrl,
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
            alpha = 0.82f,
            modifier = Modifier.fillMaxSize()
        )

        // Elegant Dark Gradient overlay to make trace grids very clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Main layout items
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState, enabled = !isDraggingPattern)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Top Bar / Clock Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 36.dp)
            ) {
                Text(
                    text = "AWAKEN YOUR POTENTIAL",
                    color = NeonGreen,
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 4.2.sp,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        shadow = Shadow(
                            color = NeonGreen.copy(alpha = 0.6f),
                            offset = Offset(0f, 0f),
                            blurRadius = 12f
                        )
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = currentTimeString,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Black,
                        fontSize = 72.sp,
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
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Pattern Trace Grid (Center Section)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Action Choice Toggles (Tactile Segmented Button Style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { 
                            activeMode = "SNOOZE"
                            patternMatched = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMode == "SNOOZE") NeonCyan else Color.Transparent,
                            contentColor = if (activeMode == "SNOOZE") TrueBlack else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text(
                            text = "SNOOZE ALARM",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 1.sp)
                        )
                    }
                    Button(
                        onClick = { 
                            activeMode = "STOP"
                            patternMatched = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMode == "STOP") BrightRed else Color.Transparent,
                            contentColor = if (activeMode == "STOP") Color.White else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text(
                            text = "STOP ALARM",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 1.sp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .widthIn(max = 270.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(24.dp))
                        .padding(10.dp)
                ) {
                    val currentTargetPattern = if (activeMode == "SNOOZE") snoozePattern else stopPattern
                    androidx.compose.runtime.key(activeMode) {
                        PatternTraceGrid(
                            gridSize = gridSize,
                            patternColor = if (patternMatched) (if (activeMode == "SNOOZE") NeonCyan else BrightRed) else NeonGreen,
                            correctPattern = currentTargetPattern,
                            onPatternComplete = { trace ->
                                if (trace == currentTargetPattern) {
                                    patternMatched = true
                                } else {
                                    patternMatched = false
                                    if (trace.size < currentTargetPattern.size) {
                                        Toast.makeText(context, "TRACE INCOMPLETE! KEEP TRACING FORWARD!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "TRACE MISMATCH! A new trace path proposed.", Toast.LENGTH_SHORT).show()
                                        val finalGridSize = when (finalSettings.patternDifficulty) {
                                            "5x5" -> 5
                                            "4x4" -> 4
                                            else -> 3
                                        }
                                        val snoozeLen = when (finalGridSize) {
                                            5 -> 6
                                            4 -> 5
                                            else -> 4
                                        }
                                        val stopLen = when (finalGridSize) {
                                            5 -> 10
                                            4 -> 8
                                            else -> 6
                                        }
                                        if (activeMode == "SNOOZE") {
                                            snoozePattern = generateRandomPattern(finalGridSize, snoozeLen)
                                        } else {
                                            stopPattern = generateRandomPattern(finalGridSize, stopLen)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            onDragStateChanged = { dragging ->
                                isDraggingPattern = dragging
                            }
                        )
                    }
                }
            }

            // Bottom controls with beautiful dynamic enabled/disabled styling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                
                // Motivational Quote
                Text(
                    text = selectedQuote,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Controls row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // SNOOZE control
                    val canSnooze = activeMode == "SNOOZE" && patternMatched
                    Button(
                        onClick = {
                            if (canSnooze) {
                                onUnlocked(true)
                            } else if (activeMode != "SNOOZE") {
                                activeMode = "SNOOZE"
                                Toast.makeText(context, "SNOOZE CHASE DEPLOYED: TRACE THE MOVEMENT!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "TRACE THE MOVEMENT TO SNOOZE", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canSnooze) NeonCyan else MetalGray.copy(alpha = 0.5f),
                            contentColor = if (canSnooze) TrueBlack else Color.Gray
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        border = if (canSnooze) BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else null
                    ) {
                        Text(
                            "SNOOZE ALARM",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        )
                    }

                    // DISMISS control
                    val canDismiss = activeMode == "STOP" && patternMatched
                    Button(
                        onClick = {
                            if (canDismiss) {
                                onUnlocked(false)
                            } else if (activeMode != "STOP") {
                                activeMode = "STOP"
                                Toast.makeText(context, "STOP CHASE DEPLOYED: TRACE THE COMPLEX PATTERN!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "TRACE THE ADVANCED SEQUENCE TO STOP ALARM", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canDismiss) BrightRed else Color.DarkGray.copy(alpha = 0.3f),
                            contentColor = if (canDismiss) Color.White else Color.Gray
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "STOP ALARM",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        )
                    }
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

        val color = when (index) {
            0, 3 -> NeonGreen
            1, 4 -> NeonCyan
            2, 5 -> BrightRed
            else -> NeonGreen
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(w / 2, h * 0.4f),
                radius = w * 0.8f
            )
        )
    }
}

private fun generateRandomPattern(gridSize: Int, length: Int): List<Int> {
    val totalNodes = gridSize * gridSize
    var attempts = 0
    while (attempts < 100) {
        attempts++
        val path = mutableListOf<Int>()
        var current = (0 until totalNodes).random()
        path.add(current)
        
        var stuck = false
        for (step in 1 until length) {
            val r = current / gridSize
            val c = current % gridSize
            val neighbors = mutableListOf<Int>()
            for (dr in -1..1) {
                for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until gridSize && nc in 0 until gridSize) {
                        val nIdx = nr * gridSize + nc
                        if (!path.contains(nIdx)) {
                            neighbors.add(nIdx)
                        }
                    }
                }
            }
            if (neighbors.isEmpty()) {
                stuck = true
                break
            }
            current = neighbors.random()
            path.add(current)
        }
        if (!stuck && path.size == length) {
            return path
        }
    }
    // Fallback if somehow stuck too many times
    return (0 until totalNodes).shuffled().take(length)
}
