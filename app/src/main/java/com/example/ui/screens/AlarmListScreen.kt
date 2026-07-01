package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import java.util.Calendar
import kotlinx.coroutines.launch
import com.example.data.model.Alarm
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlarmViewModel
import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private val homeQuotes = listOf(
    "The only bad workout is the one that didn't happen.",
    "No alarm has power over a spirit waiting to conquer.",
    "Pain is temporary. Pride is forever. Get up and grind.",
    "Defeat the bed, conquer the day. Rise and build the shield.",
    "Your dream body and life aren't built in your sleep.",
    "Iron does not lie. It tells you exactly how hard you worked.",
    "The sweat of today is the strength of tomorrow.",
    "Excuses don't build muscles. Action does.",
    "Comfort is the enemy of progress. Step outside the zone.",
    "Success is rented, and rent is due every single morning."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.alarms.collectAsState()
    var isFormDialogOpen by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPermission by remember { mutableStateOf(true) }
    var hasFullScreenPermission by remember { mutableStateOf(true) }

    fun checkPermissions() {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        hasFullScreenPermission = if (Build.VERSION.SDK_INT >= 34) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.canUseFullScreenIntent()
        } else {
            true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val showNotificationWarning = !hasNotificationPermission && Build.VERSION.SDK_INT >= 33
    val showFullScreenWarning = !hasFullScreenPermission && Build.VERSION.SDK_INT >= 34

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Active Indicator",
                            tint = NeonCyan,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "IRONWAKE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Page",
                            tint = NeonGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TrueBlack,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    editingAlarm = null
                    isFormDialogOpen = true 
                },
                containerColor = NeonGreen,
                contentColor = TrueBlack,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 40.dp)
                    .testTag("add_alarm_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New Alarm",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = TrueBlack,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TrueBlack)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tactical, high-contrast permission warning card if critical permissions are missing
                if (showNotificationWarning || showFullScreenWarning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2C1313) // Deep dark crimson warning
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF5252)), // Red border
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning icon",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "CRITICAL PERMISSIONS MISSING",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF8A8A),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            val warningDesc = if (showNotificationWarning && showFullScreenWarning) {
                                "Both Notification and Full-Screen alarm permissions are disabled. The full-screen workout pattern trace screen cannot launch when your alarm rings!"
                            } else if (showNotificationWarning) {
                                "Notification permission is disabled. Without notifications, the background alarm service is restricted and cannot show the wake-up pattern screen!"
                            } else {
                                "Full-screen alarm launch permission is disabled. Android will block the wake-up pattern screen from opening when the alarm sounds!"
                            }

                            Text(
                                text = warningDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (showNotificationWarning) {
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= 33) {
                                                notificationLauncher.launch("android.permission.POST_NOTIFICATIONS")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF5252),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Grant Notifications",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (showFullScreenWarning) {
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= 34) {
                                                val intent = Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
                                                    data = android.net.Uri.parse("package:${context.packageName}")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = android.net.Uri.parse("package:${context.packageName}")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(fallback)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonGreen,
                                            contentColor = TrueBlack
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Allow Full Screen",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (alarms.isEmpty()) {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyStateView()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 191.dp, top = 8.dp)
                    ) {
                        items(alarms, key = { it.id }) { alarm ->
                            AlarmRowItem(
                                alarm = alarm,
                                onToggle = { viewModel.toggleAlarm(alarm) },
                                onDelete = { viewModel.deleteAlarm(alarm) },
                                onEdit = {
                                    editingAlarm = alarm
                                    isFormDialogOpen = true
                                }
                            )
                        }
                    }
                }
            }

            // Sticky Translucent Bottom Motivational Message Card
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                TrueBlack.copy(alpha = 0.85f),
                                TrueBlack
                            )
                        )
                    )
                    .padding(bottom = 132.dp, top = 16.dp, start = 20.dp, end = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MetalGray.copy(alpha = 0.85f)
                    ),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_motivational_quote")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Fitness dumbbell icon",
                            tint = NeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        val quote = remember { homeQuotes.random() }
                        Text(
                            text = "\"$quote\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        if (isFormDialogOpen) {
            AddAlarmDialog(
                alarm = editingAlarm,
                onDismiss = { isFormDialogOpen = false },
                onConfirm = { hour, minute, label, repeatDays, vibrate ->
                    val currentEditing = editingAlarm
                    if (currentEditing != null) {
                        viewModel.updateAlarm(currentEditing, hour, minute, label, repeatDays, vibrate)
                    } else {
                        viewModel.addAlarm(hour, minute, label, repeatDays, vibrate)
                    }
                    isFormDialogOpen = false
                }
            )
        }
    }
}

@Composable
fun AlarmRowItem(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MetalGray
        ),
        border = BorderStroke(
            1.dp,
            if (alarm.isEnabled) NeonCyan.copy(alpha = 0.5f) else HardcoreSteel
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_item_${alarm.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = alarm.getFormattedTime(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (alarm.isEnabled) Color.White else SoftGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) NeonGreen else SoftGray
                    )
                }
                
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TrueBlack,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = SoftGray,
                        uncheckedTrackColor = HardcoreSteel
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alarm.getActiveDaysDesc(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alarm.isEnabled) NeonCyan else SoftGray,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Button (Pencil Icon)
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("alarm_edit_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit alarm, pencil icon",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete Button (Recycle Bin Icon)
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("alarm_delete_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete alarm, recycle bin icon",
                            tint = BrightRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .background(HardcoreSteel, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "NO ALARMS SCHEDULED",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "If you don't wake up early to train, someone else is taking your spot. Set your alarm now.",
            style = MaterialTheme.typography.bodyMedium,
            color = SoftGray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun mapToCalendarDay(day: Int): Int {
    return when(day) {
        1 -> Calendar.MONDAY
        2 -> Calendar.TUESDAY
        3 -> Calendar.WEDNESDAY
        4 -> Calendar.THURSDAY
        5 -> Calendar.FRIDAY
        6 -> Calendar.SATURDAY
        7 -> Calendar.SUNDAY
        else -> Calendar.MONDAY
    }
}

private fun calculateNextAlarmDuration(hour: Int, minute: Int, repeatDays: Set<Int>): Pair<Int, Int> {
    val now = Calendar.getInstance()
    var target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (repeatDays.isEmpty()) {
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
    } else {
        var minDiffMs = Long.MAX_VALUE
        var bestTarget: Calendar? = null
        
        for (day in repeatDays) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val calendarDay = mapToCalendarDay(day)
            val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            var daysDiff = calendarDay - currentDayOfWeek
            if (daysDiff < 0) {
                daysDiff += 7
            } else if (daysDiff == 0) {
                if (cal.before(now)) {
                    daysDiff = 7
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, daysDiff)
            
            val diffMs = cal.timeInMillis - now.timeInMillis
            if (diffMs > 0 && diffMs < minDiffMs) {
                minDiffMs = diffMs
                bestTarget = cal
            }
        }
        if (bestTarget != null) {
            target = bestTarget
        } else {
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    val diffMs = target.timeInMillis - now.timeInMillis
    val diffSec = maxOf(0L, diffMs / 1000)
    val diffHour = (diffSec / 3600).toInt()
    val diffMin = ((diffSec % 3600) / 60).toInt()
    return Pair(diffHour, diffMin)
}

@Composable
fun WheelTimePicker(
    selectedValue: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = range.toList()
    val itemsCount = items.size
    val virtualCount = itemsCount * 2000 // circular wrapping list size

    val middleIndex = virtualCount / 2
    val startIndex = if (itemsCount > 0) {
        middleIndex - (middleIndex % itemsCount) + maxOf(0, items.indexOf(selectedValue))
    } else {
        0
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    LaunchedEffect(selectedValue) {
        if (!listState.isScrollInProgress && itemsCount > 0) {
            val currentIndex = listState.firstVisibleItemIndex
            val currentMappedValue = items[currentIndex % itemsCount]
            if (currentMappedValue != selectedValue) {
                val offset = items.indexOf(selectedValue)
                if (offset >= 0) {
                    val currentBase = currentIndex - (currentIndex % itemsCount)
                    val targetIndex = currentBase + offset
                    listState.scrollToItem(targetIndex)
                }
            }
        }
    }

    val centerItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                startIndex
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                closest?.index ?: startIndex
            }
        }
    }

    LaunchedEffect(centerItemIndex) {
        if (itemsCount > 0) {
            val mappedValue = items[centerItemIndex % itemsCount]
            onValueChange(mappedValue)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(130.dp)
            .background(HardcoreSteel, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Highlighting bar overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(Color.White.copy(alpha = 0.05f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)))
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 44.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(virtualCount) { index ->
                val item = items[index % itemsCount]
                val isCenter = index == centerItemIndex
                
                // Find item layout details to check exact distance to center
                val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                val distance = itemInfo?.let { info ->
                    val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                    val itemCenter = info.offset + info.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                } ?: Int.MAX_VALUE

                // Highlight only when close to center (distance < 45% of item size)
                val isSelected = isCenter && (itemInfo == null || distance < (itemInfo.size * 0.45f))
                
                val scale = if (isSelected) 1.2f else 0.8f
                val color = if (isSelected) NeonGreen else SoftGray
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", item),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                            fontSize = if (isSelected) 24.sp else 18.sp
                        ),
                        color = color,
                        modifier = Modifier.graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AddAlarmDialog(
    alarm: Alarm? = null,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, label: String, repeatDays: Set<Int>, vibrate: Boolean) -> Unit
) {
    var hour by remember { mutableStateOf(alarm?.hour ?: 6) }
    var minute by remember { mutableStateOf(alarm?.minute ?: 0) }
    var labelText by remember { mutableStateOf(alarm?.label ?: "Morning Workout") }
    var vibrateOpt by remember { mutableStateOf(alarm?.vibrate ?: true) }
    var repeatDays by remember {
        mutableStateOf(
            alarm?.let {
                val set = mutableSetOf<Int>()
                if (it.monday) set.add(1)
                if (it.tuesday) set.add(2)
                if (it.wednesday) set.add(3)
                if (it.thursday) set.add(4)
                if (it.friday) set.add(5)
                if (it.saturday) set.add(6)
                if (it.sunday) set.add(7)
                set.toSet()
            } ?: setOf(1, 2, 3, 4, 5)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            border = BorderStroke(1.dp, NeonGreen),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (alarm != null) "EDIT YOUR ALARM" else "SET YOUR ALARM",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Scrollable Hours & Minutes Picker
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "HOUR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        WheelTimePicker(
                            selectedValue = hour,
                            range = 0..23,
                            onValueChange = { hour = it },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_hour_picker")
                        )
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        modifier = Modifier.padding(top = 18.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MINUTE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        WheelTimePicker(
                            selectedValue = minute,
                            range = 0..59,
                            onValueChange = { minute = it },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_minute_picker")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Next rings countdown display
                val (nextHours, nextMinutes) = remember(hour, minute, repeatDays) {
                    calculateNextAlarmDuration(hour, minute, repeatDays)
                }

                val ringText = if (nextHours == 0 && nextMinutes == 0) {
                    "Next rings in less than a minute"
                } else {
                    val hText = if (nextHours > 0) "$nextHours hour" + (if (nextHours > 1) "s" else "") else ""
                    val mText = if (nextMinutes > 0) "$nextMinutes minute" + (if (nextMinutes > 1) "s" else "") else ""
                    val join = if (hText.isNotEmpty() && mText.isNotEmpty()) " and " else ""
                    "Next rings in $hText$join$mText"
                }

                Surface(
                    color = NeonGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(
                        text = ringText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = NeonGreen,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Label field
                TextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Alarm Label", color = SoftGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = HardcoreSteel
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_label_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Day Selection Toggles
                Text(
                    text = "REPETITION DAYS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 1..7) {
                        val isSelected = repeatDays.contains(i)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isSelected) NeonCyan else HardcoreSteel,
                                    CircleShape
                                )
                                .clickable {
                                    repeatDays = if (isSelected) {
                                        repeatDays - i
                                    } else {
                                        repeatDays + i
                                    }
                                }
                        ) {
                            Text(
                                text = dayNames[i - 1],
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TrueBlack else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vibrate toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vibrate Device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Checkbox(
                        checked = vibrateOpt,
                        onCheckedChange = { vibrateOpt = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NeonGreen,
                            uncheckedColor = HardcoreSteel,
                            checkmarkColor = TrueBlack
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", color = Color.White)
                    }

                    Button(
                        onClick = {
                            onConfirm(
                                hour,
                                minute,
                                labelText.ifEmpty { "Morning Workout" },
                                repeatDays,
                                vibrateOpt
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.weight(1f).testTag("dialog_confirm_button")
                    ) {
                        Text("CONFIRM", color = TrueBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
