package com.simpleclock.app.ui.screens

import androidx.compose.ui.geometry.Offset
import com.simpleclock.app.data.ClockStyle
import com.simpleclock.app.data.ClockStyleCategory
import com.simpleclock.app.data.category
import com.simpleclock.app.data.isAnalog
import com.simpleclock.app.data.isSquare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockDisplayTest {
    @Test
    fun clockStylesUseExpectedCategories() {
        assertEquals(8, ClockStyle.entries.count { it.category == ClockStyleCategory.DIGITAL })
        assertEquals(4, ClockStyle.entries.count { it.category == ClockStyleCategory.ROUND })
        assertEquals(4, ClockStyle.entries.count { it.category == ClockStyleCategory.SQUARE })
        assertFalse(ClockStyle.LED.isAnalog)
        assertTrue(ClockStyle.ANALOG_CLASSIC.isAnalog)
        assertTrue(ClockStyle.SQUARE_CLASSIC.isAnalog)
        assertTrue(ClockStyle.SQUARE_ROMAN.isAnalog)
        assertTrue(ClockStyle.SQUARE_ROMAN.isSquare)
    }

    @Test
    fun rectangleClockPointsReachCardinalEdges() {
        val center = Offset(100f, 100f)

        assertOffsetEquals(Offset(100f, 52f), rectangleClockPoint(center, 40f, 48f, 0f))
        assertOffsetEquals(Offset(140f, 100f), rectangleClockPoint(center, 40f, 48f, 90f))
        assertOffsetEquals(Offset(100f, 148f), rectangleClockPoint(center, 40f, 48f, 180f))
        assertOffsetEquals(Offset(60f, 100f), rectangleClockPoint(center, 40f, 48f, 270f))
    }

    @Test
    fun rectangleClockDiagonalPointReachesNearestEdge() {
        assertOffsetEquals(
            Offset(140f, 60f),
            rectangleClockPoint(Offset(100f, 100f), 40f, 48f, 45f),
        )
    }

    @Test
    fun squareStylesUseFixedPortraitRectangleAspect() {
        assertEquals(1f / 1.2f, SQUARE_CLOCK_ASPECT, 0.001f)
        assertEquals(1.2f, 1f / SQUARE_CLOCK_ASPECT, 0.001f)
    }

    @Test
    fun ledUsesNormalizedVisualScale() {
        assertEquals(0.78f, LED_VISUAL_SCALE, 0.001f)
    }

    @Test
    fun analogClockKeepsSecondPrecisionWhenSecondHandIsHidden() {
        assertEquals(
            1_000L,
            clockUpdateIntervalMillis(showSeconds = false, blinkColon = false, isAnalog = true),
        )
        assertEquals(
            60_000L,
            clockUpdateIntervalMillis(showSeconds = false, blinkColon = false, isAnalog = false),
        )
    }

    @Test
    fun styleGridUsesOneColumnOnlyForVeryNarrowContent() {
        assertEquals(1, styleGridColumnCount(259f))
        assertEquals(2, styleGridColumnCount(260f))
        assertEquals(2, styleGridColumnCount(360f))
    }

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.001f)
        assertEquals(expected.y, actual.y, 0.001f)
    }
}
