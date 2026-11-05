package app.expressiveclock.ui.screens

import android.text.format.DateFormat
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.expressiveclock.ui.ClockViewModel
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.expressiveclock.core.WorldCity
import app.expressiveclock.ui.WorldMapData
import app.expressiveclock.ui.glass
import app.expressiveclock.ui.luminousElasticClickable
import app.expressiveclock.ui.rememberWallTicker
import app.expressiveclock.ui.sheets.prettify
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The expanded Alarms pane's world-clock surface. It deliberately owns only presentation and the
 * transient selection; city persistence remains in [ClockStore] through [ClockViewModel].
 */
@Composable
fun WorldClockPanel(
    cities: List<WorldCity>,
    selectedZoneId: String?,
    onSelectCity: (WorldCity) -> Unit,
    onAddLocation: () -> Unit,
    onShowLocalTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by rememberWallTicker()
    val selected = cities.firstOrNull { it.zone.id == selectedZoneId }
    // This presentation-only entry is never written to the saved-city store.
    // The zone's representative city is a label, not a claim of GPS location.
    val localCity = prettify(ZoneId.systemDefault().id).copy(region = "Local Time")
    val mapCities = (listOf(localCity) + cities).distinctBy { it.zone.id }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .glass(
                shape = RoundedCornerShape(28.dp),
                tint = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .94f),
                borderAlpha = .82f,
                shadowElevation = 14.dp,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DottedWorldMap(
            cities = mapCities,
            selectedZoneId = selected?.zone?.id ?: localCity.zone.id,
            onSelectCity = { city ->
                if (city.zone == localCity.zone) onShowLocalTime() else onSelectCity(city)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(.45f)
                .heightIn(min = 150.dp),
        )

        SavedLocationList(
            cities = cities,
            localCity = localCity,
            onShowLocalTime = onShowLocalTime,
            selectedZoneId = selected?.zone?.id,
            nowUtcMillis = now,
            onSelectCity = onSelectCity,
            modifier = Modifier
                .fillMaxWidth()
                .weight(.55f),
        )

        AddLocationButton(onClick = onAddLocation)
    }
    }
}

@Composable
private fun DottedWorldMap(
    cities: List<WorldCity>,
    selectedZoneId: String?,
    onSelectCity: (WorldCity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val markerCities = remember(cities) {
        cities.filter { WorldMapData.markerOffset(it.zone.id, it.city)?.let { p ->
            p.x in 0f..1f && p.y in 0f..1f
        } == true }
    }
    val gold = if (MaterialTheme.colorScheme.surface.luminance() < .5f)
        Color(0xFFFFD166) else Color(0xFFE8AA22)
    val pulseTransition = rememberInfiniteTransition(label = "selectedMarkerBreathing")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "selectedMarkerPulse",
    )
    val markerAppearance = markerCities.associate { city ->
        key(city.zone.id) {
            val selected = city.zone.id == selectedZoneId
            val color by animateColorAsState(
                if (selected) gold else primary,
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                label = "worldMarkerColor",
            )
            val glow by animateFloatAsState(
                if (selected) 1f else 0f,
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                label = "worldMarkerGlow",
            )
            city.zone.id to (color to glow)
        }
    }
    Canvas(
        modifier = modifier
            .pointerInput(markerCities, selectedZoneId) {
                detectTapGestures { tap ->
                    markerLayout(
                        markerCities,
                        worldMapBounds(size.width.toFloat(), size.height.toFloat(), 16.dp.toPx(), 8.dp.toPx()),
                    )
                        .minByOrNull { (_, point) -> (point - tap).getDistanceSquared() }
                        ?.takeIf { (_, point) -> (point - tap).getDistanceSquared() < 34f * 34f }
                        ?.let { (city, _) -> onSelectCity(city) }
                }
            },
    ) {
        val mapBounds = worldMapBounds(size.width, size.height, 16.dp.toPx(), 8.dp.toPx())
        val mapGlow = primary.copy(alpha = .11f)
        drawCircle(
            brush = Brush.radialGradient(listOf(mapGlow, Color.Transparent)),
            radius = size.maxDimension * .55f,
            center = center,
        )
        WorldMapData.DOT_OFFSETS.forEach { offset ->
            drawCircle(
                color = primary.copy(alpha = .70f),
                radius = 1.15f,
                center = Offset(
                    mapBounds.left + offset.x * mapBounds.width,
                    mapBounds.top + offset.y * mapBounds.height,
                ),
            )
        }
        markerLayout(markerCities, mapBounds).sortedBy { it.first.zone.id == selectedZoneId }.forEach { (city, point) ->
            val selected = city.zone.id == selectedZoneId
            val (color, glow) = markerAppearance.getValue(city.zone.id)
            drawLocationMarker(point, color, selected, glow, pulse)
        }
    }
}

/** Grow uniformly into the panel's existing padding without moving the list or clipping land. */
private fun worldMapBounds(width: Float, height: Float, horizontalMargin: Float, verticalMargin: Float): Rect {
    val growth = minOf(
        1.09f,
        1f + 2f * horizontalMargin / width.coerceAtLeast(1f),
        1f + 2f * verticalMargin / height.coerceAtLeast(1f),
    )
    val mapWidth = width * .97f * growth
    val mapHeight = height * .97f * growth
    return Rect(
        (width - mapWidth) / 2f, (height - mapHeight) / 2f,
        (width + mapWidth) / 2f, (height + mapHeight) / 2f,
    )
}

private fun markerLayout(cities: List<WorldCity>, bounds: Rect): List<Pair<WorldCity, Offset>> {
    val result = mutableListOf<Pair<WorldCity, Offset>>()
    cities.forEach { city ->
        val base = WorldMapData.markerOffset(city.zone.id, city.city) ?: return@forEach
        val basePoint = Offset(bounds.left + base.x * bounds.width, bounds.top + base.y * bounds.height)
        result += city to basePoint
    }
    return result
}

private fun DrawScope.drawLocationMarker(center: Offset, color: Color, selected: Boolean, glow: Float, pulse: Float) {
    val coreRadius = if (selected) 8f else 5.5f
    // Only the outer glow breathes; selection strength transfers the pulse smoothly between pins.
    val blueRadius = coreRadius + 10f
    val goldenRadius = 16.dp.toPx() + 12.dp.toPx() * pulse
    val glowRadius = blueRadius + (goldenRadius - blueRadius) * glow
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                color.copy(alpha = .14f + glow * (.22f - .20f * pulse)),
                Color.Transparent,
            ),
            center = center,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = center,
    )
    drawCircle(
        color = color.copy(alpha = .42f + glow * (.12f - .18f * pulse)),
        radius = coreRadius + 4f,
        center = center,
    )
    drawCircle(color = color, radius = coreRadius, center = center)
    drawCircle(color = Color.White.copy(alpha = .96f), radius = if (selected) 4.5f else 2.8f, center = center)
}

@Composable
private fun SavedLocationList(
    cities: List<WorldCity>,
    localCity: WorldCity,
    onShowLocalTime: () -> Unit,
    selectedZoneId: String?,
    nowUtcMillis: Long,
    onSelectCity: (WorldCity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val vm: ClockViewModel = viewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var revealedCity by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val topFade by animateFloatAsState(
        if (state.firstVisibleItemIndex > 0) 1f
        else (state.firstVisibleItemScrollOffset / with(density) { 32.dp.toPx() }).coerceIn(0f, 1f),
        label = "worldListTopFade",
    )
    LaunchedEffect(vm.newlyAddedCity, cities) {
        val id = vm.newlyAddedCity ?: return@LaunchedEffect
        val index = cities.indexOfFirst { it.zone.id == id }
        if (index < 0) return@LaunchedEffect
        delay(260) // Let the right-pane exit finish before revealing the persisted addition.
        // Clamp the spring's target before starting it. Otherwise the list reaches its bottom
        // early, but animateScrollBy keeps animating an unreachable target while the card is hidden.
        val stride = with(density) { 56.dp.toPx() }
        val currentScroll = state.firstVisibleItemIndex * stride + state.firstVisibleItemScrollOffset
        val viewport = state.layoutInfo.let { it.viewportEndOffset - it.viewportStartOffset }
        val contentHeight = cities.size * stride - with(density) { 2.dp.toPx() } +
            with(density) { 48.dp.toPx() }
        val maxScroll = (contentHeight - viewport).coerceAtLeast(0f)
        val targetScroll = (index * stride).coerceAtMost(maxScroll)
        val distance = targetScroll - currentScroll
        // Use animateScrollBy's same spring and consumed-distance calculation, but release
        // the entrance during its final visible approach instead of awaiting the spring's tail.
        val revealDistance = with(density) { 8.dp.toPx() }
        val blurInset = with(density) { 32.dp.toPx() }
        state.scroll {
            var consumed = 0f
            animate(
                0f, distance,
                animationSpec = spring(dampingRatio = .82f, stiffness = 45f, visibilityThreshold = .5f),
            ) { value, _ ->
                consumed += scrollBy(value - consumed)
                val layout = state.layoutInfo
                val card = layout.visibleItemsInfo.firstOrNull { it.key == id }
                if (kotlin.math.abs(distance - consumed) <= revealDistance &&
                    card != null && card.offset >= layout.viewportStartOffset &&
                    card.offset + card.size <= layout.viewportEndOffset - blurInset) {
                    revealedCity = id
                }
            }
        }
        revealedCity = id
    }
    Column(modifier = modifier.heightIn(min = 62.dp)) {
        SavedLocationRow(
            city = localCity,
            selected = selectedZoneId == null,
            nowUtcMillis = nowUtcMillis,
            onClick = onShowLocalTime,
            modifier = if (selectedZoneId == null) Modifier else Modifier.border(
                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f), RoundedCornerShape(18.dp),
            ),
        )
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth().weight(1f).progressiveListEdgeMask(topFade),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(cities, key = { it.zone.id }) { city ->
                SwipeLocation(
                    city = city,
                    selected = city.zone.id == selectedZoneId,
                    nowUtcMillis = nowUtcMillis,
                    onSelect = { onSelectCity(city) },
                    onDelete = {
                        vm.removeCity(city.zone)
                        if (selectedZoneId == city.zone.id) onShowLocalTime()
                    },
                    entering = revealedCity == city.zone.id && vm.newlyAddedCity == city.zone.id,
                    pendingEntrance = vm.newlyAddedCity == city.zone.id,
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = spring(dampingRatio = .65f, stiffness = 260f),
                    ),
                    tutorial = !settings.world.deleteTutorialShown,
                    onEntranceFinished = {
                        // Clearing the pending id is an effect key change: wait for the
                        // residual scroll to end so it cannot cancel the original scroll.
                        scope.launch {
                            snapshotFlow { state.isScrollInProgress }.first { !it }
                            if (vm.newlyAddedCity == city.zone.id) vm.finishCityEntrance()
                        }
                    },
                    onTutorialFinished = {
                        vm.updateSettings { it.copy(world = it.world.copy(deleteTutorialShown = true)) }
                    },
                )
            }
        }
        if (cities.isEmpty()) {
            Text(
                "Add a city to begin your world clock.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun SwipeLocation(
    city: WorldCity,
    selected: Boolean,
    nowUtcMillis: Long,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    entering: Boolean,
    pendingEntrance: Boolean,
    tutorial: Boolean,
    onEntranceFinished: () -> Unit,
    onTutorialFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val revealWidth = with(density) { 78.dp.toPx() }
    val flingThreshold = with(density) { 1100.dp.toPx() }
    val slide = remember(city.zone.id) { Animatable(0f) }
    val entrance = remember(city.zone.id) { Animatable(if (pendingEntrance) 0f else 1f) }
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    var deleting by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    var motion by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(entering) {
        if (!entering) return@LaunchedEffect
        entrance.animateTo(1f, spring(dampingRatio = .65f, stiffness = 380f))
        if (tutorial && !touched) {
            motion = scope.launch {
                delay(350)
                if (!touched) {
                    onTutorialFinished()
                    slide.animateTo(-revealWidth, tween(380, easing = FastOutSlowInEasing))
                    delay(500)
                    slide.animateTo(0f, spring(dampingRatio = .78f, stiffness = 300f))
                }
            }
        }
        onEntranceFinished()
    }
    BoxWithConstraints(modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp))) {
        val width = with(density) { maxWidth.toPx() }
        fun delete(velocity: Float = 0f) {
            if (deleting) return
            deleting = true
            motion?.cancel()
            motion = scope.launch {
                slide.animateTo(-width, spring(dampingRatio = .95f, stiffness = 500f), initialVelocity = velocity)
                // Removing only after the foreground exits gives the remaining keyed rows
                // one spring placement animation instead of chasing a shrinking row each frame.
                onDelete()
            }
        }
        val position = if (dragging) dragPosition else slide.value
        if (position < -1f) {
            Box(
                Modifier.matchParentSize().background(MaterialTheme.colorScheme.error),
            ) {
                Box(
                    Modifier.align(Alignment.CenterEnd).size(78.dp, 54.dp)
                        .clickable(enabled = !deleting, onClick = { delete() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }
        SavedLocationRow(
            city = city, selected = selected, nowUtcMillis = nowUtcMillis,
            onClick = { if (!deleting && !pendingEntrance) onSelect() },
            modifier = Modifier.requiredHeight(54.dp).graphicsLayer {
                translationX = position
                scaleX = .9f + .1f * entrance.value
                scaleY = .9f + .1f * entrance.value
                alpha = entrance.value.coerceIn(0f, 1f)
            }.clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .draggable(
                    enabled = !deleting && !pendingEntrance,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragPosition = (dragPosition + delta).coerceIn(-width, 0f)
                    },
                    onDragStarted = {
                        touched = true
                        motion?.cancel()
                        slide.stop()
                        dragPosition = slide.value
                        dragging = true
                    },
                    onDragStopped = { velocity ->
                        slide.snapTo(dragPosition)
                        dragging = false
                        if (dragPosition < -width * .55f ||
                            (velocity < -flingThreshold && dragPosition < -with(density) { 24.dp.toPx() })) {
                            delete(velocity)
                        } else {
                            motion = scope.launch {
                                slide.animateTo(
                                    if (velocity > flingThreshold || dragPosition > -revealWidth * .35f) 0f else -revealWidth,
                                    spring(dampingRatio = .85f, stiffness = 400f),
                                    initialVelocity = velocity,
                                )
                            }
                        }
                    },
                ),
        )
    }
}

@Composable
private fun SavedLocationRow(
    city: WorldCity,
    selected: Boolean,
    nowUtcMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val local = city.timeAt(nowUtcMillis)
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val time = remember(local, timePattern) { local.format(DateTimeFormatter.ofPattern(timePattern, Locale.getDefault())) }
    val date = remember(local) { local.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())) }
    val primary = MaterialTheme.colorScheme.primary
    val rowColor = if (selected) primary.copy(alpha = .12f) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(rowColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp)) {
            if (selected) Icon(Icons.Rounded.Check, null, tint = primary, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.size(7.dp))
        Icon(
            Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                city.city,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                city.region,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                time,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = "Select ${city.city}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f),
            modifier = Modifier.padding(start = 4.dp).size(20.dp),
        )
    }
}

@Composable
private fun AddLocationButton(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .luminousElasticClickable(
                shape = CircleShape,
                maxStretchPercent = .04f,
                maxTranslateDp = 1.2.dp,
                onClick = onClick,
            )
            .clip(CircleShape)
            .background(primary.copy(alpha = .12f), CircleShape)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, null, tint = primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.size(8.dp))
        Text("Add Location", style = MaterialTheme.typography.titleMedium, color = primary)
    }
}

@Composable
internal fun Modifier.progressiveListEdgeMask(topStrength: Float, bottomStrength: Float = 1f): Modifier {
    val sharp = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    return graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    if (topStrength <= 0f && bottomStrength <= 0f) {
        drawContent()
        return@drawWithContent
    }
    // Record once, then crossfade sharp and blurred samples in stationary viewport bands.
    sharp.record { this@drawWithContent.drawContent() }
    val edgeFraction = (32.dp.toPx() / size.height.coerceAtLeast(1f)).coerceAtMost(.25f)
    val clearMask = Brush.verticalGradient(
        0f to Color.Black.copy(alpha = 1f - topStrength), edgeFraction to Color.Black,
        (1f - edgeFraction) to Color.Black, 1f to Color.Black.copy(alpha = 1f - bottomStrength),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        blurred.record { drawLayer(sharp) }
        blurred.renderEffect = RenderEffect.createBlurEffect(
            5.dp.toPx(), 5.dp.toPx(), Shader.TileMode.CLAMP,
        ).asComposeRenderEffect()
        val canvas = drawContext.canvas
        val bounds = Rect(Offset.Zero, size)
        canvas.saveLayer(bounds, Paint())
        drawLayer(sharp)
        drawRect(clearMask, blendMode = BlendMode.DstIn)
        canvas.restore()
        canvas.saveLayer(bounds, Paint())
        drawLayer(blurred)
        drawRect(clearMask, blendMode = BlendMode.DstOut)
        canvas.restore()
    } else {
        drawLayer(sharp)
    }
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Black.copy(alpha = 1f - topStrength),
                edgeFraction to Color.Black,
                (1f - edgeFraction) to Color.Black,
                1f to Color.Black.copy(alpha = 1f - bottomStrength),
            ),
        ),
        blendMode = BlendMode.DstIn,
    )
}
}

