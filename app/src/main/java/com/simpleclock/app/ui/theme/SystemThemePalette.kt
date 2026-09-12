package com.simpleclock.app.ui.theme

import com.simpleclock.app.data.normalizeHue
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

data class SystemThemePalette(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val error: Long,
    val onError: Long,
    val errorContainer: Long,
    val onErrorContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val surfaceDim: Long,
    val surfaceBright: Long,
    val surfaceContainerLowest: Long,
    val surfaceContainerLow: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val surfaceContainerHighest: Long,
    val outline: Long,
    val outlineVariant: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val inversePrimary: Long,
)

fun systemThemePalette(hue: Float, dark: Boolean): SystemThemePalette {
    val normalized = normalizeHue(hue)
    val secondaryHue = normalized + 28f
    val tertiaryHue = normalized - 55f
    val primary = hslToArgb(normalized, 0.72f, if (dark) 0.76f else 0.30f)
    val primaryContainer = hslToArgb(normalized, if (dark) 0.58f else 0.72f, if (dark) 0.27f else 0.90f)
    val secondary = hslToArgb(secondaryHue, 0.42f, if (dark) 0.74f else 0.32f)
    val secondaryContainer = hslToArgb(secondaryHue, if (dark) 0.34f else 0.45f, if (dark) 0.27f else 0.89f)
    val tertiary = hslToArgb(tertiaryHue, 0.52f, if (dark) 0.76f else 0.32f)
    val tertiaryContainer = hslToArgb(tertiaryHue, if (dark) 0.42f else 0.58f, if (dark) 0.27f else 0.90f)
    return if (dark) {
        SystemThemePalette(
            primary = primary,
            onPrimary = contrastingForeground(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contrastingForeground(primaryContainer),
            secondary = secondary,
            onSecondary = contrastingForeground(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contrastingForeground(secondaryContainer),
            tertiary = tertiary,
            onTertiary = contrastingForeground(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contrastingForeground(tertiaryContainer),
            error = 0xFFFFB4ABL,
            onError = 0xFF690005L,
            errorContainer = 0xFF93000AL,
            onErrorContainer = 0xFFFFDAD6L,
            background = hslToArgb(normalized, 0.16f, 0.075f),
            onBackground = hslToArgb(normalized, 0.16f, 0.91f),
            surface = hslToArgb(normalized, 0.14f, 0.09f),
            onSurface = hslToArgb(normalized, 0.14f, 0.91f),
            surfaceVariant = hslToArgb(normalized, 0.18f, 0.18f),
            onSurfaceVariant = hslToArgb(normalized, 0.14f, 0.78f),
            surfaceDim = hslToArgb(normalized, 0.14f, 0.07f),
            surfaceBright = hslToArgb(normalized, 0.14f, 0.24f),
            surfaceContainerLowest = hslToArgb(normalized, 0.14f, 0.05f),
            surfaceContainerLow = hslToArgb(normalized, 0.14f, 0.11f),
            surfaceContainer = hslToArgb(normalized, 0.14f, 0.13f),
            surfaceContainerHigh = hslToArgb(normalized, 0.14f, 0.16f),
            surfaceContainerHighest = hslToArgb(normalized, 0.14f, 0.19f),
            outline = hslToArgb(normalized, 0.10f, 0.62f),
            outlineVariant = hslToArgb(normalized, 0.12f, 0.32f),
            inverseSurface = hslToArgb(normalized, 0.12f, 0.91f),
            inverseOnSurface = hslToArgb(normalized, 0.14f, 0.19f),
            inversePrimary = hslToArgb(normalized, 0.68f, 0.34f),
        )
    } else {
        SystemThemePalette(
            primary = primary,
            onPrimary = contrastingForeground(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contrastingForeground(primaryContainer),
            secondary = secondary,
            onSecondary = contrastingForeground(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contrastingForeground(secondaryContainer),
            tertiary = tertiary,
            onTertiary = contrastingForeground(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contrastingForeground(tertiaryContainer),
            error = 0xFFBA1A1AL,
            onError = 0xFFFFFFFFL,
            errorContainer = 0xFFFFDAD6L,
            onErrorContainer = 0xFF410002L,
            background = hslToArgb(normalized, 0.18f, 0.98f),
            onBackground = hslToArgb(normalized, 0.16f, 0.12f),
            surface = hslToArgb(normalized, 0.14f, 0.99f),
            onSurface = hslToArgb(normalized, 0.14f, 0.12f),
            surfaceVariant = hslToArgb(normalized, 0.22f, 0.91f),
            onSurfaceVariant = hslToArgb(normalized, 0.13f, 0.32f),
            surfaceDim = hslToArgb(normalized, 0.14f, 0.87f),
            surfaceBright = hslToArgb(normalized, 0.14f, 0.99f),
            surfaceContainerLowest = 0xFFFFFFFFL,
            surfaceContainerLow = hslToArgb(normalized, 0.14f, 0.96f),
            surfaceContainer = hslToArgb(normalized, 0.14f, 0.94f),
            surfaceContainerHigh = hslToArgb(normalized, 0.14f, 0.92f),
            surfaceContainerHighest = hslToArgb(normalized, 0.14f, 0.90f),
            outline = hslToArgb(normalized, 0.10f, 0.46f),
            outlineVariant = hslToArgb(normalized, 0.14f, 0.78f),
            inverseSurface = hslToArgb(normalized, 0.13f, 0.19f),
            inverseOnSurface = hslToArgb(normalized, 0.14f, 0.95f),
            inversePrimary = hslToArgb(normalized, 0.70f, 0.76f),
        )
    }
}

private fun contrastingForeground(background: Long): Long =
    if (contrast(background, 0xFFFFFFFFL) >= contrast(background, 0xFF000000L)) {
        0xFFFFFFFFL
    } else {
        0xFF000000L
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

private fun hslToArgb(hue: Float, saturation: Float, lightness: Float): Long {
    val normalized = normalizeHue(hue)
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val section = normalized / 60f
    val x = chroma * (1f - abs(section % 2f - 1f))
    val (redPrime, greenPrime, bluePrime) = when (section.toInt()) {
        0 -> Triple(chroma, x, 0f)
        1 -> Triple(x, chroma, 0f)
        2 -> Triple(0f, chroma, x)
        3 -> Triple(0f, x, chroma)
        4 -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    val match = lightness - chroma / 2f
    val red = ((redPrime + match) * 255f).roundToInt().coerceIn(0, 255)
    val green = ((greenPrime + match) * 255f).roundToInt().coerceIn(0, 255)
    val blue = ((bluePrime + match) * 255f).roundToInt().coerceIn(0, 255)
    return 0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
}
