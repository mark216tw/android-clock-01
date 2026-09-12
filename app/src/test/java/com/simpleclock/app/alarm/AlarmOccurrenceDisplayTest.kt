package com.simpleclock.app.alarm

import com.simpleclock.app.data.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmOccurrenceDisplayTest {
    private val zone = ZoneId.of("Asia/Taipei")

    @Test
    fun disabledAlarmIsDisabled() {
        val display = display(hour = 9, enabled = false)

        assertEquals(AlarmOccurrenceDisplay.Disabled, display)
    }

    @Test
    fun laterTimeOnSameLocalDateIsToday() {
        val display = display(hour = 9)

        assertTrue(display is AlarmOccurrenceDisplay.Today)
    }

    @Test
    fun passedOneTimeAlarmCrossesMidnightToTomorrow() {
        val display = display(hour = 7)

        assertTrue(display is AlarmOccurrenceDisplay.Tomorrow)
    }

    @Test
    fun occurrenceTwoToSixLocalDatesAwayUsesLocaleWeekdayCategory() {
        val display = display(hour = 9, repeatDays = dayMask(DayOfWeek.FRIDAY))

        assertEquals(
            DayOfWeek.FRIDAY,
            (display as AlarmOccurrenceDisplay.Weekday).dayOfWeek,
        )
    }

    @Test
    fun sameWeekdayAfterItsTimeUsesNextWeekCategory() {
        val display = display(hour = 7, repeatDays = dayMask(DayOfWeek.TUESDAY))

        assertEquals(
            DayOfWeek.TUESDAY,
            (display as AlarmOccurrenceDisplay.NextWeek).dayOfWeek,
        )
    }

    @Test
    fun countdownUsesInstantAcrossDstGap() {
        val newYork = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 8, 0, 30, 0, 0, newYork)
        val alarm = AlarmEntity(id = 1, hour = 3, minute = 30, label = "Test")

        assertEquals(
            AlarmOccurrenceDisplay.Today(AlarmScheduleDelay.HoursMinutes(2, 0)),
            alarmOccurrenceDisplay(alarm, now),
        )
    }

    private fun display(
        hour: Int,
        repeatDays: Int = 0,
        enabled: Boolean = true,
    ): AlarmOccurrenceDisplay = alarmOccurrenceDisplay(
        alarm = AlarmEntity(
            id = 1,
            hour = hour,
            minute = 0,
            label = "Test",
            repeatDays = repeatDays,
            enabled = enabled,
        ),
        now = ZonedDateTime.of(2026, 8, 25, 8, 30, 0, 0, zone), // Tuesday
    )

    private fun dayMask(day: DayOfWeek): Int = 1 shl (day.value - 1)
}
