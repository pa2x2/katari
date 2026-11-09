package app.katari.presentation.kit.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.v2.maxScrollOffset
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlin.math.abs

/**
 * Scrollbar position of a lazy grid whose items may span several slots, such as the library's full-width Pinned shelf.
 *
 * Compose's own grid adapter counts lines as items divided by slots per line, so every item spanning a whole line hides
 * one line from the content size and the thumb runs past the end of its track. This one takes the lines up to the last
 * laid-out item from where the grid placed them, and assumes one slot per item only for items not laid out.
 */
internal class GridScrollbarAdapter(private val state: LazyGridState) : ScrollbarAdapter {

    private class Lines(
        val first: LazyGridItemInfo,
        val firstLine: Int,
        val last: LazyGridItemInfo,
        val lastLine: Int,
        val count: Int,
        val averageSize: Double,
    )

    private val vertical get() = state.layoutInfo.orientation == Orientation.Vertical

    // An item animating out has no line
    private val LazyGridItemInfo.line get() = if (vertical) row else column
    private val LazyGridItemInfo.mainAxisOffset get() = if (vertical) offset.y else offset.x
    private val LazyGridItemInfo.mainAxisSize get() = if (vertical) size.height else size.width

    private val lines by derivedStateOf { measureLines() }

    private val spacing get() = state.layoutInfo.mainAxisItemSpacing

    override val viewportSize: Double
        get() = with(state.layoutInfo) { if (vertical) viewportSize.height else viewportSize.width }.toDouble()

    override val scrollOffset: Double
        get() {
            val lines = lines ?: return 0.0
            return lines.firstLine * (lines.averageSize + spacing) - lines.first.mainAxisOffset
        }

    override val contentSize: Double
        get() {
            val lines = lines ?: return 0.0
            val layoutInfo = state.layoutInfo
            return lines.averageSize * lines.count + spacing * (lines.count - 1) +
                layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding
        }

    override suspend fun scrollTo(scrollOffset: Double) {
        val distance = scrollOffset - this.scrollOffset
        // Within a viewport a scroll keeps item sizes exact; further, measuring every item on the way would stall
        if (abs(distance) <= viewportSize) {
            state.scrollBy(distance.toFloat())
            return
        }
        val lines = lines ?: return
        val target = scrollOffset.coerceIn(0.0, maxScrollOffset)
        val lineSize = lines.averageSize + spacing
        val line = (target / lineSize).toInt().coerceIn(0, lines.count - 1)
        val offset = (target - line * lineSize).toInt().coerceAtLeast(0)
        state.scrollToItem(firstItemOf(line, lines), offset)
    }

    private fun measureLines(): Lines? {
        val layoutInfo = state.layoutInfo
        val placed = layoutInfo.visibleItemsInfo.filter { it.line >= 0 }
        if (placed.isEmpty()) return null
        val first = placed.minBy { it.index }
        val last = placed.maxBy { it.index }
        val firstLine = first.line
        val lastLine = last.line
        val lastLineEnd = placed.filter { it.line == lastLine }.maxOf { it.mainAxisOffset + it.mainAxisSize }
        val placedLines = lastLine - firstLine + 1
        val averageSize =
            (lastLineEnd - first.mainAxisOffset - (placedLines - 1) * spacing).toDouble() / placedLines
        val itemsAfter = layoutInfo.totalItemsCount - 1 - last.index
        val linesAfter = (itemsAfter + layoutInfo.maxSpan - 1) / layoutInfo.maxSpan
        return Lines(first, firstLine, last, lastLine, lastLine + 1 + linesAfter, averageSize)
    }

    private fun firstItemOf(line: Int, lines: Lines): Int {
        val slots = state.layoutInfo.maxSpan
        return when {
            line < lines.firstLine -> (lines.first.index - (lines.firstLine - line) * slots).coerceAtLeast(0)
            line > lines.lastLine ->
                (lines.last.index + 1 + (line - lines.lastLine - 1) * slots)
                    .coerceAtMost(state.layoutInfo.totalItemsCount - 1)
            else -> state.layoutInfo.visibleItemsInfo.filter { it.line == line }.minOf { it.index }
        }
    }
}
