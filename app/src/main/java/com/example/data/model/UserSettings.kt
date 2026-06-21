package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val patternDifficulty: String = "3x3", // "3x3", "4x4"
    val snoozeDurationMinutes: Int = 5, // 1, 5, 10
    val progressivelyIncreaseVolume: Boolean = true,
    val volumeLevel: String = "Mid" // "Low", "Mid", "High", "Superhigh"
)
