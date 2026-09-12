package com.simpleclock.app.data

import androidx.annotation.StringRes
import com.simpleclock.app.R

enum class AppThemeColor(@StringRes val labelRes: Int) {
    CORAL(R.string.theme_coral),
    TANGERINE(R.string.theme_tangerine),
    SUNFLOWER(R.string.theme_sunflower),
    MINT(R.string.theme_mint),
    SKY(R.string.theme_sky),
    GRAPE(R.string.theme_grape),
    CUSTOM(R.string.custom_color),
}

enum class ClockBackgroundPattern(@StringRes val labelRes: Int) {
    NONE(R.string.pattern_none),
    RAINBOW(R.string.pattern_rainbow),
    AURORA(R.string.pattern_aurora),
    CONCENTRIC_GLOW(R.string.pattern_concentric_glow),
    FLUID_WAVES(R.string.pattern_fluid_waves),
    PRISM(R.string.pattern_prism),
    NEBULA(R.string.pattern_nebula),
    RADIAL_BEAMS(R.string.pattern_radial_beams),
    SOFT_CHECKER(R.string.pattern_soft_checker),
}

enum class ClockBackgroundColorMode(@StringRes val labelRes: Int) {
    SOLID(R.string.background_color_solid),
    COLORFUL(R.string.background_color_colorful),
}

val DEFAULT_RANDOM_RAINBOW_COLORS: List<Long> = listOf(
    0xFF8B5CF6L,
    0xFFEC4899L,
    0xFFEF4444L,
    0xFFF59E0BL,
    0xFF10B981L,
    0xFF06B6D4L,
)

fun generateRandomRainbowColors(): List<Long> {
    val random = java.util.Random()
    val baseHue = random.nextFloat() * 360f
    val colorCount = 6
    val step = 360f / colorCount
    return (0 until colorCount).map { i ->
        val hue = (baseHue + i * step + (random.nextFloat() * 20f - 10f) + 360f) % 360f
        val saturation = 0.75f + random.nextFloat() * 0.25f
        val value = 0.85f + random.nextFloat() * 0.15f
        hsvToColor(hue, saturation, value)
    }
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Long {
    val h = (hue % 360f + 360f) % 360f
    val c = value * saturation
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = value - c

    val (rPrime, gPrime, bPrime) = when ((h / 60f).toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((rPrime + m) * 255f).toInt().coerceIn(0, 255)
    val g = ((gPrime + m) * 255f).toInt().coerceIn(0, 255)
    val b = ((bPrime + m) * 255f).toInt().coerceIn(0, 255)

    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

enum class AppThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.follow_system),
    LIGHT(R.string.light_mode),
    DARK(R.string.dark_mode),
}

enum class ClockStyle(@StringRes val labelRes: Int) {
    BOLD(R.string.style_bold),
    THIN(R.string.style_thin),
    NEON(R.string.style_neon),
    AURA_GLOW(R.string.style_aura_glow),
    OUTLINE(R.string.style_outline),
    LED(R.string.style_led),
    GLASS(R.string.style_glass),
    NIXIE_TUBE(R.string.style_nixie_tube),
    ANALOG_CLASSIC(R.string.style_analog_classic),
    ANALOG_MINIMAL(R.string.style_analog_minimal),
    BAUHAUS_GEOMETRIC(R.string.style_bauhaus),
    SWISS_RAILWAY(R.string.style_swiss_railway),
    SQUARE_CLASSIC(R.string.style_square_classic),
    SQUARE_MINIMAL(R.string.style_square_minimal),
    SQUARE_RETRO(R.string.style_square_retro),
    SQUARE_ROMAN(R.string.style_square_roman),
}

enum class ClockStyleCategory(@StringRes val labelRes: Int) {
    DIGITAL(R.string.clock_style_category_digital),
    ROUND(R.string.clock_style_category_round),
    SQUARE(R.string.clock_style_category_square),
}

val ClockStyle.category: ClockStyleCategory
    get() = when (this) {
        ClockStyle.BOLD,
        ClockStyle.THIN,
        ClockStyle.NEON,
        ClockStyle.AURA_GLOW,
        ClockStyle.OUTLINE,
        ClockStyle.LED,
        ClockStyle.GLASS,
        ClockStyle.NIXIE_TUBE -> ClockStyleCategory.DIGITAL
        ClockStyle.ANALOG_CLASSIC,
        ClockStyle.ANALOG_MINIMAL,
        ClockStyle.BAUHAUS_GEOMETRIC,
        ClockStyle.SWISS_RAILWAY -> ClockStyleCategory.ROUND
        ClockStyle.SQUARE_CLASSIC,
        ClockStyle.SQUARE_MINIMAL,
        ClockStyle.SQUARE_RETRO,
        ClockStyle.SQUARE_ROMAN -> ClockStyleCategory.SQUARE
    }

val ClockStyle.isAnalog: Boolean
    get() = category != ClockStyleCategory.DIGITAL

val ClockStyle.isSquare: Boolean
    get() = category == ClockStyleCategory.SQUARE

enum class TimeFormat {
    SYSTEM,
    HOUR_12,
    HOUR_24,
}

enum class ScreenOrientation(@StringRes val labelRes: Int) {
    SYSTEM(R.string.orientation_system),
    PORTRAIT(R.string.orientation_portrait),
    LANDSCAPE(R.string.orientation_landscape),
}

data class AppSettings(
    val showSeconds: Boolean = true,
    val blinkColon: Boolean = false,
    val fullScreen: Boolean = false,
    val keepScreenOn: Boolean = false,
    val themeColor: AppThemeColor = AppThemeColor.SKY,
    val customThemeHue: Float = DEFAULT_SYSTEM_THEME_HUE,
    val clockBackgroundPattern: ClockBackgroundPattern = ClockBackgroundPattern.NONE,
    val clockBackgroundColorMode: ClockBackgroundColorMode = ClockBackgroundColorMode.SOLID,
    val clockBackgroundDynamic: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val systemThemeHue: Float = DEFAULT_SYSTEM_THEME_HUE,
    val customSystemThemeHue: Float = DEFAULT_SYSTEM_THEME_HUE,
    val useCustomSystemThemeColor: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.BOLD,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val screenOrientation: ScreenOrientation = ScreenOrientation.SYSTEM,
    val clockFontSizePortrait: Int = 3,
    val clockFontSizeLandscape: Int = 3,
    val randomRainbowColors: List<Long> = DEFAULT_RANDOM_RAINBOW_COLORS,
    val savedRainbowThemes: List<List<Long>> = emptyList(),
)

fun AppSettings.fontSizeScale(isPortrait: Boolean): Float {
    val level = (if (isPortrait) clockFontSizePortrait else clockFontSizeLandscape).coerceIn(1, 5)
    return when (level) {
        1 -> 0.70f
        2 -> 0.85f
        3 -> 1.00f
        4 -> 1.15f
        5 -> 1.30f
        else -> 1.00f
    }
}
