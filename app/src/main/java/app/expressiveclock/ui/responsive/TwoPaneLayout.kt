package app.expressiveclock.ui.responsive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A flexible, configurable two-pane layout foundation for expanded window widths.
 *
 * Supports:
 * - Configurable left-pane and right-pane composables.
 * - Configurable left/right weight proportions (e.g. 1f to 1f for equal split, or 0.38f to 0.62f for Master-Detail).
 * - Configurable horizontal gap between panes.
 * - Proper handling of independent pane bounds and edge clipping.
 */
@Composable
fun TwoPaneLayout(
    leftPane: @Composable BoxScope.() -> Unit,
    rightPane: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    leftWeight: Float = 1f,
    rightWeight: Float = 1f,
    paneSpacing: Dp = 24.dp,
    contentPaddingDp: Dp = 24.dp,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = contentPaddingDp),
        horizontalArrangement = Arrangement.spacedBy(paneSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(leftWeight)
                .fillMaxHeight()
                .clipToBounds(),
            contentAlignment = Alignment.Center,
            content = leftPane,
        )
        Box(
            modifier = Modifier
                .weight(rightWeight)
                .fillMaxHeight()
                .clipToBounds(),
            contentAlignment = Alignment.Center,
            content = rightPane,
        )
    }
}
