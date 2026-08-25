package com.simpleclock.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        val themeMode = stringPreferencesKey("theme_mode")
        val clockStyle = stringPreferencesKey("clock_style")
        val timeFormat = stringPreferencesKey("time_format")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { values ->
        AppSettings(
            showSeconds = values[Keys.showSeconds] ?: true,
            blinkColon = values[Keys.blinkColon] ?: false,
            fullScreen = values[Keys.fullScreen] ?: false,
            keepScreenOn = values[Keys.keepScreenOn] ?: false,
            themeColor = values[Keys.themeColor].toEnumOrDefault(AppThemeColor.SKY),
            themeMode = values[Keys.themeMode].toEnumOrDefault(AppThemeMode.SYSTEM),
            clockStyle = values[Keys.clockStyle].toEnumOrDefault(ClockStyle.BOLD),
            timeFormat = values[Keys.timeFormat].toEnumOrDefault(TimeFormat.SYSTEM),
        )
    }

    suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { values ->
            values[Keys.showSeconds] = settings.showSeconds
            values[Keys.blinkColon] = settings.blinkColon
            values[Keys.fullScreen] = settings.fullScreen
            values[Keys.keepScreenOn] = settings.keepScreenOn
            values[Keys.themeColor] = settings.themeColor.name
            values[Keys.themeMode] = settings.themeMode.name
            values[Keys.clockStyle] = settings.clockStyle.name
            values[Keys.timeFormat] = settings.timeFormat.name
        }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default
