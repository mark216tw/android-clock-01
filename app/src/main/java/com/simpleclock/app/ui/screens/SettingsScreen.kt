package com.simpleclock.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simpleclock.app.R
import com.simpleclock.app.data.AppSettings
import com.simpleclock.app.data.AppThemeColor
import com.simpleclock.app.data.AppThemeMode
import com.simpleclock.app.data.ClockBackgroundColorMode
import com.simpleclock.app.data.ClockBackgroundPattern
import com.simpleclock.app.data.ClockStyle
import com.simpleclock.app.data.ClockStyleCategory
import com.simpleclock.app.data.SYSTEM_THEME_HUE_PRESETS
import com.simpleclock.app.data.TimeFormat
import com.simpleclock.app.data.generateRandomRainbowColors
import com.simpleclock.app.data.hueDistance
import com.simpleclock.app.data.hsvToArgb
import com.simpleclock.app.data.rainbowHueColor
import com.simpleclock.app.data.category
import com.simpleclock.app.data.isAnalog
import com.simpleclock.app.ui.components.HueSlider
import com.simpleclock.app.ui.theme.previewColors
import com.simpleclock.app.ui.theme.systemThemePalette

private val clockThemePresets = listOf(
    AppThemeColor.CORAL to 350f,
    AppThemeColor.TANGERINE to 25f,
    AppThemeColor.SUNFLOWER to 50f,
    AppThemeColor.MINT to 150f,
    AppThemeColor.SKY to 200f,
    AppThemeColor.GRAPE to 270f,
)

private val systemThemeLabels = listOf(
    R.string.system_theme_berry,
    R.string.system_theme_orange,
    R.string.system_theme_lemon,
    R.string.system_theme_mint,
    R.string.system_theme_sky,
    R.string.system_theme_violet,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    isDark: Boolean,
    onBack: () -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onPreviewSystemThemeHue: (Float) -> Unit,
    onCommitSystemThemeHue: (Float, Boolean) -> Unit,
    onClearSystemThemeHuePreview: () -> Unit,
) {
    var previewSystemHue by remember { mutableFloatStateOf(settings.systemThemeHue) }
    var previewCustomSystemHue by remember { mutableFloatStateOf(settings.customSystemThemeHue) }
    var previewUsesCustomSystemColor by remember { mutableStateOf(settings.useCustomSystemThemeColor) }
    var previewCustomHue by remember { mutableFloatStateOf(settings.customThemeHue) }
    LaunchedEffect(
        settings.systemThemeHue,
        settings.customSystemThemeHue,
        settings.useCustomSystemThemeColor,
    ) {
        previewSystemHue = settings.systemThemeHue
        previewCustomSystemHue = settings.customSystemThemeHue
        previewUsesCustomSystemColor = settings.useCustomSystemThemeColor
    }
    LaunchedEffect(settings.customThemeHue) { previewCustomHue = settings.customThemeHue }
    DisposableEffect(Unit) {
        onDispose(onClearSystemThemeHuePreview)
    }

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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { SectionHeading(stringResource(R.string.settings_section_system)) }
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
                SettingsSection(title = stringResource(R.string.system_theme_color)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SYSTEM_THEME_HUE_PRESETS.forEachIndexed { index, hue ->
                            val swatchColor = Color(hsvToArgb(hue, 0.78f, 0.96f))
                            ColorSwatch(
                                color = swatchColor,
                                selected = !previewUsesCustomSystemColor &&
                                    hueDistance(previewSystemHue, hue) < 0.5f,
                                label = stringResource(systemThemeLabels[index]),
                                checkColor = contrastColor(swatchColor),
                                onClick = {
                                    previewSystemHue = hue
                                    previewCustomSystemHue = hue
                                    previewUsesCustomSystemColor = false
                                    onPreviewSystemThemeHue(hue)
                                    onCommitSystemThemeHue(hue, false)
                                },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val customPalette = systemThemePalette(previewCustomSystemHue, isDark)
                        ColorSwatch(
                            color = Color(customPalette.primary),
                            selected = previewUsesCustomSystemColor,
                            label = stringResource(R.string.system_custom_color),
                            checkColor = Color(customPalette.onPrimary),
                            onClick = {
                                previewSystemHue = previewCustomSystemHue
                                previewUsesCustomSystemColor = true
                                onPreviewSystemThemeHue(previewCustomSystemHue)
                                onCommitSystemThemeHue(previewCustomSystemHue, true)
                            },
                        )
                        HueSlider(
                            hue = previewCustomSystemHue,
                            colorAtHue = { hue -> Color(systemThemePalette(hue, isDark).primary) },
                            trackColorAtHue = { hue -> Color(rainbowHueColor(hue)) },
                            label = stringResource(R.string.system_color_hue),
                            modifier = Modifier.weight(1f),
                            onHueChange = { hue ->
                                previewCustomSystemHue = hue
                                previewSystemHue = hue
                                previewUsesCustomSystemColor = true
                                onPreviewSystemThemeHue(hue)
                            },
                            onHueChangeFinished = {
                                onCommitSystemThemeHue(previewCustomSystemHue, true)
                            },
                        )
                    }
                }
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            item { SectionHeading(stringResource(R.string.settings_section_clock)) }
            item {
                SettingsSection(title = stringResource(R.string.display)) {
                    SettingSwitch(
                        label = stringResource(R.string.show_seconds),
                        checked = settings.showSeconds,
                        onCheckedChange = { checked -> onUpdate { it.copy(showSeconds = checked) } },
                    )
                    SettingSwitch(
                        label = stringResource(R.string.blink_colon),
                        checked = settings.blinkColon,
                        onCheckedChange = { checked -> onUpdate { it.copy(blinkColon = checked) } },
                    )
                    SettingSwitch(
                        label = stringResource(R.string.keep_screen_on),
                        checked = settings.keepScreenOn,
                        onCheckedChange = { checked -> onUpdate { it.copy(keepScreenOn = checked) } },
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
                SettingsSection(title = stringResource(R.string.clock_font_size)) {
                    Text(
                        stringResource(R.string.clock_font_size_portrait),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FontSizeChoices(settings.clockFontSizePortrait) { value ->
                        onUpdate { it.copy(clockFontSizePortrait = value) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.clock_font_size_landscape),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FontSizeChoices(settings.clockFontSizeLandscape) { value ->
                        onUpdate { it.copy(clockFontSizeLandscape = value) }
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.theme_color)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        clockThemePresets.forEach { (color, hue) ->
                            val preview = previewColors(color, isDark)
                            ColorSwatch(
                                color = preview.primary,
                                selected = settings.themeColor == color,
                                label = stringResource(color.labelRes),
                                checkColor = contrastColor(preview.primary),
                                onClick = {
                                    previewCustomHue = hue
                                    onUpdate {
                                        it.copy(
                                            themeColor = color,
                                            customThemeHue = hue,
                                        )
                                    }
                                },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val customPreview = previewColors(
                            AppThemeColor.CUSTOM,
                            isDark,
                            customHue = previewCustomHue,
                        )
                        ColorSwatch(
                            color = customPreview.primary,
                            selected = settings.themeColor == AppThemeColor.CUSTOM,
                            label = stringResource(R.string.custom_color),
                            checkColor = contrastColor(customPreview.primary),
                            onClick = {
                                onUpdate {
                                    it.copy(
                                        themeColor = AppThemeColor.CUSTOM,
                                        customThemeHue = previewCustomHue,
                                    )
                                }
                            },
                        )
                        HueSlider(
                            hue = previewCustomHue,
                            colorAtHue = { hue ->
                                previewColors(AppThemeColor.CUSTOM, isDark, customHue = hue).primary
                            },
                            label = stringResource(R.string.custom_color),
                            modifier = Modifier.weight(1f),
                            onHueChange = { hue ->
                                previewCustomHue = hue
                                if (settings.themeColor != AppThemeColor.CUSTOM) {
                                    onUpdate {
                                        it.copy(themeColor = AppThemeColor.CUSTOM)
                                    }
                                }
                            },
                            onHueChangeFinished = {
                                onUpdate {
                                    it.copy(
                                        themeColor = AppThemeColor.CUSTOM,
                                        customThemeHue = previewCustomHue,
                                    )
                                }
                            },
                        )
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.background_pattern)) {
                    val dynamic = settings.clockBackgroundDynamic
                    FilterChip(
                        selected = dynamic,
                        onClick = { onUpdate { it.copy(clockBackgroundDynamic = !dynamic) } },
                        label = {
                            Text(
                                stringResource(R.string.theme_motion_dynamic),
                                fontWeight = if (dynamic) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        leadingIcon = if (dynamic) {
                            { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    ChoiceRow(
                        choices = ClockBackgroundPattern.entries.map { it to stringResource(it.labelRes) },
                        selected = settings.clockBackgroundPattern,
                        onSelected = { value -> onUpdate { it.copy(clockBackgroundPattern = value) } },
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.background_color)) {
                    ChoiceRow(
                        choices = ClockBackgroundColorMode.entries.map { it to stringResource(it.labelRes) },
                        selected = settings.clockBackgroundColorMode,
                        onSelected = { value -> onUpdate { it.copy(clockBackgroundColorMode = value) } },
                    )
                    if (settings.clockBackgroundColorMode == ClockBackgroundColorMode.COLORFUL) {
                        ColorfulPaletteControls(settings, onUpdate)
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.clock_style)) {
                    ClockStyleCategory.entries.forEach { category ->
                        SectionHeading(stringResource(category.labelRes))
                        StyleGrid(
                            styles = ClockStyle.entries.filter { it.category == category },
                            selected = settings.clockStyle,
                            onSelected = { style -> onUpdate { it.copy(clockStyle = style) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
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
                    Text(label, fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal)
                },
                leadingIcon = if (value == selected) {
                    { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) }
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
private fun FontSizeChoices(selected: Int, onSelected: (Int) -> Unit) {
    ChoiceRow(
        choices = listOf(
            1 to "1 (特小)",
            2 to "2 (小)",
            3 to "3 (標準)",
            4 to "4 (大)",
            5 to "5 (特大)",
        ),
        selected = selected,
        onSelected = onSelected,
    )
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    label: String,
    checkColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (selected) {
                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, null, tint = checkColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorfulPaletteControls(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = {
            onUpdate {
                val palette = generateRandomRainbowColors()
                it.copy(
                    randomRainbowColors = palette,
                    savedRainbowThemes = it.savedRainbowThemes + listOf(palette),
                )
            }
        },
        label = { Text(stringResource(R.string.generate_random_theme)) },
        leadingIcon = { Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp)) },
    )
    if (settings.savedRainbowThemes.isNotEmpty()) {
        Text(
            stringResource(R.string.saved_themes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            settings.savedRainbowThemes.forEachIndexed { index, palette ->
                val current = settings.randomRainbowColors == palette
                Surface(
                    onClick = { onUpdate { it.copy(randomRainbowColors = palette) } },
                    shape = RoundedCornerShape(12.dp),
                    border = if (current) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .background(Brush.linearGradient(palette.map(::Color)))
                            .padding(start = 10.dp, end = 2.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.saved_theme_name, index + 1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(
                            onClick = {
                                onUpdate {
                                    it.copy(savedRainbowThemes = it.savedRainbowThemes.toMutableList().apply {
                                        removeAt(index)
                                    })
                                }
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                stringResource(R.string.delete_saved_theme),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleGrid(
    styles: List<ClockStyle>,
    selected: ClockStyle,
    onSelected: (ClockStyle) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columnCount = styleGridColumnCount(maxWidth.value)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            styles.chunked(columnCount).forEach { rowStyles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowStyles.forEach { style ->
                        StyleCard(
                            style = style,
                            selected = selected == style,
                            onClick = { onSelected(style) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columnCount - rowStyles.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

internal fun styleGridColumnCount(availableWidthDp: Float): Int =
    if (availableWidthDp < 260f) 1 else 2

@Composable
private fun StyleCard(
    style: ClockStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(style.labelRes)
    Card(
        modifier = modifier
            .heightIn(min = 128.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                this.selected = selected
                role = Role.RadioButton
                onClick {
                    onClick()
                    true
                }
            },
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ClockStyleDisplay(
                        text = "10:08",
                        style = style,
                        size = if (style.isAnalog) 66f else 30f,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun contrastColor(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance < 0.5f) Color.White else Color.Black
}
