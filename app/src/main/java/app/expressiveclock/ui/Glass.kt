package app.expressiveclock.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Clean white canvas for the glass treatment. Colour comes from subtle blue light, not a gradient page. */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.background(androidx.compose.material3.MaterialTheme.colorScheme.background)) { content() }
}

/**
 * A restrained glass surface for a white canvas: translucent white, a cool blue inner sheen and a
 * bright edge. The shadow is intentionally soft enough to read as light passing through glass.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.White.copy(alpha = .72f),
    borderAlpha: Float = .82f,
    accent: Color? = null,
    shadowElevation: androidx.compose.ui.unit.Dp = 18.dp,
): Modifier {
    val resolvedAccent = accent ?: androidx.compose.material3.MaterialTheme.colorScheme.primary
    val shadow = if (shadowElevation > 0.dp) {
        Modifier.shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = resolvedAccent.copy(alpha = if (tint.luminance() < .5f) .36f else .20f),
            spotColor = resolvedAccent.copy(alpha = if (tint.luminance() < .5f) .50f else .30f),
        )
    } else {
        Modifier
    }
    return this
    .then(shadow)
    .background(
        Brush.linearGradient(
            colors = if (tint.luminance() < .5f) {
                listOf(
                    tint.copy(alpha = .94f),
                    tint.copy(alpha = .78f),
                    resolvedAccent.copy(alpha = .26f),
                )
            } else {
                listOf(
                    tint.copy(alpha = .96f),
                    tint.copy(alpha = .78f),
                    resolvedAccent.copy(alpha = .09f),
                )
            },
            start = Offset(0f, 0f),
            end = Offset(900f, 900f),
        ),
        shape,
    )
    .border(1.25.dp, if (tint.luminance() < .5f) Color.White.copy(alpha = .16f) else Color.White.copy(alpha = borderAlpha), shape)
}

/**
 * A controllable blue drop shadow for large circular surfaces. This deliberately avoids
 * RenderNode elevation because very large colored shadows can be rasterized with a faceted
 * outline on some Android devices. A radial field lives in a larger parent box, so the shadow
 * cannot be clipped by the circle itself.
 */
@Composable
fun FloatingCircle(
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    fill: Color,
    border: Color = Color.White.copy(alpha = .82f),
    shadowColor: Color = Color(0xFF1687D0),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.size(size + 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f + 12.dp.toPx()
            val r = this.size.width * .48f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        shadowColor.copy(alpha = .30f),
                        shadowColor.copy(alpha = .14f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = r + 28.dp.toPx(),
                ),
                radius = r + 28.dp.toPx(),
                center = Offset(cx, cy),
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .background(fill, androidx.compose.foundation.shape.CircleShape)
                .border(1.25.dp, border, androidx.compose.foundation.shape.CircleShape),
            content = content,
        )
    }
}

/** A small theme-coloured drop shadow beneath floating controls. */
@Composable
fun Modifier.blueGlow(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(32.dp),
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
): Modifier = this.shadow(
    elevation = 34.dp,
    shape = shape,
    clip = false,
    ambientColor = color.copy(alpha = .24f),
    spotColor = color.copy(alpha = .40f),
)

/** A subtle elastic press response for interactive UI. */
@Composable
fun Modifier.elasticClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    return this.luminousElasticClickable(
        enabled = enabled,
        shape = CircleShape,
        onClick = onClick,
    )
}

/**
 * An expressive, touch-reactive interaction modifier providing:
 * - Touch-originated radial ripple
 * - Finger-following luminous highlight
 * - One-sided directional elastic deformation
 * - Whole-component physical translation with spring release overshoot
 * - Hard platform/GPU offscreen layer clipping
 */
@Composable
fun Modifier.luminousElasticClickable(
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    maxStretchPercent: Float = 0.092f,
    maxTranslateDp: Dp = 2.6.dp,
    luminousRadiusScale: Float = 1.0f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isInteractionPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()

    var rawTouchPosition by remember { mutableStateOf<Offset?>(null) }
    var lastTouchPosition by remember { mutableStateOf<Offset?>(null) }
    var lastRippleOrigin by remember { mutableStateOf<Offset?>(null) }
    val rippleProgress = remember { Animatable(1f) }

    var targetDx by remember { mutableFloatStateOf(0f) }
    var targetDy by remember { mutableFloatStateOf(0f) }

    // Active press state derived from either pointer touch or interaction source
    val isPressed = rawTouchPosition != null || isInteractionPressed

    val animatedDx by animateFloatAsState(
        targetValue = targetDx,
        animationSpec = if (isPressed) {
            spring(
                stiffness = 550f,
                dampingRatio = 0.82f,
            )
        } else {
            spring(
                stiffness = 350f,
                dampingRatio = 0.52f,
            )
        },
        label = "elasticDx",
    )

    val animatedDy by animateFloatAsState(
        targetValue = targetDy,
        animationSpec = if (isPressed) {
            spring(
                stiffness = 550f,
                dampingRatio = 0.82f,
            )
        } else {
            spring(
                stiffness = 350f,
                dampingRatio = 0.52f,
            )
        },
        label = "elasticDy",
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 90, easing = FastOutSlowInEasing)
        } else {
            spring(
                stiffness = 420f,
                dampingRatio = 0.62f,
            )
        },
        label = "luminousElasticScale",
    )

    val luminousAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 120, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 260, easing = FastOutSlowInEasing)
        },
        label = "luminousAlpha",
    )

    val surfaceLuminance = MaterialTheme.colorScheme.surface.luminance()
    val isDarkTheme = surfaceLuminance < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary

    // Vibrant, rich theme-adaptive luminous bloom colors:
    val centerColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.48f * luminousAlpha)
    } else {
        primaryColor.copy(alpha = 0.28f * luminousAlpha)
    }

    val outerColor = if (isDarkTheme) {
        Color(0xFF80D8FF).copy(alpha = 0.22f * luminousAlpha)
    } else {
        primaryColor.copy(alpha = 0.11f * luminousAlpha)
    }

    var mod = this.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull()
                if (change != null) {
                    if (change.pressed) {
                        val pos = change.position
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        if (rawTouchPosition == null) {
                            lastRippleOrigin = pos
                            scope.launch {
                                rippleProgress.snapTo(0f)
                                rippleProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                                )
                            }
                        }
                        rawTouchPosition = pos
                        lastTouchPosition = pos
                        if (cx > 0f && cy > 0f) {
                            targetDx = ((pos.x - cx) / cx).coerceIn(-1f, 1f)
                            targetDy = ((pos.y - cy) / cy).coerceIn(-1f, 1f)
                        }
                    } else {
                        rawTouchPosition = null
                        targetDx = 0f
                        targetDy = 0f
                    }
                }
            }
        }
    }

    mod = mod
        .graphicsLayer {
            val maxTranslatePx = maxTranslateDp.toPx()
            val stretchX = 1f + maxStretchPercent * abs(animatedDx) - 0.03f * abs(animatedDy)
            val stretchY = 1f + (maxStretchPercent * 1.06f) * abs(animatedDy) - 0.03f * abs(animatedDx)
            val netScaleX = animatedScale * stretchX
            val netScaleY = animatedScale * stretchY

            val pivotX = (0.5f - 0.30f * animatedDx).coerceIn(0.15f, 0.85f)
            val pivotY = (0.5f - 0.30f * animatedDy).coerceIn(0.15f, 0.85f)
            transformOrigin = TransformOrigin(pivotX, pivotY)

            scaleX = netScaleX
            scaleY = netScaleY
            translationX = animatedDx * maxTranslatePx
            translationY = animatedDy * maxTranslatePx
            clip = true
            this.shape = shape
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .clip(shape)
        .drawWithContent {
            drawContent()

            val w = size.width
            val h = size.height
            val currentTouchCenter = rawTouchPosition ?: lastTouchPosition ?: Offset(w / 2f, h / 2f)

            if (luminousAlpha > 0.01f) {
                val baseBloomRadius = if (isDarkTheme) {
                    size.maxDimension * 0.75f
                } else {
                    size.minDimension * 1.15f + size.maxDimension * 0.22f
                }
                val bloomRadius = baseBloomRadius * luminousRadiusScale

                val luminousBrush = if (isDarkTheme) {
                    Brush.radialGradient(
                        colors = listOf(
                            centerColor,
                            outerColor,
                            Color.Transparent,
                        ),
                        center = currentTouchCenter,
                        radius = bloomRadius,
                    )
                } else {
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to centerColor,
                            0.38f to primaryColor.copy(alpha = 0.15f * luminousAlpha),
                            0.72f to outerColor.copy(alpha = 0.04f * luminousAlpha),
                            1.00f to Color.Transparent,
                        ),
                        center = currentTouchCenter,
                        radius = bloomRadius,
                    )
                }
                when (val outline = shape.createOutline(size, layoutDirection, this)) {
                    is Outline.Rounded -> drawRoundRect(
                        brush = luminousBrush,
                        cornerRadius = outline.roundRect.topLeftCornerRadius,
                    )
                    is Outline.Generic -> drawPath(
                        path = outline.path,
                        brush = luminousBrush,
                    )
                    is Outline.Rectangle -> drawRect(
                        brush = luminousBrush,
                    )
                }
            }

            val p = rippleProgress.value
            val rippleAlpha = (1f - p) * (if (isPressed) 1f else luminousAlpha)
            if (p < 1f && rippleAlpha > 0.01f && lastRippleOrigin != null) {
                val maxRippleRadius = size.maxDimension * 0.90f
                val minRippleRadius = 12.dp.toPx()
                val currentRippleRadius = minRippleRadius + (maxRippleRadius - minRippleRadius) * p
                val rippleOrigin = Offset(
                    lastRippleOrigin!!.x.coerceIn(0f, w),
                    lastRippleOrigin!!.y.coerceIn(0f, h),
                )

                val rippleBrush = Brush.radialGradient(
                    colors = listOf(
                        centerColor.copy(alpha = centerColor.alpha * 0.85f * rippleAlpha),
                        outerColor.copy(alpha = outerColor.alpha * 0.45f * rippleAlpha),
                        Color.Transparent,
                    ),
                    center = rippleOrigin,
                    radius = currentRippleRadius,
                )
                when (val outline = shape.createOutline(size, layoutDirection, this)) {
                    is Outline.Rounded -> drawRoundRect(
                        brush = rippleBrush,
                        cornerRadius = outline.roundRect.topLeftCornerRadius,
                    )
                    is Outline.Generic -> drawPath(
                        path = outline.path,
                        brush = rippleBrush,
                    )
                    is Outline.Rectangle -> drawRect(
                        brush = rippleBrush,
                    )
                }
            }
        }

    return if (onClick != null) {
        mod.combinedClickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        mod
    }
}
