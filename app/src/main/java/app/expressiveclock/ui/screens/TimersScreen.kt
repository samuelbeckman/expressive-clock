package app.expressiveclock.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import app.expressiveclock.core.ClockTimer
import app.expressiveclock.core.TimerState
import app.expressiveclock.data.DEFAULT_TIMER_PRESETS
import app.expressiveclock.data.formatPresetLabel
import app.expressiveclock.ui.glass
import app.expressiveclock.ui.elasticClickable
import app.expressiveclock.ui.theme.ClockFace
import app.expressiveclock.ui.theme.Numerals
import java.time.Duration
import kotlin.math.roundToInt

@Composable
fun TimersScreen(
    timer: ClockTimer?,
    draft: Duration,
    nowElapsedMillis: Long,
    presets: List<Int> = DEFAULT_TIMER_PRESETS,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onResetDraft: () -> Unit,
    onPreset: (Int) -> Unit,
    onStart: () -> Unit,
    onPauseResume: () -> Unit,
    onAddTen: () -> Unit,
    onCancel: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val active = timer != null
    val remaining = timer?.remaining(nowElapsedMillis) ?: Duration.ZERO
    val isFinished = timer?.state == TimerState.FINISHED || (timer?.state == TimerState.RUNNING && remaining.isZero)
    val running = timer?.state == TimerState.RUNNING && !isFinished

    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                modifier = Modifier.graphicsLayer { clip = false },
                targetState = active,
                transitionSpec = {
                    if (targetState) {
                        slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 420f,
                            ),
                            initialOffsetY = { it / 5 },
                        ) + fadeIn(tween(220)) togetherWith
                            (slideOutVertically(
                                animationSpec = tween(160),
                                targetOffsetY = { -it / 10 },
                            ) + fadeOut(tween(140))) using SizeTransform(clip = false)
                    } else {
                        slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 420f,
                            ),
                            initialOffsetY = { -it / 10 },
                        ) + fadeIn(tween(220)) togetherWith
                            (slideOutVertically(
                                animationSpec = tween(160),
                                targetOffsetY = { it / 5 },
                            ) + fadeOut(tween(140))) using SizeTransform(clip = false)
                    }
                },
                label = "timerBody",
            ) { showingActive ->
                if (showingActive && timer != null) {
                    RunningTimerBody(timer, nowElapsedMillis, isFinished)
                } else {
                    SetTimerBody(
                        draft = draft,
                        presets = presets,
                        onDigit = onDigit,
                        onBackspace = onBackspace,
                        onResetDraft = onResetDraft,
                        onPreset = onPreset,
                    )
                }
            }
        }

        TimerControls(
            hasTimer = active,
            running = running,
            isFinished = isFinished,
            onStart = onStart,
            onPauseResume = onPauseResume,
            onAddTen = onAddTen,
            onCancel = onCancel,
        )
        Spacer(Modifier.height(10.dp))
    }

}

private fun formatTimer(duration: Duration): String {
    val total = duration.seconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}

@Composable
private fun TickerTimerReadout(
    text: String,
    capHeight: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    scale: Float = 1f,
    spacing: androidx.compose.ui.unit.Dp = 3.dp,
    digitWidth: androidx.compose.ui.unit.Dp = 56.dp,
    colonWidth: androidx.compose.ui.unit.Dp = 15.dp,
    numeralWidth: Float = ClockFace.TIMER_WIDTH,
    autoSize: Boolean = false,
) {
    BoxWithConstraintsCompat(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (autoSize) {
            val availableWidth = maxWidth
            val numDigits = text.count { it != ':' }
            val numColons = text.count { it == ':' }
            val numGaps = (text.length - 1).coerceAtLeast(0)

            val digitWeight = 1.0f
            val colonWeight = 0.28f
            val spacingWeight = 0.08f
            val totalUnits = (numDigits * digitWeight) + (numColons * colonWeight) + (numGaps * spacingWeight)

            val unit = if (totalUnits > 0f) availableWidth / totalUnits else availableWidth
            val computedDigitWidth = unit * digitWeight
            val computedColonWidth = unit * colonWeight
            val computedSpacing = unit * spacingWeight
            val computedCapHeight = (computedDigitWidth.value * 1.32f).dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = text },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(computedSpacing),
            ) {
                text.forEachIndexed { index, char ->
                    TickerCell(
                        char = char,
                        width = if (char == ':') computedColonWidth else computedDigitWidth,
                        capHeight = computedCapHeight,
                        key = index,
                        color = color,
                        numeralWidth = numeralWidth,
                        overshoot = autoSize,
                    )
                }
            }
        } else {
            val density = LocalDensity.current
            val desiredWidth = with(density) {
                (text.count { it != ':' } * digitWidth.toPx()) +
                    (text.count { it == ':' } * colonWidth.toPx()) +
                    ((text.length - 1).coerceAtLeast(0) * spacing.toPx())
            }
            val availableWidth = with(density) { maxWidth.toPx() }
            val fitScale = if (desiredWidth > 0f) (availableWidth / desiredWidth).coerceAtMost(1f) else 1f

            Row(
                modifier = Modifier
                    .graphicsLayer {
                        val effective = fitScale * scale
                        scaleX = effective
                        scaleY = effective
                    }
                    .clearAndSetSemantics { contentDescription = text },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                text.forEachIndexed { index, char ->
                    TickerCell(
                        char = char,
                        width = if (char == ':') colonWidth else digitWidth,
                        capHeight = capHeight,
                        key = index,
                        color = color,
                        numeralWidth = numeralWidth,
                        overshoot = autoSize,
                    )
                }
            }
        }
    }
}

/**
 * A compatibility wrapper that keeps the timer readout's measuring logic local without changing
 * the surrounding screen API.
 */
@Composable
private fun BoxWithConstraintsCompat(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment,
    content: @Composable androidx.compose.foundation.layout.BoxWithConstraintsScope.() -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier,
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
private fun TickerCell(
    char: Char,
    width: androidx.compose.ui.unit.Dp,
    capHeight: androidx.compose.ui.unit.Dp,
    key: Int,
    color: Color,
    numeralWidth: Float,
    overshoot: Boolean,
) {
    androidx.compose.animation.AnimatedContent(
        modifier = Modifier
            .width(width)
            .height(capHeight + 16.dp),
        targetState = char,
        transitionSpec = {
            val oldDigit = initialState.digitToIntOrNull()
            val newDigit = targetState.digitToIntOrNull()
            if (oldDigit == null || newDigit == null) {
                fadeIn(tween(100)) togetherWith fadeOut(tween(70)) using SizeTransform(clip = true)
            } else {
                val increasing = newDigit > oldDigit
                val incomingDirection = if (increasing) 1 else -1
                val outgoingDirection = -incomingDirection
                val incoming = if (overshoot) {
                    slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = 520f,
                        ),
                        initialOffsetY = { it * incomingDirection },
                    ) + scaleIn(
                        initialScale = 0.90f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = 520f,
                        ),
                    )
                } else {
                    slideInVertically(
                        animationSpec = tween(145),
                        initialOffsetY = { it * incomingDirection },
                    )
                }

                val outgoing = if (overshoot) {
                    slideOutVertically(
                        animationSpec = tween(125),
                        targetOffsetY = { it * outgoingDirection },
                    ) + scaleOut(
                        targetScale = 0.96f,
                        animationSpec = tween(125),
                    )
                } else {
                    slideOutVertically(
                        animationSpec = tween(125),
                        targetOffsetY = { it * outgoingDirection },
                    )
                }

                incoming + fadeIn(tween(90)) togetherWith
                    (outgoing + fadeOut(tween(70))) using SizeTransform(clip = true)
            }
        },
        label = "timerTicker-$key",
    ) { value ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (value == ':') {
                Text(
                    ":",
                    fontFamily = ClockFace.family(opticalSize = 36f, width = 100f, weight = 500),
                    fontSize = (capHeight.value * 0.65f).sp,
                    color = color,
                )
            } else {
                Numerals(
                    text = value.toString(),
                    capHeight = capHeight,
                    color = color,
                    width = numeralWidth,
                    weight = ClockFace.TIMER_WEIGHT,
                    tracking = 0.008f,
                    slashedZero = false,
                )
            }
        }
    }
}

@Composable
private fun SetTimerBody(
    draft: Duration,
    presets: List<Int>,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onResetDraft: () -> Unit,
    onPreset: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        TickerTimerReadout(
            text = formatTimer(draft),
            autoSize = true,
            numeralWidth = 82f,
        )
        Spacer(Modifier.height(6.dp))
        Keypad(onDigit = onDigit, onBackspace = onBackspace, onResetDraft = onResetDraft)
        Spacer(Modifier.height(18.dp))
        Presets(presets = presets, onPreset = onPreset)
    }
}

@Composable
private fun Presets(
    presets: List<Int>,
    onPreset: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            "Presets",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            presets.forEach { seconds ->
                Box(Modifier.weight(1f)) {
                    PresetButton(
                        seconds = seconds,
                        onClick = { onPreset(seconds) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetButton(seconds: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .92f))
            .border(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f), CircleShape)
            .elasticClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            formatPresetLabel(seconds),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, onResetDraft: () -> Unit) {
    val view = LocalView.current
    val rows = listOf("123", "456", "789")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { c -> DigitKey(c, onDigit, Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Spacer(Modifier.weight(1f))
            DigitKey('0', onDigit, Modifier.weight(1f))
            KeyBox(
                modifier = Modifier.weight(1f),
                onClick = {
                    onBackspace()
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                },
                onLongClick = {
                    onResetDraft()
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                },
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "Delete",
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Char, onDigit: (Char) -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    KeyBox(modifier = modifier, onClick = {
        onDigit(digit)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }, label = digit.toString()) {
        Text(
            digit.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = ClockFace.family(opticalSize = 30f, width = ClockFace.NORMAL, weight = 500),
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun KeyBox(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    label: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .height(76.dp)
            .padding(2.dp)
            .glass(CircleShape, container.copy(alpha = .88f), .84f, shadowElevation = 0.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun RunningTimerBody(
    timer: ClockTimer,
    nowElapsedMillis: Long,
    isFinished: Boolean = false,
) {
    val remaining = if (isFinished) Duration.ZERO else timer.remaining(nowElapsedMillis)
    val progress = if (isFinished) 1f else timer.fractionLeft(nowElapsedMillis).coerceIn(0f, 1f)
    val size = 420.dp
    val surface = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .96f)
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints {
            val circleSize = minOf(maxWidth + 24.dp, size)
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(surface)
                    .border(1.25.dp, MaterialTheme.colorScheme.outline.copy(alpha = .12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                ExpressiveTimerRing(progress = progress, color = primary, running = timer.state == TimerState.RUNNING || isFinished)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TickerTimerReadout(
                        text = formatTimer(remaining),
                        capHeight = 49.dp,
                        scale = 0.98f,
                        spacing = 1.5.dp,
                        digitWidth = 47.dp,
                        colonWidth = 13.dp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isFinished) "TIME'S UP" else when (timer.state) {
                            TimerState.PAUSED -> "PAUSED"
                            TimerState.RUNNING -> "TIMER"
                            else -> "READY"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFinished) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f),
                        letterSpacing = 1.8.sp,
                    )
                }
            }
        }
    }
}

/** Material Expressive-inspired organic progress ring. */
@Composable
private fun ExpressiveTimerRing(progress: Float, color: Color, running: Boolean) {
    val phase = remember { Animatable(0f) }
    val waveFraction by animateFloatAsState(
        targetValue = if (running) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 360f,
        ),
        label = "timerWaveAmount",
    )

    // Keep the phase moving only while running. When paused, the phase coroutine is cancelled
    // but the wave amount eases smoothly back to a flat circle. Resuming reverses that transition.
    LaunchedEffect(running) {
        if (running) {
            while (true) {
                phase.animateTo(
                    targetValue = phase.value + (Math.PI * 2).toFloat(),
                    animationSpec = tween(5200, easing = LinearEasing),
                )
            }
        }
    }

    Canvas(Modifier.fillMaxSize().padding(10.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = (size.minDimension / 2f) - 14.dp.toPx()
        val amplitude = 8.dp.toPx() * waveFraction
        val activeSweep = 360f * progress

        val backgroundPath = buildSquigglePath(
            center = center,
            radius = baseRadius,
            startDegrees = 0f,
            sweepDegrees = 360f,
            amplitude = amplitude,
            phase = phase.value,
        )
        val activePath = buildSquigglePath(
            center = center,
            radius = baseRadius,
            startDegrees = 0f,
            sweepDegrees = activeSweep,
            amplitude = amplitude,
            phase = phase.value,
        )

        drawPath(
            path = backgroundPath,
            color = color.copy(alpha = .10f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 11.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )
        if (progress > 0f) {
            drawPath(
                path = activePath,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 11.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
        }
    }
}

private fun buildSquigglePath(
    center: Offset,
    radius: Float,
    startDegrees: Float,
    sweepDegrees: Float,
    amplitude: Float,
    phase: Float,
): Path {
    val path = Path()
    val steps = maxOf(96, (kotlin.math.abs(sweepDegrees) / 1.25f).toInt())

    fun point(degrees: Float): Offset {
        val angle = Math.toRadians((degrees - 90f).toDouble())
        // Tight, expressive waves with several nearby frequencies so the outline feels organic
        // without developing a visible seam.
        val primary = kotlin.math.sin(angle * 16.0 + phase)
        val secondary = kotlin.math.sin(angle * 11.0 - phase * 0.68) * 0.34
        val tertiary = kotlin.math.sin(angle * 23.0 + phase * 1.30) * 0.13
        val wave = amplitude * (primary + secondary + tertiary)
        val r = radius + wave.toFloat()
        return Offset(
            center.x + kotlin.math.cos(angle).toFloat() * r,
            center.y + kotlin.math.sin(angle).toFloat() * r,
        )
    }

    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val degrees = startDegrees + sweepDegrees * t
        val p = point(degrees)
        if (i == 0) {
            path.moveTo(p.x, p.y)
        } else {
            val previousDegrees = startDegrees + sweepDegrees * ((i - 1) / steps.toFloat())
            val previous = point(previousDegrees)
            val midpoint = Offset((previous.x + p.x) / 2f, (previous.y + p.y) / 2f)
            path.quadraticBezierTo(previous.x, previous.y, midpoint.x, midpoint.y)
            if (i == steps) path.lineTo(p.x, p.y)
        }
    }
    if (sweepDegrees >= 359f) path.close()
    return path
}

@Composable
private fun TimerControls(
    hasTimer: Boolean,
    running: Boolean,
    isFinished: Boolean = false,
    onStart: () -> Unit,
    onPauseResume: () -> Unit,
    onAddTen: () -> Unit,
    onCancel: () -> Unit,
) {
    if (isFinished) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TIMER_ROW_HEIGHT)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidePill(
                text = "Stop",
                icon = Icons.Rounded.Close,
                onClick = onCancel,
                container = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.88f),
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                height = TIMER_ROW_HEIGHT,
                compact = true,
                fillWidth = true,
                glow = false,
            )
        }
        return
    }
    // Intentionally mirrors the Stopwatch control morph so the Timer page uses the exact same
    // expansion rhythm, spacing, and button choreography. The expanded row is simply Pause/Resume,
    // +10s, Cancel instead of Lap/Reset/Stop.
    val expanded = hasTimer
    val expand by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "timerControlsExpand",
    )
    val label by animateFloatAsState(
        targetValue = if (expanded) 0f else 1f,
        animationSpec = tween(durationMillis = TIMER_LABEL_FADE_MS),
        label = "timerControlsLabel",
    )
    val split by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = TIMER_SPLIT_MS,
            delayMillis = if (expanded) TIMER_EXPAND_LEAD_MS else 0,
        ),
        label = "timerControlsSplit",
    )
    val e = expand.coerceIn(0f, 1f)

    SubcomposeLayout(
        Modifier.fillMaxWidth().height(TIMER_ROW_HEIGHT).padding(horizontal = 20.dp),
    ) { constraints ->
        val full = constraints.maxWidth
        val h = TIMER_ROW_HEIGHT.roundToPx()

        val rowWidth = subcompose("timerRowProbe") {
            DigitRow(
                part = "00",
                capHeight = 66.dp,
                color = MaterialTheme.colorScheme.primary,
                separator = true,
            )
        }.first().measure(Constraints()).width

        val natural = subcompose("timerStartProbe") {
            WidePill(
                text = "Start",
                icon = Icons.Rounded.PlayArrow,
                onClick = {},
                height = TIMER_ROW_HEIGHT,
                compact = true,
                fillWidth = false,
                contentPaddingH = 28.dp,
                glow = false,
            )
        }.first().measure(Constraints(minHeight = h, maxHeight = h)).width

        val collapsedW = maxOf(rowWidth - TIMER_ROW_INSET.roundToPx(), natural).coerceAtMost(full)
        val collapsedX = ((full - collapsedW) / 2).coerceAtLeast(0)
        val w = lerp(collapsedW, full, e)
        val x = lerp(collapsedX, 0, e)

        val single = if (split < 1f) {
            subcompose("timerSingle") {
                Box(Modifier.graphicsLayer { alpha = 1f - split }) {
                    WidePill(
                        text = "Start",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onStart,
                        container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f),
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        height = TIMER_ROW_HEIGHT,
                        compact = true,
                        contentPaddingH = 28.dp,
                        contentAlpha = label,
                        glow = false,
                    )
                }
            }.first().measure(Constraints.fixed(w, h))
        } else null

        val trio = if (split > 0f) {
            subcompose("timerTrio") {
                Box(Modifier.graphicsLayer { alpha = split }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(TIMER_GAP * split)) {
                        Box(Modifier.weight(1f)) {
                            WidePill(
                                text = if (running) "Pause" else "Resume",
                                icon = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                onClick = onPauseResume,
                                height = TIMER_ROW_HEIGHT,
                                compact = true,
                                glow = false,
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WidePill(
                                text = "+10s",
                                icon = Icons.Rounded.Add,
                                onClick = onAddTen,
                                height = TIMER_ROW_HEIGHT,
                                compact = true,
                                glow = false,
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WidePill(
                                text = "Cancel",
                                icon = Icons.Rounded.Close,
                                onClick = onCancel,
                                height = TIMER_ROW_HEIGHT,
                                outlined = true,
                                content = MaterialTheme.colorScheme.onSurface,
                                compact = true,
                                glow = false,
                            )
                        }
                    }
                }
            }.first().measure(Constraints.fixed(full, h))
        } else null

        layout(full, h) {
            single?.place(x, 0)
            trio?.place(0, 0)
        }
    }
}

private val TIMER_GAP = 6.dp
private val TIMER_ROW_HEIGHT = 56.dp
private val TIMER_ROW_INSET = 10.dp
private const val TIMER_EXPAND_LEAD_MS = 95
private const val TIMER_SPLIT_MS = 150
private const val TIMER_LABEL_FADE_MS = 70

