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

    @Test
    fun alarmColorsContainsTwentyDistinctColors() {
        val colors = com.simpleclock.app.data.ALARM_COLORS
        assertEquals(20, colors.size)
        assertEquals(20, colors.distinct().size)
    }

    @Test
    fun defaultAlarmColorMatchesFirstPaletteColor() {
        val alarm = alarm(hour = 8, minute = 0)
        assertEquals(com.simpleclock.app.data.ALARM_DEFAULT_COLOR, alarm.color)
        assertEquals(com.simpleclock.app.data.ALARM_COLORS[0], alarm.color)
    }

    @Test
    fun randomRainbowGeneratesSixValidColors() {
        val colors = com.simpleclock.app.data.generateRandomRainbowColors()
        assertEquals(6, colors.size)
        colors.forEach { color ->
            // Alpha should be 0xFF
            org.junit.Assert.assertTrue((color and 0xFF000000L) != 0L)
        }
    }

    @Test
    fun appThemeColorContainsRandomRainbow() {
        org.junit.Assert.assertEquals(
            com.simpleclock.app.R.string.theme_random_rainbow,
            com.simpleclock.app.data.AppThemeColor.RANDOM_RAINBOW.labelRes,
        )
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

