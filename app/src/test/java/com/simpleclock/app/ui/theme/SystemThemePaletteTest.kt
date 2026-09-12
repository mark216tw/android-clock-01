package com.simpleclock.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class SystemThemePaletteTest {
    @Test
    fun materialContentPairsRemainReadableForEveryHue() {
        (0 until 360).forEach { hue ->
            listOf(false, true).forEach { dark ->
                val palette = systemThemePalette(hue.toFloat(), dark)
                val pairs = listOf(
                    palette.primary to palette.onPrimary,
                    palette.primaryContainer to palette.onPrimaryContainer,
                    palette.secondary to palette.onSecondary,
                    palette.secondaryContainer to palette.onSecondaryContainer,
                    palette.tertiary to palette.onTertiary,
                    palette.tertiaryContainer to palette.onTertiaryContainer,
                    palette.error to palette.onError,
                    palette.errorContainer to palette.onErrorContainer,
                    palette.background to palette.onBackground,
                    palette.surface to palette.onSurface,
                    palette.surfaceVariant to palette.onSurfaceVariant,
                    palette.inverseSurface to palette.inverseOnSurface,
                )

                pairs.forEach { (background, foreground) ->
                    assertTrue(
                        "Insufficient contrast for hue=$hue dark=$dark: ${contrast(background, foreground)}",
                        contrast(background, foreground) >= 4.5,
                    )
                }
            }
        }
    }

    private fun contrast(first: Long, second: Long): Double {
        val lighter = maxOf(luminance(first), luminance(second))
        val darker = minOf(luminance(first), luminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(color: Long): Double {
        fun channel(shift: Int): Double {
            val value = ((color shr shift) and 0xFF).toDouble() / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
