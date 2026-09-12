package com.simpleclock.app.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmScheduleDelayTest {
    @Test
    fun lessThanOneMinuteIsImminent() {
        assertEquals(AlarmScheduleDelay.Imminent, delayAfter(seconds = 59))
    }

    @Test
    fun partialMinutesRoundUp() {
        assertEquals(AlarmScheduleDelay.Minutes(2), delayAfter(seconds = 61))
    }

    @Test
    fun exactHourOmitsMinutes() {
        assertEquals(AlarmScheduleDelay.HoursMinutes(1, 0), delayAfter(minutes = 60))
    }

    @Test
    fun hoursAndMinutesAreSeparated() {
        assertEquals(AlarmScheduleDelay.HoursMinutes(2, 16), delayAfter(minutes = 135, seconds = 1))
    }

    @Test
    fun roundedTwentyFourHoursUsesAbsoluteTime() {
        assertEquals(
            AlarmScheduleDelay.Absolute,
            delayAfter(hours = 23, minutes = 59, seconds = 1),
        )
    }

    @Test
    fun pastTriggerIsImminent() {
        assertEquals(
            AlarmScheduleDelay.Imminent,
            alarmScheduleDelay(triggerAtMillis = NOW - 1, nowMillis = NOW),
        )
    }

    private fun delayAfter(
        hours: Long = 0,
        minutes: Long = 0,
        seconds: Long = 0,
    ): AlarmScheduleDelay = alarmScheduleDelay(
        triggerAtMillis = NOW + hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L,
        nowMillis = NOW,
    )

    private companion object {
        const val NOW = 1_000_000_000L
    }
}
