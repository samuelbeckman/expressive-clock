package app.expressiveclock.ui.sheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.expressiveclock.core.Alarm
import app.expressiveclock.data.WeekStart
import app.expressiveclock.data.order
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    initial: Alarm,
    weekStart: WeekStart,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: ((Long) -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val is24Hour = app.expressiveclock.ui.rememberSystem24HourFormat()

    var hour by rememberSaveable(initial.id) { mutableStateOf(initial.time.hour) }
    var minute by rememberSaveable(initial.id) { mutableStateOf(initial.time.minute) }
    var pm by rememberSaveable(initial.id) { mutableStateOf(initial.time.hour >= 12) }
    var editingHour by rememberSaveable(initial.id) { mutableStateOf(true) }
    var label by rememberSaveable(initial.id) { mutableStateOf(initial.label) }
    var days by remember(initial.id) { mutableStateOf(initial.days) }
    var specificDateStr by rememberSaveable(initial.id) { mutableStateOf(initial.specificDate?.toString()) }
    val specificDate = specificDateStr?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
    var vibrate by rememberSaveable(initial.id) { mutableStateOf(initial.vibrate) }
    var soundUri by rememberSaveable(initial.id) { mutableStateOf(initial.soundUri) }

    val openSoundSelector = rememberAlarmSoundSelector(soundUri) { soundUri = it }

    val soundLabel by androidx.compose.runtime.produceState(initialValue = "Default", key1 = soundUri, key2 = context) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            app.expressiveclock.ui.sheets.alarmSoundTitle(context, soundUri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AlarmSheetShape,
        dragHandle = { AlarmEditorDragHandle() },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    ) {
        AlarmPickerSheetContent(
            modal = true,
            title = "Edit Alarm",
            hour = hour,
            minute = minute,
            is24Hour = is24Hour,
            pm = pm,
            editingHour = editingHour,
            label = label,
            days = days,
            weekOrder = weekStart.order(systemFirstDay()),
            vibrate = vibrate,
            soundLabel = soundLabel,
            showDelete = onDelete != null,
            specificDate = specificDate,
            onSpecificDateChange = { date ->
                specificDateStr = date?.toString()
                if (date != null) days = emptySet()
            },
            onHourChange = { hour = it },
            onMinuteChange = { minute = it },
            onPmChange = { pm = it },
            onEditingHourChange = { editingHour = it },
            onLabelChange = { label = it },
            onToggleDay = { day ->
                specificDateStr = null
                days = if (day in days) days - day else days + day
            },
            onVibrateChange = { vibrate = it },
            onSoundClick = openSoundSelector,
            onCancel = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    onDismiss()
                }
            },
            onSave = {
                val saveHour = if (is24Hour) hour else if (pm) (hour % 12) + 12 else hour % 12
                val updated = initial.copy(
                    time = LocalTime.of(saveHour, minute),
                    label = label.trim(),
                    days = days,
                    specificDate = specificDate,
                    enabled = true,
                    vibrate = vibrate,
                    soundUri = soundUri,
                )
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    onSave(updated)
                }
            },
            onDelete = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    onDelete?.invoke(initial.id)
                    onDismiss()
                }
            },
        )
    }
}
