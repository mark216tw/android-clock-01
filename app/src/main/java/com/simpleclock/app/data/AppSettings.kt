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
    RAINBOW(R.string.theme_rainbow),
}

enum class AppThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.follow_system),
    LIGHT(R.string.light_mode),
    DARK(R.string.dark_mode),
}

enum class ClockStyle(@StringRes val labelRes: Int) {
    BOLD(R.string.style_bold),
    THIN(R.string.style_thin),
    LED(R.string.style_led),
    LCD(R.string.style_lcd),
    NEON(R.string.style_neon),
    OUTLINE(R.string.style_outline),
    GLASS(R.string.style_glass),
    ANALOG_CLASSIC(R.string.style_analog_classic),
    ANALOG_MINIMAL(R.string.style_analog_minimal),
    SWISS_RAILWAY(R.string.style_swiss_railway),
}

enum class TimeFormat {
    SYSTEM,
    HOUR_12,
    HOUR_24,
}

data class AppSettings(
    val showSeconds: Boolean = true,
    val blinkColon: Boolean = false,
    val fullScreen: Boolean = false,
    val keepScreenOn: Boolean = false,
    val themeColor: AppThemeColor = AppThemeColor.SKY,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val clockStyle: ClockStyle = ClockStyle.BOLD,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
)
