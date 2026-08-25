package com.simpleclock.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simpleclock.app.R
import com.simpleclock.app.data.AppSettings
import com.simpleclock.app.data.AppThemeColor
import com.simpleclock.app.data.AppThemeMode
import com.simpleclock.app.data.ClockStyle
import com.simpleclock.app.data.TimeFormat
import com.simpleclock.app.ui.theme.previewColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    isDark: Boolean,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SettingsSection(title = "顯示") {
                    SettingSwitch(
                        label = stringResource(R.string.show_seconds),
                        checked = settings.showSeconds,
                        onCheckedChange = { checked ->
                            onUpdate { it.copy(showSeconds = checked) }
                        },
                    )
                    SettingSwitch(
                        label = stringResource(R.string.blink_colon),
                        checked = settings.blinkColon,
                        onCheckedChange = { checked ->
                            onUpdate { it.copy(blinkColon = checked) }
                        },
                    )
                    SettingSwitch(
                        label = stringResource(R.string.keep_screen_on),
                        checked = settings.keepScreenOn,
                        onCheckedChange = { checked ->
                            onUpdate { it.copy(keepScreenOn = checked) }
                        },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.use_24_hour)) {
                    ChoiceRow(
                        choices = listOf(
                            TimeFormat.SYSTEM to stringResource(R.string.follow_system),
                            TimeFormat.HOUR_12 to stringResource(R.string.hour_12),
                            TimeFormat.HOUR_24 to stringResource(R.string.hour_24),
                        ),
                        selected = settings.timeFormat,
                        onSelected = { value -> onUpdate { it.copy(timeFormat = value) } },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.display_mode)) {
                    ChoiceRow(
                        choices = AppThemeMode.entries.map { it to stringResource(it.labelRes) },
                        selected = settings.themeMode,
                        onSelected = { value -> onUpdate { it.copy(themeMode = value) } },
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.theme_color),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppThemeColor.entries.chunked(3).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowColors.forEach { color ->
                                val preview = previewColors(color, isDark)
                                ThemeColorCard(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(color.labelRes),
                                    primary = preview.primary,
                                    background = preview.background,
                                    rainbow = color == AppThemeColor.RAINBOW,
                                    selected = settings.themeColor == color,
                                    onClick = { onUpdate { it.copy(themeColor = color) } },
                                )
                            }
                            repeat(3 - rowColors.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.clock_style),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ClockStyle.entries.forEach { style ->
                        StyleCard(
                            style = style,
                            selected = settings.clockStyle == style,
                            onClick = { onUpdate { it.copy(clockStyle = style) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun <T> ChoiceRow(
    choices: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = {
                    Text(
                        label,
                        fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                leadingIcon = if (value == selected) {
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun ThemeColorCard(
    modifier: Modifier,
    label: String,
    primary: Color,
    background: Color,
    rainbow: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (rainbow) Color.Transparent else background,
        ),
        border = if (selected) BorderStroke(3.dp, primary) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (rainbow) {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4C1D95),
                                    Color(0xFF2563EB),
                                    Color(0xFF059669),
                                    Color(0xFFF59E0B),
                                    Color(0xFFDB2777),
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val contentColor = if (rainbow) Color.White else primary
            Text("10:08", color = contentColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (selected) "✓ $label" else label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun StyleCard(style: ClockStyle, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                ClockStyleDisplay(text = "10:08", style = style, size = 34f)
            }
            Text(stringResource(style.labelRes), style = MaterialTheme.typography.bodyMedium)
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
