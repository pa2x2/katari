package mihon.presentation.kit.surface

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Plain lines stay inside the kit: rows of a list are split by their row group and sections by the gap between cards,
// so a screen only draws a line through the component named for it.
@Composable
internal fun HorizontalDivider(modifier: Modifier = Modifier) {
    androidx.compose.material3.HorizontalDivider(modifier)
}

@Composable
internal fun VerticalDivider(modifier: Modifier = Modifier) {
    androidx.compose.material3.VerticalDivider(modifier)
}

/**
 * The line between parts of one surface: the two sides of a translation, the cells of a details grid, one version's
 * notes and the next, or a footer's actions under a sheet's content. [orientation] is the direction the line runs.
 */
@Composable
fun PaneDivider(modifier: Modifier = Modifier, orientation: Orientation = Orientation.Horizontal) {
    when (orientation) {
        Orientation.Horizontal -> HorizontalDivider(modifier)
        Orientation.Vertical -> VerticalDivider(modifier)
    }
}
