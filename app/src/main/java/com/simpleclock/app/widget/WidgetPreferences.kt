package com.simpleclock.app.widget

import android.content.Context
import androidx.annotation.StringRes
import com.simpleclock.app.R
import com.simpleclock.app.data.AppThemeMode

internal enum class WidgetThemeColor(@StringRes val labelRes: Int) {
    CORAL(R.string.theme_coral),
    TANGERINE(R.string.theme_tangerine),
    SUNFLOWER(R.string.theme_sunflower),
    MINT(R.string.theme_mint),
    SKY(R.string.theme_sky),
    GRAPE(R.string.theme_grape),
    RAINBOW(R.string.widget_theme_rainbow),
    BLACK(R.string.widget_theme_black),
    WHITE(R.string.widget_theme_white),
}

internal data class WidgetSettings(
    val color: WidgetThemeColor = WidgetThemeColor.SKY,
    val mode: AppThemeMode = AppThemeMode.SYSTEM,
    val transparentBackground: Boolean = false,
    val timeSizeSp: Int = DEFAULT_TIME_SIZE_SP,
)

private const val DEFAULT_TIME_SIZE_SP = 40

internal object WidgetPreferences {
    private const val PREFERENCES_NAME = "clock_widget_preferences"

    fun load(context: Context, appWidgetId: Int): WidgetSettings {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return WidgetSettings(
            color = preferences.getString(colorKey(appWidgetId), null)
                ?.let { name -> WidgetThemeColor.entries.find { it.name == name } }
                ?: WidgetThemeColor.SKY,
            mode = preferences.getString(modeKey(appWidgetId), null)
                ?.let { name -> AppThemeMode.entries.find { it.name == name } }
                ?: AppThemeMode.SYSTEM,
            transparentBackground = preferences.getBoolean(
                transparentBackgroundKey(appWidgetId),
                false,
            ),
            timeSizeSp = if (preferences.contains(timeSizeSpKey(appWidgetId))) {
                preferences.getInt(timeSizeSpKey(appWidgetId), DEFAULT_TIME_SIZE_SP)
                    .coerceAtLeast(1)
            } else {
                val oldPercent = preferences.getInt(timeSizePercentKey(appWidgetId), 100)
                    .coerceIn(70, 100)
                (DEFAULT_TIME_SIZE_SP * oldPercent / 100f).toInt().coerceAtLeast(1)
            },
        )
    }

    fun save(context: Context, appWidgetId: Int, settings: WidgetSettings) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(colorKey(appWidgetId), settings.color.name)
            .putString(modeKey(appWidgetId), settings.mode.name)
            .putBoolean(
                transparentBackgroundKey(appWidgetId),
                settings.transparentBackground,
            )
            .putInt(timeSizeSpKey(appWidgetId), settings.timeSizeSp.coerceAtLeast(1))
            .remove(timeSizePercentKey(appWidgetId))
            .apply()
    }

    fun delete(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(colorKey(appWidgetId))
            .remove(modeKey(appWidgetId))
            .remove(transparentBackgroundKey(appWidgetId))
            .remove(timeSizeSpKey(appWidgetId))
            .remove(timeSizePercentKey(appWidgetId))
            .apply()
    }

    private fun colorKey(appWidgetId: Int) = "widget_${appWidgetId}_color"

    private fun modeKey(appWidgetId: Int) = "widget_${appWidgetId}_mode"

    private fun transparentBackgroundKey(appWidgetId: Int) =
        "widget_${appWidgetId}_transparent_background"

    private fun timeSizePercentKey(appWidgetId: Int) = "widget_${appWidgetId}_time_size_percent"

    private fun timeSizeSpKey(appWidgetId: Int) = "widget_${appWidgetId}_time_size_sp"
}
