package com.simpleclock.app.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AlarmCapabilities {
    const val RINGING_CHANNEL_ID = "ringing_alarms"

    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun canUseFullScreenIntent(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun canShowAlarmNotifications(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannel(RINGING_CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun canDeliverAlarms(context: Context): Boolean =
        canScheduleExactAlarms(context) &&
            canUseFullScreenIntent(context) &&
            canShowAlarmNotifications(context)
}
