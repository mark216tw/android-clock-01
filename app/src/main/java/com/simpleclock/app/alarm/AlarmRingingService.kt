package com.simpleclock.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.simpleclock.app.R
import com.simpleclock.app.SimpleClockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmRingingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var alarmId = -1L
    private var isSnooze = false
    private var ringing = false
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val autoStop = Runnable {
        if (ringing) stopAlarm(snooze = false, requestedAlarmId = alarmId)
    }

    private val ringtoneReplay = object : Runnable {
        override fun run() {
            val currentRingtone = ringtone ?: return
            if (!ringing) return
            try {
                if (!currentRingtone.isPlaying) currentRingtone.play()
            } catch (error: RuntimeException) {
                Log.w(TAG, "Unable to replay fallback ringtone", error)
            }
            mainHandler.postDelayed(this, RINGTONE_CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm(snooze = false, requestedAlarmId = intent.alarmId())
                return START_NOT_STICKY
            }

            ACTION_SNOOZE -> {
                stopAlarm(snooze = true, requestedAlarmId = intent.alarmId())
                return START_NOT_STICKY
            }
        }

        val requestedAlarmId = intent?.alarmId() ?: -1L
        if (requestedAlarmId <= 0L) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        alarmId = requestedAlarmId
        isSnooze = intent?.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false) == true
        startInForeground(buildNotification(requestedAlarmId, getString(R.string.alarm_name_default)))
        if (!ringing) {
            ringing = true
            mainHandler.removeCallbacks(autoStop)
            mainHandler.postDelayed(autoStop, MAX_RING_DURATION_MS)
            requestAudioFocus()
            startAlarmSound()
            startVibration()
        }
        updateNotificationLabel(requestedAlarmId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPlayback()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotificationLabel(requestedAlarmId: Long) {
        serviceScope.launch {
            val label = try {
                withContext(Dispatchers.IO) {
                    (application as SimpleClockApplication)
                        .database
                        .alarmDao()
                        .getById(requestedAlarmId)
                        ?.label
                }
            } catch (error: Exception) {
                Log.w(TAG, "Unable to load alarm label", error)
                null
            }

            if (ringing && alarmId == requestedAlarmId) {
                val title = label?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.alarm_name_default)
                try {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        buildNotification(requestedAlarmId, title),
                    )
                } catch (error: RuntimeException) {
                    Log.w(TAG, "Unable to update alarm notification", error)
                }
            }
        }
    }

    private fun buildNotification(requestedAlarmId: Long, title: String): Notification {
        val activityIntent = Intent(this, AlarmActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, requestedAlarmId)
            .putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, isSnooze)
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            requestedAlarmId.requestCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPendingIntent = actionPendingIntent(ACTION_STOP, STOP_REQUEST_CODE, requestedAlarmId)
        val snoozePendingIntent = actionPendingIntent(
            ACTION_SNOOZE,
            SNOOZE_REQUEST_CODE,
            requestedAlarmId,
        )

        val builder = Notification.Builder(this, AlarmCapabilities.RINGING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(title)
            .setContentText(getString(R.string.alarm_ringing))
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(activityPendingIntent)
            .setFullScreenIntent(activityPendingIntent, true)
            .addAction(0, getString(R.string.stop), stopPendingIntent)
            .addAction(0, getString(R.string.snooze_minutes), snoozePendingIntent)
            .setSound(null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun actionPendingIntent(action: String, requestCode: Int, requestedAlarmId: Long) =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, AlarmRingingService::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, requestedAlarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            AlarmCapabilities.RINGING_CHANNEL_ID,
            getString(R.string.alarm_ringing),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alarm_ringing)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun requestAudioFocus() {
        val manager = getSystemService(AudioManager::class.java) ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { }
            .build()
        audioManager = manager
        audioFocusRequest = request
        try {
            manager.requestAudioFocus(request)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to request alarm audio focus", error)
        }
    }

    private fun startAlarmSound() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
        val player = try {
            MediaPlayer.create(
                this,
                alarmUri,
                null,
                audioAttributes,
                AudioManager.AUDIO_SESSION_ID_GENERATE,
            )
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to create alarm media player", error)
            null
        }

        if (player != null) {
            try {
                mediaPlayer = player
                player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
                player.isLooping = true
                player.setOnErrorListener { failedPlayer, _, _ ->
                    if (mediaPlayer === failedPlayer) {
                        mediaPlayer = null
                        try {
                            failedPlayer.release()
                        } catch (_: RuntimeException) {
                            // The failed player may already have released its native resources.
                        }
                        if (ringing) startFallbackRingtone(alarmUri)
                    }
                    true
                }
                player.start()
                return
            } catch (error: RuntimeException) {
                Log.w(TAG, "Unable to start alarm media player", error)
                mediaPlayer = null
                try {
                    player.release()
                } catch (_: RuntimeException) {
                    // The failed player may already have released its native resources.
                }
            }
        }

        startFallbackRingtone(alarmUri)
    }

    private fun startFallbackRingtone(alarmUri: Uri) {
        if (!ringing || ringtone != null) return
        val fallback = try {
            RingtoneManager.getRingtone(this, alarmUri)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to obtain fallback ringtone", error)
            null
        } ?: return

        try {
            fallback.audioAttributes = audioAttributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) fallback.isLooping = true
            ringtone = fallback
            fallback.play()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                mainHandler.postDelayed(ringtoneReplay, RINGTONE_CHECK_INTERVAL_MS)
            }
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to play fallback ringtone", error)
            ringtone = null
            try {
                fallback.stop()
            } catch (_: RuntimeException) {
                // Nothing else can be done when the platform ringtone fails.
            }
        }
    }

    private fun startVibration() {
        val alarmVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        vibrator = alarmVibrator
        if (!alarmVibrator.hasVibrator()) return
        try {
            val effect = VibrationEffect.createWaveform(longArrayOf(0L, 600L, 400L), 0)
            alarmVibrator.vibrate(effect, audioAttributes)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to vibrate for alarm", error)
        }
    }

    private fun stopAlarm(snooze: Boolean, requestedAlarmId: Long) {
        val idToSnooze = requestedAlarmId.takeIf { it > 0L } ?: alarmId
        if (snooze && idToSnooze > 0L) {
            try {
                (application as SimpleClockApplication).alarmScheduler.scheduleSnooze(idToSnooze)
            } catch (error: RuntimeException) {
                Log.e(TAG, "Unable to schedule alarm snooze", error)
            }
        }

        stopPlayback()
        sendBroadcast(Intent(ACTION_DISMISS_ACTIVITY).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopPlayback() {
        ringing = false
        mainHandler.removeCallbacks(ringtoneReplay)
        mainHandler.removeCallbacks(autoStop)
        mediaPlayer?.let { player ->
            try {
                player.stop()
            } catch (_: IllegalStateException) {
                // A failed MediaPlayer can already be in the stopped state.
            }
            player.release()
        }
        mediaPlayer = null
        ringtone?.let { currentRingtone ->
            try {
                currentRingtone.stop()
            } catch (_: RuntimeException) {
                // Some device ringtone implementations can disappear with their provider.
            }
        }
        ringtone = null
        vibrator?.cancel()
        vibrator = null
        audioFocusRequest?.let { request ->
            try {
                audioManager?.abandonAudioFocusRequest(request)
            } catch (_: RuntimeException) {
                // Audio focus may already have been abandoned by the system.
            }
        }
        audioFocusRequest = null
        audioManager = null
    }

    private fun Intent.alarmId(): Long = getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

    private fun Long.requestCode(): Int = (this and 0x3FFFFFFF).toInt()

    companion object {
        const val ACTION_START = "com.simpleclock.app.action.START_ALARM"
        const val ACTION_STOP = "com.simpleclock.app.action.STOP_ALARM"
        const val ACTION_SNOOZE = "com.simpleclock.app.action.SNOOZE_ALARM"
        const val ACTION_DISMISS_ACTIVITY = "com.simpleclock.app.action.DISMISS_ALARM_ACTIVITY"

        private const val TAG = "AlarmRingingService"
        private const val NOTIFICATION_ID = 1001
        private const val STOP_REQUEST_CODE = 1002
        private const val SNOOZE_REQUEST_CODE = 1003
        private const val RINGTONE_CHECK_INTERVAL_MS = 1_000L
        private const val MAX_RING_DURATION_MS = 15 * 60 * 1_000L
    }
}
