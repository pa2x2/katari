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
import kotlin.math.ceil
import kotlin.math.min

/**
 * Cards flowing down the screen in columns, such as global search's source cards or the statistics dashboard. A card
 * stays between [CardMinWidth] and [CardMaxWidth] wide, so a wider window adds columns rather than stretching cards; a
 * window too narrow for two columns has one card across its width. A card goes into the shortest column, so cards of
 * different heights leave no gaps; one that needs the width, such as a wide chart, spans every column with
 * `StaggeredGridItemSpan.FullLine`. The flow adds no space of its own: row group and dashboard cards already keep
 * their side margin and the gap below them, so one column looks as it does in a [RowList].
 *
 * [maxColumns] caps the columns for content whose sections are too short to fill them; the cards then widen to share
 * the width, past [CardMaxWidth].
 */
@Composable
fun CardFlow(
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    maxColumns: Int? = null,
    content: LazyStaggeredGridScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth - contentPadding.calculateStartPadding(direction) -
            contentPadding.calculateEndPadding(direction)
        val fitted = min((width / CardMinWidth).toInt(), ceil(width / CardMaxWidth).toInt())
        val columns = min(fitted, maxColumns ?: fitted).coerceAtLeast(1)
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = state,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

private val CardMinWidth = 320.dp
private val CardMaxWidth = 480.dp
