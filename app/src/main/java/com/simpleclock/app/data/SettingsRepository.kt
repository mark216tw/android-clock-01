package com.simpleclock.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "clock_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val showSeconds = booleanPreferencesKey("show_seconds")
        val blinkColon = booleanPreferencesKey("blink_colon")
        val fullScreen = booleanPreferencesKey("full_screen")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val themeColor = stringPreferencesKey("theme_color")
        val customThemeHue = floatPreferencesKey("custom_theme_hue")
        val clockBackgroundPattern = stringPreferencesKey("clock_background_pattern")
        val clockBackgroundColorMode = stringPreferencesKey("clock_background_color_mode")
        val clockBackgroundDynamic = booleanPreferencesKey("clock_background_dynamic")
        val themeMode = stringPreferencesKey("theme_mode")
        val systemThemeHue = floatPreferencesKey("system_theme_hue")
        val customSystemThemeHue = floatPreferencesKey("custom_system_theme_hue")
        val useCustomSystemThemeColor = booleanPreferencesKey("use_custom_system_theme_color")
        val clockStyle = stringPreferencesKey("clock_style")
        val timeFormat = stringPreferencesKey("time_format")
        val screenOrientation = stringPreferencesKey("screen_orientation")
        val clockFontSizePortrait = intPreferencesKey("clock_font_size_portrait")
        val clockFontSizeLandscape = intPreferencesKey("clock_font_size_landscape")
        val randomRainbowColors = stringPreferencesKey("random_rainbow_colors")
        val savedRainbowThemes = stringPreferencesKey("saved_rainbow_themes")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { values ->
        val storedSystemHue = systemThemeHueOrDefault(values[Keys.systemThemeHue])
        AppSettings(
            showSeconds = values[Keys.showSeconds] ?: true,
            blinkColon = values[Keys.blinkColon] ?: false,
            fullScreen = values[Keys.fullScreen] ?: false,
            keepScreenOn = values[Keys.keepScreenOn] ?: false,
            themeColor = values[Keys.themeColor].toEnumOrDefault(AppThemeColor.SKY),
            customThemeHue = systemThemeHueOrDefault(values[Keys.customThemeHue]),
            clockBackgroundPattern = values[Keys.clockBackgroundPattern]
                .toEnumOrDefault(ClockBackgroundPattern.NONE),
            clockBackgroundColorMode = values[Keys.clockBackgroundColorMode]
                .toEnumOrDefault(ClockBackgroundColorMode.SOLID),
            clockBackgroundDynamic = values[Keys.clockBackgroundDynamic] ?: false,
            themeMode = values[Keys.themeMode].toEnumOrDefault(AppThemeMode.SYSTEM),
            systemThemeHue = storedSystemHue,
            customSystemThemeHue = systemThemeHueOrDefault(
                values[Keys.customSystemThemeHue] ?: storedSystemHue,
            ),
            useCustomSystemThemeColor = values[Keys.useCustomSystemThemeColor] ?: false,
            clockStyle = values[Keys.clockStyle].toEnumOrDefault(ClockStyle.BOLD),
            timeFormat = values[Keys.timeFormat].toEnumOrDefault(TimeFormat.SYSTEM),
            screenOrientation = values[Keys.screenOrientation].toEnumOrDefault(ScreenOrientation.SYSTEM),
            clockFontSizePortrait = (values[Keys.clockFontSizePortrait] ?: 3).coerceIn(1, 5),
            clockFontSizeLandscape = (values[Keys.clockFontSizeLandscape] ?: 3).coerceIn(1, 5),
            randomRainbowColors = values[Keys.randomRainbowColors]?.let { encoded ->
                encoded.split(',').mapNotNull { it.trim().toLongOrNull() }.takeIf { it.size >= 2 }
            } ?: DEFAULT_RANDOM_RAINBOW_COLORS,
            savedRainbowThemes = values[Keys.savedRainbowThemes]?.let { encoded ->
                encoded.split(';').mapNotNull { themeStr ->
                    themeStr.split(',').mapNotNull { it.trim().toLongOrNull() }.takeIf { it.size >= 2 }
                }
            } ?: emptyList(),
        )
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { values ->
            values[Keys.showSeconds] = settings.showSeconds
            values[Keys.blinkColon] = settings.blinkColon
            values[Keys.fullScreen] = settings.fullScreen
            values[Keys.keepScreenOn] = settings.keepScreenOn
            values[Keys.themeColor] = settings.themeColor.name
            values[Keys.customThemeHue] = normalizeHue(settings.customThemeHue)
            values[Keys.clockBackgroundPattern] = settings.clockBackgroundPattern.name
            values[Keys.clockBackgroundColorMode] = settings.clockBackgroundColorMode.name
            values[Keys.clockBackgroundDynamic] = settings.clockBackgroundDynamic
            values[Keys.themeMode] = settings.themeMode.name
            values[Keys.systemThemeHue] = normalizeHue(settings.systemThemeHue)
            values[Keys.customSystemThemeHue] = normalizeHue(settings.customSystemThemeHue)
            values[Keys.useCustomSystemThemeColor] = settings.useCustomSystemThemeColor
            values[Keys.clockStyle] = settings.clockStyle.name
            values[Keys.timeFormat] = settings.timeFormat.name
            values[Keys.screenOrientation] = settings.screenOrientation.name
            values[Keys.clockFontSizePortrait] = settings.clockFontSizePortrait.coerceIn(1, 5)
            values[Keys.clockFontSizeLandscape] = settings.clockFontSizeLandscape.coerceIn(1, 5)
            values[Keys.randomRainbowColors] = settings.randomRainbowColors.joinToString(",")
            values[Keys.savedRainbowThemes] = settings.savedRainbowThemes.joinToString(";") { theme ->
                theme.joinToString(",")
            }
        }
    }

}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
