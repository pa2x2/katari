package app.katari.presentation.kit.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.navigation.LocalDockClearance
import app.katari.presentation.kit.window.currentWindowLayout

/**
 * Two panes side by side, for a window wide enough for two panes. Where a vertical fold crosses them they split on it,
 * so neither pane bends. Otherwise the start pane takes half the room, up to a width that grows with the window
 * ([startPaneWidth]), and the end pane, a column of rows such as chapters or a settings page, takes the rest up to a
 * [RowList]'s width. Panes that don't fill the room sit centred in it, as the `ScreenFrame.ListDetail` frame does.
 *
 * Where the panes reach the window's edges, the screen's side room and the side dock are inside the start pane and
 * the room on the end inside the end pane, and each pane keeps clear of its own side's room the way a screen does:
 * the kit's layouts in it apply the room, so a backdrop in the start pane can run under the dock. Panes centred in a
 * wider window, or in a screen frame narrower than the window, start where the content does and have no room to keep
 * clear of: the margins beside them stay empty.
 */
@Composable
fun TwoPanes(
    start: @Composable BoxScope.() -> Unit,
    end: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val window = currentWindowLayout()
    val dockClearance = LocalDockClearance.current
    val sides = LocalContentSides.current
    val startPadding = sides.start + dockClearance.start
    val endPadding = sides.end
    val fold = window.fold?.takeIf { it.isVertical }
    val frameMargin = sides.frameMargin
    // Where the panes start in the window, to find the fold within them
    var left by remember { mutableStateOf<Dp?>(null) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { left = with(density) { it.positionInWindow().x.toDp() } },
    ) {
        val leftOfFold = left?.let { fold?.start?.minus(it) }
        val rightOfFold = left?.let { fold?.end?.minus(it) }?.let { maxWidth - it }
        val panes = if (
            leftOfFold != null && rightOfFold != null && leftOfFold >= MinPaneWidth && rightOfFold >= MinPaneWidth
        ) {
            val (startWidth, endWidth) =
                if (direction == LayoutDirection.Ltr) leftOfFold to rightOfFold else rightOfFold to leftOfFold
            PaneWidths(margin = 0.dp, start = startWidth, end = endWidth)
        } else {
            val room = maxWidth - startPadding - endPadding
            val startWidth = startPaneWidth(window.width, room)
            val endWidth = (room - startWidth).coerceAtMost(RowListMaxWidth)
            PaneWidths(
                margin = ((room - startWidth - endWidth) / 2).coerceAtLeast(0.dp),
                start = startWidth + startPadding,
                end = endWidth + endPadding,
            )
        }
        val atEdges = panes.margin == 0.dp && frameMargin == 0.dp
        val startInside = if (atEdges) startPadding else 0.dp
        val endInside = if (atEdges) endPadding else 0.dp
        // Each pane keeps clear of the insets on its own side only, such as a camera cutout beside the start pane
        CompositionLocalProvider(
            LocalDockClearance provides if (atEdges) dockClearance else dockClearance.copy(start = 0.dp),
            LocalContentSides provides if (atEdges) {
                ContentSides(start = sides.start, insetStart = sides.insetStart)
            } else {
                ContentSides.None
            },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = panes.margin + startPadding - startInside)
                    .width(panes.start - startPadding + startInside)
                    .consumeWindowInsets(PaneInsets.only(WindowInsetsSides.End)),
            ) {
                start()
            }
        }
        CompositionLocalProvider(
            LocalDockClearance provides dockClearance.copy(start = 0.dp),
            LocalContentSides provides if (atEdges) {
                ContentSides(end = sides.end, insetEnd = sides.insetEnd)
            } else {
                ContentSides.None
            },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = panes.margin + endPadding - endInside)
                    .width(panes.end - endPadding + endInside)
                    .consumeWindowInsets(PaneInsets.only(WindowInsetsSides.Start)),
            ) {
                end()
            }
        }
    }
}

/** Each pane's width, its side's padding included, and the room left beside both on each side. */
private data class PaneWidths(val margin: Dp, val start: Dp, val end: Dp)

private val PaneInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)

// A fold this close to an edge of the panes is outside them in practice, such as a fold beside a narrow window
private val MinPaneWidth = 240.dp
