package com.simpleclock.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.provider.AlarmClock
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.TypedValue
import android.widget.RemoteViews
import com.simpleclock.app.R
import com.simpleclock.app.data.AppThemeMode
import kotlin.math.min

class ClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId -> WidgetPreferences.delete(context, appWidgetId) }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in REFRESH_ACTIONS) {
            updateAllWidgets(context)
        }
    }

    companion object {
        private val REFRESH_ACTIONS = setOf(
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )

        internal fun updateWidget(context: Context, appWidgetId: Int) {
            updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
        }

        internal fun nextAlarmLabel(context: Context): String {
            val nextAlarm = context.getSystemService(AlarmManager::class.java).nextAlarmClock
                ?: return context.getString(R.string.no_alarm)
            return DateUtils.formatDateTime(
                context,
                nextAlarm.triggerTime,
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_TIME,
            ).replace("星期", "週")
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val settings = WidgetPreferences.load(context, appWidgetId)
            val isDark = when (settings.mode) {
                AppThemeMode.SYSTEM -> (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val palette = paletteFor(settings.color, isDark)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40)
            val widthBasedDetailSize = when {
                minWidth >= 380 -> 19f
                minWidth >= 320 -> 18f
                minWidth >= 280 -> 17f
                else -> 16f
            }
            val heightBasedDetailSize = when {
                minHeight >= 64 -> 19f
                minHeight >= 56 -> 18f
                minHeight >= 48 -> 17f
                else -> 16f
            }
            val detailSize = min(widthBasedDetailSize, heightBasedDetailSize)
            val fontScale = context.resources.configuration.fontScale.coerceAtLeast(1f)
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val nextAlarm = alarmManager.nextAlarmClock
            val alarmText = nextAlarmLabel(context)
            val locale = context.resources.configuration.locales[0]
            val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEE MMM d")

            val views = RemoteViews(context.packageName, R.layout.clock_widget).apply {
                val primaryText = when {
                    settings.transparentBackground && settings.color == WidgetThemeColor.BLACK ->
                        0xFF000000.toInt()
                    settings.transparentBackground && settings.color == WidgetThemeColor.WHITE ->
                        0xFFFFFFFF.toInt()
                    else -> palette.primaryText
                }
                val secondaryText = when {
                    settings.transparentBackground && settings.color == WidgetThemeColor.BLACK ->
                        0xFF000000.toInt()
                    settings.transparentBackground && settings.color == WidgetThemeColor.WHITE ->
                        0xFFFFFFFF.toInt()
                    else -> palette.secondaryText
                }
                setImageViewResource(
                    R.id.widget_background,
                    if (settings.transparentBackground) {
                        R.drawable.widget_bg_transparent
                    } else {
                        palette.backgroundRes
                    },
                )
                setTextColor(R.id.widget_time, primaryText)
                setTextColor(R.id.widget_date, secondaryText)
                setTextColor(R.id.widget_alarm, primaryText)
                setInt(R.id.widget_alarm_icon, "setColorFilter", primaryText)
                setTextViewTextSize(
                    R.id.widget_time,
                    TypedValue.COMPLEX_UNIT_SP,
                    settings.timeSizeSp.toFloat(),
                )
                setTextViewTextSize(
                    R.id.widget_date,
                    TypedValue.COMPLEX_UNIT_SP,
                    detailSize / fontScale,
                )
                setTextViewTextSize(
                    R.id.widget_alarm,
                    TypedValue.COMPLEX_UNIT_SP,
                    detailSize / fontScale,
                )
                setCharSequence(R.id.widget_date, "setFormat12Hour", datePattern)
                setCharSequence(R.id.widget_date, "setFormat24Hour", datePattern)
                setTextViewText(R.id.widget_alarm, alarmText)
                setContentDescription(
                    R.id.widget_alarm,
                    context.getString(R.string.next_alarm) + ": " + alarmText,
                )
                setOnClickPendingIntent(
                    R.id.widget_time,
                    mainActivityPendingIntent(context, appWidgetId),
                )
                setOnClickPendingIntent(
                    R.id.widget_alarm_container,
                    nextAlarm?.showIntent ?: showAlarmsPendingIntent(context, appWidgetId),
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        internal fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ClockWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { appWidgetId ->
                updateWidget(context, manager, appWidgetId)
            }
        }

        private fun mainActivityPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(Intent.ACTION_MAIN)
                .setClassName(context.packageName, "${context.packageName}.MainActivity")
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun showAlarmsPendingIntent(context: Context, appWidgetId: Int): PendingIntent =
            PendingIntent.getActivity(
                context,
                appWidgetId xor 0x40000000,
                Intent(AlarmClock.ACTION_SHOW_ALARMS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun paletteFor(color: WidgetThemeColor, isDark: Boolean): WidgetPalette {
            return when (color) {
                WidgetThemeColor.CORAL -> if (isDark) {
                    WidgetPalette(R.drawable.widget_bg_coral_dark, 0xFFFFF5F4.toInt(), 0xFFE8B6B1.toInt())
                } else {
                    WidgetPalette(R.drawable.widget_bg_coral_light, 0xFF7D2822.toInt(), 0xFF79504C.toInt())
                }
                WidgetThemeColor.TANGERINE -> if (isDark) {
                    WidgetPalette(R.drawable.widget_bg_tangerine_dark, 0xFFFFF6ED.toInt(), 0xFFE9C19E.toInt())
                } else {
                    WidgetPalette(R.drawable.widget_bg_tangerine_light, 0xFF713B0D.toInt(), 0xFF75593E.toInt())
                }
                WidgetThemeColor.SUNFLOWER -> if (isDark) {
                    WidgetPalette(R.drawable.widget_bg_sunflower_dark, 0xFFFFF9DF.toInt(), 0xFFE6D79A.toInt())
                } else {
                    WidgetPalette(R.drawable.widget_bg_sunflower_light, 0xFF5F4B00.toInt(), 0xFF6F6540.toInt())
                }
                WidgetThemeColor.MINT -> if (isDark) {
                    WidgetPalette(R.drawable.widget_bg_mint_dark, 0xFFEFFFF8.toInt(), 0xFFA9DCC7.toInt())
                } else {
                    WidgetPalette(R.drawable.widget_bg_mint_light, 0xFF165B43.toInt(), 0xFF496B5E.toInt())
                }
                WidgetThemeColor.SKY -> if (isDark) {
                    WidgetPalette(R.drawable.widget_bg_sky_dark, 0xFFF0F8FF.toInt(), 0xFFAFCFE9.toInt())
                } else {
                    WidgetPalette(R.drawable.widget_bg_sky_light, 0xFF19547D.toInt(), 0xFF4A667A.toInt())
                }
                WidgetThemeColor.GRAPE -> if (isDark) {
                    WidgetPalette(R.drawable.widget_bg_grape_dark, 0xFFFBF5FF.toInt(), 0xFFD5B7E5.toInt())
                } else {
                    WidgetPalette(R.drawable.widget_bg_grape_light, 0xFF60317A.toInt(), 0xFF6C5578.toInt())
                }
                WidgetThemeColor.RAINBOW -> WidgetPalette(
                    R.drawable.widget_bg_rainbow,
                    0xFFFFFFFF.toInt(),
                    0xFFF7F2FF.toInt(),
                )
                WidgetThemeColor.BLACK -> WidgetPalette(
                    R.drawable.widget_bg_black,
                    0xFFFFFFFF.toInt(),
                    0xFFFFFFFF.toInt(),
                )
                WidgetThemeColor.WHITE -> WidgetPalette(
                    R.drawable.widget_bg_white,
                    0xFF000000.toInt(),
                    0xFF000000.toInt(),
                )
            }
        }

    }
}

private data class WidgetPalette(
    val backgroundRes: Int,
    val primaryText: Int,
    val secondaryText: Int,
)
