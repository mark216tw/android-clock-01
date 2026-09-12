package com.simpleclock.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HueTest {
    @Test
    fun systemThemeHueDefaultsToSkyWithoutDependingOnClockTheme() {
        val settings = AppSettings(themeColor = AppThemeColor.CORAL)

        assertEquals(DEFAULT_SYSTEM_THEME_HUE, settings.systemThemeHue)
        assertEquals(AppThemeColor.CORAL, settings.themeColor)
    }

    @Test
    fun hueNormalizationWrapsAndRejectsNonFiniteValues() {
        assertEquals(350f, normalizeHue(-10f))
        assertEquals(5f, normalizeHue(725f))
        assertEquals(DEFAULT_SYSTEM_THEME_HUE, normalizeHue(Float.NaN))
        assertEquals(DEFAULT_SYSTEM_THEME_HUE, normalizeHue(Float.POSITIVE_INFINITY))
    }

    @Test
    fun missingAndIllegalStoredSystemHuesUseSky() {
        assertEquals(DEFAULT_SYSTEM_THEME_HUE, systemThemeHueOrDefault(null))
        assertEquals(DEFAULT_SYSTEM_THEME_HUE, systemThemeHueOrDefault(Float.NaN))
        assertEquals(DEFAULT_SYSTEM_THEME_HUE, systemThemeHueOrDefault(Float.NEGATIVE_INFINITY))
        assertEquals(350f, systemThemeHueOrDefault(-10f))
    }

    @Test
    fun sharedAlarmHueConversionKeepsExistingPresetColors() {
        assertEquals(0xFFF24965L, alarmColorFromHue(350f))
        assertEquals(0xFFF28F49L, alarmColorFromHue(25f))
        assertEquals(0xFFF2D649L, alarmColorFromHue(50f))
        assertEquals(0xFF49F29DL, alarmColorFromHue(150f))
        assertEquals(0xFF49BAF2L, alarmColorFromHue(200f))
        assertEquals(0xFF9D49F2L, alarmColorFromHue(270f))
    }

    @Test
    fun systemThemePresetsAreLivelyAndIndependentFromAlarmPresets() {
        assertEquals(listOf(340f, 24f, 52f, 155f, 200f, 275f), SYSTEM_THEME_HUE_PRESETS)
        assertEquals(listOf(350f, 25f, 50f, 150f, 200f, 270f), ALARM_COLOR_HUES)
    }

    @Test
    fun rainbowHueColorsUseTheExpectedSpectrumAnchors() {
        assertEquals(0xFFFF0000L, rainbowHueColor(0f))
        assertEquals(0xFFFFFF00L, rainbowHueColor(60f))
        assertEquals(0xFF00FF00L, rainbowHueColor(120f))
        assertEquals(0xFF00FFFFL, rainbowHueColor(180f))
        assertEquals(0xFF0000FFL, rainbowHueColor(240f))
        assertEquals(0xFFFF00FFL, rainbowHueColor(300f))
        assertEquals(0xFFFF0000L, rainbowHueColor(360f))
    }
}
