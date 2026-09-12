package com.simpleclock.app.alarm

sealed interface AlarmScheduleDelay {
    data object Imminent : AlarmScheduleDelay
    data class Minutes(val minutes: Long) : AlarmScheduleDelay
    data class HoursMinutes(val hours: Long, val minutes: Long) : AlarmScheduleDelay
    data object Absolute : AlarmScheduleDelay
}

fun alarmScheduleDelay(
    triggerAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): AlarmScheduleDelay {
    val remainingMillis = triggerAtMillis - nowMillis
    if (remainingMillis < MILLIS_PER_MINUTE) return AlarmScheduleDelay.Imminent

    val roundedMinutes = remainingMillis / MILLIS_PER_MINUTE +
        if (remainingMillis % MILLIS_PER_MINUTE == 0L) 0L else 1L
    return when {
        roundedMinutes < MINUTES_PER_HOUR -> AlarmScheduleDelay.Minutes(roundedMinutes)
        roundedMinutes < MINUTES_PER_DAY -> AlarmScheduleDelay.HoursMinutes(
            hours = roundedMinutes / MINUTES_PER_HOUR,
            minutes = roundedMinutes % MINUTES_PER_HOUR,
        )
        else -> AlarmScheduleDelay.Absolute
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR
