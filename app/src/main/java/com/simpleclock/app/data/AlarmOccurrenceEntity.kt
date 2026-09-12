package com.simpleclock.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarm_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("alarmId"), Index(value = ["alarmId", "kind"])],
)
data class AlarmOccurrenceEntity(
    @PrimaryKey val token: String,
    val alarmId: Long,
    val kind: Int,
    val triggerAt: Long,
    val claimedAt: Long? = null,
)

object AlarmOccurrenceKind {
    const val REGULAR = 0
    const val SNOOZE = 1

    fun isValid(kind: Int): Boolean = kind == REGULAR || kind == SNOOZE
}
