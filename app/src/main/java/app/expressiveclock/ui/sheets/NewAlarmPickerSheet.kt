package app.expressiveclock.ui.sheets

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.expressiveclock.core.Alarm
import app.expressiveclock.data.WeekStart
import app.expressiveclock.data.order
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAlarmPickerSheet(
    initial: Alarm,
    weekStart: WeekStart,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

    var hour by rememberSaveable { mutableStateOf(initial.time.hour) }
    var minute by rememberSaveable { mutableStateOf(initial.time.minute) }
    var pm by rememberSaveable { mutableStateOf(initial.time.hour >= 12) }
    var editingHour by rememberSaveable { mutableStateOf(true) }
    var label by rememberSaveable { mutableStateOf(initial.label.ifBlank { "Alarm" }) }
    var days by remember { mutableStateOf(initial.days) }
    var vibrate by rememberSaveable { mutableStateOf(initial.vibrate) }
    var soundUri by rememberSaveable { mutableStateOf(initial.soundUri) }

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            soundUri = picked?.toString() ?: SILENT
        }
    }

    val soundLabel = remember(soundUri) {
        when {
            soundUri == null -> "Default"
            soundUri == SILENT -> "Silent"
            else -> runCatching { RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context) }
                .getOrNull() ?: "Default"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AlarmSheetShape,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    ) {
        AlarmPickerSheetContent(
            title = "New Alarm",
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
            showDelete = false,
            onHourChange = { hour = it },
            onMinuteChange = { minute = it },
            onPmChange = { pm = it },
            onEditingHourChange = { editingHour = it },
            onLabelChange = { label = it },
            onToggleDay = { day -> days = if (day in days) days - day else days + day },
            onVibrateChange = { vibrate = it },
            onSoundClick = {
                ringtonePicker.launch(
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundUri?.takeIf { it != SILENT }?.let(Uri::parse))
                )
            },
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
            onDelete = {},
        )
    }
}
