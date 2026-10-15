package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import mihon.presentation.kit.theme.KatariTheme

internal enum class DropIndicatorEdge {
    Start,
    End,
    Top,
    Bottom,
}

/** Draws a line on [edge] of a destination to show where a dragged destination would be inserted. */
internal fun Modifier.dropInsertionIndicator(
    visible: Boolean,
    edge: DropIndicatorEdge,
    color: Color,
): Modifier {
    if (!visible) return this

    return drawWithContent {
        drawContent()
        val strokeWidth = KatariTheme.sizes.selectedBorder.toPx()
        val inset = KatariTheme.spacing.s.toPx()
        when (edge) {
            DropIndicatorEdge.Start,
            DropIndicatorEdge.End,
            -> {
                val x = if (edge == DropIndicatorEdge.Start) strokeWidth / 2 else size.width - strokeWidth / 2
                drawLine(
                    color = color,
                    start = Offset(x, inset),
                    end = Offset(x, size.height - inset),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
            DropIndicatorEdge.Top,
            DropIndicatorEdge.Bottom,
            -> {
                val y = if (edge == DropIndicatorEdge.Top) strokeWidth / 2 else size.height - strokeWidth / 2
                drawLine(
                    color = color,
                    start = Offset(inset, y),
                    end = Offset(size.width - inset, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
