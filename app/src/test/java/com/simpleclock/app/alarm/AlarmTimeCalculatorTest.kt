package com.simpleclock.app.alarm

import com.simpleclock.app.data.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTimeCalculatorTest {
    private val zone = ZoneId.of("Asia/Taipei")

    @Test
    fun oneTimeAlarmUsesTodayWhenTimeIsStillAhead() {
        val now = dateTime(2026, 8, 25, 8, 30)
        val alarm = alarm(hour = 9, minute = 15)

        assertEquals(dateTime(2026, 8, 25, 9, 15), AlarmTimeCalculator.nextOccurrence(alarm, now))
    }

    @Test
    fun oneTimeAlarmMovesToTomorrowWhenTimeHasPassed() {
        val now = dateTime(2026, 8, 25, 9, 16)
        val alarm = alarm(hour = 9, minute = 15)

        assertEquals(dateTime(2026, 8, 26, 9, 15), AlarmTimeCalculator.nextOccurrence(alarm, now))
    }

    @Test
    fun repeatedAlarmUsesSelectedDayLaterToday() {
        val now = dateTime(2026, 8, 25, 8, 30) // Tuesday
        val alarm = alarm(hour = 9, minute = 15, repeatDays = dayMask(2))

        assertEquals(dateTime(2026, 8, 25, 9, 15), AlarmTimeCalculator.nextOccurrence(alarm, now))
    }

    @Test
    fun repeatedAlarmSkipsToNextSelectedWeekday() {
        val now = dateTime(2026, 8, 25, 10, 0) // Tuesday
        val alarm = alarm(hour = 9, minute = 15, repeatDays = dayMask(2, 4))

        assertEquals(dateTime(2026, 8, 27, 9, 15), AlarmTimeCalculator.nextOccurrence(alarm, now))
    }

    @Test
    fun repeatedAlarmAtExactCurrentMinuteMovesToNextWeek() {
        val now = dateTime(2026, 8, 25, 9, 15) // Tuesday
        val alarm = alarm(hour = 9, minute = 15, repeatDays = dayMask(2))

        assertEquals(dateTime(2026, 9, 1, 9, 15), AlarmTimeCalculator.nextOccurrence(alarm, now))
    }

    private fun alarm(hour: Int, minute: Int, repeatDays: Int = 0) = AlarmEntity(
        id = 1,
        hour = hour,
        minute = minute,
        label = "Test",
        repeatDays = repeatDays,
    )

    private fun dayMask(vararg isoDays: Int): Int = isoDays.fold(0) { mask, day ->
        mask or (1 shl (day - 1))
    }

    private fun dateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)
}
