package mihon.presentation.kit.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Cards flowing down the screen in one column, or in two once the width holds two cards of [CardFlowMinCardWidth],
 * such as global search's source cards or the statistics dashboard. A card goes into the shorter column, so cards of
 * different heights leave no gaps; one that needs the width, such as a wide chart, spans both columns with
 * `StaggeredGridItemSpan.FullLine`. The flow adds no space of its own: row group and dashboard cards already keep
 * their side margin and the gap below them, so one column looks as it does in a [RowList].
 */
@Composable
fun CardFlow(
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyStaggeredGridScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth - contentPadding.calculateStartPadding(direction) -
            contentPadding.calculateEndPadding(direction)
        val columns = if (width >= CardFlowMinCardWidth * 2) 2 else 1
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = state,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

private val CardFlowMinCardWidth = 320.dp
