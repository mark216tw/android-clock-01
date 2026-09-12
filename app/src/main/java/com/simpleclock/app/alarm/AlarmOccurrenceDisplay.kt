package com.simpleclock.app.alarm

import com.simpleclock.app.data.AlarmEntity
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

sealed interface AlarmOccurrenceDisplay {
    data object Disabled : AlarmOccurrenceDisplay
    data class Today(val delay: AlarmScheduleDelay) : AlarmOccurrenceDisplay
    data class Tomorrow(val delay: AlarmScheduleDelay) : AlarmOccurrenceDisplay
    data class Weekday(val dayOfWeek: DayOfWeek, val delay: AlarmScheduleDelay) : AlarmOccurrenceDisplay
    data class NextWeek(val dayOfWeek: DayOfWeek, val delay: AlarmScheduleDelay) : AlarmOccurrenceDisplay
}

fun alarmOccurrenceDisplay(
    alarm: AlarmEntity,
    now: ZonedDateTime = ZonedDateTime.now(),
): AlarmOccurrenceDisplay {
    if (!alarm.enabled) return AlarmOccurrenceDisplay.Disabled

    val occurrence = AlarmTimeCalculator.nextOccurrence(alarm, now)
    val delay = alarmScheduleDelay(
        triggerAtMillis = occurrence.toInstant().toEpochMilli(),
        nowMillis = now.toInstant().toEpochMilli(),
    )
    return when (ChronoUnit.DAYS.between(now.toLocalDate(), occurrence.toLocalDate())) {
        0L -> AlarmOccurrenceDisplay.Today(delay)
        1L -> AlarmOccurrenceDisplay.Tomorrow(delay)
        in 2L..6L -> AlarmOccurrenceDisplay.Weekday(occurrence.dayOfWeek, delay)
        else -> AlarmOccurrenceDisplay.NextWeek(occurrence.dayOfWeek, delay)
    }
}
