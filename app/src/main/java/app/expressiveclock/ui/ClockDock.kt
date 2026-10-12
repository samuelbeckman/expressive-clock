package app.expressiveclock.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * Fixed-stage dock. The add button never changes the measured width of the stage, so moving from
 * Alarms to another tab cannot make the dock jump/stutter. Instead the dock and add button animate
 * within the same centered stage.
 */
@Composable
fun ClockDock(
    destinations: List<Tab>,
    selected: Tab,
    onSelect: (Tab) -> Unit,
    showAdd: Boolean,
    addLabel: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    enableGlow: Boolean = true,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val blue = MaterialTheme.colorScheme.primary
    val dockTint = if (dark) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .88f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .84f)

    val dockShift by animateDpAsState(
        targetValue = if (showAdd) 0.dp else (DOCK_HEIGHT + FAB_GAP) / 2,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 560f),
        label = "dockShift",
    )
    val addAlpha by animateFloatAsState(
        targetValue = if (showAdd) 1f else 0f,
        animationSpec = tween(180),
        label = "addAlpha",
    )
    val addScale by animateFloatAsState(
        targetValue = if (showAdd) 1f else .78f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 620f),
        label = "addScale",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(DOCK_STAGE_WIDTH)
                .padding(horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FAB_GAP),
        ) {
            Box(
                modifier = Modifier
                    .width(DOCK_WIDTH)
                    .height(DOCK_HEIGHT)
                    .graphicsLayer { translationX = dockShift.toPx() }
                    .glass(
                        RoundedCornerShape(DOCK_HEIGHT / 2),
                        dockTint,
                        .88f,
                        shadowElevation = if (enableGlow) 18.dp else 0.dp,
                    )
                    .then(if (enableGlow) Modifier.blueGlow(RoundedCornerShape(DOCK_HEIGHT / 2)) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val selectedIndex = destinations.indexOf(selected).coerceAtLeast(0)
                    val itemWidth = maxWidth / destinations.size
                    val indicatorSize = 50.dp
                    val selectedOffset by animateDpAsState(
                        targetValue = itemWidth * selectedIndex + (itemWidth - indicatorSize) / 2,
                        animationSpec = spring(dampingRatio = 0.84f, stiffness = 600f),
                        label = "dockIndicator",
                    )
                    Box(
                        modifier = Modifier
                            .offset(
                                x = selectedOffset,
                                y = (DOCK_HEIGHT - indicatorSize) / 2,
                            )
                            .size(indicatorSize)
                            .clip(CircleShape)
                            .background(
                                if (dark) Color(0xFF24303B) else Color.White.copy(alpha = .99f)
                            ),
                    )
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        destinations.forEach { d ->
                            DockItem(
                                tab = d,
                                selected = d == selected,
                                iconColor = if (dark) Color.White.copy(alpha = .94f) else blue,
                                onClick = { onSelect(d) },
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(DOCK_HEIGHT)
                    .graphicsLayer {
                        alpha = addAlpha
                        scaleX = addScale
                        scaleY = addScale
                    }
                    .clip(CircleShape)
                    .glass(
                        CircleShape,
                        if (dark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f),
                        .92f,
                        shadowElevation = if (enableGlow) 18.dp else 0.dp,
                    )
                    .then(if (enableGlow) Modifier.blueGlow(CircleShape) else Modifier)
                    .clearAndSetSemantics { contentDescription = addLabel },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .elasticClickable(enabled = showAdd, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(31.dp))
                }
            }
        }
    }
}

@Composable
private fun RowScope.DockItem(tab: Tab, selected: Boolean, iconColor: Color, onClick: () -> Unit) {
    var tapCount by remember { mutableIntStateOf(0) }
    val rotation = remember { Animatable(0f) }
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1f else .94f,
        animationSpec = tween(180),
        label = "dockScale",
    )

    LaunchedEffect(tapCount) {
        if (tapCount == 0) return@LaunchedEffect
        rotation.snapTo(0f)
        when (tab) {
            Tab.ALARMS -> {
                rotation.animateTo(-5f, tween(65))
                rotation.animateTo(5f, tween(85))
                rotation.animateTo(-3f, tween(65))
                rotation.animateTo(0f, tween(65))
            }
            Tab.TIMERS -> rotation.animateTo(180f, tween(320))
            Tab.STOPWATCH -> rotation.animateTo(360f, tween(420))
        }
        rotation.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(DOCK_HEIGHT)
            .elasticClickable(
                onClick = {
                    tapCount++
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (selected) MaterialTheme.colorScheme.onSurface else iconColor,
            modifier = Modifier
                .size(27.dp)
                .graphicsLayer {
                    scaleX = selectedScale
                    scaleY = selectedScale
                    when (tab) {
                        Tab.ALARMS -> rotationZ = rotation.value
                        Tab.TIMERS -> rotationZ = rotation.value
                        Tab.STOPWATCH -> rotationZ = rotation.value
                    }
                    cameraDistance = 18f * density
                },
        )
    }
}

val DOCK_HEIGHT = 72.dp
private val DOCK_WIDTH = 204.dp
private val FAB_GAP = 10.dp
private val DOCK_STAGE_WIDTH = DOCK_WIDTH + FAB_GAP + DOCK_HEIGHT
