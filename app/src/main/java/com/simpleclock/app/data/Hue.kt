package com.simpleclock.app.data

import kotlin.math.abs
import kotlin.math.roundToInt

const val DEFAULT_SYSTEM_THEME_HUE = 200f
val THEME_HUE_PRESETS: List<Float> = listOf(350f, 25f, 50f, 150f, 200f, 270f)
val SYSTEM_THEME_HUE_PRESETS: List<Float> = listOf(340f, 24f, 52f, 155f, 200f, 275f)

fun normalizeHue(hue: Float, default: Float = DEFAULT_SYSTEM_THEME_HUE): Float {
    if (!hue.isFinite()) return default
    return ((hue % 360f) + 360f) % 360f
}

fun systemThemeHueOrDefault(hue: Float?): Float =
    hue?.takeIf(Float::isFinite)?.let(::normalizeHue) ?: DEFAULT_SYSTEM_THEME_HUE

fun hueDistance(first: Float, second: Float): Float {
    val difference = abs(normalizeHue(first) - normalizeHue(second))
    return minOf(difference, 360f - difference)
}

fun hsvToArgb(hue: Float, saturation: Float, value: Float): Long {
    val normalized = normalizeHue(hue)
    val chroma = value * saturation
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
    val match = value - chroma
    val red = ((redPrime + match) * 255f).roundToInt().coerceIn(0, 255)
    val green = ((greenPrime + match) * 255f).roundToInt().coerceIn(0, 255)
    val blue = ((bluePrime + match) * 255f).roundToInt().coerceIn(0, 255)
    return 0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
}

fun rainbowHueColor(hue: Float): Long = hsvToArgb(hue, saturation = 1f, value = 1f)
