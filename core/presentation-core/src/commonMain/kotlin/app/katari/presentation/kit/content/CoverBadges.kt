package app.katari.presentation.kit.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import app.katari.presentation.kit.theme.KatariTheme

/**
 * Badges along the top of a cover, such as a library cover's unread count at the start and its entry type at the
 * end. The two sides share the width: when they don't fit side by side, the end badges move to a line below the start
 * ones and keep to the end edge, and a side too wide for the cover on its own wraps its badges.
 */
@Composable
fun CoverBadges(
    modifier: Modifier = Modifier,
    start: @Composable RowScope.() -> Unit = {},
    end: @Composable RowScope.() -> Unit = {},
) {
    val gap = KatariTheme.spacing.xs
    Layout(
        contents = listOf(
            {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) { start() }
            },
            {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) { end() }
            },
        ),
        modifier = modifier,
    ) { (startMeasurables, endMeasurables), constraints ->
        val sideConstraints = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val startSide = startMeasurables.single().measure(sideConstraints)
        val endSide = endMeasurables.single().measure(sideConstraints)
        val gapPx = gap.roundToPx()
        val sideBySideWidth = startSide.width + gapPx + endSide.width
        val sideBySide = startSide.width == 0 || endSide.width == 0 || sideBySideWidth <= constraints.maxWidth
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else sideBySideWidth
        val endY = if (sideBySide) 0 else startSide.height + gapPx
        val height = maxOf(startSide.height, endY + endSide.height)
        layout(width, constraints.constrainHeight(height)) {
            startSide.place(0, 0)
            endSide.place(width - endSide.width, endY)
        }
    }
}
