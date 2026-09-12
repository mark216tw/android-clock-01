package com.simpleclock.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.simpleclock.app.alarm.AlarmTimeCalculator
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabled(): List<AlarmEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM alarms")
    suspend fun nextSortOrder(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("UPDATE alarms SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Long)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("SELECT * FROM alarm_occurrences WHERE token = :token")
    suspend fun getOccurrence(token: String): AlarmOccurrenceEntity?

    @Query("SELECT * FROM alarm_occurrences WHERE alarmId = :alarmId")
    suspend fun getOccurrences(alarmId: Long): List<AlarmOccurrenceEntity>

    @Query("SELECT * FROM alarm_occurrences WHERE alarmId = :alarmId AND kind = :kind")
    suspend fun getOccurrences(alarmId: Long, kind: Int): List<AlarmOccurrenceEntity>

    @Query(
        """SELECT * FROM alarm_occurrences
        WHERE alarmId = :alarmId AND kind = :kind AND claimedAt IS NULL""",
    )
    suspend fun getUnclaimedOccurrences(alarmId: Long, kind: Int): List<AlarmOccurrenceEntity>

    @Insert
    suspend fun insertOccurrence(occurrence: AlarmOccurrenceEntity)

    @Query("DELETE FROM alarm_occurrences WHERE token = :token")
    suspend fun deleteOccurrence(token: String)

    @Query("DELETE FROM alarm_occurrences WHERE alarmId = :alarmId AND kind = :kind")
    suspend fun deleteOccurrences(alarmId: Long, kind: Int)

    @Query("DELETE FROM alarm_occurrences WHERE alarmId = :alarmId")
    suspend fun deleteOccurrences(alarmId: Long)

    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: Long): Int

    @Query(
        """DELETE FROM alarm_occurrences
        WHERE alarmId = :alarmId AND kind = :kind AND claimedAt IS NULL""",
    )
    suspend fun deleteUnclaimedOccurrences(alarmId: Long, kind: Int)

    @Query(
        """UPDATE alarm_occurrences SET claimedAt = :claimedAt
        WHERE token = :token AND alarmId = :alarmId AND kind = :kind
        AND triggerAt = :triggerAt AND claimedAt IS NULL""",
    )
    suspend fun markOccurrenceClaimed(
        token: String,
        alarmId: Long,
        kind: Int,
        triggerAt: Long,
        claimedAt: Long,
    ): Int

    @Query(
        """DELETE FROM alarm_occurrences WHERE token = :token AND alarmId = :alarmId
        AND kind = :kind AND triggerAt = :triggerAt AND claimedAt IS NULL""",
    )
    suspend fun deleteUnclaimedOccurrence(
        token: String,
        alarmId: Long,
        kind: Int,
        triggerAt: Long,
    ): Int

    @Transaction
    suspend fun replaceOccurrence(occurrence: AlarmOccurrenceEntity): List<AlarmOccurrenceEntity> {
        val replaced = getUnclaimedOccurrences(occurrence.alarmId, occurrence.kind)
        deleteUnclaimedOccurrences(occurrence.alarmId, occurrence.kind)
        insertOccurrence(occurrence)
        return replaced
    }

    @Transaction
    suspend fun replaceClaimedOccurrence(
        sourceAlarmId: Long,
        sourceKind: Int,
        sourceToken: String,
        sourceTriggerAt: Long,
        replacement: AlarmOccurrenceEntity,
    ): List<AlarmOccurrenceEntity>? {
        val source = getOccurrence(sourceToken) ?: return null
        if (source.alarmId != sourceAlarmId ||
            source.kind != sourceKind ||
            source.triggerAt != sourceTriggerAt ||
            source.claimedAt == null ||
            getById(sourceAlarmId)?.enabled != true
        ) {
            return null
        }
        val replaced = getUnclaimedOccurrences(replacement.alarmId, replacement.kind)
        deleteUnclaimedOccurrences(replacement.alarmId, replacement.kind)
        deleteOccurrence(sourceToken)
        insertOccurrence(replacement)
        return replaced
    }

    @Transaction
    suspend fun disableAndRemoveOccurrences(alarmId: Long): List<AlarmOccurrenceEntity> {
        setEnabled(alarmId, false)
        val occurrences = getOccurrences(alarmId)
        deleteOccurrences(alarmId)
        return occurrences
    }

    @Transaction
    suspend fun deleteAlarmAndGetOccurrences(alarmId: Long): List<AlarmOccurrenceEntity> {
        val occurrences = getOccurrences(alarmId)
        deleteAlarmById(alarmId)
        return occurrences
    }

    @Transaction
    suspend fun removeOccurrences(alarmId: Long): List<AlarmOccurrenceEntity> {
        val occurrences = getOccurrences(alarmId)
        deleteOccurrences(alarmId)
        return occurrences
    }

    @Transaction
    suspend fun updateAlarmAndRemoveOccurrences(alarm: AlarmEntity): List<AlarmOccurrenceEntity> {
        update(alarm)
        val occurrences = getOccurrences(alarm.id)
        deleteOccurrences(alarm.id)
        return occurrences
    }

    @Transaction
    suspend fun skipOccurrence(
        alarmId: Long,
        kind: Int,
        token: String,
        triggerAt: Long,
    ): AlarmEntity? {
        val alarm = getById(alarmId) ?: return null
        if (!alarm.enabled || deleteUnclaimedOccurrence(token, alarmId, kind, triggerAt) != 1) {
            return null
        }
        if (alarm.repeatDays == 0) setEnabled(alarmId, false)
        return alarm
    }

    @Transaction
    suspend fun claimOccurrence(
        alarmId: Long,
        kind: Int,
        token: String,
        triggerAt: Long,
        now: Long,
        maxLateMillis: Long,
    ): AlarmEntity? {
        val occurrence = getOccurrence(token) ?: return null
        val alarm = getById(alarmId) ?: return null
        if (!alarm.enabled ||
            occurrence.alarmId != alarmId ||
            occurrence.kind != kind ||
            occurrence.triggerAt != triggerAt ||
            occurrence.claimedAt != null ||
            now < triggerAt ||
            now - triggerAt > maxLateMillis
        ) {
            return null
        }
        if (kind == AlarmOccurrenceKind.REGULAR) {
            val immediatelyBefore = Instant.ofEpochMilli(triggerAt)
                .minusNanos(1)
                .atZone(ZoneId.systemDefault())
            val expectedTriggerAt = AlarmTimeCalculator.nextOccurrence(alarm, immediatelyBefore)
                .toInstant()
                .toEpochMilli()
            if (expectedTriggerAt != triggerAt) return null
        }
        return alarm.takeIf {
            markOccurrenceClaimed(token, alarmId, kind, triggerAt, now) == 1
        }
    }

    @Transaction
    suspend fun completeOccurrence(
        alarmId: Long,
        kind: Int,
        token: String,
        triggerAt: Long,
        disableOneTime: Boolean,
    ): Boolean {
        val occurrence = getOccurrence(token) ?: return false
        if (occurrence.alarmId != alarmId ||
            occurrence.kind != kind ||
            occurrence.triggerAt != triggerAt ||
            occurrence.claimedAt == null
        ) {
            return false
        }
        val alarm = getById(alarmId) ?: return false
        deleteOccurrence(token)
        if (disableOneTime && alarm.repeatDays == 0) setEnabled(alarmId, false)
        return true
    }

    @Transaction
    suspend fun reorder(alarmIds: List<Long>) {
        alarmIds.forEachIndexed { index, id ->
            updateSortOrder(id, index.toLong())
        }
    }
}
