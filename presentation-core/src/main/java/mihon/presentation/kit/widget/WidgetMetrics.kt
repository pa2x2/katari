package mihon.presentation.kit.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariSpacing

/**
 * Geometry of the home-screen widgets. Glance lays them out outside the app's Compose theme, so they read these plain
 * values instead of KatariTheme; the grid's row and column count is derived from the same values.
 */
object WidgetMetrics {
    /** Size of an entry's cover in the updates grid. */
    val coverWidth: Dp = 58.dp
    val coverHeight: Dp = 87.dp

    /** Gap on each side of a cover across a row of the grid. */
    val coverGap: Dp = 3.dp

    /** Gap above and below each row of the grid. */
    val rowGap: Dp = KatariSpacing.xs

    /** Inset of a message, such as the locked notice, from the widget's edges. */
    val messageInset: Dp = KatariSpacing.s

    /** Space the cover-screen widget keeps free at its bottom, over the device's own cover-screen controls. */
    val coverScreenBottomInset: Dp = KatariSpacing.xl
}
