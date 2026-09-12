package com.simpleclock.app.ui.screens

import androidx.compose.ui.graphics.Color
import com.simpleclock.app.data.AppSettings
import com.simpleclock.app.data.ClockBackgroundColorMode
import com.simpleclock.app.data.ClockBackgroundPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeMotionBackgroundTest {
    @Test
    fun backgroundDefaultsArePlainSolidAndStatic() {
        val settings = AppSettings()

        assertEquals(ClockBackgroundPattern.NONE, settings.clockBackgroundPattern)
        assertEquals(ClockBackgroundColorMode.SOLID, settings.clockBackgroundColorMode)
        assertEquals(false, settings.clockBackgroundDynamic)
    }

    @Test
    fun backgroundPatternsUseRequestedOrder() {
        assertEquals(
            listOf(
                ClockBackgroundPattern.NONE,
                ClockBackgroundPattern.RAINBOW,
                ClockBackgroundPattern.AURORA,
                ClockBackgroundPattern.CONCENTRIC_GLOW,
                ClockBackgroundPattern.FLUID_WAVES,
                ClockBackgroundPattern.PRISM,
                ClockBackgroundPattern.NEBULA,
                ClockBackgroundPattern.RADIAL_BEAMS,
                ClockBackgroundPattern.SOFT_CHECKER,
            ),
            ClockBackgroundPattern.entries,
        )
    }

    @Test
    fun backgroundColorModesUseRequestedOrder() {
        assertEquals(
            listOf(ClockBackgroundColorMode.SOLID, ClockBackgroundColorMode.COLORFUL),
            ClockBackgroundColorMode.entries,
        )
    }

    @Test
    fun rainbowPaletteKeepsEveryConfiguredColorInOrder() {
        val rainbowColors = listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
        )

        val palette = buildThemeMotionPalette(
            isColorful = true,
            rainbowColors = rainbowColors,
            background = Color.White,
            surface = Color.LightGray,
            primary = Color.Blue,
        )

        assertEquals(rainbowColors, palette.accents)
        assertEquals(rainbowColors, palette.gradient)
    }

    @Test
    fun fixedThemePaletteUsesThemeColorsAndReturnsToBackground() {
        val background = Color(0xFFF2FAFF)
        val surface = Color(0xFFDCEFFC)
        val primary = Color(0xFF006493)

        val palette = buildThemeMotionPalette(
            isColorful = false,
            rainbowColors = emptyList(),
            background = background,
            surface = surface,
            primary = primary,
        )

        assertEquals(background, palette.base)
        assertEquals(background, palette.gradient.first())
        assertEquals(background, palette.gradient.last())
        assertTrue(surface in palette.gradient)
        assertEquals(4, palette.accents.size)
    }
}
