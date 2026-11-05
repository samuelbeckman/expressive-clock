package app.expressiveclock.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TWO_PANE_MIN_WIDTH = 600.dp
private val TWO_PANE_MIN_HEIGHT = 600.dp

/**
 * Layout classes for responsive switching in Expressive Clock.
 *
 * - [COMPACT]: Either dimension is below 600dp (including phones in landscape).
 * - [EXPANDED]: Width and height are both at least 600dp (unfolded/tablet two-pane layout).
 */
enum class AppWindowWidthClass {
    COMPACT,
    EXPANDED,
}

/**
 * Window size information wrapper.
 */
data class AppWindowSizeInfo(
    val widthClass: AppWindowWidthClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
) {
    /** Short windows need scrolling even when there are no laps or other list items yet. */
    val isHeightCompact: Boolean
        get() = screenHeightDp < TWO_PANE_MIN_HEIGHT

    val isExpanded: Boolean
        get() = widthClass == AppWindowWidthClass.EXPANDED

    val isCompact: Boolean
        get() = widthClass == AppWindowWidthClass.COMPACT
}

/**
 * Calculates the current window size info from both available window dimensions.
 *
 * Re-evaluates dynamically when screen width changes (e.g. folding/unfolding, window resizing).
 */
@Composable
fun rememberAppWindowSizeInfo(): AppWindowSizeInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp

    val widthClass = if (widthDp >= TWO_PANE_MIN_WIDTH && heightDp >= TWO_PANE_MIN_HEIGHT) {
        AppWindowWidthClass.EXPANDED
    } else {
        AppWindowWidthClass.COMPACT
    }

    return AppWindowSizeInfo(
        widthClass = widthClass,
        screenWidthDp = widthDp,
        screenHeightDp = heightDp,
    )
}

/**
 * CompositionLocal providing [AppWindowSizeInfo] throughout the UI hierarchy.
 */
val LocalAppWindowSizeInfo = staticCompositionLocalOf {
    AppWindowSizeInfo(
        widthClass = AppWindowWidthClass.COMPACT,
        screenWidthDp = 360.dp,
        screenHeightDp = 800.dp,
    )
}
