package com.simpleclock.app.ui.screens

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.simpleclock.app.AlarmEnableSuccess
import com.simpleclock.app.AlarmSaveError
import com.simpleclock.app.AlarmSaveState
import com.simpleclock.app.R
import com.simpleclock.app.alarm.AlarmOccurrenceDisplay
import com.simpleclock.app.alarm.AlarmScheduleDelay
import com.simpleclock.app.alarm.AlarmTimeCalculator
import com.simpleclock.app.alarm.alarmOccurrenceDisplay
import com.simpleclock.app.alarm.alarmScheduleDelay
import com.simpleclock.app.data.ALARM_COLOR_HUES
import com.simpleclock.app.data.ALARM_COLORS
import com.simpleclock.app.data.AlarmEntity
import com.simpleclock.app.data.alarmColorFromHue
import com.simpleclock.app.data.alarmHueFromColor
import com.simpleclock.app.ui.components.HueSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<AlarmEntity>,
    saveState: AlarmSaveState,
    enableSuccess: AlarmEnableSuccess?,
    onBack: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onEnabledChange: (AlarmEntity, Boolean) -> Unit,
    onDelete: (AlarmEntity) -> Unit,
    onSaveResultConsumed: () -> Unit,
    onEnableSuccessConsumed: () -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var editorAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var displayedAlarms by remember { mutableStateOf(alarms) }
    var draggedAlarmId by remember { mutableStateOf<Long?>(null) }
    var draggedOffset by remember { mutableFloatStateOf(0f) }
    var pendingOrder by remember { mutableStateOf<List<Long>?>(null) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    val nextAlarmId = remember(displayedAlarms, now) {
        displayedAlarms
            .filter { it.enabled }
            .minByOrNull { AlarmTimeCalculator.nextOccurrence(it, now).toInstant() }
            ?.id
    }
    val showSnackbar: (String) -> Unit = { message ->
        snackbarJob.value?.cancel()
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarJob.value = coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(MINUTE_MILLIS - System.currentTimeMillis() % MINUTE_MILLIS + 20L)
        }
    }

    LaunchedEffect(alarms, draggedAlarmId, pendingOrder) {
        if (draggedAlarmId == null) {
            val receivedOrder = alarms.map { it.id }
            if (pendingOrder == null || pendingOrder == receivedOrder) {
                displayedAlarms = alarms
                pendingOrder = null
            }
        }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            AlarmSaveState.Idle -> Unit
            is AlarmSaveState.RequestingPermission -> {
                if (editorAlarm == null) editorAlarm = saveState.draft
            }
            is AlarmSaveState.Saving -> {
                if (editorAlarm == null) editorAlarm = saveState.draft
            }
            is AlarmSaveState.Error -> {
                if (editorAlarm == null) editorAlarm = saveState.draft
            }
            is AlarmSaveState.Success -> {
                editorAlarm = null
                val message = saveState.triggerAtMillis?.let { triggerAt ->
                    alarmScheduledMessage(context, triggerAt)
                } ?: context.getString(R.string.alarm_saved_disabled)
                onSaveResultConsumed()
                showSnackbar(message)
            }
        }
    }

    LaunchedEffect(enableSuccess) {
        enableSuccess?.let { success ->
            val message = alarmScheduledMessage(context, success.triggerAtMillis)
            onEnableSuccessConsumed()
            showSnackbar(message)
        }
    }

    val moveAlarm: (Long, Int) -> Boolean = { alarmId, direction ->
        val from = displayedAlarms.indexOfFirst { it.id == alarmId }
        val to = from + direction
        if (from < 0 || to !in displayedAlarms.indices) {
            false
        } else {
            val reordered = displayedAlarms.toMutableList().apply {
                add(to, removeAt(from))
            }.mapIndexed { index, alarm -> alarm.copy(sortOrder = index.toLong()) }
            displayedAlarms = reordered
            pendingOrder = reordered.map { it.id }
            onReorder(pendingOrder.orEmpty())
            true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alarms)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val initialTime = LocalTime.now().plusMinutes(5)
                            editorAlarm = AlarmEntity(
                                hour = initialTime.hour,
                                minute = initialTime.minute,
                                label = "",
                                enabled = true,
                            )
                        },
                    ) {
                        Text(stringResource(R.string.add_alarm_action), fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (displayedAlarms.isEmpty()) {
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
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(displayedAlarms, key = { it.id }) { alarm ->
                    val isDragging = draggedAlarmId == alarm.id
                    AlarmCard(
                        alarm = alarm,
                        now = now,
                        isNextAlarm = alarm.id == nextAlarmId,
                        modifier = Modifier
                            .animateItem()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) draggedOffset else 0f },
                        onClick = { editorAlarm = alarm },
                        onEnabledChange = { enabled -> onEnabledChange(alarm, enabled) },
                        onMove = { direction -> moveAlarm(alarm.id, direction) },
                        onDragStart = {
                            draggedAlarmId = alarm.id
                            draggedOffset = 0f
                        },
                        onDrag = { delta ->
                            draggedOffset += delta
                            val currentInfo = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.key == draggedAlarmId }
                            if (currentInfo != null) {
                                val draggedCenter = currentInfo.offset + currentInfo.size / 2f + draggedOffset
                                val targetInfo = listState.layoutInfo.visibleItemsInfo.minByOrNull { info ->
                                    abs(draggedCenter - (info.offset + info.size / 2f))
                                }
                                if (targetInfo != null && targetInfo.key != draggedAlarmId) {
                                    val from = displayedAlarms.indexOfFirst { it.id == draggedAlarmId }
                                    val targetId = targetInfo.key as? Long
                                    val to = displayedAlarms.indexOfFirst { it.id == targetId }
                                    if (from >= 0 && to >= 0) {
                                        displayedAlarms = displayedAlarms.toMutableList().apply {
                                            add(to, removeAt(from))
                                        }.mapIndexed { index, item -> item.copy(sortOrder = index.toLong()) }
                                        draggedOffset += currentInfo.offset - targetInfo.offset
                                    }
                                }

                                val viewportStart = listState.layoutInfo.viewportStartOffset + 72f
                                val viewportEnd = listState.layoutInfo.viewportEndOffset - 72f
                                val scrollAmount = when {
                                    draggedCenter < viewportStart -> -24f
                                    draggedCenter > viewportEnd -> 24f
                                    else -> 0f
                                }
                                if (scrollAmount != 0f) {
                                    coroutineScope.launch { listState.scrollBy(scrollAmount) }
                                }
                            }
                        },
                        onDragEnd = {
                            if (draggedAlarmId != null) {
                                pendingOrder = displayedAlarms.map { it.id }
                                onReorder(pendingOrder.orEmpty())
                            }
                            draggedAlarmId = null
                            draggedOffset = 0f
                        },
                    )
                }
            }
        }
    }

    editorAlarm?.let { alarm ->
        val isSaving = saveState is AlarmSaveState.Saving || saveState is AlarmSaveState.RequestingPermission
        val saveError = (saveState as? AlarmSaveState.Error)?.takeIf { it.draft.id == alarm.id }?.reason
        AlarmEditorDialog(
            alarm = alarm,
            isNew = alarm.id == 0L,
            isSaving = isSaving,
            saveError = saveError,
            onDismiss = {
                if (!isSaving) {
                    editorAlarm = null
                    onSaveResultConsumed()
                }
            },
            onSave = onSave,
            onDelete = if (alarm.id == 0L) null else {
                {
                    onDelete(alarm)
                    editorAlarm = null
                    onSaveResultConsumed()
                }
            },
        )
    }
}

private fun alarmScheduledMessage(
    context: Context,
    triggerAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String = when (val delay = alarmScheduleDelay(triggerAtMillis, nowMillis)) {
    AlarmScheduleDelay.Imminent -> context.getString(R.string.alarm_scheduled_imminent)
    is AlarmScheduleDelay.Minutes -> context.getString(
        R.string.alarm_scheduled_in_minutes,
        delay.minutes,
    )
    is AlarmScheduleDelay.HoursMinutes -> if (delay.minutes == 0L) {
        context.getString(R.string.alarm_scheduled_in_hours, delay.hours)
    } else {
        context.getString(
            R.string.alarm_scheduled_in_hours_minutes,
            delay.hours,
            delay.minutes,
        )
    }
    AlarmScheduleDelay.Absolute -> {
        val trigger = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val date = if (trigger.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            context.getString(
                R.string.alarm_date_same_year,
                trigger.get(Calendar.MONTH) + 1,
                trigger.get(Calendar.DAY_OF_MONTH),
            )
        } else {
            context.getString(
                R.string.alarm_date_other_year,
                trigger.get(Calendar.YEAR),
                trigger.get(Calendar.MONTH) + 1,
                trigger.get(Calendar.DAY_OF_MONTH),
            )
        }
        val time = DateFormat.getTimeFormat(context).format(Date(triggerAtMillis))
        context.getString(R.string.alarm_scheduled_for, date, time)
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    now: ZonedDateTime,
    isNextAlarm: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onMove: (Int) -> Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val context = LocalContext.current
    val formatter = if (DateFormat.is24HourFormat(context)) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
    val alarmColor = Color(alarm.color)
    val cardContainerColor = alarmColor.copy(alpha = if (alarm.enabled) 0.20f else 0.08f)
    val alarmTime = LocalTime.of(alarm.hour, alarm.minute).format(formatter)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = BorderStroke(
            1.5.dp,
            alarmColor.copy(alpha = if (alarm.enabled) 0.55f else 0.20f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 18.dp, end = 6.dp, bottom = 18.dp),
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
                if (isNextAlarm) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(alarmColor.copy(alpha = 0.24f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.next_alarm_indicator),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = alarmTime,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (alarm.enabled) 1f else 0.45f),
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
                Text(
                    text = alarmOccurrenceText(context, alarm, now, alarmTime),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onEnabledChange)
            AlarmDragHandle(
                onMove = onMove,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
            )
        }
    }
}

private fun alarmOccurrenceText(
    context: Context,
    alarm: AlarmEntity,
    now: ZonedDateTime,
    alarmTime: String,
): String {
    val display = alarmOccurrenceDisplay(alarm, now)
    if (display == AlarmOccurrenceDisplay.Disabled) return context.getString(R.string.disabled)

    val locale = context.resources.configuration.locales[0]
    val dayText: String
    val delay: AlarmScheduleDelay
    when (display) {
        AlarmOccurrenceDisplay.Disabled -> error("Handled above")
        is AlarmOccurrenceDisplay.Today -> {
            dayText = context.getString(R.string.alarm_occurrence_today)
            delay = display.delay
        }
        is AlarmOccurrenceDisplay.Tomorrow -> {
            dayText = context.getString(R.string.alarm_occurrence_tomorrow)
            delay = display.delay
        }
        is AlarmOccurrenceDisplay.Weekday -> {
            dayText = display.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            delay = display.delay
        }
        is AlarmOccurrenceDisplay.NextWeek -> {
            dayText = context.getString(
                R.string.alarm_occurrence_next_week,
                display.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
            )
            delay = display.delay
        }
    }

    val delayText = when (delay) {
        AlarmScheduleDelay.Imminent -> context.getString(R.string.alarm_occurrence_imminent)
        is AlarmScheduleDelay.Minutes -> context.getString(
            R.string.alarm_occurrence_in_minutes,
            delay.minutes,
        )
        is AlarmScheduleDelay.HoursMinutes -> if (delay.minutes == 0L) {
            context.getString(R.string.alarm_occurrence_in_hours, delay.hours)
        } else {
            context.getString(
                R.string.alarm_occurrence_in_hours_minutes,
                delay.hours,
                delay.minutes,
            )
        }
        AlarmScheduleDelay.Absolute -> null
    }
    return delayText?.let {
        context.getString(R.string.alarm_occurrence_day_time_and_delay, dayText, alarmTime, it)
    } ?: context.getString(R.string.alarm_occurrence_day_and_time, dayText, alarmTime)
}

@Composable
private fun AlarmDragHandle(
    onMove: (Int) -> Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val dragDescription = stringResource(R.string.drag_to_reorder)
    val moveUp = stringResource(R.string.move_up)
    val moveDown = stringResource(R.string.move_down)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = dragDescription
                customActions = listOf(
                    CustomAccessibilityAction(moveUp) { onMove(-1) },
                    CustomAccessibilityAction(moveDown) { onMove(1) },
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnDragStart() },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(dragAmount.y)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorDialog(
    alarm: AlarmEntity,
    isNew: Boolean,
    isSaving: Boolean,
    saveError: AlarmSaveError?,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    var hour by remember(alarm.id) { mutableIntStateOf(alarm.hour) }
    var minute by remember(alarm.id) { mutableIntStateOf(alarm.minute) }
    var label by remember(alarm.id) { mutableStateOf(alarm.label) }
    var repeatDays by remember(alarm.id) { mutableIntStateOf(alarm.repeatDays) }
    var customHue by remember(alarm.id) { mutableFloatStateOf(alarmHueFromColor(alarm.color)) }
    var selectedPresetIndex by remember(alarm.id) {
        mutableStateOf<Int?>(ALARM_COLORS.indexOf(alarm.color).takeIf { it >= 0 })
    }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedColor = selectedPresetIndex?.let(ALARM_COLORS::get) ?: alarmColorFromHue(customHue)
    val dayLabels = listOf(
        R.string.monday_short,
        R.string.tuesday_short,
        R.string.wednesday_short,
        R.string.thursday_short,
        R.string.friday_short,
        R.string.saturday_short,
        R.string.sunday_short,
    )
    val colorLabels = listOf(
        R.string.theme_coral,
        R.string.theme_tangerine,
        R.string.theme_sunflower,
        R.string.theme_mint,
        R.string.theme_sky,
        R.string.theme_grape,
    )
    val timeFormatter = if (DateFormat.is24HourFormat(context)) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(max = 760.dp)
                .padding(horizontal = 20.dp, vertical = 24.dp),
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
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = LocalTime.of(hour, minute).format(timeFormatter),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.alarm_name)) },
                    singleLine = true,
                    enabled = !isSaving,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.choose_repeat_days),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
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
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        fontWeight = if (repeatDays and bit != 0) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    )
                                },
                                leadingIcon = if (repeatDays and bit != 0) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
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
                                enabled = !isSaving,
                            )
                        }
                    }
                }
                Text(
                    repeatDescription(repeatDays),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.alarm_color),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ALARM_COLORS.forEachIndexed { index, colorHex ->
                        val selected = selectedPresetIndex == index
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !isSaving) {
                                    customHue = ALARM_COLOR_HUES[index]
                                    selectedPresetIndex = index
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .then(
                                        if (selected) Modifier.border(
                                            2.5.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            CircleShape,
                                        ) else Modifier,
                                    )
                                    .semantics {
                                        contentDescription = context.getString(colorLabels[index])
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (isDarkColor(Color(colorHex))) Color.White else Color.Black,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val customSelected = selectedPresetIndex == null
                    val customColor = Color(alarmColorFromHue(customHue))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isSaving) { selectedPresetIndex = null },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(customColor)
                                .then(
                                    if (customSelected) Modifier.border(
                                        2.5.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape,
                                    ) else Modifier,
                                )
                                .semantics {
                                    contentDescription = context.getString(R.string.custom_color)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (customSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (isDarkColor(customColor)) Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    HueSlider(
                        hue = customHue,
                        colorAtHue = { Color(alarmColorFromHue(it)) },
                        label = stringResource(R.string.alarm_color),
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        onHueChange = {
                            customHue = it
                            selectedPresetIndex = null
                        },
                    )
                }

                saveError?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            when (error) {
                                AlarmSaveError.PERMISSION_DENIED -> R.string.alarm_save_permission_error
                                AlarmSaveError.SCHEDULE_FAILED -> R.string.alarm_save_schedule_error
                                AlarmSaveError.SAVE_FAILED -> R.string.alarm_save_general_error
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete, enabled = !isSaving) {
                            Icon(Icons.Rounded.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.delete))
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                alarm.copy(
                                    hour = hour,
                                    minute = minute,
                                    label = label.trim(),
                                    repeatDays = repeatDays,
                                    color = selectedColor,
                                ),
                            )
                        },
                        enabled = !isSaving,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = pickerState.hour
                        minute = pickerState.minute
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = { TimePicker(state = pickerState) },
        )
    }
}

private fun isDarkColor(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance < 0.5f
}

private const val MINUTE_MILLIS = 60_000L

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
