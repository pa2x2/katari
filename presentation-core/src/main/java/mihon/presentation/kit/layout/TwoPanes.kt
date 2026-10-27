package mihon.presentation.kit.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.navigation.LocalDockClearance
import mihon.presentation.kit.window.currentWindowLayout

/**
 * Two panes side by side, for a window wide enough for two panes. Where a vertical fold crosses them they split on it,
 * so neither pane bends; otherwise the start pane takes half the width, at most [StartPaneMaxWidth]. The side dock
 * floats at the start edge, so only the start pane keeps clear of it.
 */
@Composable
fun TwoPanes(
    start: @Composable BoxScope.() -> Unit,
    end: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets(0),
) {
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val padding = contentWindowInsets.asPaddingValues()
    val startPadding = padding.calculateStartPadding(direction)
    val endPadding = padding.calculateEndPadding(direction)
    val fold = currentWindowLayout().fold?.takeIf { it.isVertical }
    // Where the panes start in the window, to find the fold within them
    var left by remember { mutableStateOf<Dp?>(null) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { left = with(density) { it.positionInWindow().x.toDp() } },
    ) {
        val leftOfFold = left?.let { fold?.start?.minus(it) }
        val rightOfFold = left?.let { fold?.end?.minus(it) }?.let { maxWidth - it }
        val (startWidth, endWidth) = if (
            leftOfFold != null && rightOfFold != null && leftOfFold >= MinPaneWidth && rightOfFold >= MinPaneWidth
        ) {
            if (direction == LayoutDirection.Ltr) leftOfFold to rightOfFold else rightOfFold to leftOfFold
        } else {
            val width = maxWidth - startPadding - endPadding
            val firstWidth = (width / 2).coerceAtMost(StartPaneMaxWidth)
            firstWidth + startPadding to width - firstWidth + endPadding
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(startWidth),
            content = start,
        )
        CompositionLocalProvider(LocalDockClearance provides LocalDockClearance.current.copy(start = 0.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(endWidth),
                content = end,
            )
        }
    }
}

private val StartPaneMaxWidth = 450.dp

// A fold this close to an edge of the panes is outside them in practice, such as a fold beside a narrow window
private val MinPaneWidth = 240.dp
