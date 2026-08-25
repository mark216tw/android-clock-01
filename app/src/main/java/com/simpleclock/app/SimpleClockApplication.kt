package com.simpleclock.app

import android.app.Application
import android.content.res.Configuration
import com.simpleclock.app.alarm.AlarmScheduler
import com.simpleclock.app.data.AppDatabase
import com.simpleclock.app.data.SettingsRepository
import com.simpleclock.app.widget.ClockWidgetProvider

class SimpleClockApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(this) }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ClockWidgetProvider.updateAllWidgets(this)
    }
}
