package app.katari.presentation.kit.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.katari.presentation.kit.pointer.LocalWindowHasMouse
import app.katari.presentation.kit.sheet.LocalSheetColumns
import app.katari.presentation.kit.surface.background
import app.katari.presentation.kit.surface.currentSurfaceContainer
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.util.drawVerticalScrollbar

/**
 * A scrolling list of rows, such as row group cards, history or sources. However wide the window, the rows stay in a
 * column at most [RowListMaxWidth] wide, so a row's buttons stay near the name they act on; the list still scrolls
 * from anywhere across its width. A screen of rows gets a frame that wide (`ScreenFrame.Rows`); in a wider frame the
 * column keeps to the frame's start edge, under the title. The list keeps clear of the screen's side room itself, so
 * [contentPadding] only adds to it, such as the room under the app bar. With [wrapGroups], a list of row groups whose frame holds two
 * columns of rows fills its width instead: the rows `rowGroupItems` adds wrap into lines across each group's card, while
 * group headers and anything else span the width; in a sheet with two columns of rows its groups always wrap into
 * both. Every list shows a scroll indicator at its edge while it scrolls;
 * [fastScroll] makes that a draggable thumb for long lists, starting [fastScrollTopPadding] down, which a list whose
 * first row runs under a transparent app bar sets to the bar's height. In a window worked with a mouse, every list
 * shows a scrollbar from [fastScrollTopPadding] down instead.
 */
@Composable
fun RowList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fastScroll: Boolean = false,
    fastScrollTopPadding: Dp = contentPadding.calculateTopPadding(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    wrapGroups: Boolean = false,
    content: RowListScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val framedPadding = contentPadding.plusSides(LocalContentSides.current, direction)
    val end = framedPadding.calculateEndPadding(direction)
    val sheetColumns = LocalSheetColumns.current
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        val groupColumns = groupColumns(
            width = maxWidth - framedPadding.calculateStartPadding(direction) - end,
            wrapGroups = wrapGroups,
            sheetColumns = sheetColumns,
        )
        val padding = if (groupColumns > 1) framedPadding else framedPadding.inRowColumn(maxWidth, direction)
        val columnInset = PaddingValues(
            start = padding.calculateStartPadding(direction),
            end = padding.calculateEndPadding(direction),
        )
        val list = @Composable { listModifier: Modifier ->
            SidesApplied {
                LazyColumn(
                    modifier = listModifier,
                    state = state,
                    contentPadding = padding,
                    verticalArrangement = verticalArrangement,
                ) {
                    RowListScopeImpl(this, columnInset, groupColumns).content()
                }
            }
        }
        if (LocalWindowHasMouse.current) {
            ScrollbarFrame(
                state = state,
                contentPadding = PaddingValues(
                    top = fastScrollTopPadding,
                    bottom = framedPadding.calculateBottomPadding(),
                    end = end,
                ),
            ) {
                list(Modifier)
            }
        } else if (fastScroll) {
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

/**
 * What sits above a [RowList] and stays put while it scrolls, such as a search field over its rows. It keeps to the
 * rows' column and the side room the list keeps clear of, so it lines up with them however the list places them;
 * [wrapGroups] is the list's.
 */
@Composable
fun RowListHeader(modifier: Modifier = Modifier, wrapGroups: Boolean = false, content: @Composable () -> Unit) {
    val direction = LocalLayoutDirection.current
    val sheetColumns = LocalSheetColumns.current
    val sides = LocalContentSides.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val padding = if (groupColumns(maxWidth - sides.start - sides.end, wrapGroups, sheetColumns) > 1) {
            sides.padding
        } else {
            sides.padding.inRowColumn(maxWidth, direction)
        }
        Box(Modifier.padding(padding)) { SidesApplied(content) }
    }
}

private fun groupColumns(width: Dp, wrapGroups: Boolean, sheetColumns: Int): Int = when {
    sheetColumns > 1 -> sheetColumns
    wrapGroups -> (width / WrappedRowMinWidth).toInt().coerceAtLeast(1)
    else -> 1
}

/** The rows of a [RowList]. */
interface RowListScope : LazyListScope {
    /**
     * How far the rows sit in from the list's sides, its content padding and the screen's side room included, so artwork
     * behind the first rows,
     * such as an `ArtworkBackdrop`, can bleed out to the list's edges.
     */
    val columnInset: PaddingValues

    /** How many columns the rows of a row group wrap into: 1 unless the list wraps groups and has room for more. */
    val groupColumns: Int

    /**
     * A header, typically a `RowGroupHeader`, that stays at the top of the list while the rows under it scroll by. It is
     * filled like the surface the list sits on, so the rows don't show through it.
     */
    fun stickyRowGroupHeader(key: Any? = null, header: @Composable () -> Unit) {
        stickyHeader(key = key) {
            Box(Modifier.fillMaxWidth().background(currentSurfaceContainer())) { header() }
        }
    }
}

private class RowListScopeImpl(
    scope: LazyListScope,
    override val columnInset: PaddingValues,
    override val groupColumns: Int,
) : RowListScope, LazyListScope by scope

/** [this] padding widened at the end so the content between the sides is at most [RowListMaxWidth] across [width]. */
internal fun PaddingValues.inRowColumn(width: Dp, direction: LayoutDirection): PaddingValues {
    val start = calculateStartPadding(direction)
    val end = calculateEndPadding(direction)
    val rest = (width - start - end - RowListMaxWidth).coerceAtLeast(0.dp)
    return PaddingValues(
        start = start,
        top = calculateTopPadding(),
        end = end + rest,
        bottom = calculateBottomPadding(),
    )
}

val RowListMaxWidth = 640.dp

// About a phone's width: rows are laid out for it, so a column of wrapped rows is never narrower
private val WrappedRowMinWidth = 400.dp
