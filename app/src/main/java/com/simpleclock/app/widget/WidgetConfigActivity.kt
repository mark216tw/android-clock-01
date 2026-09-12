package com.simpleclock.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simpleclock.app.R
import com.simpleclock.app.data.AppThemeMode
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val initialSettings = WidgetPreferences.load(this, appWidgetId)
        setContent {
            MaterialTheme {
                WidgetConfigScreen(
                    initialSettings = initialSettings,
                    alarmText = ClockWidgetProvider.nextAlarmLabel(this),
                    onSave = { settings ->
                        WidgetPreferences.save(this, appWidgetId, settings)
                        ClockWidgetProvider.updateWidget(this, appWidgetId)
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun WidgetConfigScreen(
    initialSettings: WidgetSettings,
    alarmText: String,
    onSave: (WidgetSettings) -> Unit,
) {
    var selectedColor by remember { mutableStateOf(initialSettings.color) }
    var selectedMode by remember { mutableStateOf(initialSettings.mode) }
    var transparentBackground by remember {
        mutableStateOf(initialSettings.transparentBackground)
    }
    var timeSizeText by remember { mutableStateOf(initialSettings.timeSizeSp.toString()) }
    val timeSizeSp = timeSizeText.toIntOrNull()?.takeIf { it > 0 }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.widget_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            WidgetPreview(
                selectedColor,
                selectedMode,
                transparentBackground,
                timeSizeSp ?: 1,
                alarmText,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.widget_time_size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = timeSizeText,
                onValueChange = { value ->
                    if (value.all(Char::isDigit)) {
                        timeSizeText = value
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = timeSizeSp == null,
                supportingText = if (timeSizeSp == null) {
                    {
                        Text(
                            androidx.compose.ui.res.stringResource(
                                R.string.widget_time_size_invalid,
                            ),
                        )
                    }
                } else {
                    null
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.theme_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WidgetThemeColor.entries.forEach { color ->
                    val swatch = previewPalette(color, false).accent
                    val swatchBackground = if (color == WidgetThemeColor.RAINBOW) {
                        Modifier.background(Brush.linearGradient(RainbowPreviewColors), CircleShape)
                    } else {
                        Modifier.background(swatch, CircleShape)
                    }
                    FilterChip(
                        selected = selectedColor == color,
                        onClick = { selectedColor = color },
                        label = {
                            Text(
                                text = androidx.compose.ui.res.stringResource(color.labelRes),
                                fontWeight = if (selectedColor == color) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                            )
                        },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .then(swatchBackground)
                                    .border(1.dp, Color.Gray, CircleShape),
                            )
                        },
                        trailingIcon = if (selectedColor == color) {
                            {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                        colors = prominentFilterChipColors(),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.display_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = {
                            Text(
                                text = androidx.compose.ui.res.stringResource(mode.labelRes),
                                fontWeight = if (selectedMode == mode) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                            )
                        },
                        leadingIcon = selectedCheckIcon(selectedMode == mode),
                        colors = prominentFilterChipColors(),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.widget_background_style),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !transparentBackground,
                    onClick = { transparentBackground = false },
                    label = {
                        Text(
                            androidx.compose.ui.res.stringResource(
                                R.string.widget_background_theme,
                            ),
                            fontWeight = if (!transparentBackground) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    leadingIcon = selectedCheckIcon(!transparentBackground),
                    colors = prominentFilterChipColors(),
                )
                FilterChip(
                    selected = transparentBackground,
                    onClick = { transparentBackground = true },
                    label = {
                        Text(
                            androidx.compose.ui.res.stringResource(
                                R.string.widget_background_transparent,
                            ),
                            fontWeight = if (transparentBackground) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    leadingIcon = selectedCheckIcon(transparentBackground),
                    colors = prominentFilterChipColors(),
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(
                        WidgetSettings(
                            color = selectedColor,
                            mode = selectedMode,
                            transparentBackground = transparentBackground,
                            timeSizeSp = requireNotNull(timeSizeSp),
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = timeSizeSp != null,
            ) {
                Text(androidx.compose.ui.res.stringResource(R.string.add_widget))
            }
        }
    }
}

@Composable
private fun prominentFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
)

private fun selectedCheckIcon(selected: Boolean): (@Composable () -> Unit)? = if (selected) {
    {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
} else {
    null
}

@Composable
private fun WidgetPreview(
    color: WidgetThemeColor,
    mode: AppThemeMode,
    transparentBackground: Boolean,
    timeSizeSp: Int,
    alarmText: String,
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val systemIsDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val isDark = when (mode) {
        AppThemeMode.SYSTEM -> systemIsDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val palette = previewPalette(color, isDark)
    val previewPrimary = when {
        transparentBackground && color == WidgetThemeColor.BLACK -> Color.Black
        transparentBackground && color == WidgetThemeColor.WHITE -> Color.White
        else -> palette.primary
    }
    val previewSecondary = when {
        transparentBackground && color == WidgetThemeColor.BLACK -> Color.Black
        transparentBackground && color == WidgetThemeColor.WHITE -> Color.White
        else -> palette.secondary
    }
    val previewBackground = when {
        transparentBackground -> Modifier
        color == WidgetThemeColor.RAINBOW -> Modifier.background(
            Brush.linearGradient(RainbowPreviewColors),
        )
        else -> Modifier.background(palette.background)
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val date = Date(now)
    val locale = configuration.locales[0]
    val datePattern = remember(configuration) {
        DateFormat.getBestDateTimePattern(locale, "EEEE MMM d")
    }
    val dateText = remember(now, datePattern) {
        SimpleDateFormat(datePattern, locale).format(date)
    }
    val timeText = remember(now, configuration) {
        DateFormat.getTimeFormat(context).format(date)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
            .padding(8.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize().then(previewBackground)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = timeText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .wrapContentHeight(Alignment.CenterVertically),
                        color = previewPrimary,
                        fontSize = timeSizeSp.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = dateText,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            color = previewSecondary,
                            fontSize = 17.sp,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = previewPrimary,
                            )
                            Text(
                                text = alarmText,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                color = previewPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class PreviewPalette(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
)

private fun previewPalette(color: WidgetThemeColor, isDark: Boolean): PreviewPalette {
    return when (color) {
        WidgetThemeColor.CORAL -> if (isDark) PreviewPalette(
            Color(0xFF3B1715), Color(0xFFFFF5F4), Color(0xFFE8B6B1), Color(0xFFFF7B70),
        ) else PreviewPalette(
            Color(0xFFFFF0ED), Color(0xFF7D2822), Color(0xFF79504C), Color(0xFFC8443A),
        )
        WidgetThemeColor.TANGERINE -> if (isDark) PreviewPalette(
            Color(0xFF39200D), Color(0xFFFFF6ED), Color(0xFFE9C19E), Color(0xFFFFA24A),
        ) else PreviewPalette(
            Color(0xFFFFF2E4), Color(0xFF713B0D), Color(0xFF75593E), Color(0xFFD66B0B),
        )
        WidgetThemeColor.SUNFLOWER -> if (isDark) PreviewPalette(
            Color(0xFF332B08), Color(0xFFFFF9DF), Color(0xFFE6D79A), Color(0xFFF1CA32),
        ) else PreviewPalette(
            Color(0xFFFFF8D8), Color(0xFF5F4B00), Color(0xFF6F6540), Color(0xFFB68A00),
        )
        WidgetThemeColor.MINT -> if (isDark) PreviewPalette(
            Color(0xFF103528), Color(0xFFEFFFF8), Color(0xFFA9DCC7), Color(0xFF55D69F),
        ) else PreviewPalette(
            Color(0xFFE6F8F0), Color(0xFF165B43), Color(0xFF496B5E), Color(0xFF24966A),
        )
        WidgetThemeColor.SKY -> if (isDark) PreviewPalette(
            Color(0xFF102E43), Color(0xFFF0F8FF), Color(0xFFAFCFE9), Color(0xFF64B5F0),
        ) else PreviewPalette(
            Color(0xFFE8F4FC), Color(0xFF19547D), Color(0xFF4A667A), Color(0xFF2787C1),
        )
        WidgetThemeColor.GRAPE -> if (isDark) PreviewPalette(
            Color(0xFF30183C), Color(0xFFFBF5FF), Color(0xFFD5B7E5), Color(0xFFC484E0),
        ) else PreviewPalette(
            Color(0xFFF5ECFA), Color(0xFF60317A), Color(0xFF6C5578), Color(0xFF9355B2),
        )
        WidgetThemeColor.RAINBOW -> PreviewPalette(
            Color(0xFF6636A6), Color.White, Color(0xFFF7F2FF), Color(0xFFB4235A),
        )
        WidgetThemeColor.BLACK -> PreviewPalette(
            Color.Black, Color.White, Color.White, Color.Black,
        )
        WidgetThemeColor.WHITE -> PreviewPalette(
            Color.White, Color.Black, Color.Black, Color.White,
        )
    }
}

private val RainbowPreviewColors = listOf(
    Color(0xFFB4235A),
    Color(0xFF6636A6),
    Color(0xFF075985),
)
