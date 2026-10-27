package mihon.presentation.kit.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.util.drawVerticalScrollbar

/**
 * A scrolling list of rows, such as row group cards, history or sources. However wide the window, the rows stay in a
 * column at most [RowListMaxWidth] wide, centred, so a row's buttons stay near the name they act on; the list still
 * scrolls from anywhere across its width. Every list shows a scroll indicator at its edge while it scrolls;
 * [fastScroll] makes that a draggable thumb for long lists, starting [fastScrollTopPadding] down, which a list whose
 * first row runs under a transparent app bar sets to the bar's height.
 */
@Composable
fun RowList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fastScroll: Boolean = false,
    fastScrollTopPadding: Dp = contentPadding.calculateTopPadding(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: RowListScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val end = contentPadding.calculateEndPadding(direction)
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        val padding = contentPadding.centredInRowColumn(maxWidth, direction)
        val columnInset = PaddingValues(
            start = padding.calculateStartPadding(direction),
            end = padding.calculateEndPadding(direction),
        )
        val list = @Composable { listModifier: Modifier ->
            LazyColumn(
                modifier = listModifier,
                state = state,
                contentPadding = padding,
                verticalArrangement = verticalArrangement,
            ) {
                RowListScopeImpl(this, columnInset).content()
            }
        }
        if (fastScroll) {
            VerticalFastScroller(
                listState = state,
                topContentPadding = fastScrollTopPadding,
                endContentPadding = end,
            ) {
                list(Modifier)
            }
        } else {
            val endPx = with(LocalDensity.current) { end.toPx() }
            list(Modifier.drawVerticalScrollbar(state = state, positionOffsetPx = endPx))
        }
    }
}

/** The rows of a [RowList]. */
interface RowListScope : LazyListScope {
    /**
     * How far the rows sit in from the list's sides, its content padding included, so artwork behind the first rows,
     * such as an `ArtworkBackdrop`, can bleed out to the list's edges.
     */
    val columnInset: PaddingValues
}

private class RowListScopeImpl(
    scope: LazyListScope,
    override val columnInset: PaddingValues,
) : RowListScope, LazyListScope by scope

/** [this] padding widened at the sides so the content between them is at most [RowListMaxWidth] across [width]. */
internal fun PaddingValues.centredInRowColumn(width: Dp, direction: LayoutDirection): PaddingValues {
    val start = calculateStartPadding(direction)
    val end = calculateEndPadding(direction)
    val margin = ((width - start - end - RowListMaxWidth) / 2).coerceAtLeast(0.dp)
    return PaddingValues(
        start = start + margin,
        top = calculateTopPadding(),
        end = end + margin,
        bottom = calculateBottomPadding(),
    )
}

val RowListMaxWidth = 640.dp
