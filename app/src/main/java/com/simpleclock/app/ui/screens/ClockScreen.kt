package com.simpleclock.app.ui.screens

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessAlarm
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simpleclock.app.data.AppSettings
import com.simpleclock.app.data.AppThemeColor
import com.simpleclock.app.data.ClockStyle
import com.simpleclock.app.data.ScreenOrientation
import com.simpleclock.app.data.ThemeColorMotion
import com.simpleclock.app.data.TimeFormat
import com.simpleclock.app.data.fontSizeScale
import com.simpleclock.app.R
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.min

@Composable
fun ClockScreen(
    settings: AppSettings,
    randomThemeMotion: ThemeColorMotion,
    openAlarms: () -> Unit,
    openSettings: () -> Unit,
    toggleFullScreen: () -> Unit,
    cycleOrientation: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(settings.showSeconds, settings.blinkColon) {
        while (true) {
            now = ZonedDateTime.now()
            val interval = if (settings.showSeconds || settings.blinkColon) 1_000L else 60_000L
            delay(interval - (System.currentTimeMillis() % interval) + 20L)
        }
    }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4_000)
            showControls = false
        }
    }

    val is24Hour = when (settings.timeFormat) {
        TimeFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        TimeFormat.HOUR_12 -> false
        TimeFormat.HOUR_24 -> true
    }
    val time = now.format(
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", Locale.getDefault()),
    )
    val seconds = now.format(DateTimeFormatter.ofPattern("ss", Locale.getDefault()))
    val dayPeriod = if (is24Hour) "" else {
        now.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))
    }
    val date = now.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()),
    )
    val colonVisible = !settings.blinkColon || now.second % 2 == 0
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val isRainbow = settings.themeColor == AppThemeColor.RANDOM_RAINBOW
    val clockColor = if (isRainbow) Color.White else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls },
    ) {
        ThemeMotionBackground(
            settings = settings,
            randomThemeMotion = randomThemeMotion,
            background = MaterialTheme.colorScheme.background,
            surface = MaterialTheme.colorScheme.surface,
            primary = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(),
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (settings.fullScreen) {
                        Modifier
                    } else {
                        Modifier.systemBarsPadding()
                    },
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            val mainAspect = displayAspect(time, settings.clockStyle)
            val isAnalog = settings.clockStyle.isAnalog()
            val hasSideInfo = !isAnalog && (settings.showSeconds || dayPeriod.isNotEmpty())
            val secondaryScale = 0.15f
            val sideAspect = when {
                isAnalog -> 0f
                settings.showSeconds ->
                    displayAspect(seconds, settings.clockStyle) * secondaryScale + 0.05f
                dayPeriod.isNotEmpty() -> 0.24f
                else -> 0f
            }
            val fontScaleMultiplier = settings.fontSizeScale(isPortrait)
            val styleScale = when (settings.clockStyle) {
                ClockStyle.LED -> if (isPortrait) 0.88f else 0.82f
                ClockStyle.GLASS -> if (isPortrait) 0.86f else 0.82f
                ClockStyle.NIXIE_TUBE -> if (isPortrait) 0.90f else 0.85f
                ClockStyle.ANALOG_CLASSIC,
                ClockStyle.ANALOG_MINIMAL,
                ClockStyle.SWISS_RAILWAY,
                ClockStyle.BAUHAUS_GEOMETRIC -> 0.88f
                else -> 1f
            }
            val rawMainSize = min(
                maxWidth.value / (mainAspect + sideAspect),
                maxHeight.value * 0.82f,
            ).coerceAtLeast(24f) * fontScaleMultiplier
            val mainSize = rawMainSize * styleScale

            Row(verticalAlignment = Alignment.Bottom) {
                ClockStyleDisplay(
                    text = time,
                    style = settings.clockStyle,
                    size = mainSize,
                    colonVisible = colonVisible,
                    displayColor = clockColor,
                    analogSecond = now.second.takeIf { isAnalog && settings.showSeconds },
                )
                if (hasSideInfo) {
                    Column(
                        modifier = Modifier.padding(
                            start = (mainSize * 0.025f).dp,
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (dayPeriod.isNotEmpty()) {
                            Text(
                                text = dayPeriod,
                                color = if (isRainbow) Color.White.copy(alpha = 0.82f) else {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                                },
                                fontSize = (mainSize * 0.10f).coerceAtLeast(12f).sp,
                            )
                        }
                        if (settings.showSeconds) {
                            ClockStyleDisplay(
                                text = seconds,
                                style = settings.clockStyle,
                                size = mainSize * secondaryScale,
                                displayColor = clockColor,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = date,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(
                    if (settings.fullScreen) {
                        Modifier.windowInsetsPadding(
                            WindowInsets.displayCutout.only(WindowInsetsSides.Top),
                        )
                    } else {
                        Modifier.statusBarsPadding()
                    },
                )
                .padding(
                    top = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        10.dp
                    } else {
                        12.dp
                    },
                    start = 12.dp,
                    end = 12.dp,
                ),
            color = if (isRainbow) Color.White.copy(alpha = 0.86f) else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
            },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ClockAction(Icons.Rounded.AccessAlarm, "鬧鐘", openAlarms)
                    ClockAction(Icons.Rounded.Settings, "設定", openSettings)
                    ClockAction(
                        icon = when (settings.screenOrientation) {
                            ScreenOrientation.SYSTEM -> Icons.Rounded.ScreenRotation
                            ScreenOrientation.PORTRAIT -> Icons.Rounded.StayCurrentPortrait
                            ScreenOrientation.LANDSCAPE -> Icons.Rounded.StayCurrentLandscape
                        },
                        label = stringResource(settings.screenOrientation.labelRes),
                        onClick = cycleOrientation,
                    )
                    ClockAction(
                        icon = if (settings.fullScreen) {
                            Icons.Rounded.FullscreenExit
                        } else {
                            Icons.Rounded.Fullscreen
                        },
                        label = stringResource(
                            if (settings.fullScreen) R.string.exit_full_screen
                            else R.string.enter_full_screen,
                        ),
                        onClick = toggleFullScreen,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ClockStyleDisplay(
    text: String,
    style: ClockStyle,
    size: Float,
    colonVisible: Boolean = true,
    displayColor: Color? = null,
    analogSecond: Int? = null,
) {
    val color = displayColor ?: MaterialTheme.colorScheme.primary
    val fontScale = LocalDensity.current.fontScale
    val adjustedSize = size / fontScale
    val displayText = text
    when (style) {
        ClockStyle.LED -> SevenSegmentDisplay(
            text = displayText,
            color = color,
            colonVisible = colonVisible,
            modifier = Modifier
                .width((size * sevenSegmentAspect(displayText)).dp)
                .height(size.dp),
        )
        ClockStyle.GLASS -> GlassClockDisplay(
            text = displayText,
            color = color,
            colonVisible = colonVisible,
            modifier = Modifier
                .width((size * glassClockAspect(displayText)).dp)
                .height(size.dp),
        )
        ClockStyle.NIXIE_TUBE -> NixieTubeDisplay(
            text = displayText,
            color = color,
            colonVisible = colonVisible,
            modifier = Modifier
                .width((size * nixieTubeAspect(displayText)).dp)
                .height(size.dp),
        )
        ClockStyle.AURA_GLOW -> AuraGlowDisplay(
            text = displayText,
            color = color,
            colonVisible = colonVisible,
            modifier = Modifier
                .width((size * (displayText.length * 0.58f)).dp)
                .height(size.dp),
        )
        ClockStyle.ANALOG_CLASSIC,
        ClockStyle.ANALOG_MINIMAL,
        ClockStyle.SWISS_RAILWAY,
        ClockStyle.BAUHAUS_GEOMETRIC -> {
            val parts = displayText.split(':')
            AnalogClockDisplay(
                hour = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                minute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                second = analogSecond,
                color = color,
                minimal = style == ClockStyle.ANALOG_MINIMAL,
                swissRailway = style == ClockStyle.SWISS_RAILWAY,
                bauhaus = style == ClockStyle.BAUHAUS_GEOMETRIC,
                modifier = Modifier
                    .width(size.dp)
                    .height(size.dp),
            )
        }
        else -> {
            val fontWeight = when (style) {
                ClockStyle.BOLD -> FontWeight.Black
                ClockStyle.THIN -> FontWeight.Light
                ClockStyle.NEON -> FontWeight.Medium
                ClockStyle.OUTLINE -> FontWeight.Black
                else -> FontWeight.Normal
            }
            val annotatedText = buildAnnotatedString {
                displayText.forEach { character ->
                    if (character == ':' && !colonVisible) {
                        withStyle(SpanStyle(color = Color.Transparent)) { append(character) }
                    } else {
                        append(character)
                    }
                }
            }
            Text(
                text = annotatedText,
                color = color,
                fontFamily = when (style) {
                    ClockStyle.NEON -> FontFamily.Monospace
                    else -> FontFamily.SansSerif
                },
                fontWeight = fontWeight,
                fontSize = adjustedSize.sp,
                lineHeight = adjustedSize.sp,
                letterSpacing = when (style) {
                    ClockStyle.NEON -> (2f / fontScale).sp
                    else -> 0.sp
                },
                style = when (style) {
                    ClockStyle.NEON -> TextStyle(
                        shadow = Shadow(
                            color = color.copy(alpha = 0.9f),
                            blurRadius = (size * 0.12f).coerceAtLeast(8f),
                        ),
                    )
                    ClockStyle.OUTLINE -> TextStyle(
                        drawStyle = Stroke(width = (size * 0.025f).coerceAtLeast(2f)),
                    )
                    else -> TextStyle.Default
                },
                maxLines = 1,
            )
        }
    }
}

private fun displayAspect(text: String, style: ClockStyle): Float = when (style) {
    ClockStyle.LED -> sevenSegmentAspect(text)
    ClockStyle.GLASS -> glassClockAspect(text)
    ClockStyle.NIXIE_TUBE -> nixieTubeAspect(text)
    ClockStyle.AURA_GLOW -> text.length * 0.58f
    ClockStyle.ANALOG_CLASSIC,
    ClockStyle.ANALOG_MINIMAL,
    ClockStyle.SWISS_RAILWAY,
    ClockStyle.BAUHAUS_GEOMETRIC -> 1f
    ClockStyle.NEON -> text.length * 0.64f
    else -> text.length * 0.56f
}

private fun ClockStyle.isAnalog(): Boolean =
    this == ClockStyle.ANALOG_CLASSIC ||
        this == ClockStyle.ANALOG_MINIMAL ||
        this == ClockStyle.SWISS_RAILWAY ||
        this == ClockStyle.BAUHAUS_GEOMETRIC

@Composable
private fun ClockAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
