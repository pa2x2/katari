package mihon.presentation.kit.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.window.WindowFold
import mihon.presentation.kit.window.currentWindowLayout

/**
 * The two pages of an open book side by side, for paged reading in a window that shows two panes
 * ([mihon.presentation.kit.window.WindowLayout.usesTwoPanes]). Where a vertical fold crosses the box the pages split on
 * it, so no line bends across it; otherwise each takes half. [content] gets the [PageSpreadScope.pageWidth] both pages
 * share, to measure pages before they are drawn, and places them with [PageSpreadScope.Pages]. Every box that draws a
 * part of the same spread (the pages, the numbers under them) wraps itself in its own [PageSpread], so each finds the
 * fold within itself.
 */
@Composable
fun PageSpread(modifier: Modifier = Modifier, content: @Composable PageSpreadScope.() -> Unit) {
    val density = LocalDensity.current
    val fold = currentWindowLayout().fold?.takeIf { it.isVertical }
    // Where the box starts in the window, to find the fold within it
    var left by remember { mutableStateOf<Dp?>(null) }
    BoxWithConstraints(
        modifier = modifier.onPlaced { left = with(density) { it.positionInWindow().x.toDp() } },
        propagateMinConstraints = true,
    ) {
        val split = remember(maxWidth, left, fold) { pageSpreadSplit(maxWidth, left, fold) }
        PageSpreadScopeImpl(split).content()
    }
}

/** What a [PageSpread]'s content can read and place. */
interface PageSpreadScope {
    /** The width of each page: the narrower side's, so text measured once fits on either. */
    val pageWidth: Dp

    /**
     * Lays [left] and [right] out as the two pages, each [pageWidth] wide and centred on its side of the split, in a box
     * as large as the [PageSpread]. The sides are physical because the order of a book's pages follows its reading
     * direction, not the layout's.
     */
    @Composable
    fun Pages(
        left: @Composable BoxScope.() -> Unit,
        right: @Composable BoxScope.() -> Unit,
        modifier: Modifier = Modifier,
    )
}

/** Where the spread splits: the width left of the split and the width right of it, which a hinge may leave apart. */
@Immutable
private data class PageSpreadSplit(val leftWidth: Dp, val rightStart: Dp, val rightWidth: Dp)

private fun pageSpreadSplit(width: Dp, left: Dp?, fold: WindowFold?): PageSpreadSplit {
    val foldStart = left?.let { fold?.start?.minus(it) }
    val foldEnd = left?.let { fold?.end?.minus(it) }
    if (foldStart != null && foldEnd != null && foldStart >= MinPageWidth && width - foldEnd >= MinPageWidth) {
        return PageSpreadSplit(leftWidth = foldStart, rightStart = foldEnd, rightWidth = width - foldEnd)
    }
    val half = width / 2
    return PageSpreadSplit(leftWidth = half, rightStart = half, rightWidth = width - half)
}

private class PageSpreadScopeImpl(private val split: PageSpreadSplit) : PageSpreadScope {
    override val pageWidth: Dp = minOf(split.leftWidth, split.rightWidth).coerceAtLeast(0.dp)

    @Composable
    override fun Pages(
        left: @Composable BoxScope.() -> Unit,
        right: @Composable BoxScope.() -> Unit,
        modifier: Modifier,
    ) {
        Box(modifier, contentAlignment = AbsoluteAlignment.TopLeft) {
            Box(Modifier.width(split.leftWidth), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.width(pageWidth), content = left)
            }
            Box(
                Modifier.absoluteOffset(x = split.rightStart).width(split.rightWidth),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(Modifier.width(pageWidth), content = right)
            }
        }
    }
}

// A fold this close to an edge of the spread is outside it in practice, such as a fold beside a narrow window
private val MinPageWidth = 240.dp
