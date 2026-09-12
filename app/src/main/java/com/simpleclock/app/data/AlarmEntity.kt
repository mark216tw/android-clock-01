package com.simpleclock.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val ALARM_COLOR_SATURATION = 0.70f
const val ALARM_COLOR_VALUE = 0.95f

val ALARM_COLOR_HUES: List<Float> = THEME_HUE_PRESETS
val ALARM_COLORS: List<Long> = ALARM_COLOR_HUES.map(::alarmColorFromHue)
val ALARM_DEFAULT_COLOR: Long = ALARM_COLORS.first()

fun alarmColorFromHue(hue: Float): Long = hsvToArgb(hue, ALARM_COLOR_SATURATION, ALARM_COLOR_VALUE)

fun alarmHueFromColor(color: Long): Float {
    val red = ((color shr 16) and 0xFF).toFloat() / 255f
    val green = ((color shr 8) and 0xFF).toFloat() / 255f
    val blue = (color and 0xFF).toFloat() / 255f
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta == 0f) return 0f
    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return if (hue < 0f) hue + 360f else hue
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Int = 0,
    val enabled: Boolean = true,
    val color: Long = ALARM_DEFAULT_COLOR,
    val sortOrder: Long = 0,
)
