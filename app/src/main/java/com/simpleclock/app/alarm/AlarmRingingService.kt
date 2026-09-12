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
import com.simpleclock.app.data.AlarmOccurrenceEntity
import com.simpleclock.app.data.AlarmOccurrenceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

class AlarmRingingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var alarmId = -1L
    private var occurrenceKind = -1
    private var occurrenceTriggerAt = -1L
    private var occurrenceToken = ""
    private var currentStartId = 0
    private var latestStartId = 0
    private var stoppingToken: String? = null
    private val pendingOccurrences = ArrayDeque<PendingOccurrence>()
    private var ringing = false
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val autoStop = Runnable {
        if (ringing) {
            stopAlarm(
                snooze = false,
                requestedOccurrence = currentOccurrence(),
                stopStartId = currentStartId,
            )
        }
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
        latestStartId = startId
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm(
                    snooze = false,
                    requestedOccurrence = intent.occurrenceOrNull(),
                    stopStartId = startId,
                )
                return START_NOT_STICKY
            }

            ACTION_SNOOZE -> {
                stopAlarm(
                    snooze = true,
                    requestedOccurrence = intent.occurrenceOrNull(),
                    stopStartId = startId,
                )
                return START_NOT_STICKY
            }

            ACTION_INVALIDATE -> {
                invalidateOccurrence(intent.occurrenceOrNull(), startId)
                return START_NOT_STICKY
            }
        }

        val occurrence = intent?.occurrenceOrNull()
        if (occurrence == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val app = application as SimpleClockApplication
        if (app.consumeOccurrenceInvalidation(occurrence.token)) {
            if (!ringing && stoppingToken == null) stopSelf(startId)
            return START_NOT_STICKY
        }

        val current = currentOccurrence()
        if ((ringing || stoppingToken != null) && current?.token != occurrence.token) {
            if (pendingOccurrences.none { it.occurrence.token == occurrence.token }) {
                pendingOccurrences.addLast(PendingOccurrence(occurrence, startId))
            }
            return START_NOT_STICKY
        }
        if (current?.token == occurrence.token && (ringing || stoppingToken != null)) {
            return START_NOT_STICKY
        }

        beginOccurrence(occurrence, startId)
        return START_NOT_STICKY
    }

    private fun beginOccurrence(occurrence: AlarmOccurrenceEntity, startId: Int) {
        alarmId = occurrence.alarmId
        occurrenceKind = occurrence.kind
        occurrenceTriggerAt = occurrence.triggerAt
        occurrenceToken = occurrence.token
        currentStartId = startId
        stoppingToken = null
        startInForeground(buildNotification(occurrence, getString(R.string.alarm_name_default)))
        ringing = true
        mainHandler.removeCallbacks(autoStop)
        mainHandler.postDelayed(autoStop, MAX_RING_DURATION_MS)
        requestAudioFocus()
        startAlarmSound()
        startVibration()
        updateNotificationLabel(occurrence)
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

    private fun updateNotificationLabel(occurrence: AlarmOccurrenceEntity) {
        serviceScope.launch {
            val alarm = try {
                withContext(Dispatchers.IO) {
                    (application as SimpleClockApplication)
                        .database
                        .alarmDao()
                        .getById(occurrence.alarmId)
                }
            } catch (error: Exception) {
                Log.w(TAG, "Unable to load alarm label", error)
                null
            }

            if (ringing && occurrenceToken == occurrence.token) {
                val title = alarm?.label?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.alarm_name_default)
                val color = alarm?.color ?: 0L
                try {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        buildNotification(occurrence, title, color),
                    )
                } catch (error: RuntimeException) {
                    Log.w(TAG, "Unable to update alarm notification", error)
                }
            }
        }
    }

    private fun buildNotification(
        occurrence: AlarmOccurrenceEntity,
        title: String,
        color: Long = 0L,
    ): Notification {
        val activityIntent = Intent(this, AlarmActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            .let { occurrence.putInto(it) }
            .putExtra(
                AlarmScheduler.EXTRA_IS_SNOOZE,
                occurrence.kind == AlarmOccurrenceKind.SNOOZE,
            )
            .setData(occurrence.uri("ringing"))
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            occurrence.alarmId.requestCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPendingIntent = actionPendingIntent(ACTION_STOP, STOP_REQUEST_CODE, occurrence)
        val snoozePendingIntent = actionPendingIntent(
            ACTION_SNOOZE,
            SNOOZE_REQUEST_CODE,
            occurrence,
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
            .addAction(0, getString(R.string.stop), stopPendingIntent)
            .addAction(0, getString(R.string.snooze_minutes), snoozePendingIntent)
            .setSound(null)

        if (color != 0L) {
            builder.setColor(color.toInt())
        }
        if (AlarmCapabilities.canUseFullScreenIntent(this)) {
            builder.setFullScreenIntent(activityPendingIntent, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun actionPendingIntent(
        action: String,
        requestCode: Int,
        occurrence: AlarmOccurrenceEntity,
    ) =
        PendingIntent.getService(
            this,
            requestCode,
            occurrence.putInto(Intent(this, AlarmRingingService::class.java))
                .setAction(action)
                .setData(occurrence.uri(action)),
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

    private fun stopAlarm(
        snooze: Boolean,
        requestedOccurrence: AlarmOccurrenceEntity?,
        stopStartId: Int,
    ) {
        val occurrence = requestedOccurrence ?: return
        if (occurrence != currentOccurrence()) return
        if (stoppingToken == occurrence.token) return
        stoppingToken = occurrence.token
        stopPlayback()
        serviceScope.launch {
            val app = application as SimpleClockApplication
            try {
                withContext(Dispatchers.IO) {
                    val keepEnabled = snooze &&
                        app.alarmScheduler.scheduleSnooze(occurrence) != null
                    if (!keepEnabled) {
                        app.database.alarmDao().completeOccurrence(
                            alarmId = occurrence.alarmId,
                            kind = occurrence.kind,
                            token = occurrence.token,
                            triggerAt = occurrence.triggerAt,
                            disableOneTime = true,
                        )
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Unable to complete alarm occurrence ${occurrence.token}", error)
            } finally {
                sendBroadcast(
                    Intent(ACTION_DISMISS_ACTIVITY)
                        .setPackage(packageName)
                        .putExtra(AlarmScheduler.EXTRA_OCCURRENCE_TOKEN, occurrence.token),
                )
                continueWithNextOrStop(occurrence, stopStartId)
            }
        }
    }

    private fun invalidateOccurrence(occurrence: AlarmOccurrenceEntity?, startId: Int) {
        if (occurrence == null) {
            if (!ringing && stoppingToken == null) stopSelf(startId)
            return
        }
        val app = application as SimpleClockApplication
        val removedFromQueue = pendingOccurrences.removeIf {
            it.occurrence.token == occurrence.token
        }
        if (occurrence == currentOccurrence()) {
            app.consumeOccurrenceInvalidation(occurrence.token)
            stopAlarm(snooze = false, requestedOccurrence = occurrence, stopStartId = startId)
        } else if (removedFromQueue) {
            app.consumeOccurrenceInvalidation(occurrence.token)
        } else if (!ringing && stoppingToken == null && pendingOccurrences.isEmpty()) {
            stopSelf(startId)
        }
    }

    private fun continueWithNextOrStop(completed: AlarmOccurrenceEntity, stopStartId: Int) {
        if (currentOccurrence()?.token != completed.token) return
        stoppingToken = null
        val app = application as SimpleClockApplication
        var next = pendingOccurrences.pollFirst()
        while (next != null && app.consumeOccurrenceInvalidation(next.occurrence.token)) {
            next = pendingOccurrences.pollFirst()
        }
        if (next != null) {
            beginOccurrence(next.occurrence, maxOf(next.startId, latestStartId))
            return
        }
        alarmId = -1L
        occurrenceKind = -1
        occurrenceTriggerAt = -1L
        occurrenceToken = ""
        currentStartId = 0
        if (stopSelfResult(maxOf(stopStartId, latestStartId))) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
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

    private fun currentOccurrence(): AlarmOccurrenceEntity? =
        if (alarmId > 0L && occurrenceTriggerAt > 0L && occurrenceToken.isNotBlank() &&
            AlarmOccurrenceKind.isValid(occurrenceKind)
        ) {
            AlarmOccurrenceEntity(occurrenceToken, alarmId, occurrenceKind, occurrenceTriggerAt)
        } else {
            null
        }

    private fun Intent.occurrenceOrNull(): AlarmOccurrenceEntity? {
        val requestedAlarmId = getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val kind = getIntExtra(AlarmScheduler.EXTRA_OCCURRENCE_KIND, -1)
        val triggerAt = getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, -1L)
        val token = getStringExtra(AlarmScheduler.EXTRA_OCCURRENCE_TOKEN).orEmpty()
        return if (requestedAlarmId > 0L && triggerAt > 0L && token.isNotBlank() &&
            AlarmOccurrenceKind.isValid(kind)
        ) {
            AlarmOccurrenceEntity(token, requestedAlarmId, kind, triggerAt)
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

    companion object {
        const val ACTION_START = "com.simpleclock.app.action.START_ALARM"
        const val ACTION_STOP = "com.simpleclock.app.action.STOP_ALARM"
        const val ACTION_SNOOZE = "com.simpleclock.app.action.SNOOZE_ALARM"
        const val ACTION_INVALIDATE = "com.simpleclock.app.action.INVALIDATE_ALARM"
        const val ACTION_DISMISS_ACTIVITY = "com.simpleclock.app.action.DISMISS_ALARM_ACTIVITY"

        private const val TAG = "AlarmRingingService"
        private const val NOTIFICATION_ID = 1001
        private const val STOP_REQUEST_CODE = 1002
        private const val SNOOZE_REQUEST_CODE = 1003
        private const val RINGTONE_CHECK_INTERVAL_MS = 1_000L
        private const val MAX_RING_DURATION_MS = 15 * 60 * 1_000L
    }

    private data class PendingOccurrence(
        val occurrence: AlarmOccurrenceEntity,
        val startId: Int,
    )
}
