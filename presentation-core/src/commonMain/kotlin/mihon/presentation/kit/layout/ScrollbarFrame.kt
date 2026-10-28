package mihon.presentation.kit.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import mihon.presentation.kit.pointer.LocalWindowHasMouse

/**
 * [content], a lazy grid scrolled by [state], with the scrollbar of a window worked with a mouse at its end edge,
 * kept inside [contentPadding] so it doesn't run under the bars the grid scrolls beneath. A touch window gets [content]
 * as it is.
 */
@Composable
fun ScrollbarFrame(
    state: LazyGridState,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit,
) {
    ScrollbarOverlay(contentPadding, scrollbar = { GridScrollbar(state, it) }, content = content)
}

/** [content], a lazy list scrolled by [state], with a scrollbar as for a grid. */
@Composable
fun ScrollbarFrame(
    state: LazyListState,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit,
) {
    ScrollbarOverlay(contentPadding, scrollbar = { ListScrollbar(state, it) }, content = content)
}

@Composable
private fun ScrollbarOverlay(
    contentPadding: PaddingValues,
    scrollbar: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    if (!LocalWindowHasMouse.current) {
        content()
        return
    }
    val end = contentPadding.calculateEndPadding(LocalLayoutDirection.current)
    Box(propagateMinConstraints = true) {
        content()
        Box(modifier = Modifier.matchParentSize()) {
            scrollbar(
                Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .padding(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding(),
                        end = end,
                    ),
            )
        }
    }
}

/**
 * [content], a row scrolled sideways by [state], with a scrollbar along its bottom edge in a window worked with a
 * mouse, where the vertical wheel scrolls the row too (see [verticalWheelScrollsRow]). [modifier] places the row in its
 * parent and [content] is handed what it must apply to itself, as the scrollbar needs a frame around the row. A touch
 * window gets [content] with [modifier] as it is.
 */
@Composable
internal fun HorizontalScrollbarFrame(
    state: LazyListState,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    HorizontalScrollbarOverlay(state, modifier, scrollbar = { RowScrollbar(state, it) }, content = content)
}

/** [content], a row scrolled sideways by [state], with a scrollbar as for a lazy row. */
@Composable
internal fun HorizontalScrollbarFrame(
    state: ScrollState,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    HorizontalScrollbarOverlay(state, modifier, scrollbar = { RowScrollbar(state, it) }, content = content)
}

@Composable
private fun HorizontalScrollbarOverlay(
    state: ScrollableState,
    modifier: Modifier,
    scrollbar: @Composable (Modifier) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    if (!LocalWindowHasMouse.current) {
        content(modifier)
        return
    }
    Box(modifier = modifier.verticalWheelScrollsRow(state), propagateMinConstraints = true) {
        content(Modifier)
        Box(modifier = Modifier.matchParentSize()) {
            scrollbar(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            )
        }
    }
}

/** A desktop scrollbar: always shown while there is more to scroll to, dragged or clicked to scroll. */
@Composable
internal expect fun ListScrollbar(state: LazyListState, modifier: Modifier)

@Composable
internal expect fun GridScrollbar(state: LazyGridState, modifier: Modifier)

@Composable
internal expect fun RowScrollbar(state: LazyListState, modifier: Modifier)

@Composable
internal expect fun RowScrollbar(state: ScrollState, modifier: Modifier)
