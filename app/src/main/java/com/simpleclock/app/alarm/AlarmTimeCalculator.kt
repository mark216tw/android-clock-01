package com.simpleclock.app.alarm

import com.simpleclock.app.data.AlarmEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

object AlarmTimeCalculator {
    fun nextOccurrence(alarm: AlarmEntity, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val today = now.toLocalDate()

        if (alarm.repeatDays == 0) {
            val todayAtTime = occurrenceAt(alarm, today, now)
            return if (todayAtTime.isAfter(now)) {
                todayAtTime
            } else {
                occurrenceAt(alarm, today.plusDays(1), now)
            }
        }

        for (daysAhead in 0..7) {
            val candidate = occurrenceAt(alarm, today.plusDays(daysAhead.toLong()), now)
            val dayBit = 1 shl (candidate.dayOfWeek.value - 1)
            if (alarm.repeatDays and dayBit != 0 && candidate.isAfter(now)) {
                return candidate
            }
        }

        return occurrenceAt(alarm, today.plusWeeks(1), now)
    }

    private fun occurrenceAt(alarm: AlarmEntity, date: LocalDate, now: ZonedDateTime): ZonedDateTime {
        val localDateTime = date.atTime(LocalTime.of(alarm.hour, alarm.minute))
        val offsets = now.zone.rules.getValidOffsets(localDateTime)
        if (offsets.isEmpty()) return localDateTime.atZone(now.zone)

        val occurrences = offsets.map { offset ->
            ZonedDateTime.ofLocal(localDateTime, now.zone, offset)
        }
        return occurrences.firstOrNull { it.isAfter(now) } ?: occurrences.first()
    }
}
