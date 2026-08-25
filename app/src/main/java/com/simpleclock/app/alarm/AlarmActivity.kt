package com.simpleclock.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simpleclock.app.R
import com.simpleclock.app.SimpleClockApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    private val alarmIdState = mutableLongStateOf(-1L)
    private var dismissReceiverRegistered = false

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlarmRingingService.ACTION_DISMISS_ACTIVITY) {
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmIdState.longValue = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        configureLockScreenWindow()

        setContent {
            val alarmId = alarmIdState.longValue
            val defaultLabel = stringResource(R.string.alarm_name_default)
            var display by remember(alarmId) {
                mutableStateOf(
                    AlarmDisplay(
                        time = formatTime(LocalTime.now()),
                        label = defaultLabel,
                    ),
                )
            }

            LaunchedEffect(alarmId) {
                val alarm = if (alarmId > 0L) {
                    try {
                        withContext(Dispatchers.IO) {
                            (application as SimpleClockApplication)
                                .database
                                .alarmDao()
                                .getById(alarmId)
                        }
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    null
                }
                if (alarm != null) {
                    display = AlarmDisplay(
                        time = if (intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)) {
                            formatTime(LocalTime.now())
                        } else {
                            formatTime(LocalTime.of(alarm.hour, alarm.minute))
                        },
                        label = alarm.label.ifBlank { defaultLabel },
                    )
                }
            }

            BackHandler(enabled = true) { }
            AlarmScreen(
                display = display,
                onStop = { performAlarmAction(AlarmRingingService.ACTION_STOP, alarmId) },
                onSnooze = { performAlarmAction(AlarmRingingService.ACTION_SNOOZE, alarmId) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmIdState.longValue = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(AlarmRingingService.ACTION_DISMISS_ACTIVITY)
        ContextCompat.registerReceiver(
            this,
            dismissReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        dismissReceiverRegistered = true
    }

    override fun onStop() {
        if (dismissReceiverRegistered) {
            unregisterReceiver(dismissReceiver)
            dismissReceiverRegistered = false
        }
        super.onStop()
    }

    private fun configureLockScreenWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideNavigationBar()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    private fun hideNavigationBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun performAlarmAction(action: String, alarmId: Long) {
        try {
            startService(
                Intent(this, AlarmRingingService::class.java)
                    .setAction(action)
                    .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
            )
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to deliver alarm action", error)
        } finally {
            finishAndRemoveTask()
        }
    }

    private fun formatTime(time: LocalTime): String {
        val pattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    private companion object {
        const val TAG = "AlarmActivity"
    }
}

private data class AlarmDisplay(
    val time: String,
    val label: String,
)

@androidx.compose.runtime.Composable
private fun AlarmScreen(
    display: AlarmDisplay,
    onStop: () -> Unit,
    onSnooze: () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFFFFB59E),
        onPrimary = Color(0xFF3E0A00),
        surface = Color(0xFF171218),
        onSurface = Color(0xFFFFF7F5),
    )
    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF171218))
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
            ) {
                if (maxWidth > maxHeight) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlarmInfo(display, compact = true, modifier = Modifier.weight(1f))
                        AlarmButtons(
                            onStop = onStop,
                            onSnooze = onSnooze,
                            compact = true,
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 360.dp),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        AlarmInfo(display, compact = false)
                        Spacer(modifier = Modifier.weight(1f))
                        AlarmButtons(onStop, onSnooze, compact = false)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AlarmInfo(display: AlarmDisplay, compact: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (compact) 64.dp else 88.dp)
                .background(Color(0x33FF8A65), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Alarm,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 38.dp else 48.dp),
                tint = Color(0xFFFFA184),
            )
        }
        Text(
            text = stringResource(R.string.alarm_ringing),
            modifier = Modifier.padding(top = if (compact) 10.dp else 24.dp),
            color = Color(0xFFFFB59E),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = display.time,
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 52.sp else 64.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = display.label,
            modifier = Modifier.padding(top = 4.dp),
            color = Color(0xFFD8C2BE),
            fontSize = if (compact) 18.sp else 22.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@androidx.compose.runtime.Composable
private fun AlarmButtons(
    onStop: () -> Unit,
    onSnooze: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Button(
            onClick = onSnooze,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 60.dp else 76.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF352B35),
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = stringResource(R.string.snooze_minutes),
                fontSize = if (compact) 18.sp else 21.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Button(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 60.dp else 76.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF8A65),
                contentColor = Color(0xFF3E0A00),
            ),
        ) {
            Text(
                text = stringResource(R.string.stop),
                fontSize = if (compact) 19.sp else 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
