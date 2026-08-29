package com.simpleclock.app.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.simpleclock.app.R
import com.simpleclock.app.data.ALARM_COLORS
import com.simpleclock.app.data.AlarmEntity
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<AlarmEntity>,
    onBack: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onEnabledChange: (AlarmEntity, Boolean) -> Unit,
    onDelete: (AlarmEntity) -> Unit,
) {
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var addingAlarm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alarms)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addingAlarm = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.add_alarm)) },
            )
        },
    ) { padding ->
        if (alarms.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.no_alarms_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.no_alarms_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    top = 12.dp,
                    end = 20.dp,
                    bottom = 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onClick = { editingAlarm = alarm },
                        onEnabledChange = { enabled -> onEnabledChange(alarm, enabled) },
                    )
                }
            }
        }
    }

    if (addingAlarm) {
        val initialTime = remember { LocalTime.now().plusMinutes(5) }
        AlarmEditorDialog(
            alarm = AlarmEntity(
                hour = initialTime.hour,
                minute = initialTime.minute,
                label = "",
            ),
            isNew = true,
            onDismiss = { addingAlarm = false },
            onSave = {
                onSave(it)
                addingAlarm = false
            },
            onDelete = null,
        )
    }

    editingAlarm?.let { alarm ->
        AlarmEditorDialog(
            alarm = alarm,
            isNew = false,
            onDismiss = { editingAlarm = null },
            onSave = {
                onSave(it)
                editingAlarm = null
            },
            onDelete = {
                onDelete(alarm)
                editingAlarm = null
            },
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val formatter = if (DateFormat.is24HourFormat(context)) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
    val alarmColor = Color(alarm.color)
    val cardContainerColor = if (alarm.enabled) {
        alarmColor.copy(alpha = 0.20f)
    } else {
        alarmColor.copy(alpha = 0.08f)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
        ),
        border = BorderStroke(
            1.5.dp,
            if (alarm.enabled) alarmColor.copy(alpha = 0.55f) else alarmColor.copy(alpha = 0.20f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(48.dp)
                    .background(
                        if (alarm.enabled) alarmColor else alarmColor.copy(alpha = 0.35f),
                        RoundedCornerShape(3.dp),
                    ),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = LocalTime.of(alarm.hour, alarm.minute).format(formatter),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.enabled) alarmColor
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
                Text(
                    text = alarm.label.ifBlank { stringResource(R.string.alarm_name_default) },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = repeatDescription(alarm.repeatDays),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorDialog(
    alarm: AlarmEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    val pickerState = rememberTimePickerState(
        initialHour = alarm.hour,
        initialMinute = alarm.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    var label by remember(alarm.id) { mutableStateOf(alarm.label) }
    var repeatDays by remember(alarm.id) { mutableIntStateOf(alarm.repeatDays) }
    var selectedColor by remember(alarm.id) { mutableLongStateOf(alarm.color) }
    var showCustomColorDialog by remember { mutableStateOf(false) }

    val dayLabels = listOf(
        R.string.monday_short,
        R.string.tuesday_short,
        R.string.wednesday_short,
        R.string.thursday_short,
        R.string.friday_short,
        R.string.saturday_short,
        R.string.sunday_short,
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(if (isNew) R.string.add_alarm else R.string.edit_alarm),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                TimePicker(state = pickerState)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.alarm_name)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.choose_repeat_days),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                dayLabels.chunked(4).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEachIndexed { columnIndex, labelRes ->
                            val dayIndex = rowIndex * 4 + columnIndex
                            val bit = 1 shl dayIndex
                            FilterChip(
                                selected = repeatDays and bit != 0,
                                onClick = { repeatDays = repeatDays xor bit },
                                label = { Text(stringResource(labelRes)) },
                            )
                        }
                    }
                }
                Text(
                    repeatDescription(repeatDays),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.alarm_color),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                ALARM_COLORS.chunked(5).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        rowColors.forEach { colorHex ->
                            val color = Color(colorHex)
                            val isSelected = selectedColor == colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = colorHex }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (isDarkColor(color)) Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                val isCustomColorSelected = selectedColor !in ALARM_COLORS
                OutlinedButton(
                    onClick = { showCustomColorDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = if (isCustomColorSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCustomColorSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (isDarkColor(Color(selectedColor))) Color.White else Color.Black,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isCustomColorSelected) {
                            String.format("#%06X (%s)", selectedColor and 0xFFFFFFL, stringResource(R.string.custom_color))
                        } else {
                            stringResource(R.string.custom_color)
                        },
                        fontWeight = if (isCustomColorSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.delete))
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                alarm.copy(
                                    hour = pickerState.hour,
                                    minute = pickerState.minute,
                                    label = label.trim(),
                                    repeatDays = repeatDays,
                                    color = selectedColor,
                                    enabled = true,
                                ),
                            )
                        },
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialColor = selectedColor,
            onDismiss = { showCustomColorDialog = false },
            onColorSelected = {
                selectedColor = it
                showCustomColorDialog = false
            },
        )
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Long,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit,
) {
    val initialRed = ((initialColor shr 16) and 0xFFL).toInt()
    val initialGreen = ((initialColor shr 8) and 0xFFL).toInt()
    val initialBlue = (initialColor and 0xFFL).toInt()

    var red by remember { mutableIntStateOf(initialRed) }
    var green by remember { mutableIntStateOf(initialGreen) }
    var blue by remember { mutableIntStateOf(initialBlue) }
    var hexInput by remember {
        mutableStateOf(String.format("%02X%02X%02X", initialRed, initialGreen, initialBlue))
    }

    val currentColor = Color(red, green, blue)
    val currentColorHex = 0xFF000000L or ((red.toLong() and 0xFFL) shl 16) or ((green.toLong() and 0xFFL) shl 8) or (blue.toLong() and 0xFFL)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.custom_color_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = String.format("#%02X%02X%02X", red, green, blue),
                        color = if (isDarkColor(currentColor)) Color.White else Color.Black,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                        hexInput = filtered.uppercase()
                        if (filtered.length == 6) {
                            try {
                                val parsed = filtered.toInt(16)
                                red = (parsed shr 16) and 0xFF
                                green = (parsed shr 8) and 0xFF
                                blue = parsed and 0xFF
                            } catch (_: Exception) { }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.color_hex)) },
                    prefix = { Text("#") },
                    singleLine = true,
                )
                Spacer(Modifier.height(14.dp))

                ColorSlider(
                    label = stringResource(R.string.color_red),
                    value = red,
                    tint = Color(0xFFFF5252),
                    onValueChange = {
                        red = it
                        hexInput = String.format("%02X%02X%02X", red, green, blue)
                    },
                )
                Spacer(Modifier.height(8.dp))

                ColorSlider(
                    label = stringResource(R.string.color_green),
                    value = green,
                    tint = Color(0xFF4CAF50),
                    onValueChange = {
                        green = it
                        hexInput = String.format("%02X%02X%02X", red, green, blue)
                    },
                )
                Spacer(Modifier.height(8.dp))

                ColorSlider(
                    label = stringResource(R.string.color_blue),
                    value = blue,
                    tint = Color(0xFF2196F3),
                    onValueChange = {
                        blue = it
                        hexInput = String.format("%02X%02X%02X", red, green, blue)
                    },
                )
                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(currentColorHex) }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Int,
    tint: Color,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tint, fontWeight = FontWeight.SemiBold)
            Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = tint,
                activeTrackColor = tint,
            ),
        )
    }
}

private fun isDarkColor(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance < 0.5f
}

@Composable
private fun repeatDescription(mask: Int): String {
    if (mask == 0) return stringResource(R.string.one_time)
    if (mask == 0b1111111) return stringResource(R.string.every_day)
    if (mask == 0b0011111) return stringResource(R.string.weekdays)
    val labels = listOf(
        R.string.monday_short,
        R.string.tuesday_short,
        R.string.wednesday_short,
        R.string.thursday_short,
        R.string.friday_short,
        R.string.saturday_short,
        R.string.sunday_short,
    )
    return labels.mapIndexedNotNull { index, label ->
        stringResource(label).takeIf { mask and (1 shl index) != 0 }
    }.joinToString("、")
}


