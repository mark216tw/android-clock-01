package com.simpleclock.app.alarm

import com.simpleclock.app.data.AlarmEntity
import java.time.ZonedDateTime

object AlarmTimeCalculator {
    fun nextOccurrence(alarm: AlarmEntity, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val todayAtTime = now
            .withHour(alarm.hour)
            .withMinute(alarm.minute)
            .withSecond(0)
            .withNano(0)

        if (alarm.repeatDays == 0) {
            return if (todayAtTime.isAfter(now)) todayAtTime else todayAtTime.plusDays(1)
        }

        for (daysAhead in 0..7) {
            val candidate = todayAtTime.plusDays(daysAhead.toLong())
            val dayBit = 1 shl (candidate.dayOfWeek.value - 1)
            if (alarm.repeatDays and dayBit != 0 && candidate.isAfter(now)) {
                return candidate
            }
        }

        return todayAtTime.plusWeeks(1)
    }
}
