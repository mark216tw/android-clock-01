package com.simpleclock.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simpleclock.app.data.AlarmEntity
import com.simpleclock.app.data.AppSettings
import com.simpleclock.app.data.normalizeHue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

enum class AppDestination {
    CLOCK,
    ALARMS,
    SETTINGS,
}

sealed interface AlarmSaveState {
    data object Idle : AlarmSaveState
    data class RequestingPermission(val draft: AlarmEntity) : AlarmSaveState
    data class Saving(val draft: AlarmEntity) : AlarmSaveState
    data class Success(val alarm: AlarmEntity, val triggerAtMillis: Long?) : AlarmSaveState
    data class Error(val draft: AlarmEntity, val reason: AlarmSaveError) : AlarmSaveState
}

data class AlarmEnableSuccess(val alarmId: Long, val triggerAtMillis: Long)

enum class AlarmSaveError {
    PERMISSION_DENIED,
    SCHEDULE_FAILED,
    SAVE_FAILED,
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SimpleClockApplication
    private val alarmDao = app.database.alarmDao()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _systemThemeHuePreview = MutableStateFlow<Float?>(null)
    val systemThemeHuePreview: StateFlow<Float?> = _systemThemeHuePreview.asStateFlow()

    private val _destination = MutableStateFlow(AppDestination.CLOCK)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _alarmSaveState = MutableStateFlow<AlarmSaveState>(AlarmSaveState.Idle)
    val alarmSaveState: StateFlow<AlarmSaveState> = _alarmSaveState.asStateFlow()

    private val _alarmEnableRequest = MutableStateFlow<AlarmEntity?>(null)
    val alarmEnableRequest: StateFlow<AlarmEntity?> = _alarmEnableRequest.asStateFlow()

    private val _alarmEnableSuccess = MutableStateFlow<AlarmEnableSuccess?>(null)
    val alarmEnableSuccess: StateFlow<AlarmEnableSuccess?> = _alarmEnableSuccess.asStateFlow()

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

    fun requestSaveAlarm(alarm: AlarmEntity) {
        if (alarm.enabled) {
            _alarmSaveState.value = AlarmSaveState.RequestingPermission(alarm)
        } else {
            persistAlarm(alarm)
        }
    }

    fun onAlarmAuthorizationResult(result: AlarmAuthorizationResult) {
        val draft = (_alarmSaveState.value as? AlarmSaveState.RequestingPermission)?.draft ?: return
        when (result) {
            AlarmAuthorizationResult.Granted -> persistAlarm(draft)
            is AlarmAuthorizationResult.Denied -> {
                _alarmSaveState.value = AlarmSaveState.Error(draft, AlarmSaveError.PERMISSION_DENIED)
            }
        }
    }

    fun consumeAlarmSaveResult() {
        if (_alarmSaveState.value !is AlarmSaveState.Saving) {
            _alarmSaveState.value = AlarmSaveState.Idle
        }
    }

    private fun persistAlarm(alarm: AlarmEntity) {
        _alarmSaveState.value = AlarmSaveState.Saving(alarm)
        viewModelScope.launch {
            var original: AlarmEntity? = null
            var saved: AlarmEntity? = null
            try {
                original = alarm.takeIf { it.id != 0L }?.let { alarmDao.getById(it.id) }
                saved = if (alarm.id == 0L) {
                    val ordered = alarm.copy(sortOrder = alarmDao.nextSortOrder())
                    ordered.copy(id = alarmDao.insert(ordered))
                } else {
                    app.alarmScheduler.update(alarm)
                    alarm
                }

                if (!saved.enabled) {
                    app.alarmScheduler.cancel(saved.id)
                    _alarmSaveState.value = AlarmSaveState.Success(saved, null)
                    return@launch
                }

                val triggerAt = app.alarmScheduler.scheduleWithResult(saved)
                if (triggerAt == null) {
                    rollbackAlarmSave(saved, original)
                    _alarmSaveState.value = AlarmSaveState.Error(alarm, AlarmSaveError.SCHEDULE_FAILED)
                } else {
                    _alarmSaveState.value = AlarmSaveState.Success(saved, triggerAt)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                saved?.let { rollbackAlarmSave(it, original) }
                _alarmSaveState.value = AlarmSaveState.Error(alarm, AlarmSaveError.SAVE_FAILED)
            }
        }
    }

    private suspend fun rollbackAlarmSave(saved: AlarmEntity, original: AlarmEntity?) {
        app.alarmScheduler.cancel(saved.id)
        if (original == null) {
            alarmDao.delete(saved)
        } else {
            alarmDao.update(original)
            if (original.enabled) app.alarmScheduler.schedule(original)
        }
    }

    fun requestSetAlarmEnabled(alarm: AlarmEntity, enabled: Boolean) {
        if (enabled) {
            _alarmEnableRequest.value = alarm
        } else {
            setAlarmEnabled(alarm, false)
        }
    }

    fun onAlarmEnableAuthorizationResult(result: AlarmAuthorizationResult) {
        val alarm = _alarmEnableRequest.value ?: return
        _alarmEnableRequest.value = null
        if (result is AlarmAuthorizationResult.Granted) {
            setAlarmEnabled(alarm, true)
        }
    }

    fun previewSystemThemeHue(hue: Float) {
        _systemThemeHuePreview.value = normalizeHue(hue)
    }

    fun clearSystemThemeHuePreview() {
        _systemThemeHuePreview.value = null
    }

    fun commitSystemThemeHue(hue: Float, custom: Boolean) {
        val normalized = normalizeHue(hue)
        val updated = _settings.value.copy(
            systemThemeHue = normalized,
            customSystemThemeHue = normalized,
            useCustomSystemThemeColor = custom,
        )
        _settings.value = updated
        _systemThemeHuePreview.value = null
        viewModelScope.launch { app.settingsRepository.save(updated) }
    }

    fun consumeAlarmEnableSuccess() {
        _alarmEnableSuccess.value = null
    }

    private fun setAlarmEnabled(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(enabled = enabled)
            if (!enabled) {
                app.alarmScheduler.disable(updated.id)
                return@launch
            }

            try {
                alarmDao.setEnabled(updated.id, true)
                val triggerAt = app.alarmScheduler.scheduleWithResult(updated)
                if (triggerAt == null) {
                    app.alarmScheduler.disable(updated.id)
                } else {
                    _alarmEnableSuccess.value = AlarmEnableSuccess(updated.id, triggerAt)
                }
            } catch (cancellation: CancellationException) {
                withContext(NonCancellable) { app.alarmScheduler.disable(updated.id) }
                throw cancellation
            } catch (_: Exception) {
                runCatching { app.alarmScheduler.disable(updated.id) }
                    .onFailure { runCatching { alarmDao.setEnabled(updated.id, false) } }
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            app.alarmScheduler.delete(alarm.id)
        }
    }

    fun reconcileAlarmCapabilities(canDeliverAlarms: Boolean) {
        viewModelScope.launch {
            alarmDao.getEnabled().forEach { alarm ->
                if (canDeliverAlarms) {
                    app.alarmScheduler.reschedulePersisted(alarm)
                } else {
                    app.alarmScheduler.cancel(alarm.id)
                }
            }
        }
    }

    fun reorderAlarms(alarmIds: List<Long>) {
        viewModelScope.launch { alarmDao.reorder(alarmIds) }
    }
}
