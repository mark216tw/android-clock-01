package com.simpleclock.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        val themeColorMotion = stringPreferencesKey("theme_color_motion")
        val themeMode = stringPreferencesKey("theme_mode")
        val clockStyle = stringPreferencesKey("clock_style")
        val timeFormat = stringPreferencesKey("time_format")
        val screenOrientation = stringPreferencesKey("screen_orientation")
        val clockFontSizePortrait = intPreferencesKey("clock_font_size_portrait")
        val clockFontSizeLandscape = intPreferencesKey("clock_font_size_landscape")
        val clockFontSizeLevelLegacy = intPreferencesKey("clock_font_size_level")
        val randomRainbowColors = stringPreferencesKey("random_rainbow_colors")
        val savedRainbowThemes = stringPreferencesKey("saved_rainbow_themes")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { values ->
        val legacyFontSize = values[Keys.clockFontSizeLevelLegacy]
        AppSettings(
            showSeconds = values[Keys.showSeconds] ?: true,
            blinkColon = values[Keys.blinkColon] ?: false,
            fullScreen = values[Keys.fullScreen] ?: false,
            keepScreenOn = values[Keys.keepScreenOn] ?: false,
            themeColor = values[Keys.themeColor].toEnumOrDefault(AppThemeColor.SKY),
            themeColorMotion = values[Keys.themeColorMotion].toThemeColorMotion(),
            themeMode = values[Keys.themeMode].toEnumOrDefault(AppThemeMode.SYSTEM),
            clockStyle = values[Keys.clockStyle].toEnumOrDefault(ClockStyle.BOLD),
            timeFormat = values[Keys.timeFormat].toEnumOrDefault(TimeFormat.SYSTEM),
            screenOrientation = values[Keys.screenOrientation].toEnumOrDefault(ScreenOrientation.SYSTEM),
            clockFontSizePortrait = (values[Keys.clockFontSizePortrait] ?: legacyFontSize ?: 3).coerceIn(1, 5),
            clockFontSizeLandscape = (values[Keys.clockFontSizeLandscape] ?: legacyFontSize ?: 3).coerceIn(1, 5),
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
            values[Keys.themeColorMotion] = settings.themeColorMotion.name
            values[Keys.themeMode] = settings.themeMode.name
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

internal fun String?.toThemeColorMotion(): ThemeColorMotion = when (this) {
    "DYNAMIC" -> ThemeColorMotion.RANDOM_DYNAMIC
    else -> toEnumOrDefault(ThemeColorMotion.STATIC)
}
