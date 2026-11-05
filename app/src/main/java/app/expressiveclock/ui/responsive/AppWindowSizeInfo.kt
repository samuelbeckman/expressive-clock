package app.expressiveclock.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window width classes for responsive layout switching in Expressive Clock.
 *
 * - [COMPACT]: Content width < 600dp (Phone portrait layout).
 * - [EXPANDED]: Content width >= 600dp (Foldables unfolded, tablets, two-pane layout).
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
    val isExpanded: Boolean
        get() = widthClass == AppWindowWidthClass.EXPANDED

    val isCompact: Boolean
        get() = widthClass == AppWindowWidthClass.COMPACT
}

/**
 * Calculates the current window size info based on available content width.
 *
 * Re-evaluates dynamically when screen width changes (e.g. folding/unfolding, window resizing).
 */
@Composable
fun rememberAppWindowSizeInfo(): AppWindowSizeInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp

    val widthClass = if (widthDp >= 600.dp) {
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
