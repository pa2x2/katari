package mihon.presentation.kit.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.navigation.LocalAppBarScrollBehavior

/**
 * @param refreshing Whether the layout is currently refreshing
 * @param onRefresh Lambda which is invoked when a swipe to refresh gesture is completed.
 * @param enabled Whether the the layout should react to swipe gestures or not.
 * @param indicatorPadding Content padding for the indicator, to inset the indicator in if required.
 * @param content The content containing a vertically scrollable composable.
 */
@Composable
fun PullRefresh(
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    val appBarState = LocalAppBarScrollBehavior.current?.state
    val expandAppBarFirst = remember(appBarState) { appBarState?.let(::ExpandAppBarBeforePull) }
    Box(
        modifier = modifier
            .pullToRefresh(
                isRefreshing = refreshing,
                state = state,
                enabled = enabled,
                onRefresh = onRefresh,
            )
            .then(expandAppBarFirst?.let { Modifier.nestedScroll(it) } ?: Modifier),
    ) {
        content()

        PullToRefreshDefaults.Indicator(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(indicatorPadding),
            isRefreshing = refreshing,
            state = state,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Pull-to-refresh sits inside the scaffold's nested scroll, so it takes a drag past the content's top before a collapsed
 * large app bar can grow back, and Material's fling settling leaves a fully collapsed bar alone. Without this the title
 * could not come back once the content was at its top. Placed inside the pull, it grows the bar first and leaves the
 * rest of the drag to the pull.
 */
private class ExpandAppBarBeforePull(private val appBarState: TopAppBarState) : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        val before = appBarState.heightOffset
        appBarState.heightOffset = before + available.y
        return Offset(0f, appBarState.heightOffset - before)
    }
}
