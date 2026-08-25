package com.simpleclock.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simpleclock.app.data.AlarmEntity
import com.simpleclock.app.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination {
    CLOCK,
    ALARMS,
    SETTINGS,
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SimpleClockApplication
    private val alarmDao = app.database.alarmDao()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _destination = MutableStateFlow(AppDestination.CLOCK)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    val alarms = alarmDao.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        viewModelScope.launch {
            app.settingsRepository.settings.collect { _settings.value = it }
        }
    }

    fun navigate(destination: AppDestination) {
        _destination.value = destination
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        viewModelScope.launch { app.settingsRepository.save(updated) }
    }

    fun saveAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val saved = if (alarm.id == 0L) {
                alarm.copy(id = alarmDao.insert(alarm))
            } else {
                alarmDao.update(alarm)
                alarm
            }
            app.alarmScheduler.cancel(saved.id)
            if (saved.enabled) app.alarmScheduler.schedule(saved)
        }
    }

    fun setAlarmEnabled(alarm: AlarmEntity, enabled: Boolean) {
        saveAlarm(alarm.copy(enabled = enabled))
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            app.alarmScheduler.cancel(alarm.id)
            alarmDao.delete(alarm)
        }
    }

    fun reconcileAlarmCapabilities(canDeliverAlarms: Boolean) {
        viewModelScope.launch {
            alarmDao.getEnabled().forEach { alarm ->
                val scheduled = canDeliverAlarms && app.alarmScheduler.schedule(alarm)
                if (!scheduled) {
                    app.alarmScheduler.cancel(alarm.id)
                    alarmDao.update(alarm.copy(enabled = false))
                }
            }
        }
    }
}
