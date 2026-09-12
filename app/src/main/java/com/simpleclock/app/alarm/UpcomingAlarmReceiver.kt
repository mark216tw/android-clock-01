package com.simpleclock.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.util.Log
import com.simpleclock.app.MainActivity
import com.simpleclock.app.R
import com.simpleclock.app.SimpleClockApplication
import com.simpleclock.app.data.AlarmEntity
import com.simpleclock.app.data.AlarmOccurrenceEntity
import com.simpleclock.app.data.AlarmOccurrenceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date

class UpcomingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val occurrence = intent.occurrenceOrNull() ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SimpleClockApplication
                val alarm = app.database.alarmDao().getById(occurrence.alarmId)
                val persisted = app.database.alarmDao().getOccurrence(occurrence.token)
                val isCurrent = persisted == occurrence && persisted.claimedAt == null &&
                    alarm?.enabled == true
                when (intent.action) {
                    ACTION_SHOW_UPCOMING, ACTION_SHOW_SNOOZED -> {
                        if (isCurrent && occurrence.triggerAt > System.currentTimeMillis()) {
                            showNotification(context, alarm, occurrence)
                        } else {
                            app.alarmScheduler.clearOccurrence(
                                occurrence.alarmId,
                                occurrence.kind,
                                occurrence.token,
                                occurrence.triggerAt,
                            )
                        }
                    }
                    ACTION_CANCEL_OCCURRENCE, ACTION_CANCEL_SNOOZE -> {
                        cancelOccurrence(app, alarm, occurrence)
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Unable to handle occurrence ${occurrence.token}", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        alarm: AlarmEntity,
        occurrence: AlarmOccurrenceEntity,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        createNotificationChannel(context, manager)
        val isSnooze = occurrence.kind == AlarmOccurrenceKind.SNOOZE
        val title = alarm.label.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.alarm_name_default)
        val formattedTime = DateFormat.getTimeFormat(context).format(Date(occurrence.triggerAt))
        val contentIntent = PendingIntent.getActivity(
            context,
            occurrence.alarmId.requestCode(),
            Intent(context, MainActivity::class.java)
                .setData(occurrence.uri("open"))
                .putExtra(MainActivity.EXTRA_OPEN_ALARMS, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelAction = if (isSnooze) ACTION_CANCEL_SNOOZE else ACTION_CANCEL_OCCURRENCE
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            occurrence.alarmId.cancelRequestCode(),
            occurrence.putInto(Intent(context, UpcomingAlarmReceiver::class.java))
                .setAction(cancelAction)
                .setData(occurrence.uri("cancel")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (isSnooze) {
            context.getString(R.string.snoozed_alarm_at_time, formattedTime)
        } else {
            context.getString(R.string.upcoming_alarm_at_time, formattedTime)
        }
        val actionText = if (isSnooze) R.string.cancel_snooze
        else R.string.cancel_upcoming_alarm_occurrence
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setColor(alarm.color.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_LOW)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setWhen(occurrence.triggerAt)
            .setShowWhen(true)
            .setAutoCancel(false)
            .setSound(null)
            .setDefaults(0)
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(actionText), cancelIntent)
            .build()
        try {
            manager.notify(notificationId(occurrence.token), notification)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to show alarm notification", error)
        }
    }

    private suspend fun cancelOccurrence(
        app: SimpleClockApplication,
        alarm: AlarmEntity?,
        occurrence: AlarmOccurrenceEntity,
    ) {
        val currentAlarm = app.alarmScheduler.cancelUnclaimedOccurrence(
            occurrence.alarmId,
            occurrence.kind,
            occurrence.token,
            occurrence.triggerAt,
        )
        if (currentAlarm == null || alarm == null ||
            occurrence.kind == AlarmOccurrenceKind.SNOOZE
        ) {
            return
        }
        if (currentAlarm.repeatDays != 0 &&
            !app.alarmScheduler.scheduleNextAfter(currentAlarm, occurrence.triggerAt)
        ) {
            Log.e(TAG, "Unable to schedule alarm ${alarm.id} after skipped occurrence")
        }
    }

    private fun createNotificationChannel(context: Context, manager: NotificationManager) {
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
        manager.createNotificationChannel(channel)
    }

    private fun Intent.occurrenceOrNull(): AlarmOccurrenceEntity? {
        val alarmId = getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val kind = getIntExtra(AlarmScheduler.EXTRA_OCCURRENCE_KIND, -1)
        val triggerAt = getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, -1L)
        val token = getStringExtra(AlarmScheduler.EXTRA_OCCURRENCE_TOKEN).orEmpty()
        return if (alarmId > 0L && triggerAt > 0L && token.isNotBlank() &&
            AlarmOccurrenceKind.isValid(kind)
        ) {
            AlarmOccurrenceEntity(token, alarmId, kind, triggerAt)
        } else {
            null
        }
    }

    private fun AlarmOccurrenceEntity.putInto(intent: Intent): Intent = intent
        .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        .putExtra(AlarmScheduler.EXTRA_OCCURRENCE_KIND, kind)
        .putExtra(AlarmScheduler.EXTRA_TRIGGER_AT, triggerAt)
        .putExtra(AlarmScheduler.EXTRA_OCCURRENCE_TOKEN, token)

    private fun AlarmOccurrenceEntity.uri(role: String): Uri = Uri.Builder()
        .scheme("simpleclock")
        .authority("alarm-occurrence")
        .appendPath(role)
        .appendPath(token)
        .build()

    private fun Long.requestCode(): Int = (this and 0x3FFFFFFF).toInt()
    private fun Long.cancelRequestCode(): Int = requestCode() or -0x40000000

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

        fun notificationId(token: String): Int = token.hashCode()
    }
}
