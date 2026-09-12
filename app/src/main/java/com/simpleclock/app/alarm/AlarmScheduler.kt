package com.simpleclock.app.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.simpleclock.app.MainActivity
import com.simpleclock.app.SimpleClockApplication
import com.simpleclock.app.data.AlarmDao
import com.simpleclock.app.data.AlarmEntity
import com.simpleclock.app.data.AlarmOccurrenceEntity
import com.simpleclock.app.data.AlarmOccurrenceKind
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class AlarmScheduler(
    private val context: Context,
    private val alarmDao: AlarmDao,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    suspend fun schedule(alarm: AlarmEntity): Boolean = scheduleWithResult(alarm) != null

    suspend fun scheduleWithResult(alarm: AlarmEntity): Long? = mutex.withLock {
        if (!alarm.enabled || alarm.id == 0L || !canScheduleExactAlarms()) return@withLock null
        val triggerAt = AlarmTimeCalculator.nextOccurrence(alarm).toInstant().toEpochMilli()
        triggerAt.takeIf { scheduleAtLocked(alarm.id, AlarmOccurrenceKind.REGULAR, triggerAt) != null }
    }

    suspend fun scheduleNextAfter(alarm: AlarmEntity, occurrenceTriggerAt: Long): Boolean =
        mutex.withLock {
            val currentAlarm = alarmDao.getById(alarm.id)
            if (currentAlarm == null || !currentAlarm.enabled || currentAlarm.repeatDays == 0 ||
                !canScheduleExactAlarms()
            ) {
                return@withLock false
            }
            val afterOccurrence = Instant.ofEpochMilli(occurrenceTriggerAt)
                .atZone(ZoneId.systemDefault())
                .plusNanos(1)
            val triggerAt = AlarmTimeCalculator.nextOccurrence(currentAlarm, afterOccurrence)
                .toInstant()
                .toEpochMilli()
            scheduleAtLocked(alarm.id, AlarmOccurrenceKind.REGULAR, triggerAt) != null
        }

    suspend fun scheduleSnooze(source: AlarmOccurrenceEntity): AlarmOccurrenceEntity? = mutex.withLock {
        if (!canScheduleExactAlarms()) return@withLock null
        val occurrence = AlarmOccurrenceEntity(
            token = UUID.randomUUID().toString(),
            alarmId = source.alarmId,
            kind = AlarmOccurrenceKind.SNOOZE,
            triggerAt = System.currentTimeMillis() + SNOOZE_DURATION_MS,
        )
        val replaced = alarmDao.replaceClaimedOccurrence(
            sourceAlarmId = source.alarmId,
            sourceKind = source.kind,
            sourceToken = source.token,
            sourceTriggerAt = source.triggerAt,
            replacement = occurrence,
        ) ?: return@withLock null
        try {
            replaced.forEach(::cancelSystemOccurrence)
            scheduleSystemOccurrence(occurrence)
            showSnoozedNotification(occurrence)
            occurrence
        } catch (_: RuntimeException) {
            alarmDao.deleteOccurrence(occurrence.token)
            if (alarmDao.getById(source.alarmId)?.repeatDays == 0) {
                alarmDao.setEnabled(source.alarmId, false)
            }
            cancelSystemOccurrence(occurrence)
            null
        }
    }

    suspend fun cancel(alarmId: Long) = mutex.withLock {
        alarmDao.removeOccurrences(alarmId).forEach(::cancelAndInvalidateOccurrence)
        cancelLegacyPendingIntents(alarmId)
    }

    suspend fun disable(alarmId: Long) = mutex.withLock {
        alarmDao.disableAndRemoveOccurrences(alarmId).forEach(::cancelAndInvalidateOccurrence)
        cancelLegacyPendingIntents(alarmId)
    }

    suspend fun delete(alarmId: Long) = mutex.withLock {
        alarmDao.deleteAlarmAndGetOccurrences(alarmId).forEach(::cancelAndInvalidateOccurrence)
        cancelLegacyPendingIntents(alarmId)
    }

    suspend fun update(alarm: AlarmEntity) = mutex.withLock {
        alarmDao.updateAlarmAndRemoveOccurrences(alarm).forEach(::cancelAndInvalidateOccurrence)
        cancelLegacyPendingIntents(alarm.id)
    }

    suspend fun cancelUnclaimedOccurrence(
        alarmId: Long,
        kind: Int,
        token: String,
        triggerAt: Long,
    ): AlarmEntity? = mutex.withLock {
        val occurrence = AlarmOccurrenceEntity(token, alarmId, kind, triggerAt)
        val alarm = alarmDao.skipOccurrence(token = token, alarmId = alarmId, kind = kind, triggerAt = triggerAt)
        if (alarm != null) cancelSystemOccurrence(occurrence)
        alarm
    }

    suspend fun clearOccurrence(
        alarmId: Long,
        kind: Int,
        token: String,
        triggerAt: Long,
    ) = mutex.withLock {
        val occurrence = AlarmOccurrenceEntity(token, alarmId, kind, triggerAt)
        alarmDao.deleteUnclaimedOccurrence(token, alarmId, kind, triggerAt)
        cancelSystemOccurrence(occurrence)
    }

    fun clearUpcomingPresentation(occurrence: AlarmOccurrenceEntity) {
        alarmManager.cancel(upcomingPendingIntent(occurrence))
        context.getSystemService(NotificationManager::class.java)
            .cancel(UpcomingAlarmReceiver.notificationId(occurrence.token))
    }

    suspend fun reschedulePersisted(
        alarm: AlarmEntity,
        mode: AlarmRescheduleMode = AlarmRescheduleMode.RESTORE_EPOCH,
    ) = mutex.withLock {
        if (!alarm.enabled || !canScheduleExactAlarms()) return@withLock
        val now = System.currentTimeMillis()
        val occurrences = alarmDao.getOccurrences(alarm.id)
        val activeClaimed = occurrences.filter {
            it.claimedAt != null && mode != AlarmRescheduleMode.RESTORE_AFTER_RESTART &&
                now - it.claimedAt <= CLAIM_LEASE_MILLIS
        }
        val staleClaimed = occurrences.filter {
            it.claimedAt != null && (mode == AlarmRescheduleMode.RESTORE_AFTER_RESTART ||
                now - it.claimedAt > CLAIM_LEASE_MILLIS)
        }
        staleClaimed.forEach { alarmDao.deleteOccurrence(it.token) }
        if (staleClaimed.isNotEmpty() && alarm.repeatDays == 0) {
            alarmDao.setEnabled(alarm.id, false)
            return@withLock
        }

        val unclaimed = occurrences.filter { it.claimedAt == null }
        val expired = unclaimed.filter { it.triggerAt < now - MAX_LATE_MILLIS }
        expired.forEach {
            alarmDao.deleteOccurrence(it.token)
            cancelSystemOccurrence(it)
        }
        val candidates = unclaimed - expired.toSet()
        val restorable = if (mode == AlarmRescheduleMode.RECALCULATE_REGULAR) {
            val regular = candidates.filter { it.kind == AlarmOccurrenceKind.REGULAR }
            regular.forEach {
                alarmDao.deleteOccurrence(it.token)
                cancelSystemOccurrence(it)
            }
            candidates.filter { it.kind == AlarmOccurrenceKind.SNOOZE }
        } else {
            candidates
        }
        restorable.forEach {
            scheduleSystemOccurrence(it)
            if (it.kind == AlarmOccurrenceKind.REGULAR) scheduleUpcoming(it)
            else showSnoozedNotification(it)
        }
        val hasRegular = restorable.any { it.kind == AlarmOccurrenceKind.REGULAR }
        val oneTimeInProgress = alarm.repeatDays == 0 &&
            (activeClaimed.isNotEmpty() || restorable.any { it.kind == AlarmOccurrenceKind.SNOOZE })
        if (!hasRegular && !oneTimeInProgress) {
            val triggerAt = AlarmTimeCalculator.nextOccurrence(alarm).toInstant().toEpochMilli()
            scheduleAtLocked(alarm.id, AlarmOccurrenceKind.REGULAR, triggerAt)
        }
    }

    private suspend fun scheduleAtLocked(
        alarmId: Long,
        kind: Int,
        triggerAt: Long,
    ): AlarmOccurrenceEntity? {
        val occurrence = AlarmOccurrenceEntity(
            token = UUID.randomUUID().toString(),
            alarmId = alarmId,
            kind = kind,
            triggerAt = triggerAt,
        )
        val replaced = alarmDao.replaceOccurrence(occurrence)
        replaced.forEach(::cancelSystemOccurrence)
        return try {
            scheduleSystemOccurrence(occurrence)
            if (kind == AlarmOccurrenceKind.REGULAR) {
                scheduleUpcoming(occurrence)
            } else {
                showSnoozedNotification(occurrence)
            }
            occurrence
        } catch (_: RuntimeException) {
            alarmDao.deleteOccurrence(occurrence.token)
            cancelSystemOccurrence(occurrence)
            null
        }
    }

    private fun scheduleSystemOccurrence(occurrence: AlarmOccurrenceEntity) {
        val showIntent = PendingIntent.getActivity(
            context,
            occurrence.alarmId.requestCode(),
            Intent(context, MainActivity::class.java)
                .setData(occurrence.uri("show"))
                .putExtra(MainActivity.EXTRA_OPEN_ALARMS, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(occurrence.triggerAt, showIntent),
            alarmPendingIntent(occurrence),
        )
    }

    private fun scheduleUpcoming(occurrence: AlarmOccurrenceEntity) {
        val upcomingAt = occurrence.triggerAt - UPCOMING_NOTICE_MS
        val intent = upcomingIntent(occurrence)
        if (upcomingAt <= System.currentTimeMillis()) {
            context.sendBroadcast(intent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                upcomingAt,
                upcomingPendingIntent(occurrence),
            )
        }
    }

    private fun showSnoozedNotification(occurrence: AlarmOccurrenceEntity) {
        context.sendBroadcast(
            occurrence.intent(context, UpcomingAlarmReceiver::class.java)
                .setAction(UpcomingAlarmReceiver.ACTION_SHOW_SNOOZED)
                .setData(occurrence.uri("snoozed")),
        )
    }

    private fun cancelSystemOccurrence(occurrence: AlarmOccurrenceEntity) {
        runCatching { alarmManager.cancel(alarmPendingIntent(occurrence)) }
        runCatching { alarmManager.cancel(upcomingPendingIntent(occurrence)) }
        val manager = context.getSystemService(NotificationManager::class.java)
        runCatching { manager.cancel(UpcomingAlarmReceiver.notificationId(occurrence.token)) }
    }

    private fun cancelAndInvalidateOccurrence(occurrence: AlarmOccurrenceEntity) {
        cancelSystemOccurrence(occurrence)
        if (occurrence.claimedAt == null) return
        (context.applicationContext as SimpleClockApplication)
            .markOccurrenceInvalidated(occurrence.token)
        runCatching {
            context.startService(
                occurrence.intent(context, AlarmRingingService::class.java)
                    .setAction(AlarmRingingService.ACTION_INVALIDATE)
                    .setData(occurrence.uri("invalidate")),
            )
        }
    }

    private fun alarmPendingIntent(occurrence: AlarmOccurrenceEntity): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            occurrence.alarmId.requestCode(),
            occurrence.intent(context, AlarmReceiver::class.java)
                .setAction(
                    if (occurrence.kind == AlarmOccurrenceKind.SNOOZE) {
                        ACTION_SNOOZE_ALARM
                    } else {
                        ACTION_ALARM
                    },
                )
                .setData(occurrence.uri("fire")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun upcomingPendingIntent(occurrence: AlarmOccurrenceEntity): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            occurrence.alarmId.upcomingRequestCode(),
            upcomingIntent(occurrence),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun upcomingIntent(occurrence: AlarmOccurrenceEntity) =
        occurrence.intent(context, UpcomingAlarmReceiver::class.java)
            .setAction(UpcomingAlarmReceiver.ACTION_SHOW_UPCOMING)
            .setData(occurrence.uri("upcoming"))

    private fun cancelLegacyPendingIntents(alarmId: Long) {
        fun legacy(snooze: Boolean) = PendingIntent.getBroadcast(
            context,
            if (snooze) alarmId.snoozeRequestCode() else alarmId.requestCode(),
            Intent(context, AlarmReceiver::class.java)
                .setAction(if (snooze) ACTION_SNOOZE_ALARM else ACTION_ALARM),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        legacy(false)?.let(alarmManager::cancel)
        legacy(true)?.let(alarmManager::cancel)
    }

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun AlarmOccurrenceEntity.intent(context: Context, target: Class<*>) =
        Intent(context, target)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_OCCURRENCE_KIND, kind)
            .putExtra(EXTRA_TRIGGER_AT, triggerAt)
            .putExtra(EXTRA_OCCURRENCE_TOKEN, token)

    private fun AlarmOccurrenceEntity.uri(role: String): Uri = Uri.Builder()
        .scheme("simpleclock")
        .authority("alarm-occurrence")
        .appendPath(role)
        .appendPath(token)
        .build()

    private fun Long.requestCode(): Int = (this and 0x3FFFFFFF).toInt()
    private fun Long.snoozeRequestCode(): Int = requestCode() or 0x40000000
    private fun Long.upcomingRequestCode(): Int = requestCode() or Int.MIN_VALUE

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        const val EXTRA_OCCURRENCE_KIND = "occurrence_kind"
        const val EXTRA_OCCURRENCE_TOKEN = "occurrence_token"
        const val ACTION_ALARM = "com.simpleclock.app.ALARM"
        const val ACTION_SNOOZE_ALARM = "com.simpleclock.app.SNOOZE_ALARM"
        const val MAX_LATE_MILLIS = 5 * 60 * 1000L
        const val CLAIM_LEASE_MILLIS = 16 * 60 * 1000L
        private const val SNOOZE_DURATION_MS = 10 * 60 * 1000L
        private const val UPCOMING_NOTICE_MS = 10 * 60 * 1000L
    }
}

enum class AlarmRescheduleMode {
    RESTORE_EPOCH,
    RESTORE_AFTER_RESTART,
    RECALCULATE_REGULAR,
}
