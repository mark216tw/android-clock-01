package com.simpleclock.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.simpleclock.app.ui.SimpleClockApp
import com.simpleclock.app.alarm.AlarmCapabilities

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingAlarmAction: (() -> Unit)? = null
    private var waitingForSetting: SpecialSetting? = null
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            continueAlarmPermissionFlow()
        } else {
            denyPendingAlarm()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openRequestedDestination(intent)
        setContent {
            SimpleClockApp(
                viewModel = viewModel,
                authorizeAlarm = ::authorizeAlarm,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            viewModel.onAppBackgrounded()
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        val returningFrom = waitingForSetting
        if (returningFrom == null) {
            if (pendingAlarmAction == null) {
                viewModel.reconcileAlarmCapabilities(AlarmCapabilities.canDeliverAlarms(this))
            }
            return
        }
        waitingForSetting = null
        val granted = when (returningFrom) {
            SpecialSetting.EXACT_ALARM -> AlarmCapabilities.canScheduleExactAlarms(this)
            SpecialSetting.FULL_SCREEN -> AlarmCapabilities.canUseFullScreenIntent(this)
            SpecialSetting.NOTIFICATIONS -> AlarmCapabilities.canShowAlarmNotifications(this)
        }
        if (granted) continueAlarmPermissionFlow() else denyPendingAlarm()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequestedDestination(intent)
    }

    private fun openRequestedDestination(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ALARMS, false) == true) {
            viewModel.navigate(AppDestination.ALARMS)
        }
    }

    private fun authorizeAlarm(action: () -> Unit) {
        pendingAlarmAction = action
        continueAlarmPermissionFlow()
    }

    private fun continueAlarmPermissionFlow() {
        if (!AlarmCapabilities.canScheduleExactAlarms(this)) {
            waitingForSetting = SpecialSetting.EXACT_ALARM
            Toast.makeText(this, R.string.exact_alarm_permission_message, Toast.LENGTH_LONG).show()
            launchSpecialSetting(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            return
        }
        if (!AlarmCapabilities.canUseFullScreenIntent(this)) {
            waitingForSetting = SpecialSetting.FULL_SCREEN
            Toast.makeText(this, R.string.full_screen_permission_message, Toast.LENGTH_LONG).show()
            launchSpecialSetting(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        if (!AlarmCapabilities.canShowAlarmNotifications(this)) {
            waitingForSetting = SpecialSetting.NOTIFICATIONS
            Toast.makeText(this, R.string.notification_permission_message, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
            return
        }
        pendingAlarmAction?.invoke()
        pendingAlarmAction = null
    }

    private fun launchSpecialSetting(action: String) {
        val intent = Intent(action, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }.onFailure {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
            )
        }
    }

    private fun denyPendingAlarm() {
        pendingAlarmAction = null
        Toast.makeText(this, R.string.alarm_permission_denied, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_OPEN_ALARMS = "open_alarms"
    }

    private enum class SpecialSetting {
        EXACT_ALARM,
        FULL_SCREEN,
        NOTIFICATIONS,
    }
}
