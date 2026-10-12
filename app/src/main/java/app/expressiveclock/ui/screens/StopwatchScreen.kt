package app.expressiveclock.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import app.expressiveclock.core.Lap
import app.expressiveclock.ui.glass
import app.expressiveclock.ui.elasticClickable
import app.expressiveclock.ui.theme.ClockFace
import app.expressiveclock.core.Stopwatch
import app.expressiveclock.core.stopwatchParts

/**
 * The stopwatch.
 *
 * Extrapolated: the concept set contains no stopwatch at all, so this reuses the timer's own
 * vocabulary (the stacked hollow digits, the wide pills) rather than importing a different look.
 * The bottom pair of digits is hundredths, which is why the whole screen is driven off a
 * frame-paced ticker instead of a one-second one.
 *
 * The fastest and slowest laps are marked in tertiary and error. That is not in the renders; it is
 * the one addition, on the grounds that a lap list nobody can compare is just a receipt.
 */
@Composable
fun StopwatchScreen(
    stopwatch: Stopwatch,
    nowElapsedMillis: Long,
    onToggle: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val elapsed = stopwatch.elapsed(nowElapsedMillis)
    val fastest = stopwatch.fastest
    val slowest = stopwatch.slowest
    val listState = rememberLazyListState()
    val view = LocalView.current
    var previousRunning by remember { mutableStateOf<Boolean?>(null) }

    val faceScale = remember { Animatable(1f) }
    var previousRunningForScale by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(stopwatch.running) {
        val prevScale = previousRunningForScale
        previousRunningForScale = stopwatch.running
        if (prevScale != null && prevScale != stopwatch.running) {
            faceScale.snapTo(0.95f)
            faceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = 380f,
                ),
            )
        }
    }

    LaunchedEffect(stopwatch.running) {
        val prev = previousRunning
        previousRunning = stopwatch.running
        if (prev != null && prev != stopwatch.running) {
            if (stopwatch.running) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        }
    }

    val handleReset = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        onReset()
    }

    val density = LocalDensity.current
    val totalBottomPadding = contentPadding.calculateBottomPadding() + 140.dp
    var previousLapCount by remember { mutableIntStateOf(stopwatch.laps.size) }

    LaunchedEffect(stopwatch.laps.size) {
        val currentCount = stopwatch.laps.size
        val prevCount = previousLapCount
        previousLapCount = currentCount

        if (currentCount > prevCount) {
            // New lap added: Smooth spring glide down with subtle overshoot
            val lapRowHeightPx = with(density) { (LAP_ROW_HEIGHT + 14.dp).toPx() }
            listState.animateScrollBy(
                value = lapRowHeightPx,
                animationSpec = spring(
                    dampingRatio = 0.68f, // Subtle bouncy overshoot
                    stiffness = 320f,
                ),
            )
        } else if (currentCount == 0 && prevCount > 0) {
            // Reset: Smooth spring glide back up to top (item 0) with exact distance and overshoot
            val topItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
            val scrollDistance = topItem?.offset?.toFloat() ?: -listState.firstVisibleItemScrollOffset.toFloat()
            if (abs(scrollDistance) > 1f) {
                listState.animateScrollBy(
                    value = scrollDistance,
                    animationSpec = spring(
                        dampingRatio = 0.68f, // Matching bouncy spring overshoot
                        stiffness = 320f,
                    ),
                )
            }
        }
    }

    FixedEdgeBlurContainer(
        topInset = contentPadding.calculateTopPadding(),
        bottomInset = contentPadding.calculateBottomPadding(),
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = totalBottomPadding,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "face") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(540.dp)
                        .graphicsLayer {
                            scaleX = faceScale.value
                            scaleY = faceScale.value
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    StopwatchFace(
                        elapsedMillis = elapsed.toMillis(),
                        running = stopwatch.running,
                        onToggle = onToggle,
                    )
                }
            }
            item(key = "controls") {
                StopwatchControls(
                    idle = !stopwatch.running && elapsed.isZero,
                    running = stopwatch.running,
                    onToggle = onToggle,
                    onLap = onLap,
                    onReset = handleReset,
                )
            }
            if (stopwatch.laps.isNotEmpty()) {
                items(stopwatch.laps, key = { it.index }) { lap ->
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        LapRow(lap, fastest == lap.index, slowest == lap.index)
                    }
                }
            }
        }
    }
}

@Composable
private fun StopwatchFace(elapsedMillis: Long, running: Boolean, onToggle: () -> Unit) {
    val safeMillis = elapsedMillis.coerceAtLeast(0L)
    val totalSeconds = safeMillis / 1000f
    val secondAngle = (totalSeconds % 60f) / 60f * (2f * Math.PI.toFloat())
    val hundredths = ((safeMillis % 1000L) / 10L).toInt()
    val minutes = (safeMillis / 60000L).toInt()
    val seconds = ((safeMillis / 1000L) % 60L).toInt()
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(STOPWATCH_FACE_SIZE)
            .clip(CircleShape)
            .elasticClickable(onClick = onToggle)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .96f), CircleShape)
            .border(1.25.dp, MaterialTheme.colorScheme.outline.copy(alpha = .12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize().padding(22.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // A restrained 60-mark track, with the quarter marks slightly heavier.
            for (i in 0 until 60) {
                val angle = i / 60f * (2f * Math.PI.toFloat()) - Math.PI.toFloat() / 2f
                val outer = radius - if (i % 5 == 0) 5.dp.toPx() else 2.dp.toPx()
                val inner = radius - if (i % 5 == 0) 15.dp.toPx() else 8.dp.toPx()
                val p1 = Offset(center.x + kotlin.math.cos(angle) * inner, center.y + kotlin.math.sin(angle) * inner)
                val p2 = Offset(center.x + kotlin.math.cos(angle) * outer, center.y + kotlin.math.sin(angle) * outer)
                drawLine(
                    color = if (i % 5 == 0) primary.copy(alpha = .65f) else onSurface.copy(alpha = .16f),
                    start = p1,
                    end = p2,
                    strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            val handLength = radius - 28.dp.toPx()
            val handAngle = secondAngle - Math.PI.toFloat() / 2f
            val tip = Offset(center.x + kotlin.math.cos(handAngle) * handLength, center.y + kotlin.math.sin(handAngle) * handLength)

            // Radial gradient so the hand fades out underneath the central readout text
            val innerCutoff = 72.dp.toPx()
            val fadeRange = 20.dp.toPx()
            val handBrush = Brush.radialGradient(
                0.0f to Color.Transparent,
                (innerCutoff / handLength).coerceIn(0f, 1f) to Color.Transparent,
                ((innerCutoff + fadeRange) / handLength).coerceIn(0f, 1f) to primary,
                1.0f to primary,
                center = center,
                radius = handLength,
            )

            drawLine(
                brush = handBrush,
                start = center,
                end = tip,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(primary, 5.dp.toPx(), tip)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    "%02d:".format(minutes),
                    fontFamily = ClockFace.family(opticalSize = 34f, width = 100f, weight = 500),
                    fontSize = 42.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "%02d.%02d".format(seconds, hundredths),
                    fontFamily = ClockFace.family(opticalSize = 34f, width = 100f, weight = 500),
                    fontSize = 42.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                if (running) "RUNNING" else "STOPWATCH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f),
                letterSpacing = 1.5.sp,
            )
        }
    }
}

private val STOPWATCH_FACE_SIZE = 410.dp

/**
 * The controls: **one row, one height, two states, and a morph between them.**
 *
 * Untouched, the screen offers only Start. Lap and Reset have nothing to act on yet, and a live
 * control that does nothing is worse than an absent one. It is sized to its own label, because a
 * lone button that spans the screen claims an importance the row does not have once it splits.
 *
 * Pressing it runs two phases, in order, and the order is the effect:
 *
 *  1. **Expand.** The pill grows from its content width to the full row on the expressive spatial
 *     spring. Its shape is already fully round, so growing the width *is* the morph; a small pill
 *     becomes a wide one with no corner interpolation to get wrong.
 *  2. **Split.** Only once it has arrived does the three-button row cross-fade in, with its 6 dp
 *     gaps opening from nothing. So the wide pill is seen to divide rather than to be replaced.
 *
 * The delay on phase 2 is what sequences them; running both on one curve reads as a dissolve.
 *
 * Both states are exactly [ROW_HEIGHT], and that is the whole point. An earlier version graded the
 * button heights down as laps accumulated to buy list space, which worked and looked wrong: the
 * controls twitched under the thumb on every lap press, and the band left for laps changed four
 * times. Fixing the height instead means nothing below the readout ever moves, and it makes the lap
 * band a constant, the same on a fresh screen as on a full one.
 */
@Composable
private fun StopwatchControls(
    idle: Boolean,
    running: Boolean,
    onToggle: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
) {
    val expanded = !idle
    // The *effects* spring, not the spatial one, and not by accident. Material's fast spatial
    // spring is under-damped (0.6/800), which means it takes about 240 ms and rings past its
    // target. Both properties are wrong here: the split is timed to land the moment the pill
    // arrives, so the pill has to actually arrive rather than wobble around full width.
    // `fastEffectsSpec` is critically damped and settles in well under half the time.
    val expand by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "expand",
    )
    // The label goes almost at once. A word sliding along inside a container that is still growing
    // reads as a glitch, so it is gone before the eye can follow it. The container it leaves
    // behind is what the three buttons then divide.
    val label by animateFloatAsState(
        targetValue = if (expanded) 0f else 1f,
        animationSpec = tween(durationMillis = LABEL_FADE_MS),
        label = "label",
    )
    // Deliberately not a spring: this one has to *wait*, and a delay is a tween's parameter. It
    // only waits on the way out, since collapsing back to Start should not stall on an empty row.
    val split by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = SPLIT_MS,
            delayMillis = if (expanded) EXPAND_LEAD_MS else 0,
        ),
        label = "split",
    )
    // The spring overshoots past 1 on the way in, which is wanted for the width but would push the
    // interpolation out of range.
    val e = expand.coerceIn(0f, 1f)

    val startStop: @Composable (Boolean) -> Unit = { compact ->
        WidePill(
            text = if (running) "Stop" else "Start",
            icon = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            onClick = onToggle,
            container = if (running) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.58f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
            },
            content = if (running) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            height = ROW_HEIGHT,
            compact = compact,
            glow = false,
        )
    }

    SubcomposeLayout(
        Modifier.fillMaxWidth().height(ROW_HEIGHT).padding(horizontal = 20.dp),
    ) { constraints ->
        val full = constraints.maxWidth
        val h = ROW_HEIGHT.roundToPx()

        // The resting width is **one readout row**: the `: ss` line directly above it, colon cell
        // and both digits. Measured off the real composable rather than added up from the cell
        // constant and a guess at the digit advance, so the two stay locked together if the cap
        // height or the width axis ever changes.
        val inset = ROW_INK_INSET.roundToPx()
        val rowWidth = subcompose("rowProbe") {
            DigitRow(
                part = "00",
                capHeight = READOUT_CAP,
                color = MaterialTheme.colorScheme.primary,
                separator = true,
            )
        }.first().measure(Constraints()).width

        // …but never narrower than the label needs. The two figures are within a few dp of each
        // other today; if a translation ever made "Start" the longer of the two, clipping the word
        // would be the worse failure.
        val natural = subcompose("probe") {
            WidePill(
                text = "Start",
                icon = Icons.Rounded.PlayArrow,
                onClick = {},
                height = ROW_HEIGHT,
                compact = true,
                fillWidth = false,
                contentPaddingH = IDLE_PADDING_H,
                glow = false,
            )
        }.first().measure(Constraints(minHeight = h, maxHeight = h)).width

        val collapsedW = maxOf(rowWidth - inset, natural).coerceAtMost(full)
        val collapsedX = ((full - collapsedW) / 2).coerceAtLeast(0)

        val w = lerp(collapsedW, full, e)
        val x = lerp(collapsedX, 0, e)

        val single = if (split < 1f) {
            subcompose("single") {
                Box(Modifier.graphicsLayer { alpha = 1f - split }) {
                    // Always the *idle* button, never "Stop". `running` has already flipped by the
                    // time this frame draws, and relabelling the pill at the instant of the tap
                    // makes the thing that grows a different button from the one that was pressed.
                    // The new labels belong to the split, which is what the split is for.
                    WidePill(
                        text = "Start",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = onToggle,
                        container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        height = ROW_HEIGHT,
                        compact = true,
                        contentPaddingH = IDLE_PADDING_H,
                        contentAlpha = label,
                        glow = false,
                    )
                }
            }.first().measure(Constraints.fixed(w, h))
        } else {
            null
        }

        val trio = if (split > 0f) {
            subcompose("trio") {
                Box(Modifier.graphicsLayer { alpha = split }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(GAP * split)) {
                        Box(Modifier.weight(1f)) {
                            WidePill(text = "Lap", onClick = onLap, height = ROW_HEIGHT, compact = true, glow = false)
                        }
                        Box(Modifier.weight(1f)) {
                            WidePill(
                                text = "Reset",
                                onClick = onReset,
                                outlined = true,
                                content = MaterialTheme.colorScheme.onSurface,
                                height = ROW_HEIGHT,
                                compact = true,
                                glow = false,
                            )
                        }
                        Box(Modifier.weight(1f)) { startStop(true) }
                    }
                }
            }.first().measure(Constraints.fixed(full, h))
        } else {
            null
        }

        layout(full, h) {
            single?.place(x, 0)
            trio?.place(0, 0)
        }
    }
}

/** How long the pill has to finish growing before the row is allowed to divide. */
private const val EXPAND_LEAD_MS = 95
private const val SPLIT_MS = 150
private const val LABEL_FADE_MS = 70

/** Roomier than the three-abreast pills: this one is not fighting two neighbours for width. */
private val IDLE_PADDING_H = 28.dp

/** `StackedDigits` sets its rows 35 dp apart; the button sits on the same rhythm. */
private val STACK_GAP = 35.dp

/** Shared so the Start button can be measured against a row drawn at the same size. */
private val READOUT_CAP = 66.dp

private val GAP = 6.dp
private val ROW_HEIGHT = 56.dp

/**
 * Lap rows are deliberately shorter than a list item usually is. Every dp here buys list, and the
 * row carries three short strings (an index badge, a split and a total), none of which needs the
 * height a two-line item would. At [LAP_ROW_HEIGHT] plus [LAP_GAP] a lap costs 52 dp, so the
 * four-lap control layout shows three of them where the old 124 dp cap showed one.
 */
private val LAP_ROW_HEIGHT = 48.dp
private val LAP_GAP = 4.dp

@Composable
private fun LapRow(lap: Lap, isFastest: Boolean, isSlowest: Boolean) {
    val badge = when {
        isFastest -> MaterialTheme.colorScheme.tertiaryContainer
        isSlowest -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val onBadge = when {
        isFastest -> MaterialTheme.colorScheme.onTertiaryContainer
        isSlowest -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val (m, s, cs) = lap.split.stopwatchParts()
    val (tm, ts, tcs) = lap.total.stopwatchParts()

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(percent = 50),
        border = null,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(LAP_ROW_HEIGHT)
            .glass(
                shape = RoundedCornerShape(percent = 50),
                tint = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .76f),
                borderAlpha = .72f,
                shadowElevation = 0.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(color = badge, shape = CircleShape, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        lap.index.toString(),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = onBadge,
                    )
                }
            }
            Text(
                "$m:$s.$cs",
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$tm:$ts.$tcs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
