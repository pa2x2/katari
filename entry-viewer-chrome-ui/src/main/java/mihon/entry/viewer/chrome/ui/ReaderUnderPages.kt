package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import mihon.presentation.kit.layout.PageSpread
import mihon.presentation.kit.window.currentWindowLayout
import kotlin.math.roundToInt

/**
 * Lays out what sits under the pages, such as page numbers, so it lines up with them. Under a two-page spread
 * [content] goes under the first page and [facing] under the second, the first being the right one when the pages run
 * [rightToLeft]. With one page, half-open like a book, [content] sits under the end half rather than on the crease;
 * otherwise it is centred. [modifier] should span the pages' width, so the split falls where theirs does.
 *
 * Pages that are narrower than their halves and meet at the split, as manga pages do, give [pageMiddles]: the middle
 * of the left and of the right page, as fractions of the width, so each number sits under its own page.
 */
@Composable
fun ReaderUnderPages(
    modifier: Modifier = Modifier,
    facing: (@Composable BoxScope.() -> Unit)? = null,
    rightToLeft: Boolean = false,
    pageMiddles: Pair<Float, Float>? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    when {
        facing != null && pageMiddles != null -> Box(modifier) {
            CentredAt(pageMiddles.first, if (rightToLeft) facing else content)
            CentredAt(pageMiddles.second, if (rightToLeft) content else facing)
        }
        facing != null -> PageSpread(modifier) {
            Pages(
                left = if (rightToLeft) facing else content,
                right = if (rightToLeft) content else facing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        currentWindowLayout().fold?.let { it.isVertical && it.isHalfOpen } == true -> PageSpread(modifier) {
            val rtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
            Pages(
                left = if (rtlLayout) content else ({}),
                right = if (rtlLayout) ({}) else content,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        else -> Box(modifier, contentAlignment = Alignment.Center, content = content)
    }
}

/** [content] with its middle at [fraction] of the width. */
@Composable
private fun CentredAt(fraction: Float, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0))
                layout(constraints.maxWidth, placeable.height) {
                    placeable.place((constraints.maxWidth * fraction - placeable.width / 2f).roundToInt(), 0)
                }
            },
        content = content,
    )
}
