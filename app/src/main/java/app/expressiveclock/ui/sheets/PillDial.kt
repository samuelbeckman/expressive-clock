package app.expressiveclock.ui.sheets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePickerSelectionMode
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.expressiveclock.ui.theme.ClockFace
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** A full circular alarm-time dial, intentionally matching the stopwatch face. */
@Composable
fun PillDial(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    onHourPicked: () -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val minutes = state.selection == TimePickerSelectionMode.Minute
    val rings = if (!minutes && state.is24hour) 2 else 1
    val dialColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .74f)
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val labelStyle = remember(density) {
        ClockFace.numerals(capHeight = 15.dp, width = 105f, weight = 520, tabular = false)
    }
    val label = if (minutes) "minutes" else "hour"

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .93f), androidx.compose.foundation.shape.CircleShape)
            .border(1.25.dp, Color.White.copy(alpha = .86f), androidx.compose.foundation.shape.CircleShape)
            .semantics {
                contentDescription = "Pick the $label. Currently " +
                    if (minutes) "${state.minute} minutes" else "${state.hour} o'clock"
            }
            .pointerInput(minutes, rings, state.is24hour) {
                detectTapGestures { p -> applyCircular(state, p, size.width.toFloat(), size.height.toFloat(), rings, minutes, onHourPicked) }
            }
            .pointerInput(minutes, rings, state.is24hour) {
                detectDragGestures(
                    onDragEnd = { if (!minutes) onHourPicked() },
                ) { change, _ ->
                    applyCircular(state, change.position, size.width.toFloat(), size.height.toFloat(), rings, minutes, null)
                }
            },
    ) {
        val diameter = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = diameter / 2f
        drawCircle(color = dialColor, radius = radius)

        val ringStep = radius * .19f
        val selectedRadius = if (rings == 2 && state.hour >= 12) radius * .57f else radius * .72f
        val selectedAngle = if (minutes) state.minute * PI.toFloat() / 30f else (state.hour % 12) * PI.toFloat() / 6f
        val tip = Offset(
            center.x + sin(selectedAngle) * selectedRadius,
            center.y - cos(selectedAngle) * selectedRadius,
        )
        drawLine(accent, center, tip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(accent, 4.dp.toPx(), center)
        drawCircle(accent, 16.dp.toPx(), tip)

        if (!minutes) {
            val outerR = radius * .72f
            for (i in 0 until 12) {
                val value = if (i == 0) 12 else i
                val angle = i * PI.toFloat() / 6f
                drawDialLabel(
                    value.toString(),
                    center,
                    outerR,
                    angle,
                    measurer,
                    labelStyle,
                    if (value == currentHourValue(state)) onAccent else ink,
                    value == currentHourValue(state),
                )
            }
            if (rings == 2) {
                val innerR = radius * .53f
                for (i in 0 until 12) {
                    val value = if (i == 0) 24 else i + 12
                    val angle = i * PI.toFloat() / 6f
                    drawDialLabel(
                        if (value == 24) "00" else value.toString(),
                        center,
                        innerR,
                        angle,
                        measurer,
                        labelStyle,
                        if (valueMatchesHour(state.hour, value)) onAccent else ink,
                        valueMatchesHour(state.hour, value),
                    )
                }
            }
        } else {
            val minuteValue = state.minute
            val outerR = radius * .72f
            for (i in 0 until 12) {
                val value = i * 5
                val angle = i * PI.toFloat() / 6f
                drawDialLabel(
                    "%02d".format(value),
                    center,
                    outerR,
                    angle,
                    measurer,
                    labelStyle,
                    if (value == minuteValue) onAccent else ink,
                    value == minuteValue,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDialLabel(
    text: String,
    center: Offset,
    radius: Float,
    angle: Float,
    measurer: androidx.compose.ui.text.TextMeasurer,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    selected: Boolean,
) {
    val laid = measurer.measure(text, style)
    val x = center.x + sin(angle) * radius - laid.size.width / 2f
    val y = center.y - cos(angle) * radius - laid.size.height / 2f
    drawText(laid, color, Offset(x, y))
}

private fun currentHourValue(state: TimePickerState): Int = if (state.is24hour) state.hour else (state.hour % 12).let { if (it == 0) 12 else it }

private fun valueMatchesHour(hour: Int, value: Int): Boolean = when (value) {
    24 -> hour == 0
    else -> hour == value
}

private fun applyCircular(
    state: TimePickerState,
    at: Offset,
    w: Float,
    h: Float,
    rings: Int,
    minutes: Boolean,
    onHourPicked: (() -> Unit)?,
) {
    val centerX = w / 2f
    val centerY = h / 2f
    val dx = at.x - centerX
    val dy = at.y - centerY
    if (dx * dx + dy * dy < 1f) return

    var turns = atan2(dx, -dy) / (2f * PI.toFloat())
    if (turns < 0f) turns += 1f
    val index = ((turns * 12f) + .5f).toInt() % 12

    if (minutes) {
        state.minute = index * 5
        return
    }

    if (rings == 2 && sqrt(dx * dx + dy * dy) < (minOf(w, h) / 2f) * .64f) {
        state.hour = if (index == 0) 0 else index + 12
    } else {
        state.hour = if (index == 0) 12 else index
    }
    onHourPicked?.invoke()
}
