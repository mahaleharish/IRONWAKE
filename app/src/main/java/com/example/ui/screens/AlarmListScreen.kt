package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Alarm
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlarmViewModel

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
                modifier = Modifier.testTag("add_alarm_button")
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
            if (alarms.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
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

@Composable
fun AddAlarmDialog(
    alarm: Alarm? = null,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, label: String, repeatDays: Set<Int>, vibrate: Boolean) -> Unit
) {
    var hourString by remember { mutableStateOf(alarm?.let { String.format("%02d", it.hour) } ?: "06") }
    var minuteString by remember { mutableStateOf(alarm?.let { String.format("%02d", it.minute) } ?: "00") }
    var labelText by remember { mutableStateOf(alarm?.label ?: "Morning Gym Session") }
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

                // Hour and minute inputs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextField(
                        value = hourString,
                        onValueChange = { hourString = it.take(2) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NeonGreen,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = HardcoreSteel,
                            unfocusedContainerColor = HardcoreSteel,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.width(90.dp).testTag("dialog_hour_input")
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    TextField(
                        value = minuteString,
                        onValueChange = { minuteString = it.take(2) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NeonGreen,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = HardcoreSteel,
                            unfocusedContainerColor = HardcoreSteel,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.width(90.dp).testTag("dialog_minute_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            val parsedHour = hourString.toIntOrNull() ?: 6
                            val parsedMin = minuteString.toIntOrNull() ?: 0
                            onConfirm(
                                parsedHour.coerceIn(0, 23),
                                parsedMin.coerceIn(0, 59),
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
