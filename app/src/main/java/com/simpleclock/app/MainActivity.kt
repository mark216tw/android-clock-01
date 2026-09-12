package com.simpleclock.app

import android.Manifest
import android.app.Activity
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
    private var suppressWindowAnimations = false
    private var pendingAlarmAction: ((AlarmAuthorizationResult) -> Unit)? = null
    private var waitingForSetting: AlarmCapability? = null
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            continueAlarmPermissionFlow()
        } else {
            denyPendingAlarm(AlarmCapability.NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        suppressWindowAnimations = intent.isLaunchedFromWidget()
        super.onCreate(savedInstanceState)
        updateWindowTransitions()
        enableEdgeToEdge()
        openRequestedDestination(intent)
        setContent {
            SimpleClockApp(
                viewModel = viewModel,
                authorizeAlarm = ::authorizeAlarm,
                moveAppToBackground = { moveTaskToBack(true) },
            )
        }
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
            AlarmCapability.EXACT_ALARM -> AlarmCapabilities.canScheduleExactAlarms(this)
            AlarmCapability.FULL_SCREEN -> AlarmCapabilities.canUseFullScreenIntent(this)
            AlarmCapability.NOTIFICATIONS -> AlarmCapabilities.canShowAlarmNotifications(this)
        }
        if (granted) continueAlarmPermissionFlow() else denyPendingAlarm(returningFrom)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        suppressWindowAnimations = intent.isLaunchedFromWidget()
        updateWindowTransitions()
        openRequestedDestination(intent)
    }

    override fun finish() {
        super.finish()
        if (suppressWindowAnimations && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun updateWindowTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (suppressWindowAnimations) {
                overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            } else {
                clearOverrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN)
                clearOverrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE)
            }
        } else if (suppressWindowAnimations) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun openRequestedDestination(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ALARMS, false) == true) {
            viewModel.navigate(AppDestination.ALARMS)
        }
    }

    private fun authorizeAlarm(action: (AlarmAuthorizationResult) -> Unit) {
        pendingAlarmAction = action
        continueAlarmPermissionFlow()
    }

    private fun continueAlarmPermissionFlow() {
        if (!AlarmCapabilities.canScheduleExactAlarms(this)) {
            waitingForSetting = AlarmCapability.EXACT_ALARM
            Toast.makeText(this, R.string.exact_alarm_permission_message, Toast.LENGTH_LONG).show()
            launchSpecialSetting(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
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
            waitingForSetting = AlarmCapability.NOTIFICATIONS
            Toast.makeText(this, R.string.notification_permission_message, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
            return
        }
        if (!AlarmCapabilities.canUseFullScreenIntent(this)) {
            waitingForSetting = AlarmCapability.FULL_SCREEN
            Toast.makeText(this, R.string.full_screen_permission_message, Toast.LENGTH_LONG).show()
            launchSpecialSetting(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            return
        }
        pendingAlarmAction?.invoke(AlarmAuthorizationResult.Granted)
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

    private fun denyPendingAlarm(capability: AlarmCapability) {
        pendingAlarmAction?.invoke(AlarmAuthorizationResult.Denied(capability))
        pendingAlarmAction = null
        Toast.makeText(this, R.string.alarm_permission_denied, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_OPEN_ALARMS = "open_alarms"
        const val EXTRA_LAUNCHED_FROM_WIDGET = "launched_from_widget"
    }

}

private fun Intent?.isLaunchedFromWidget(): Boolean =
    this?.getBooleanExtra(MainActivity.EXTRA_LAUNCHED_FROM_WIDGET, false) == true

enum class AlarmCapability {
    EXACT_ALARM,
    FULL_SCREEN,
    NOTIFICATIONS,
}

sealed interface AlarmAuthorizationResult {
    data object Granted : AlarmAuthorizationResult
    data class Denied(val capability: AlarmCapability) : AlarmAuthorizationResult
}
