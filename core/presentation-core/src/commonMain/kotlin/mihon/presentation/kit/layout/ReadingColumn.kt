package mihon.presentation.kit.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A scrolling column of running text, such as a book read in continuous scrolling. However wide the window, the text
 * stays in a column at most [maxWidth] wide, centred (see `ReadingMetrics.columnWidth`), while the list spans the
 * window so a drag or tap beside the text still reaches it. It shows no scroll indicator: readers show their own
 * progress.
 */
@Composable
fun ReadingColumn(
    state: LazyListState,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        val side = ((this.maxWidth - maxWidth) / 2).coerceAtLeast(0.dp)
        LazyColumn(state = state, contentPadding = PaddingValues(horizontal = side), content = content)
    }
}
