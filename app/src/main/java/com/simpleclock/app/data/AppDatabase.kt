package com.simpleclock.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AlarmEntity::class, AlarmOccurrenceEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "simple-clock.db",
        )
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `alarm_occurrences` (
                        `token` TEXT NOT NULL,
                        `alarmId` INTEGER NOT NULL,
                        `kind` INTEGER NOT NULL,
                        `triggerAt` INTEGER NOT NULL,
                        `claimedAt` INTEGER,
                        PRIMARY KEY(`token`),
                        FOREIGN KEY(`alarmId`) REFERENCES `alarms`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_alarm_occurrences_alarmId` " +
                        "ON `alarm_occurrences` (`alarmId`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_alarm_occurrences_alarmId_kind` " +
                        "ON `alarm_occurrences` (`alarmId`, `kind`)",
                )
            }
        }
    }
}
