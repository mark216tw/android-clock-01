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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
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
import com.simpleclock.app.data.ScreenOrientation
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
                SettingsSection(title = stringResource(R.string.orientation_title)) {
                    ChoiceRow(
                        choices = ScreenOrientation.entries.map { it to stringResource(it.labelRes) },
                        selected = settings.screenOrientation,
                        onSelected = { value -> onUpdate { it.copy(screenOrientation = value) } },
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
                SettingsSection(title = stringResource(R.string.clock_font_size)) {
                    Text(
                        stringResource(R.string.clock_font_size_portrait),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ChoiceRow(
                        choices = listOf(
                            1 to "1 (特小)",
                            2 to "2 (小)",
                            3 to "3 (標準)",
                            4 to "4 (大)",
                            5 to "5 (特大)",
                        ),
                        selected = settings.clockFontSizePortrait,
                        onSelected = { value -> onUpdate { it.copy(clockFontSizePortrait = value) } },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.clock_font_size_landscape),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ChoiceRow(
                        choices = listOf(
                            1 to "1 (特小)",
                            2 to "2 (小)",
                            3 to "3 (標準)",
                            4 to "4 (大)",
                            5 to "5 (特大)",
                        ),
                        selected = settings.clockFontSizeLandscape,
                        onSelected = { value -> onUpdate { it.copy(clockFontSizeLandscape = value) } },
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
                    AppThemeColor.entries.chunked(2).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowColors.forEach { color ->
                                val preview = previewColors(color, isDark, settings.randomRainbowColors)
                                val isRandomRainbow = color == AppThemeColor.RANDOM_RAINBOW
                                val gradientColors = if (isRandomRainbow) {
                                    settings.randomRainbowColors.map { Color(it) }
                                } else {
                                    null
                                }
                                val isSaved = isRandomRainbow && settings.savedRainbowThemes.contains(settings.randomRainbowColors)
                                ThemeColorCard(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(color.labelRes),
                                    primary = preview.primary,
                                    background = preview.background,
                                    rainbow = isRandomRainbow,
                                    gradientColors = gradientColors,
                                    selected = settings.themeColor == color,
                                    onGenerateClick = if (isRandomRainbow) {
                                        {
                                            val newColors = com.simpleclock.app.data.generateRandomRainbowColors()
                                            onUpdate {
                                                it.copy(
                                                    themeColor = AppThemeColor.RANDOM_RAINBOW,
                                                    randomRainbowColors = newColors,
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                    onSaveClick = if (isRandomRainbow && !isSaved) {
                                        {
                                            val updated = settings.savedRainbowThemes.toMutableList().apply {
                                                add(settings.randomRainbowColors)
                                            }
                                            onUpdate { it.copy(savedRainbowThemes = updated) }
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        if (isRandomRainbow && settings.themeColor == AppThemeColor.RANDOM_RAINBOW) {
                                            val newColors = com.simpleclock.app.data.generateRandomRainbowColors()
                                            onUpdate { it.copy(randomRainbowColors = newColors) }
                                        } else {
                                            onUpdate { it.copy(themeColor = color) }
                                        }
                                    },
                                )
                            }
                            repeat(2 - rowColors.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (settings.savedRainbowThemes.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.saved_themes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        settings.savedRainbowThemes.forEachIndexed { index, palette ->
                            val isCurrent = settings.themeColor == AppThemeColor.RANDOM_RAINBOW &&
                                settings.randomRainbowColors == palette
                            Surface(
                                onClick = {
                                    onUpdate {
                                        it.copy(
                                            themeColor = AppThemeColor.RANDOM_RAINBOW,
                                            randomRainbowColors = palette,
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = if (isCurrent) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null,
                                shadowElevation = if (isCurrent) 4.dp else 1.dp,
                                color = Color.Transparent,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(Brush.linearGradient(palette.map { Color(it) }))
                                        .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = if (isCurrent) "✓ 配色 ${index + 1}" else "配色 ${index + 1}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    IconButton(
                                        onClick = {
                                            val updated = settings.savedRainbowThemes.toMutableList().apply { removeAt(index) }
                                            onUpdate { it.copy(savedRainbowThemes = updated) }
                                        },
                                        modifier = Modifier.size(22.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.delete_saved_theme),
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
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
    gradientColors: List<Color>? = null,
    onGenerateClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null,
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
                        val brush = if (gradientColors != null && gradientColors.isNotEmpty()) {
                            Brush.linearGradient(gradientColors)
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4C1D95),
                                    Color(0xFF2563EB),
                                    Color(0xFF059669),
                                    Color(0xFFF59E0B),
                                    Color(0xFFDB2777),
                                ),
                            )
                        }
                        Modifier.background(brush)
                    } else {
                        Modifier
                    },
                )
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val contentColor = if (rainbow) Color.White else primary
            Text("10:08", color = contentColor, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (selected) "✓ $label" else label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            if (onGenerateClick != null || onSaveClick != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onGenerateClick != null) {
                        Surface(
                            onClick = onGenerateClick,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.38f),
                            contentColor = Color.White,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.generate_random_theme),
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = stringResource(R.string.generate_random_theme),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    if (onSaveClick != null) {
                        Surface(
                            onClick = onSaveClick,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.38f),
                            contentColor = Color.White,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.save_lucky_theme),
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = "儲存",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
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
