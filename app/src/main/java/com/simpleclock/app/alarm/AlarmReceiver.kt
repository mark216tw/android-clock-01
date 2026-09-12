package com.simpleclock.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.simpleclock.app.SimpleClockApplication
import com.simpleclock.app.data.AlarmOccurrenceEntity
import com.simpleclock.app.data.AlarmOccurrenceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val occurrence = intent.occurrenceOrNull() ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val app = context.applicationContext as SimpleClockApplication
            try {
                val expectedKind = when (intent.action) {
                    AlarmScheduler.ACTION_ALARM -> AlarmOccurrenceKind.REGULAR
                    AlarmScheduler.ACTION_SNOOZE_ALARM -> AlarmOccurrenceKind.SNOOZE
                    else -> -1
                }
                if (occurrence.kind != expectedKind) {
                    app.alarmScheduler.clearOccurrence(
                        occurrence.alarmId,
                        occurrence.kind,
                        occurrence.token,
                        occurrence.triggerAt,
                    )
                    return@launch
                }
                val alarm = app.database.alarmDao().claimOccurrence(
                    alarmId = occurrence.alarmId,
                    kind = occurrence.kind,
                    token = occurrence.token,
                    triggerAt = occurrence.triggerAt,
                    now = System.currentTimeMillis(),
                    maxLateMillis = AlarmScheduler.MAX_LATE_MILLIS,
                )
                if (alarm == null) {
                    app.alarmScheduler.clearOccurrence(
                        occurrence.alarmId,
                        occurrence.kind,
                        occurrence.token,
                        occurrence.triggerAt,
                    )
                    return@launch
                }

                app.alarmScheduler.clearUpcomingPresentation(occurrence)
                val serviceIntent = occurrence.putInto(
                    Intent(context, AlarmRingingService::class.java)
                        .setAction(AlarmRingingService.ACTION_START),
                )
                val serviceStarted = try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                    true
                } catch (error: RuntimeException) {
                    Log.e(TAG, "Unable to start alarm service", error)
                    false
                }
                if (!serviceStarted) {
                    app.database.alarmDao().completeOccurrence(
                        alarmId = occurrence.alarmId,
                        kind = occurrence.kind,
                        token = occurrence.token,
                        triggerAt = occurrence.triggerAt,
                        disableOneTime = true,
                    )
                }

                if (occurrence.kind == AlarmOccurrenceKind.REGULAR && alarm.repeatDays != 0 &&
                    !app.alarmScheduler.scheduleNextAfter(alarm, occurrence.triggerAt)
                ) {
                    Log.e(TAG, "Unable to schedule next occurrence for alarm ${alarm.id}")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Unable to validate alarm occurrence ${occurrence.token}", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun Intent.occurrenceOrNull(): AlarmOccurrenceEntity? {
        val alarmId = getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val kind = getIntExtra(AlarmScheduler.EXTRA_OCCURRENCE_KIND, -1)
        val triggerAt = getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, -1L)
        val token = getStringExtra(AlarmScheduler.EXTRA_OCCURRENCE_TOKEN).orEmpty()
        if (alarmId <= 0L || triggerAt <= 0L || token.isBlank() ||
            !AlarmOccurrenceKind.isValid(kind)
        ) {
            return null
        }
        return AlarmOccurrenceEntity(token, alarmId, kind, triggerAt)
    }

    private fun AlarmOccurrenceEntity.putInto(intent: Intent): Intent = intent
        .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        .putExtra(AlarmScheduler.EXTRA_OCCURRENCE_KIND, kind)
        .putExtra(AlarmScheduler.EXTRA_TRIGGER_AT, triggerAt)
        .putExtra(AlarmScheduler.EXTRA_OCCURRENCE_TOKEN, token)
        .putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, kind == AlarmOccurrenceKind.SNOOZE)

    private companion object {
        const val TAG = "AlarmReceiver"
    }
}
