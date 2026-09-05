package com.simpleclock.app.ui.screens

import androidx.compose.ui.graphics.Color
import com.simpleclock.app.ForegroundThemeMotionSelector
import com.simpleclock.app.nextRandomThemeMotion
import com.simpleclock.app.data.ThemeColorMotion
import com.simpleclock.app.data.toThemeColorMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ThemeMotionBackgroundTest {
    @Test
    fun randomMotionNeverImmediatelyRepeats() {
        val random = Random(216)
        val concreteMotions = ThemeColorMotion.entries.filterNot {
            it == ThemeColorMotion.STATIC || it == ThemeColorMotion.RANDOM_DYNAMIC
        }

        concreteMotions.forEach { current ->
            repeat(100) {
                assertNotEquals(current, nextRandomThemeMotion(current, random))
            }
        }
    }

    @Test
    fun randomMotionCanSelectEveryDynamicEffect() {
        val random = Random(216)
        val selected = buildSet {
            repeat(1_000) {
                add(nextRandomThemeMotion(random = random))
            }
        }
        val expected = ThemeColorMotion.entries.filterNot {
            it == ThemeColorMotion.STATIC || it == ThemeColorMotion.RANDOM_DYNAMIC
        }.toSet()

        assertEquals(expected, selected)
    }

    @Test
    fun foregroundSelectorChangesOnlyAfterBackgrounding() {
        val selector = ForegroundThemeMotionSelector(Random(216))
        val initial = selector.current

        assertEquals(initial, selector.onForegrounded())
        assertEquals(initial, selector.onForegrounded())

        selector.onBackgrounded()
        val resumed = selector.onForegrounded()

        assertNotEquals(initial, resumed)
        assertEquals(resumed, selector.onForegrounded())
    }

    @Test
    fun themeMotionOptionsUseRequestedOrder() {
        assertEquals(
            listOf(
                ThemeColorMotion.STATIC,
                ThemeColorMotion.FLOWING_GRADIENT,
                ThemeColorMotion.FLOATING_AURORA,
                ThemeColorMotion.ROTATING_GLOW,
                ThemeColorMotion.EXPANDING_RIPPLES,
                ThemeColorMotion.FLOATING_BOKEH,
                ThemeColorMotion.RANDOM_DYNAMIC,
            ),
            ThemeColorMotion.entries,
        )
    }

    @Test
    fun legacyDynamicSettingBecomesRandomDynamic() {
        assertEquals(ThemeColorMotion.RANDOM_DYNAMIC, "DYNAMIC".toThemeColorMotion())
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
            isRainbow = true,
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
            isRainbow = false,
            rainbowColors = emptyList(),
            background = background,
            surface = surface,
            primary = primary,
        )

        assertEquals(background, palette.base)
        assertEquals(background, palette.gradient.first())
        assertEquals(background, palette.gradient.last())
        assertTrue(surface in palette.gradient)
        assertEquals(3, palette.accents.size)
    }
}
