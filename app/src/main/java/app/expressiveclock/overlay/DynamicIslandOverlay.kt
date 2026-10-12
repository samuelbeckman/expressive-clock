package app.expressiveclock.overlay

import android.content.Intent
import android.graphics.Rect
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import app.expressiveclock.MainActivity
import app.expressiveclock.alarm.Notifications
import app.expressiveclock.core.ClockTimer
import app.expressiveclock.core.Stopwatch
import app.expressiveclock.core.TimerState
import app.expressiveclock.ui.rememberElapsedTicker
import app.expressiveclock.ui.theme.ClockFace
import app.expressiveclock.ui.theme.Numerals
import java.time.Duration
import kotlin.math.roundToInt

private enum class IslandState { HIDDEN, COLLAPSED, EXPANDED }

/**
 * Dynamic Island-style overlay container and morphing UI for active Timers and Stopwatches.
 * Positioned around the status bar and camera cutout.
 *
 * Architecture reference: SmartIsland (GPL-3.0 by agupta07505).
 * Unified 3-state updateTransition machine driving geometry springs and content layers.
 */
@Composable
fun DynamicIslandOverlayContent(
    timer: ClockTimer?,
    stopwatch: Stopwatch?,
    topInsetDp: Dp,
    cutoutWidthDp: Dp = 44.dp,
    offsetXDp: Dp = 0.dp,
    offsetYDp: Dp = 0.dp,
    widthOffsetDp: Dp = 0.dp,
    heightOffsetDp: Dp = 0.dp,
    isCalibrationPreview: Boolean = false,
    isExiting: Boolean = false,
    isExpanded: Boolean = false,
    exitRequestId: Int = 0,
    onBoundsChanged: (Rect) -> Unit = {},
    onCollapsedSettled: () -> Unit = {},
    onExitComplete: () -> Unit = {},
    onExpandChanged: (Boolean) -> Unit,
    onOpenAppTab: (String) -> Unit = {},
    onPauseResumeTimer: () -> Unit,
    onAddTenTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
) {
    val context = LocalContext.current

    val isTimerActive = timer != null
    val isStopwatchActive = stopwatch != null

    if (!isCalibrationPreview && !isTimerActive && !isStopwatchActive && !isExiting) return

    var isEntranceInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isEntranceInitialized = true
    }

    val targetIslandState = when {
        !isEntranceInitialized -> IslandState.HIDDEN
        isExpanded && !isCalibrationPreview -> IslandState.EXPANDED
        else -> IslandState.COLLAPSED
    }

    // Dedicated Animatable for normal exit presence animation
    val exitProgress = remember { Animatable(1f) }

    LaunchedEffect(exitRequestId) {
        if (exitRequestId <= 0) return@LaunchedEffect
        Log.d("DynamicIslandOverlay", "DI EXIT EFFECT START request=$exitRequestId")
        exitProgress.snapTo(1f)
        exitProgress.animateTo(
            targetValue = 0f,
            animationSpec = spring(stiffness = 520f, dampingRatio = 0.72f),
        )
        Log.d("DynamicIslandOverlay", "DI EXIT EFFECT COMPLETE request=$exitRequestId")
        Log.d("DynamicIslandOverlay", "DI EXIT CALLING ON_EXIT_COMPLETE request=$exitRequestId")
        onExitComplete()
    }

    // Unified updateTransition state machine for normal states (HIDDEN, COLLAPSED, EXPANDED)
    val transition = updateTransition(targetState = targetIslandState, label = "SmartIslandTransition")

    // Logcat verification for entrance presence transition
    LaunchedEffect(transition.targetState) {
        if (transition.targetState == IslandState.COLLAPSED && transition.currentState == IslandState.HIDDEN) {
            Log.d("DynamicIslandOverlay", "DynamicIsland: APPEARING START")
        }
    }

    LaunchedEffect(transition.currentState, transition.targetState) {
        if (transition.currentState == IslandState.COLLAPSED) {
            Log.d("DynamicIslandOverlay", "DynamicIsland: APPEARING END")
        }
        if (transition.currentState == IslandState.COLLAPSED && transition.targetState == IslandState.COLLAPSED && !isExiting && isEntranceInitialized) {
            onCollapsedSettled()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val expandedOuterMargin = topInsetDp.coerceAtLeast(12.dp)
    val expandedWidth = (screenWidthDp - (expandedOuterMargin * 2)).coerceAtLeast(320.dp)
    val expandedHeight = 152.dp

    val collapsedWidth = (96.dp + cutoutWidthDp + widthOffsetDp).coerceIn(80.dp, 280.dp)
    val collapsedHeight = (38.dp + heightOffsetDp).coerceIn(24.dp, 60.dp)

    // Synchronized geometry spring spec (stiffness = 520f, dampingRatio = 0.72f)
    val geometrySpringDp = spring<Dp>(
        stiffness = 520f,
        dampingRatio = 0.72f,
    )

    val cameraPointDiameter = 1.dp

    // Master presence progress for shape convergence guard
    val transitionPresenceProgress by transition.animateFloat(
        label = "presenceProgress",
        transitionSpec = { spring(stiffness = 520f, dampingRatio = 0.72f) },
    ) { state ->
        when (state) {
            IslandState.HIDDEN -> 0f
            IslandState.COLLAPSED -> 1f
            IslandState.EXPANDED -> 1f
        }
    }

    val presenceProgress = if (isExiting) exitProgress.value else transitionPresenceProgress

    val visualWidth by transition.animateDp(
        label = "visualWidth",
        transitionSpec = { geometrySpringDp },
    ) { state ->
        when (state) {
            IslandState.HIDDEN -> cameraPointDiameter
            IslandState.COLLAPSED -> collapsedWidth
            IslandState.EXPANDED -> expandedWidth
        }
    }

    val visualHeight by transition.animateDp(
        label = "visualHeight",
        transitionSpec = { geometrySpringDp },
    ) { state ->
        when (state) {
            IslandState.HIDDEN -> cameraPointDiameter
            IslandState.COLLAPSED -> collapsedHeight
            IslandState.EXPANDED -> expandedHeight
        }
    }

    val currentCornerRadius by transition.animateDp(
        label = "cornerRadius",
        transitionSpec = { geometrySpringDp },
    ) { state ->
        when (state) {
            IslandState.HIDDEN -> (cameraPointDiameter / 2)
            IslandState.COLLAPSED -> (collapsedHeight / 2)
            IslandState.EXPANDED -> 42.dp
        }
    }

    // Two-Phase Geometry Model:
    // Phase 1 (clampedProgress <= 0.25f): Tiny Point (1dp x 1dp) <-> Small Circle (36dp x 36dp), width == height!
    // Phase 2 (clampedProgress > 0.25f): Small Circle (36dp x 36dp) <-> Calibrated Collapsed Pill
    val clampedProgress = presenceProgress.coerceIn(0f, 1f)
    val effectiveWidth: Dp
    val effectiveHeight: Dp
    val effectiveCornerRadius: Dp

    if (!isExiting && (transition.currentState == IslandState.EXPANDED || transition.targetState == IslandState.EXPANDED)) {
        // Collapsed <-> Expanded morph
        effectiveWidth = visualWidth
        effectiveHeight = visualHeight
        effectiveCornerRadius = currentCornerRadius
    } else if (clampedProgress <= 0.25f) {
        // Phase 1: Tiny Point (1dp x 1dp) <-> Small Circle (36dp x 36dp), width == height at all times!
        val circleProgress = (clampedProgress / 0.25f).coerceIn(0f, 1f)
        val diameter = lerp(cameraPointDiameter, cutoutWidthDp.coerceAtLeast(36.dp), circleProgress)
        effectiveWidth = diameter
        effectiveHeight = diameter
        effectiveCornerRadius = diameter / 2f
    } else {
        // Phase 2: Small Circle (36dp x 36dp) <-> Calibrated Collapsed Pill
        val pillProgress = ((clampedProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)
        val circleDiameter = cutoutWidthDp.coerceAtLeast(36.dp)
        effectiveWidth = lerp(circleDiameter, collapsedWidth, pillProgress)
        effectiveHeight = lerp(circleDiameter, collapsedHeight, pillProgress)
        effectiveCornerRadius = lerp(circleDiameter / 2f, collapsedHeight / 2f, pillProgress)
    }

    val transitionAlpha by transition.animateFloat(
        label = "currentAlpha",
        transitionSpec = {
            if (targetState == IslandState.HIDDEN) tween(durationMillis = 200, easing = LinearEasing)
            else tween(durationMillis = 150, easing = LinearEasing)
        },
    ) { state ->
        if (state == IslandState.HIDDEN) 0f else 1f
    }

    // During exit presence animation (isExiting == true), the black surface remains 100% opaque (1f)
    // until presenceProgress reaches ~0f.
    val currentAlpha = if (isExiting) {
        if (presenceProgress <= 0.01f) 0f else 1f
    } else {
        transitionAlpha
    }

    // Layer 2: Outgoing Collapsed Content Fade & Scale
    val transitionCollapsedAlpha by transition.animateFloat(
        label = "collapsedAlpha",
        transitionSpec = {
            if (targetState == IslandState.EXPANDED) {
                tween(durationMillis = 140, easing = LinearEasing)
            } else if (targetState == IslandState.HIDDEN) {
                tween(durationMillis = 100, easing = LinearEasing)
            } else {
                tween(durationMillis = 160, delayMillis = 120, easing = LinearEasing)
            }
        },
    ) { state ->
        if (state == IslandState.COLLAPSED) 1f else 0f
    }

    val collapsedAlpha = if (isExiting) {
        ((presenceProgress - 0.40f) / 0.60f).coerceIn(0f, 1f)
    } else {
        transitionCollapsedAlpha
    }

    val collapsedScale by transition.animateFloat(
        label = "collapsedScale",
        transitionSpec = { spring(stiffness = 450f, dampingRatio = 0.8f) },
    ) { state ->
        when (state) {
            IslandState.HIDDEN -> 0.3f
            IslandState.EXPANDED -> 0.85f
            IslandState.COLLAPSED -> 1f
        }
    }

    // Layer 3: Incoming Expanded Content Fade & Scale
    val expandedAlpha by transition.animateFloat(
        label = "expandedAlpha",
        transitionSpec = {
            if (targetState == IslandState.EXPANDED) {
                tween(durationMillis = 180, delayMillis = 80, easing = LinearEasing)
            } else {
                tween(durationMillis = 120, easing = LinearEasing) // Fast early fade-out on collapse!
            }
        },
    ) { state ->
        if (state == IslandState.EXPANDED) 1f else 0f
    }

    val expandedScale by transition.animateFloat(
        label = "expandedScale",
        transitionSpec = { spring(stiffness = 450f, dampingRatio = 0.8f) },
    ) { state ->
        if (state == IslandState.EXPANDED) 1f else 0.85f
    }

    fun openAppTab(tab: String) {
        onOpenAppTab(tab)
    }

    val density = LocalDensity.current
    val topInsetPx = with(density) { topInsetDp.toPx() }
    val offsetXPx = with(density) { offsetXDp.toPx() }
    val offsetYPx = with(density) { offsetYDp.toPx() }
    val effectiveWidthPx = with(density) { effectiveWidth.toPx() }
    val effectiveHeightPx = with(density) { effectiveHeight.toPx() }
    val effectiveCornerRadiusPx = with(density) { effectiveCornerRadius.toPx() }

    // Outer root container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isExpanded && !isCalibrationPreview) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            // Tap outside expanded card collapses back to capsule
                            onExpandChanged(false)
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Layer A: Full-screen Canvas drawing the single black surface
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = currentAlpha
                },
        ) {
            val screenCenterX = size.width / 2f
            val islandLeftPx = screenCenterX - (effectiveWidthPx / 2f) + offsetXPx
            val islandTopPx = topInsetPx + offsetYPx

            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(islandLeftPx, islandTopPx),
                size = Size(effectiveWidthPx, effectiveHeightPx),
                cornerRadius = CornerRadius(effectiveCornerRadiusPx, effectiveCornerRadiusPx),
            )
        }

        // Layer B: Content Box positioned over the drawn black island surface
        Box(
            modifier = Modifier
                .padding(top = topInsetDp)
                .offset(x = offsetXDp, y = offsetYDp)
                .width(effectiveWidth)
                .height(effectiveHeight)
                .onGloballyPositioned { coordinates ->
                    val pos = coordinates.positionOnScreen()
                    val size = coordinates.size
                    onBoundsChanged(
                        Rect(
                            pos.x.roundToInt(),
                            pos.y.roundToInt(),
                            (pos.x + size.width).roundToInt(),
                            (pos.y + size.height).roundToInt(),
                        )
                    )
                }
                .clickable(
                    enabled = !isCalibrationPreview,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    if (isExpanded) {
                        // Tapping the main black island surface opens the Clock app to the active tab
                        openAppTab(if (isTimerActive) Notifications.TAB_TIMERS else Notifications.TAB_STOPWATCH)
                    } else if (!isExiting) {
                        onExpandChanged(true)
                    }
                }
                .padding(
                    horizontal = if (targetIslandState == IslandState.EXPANDED) 20.dp else 0.dp,
                    vertical = if (targetIslandState == IslandState.EXPANDED) 16.dp else 0.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isCalibrationPreview) {
                CalibrationTargetCapsule(cutoutWidthDp = cutoutWidthDp)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Layer 2: Collapsed Content
                    if (collapsedAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = collapsedScale
                                    scaleY = collapsedScale
                                    alpha = collapsedAlpha
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            CollapsedIslandCapsule(
                                timer = timer,
                                stopwatch = stopwatch,
                                cutoutWidthDp = cutoutWidthDp,
                            )
                        }
                    }

                    // Layer 3: Expanded Content
                    if (expandedAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = expandedScale
                                    scaleY = expandedScale
                                    alpha = expandedAlpha
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ExpandedIslandCard(
                                timer = timer,
                                stopwatch = stopwatch,
                                onOpenTimerTab = { openAppTab(Notifications.TAB_TIMERS) },
                                onOpenStopwatchTab = { openAppTab(Notifications.TAB_STOPWATCH) },
                                onPauseResumeTimer = onPauseResumeTimer,
                                onAddTenTimer = onAddTenTimer,
                                onCancelTimer = onCancelTimer,
                                onToggleStopwatch = onToggleStopwatch,
                                onLapStopwatch = onLapStopwatch,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationTargetCapsule(
    cutoutWidthDp: Dp,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Outer faded ring/circle (matches MT Capsule calibration visual reference)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Bright red inner target circle with white border stroke
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .border(1.5.dp, Color.White, CircleShape),
            )
        }
    }
}

@Composable
private fun CompactTickerText(
    valueString: String,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    if (valueString.length == 3 && (valueString.endsWith('m') || valueString.endsWith('s')) && valueString[0].isDigit() && valueString[1].isDigit()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = modifier,
        ) {
            val digit1 = valueString[0]
            val digit2 = valueString[1]
            val suffix = valueString[2]

            CompactDigitCell(char = digit1, fontFamily = fontFamily, key = 1)
            CompactDigitCell(char = digit2, fontFamily = fontFamily, key = 2)

            Text(
                text = suffix.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = fontFamily,
                ),
                color = Color.White,
            )
        }
    } else {
        Text(
            text = valueString,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = fontFamily,
            ),
            color = Color.White,
            modifier = modifier,
        )
    }
}

@Composable
private fun CompactDigitCell(
    char: Char,
    fontFamily: FontFamily,
    key: Int,
) {
    AnimatedContent(
        targetState = char,
        transitionSpec = {
            val oldDigit = initialState.digitToIntOrNull()
            val newDigit = targetState.digitToIntOrNull()
            if (oldDigit == null || newDigit == null) {
                fadeIn(tween(80)) togetherWith fadeOut(tween(60)) using SizeTransform(clip = true)
            } else {
                val increasing = newDigit > oldDigit
                val incomingDirection = if (increasing) 1 else -1
                val outgoingDirection = -incomingDirection
                (slideInVertically(animationSpec = tween(120), initialOffsetY = { it * incomingDirection }) + fadeIn(tween(80))) togetherWith
                    (slideOutVertically(animationSpec = tween(100), targetOffsetY = { it * outgoingDirection }) + fadeOut(tween(60))) using SizeTransform(clip = true)
            }
        },
        label = "compactDigit-$key",
    ) { value ->
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                fontFamily = fontFamily,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun CollapsedIslandCapsule(
    timer: ClockTimer?,
    stopwatch: Stopwatch?,
    cutoutWidthDp: Dp,
) {
    val isTimer = timer != null
    val nowElapsed by rememberElapsedTicker(active = isTimer || (stopwatch != null && stopwatch.running))
    val compactFontFamily = remember { ClockFace.family(opticalSize = 14f, width = 65f, weight = 700) }

    val iconRotationZ = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000L)
            iconRotationZ.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            )
            iconRotationZ.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isTimer && timer != null) {
            val remaining = timer.remaining(nowElapsed)
            // Left side: Icon anchored 6dp from left inner edge
            Icon(
                Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(16.dp)
                    .graphicsLayer {
                        rotationZ = iconRotationZ.value
                    },
            )

            // Right side: Compact ticker text anchored 6dp from right inner edge
            CompactTickerText(
                valueString = formatCompactDuration(remaining),
                fontFamily = compactFontFamily,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            )
        } else if (stopwatch != null) {
            val elapsed = stopwatch.elapsed(nowElapsed)
            // Left side: Icon anchored 6dp from left inner edge
            Icon(
                Icons.Outlined.Timer,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(16.dp)
                    .graphicsLayer {
                        rotationZ = iconRotationZ.value
                    },
            )

            // Right side: Compact ticker text anchored 6dp from right inner edge
            CompactTickerText(
                valueString = formatCompactDuration(elapsed),
                fontFamily = compactFontFamily,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
            )
        }
    }
}

/**
 * Material 3 Expressive circular scalloped shape with 12 subtle rounded lobes.
 */
private val ScallopedShape: Shape = GenericShape { size, _ ->
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val outerRadius = minOf(size.width, size.height) / 2f
    val innerRadius = outerRadius * 0.91f
    val lobes = 12
    val angleStep = (2 * Math.PI / lobes).toFloat()

    for (i in 0 until lobes) {
        val angle = i * angleStep
        val nextAngle = (i + 1) * angleStep
        val midAngle = angle + angleStep / 2f

        val xOuter = centerX + outerRadius * cos(midAngle)
        val yOuter = centerY + outerRadius * sin(midAngle)
        val xInner = centerX + innerRadius * cos(nextAngle)
        val yInner = centerY + innerRadius * sin(nextAngle)

        if (i == 0) {
            val xStart = centerX + innerRadius * cos(0f)
            val yStart = centerY + innerRadius * sin(0f)
            moveTo(xStart, yStart)
        }

        quadraticTo(xOuter, yOuter, xInner, yInner)
    }
    close()
}

@Composable
private fun ExpressiveButton(
    containerColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(ScallopedShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ExpandedIslandCard(
    timer: ClockTimer?,
    stopwatch: Stopwatch?,
    onOpenTimerTab: () -> Unit,
    onOpenStopwatchTab: () -> Unit,
    onPauseResumeTimer: () -> Unit,
    onAddTenTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
) {
    val isTimer = timer != null && timer.state != TimerState.FINISHED
    val nowElapsed by rememberElapsedTicker(active = isTimer || (stopwatch != null && stopwatch.running))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = if (isTimer) onOpenTimerTab else onOpenStopwatchTab),
    ) {
        // Left side: Giant readout and "Time remaining" / "Elapsed time" label below
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            TickerTimerReadout(
                text = formatFullDuration(if (isTimer && timer != null) timer.remaining(nowElapsed) else stopwatch?.elapsed(nowElapsed) ?: Duration.ZERO),
                capHeight = 38.dp,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isTimer) "Time remaining" else "Elapsed time",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        // Right side: Three expressive scalloped buttons
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isTimer && timer != null) {
                val running = timer.state == TimerState.RUNNING
                // Button 1: Pause / Resume
                ExpressiveButton(
                    containerColor = Color(0xFF2C3242),
                    onClick = onPauseResumeTimer,
                ) {
                    Icon(
                        imageVector = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (running) "Pause" else "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                // Button 2: +1m
                ExpressiveButton(
                    containerColor = Color(0xFFD6E2FF),
                    onClick = onAddTenTimer,
                ) {
                    Text(
                        text = "+1m",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        ),
                        color = Color(0xFF0F1A2D),
                    )
                }
                // Button 3: Cancel
                ExpressiveButton(
                    containerColor = Color(0xFF5B2B31),
                    onClick = onCancelTimer,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFFFFB4AB),
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else if (stopwatch != null) {
                val running = stopwatch.running
                // Button 1: Pause / Resume
                ExpressiveButton(
                    containerColor = Color(0xFF2C3242),
                    onClick = onToggleStopwatch,
                ) {
                    Icon(
                        imageVector = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (running) "Pause" else "Start",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                // Button 2: Lap
                ExpressiveButton(
                    containerColor = Color(0xFFD6E2FF),
                    onClick = onLapStopwatch,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = "Lap",
                        tint = Color(0xFF0F1A2D),
                        modifier = Modifier.size(28.dp),
                    )
                }
                // Button 3: Cancel
                ExpressiveButton(
                    containerColor = Color(0xFF5B2B31),
                    onClick = onCancelTimer,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFFFFB4AB),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/**
 * Ticker readout that always formats as 00:00:00 (6 digits) and uses digit-by-digit overshoot animations.
 */
@Composable
private fun TickerTimerReadout(
    text: String,
    capHeight: Dp = 38.dp,
    color: Color = Color.White,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
    ) {
        text.forEachIndexed { index, char ->
            if (char == ':') {
                Numerals(
                    text = ":",
                    capHeight = capHeight,
                    color = color,
                    width = ClockFace.CONDENSED,
                    weight = 500, // Medium weight: clean, modern, highly legible
                    tracking = ClockFace.CONDENSED_TRACKING,
                )
            } else {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        slideInVertically { height -> height / 3 } + fadeIn(tween(120)) togetherWith
                            slideOutVertically { height -> -height / 3 } + fadeOut(tween(100))
                    },
                    label = "overlayDigit_$index",
                ) { digitChar ->
                    Numerals(
                        text = digitChar.toString(),
                        capHeight = capHeight,
                        color = color,
                        width = ClockFace.CONDENSED,
                        weight = 500, // Medium weight
                        tracking = ClockFace.CONDENSED_TRACKING,
                    )
                }
            }
        }
    }
}

private fun formatCompactDuration(duration: Duration): String {
    val totalSecs = duration.seconds.coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return if (mins > 0) {
        "%02dm".format(mins)
    } else {
        "%02ds".format(secs)
    }
}

private fun formatFullDuration(duration: Duration): String {
    val totalSecs = duration.seconds.coerceAtLeast(0)
    val hrs = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return "%02d:%02d:%02d".format(hrs, mins, secs)
}
