package mihon.presentation.kit.layout

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

@Composable
internal actual fun ListScrollbar(state: LazyListState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier.padding(KatariTheme.spacing.xxs),
        style = scrollbarStyle(),
    )
}

@Composable
internal actual fun GridScrollbar(state: LazyGridState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier.padding(KatariTheme.spacing.xxs),
        style = scrollbarStyle(),
    )
}

@Composable
internal actual fun RowScrollbar(state: LazyListState, modifier: Modifier) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier.padding(KatariTheme.spacing.xxs),
        style = scrollbarStyle(),
    )
}

@Composable
internal actual fun RowScrollbar(state: ScrollState, modifier: Modifier) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier.padding(KatariTheme.spacing.xxs),
        style = scrollbarStyle(),
    )
}

@Composable
private fun scrollbarStyle() = ScrollbarStyle(
    minimalHeight = ThumbMinLength,
    thickness = ThumbThickness,
    shape = KatariTheme.shapes.full,
    hoverDurationMillis = HOVER_DURATION_MILLIS,
    unhoverColor = KatariTheme.colors.outlineVariant,
    hoverColor = KatariTheme.colors.outline,
)

private val ThumbThickness = 8.dp
private val ThumbMinLength = 32.dp
private const val HOVER_DURATION_MILLIS = 300
