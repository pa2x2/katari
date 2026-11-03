package mihon.presentation.kit.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The spacing scale. Every padding, gap and spacer outside the kit is one of these steps. */
object KatariSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    /** Side inset of list rows, section headers and sheet content. */
    val rowInset: Dp = xl

    /** Side inset of grids, cards and edge-to-edge lists. */
    val contentInset: Dp = l
}
