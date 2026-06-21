package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserSettings
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AlarmViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
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
        containerColor = TrueBlack,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TrueBlack)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Complexity level
            SettingsHeader(title = "PATTERN COMPLEXITY")
            DifficultySelectionCard(
                selectedDifficulty = settings.patternDifficulty,
                onSelect = { difficulty ->
                    viewModel.saveSettings(settings.copy(patternDifficulty = difficulty))
                }
            )

            // Section 2: Snooze settings
            SettingsHeader(title = "SNOOZE SCHEDULE")
            SnoozeDurationCard(
                selectedMinutes = settings.snoozeDurationMinutes,
                onSelect = { minutes ->
                    viewModel.saveSettings(settings.copy(snoozeDurationMinutes = minutes))
                }
            )

            // Section 3: Audio behaviors
            SettingsHeader(title = "WAKEUPS VOLUME CONTROL")
            VolumeLevelCard(
                currentVolumeLevel = settings.volumeLevel,
                progressiveIncrease = settings.progressivelyIncreaseVolume,
                onVolumeSelect = { level ->
                    viewModel.saveSettings(settings.copy(volumeLevel = level))
                },
                onToggleProgressive = { increase ->
                    viewModel.saveSettings(settings.copy(progressivelyIncreaseVolume = increase))
                }
            )

            Spacer(modifier = Modifier.height(30.dp))
            
            // Helpful bodybuilding quote for motivation!
            FitnessQuoteCard()
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = NeonCyan,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
fun DifficultySelectionCard(
    selectedDifficulty: String,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MetalGray),
        border = BorderStroke(1.dp, HardcoreSteel),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Trace Grid Dimensions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Select larger grid dimensions to challenge your motor controls in the morning.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("3x3", "4x4").forEach { size ->
                    val isSelected = selectedDifficulty == size
                    Button(
                        onClick = { onSelect(size) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NeonGreen else HardcoreSteel,
                            contentColor = if (isSelected) TrueBlack else Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("difficulty_$size")
                    ) {
                        Text(
                            text = size,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SnoozeDurationCard(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MetalGray),
        border = BorderStroke(1.dp, HardcoreSteel),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Snooze Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Time interval allowed between pattern prompts if snoozed.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 10).forEach { mins ->
                    val isSelected = selectedMinutes == mins
                    Button(
                        onClick = { onSelect(mins) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NeonGreen else HardcoreSteel,
                            contentColor = if (isSelected) TrueBlack else Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("snooze_${mins}m")
                    ) {
                        Text(
                            text = "$mins MIN",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VolumeLevelCard(
    currentVolumeLevel: String,
    progressiveIncrease: Boolean,
    onVolumeSelect: (String) -> Unit,
    onToggleProgressive: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MetalGray),
        border = BorderStroke(1.dp, HardcoreSteel),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Volume Levels",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Select volume level. Superhigh overrides DND mode and sets actual speaker output to maximum.",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Volume Options grid
            val levels = listOf("Low", "Mid", "High", "Superhigh")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                levels.forEach { level ->
                    val isSelected = currentVolumeLevel == level
                    Button(
                        onClick = { onVolumeSelect(level) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NeonGreen else HardcoreSteel,
                            contentColor = if (isSelected) TrueBlack else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("volume_$level")
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = HardcoreSteel, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Progressively Increase Volume",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Audio volume ramps up gradually to wake you up softly rather than startling your heart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGray
                    )
                }

                Switch(
                    checked = progressiveIncrease,
                    onCheckedChange = onToggleProgressive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TrueBlack,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = SoftGray,
                        uncheckedTrackColor = HardcoreSteel
                    ),
                    modifier = Modifier.testTag("progressive_volume_switch")
                )
            }
        }
    }
}

@Composable
fun FitnessQuoteCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ShinySteel),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "“The only bad workout is the one that didn't happen.”",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "— Champion Mentality",
                style = MaterialTheme.typography.labelSmall,
                color = SoftGray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
