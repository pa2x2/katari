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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.VerticalFastScroller

/**
 * A scrolling list of rows, such as row group cards, history or sources. However wide the window, the rows stay in a
 * column at most [RowListMaxWidth] wide, centred, so a row's buttons stay near the name they act on; the list still
 * scrolls from anywhere across its width. [fastScroll] adds the draggable thumb long lists use, kept at the list's
 * edge rather than the column's.
 */
@Composable
fun RowList(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fastScroll: Boolean = false,
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val start = contentPadding.calculateStartPadding(direction)
    val end = contentPadding.calculateEndPadding(direction)
    BoxWithConstraints(modifier = modifier) {
        val margin = ((maxWidth - start - end - RowListMaxWidth) / 2).coerceAtLeast(0.dp)
        val centredPadding = PaddingValues(
            start = start + margin,
            top = contentPadding.calculateTopPadding(),
            end = end + margin,
            bottom = contentPadding.calculateBottomPadding(),
        )
        val list = @Composable {
            LazyColumn(
                state = state,
                contentPadding = centredPadding,
                reverseLayout = reverseLayout,
                verticalArrangement = verticalArrangement,
                userScrollEnabled = userScrollEnabled,
                content = content,
            )
        }
        if (fastScroll) {
            VerticalFastScroller(
                listState = state,
                topContentPadding = contentPadding.calculateTopPadding(),
                endContentPadding = end,
                content = list,
            )
        } else {
            list()
        }
    }
}

val RowListMaxWidth = 640.dp
