package com.simpleclock.app.ui

import android.app.Activity
import android.graphics.Color
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.simpleclock.app.AppDestination
import com.simpleclock.app.MainViewModel
import android.content.pm.ActivityInfo
import com.simpleclock.app.data.AppThemeColor
import com.simpleclock.app.data.ScreenOrientation
import com.simpleclock.app.ui.screens.AlarmListScreen
import com.simpleclock.app.ui.screens.ClockScreen
import com.simpleclock.app.ui.screens.SettingsScreen
import com.simpleclock.app.ui.theme.SimpleClockTheme

@Composable
fun SimpleClockApp(
    viewModel: MainViewModel,
    authorizeAlarm: (() -> Unit) -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val view = LocalView.current

    BackHandler(enabled = destination != AppDestination.CLOCK) {
        viewModel.navigate(AppDestination.CLOCK)
    }

    SimpleClockTheme(settings) { isDark ->
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            WindowInsetsControllerCompat(window, view).apply {
                val useDarkSystemIcons = !isDark
                isAppearanceLightStatusBars = useDarkSystemIcons
                isAppearanceLightNavigationBars = useDarkSystemIcons
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (destination == AppDestination.CLOCK && settings.fullScreen) {
                    hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        DisposableEffect(settings.keepScreenOn) {
            val window = (view.context as? Activity)?.window
            if (settings.keepScreenOn) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose { }
        }

        DisposableEffect(settings.screenOrientation) {
            val activity = view.context as? Activity
            activity?.requestedOrientation = when (settings.screenOrientation) {
                ScreenOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            onDispose { }
        }

        when (destination) {
            AppDestination.CLOCK -> ClockScreen(
                settings = settings,
                openAlarms = { viewModel.navigate(AppDestination.ALARMS) },
                openSettings = { viewModel.navigate(AppDestination.SETTINGS) },
                toggleFullScreen = {
                    viewModel.updateSettings { current ->
                        current.copy(fullScreen = !current.fullScreen)
                    }
                },
                cycleOrientation = {
                    viewModel.updateSettings { current ->
                        val next = when (current.screenOrientation) {
                            ScreenOrientation.SYSTEM -> ScreenOrientation.PORTRAIT
                            ScreenOrientation.PORTRAIT -> ScreenOrientation.LANDSCAPE
                            ScreenOrientation.LANDSCAPE -> ScreenOrientation.SYSTEM
                        }
                        current.copy(screenOrientation = next)
                    }
                },
            )
            AppDestination.ALARMS -> AlarmListScreen(
                alarms = alarms,
                onBack = { viewModel.navigate(AppDestination.CLOCK) },
                onSave = { alarm ->
                    if (alarm.enabled) {
                        authorizeAlarm { viewModel.saveAlarm(alarm) }
                    } else {
                        viewModel.saveAlarm(alarm)
                    }
                },
                onEnabledChange = { alarm, enabled ->
                    if (enabled) {
                        authorizeAlarm { viewModel.setAlarmEnabled(alarm, true) }
                    } else {
                        viewModel.setAlarmEnabled(alarm, false)
                    }
                },
                onDelete = viewModel::deleteAlarm,
            )
            AppDestination.SETTINGS -> SettingsScreen(
                settings = settings,
                isDark = isDark,
                onBack = { viewModel.navigate(AppDestination.CLOCK) },
                onUpdate = viewModel::updateSettings,
            )
        }
    }
}
