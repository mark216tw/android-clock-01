package com.simpleclock.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.simpleclock.app.SimpleClockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        val app = context.applicationContext as SimpleClockApplication
        app.alarmScheduler.cancelUpcoming(alarmId)
        app.alarmScheduler.cancelSnooze(alarmId)


        val serviceIntent = Intent(context, AlarmRingingService::class.java)
            .setAction(AlarmRingingService.ACTION_START)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            .putExtra(
                AlarmScheduler.EXTRA_IS_SNOOZE,
                intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false),
            )
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to start alarm service", error)
        }

        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false) ||
            intent.action == AlarmScheduler.ACTION_SNOOZE_ALARM
        if (isSnooze) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SimpleClockApplication
                val alarm = app.database.alarmDao().getById(alarmId) ?: return@launch
                if (alarm.repeatDays == 0) {
                    app.database.alarmDao().update(alarm.copy(enabled = false))
                } else {
                    app.alarmScheduler.schedule(alarm)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Unable to update alarm $alarmId after firing", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AlarmReceiver"
    }
}
