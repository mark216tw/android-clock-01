package com.simpleclock.app.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.simpleclock.app.MainActivity
import com.simpleclock.app.data.AlarmEntity
import java.time.Instant
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: AlarmEntity): Boolean {
        if (!alarm.enabled || alarm.id == 0L) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }

        val triggerAt = AlarmTimeCalculator.nextOccurrence(alarm).toInstant().toEpochMilli()
        return scheduleAt(alarm, triggerAt)
    }

    fun scheduleNextAfter(alarm: AlarmEntity, occurrenceTriggerAt: Long): Boolean {
        if (!alarm.enabled || alarm.id == 0L || alarm.repeatDays == 0) return false
        val afterOccurrence = Instant.ofEpochMilli(occurrenceTriggerAt)
            .atZone(ZoneId.systemDefault())
            .plusNanos(1)
        val triggerAt = AlarmTimeCalculator.nextOccurrence(alarm, afterOccurrence)
            .toInstant()
            .toEpochMilli()
        return scheduleAt(alarm, triggerAt)
    }

    private fun scheduleAt(alarm: AlarmEntity, triggerAt: Long): Boolean {
        cancelUpcoming(alarm.id)
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.requestCode(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_ALARMS, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
        return try {
            alarmManager.setAlarmClock(info, alarmPendingIntent(alarm.id, false))
            scheduleUpcoming(alarm.id, triggerAt)
            true
        } catch (_: SecurityException) {
            alarmManager.cancel(alarmPendingIntent(alarm.id, false))
            cancelUpcoming(alarm.id)
            false
        }
    }

    fun scheduleSnooze(alarmId: Long): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }
        val triggerAt = System.currentTimeMillis() + SNOOZE_DURATION_MS
        val showIntent = PendingIntent.getActivity(
            context,
            alarmId.snoozeRequestCode(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_ALARMS, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
        return try {
            alarmManager.setAlarmClock(info, alarmPendingIntent(alarmId, true))
            showSnoozedNotification(alarmId, triggerAt)
            true
        } catch (_: SecurityException) {
            cancelSnooze(alarmId)
            false
        }
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(alarmPendingIntent(alarmId, false))
        cancelSnooze(alarmId)
        cancelUpcoming(alarmId)
    }

    fun cancelOccurrence(alarmId: Long) {
        alarmManager.cancel(alarmPendingIntent(alarmId, false))
        cancelSnooze(alarmId)
        cancelUpcoming(alarmId)
    }

    fun cancelSnooze(alarmId: Long) {
        alarmManager.cancel(alarmPendingIntent(alarmId, true))
        context.getSystemService(NotificationManager::class.java)
            .cancel(UpcomingAlarmReceiver.snoozeNotificationId(alarmId))
    }

    fun cancelUpcoming(alarmId: Long) {
        alarmManager.cancel(upcomingPendingIntent(alarmId, 0L))
        context.getSystemService(NotificationManager::class.java)
            .cancel(UpcomingAlarmReceiver.notificationId(alarmId))
    }

    private fun showSnoozedNotification(alarmId: Long, triggerAt: Long) {
        val intent = Intent(context, UpcomingAlarmReceiver::class.java)
            .setAction(UpcomingAlarmReceiver.ACTION_SHOW_SNOOZED)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_TRIGGER_AT, triggerAt)
        context.sendBroadcast(intent)
    }

    private fun scheduleUpcoming(alarmId: Long, triggerAt: Long) {
        val upcomingAt = triggerAt - UPCOMING_NOTICE_MS
        val intent = upcomingIntent(alarmId, triggerAt)
        if (upcomingAt <= System.currentTimeMillis()) {
            context.sendBroadcast(intent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                upcomingAt,
                PendingIntent.getBroadcast(
                    context,
                    alarmId.upcomingRequestCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
    }

    private fun upcomingPendingIntent(alarmId: Long, triggerAt: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            alarmId.upcomingRequestCode(),
            upcomingIntent(alarmId, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun upcomingIntent(alarmId: Long, triggerAt: Long) =
        Intent(context, UpcomingAlarmReceiver::class.java)
            .setAction(UpcomingAlarmReceiver.ACTION_SHOW_UPCOMING)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_TRIGGER_AT, triggerAt)

    private fun alarmPendingIntent(alarmId: Long, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(if (snooze) ACTION_SNOOZE_ALARM else ACTION_ALARM)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_IS_SNOOZE, snooze)
        val requestCode = if (snooze) alarmId.snoozeRequestCode() else alarmId.requestCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun Long.requestCode(): Int = (this and 0x3FFFFFFF).toInt()
    private fun Long.snoozeRequestCode(): Int = requestCode() or 0x40000000
    private fun Long.upcomingRequestCode(): Int = requestCode() or Int.MIN_VALUE

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        const val ACTION_ALARM = "com.simpleclock.app.ALARM"
        const val ACTION_SNOOZE_ALARM = "com.simpleclock.app.SNOOZE_ALARM"
        private const val SNOOZE_DURATION_MS = 10 * 60 * 1000L
        private const val UPCOMING_NOTICE_MS = 10 * 60 * 1000L
    }
}
