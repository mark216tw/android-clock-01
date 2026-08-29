package com.simpleclock.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.util.Log
import com.simpleclock.app.MainActivity
import com.simpleclock.app.R
import com.simpleclock.app.SimpleClockApplication
import com.simpleclock.app.data.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.Date

class UpcomingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        val triggerAt = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, -1L)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SimpleClockApplication
                val alarm = app.database.alarmDao().getById(alarmId)
                when (intent.action) {
                    ACTION_SHOW_UPCOMING -> {
                        if (triggerAt > 0L) {
                            showUpcoming(context, app, alarm, triggerAt)
                        }
                    }
                    ACTION_CANCEL_OCCURRENCE -> {
                        if (triggerAt > 0L) {
                            cancelOccurrence(app, alarm, triggerAt)
                        }
                    }
                    ACTION_SHOW_SNOOZED -> {
                        if (triggerAt > 0L) {
                            showSnoozed(context, app, alarm, triggerAt)
                        }
                    }
                    ACTION_CANCEL_SNOOZE -> cancelSnooze(app, alarmId)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Unable to handle upcoming/snooze alarm $alarmId", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showUpcoming(
        context: Context,
        app: SimpleClockApplication,
        alarm: AlarmEntity?,
        triggerAt: Long,
    ) {
        if (alarm == null || !alarm.enabled || !isCurrentOccurrence(alarm, triggerAt)) {
            alarm?.let { app.alarmScheduler.cancelUpcoming(it.id) }
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        createNotificationChannel(context, notificationManager)
        val title = alarm.label.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.alarm_name_default)
        val formattedTime = DateFormat.getTimeFormat(context).format(Date(triggerAt))
        val contentIntent = PendingIntent.getActivity(
            context,
            alarm.id.upcomingRequestCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_ALARMS, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.cancelRequestCode(),
            Intent(context, UpcomingAlarmReceiver::class.java)
                .setAction(ACTION_CANCEL_OCCURRENCE)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                .putExtra(AlarmScheduler.EXTRA_TRIGGER_AT, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setColor(alarm.color.toInt())
            .setContentTitle(title)
            .setContentText(
                context.getString(R.string.upcoming_alarm_at_time, formattedTime),
            )
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_LOW)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setWhen(triggerAt)
            .setShowWhen(true)
            .setAutoCancel(false)
            .setSound(null)
            .setDefaults(0)
            .setContentIntent(contentIntent)
            .addAction(
                0,
                context.getString(R.string.cancel_upcoming_alarm_occurrence),
                cancelIntent,
            )
            .build()

        try {
            notificationManager.notify(notificationId(alarm.id), notification)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to show upcoming alarm notification", error)
        }
    }

    private fun showSnoozed(
        context: Context,
        app: SimpleClockApplication,
        alarm: AlarmEntity?,
        triggerAt: Long,
    ) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        createNotificationChannel(context, notificationManager)
        val title = alarm?.label?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.alarm_name_default)
        val formattedTime = DateFormat.getTimeFormat(context).format(Date(triggerAt))
        val alarmId = alarm?.id ?: return
        val contentIntent = PendingIntent.getActivity(
            context,
            alarmId.snoozeNotificationRequestCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_ALARMS, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            alarmId.cancelSnoozeRequestCode(),
            Intent(context, UpcomingAlarmReceiver::class.java)
                .setAction(ACTION_CANCEL_SNOOZE)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setColor(alarm.color.toInt())
            .setContentTitle(title)
            .setContentText(
                context.getString(R.string.snoozed_alarm_at_time, formattedTime),
            )
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_LOW)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setWhen(triggerAt)
            .setShowWhen(true)
            .setAutoCancel(false)
            .setSound(null)
            .setDefaults(0)
            .setContentIntent(contentIntent)
            .addAction(
                0,
                context.getString(R.string.cancel_snooze),
                cancelIntent,
            )
            .build()

        try {
            notificationManager.notify(snoozeNotificationId(alarmId), notification)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to show snoozed alarm notification", error)
        }
    }

    private fun cancelSnooze(app: SimpleClockApplication, alarmId: Long) {
        app.alarmScheduler.cancelSnooze(alarmId)
    }

    private suspend fun cancelOccurrence(
        app: SimpleClockApplication,
        alarm: AlarmEntity?,
        triggerAt: Long,
    ) {
        if (alarm == null) return
        app.alarmScheduler.cancelUpcoming(alarm.id)
        if (!alarm.enabled || !isCurrentOccurrence(alarm, triggerAt)) return

        app.alarmScheduler.cancelOccurrence(alarm.id)
        if (alarm.repeatDays == 0) {
            app.database.alarmDao().update(alarm.copy(enabled = false))
        } else if (!app.alarmScheduler.scheduleNextAfter(alarm, triggerAt)) {
            Log.e(TAG, "Unable to schedule alarm ${alarm.id} after skipped occurrence")
        }
    }

    private fun isCurrentOccurrence(alarm: AlarmEntity, triggerAt: Long): Boolean {
        if (triggerAt <= System.currentTimeMillis()) return false
        val now = ZonedDateTime.now()
        return AlarmTimeCalculator.nextOccurrence(alarm, now).toInstant().toEpochMilli() == triggerAt
    }

    private fun createNotificationChannel(
        context: Context,
        notificationManager: NotificationManager,
    ) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.upcoming_alarm_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.upcoming_alarm_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun Long.baseRequestCode(): Int = (this and 0x3FFFFFFF).toInt()
    private fun Long.upcomingRequestCode(): Int = baseRequestCode() or Int.MIN_VALUE
    private fun Long.cancelRequestCode(): Int = baseRequestCode() or -0x40000000
    private fun Long.snoozeNotificationRequestCode(): Int = baseRequestCode() or 0x20000000
    private fun Long.cancelSnoozeRequestCode(): Int = baseRequestCode() or 0x60000000

    companion object {
        const val ACTION_SHOW_UPCOMING = "com.simpleclock.app.action.SHOW_UPCOMING_ALARM"
        const val ACTION_CANCEL_OCCURRENCE = "com.simpleclock.app.action.CANCEL_ALARM_OCCURRENCE"
        const val ACTION_SHOW_SNOOZED = "com.simpleclock.app.action.SHOW_SNOOZED_ALARM"
        const val ACTION_CANCEL_SNOOZE = "com.simpleclock.app.action.CANCEL_SNOOZE_ALARM"

        private const val TAG = "UpcomingAlarmReceiver"
        private const val CHANNEL_ID = "upcoming_alarms_silent"
        private val SUPPORTED_ACTIONS = setOf(
            ACTION_SHOW_UPCOMING,
            ACTION_CANCEL_OCCURRENCE,
            ACTION_SHOW_SNOOZED,
            ACTION_CANCEL_SNOOZE,
        )

        fun notificationId(alarmId: Long): Int =
            (alarmId and 0x3FFFFFFF).toInt() or Int.MIN_VALUE

        fun snoozeNotificationId(alarmId: Long): Int =
            (alarmId and 0x3FFFFFFF).toInt() or 0x40000000
    }
}

