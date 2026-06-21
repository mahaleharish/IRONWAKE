package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Workout Time",
    val isEnabled: Boolean = true,
    val monday: Boolean = true,
    val tuesday: Boolean = true,
    val wednesday: Boolean = true,
    val thursday: Boolean = true,
    val friday: Boolean = true,
    val saturday: Boolean = false,
    val sunday: Boolean = false,
    val vibrate: Boolean = true
) {
    fun getFormattedTime(): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    fun getActiveDaysDesc(): String {
        val days = mutableListOf<String>()
        if (monday) days.add("Mon")
        if (tuesday) days.add("Tue")
        if (wednesday) days.add("Wed")
        if (thursday) days.add("Thu")
        if (friday) days.add("Fri")
        if (saturday) days.add("Sat")
        if (sunday) days.add("Sun")

        return when {
            days.size == 7 -> "Everyday"
            days.size == 5 && !saturday && !sunday -> "Weekdays"
            days.size == 2 && saturday && sunday -> "Weekends"
            days.isEmpty() -> "Once Off"
            else -> days.joinToString(", ")
        }
    }
}
