package app.expressiveclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

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
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
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
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return this
        .graphicsLayer {
            val scale = if (pressed) .965f else 1f
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = source,
            indication = null,
            onClick = onClick,
        )
}
