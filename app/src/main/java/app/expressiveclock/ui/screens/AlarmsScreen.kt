package app.expressiveclock.ui.screens

import android.graphics.Paint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import app.expressiveclock.ui.elasticClickable
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.expressiveclock.core.Alarm
import app.expressiveclock.ui.glass
import app.expressiveclock.ui.elasticClickable
import app.expressiveclock.data.WeekStart
import app.expressiveclock.data.order
import app.expressiveclock.ui.sheets.systemFirstDay
import app.expressiveclock.ui.theme.CapText
import app.expressiveclock.ui.theme.ClockFace
import app.expressiveclock.ui.theme.Numerals
import app.expressiveclock.ui.theme.inkBounds
import app.expressiveclock.ui.theme.rememberInkBounds
import app.expressiveclock.ui.theme.StretchedCaps
import java.time.DayOfWeek
import kotlin.math.sin

/**
 * The alarm grid.
 *
 * Proportions are measured off the research-concept renders in `clock-mockups/reference/concept/`.
 * At 412 dp wide the render gives a tile of 184 × 250 dp on a 15.7 dp margin with a 12 dp gutter,
 * a 23.9 dp corner radius, and an hour numeral whose cap height is 153 dp: **0.62 of the tile**.
 * That figure is the design; it is what makes the grid read as a wall of numerals rather than a
 * list of cards with big text on them.
 *
 * Armed and disarmed differ by **weight, not by fill**: `wght` 260 against `wght` 120 on the same
 * ultra-condensed cut, solved from the render's measured stem-over-cap of 0.0639 and 0.0306. The
 * off state is a light numeral, not an outlined one.
 *
 * Every element of the time (hour, minutes, meridiem) carries the *same* tracking. See
 * [ClockFace.CONDENSED_TRACKING] for why that is one constant rather than solved per element.
 */
/**
 * The inset every edge of the tile shares: numeral ink, day letters, switch track alike.
 *
 * The render measured this twice and got the same answer both times: numeral ink 17.9 dp down a
 * 250 dp tile, and the switch's visible track 18.2 dp up from its bottom. It was a *fraction* of
 * the tile height while the tile had a fixed aspect; now the height is derived from the content it
 * has to be an absolute, or the two would chase each other.
 */
private const val CONTENT_INSET_DP = 18.1f

/**
 * From the top of the day row's box to the bottom of the switch you can *see*.
 *
 * `Switch` is 48 dp of touch target around a 32 dp track, centred, so the visible track starts 8 dp
 * down. The column's bottom padding gives the other 8 dp back, which is what lands the track on
 * [CONTENT_INSET_DP].
 */
private const val SWITCH_ROW_VISIBLE_DP = 40f
/** Measured: the minutes are three-quarters of the hour's cap height, cap-tops aligned. */
private const val MINUTE_CAP_FRACTION = 0.743f
/** Measured: the meridiem's cap is 0.145 of the hour's. */
private const val MERIDIEM_FRACTION = 0.145f
/** Derived, so the column closes on the hour's baseline: 1 − 0.743 − 0.145. */
private const val MERIDIEM_GAP_FRACTION = 1f - MINUTE_CAP_FRACTION - MERIDIEM_FRACTION
/**
 * The gap between the hour's and the minutes' boxes, as a fraction of the hour's cap.
 *
 * A fraction rather than the 8.3 dp it was measured at, because the cap is now solved per tile: a
 * fixed gap would be a different proportion of a 140 dp numeral than of a 170 dp one, and the fit
 * would drift between tiles.
 */
/**
 * What one step of [ClockFace.TRACKING_DELTA] is worth, measured in hour-caps.
 *
 * Tracking is in em and this gap is in caps, so the two only trade through the digit's own
 * ink-to-em ratio: a font size of `cap / DIGIT_EM` means `delta` em is `delta / DIGIT_EM` of a cap.
 * There are exactly two internal digit gaps in a time, one inside the hour and one inside the
 * minutes, so twice this comes out of the gap between them and the block's width is unchanged.
 */
private const val TRACKING_DELTA_AS_CAP = ClockFace.TRACKING_DELTA / ClockFace.DIGIT_EM
private const val GAP_FRACTION = 8.3f / 155f - 2f * TRACKING_DELTA_AS_CAP
/**
 * `Switch` reserves a 48 dp touch target around a 32 dp track, so its visible top is 8 dp below the
 * row's. Every vertical measurement here is of the switch you can *see*, so the row is placed 8 dp
 * higher than the number says.
 */
private const val SWITCH_TOUCH_INSET_DP = 8f
/** A little shorter than the switch's 32 dp track, because level with it reads as shouting. */
private const val DAY_CAP_DP = 23f
/** Fixed, so the block is one width whatever the repeat pattern and the switch never shifts. */
private const val DAY_BLOCK_DP = 91f
/** The constant slice of space between letters. The scale is derived from it, not tuned. */
private const val DAY_TRACKING_DP = 2.2f

@Composable
fun AlarmsScreen(
    alarms: List<Alarm>,
    weekStart: WeekStart,
    staggerEntranceToken: Int = 0,
    onStaggerConsumed: () -> Unit = {},
    onToggle: (Long) -> Unit,
    onEdit: (Alarm) -> Unit,
    contentPadding: PaddingValues,
    onAddAlarm: (() -> Unit)? = null,
    onDeleteAlarms: ((Set<Long>) -> Unit)? = null,
    onSelectionCountChange: ((Int) -> Unit)? = null,
    clearSelectionSignal: Int = 0,
    deleteClickSignal: Int = 0,
    modifier: Modifier = Modifier,
) {
    // Track how many cards are revealed during first-launch post-onboarding stagger
    var visibleCardsCount by remember(staggerEntranceToken) {
        mutableIntStateOf(if (staggerEntranceToken > 0) 0 else Int.MAX_VALUE)
    }

    LaunchedEffect(staggerEntranceToken, alarms.size) {
        if (staggerEntranceToken > 0) {
            // Brief 120ms settling pause after page slide transition completes
            delay(120L)
            Log.d("DynamicIslandOverlay", "SETUP INITIAL ALARM STAGGER START count=${alarms.size}")
            for (i in 1..alarms.size) {
                visibleCardsCount = i
                Log.d("DynamicIslandOverlay", "SETUP INITIAL ALARM ITEM index=${i - 1}")
                if (i < alarms.size) {
                    delay(70L)
                }
            }
            onStaggerConsumed()
        }
    }
    // The setting reaches all the way into the tile, not only into the editor. An app that starts
    // the week on Monday in the picker and on Sunday in the grid is worse than one with no setting.
    val order = remember(weekStart) { weekStart.order(systemFirstDay()) }

    // Keep a visual copy of the list so a deleted alarm can remain in the grid long enough for
    // its exit animation to finish. Removing an item directly from the LazyGrid data source makes
    // it disappear from composition immediately, which means there is nothing left to animate.
    // The source list remains authoritative; this is only a short-lived presentation layer.
    var displayedAlarms by remember { mutableStateOf(alarms) }
    var exitingAlarmIds by remember { mutableStateOf(emptySet<Long>()) }

    var selectedIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val inSelectionMode = selectedIds.isNotEmpty()
    var showDeleteConfirmSheet by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIds.size) {
        onSelectionCountChange?.invoke(selectedIds.size)
    }

    LaunchedEffect(clearSelectionSignal) {
        if (clearSelectionSignal > 0) {
            selectedIds = emptySet()
        }
    }

    LaunchedEffect(deleteClickSignal) {
        if (deleteClickSignal > 0 && selectedIds.isNotEmpty()) {
            showDeleteConfirmSheet = true
        }
    }

    LaunchedEffect(displayedAlarms) {
        val existingIds = displayedAlarms.map { it.id }.toSet()
        val validSelected = selectedIds.filter { it in existingIds }.toSet()
        if (validSelected != selectedIds) {
            selectedIds = validSelected
        }
    }

    LaunchedEffect(alarms) {
        val sourceIds = alarms.asSequence().map { it.id }.toSet()
        val oldDisplayed = displayedAlarms
        val removed = oldDisplayed.filter { it.id !in sourceIds }
        val removedIds = removed.map { it.id }.toSet()

        if (removed.isNotEmpty()) {
            exitingAlarmIds = exitingAlarmIds + removed.map { it.id }
        }

        if (removed.isEmpty()) {
            // Normal updates, including an edited alarm changing its sort position, should mirror
            // the source list exactly.
            displayedAlarms = alarms
        } else {
            // During deletion, preserve the exiting card in its original slot while updating every
            // surviving card by ID. This prevents a key from accidentally receiving another alarm's
            // content when two alarms swap positions.
            val sourceById = alarms.associateBy { it.id }
            val oldIds = oldDisplayed.map { it.id }.toSet()
            displayedAlarms = buildList {
                oldDisplayed.forEach { old ->
                    add(sourceById[old.id] ?: old)
                }
                alarms.forEach { alarm ->
                    if (alarm.id !in oldIds) add(alarm)
                }
            }
        }

        if (removed.isNotEmpty()) {
            // Slightly longer than the visual exit so the final frame is not clipped by the list
            // mutation. The neighbouring cards then get their own spring placement animation.
            kotlinx.coroutines.delay(360)
            displayedAlarms = displayedAlarms.filter { it.id in sourceIds }
            exitingAlarmIds = exitingAlarmIds - removedIds
        }
    }

    FixedEdgeBlurContainer(
        topInset = contentPadding.calculateTopPadding(),
        bottomInset = contentPadding.calculateBottomPadding(),
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = displayedAlarms.isEmpty(),
            transitionSpec = {
                if (targetState) {
                    (fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = 380f, dampingRatio = 0.78f)) +
                        slideInVertically(animationSpec = spring(stiffness = 380f, dampingRatio = 0.78f)) { it / 16 }
                    ) togetherWith (
                        fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 0.95f, animationSpec = tween(200))
                    )
                } else {
                    (fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.95f, animationSpec = spring(stiffness = 380f, dampingRatio = 0.78f))
                    ) togetherWith (
                        fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(200)) +
                        slideOutVertically(animationSpec = tween(200)) { it / 16 }
                    )
                }
            },
            label = "alarmsEmptyTransition",
            modifier = Modifier.fillMaxSize(),
        ) { isEmpty ->
            if (isEmpty) {
                EmptyAlarmState(
                    onAddAlarm = { onAddAlarm?.invoke() },
                    contentPadding = contentPadding,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(displayedAlarms, key = { _, alarm -> alarm.id }) { index, alarm ->
                        val isCardVisible = alarm.id !in exitingAlarmIds && (index < visibleCardsCount)
                        AnimatedVisibility(
                            modifier = Modifier.animateItem(
                                placementSpec = spring(
                                    stiffness = 380f,
                                    dampingRatio = 0.68f,
                                ),
                            ),
                            visible = isCardVisible,
                            enter = fadeIn(tween(180)) +
                                scaleIn(
                                    initialScale = 0.86f,
                                    animationSpec = spring(
                                        dampingRatio = 0.62f,
                                        stiffness = 400f,
                                    ),
                                ) +
                                slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.62f,
                                        stiffness = 400f,
                                    ),
                                ) { it / 10 },
                            exit = fadeOut(tween(260)) +
                                scaleOut(
                                    targetScale = 0.90f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 500f),
                                ),
                        ) {
                            AlarmTile(
                                alarm = alarm,
                                order = order,
                                selected = alarm.id in selectedIds,
                                inSelectionMode = inSelectionMode,
                                onToggle = { onToggle(alarm.id) },
                                onEdit = { onEdit(alarm) },
                                onToggleSelect = {
                                    selectedIds = if (alarm.id in selectedIds) {
                                        selectedIds - alarm.id
                                    } else {
                                        selectedIds + alarm.id
                                    }
                                },
                                onStartSelection = {
                                    selectedIds = selectedIds + alarm.id
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmSheet) {
        DeleteAlarmsConfirmationSheet(
            count = selectedIds.size,
            onDismiss = { showDeleteConfirmSheet = false },
            onConfirmDelete = {
                val idsToDelete = selectedIds
                selectedIds = emptySet()
                showDeleteConfirmSheet = false
                onDeleteAlarms?.invoke(idsToDelete)
            },
        )
    }
}

/**
 * Fixed edge blur and fade container anchored at top (Settings icon level) and bottom (Nav bar level).
 * Renders in parent layout above the scrolling alarm grid content.
 */
@Composable
fun FixedEdgeBlurContainer(
    topInset: Dp,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                // Step 1: Draw the base sharp scrolling alarm grid
                drawContent()

                val width = size.width
                val height = size.height
                val topInsetPx = topInset.toPx()
                val bottomInsetPx = bottomInset.toPx()

                // Step 2: On Android 12+, render genuine progressive hardware blur overlays
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas

                        val tileMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Shader.TileMode.DECAL
                        } else {
                            Shader.TileMode.CLAMP
                        }

                        val blurPaint = Paint()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setPaintBlurEffect(blurPaint, 32f, tileMode)
                        }

                        // Fixed top edge blur overlay (from y = 0 down to Settings icon level)
                        if (topInsetPx > 0f) {
                            val saveCount = nativeCanvas.saveLayer(
                                0f, 0f, width, topInsetPx, blurPaint
                            )
                            drawContent()

                            // Organic gradient mask for top blur
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black,
                                        Color.Black.copy(alpha = 0.70f),
                                        Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = topInsetPx,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                            nativeCanvas.restoreToCount(saveCount)
                        }

                        // Fixed bottom edge blur overlay (from top edge of nav bar to bottom)
                        val bottomStart = height - bottomInsetPx
                        if (bottomInsetPx > 0f && bottomStart < height) {
                            val saveCount = nativeCanvas.saveLayer(
                                0f, bottomStart, width, height, blurPaint
                            )
                            drawContent()

                            // Organic gradient mask for bottom blur
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.70f),
                                        Color.Black,
                                    ),
                                    startY = bottomStart,
                                    endY = height,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                            nativeCanvas.restoreToCount(saveCount)
                        }
                    }
                }

                // Step 3: Smooth organic gradient fade mask for extreme top & bottom edge dissolve
                if (topInsetPx > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black,
                            ),
                            startY = 0f,
                            endY = topInsetPx,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }

                val bottomStart = height - bottomInsetPx
                if (bottomInsetPx > 0f && bottomStart < height) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                            ),
                            startY = bottomStart,
                            endY = height,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) {
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun setPaintBlurEffect(paint: Paint, radius: Float, tileMode: Shader.TileMode) {
    runCatching {
        val method = paint.javaClass.getMethod("setRenderEffect", RenderEffect::class.java)
        method.invoke(paint, RenderEffect.createBlurEffect(radius, radius, tileMode))
    }
}

/**
 * The one cap height every tile in the grid is set at.
 *
 * Everything here is linear in the cap. The numerals are ink-tight, the gap is a fraction of the
 * cap and the meridiem is a fraction of the cap, so one measurement at a reference size gives the
 * constant of proportionality and the answer is a division.
 *
 * ## Why this takes no arguments about *this* tile
 *
 * It used to. It solved the cap per tile from that tile's own **ink**, and returned a per-tile
 * shift that centred that ink. Both were wrong, and together they were the whole bug:
 *
 *  - **Ink varies with the digits.** At `wdth` 25 a four-digit time spans 0.645 em (`11:15`) to
 *    0.820 em (`06:40`) in proportional figures. On a 174 dp tile that is a 45 dp swing in the
 *    block's width.
 *  - **The ceiling always won.** Solved over all 720 times, the width fit *never* came in under
 *    the old `0.62 * tileHeight` ceiling. The cap was therefore always the ceiling, the fit never
 *    bound, and every one of those 45 dp went into slack instead of into the numeral.
 *  - **The shift then split that slack.** `06:40` filled the tile and sat on a 12.7 dp margin;
 *    `07:15` was 35 dp narrower and sat on a 22.5 dp one. Two tiles side by side, ten dp apart,
 *    for no reason a reader could see.
 *
 * The fix is upstream of all three: with tabular figures every time is the same number of identical
 * cells, so there is exactly one answer and it is the same for every tile. This function computes
 * it from `"00"` at [ClockFace.WEIGHT_ON], never from the tile's own time or weight, precisely so
 * that it cannot come out differently anywhere.
 *
 * It still divides by [available], so a wider or narrower tile still gets a cap that fits, and
 * [ceiling] still stops a wide tile growing the numeral down into the switch.
 *
 * The meridiem stays in the max for safety, though tabular minutes are now always the wider of the
 * two: `00` at 0.743 cap is 71.5 dp against `AM`'s 28 dp at 0.145 cap.
 */
private class TimeFit(val cap: Dp, val padStart: Dp, val padEnd: Dp)

@Composable
private fun fittedTime(meridiem: String, tileWidth: Dp, target: Dp): TimeFit {
    val density = LocalDensity.current
    val ref = REFERENCE_CAP_DP.dp
    // `WEIGHT_ON` whatever this tile's weight is, and "00" whatever its time is. Both are
    // deliberate: the answer has to be the *same number on every tile in the grid*, so it may not
    // depend on either. Armed is the wider of the two weights, so solving from it is also what
    // guarantees no tile overflows.
    val hourStyle = ClockFace.numerals(
        ref, ClockFace.CONDENSED, ClockFace.WEIGHT_ON,
        tabular = true, tracking = ClockFace.CONDENSED_TRACKING,
    )
    val minuteStyle = ClockFace.numerals(
        ref * MINUTE_CAP_FRACTION, ClockFace.CONDENSED, ClockFace.WEIGHT_ON,
        tabular = true, tracking = ClockFace.CONDENSED_TRACKING,
    )
    val meridiemStyle = ClockFace.capitals(
        ref * MERIDIEM_FRACTION, ClockFace.CONDENSED, tracking = ClockFace.CONDENSED_TRACKING,
    )
    val h = rememberInkBounds("00", hourStyle)
    val m = rememberInkBounds("00", minuteStyle)
    val a = rememberInkBounds(meridiem, meridiemStyle)

    return remember(h, m, a, tileWidth, target, density) {
        with(density) {
            // **Advance, not ink.** Ink is what varies between digits even when tabular has made
            // the cells identical, so fitting to ink puts the variation straight back into the
            // margins. The advance box is the grid the digits are set on, and it is the thing that
            // has to line up from tile to tile.
            val trailing = if (m.advance >= a.advance) m else a

            // Everything as a multiple of the cap, so the whole thing solves in closed form.
            //  k  is the block's advance
            //  lf is how far the hour's ink starts inside the block's leading edge
            //  rf is how far the trailing ink stops short of its trailing edge
            val k = (h.advance + maxOf(m.advance, a.advance)).toDp() / ref + GAP_FRACTION
            val lf = h.left.toDp() / ref
            val rf = trailing.right.toDp() / ref

            // Solve the cap that puts the **ink**, not the box, exactly [target] from both edges:
            //     target + lf*cap + k*cap + rf*cap + target = tileWidth
            // The two bearings are not equal: the hour's is at full size, the trailing element's at
            // 0.743 of it less a step of negative tracking. That is precisely why padding the box
            // symmetrically cannot square the two visible margins, and why an earlier "shift by half
            // the difference" only ever closed part of the gap. Solving for the ink leaves no gap.
            val cap = (tileWidth - target * 2f) / (k - lf - rf)

            // The block fills exactly, so this is nought. It is kept because it is what makes
            // the two *ink* margins equal rather than the two box margins, if a caller ever
            // clamps the cap.
            val slack = tileWidth - cap * k
            val padStart = (slack + cap * (rf - lf)) / 2f
            TimeFit(cap = cap, padStart = padStart, padEnd = slack - padStart)
        }
    }
}

/** The `wdth` that makes the meridiem as wide as the minutes, and the shift that squares them up. */
private class MeridiemFit(val width: Float, val offsetX: Dp)

/**
 * Stretches `AM`/`PM` on the **width axis** until its ink is exactly as wide as the minutes above
 * it, then nudges it so the two are concentric.
 *
 * Only possible because the minutes are now tabular: their ink is one width for every time, so
 * there is a single target to solve against rather than sixty. Solved by bisection on `wdth`
 * because ink width is monotonic in the axis but not linear in it. Measured, `AM` runs 0.269 of
 * the hour's cap at `wdth` 100 and 0.498 at 151, so a straight interpolation between the ends
 * misses by several dp in the middle.
 *
 * `AM` and `PM` land on different axis values (about 130 and 140), and that is the point: matching
 * their *widths* is what makes them interchangeable in the tile, and no single axis value does that
 * for both. Both sit comfortably inside the font's 25–151 range, so nothing is clamped and no
 * horizontal scale is involved: these are real widths the typeface was drawn to hold.
 */
@Composable
private fun rememberMeridiemFit(
    meridiem: String,
    minutes: String,
    hourCap: Dp,
    weight: Int,
): MeridiemFit {
    val resolver = LocalFontFamilyResolver.current
    val density = LocalDensity.current
    return remember(meridiem, minutes, hourCap, weight, density) {
        // Measured at the size it will be drawn at, and **not** at some multiple of it.
        //
        // `getTextBounds` reports whole pixels, so measuring larger and scaling down is the obvious
        // way to keep rounding out of the answer. It is wrong for this face: `opsz` is an axis here
        // and [ClockFace.capitals] drives it from the size, so a 4x measurement is of a differently
        // *shaped* glyph, and the `wdth` it solves comes out 3-6 dp narrow at the real size. Tried
        // and measured; the pixel of rounding it saves is not worth the width it costs.
        val minuteInk = inkBounds(
            resolver, density, minutes,
            ClockFace.numerals(
                hourCap * MINUTE_CAP_FRACTION, ClockFace.CONDENSED, weight,
                tracking = ClockFace.CONDENSED_TRACKING,
            ),
        )
        val cap = hourCap * MERIDIEM_FRACTION
        fun measure(w: Float) = inkBounds(
            resolver, density, meridiem,
            ClockFace.capitals(cap, w, tracking = ClockFace.CONDENSED_TRACKING),
        )

        var lo = ClockFace.CONDENSED
        var hi = 151f
        repeat(16) {
            val mid = (lo + hi) / 2f
            if (measure(mid).width < minuteInk.width) lo = mid else hi = mid
        }
        val width = (lo + hi) / 2f
        val fitted = measure(width)
        // Both are centred in the same column, so their *boxes* line up; the ink inside each does
        // not, because a capital's bearings are nothing like a digit's. Concentric ink is what the
        // eye reads as aligned.
        //
        // Two terms, and the second is easy to miss. Column width is `max(Wm, Wa)` and both boxes
        // are centred in it, so their origins already differ by half the difference of their
        // *advances* before any ink is considered. Leaving that out leaves the meridiem right of
        // where it belongs by an amount that depends on the minutes' bearings: measured, 4.5 px
        // under `15` but 8.0 px under `00`, because a tabular pair of zeros carries 64 units of
        // bearing each side against `15`'s 33 and 71. It reads as "some times line up and some
        // don't", which is exactly how it was reported.
        val boxOffset = (fitted.advance - minuteInk.advance) / 2f
        val minuteMid = minuteInk.left + minuteInk.width / 2f
        val meridiemMid = fitted.left + fitted.width / 2f
        MeridiemFit(width, with(density) { (boxOffset + minuteMid - meridiemMid).toDp() })
    }
}

/** Any size would do; widths are linear in the cap, so this only sets the measurement's precision. */
private const val REFERENCE_CAP_DP = 120f

@Composable
private fun AlarmTile(
    alarm: Alarm,
    order: List<DayOfWeek>,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
    itemModifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onToggleSelect: () -> Unit = {},
    onStartSelection: () -> Unit = {},
) {
    val view = LocalView.current
    val armed = alarm.enabled
    val weightTarget = if (armed) ClockFace.WEIGHT_ON else ClockFace.WEIGHT_OFF
    val animatedWeight by androidx.compose.animation.core.animateIntAsState(
        targetValue = weightTarget,
        animationSpec = tween(durationMillis = 220),
        label = "alarmTimeWeight",
    )
    val animatedContainer by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .92f)
            armed -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .62f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .88f)
        },
        animationSpec = tween(220),
        label = "alarmContainer",
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = .26f),
        animationSpec = tween(220),
        label = "alarmBorder",
    )
    val animatedInk by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.onPrimaryContainer
            armed -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "alarmInk",
    )
    // Separate entrance scale from the armed-state scale so creating an alarm can have its own
    // subtle bounce without disturbing the polished enable/disable animation.
    val cardScale = remember { Animatable(1f) }
    var hasComposed by remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(armed) {
        if (!hasComposed) {
            hasComposed = true
            return@LaunchedEffect
        }
        cardScale.snapTo(if (armed) .985f else 1.015f)
        cardScale.animateTo(
            if (armed) 1.018f else .982f,
            animationSpec = spring(dampingRatio = .72f, stiffness = 520f),
        )
        cardScale.animateTo(1f, spring(dampingRatio = .82f, stiffness = 430f))
    }

    val hour12 = (alarm.time.hour % 12).takeIf { it != 0 } ?: 12
    val meridiem = if (alarm.time.hour < 12) "AM" else "PM"
    val minutesText = "%02d".format(alarm.time.minute)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val tileWidth = maxWidth
    val inset = CONTENT_INSET_DP.dp
    val fit = fittedTime(meridiem = meridiem, tileWidth = tileWidth, target = inset)
    val hourCap = fit.cap
    val meridiemFit = rememberMeridiemFit(meridiem, minutesText, hourCap, animatedWeight)
    val timeToDays = hourCap * MERIDIEM_GAP_FRACTION - SWITCH_TOUCH_INSET_DP.dp - ALARM_DIGIT_BOTTOM_BLEED_DP.dp
    val tileHeight = inset * 2 + hourCap + timeToDays + SWITCH_ROW_VISIBLE_DP.dp

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        border = null,
        shadowElevation = 0.dp,
        modifier = itemModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .graphicsLayer {
                val scale = cardScale.value
                scaleX = scale
                scaleY = scale
            }
            .background(animatedContainer, RoundedCornerShape(24.dp))
            .border(if (selected) 2.dp else 1.25.dp, animatedBorderColor, RoundedCornerShape(24.dp))
            .height(tileHeight)
            .combinedClickable(
                onClick = {
                    if (inSelectionMode) {
                        onToggleSelect()
                    } else {
                        onEdit()
                    }
                },
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    if (inSelectionMode) {
                        onToggleSelect()
                    } else {
                        onStartSelection()
                    }
                },
            )
            .clearAndSetSemantics {
                contentDescription = buildString {
                    if (selected) append("Selected, ")
                    append("%d:%02d %s".format(hour12, alarm.time.minute, meridiem))
                    if (alarm.label.isNotBlank()) append(", ${alarm.label}")
                    append(", ${alarm.repeatLabel()}")
                    append(if (armed) ", on" else ", off")
                    append(", double tap to edit")
                }
            },
    ) {
            Box(Modifier.fillMaxSize()) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, top = 12.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = inset, bottom = inset - SWITCH_TOUCH_INSET_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(timeToDays),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = fit.padStart, end = fit.padEnd),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        AnimatedAlarmNumber(
                            value = hour12,
                            capHeight = hourCap,
                            color = animatedInk,
                            weight = animatedWeight,
                        )
                        Spacer(Modifier.width(hourCap * GAP_FRACTION))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedAlarmNumber(
                                value = alarm.time.minute,
                                capHeight = hourCap * MINUTE_CAP_FRACTION,
                                color = animatedInk,
                                weight = animatedWeight,
                            )
                            Spacer(Modifier.height(hourCap * MERIDIEM_GAP_FRACTION))
                            CapText(
                                text = meridiem,
                                capHeight = hourCap * MERIDIEM_FRACTION,
                                color = animatedInk,
                                width = meridiemFit.width,
                                tracking = ClockFace.CONDENSED_TRACKING,
                                modifier = Modifier.offset(x = meridiemFit.offsetX),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = inset),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        DayLetters(alarm, order, animatedInk)
                        Switch(
                            checked = armed,
                            onCheckedChange = {
                                if (inSelectionMode) {
                                    onToggleSelect()
                                } else {
                                    onToggle()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = animatedContainer,
                                checkedTrackColor = animatedInk,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = animatedInk,
                                uncheckedTrackColor = Color.Transparent,
                                uncheckedBorderColor = animatedInk,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionToolbar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f))
            .border(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), CircleShape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onClearSelection) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Clear selection",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Text(
            "$selectedCount selected",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete selected alarms",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAlarmsConfirmationSheet(
    count: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.60f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Delete $count alarm${if (count > 1) "s" else ""}?",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "This can't be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .elasticClickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onConfirmDelete()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Delete",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                    .elasticClickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * Animated numeric display for the alarm tile.
 *
 * When an alarm is edited, the number rolls toward its new value rather than being replaced in a
 * single frame. Increasing values enter from below and decreasing values enter from above, with a
 * restrained spring so the final position has a tiny amount of physical bounce.
 */
private const val ALARM_DIGIT_BOTTOM_BLEED_DP = 2

@Composable
private fun AnimatedAlarmNumber(
    value: Int,
    capHeight: Dp,
    color: Color,
    weight: Int,
) {
    // Keep the two digits in the same horizontal geometry as the original two-character numeral,
    // but give each digit its own vertical viewport and animation state. This is important for the
    // alarm tiles: a change from 09 -> 10 should not make the whole number move as one object.
    val digits = "%02d".format(value)

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .height(capHeight + ALARM_DIGIT_BOTTOM_BLEED_DP.dp)
            .clipToBounds(),
    ) {
        digits.forEachIndexed { index, digit ->
            AnimatedAlarmDigit(
                digit = digit.digitToInt(),
                capHeight = capHeight,
                color = color,
                weight = weight,
                key = index,
            )
        }
    }
}

@Composable
private fun AnimatedAlarmDigit(
    digit: Int,
    capHeight: Dp,
    color: Color,
    weight: Int,
    key: Int,
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            // Treat each character like a tiny physical ticker wheel. The shortest path around
            // 0..9 gives 9 -> 0 and 0 -> 9 the same natural direction as a real digit wheel.
            val forward = (targetState - initialState + 10) % 10
            val increasing = forward in 1..5
            val enterOffset: (Int) -> Int = if (increasing) { { it } } else { { -it } }
            val exitOffset: (Int) -> Int = if (increasing) { { -it } } else { { it } }

            slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = 430f,
                ),
                initialOffsetY = enterOffset,
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 430f,
                ),
            ) + fadeIn(tween(85)) togetherWith
                slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 520f,
                    ),
                    targetOffsetY = exitOffset,
                ) + androidx.compose.animation.scaleOut(
                    targetScale = 0.96f,
                    animationSpec = tween(120),
                ) + fadeOut(tween(75))
        },
        label = "alarmDigitChange_$key",
        contentAlignment = Alignment.Center,
    ) { target ->
        // The fixed-height box is deliberately clipped. The spring is allowed to overshoot, but
        // the digit must never paint outside the alarm tile or collide with the day row below it.
        Box(
            modifier = Modifier
                .height(capHeight + ALARM_DIGIT_BOTTOM_BLEED_DP.dp)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Numerals(
                text = target.toString(),
                capHeight = capHeight,
                color = color,
                width = ClockFace.CONDENSED,
                weight = weight,
                tracking = ClockFace.CONDENSED_TRACKING,
            )
        }
    }
}

/**
 * The repeat days, on the switch's own line.
 *
 * ## One text run, so the spacing is the font's
 *
 * Two earlier attempts gave each letter its own fixed-width box: first every box the same, then
 * each box sized to its own advance. Both are the same mistake in different clothes. A box
 * boundary is a wall the shaper cannot see across, so the side bearings the type designer drew get
 * overridden by an invented cell width and no kerning pair can apply. Equal boxes put 6.9 dp beside
 * `F` and 2.3 dp beside `T`; advance-sized boxes still left slack around every inactive letter,
 * because a light glyph is narrower than the bold cell it was centred in.
 *
 * So all seven are **one** [AnnotatedString], each letter a span carrying its own weight and
 * colour. The text engine then spaces them exactly as the font says to, which is the only
 * definition of "even" that the eye agrees with.
 *
 * ## And the block still cannot change width
 *
 * The switch sits beside this and must land in the same place on every tile, so the run is squeezed
 * to [DAY_BLOCK_DP] by a scale *derived* from its own measured width rather than tuned. A tile with
 * more bold letters squeezes a fraction harder (about 1.5 % between the extremes, some 0.2 dp on
 * a letter), and the block, and so the switch, does not move at all.
 *
 * [DAY_TRACKING_DP] is inside that squeeze, so tightening it hands the space straight to the
 * letters: block width fixed, tracking down, glyphs wider.
 */
@Composable
private fun DayLetters(alarm: Alarm, order: List<DayOfWeek>, ink: Color) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val bold = ClockFace.capitals(DAY_CAP_DP.dp, ClockFace.CONDENSED, weight = DAY_WEIGHT_ON)
    val light = ClockFace.capitals(DAY_CAP_DP.dp, ClockFace.CONDENSED, weight = DAY_WEIGHT_OFF)

    val text = remember(alarm.days, alarm.isOneShot, order, ink, bold, light) {
        buildAnnotatedString {
            order.forEach { day ->
                val on = !alarm.isOneShot && day in alarm.days
                withStyle(
                    SpanStyle(
                        // Weight is the state here too, matching the numerals above.
                        fontFamily = (if (on) bold else light).fontFamily,
                        color = if (on) ink else ink.copy(alpha = 0.30f),
                    )
                ) {
                    append(day.name.take(1))
                }
            }
        }
    }

    // Tracking in em, so it rides the type size the way the rest of the face's tracking does.
    val style = bold.copy(
        letterSpacing = with(density) { (DAY_TRACKING_DP.dp / bold.fontSize.toDp()).em },
    )
    val scaleX = with(density) {
        DAY_BLOCK_DP.dp.toPx() / measurer.measure(text, style).size.width
    }

    StretchedCaps(text = text, style = style, capHeight = DAY_CAP_DP.dp, scaleX = scaleX)
}

/** Weight is the state, the same way it is on the numerals above. Not a second colour ramp. */
private const val DAY_WEIGHT_ON = 700
private const val DAY_WEIGHT_OFF = 400

@Composable
private fun EmptyAlarmState(
    onAddAlarm: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SleepingAlarmClockIcon(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            bellColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
            modifier = Modifier.size(136.dp),
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "No alarms yet",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Get started by adding your first alarm.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f))
                .elasticClickable(onClick = onAddAlarm)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add Alarm",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SleepingAlarmClockIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
    bellColor: Color = color.copy(alpha = 0.50f),
) {
    // 1. Occasional clock ring/tilt vibration animation
    val clockRotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(6500)
            val angles = listOf(-5.5f, 5.5f, -5f, 5f, -3.5f, 3.5f, -2f, 2f, 0f)
            for (angle in angles) {
                clockRotation.animateTo(
                    targetValue = angle,
                    animationSpec = tween(durationMillis = 65, easing = LinearEasing),
                )
            }
        }
    }

    // 2. Continuous staggered Z drift & fade animations
    val zTransition = rememberInfiniteTransition(label = "zLanesTransition")

    val z0Progress by zTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "z0",
    )

    val z1Progress by zTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, delayMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "z1",
    )

    val z2Progress by zTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, delayMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "z2",
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 3.5.dp.toPx()
        val cx = size.width * 0.42f
        val cy = size.height * 0.70f
        val r = size.width * 0.24f

        // Clock Body (rotates occasionally when ringing)
        rotate(degrees = clockRotation.value, pivot = Offset(cx, cy)) {
            // 1. Main Clock Face Circle Body
            drawCircle(
                color = color,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = strokeWidth),
            )

            // 2. Bottom Feet
            val footLength = r * 0.38f
            val leftFootStart = Offset(cx - r * 0.707f, cy + r * 0.707f)
            val leftFootEnd = Offset(
                leftFootStart.x - footLength * 0.55f,
                leftFootStart.y + footLength * 0.85f,
            )
            drawLine(
                color = color,
                start = leftFootStart,
                end = leftFootEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            val rightFootStart = Offset(cx + r * 0.707f, cy + r * 0.707f)
            val rightFootEnd = Offset(
                rightFootStart.x + footLength * 0.55f,
                rightFootStart.y + footLength * 0.85f,
            )
            drawLine(
                color = color,
                start = rightFootStart,
                end = rightFootEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            // 3. Separate Alarm Bells sitting higher and slightly outward, in a lighter blue tone
            val bellRadius = r * 0.32f

            // Left bell
            val leftBellCenter = Offset(cx - r * 0.76f, cy - r * 0.82f)
            drawArc(
                color = bellColor,
                startAngle = 135f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftBellCenter.x - bellRadius, leftBellCenter.y - bellRadius),
                size = Size(bellRadius * 2f, bellRadius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Right bell
            val rightBellCenter = Offset(cx + r * 0.76f, cy - r * 0.82f)
            drawArc(
                color = bellColor,
                startAngle = 225f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(rightBellCenter.x - bellRadius, rightBellCenter.y - bellRadius),
                size = Size(bellRadius * 2f, bellRadius * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // 4. Clock Hands (10:00 position)
            drawCircle(
                color = color,
                radius = strokeWidth * 0.75f,
                center = Offset(cx, cy),
            )

            // Minute hand (12:00)
            drawLine(
                color = color,
                start = Offset(cx, cy),
                end = Offset(cx, cy - r * 0.62f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            // Hour hand (10:00)
            drawLine(
                color = color,
                start = Offset(cx, cy),
                end = Offset(cx - r * 0.36f, cy - r * 0.20f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }

        // 5. Floating Z marks restricted to their own dedicated, non-overlapping spatial lanes
        fun drawZMarkInLane(
            baseX: Float,
            baseY: Float,
            driftDistanceDp: Dp,
            sizePx: Float,
            swPx: Float,
            progress: Float,
            maxRotation: Float,
        ) {
            val p = progress.coerceIn(0f, 1f)
            val driftY = -driftDistanceDp.toPx() * p
            val driftX = (driftDistanceDp.toPx() * 0.55f) * p

            val alpha = when {
                p < 0.20f -> p / 0.20f
                p > 0.75f -> (1f - p) / 0.25f
                else -> 1f
            }

            val x = baseX + driftX
            val y = baseY + driftY
            val zColor = color.copy(alpha = color.alpha * alpha)
            val rotationAngle = sin(p * Math.PI.toFloat() * 2f) * maxRotation

            rotate(degrees = rotationAngle, pivot = Offset(x + sizePx / 2f, y + sizePx * 0.55f)) {
                val zPath = Path().apply {
                    moveTo(x, y)
                    lineTo(x + sizePx, y)
                    lineTo(x, y + sizePx * 1.1f)
                    lineTo(x + sizePx, y + sizePx * 1.1f)
                }
                drawPath(
                    path = zPath,
                    color = zColor,
                    style = Stroke(width = swPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        // Smallest Z - Lowest Lane (well above top of right bell)
        drawZMarkInLane(
            baseX = cx + r * 0.55f,
            baseY = cy - r * 1.55f,
            driftDistanceDp = 6.dp,
            sizePx = 9.dp.toPx(),
            swPx = 2.2.dp.toPx(),
            progress = z0Progress,
            maxRotation = -4.0f,
        )

        // Medium Z - Middle Lane (guaranteed 12dp gap above Smallest Z top point)
        drawZMarkInLane(
            baseX = cx + r * 1.05f,
            baseY = cy - r * 2.50f,
            driftDistanceDp = 8.dp,
            sizePx = 13.dp.toPx(),
            swPx = 2.8.dp.toPx(),
            progress = z1Progress,
            maxRotation = 4.5f,
        )

        // Largest Z - Highest Lane (guaranteed 14dp gap above Medium Z top point)
        drawZMarkInLane(
            baseX = cx + r * 1.55f,
            baseY = cy - r * 3.65f,
            driftDistanceDp = 10.dp,
            sizePx = 18.dp.toPx(),
            swPx = 3.5.dp.toPx(),
            progress = z2Progress,
            maxRotation = -4.0f,
        )
    }
}
