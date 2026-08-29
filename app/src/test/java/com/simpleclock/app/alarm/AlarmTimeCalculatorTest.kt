package com.simpleclock.app.alarm

import com.simpleclock.app.data.AlarmEntity
import com.simpleclock.app.data.fontSizeScale
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

    @Test
    fun fontSizeScaleCoversFiveLevelsForPortraitAndLandscape() {
        val base = com.simpleclock.app.data.AppSettings()
        assertEquals(0.70f, base.copy(clockFontSizePortrait = 1).fontSizeScale(isPortrait = true), 0.001f)
        assertEquals(0.85f, base.copy(clockFontSizePortrait = 2).fontSizeScale(isPortrait = true), 0.001f)
        assertEquals(1.00f, base.copy(clockFontSizePortrait = 3).fontSizeScale(isPortrait = true), 0.001f)
        assertEquals(1.15f, base.copy(clockFontSizePortrait = 4).fontSizeScale(isPortrait = true), 0.001f)
        assertEquals(1.30f, base.copy(clockFontSizePortrait = 5).fontSizeScale(isPortrait = true), 0.001f)

        assertEquals(0.70f, base.copy(clockFontSizeLandscape = 1).fontSizeScale(isPortrait = false), 0.001f)
        assertEquals(0.85f, base.copy(clockFontSizeLandscape = 2).fontSizeScale(isPortrait = false), 0.001f)
        assertEquals(1.00f, base.copy(clockFontSizeLandscape = 3).fontSizeScale(isPortrait = false), 0.001f)
        assertEquals(1.15f, base.copy(clockFontSizeLandscape = 4).fontSizeScale(isPortrait = false), 0.001f)
        assertEquals(1.30f, base.copy(clockFontSizeLandscape = 5).fontSizeScale(isPortrait = false), 0.001f)
    }

    @Test
    fun glassClockStyleMatchesResource() {
        org.junit.Assert.assertEquals(
            com.simpleclock.app.R.string.style_glass,
            com.simpleclock.app.data.ClockStyle.GLASS.labelRes,
        )
    }

    @Test
    fun clockStylesOrderedAsExpected() {
        val expected = listOf(
            com.simpleclock.app.data.ClockStyle.BOLD,
            com.simpleclock.app.data.ClockStyle.THIN,
            com.simpleclock.app.data.ClockStyle.NEON,
            com.simpleclock.app.data.ClockStyle.AURA_GLOW,
            com.simpleclock.app.data.ClockStyle.OUTLINE,
            com.simpleclock.app.data.ClockStyle.LED,
            com.simpleclock.app.data.ClockStyle.GLASS,
            com.simpleclock.app.data.ClockStyle.NIXIE_TUBE,
            com.simpleclock.app.data.ClockStyle.ANALOG_CLASSIC,
            com.simpleclock.app.data.ClockStyle.ANALOG_MINIMAL,
            com.simpleclock.app.data.ClockStyle.BAUHAUS_GEOMETRIC,
            com.simpleclock.app.data.ClockStyle.SWISS_RAILWAY,
        )
        org.junit.Assert.assertEquals(expected, com.simpleclock.app.data.ClockStyle.entries)
    }

    @Test
    fun screenOrientationEnumOrderedAsExpected() {
        val expected = listOf(
            com.simpleclock.app.data.ScreenOrientation.SYSTEM,
            com.simpleclock.app.data.ScreenOrientation.PORTRAIT,
            com.simpleclock.app.data.ScreenOrientation.LANDSCAPE,
        )
        org.junit.Assert.assertEquals(expected, com.simpleclock.app.data.ScreenOrientation.entries)
        org.junit.Assert.assertEquals(
            com.simpleclock.app.R.string.orientation_system,
            com.simpleclock.app.data.ScreenOrientation.SYSTEM.labelRes,
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

