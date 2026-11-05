package mihon.presentation.kit.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import mihon.presentation.kit.navigation.LocalDockClearance
import mihon.presentation.kit.window.currentWindowLayout

/**
 * Two panes side by side, for a window wide enough for two panes. Where a vertical fold crosses them they split on it,
 * so neither pane bends. Otherwise the start pane takes half the room, up to a width that grows with the window
 * ([startPaneWidth]), and the end pane, a column of rows such as chapters or a settings page, takes the rest up to a
 * [RowList]'s width. Panes that don't fill the room sit centred in it, as the `ScreenFrame.ListDetail` frame does.
 *
 * Where the panes reach the window's edges, [contentPadding]'s start and the side dock are inside the start pane and
 * its end inside the end pane, and each pane gets the padding of its own side to keep clear of, so a backdrop in the
 * start pane can run under the dock. Panes centred in a wider window, or in a screen frame narrower than the window,
 * start where the content does and get no padding: the margins beside them stay empty.
 */
@Composable
fun TwoPanes(
    start: @Composable BoxScope.(padding: PaddingValues) -> Unit,
    end: @Composable BoxScope.(padding: PaddingValues) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val window = currentWindowLayout()
    val dockClearance = LocalDockClearance.current
    val contentStart = contentPadding.calculateStartPadding(direction)
    val startPadding = contentStart + dockClearance.start
    val endPadding = contentPadding.calculateEndPadding(direction)
    val fold = window.fold?.takeIf { it.isVertical }
    val frameMargin = LocalContentFrameMargin.current
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
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = panes.margin + startPadding - startInside)
                    .width(panes.start - startPadding + startInside)
                    .consumeWindowInsets(PaneInsets.only(WindowInsetsSides.End)),
            ) {
                // The dock's room isn't part of the padding: the pane keeps clear of the dock itself
                start(PaddingValues(start = if (atEdges) contentStart else 0.dp))
            }
        }
        CompositionLocalProvider(LocalDockClearance provides dockClearance.copy(start = 0.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = panes.margin + endPadding - endInside)
                    .width(panes.end - endPadding + endInside)
                    .consumeWindowInsets(PaneInsets.only(WindowInsetsSides.Start)),
            ) {
                end(PaddingValues(end = endInside))
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
