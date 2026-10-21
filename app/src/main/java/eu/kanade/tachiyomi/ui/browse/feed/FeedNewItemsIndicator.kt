package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.chip.FloatingChip
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource

/**
 * The chip that jumps to items a refresh added above the reader, and the reader reaching the newest item: that
 * clears the chip and marks the feed's new items as viewed. [rowCount] is the number of rows the viewport lays out
 * for the current state; until it does, the viewport still describes the previous rows.
 */
@Composable
internal fun FeedNewItemsIndicator(
    state: FeedViewModel.State,
    viewModel: FeedViewModel<*>,
    rowCount: Int,
    viewportKey: Any?,
    viewport: () -> FeedViewport,
    onScrollToNewest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewportKey, state.newItemsAvailableCount, rowCount) {
        if (state.newItemsAvailableCount == 0) return@LaunchedEffect

        snapshotFlow(viewport)
            .distinctUntilChanged()
            .collectLatest { currentViewport ->
                if (shouldConsumeNewItemsIndicator(currentViewport, rowCount)) {
                    viewModel.consumeNewItemsIndicator()
                }
            }
    }

    LaunchedEffect(viewportKey, state.newItems, rowCount) {
        if (state.newItems?.viewed != false) return@LaunchedEffect

        snapshotFlow(viewport)
            .first { it.totalItemsCount >= rowCount && !it.canScrollBackward }
        viewModel.markNewItemsViewed()
    }

    // Reading the viewport directly would recompose on every scrolled frame through layout info.
    val currentViewport by rememberUpdatedState(viewport)
    val canScrollBackward by remember { derivedStateOf { currentViewport().canScrollBackward } }
    if (!shouldShowNewItemsChip(state, canScrollBackward)) return

    NewItemsChip(
        count = state.newItemsAvailableCount,
        isLoading = state.gapLoad != null,
        onClick = onScrollToNewest,
        modifier = modifier,
    )
}

internal fun shouldShowNewItemsChip(
    state: FeedViewModel.State,
    canScrollBackward: Boolean,
): Boolean {
    return state.newItemsAvailableCount > 0 && !state.isRefreshing && canScrollBackward
}

internal data class FeedViewport(
    val canScrollBackward: Boolean,
    val isScrollInProgress: Boolean,
    val lastScrolledBackward: Boolean,
    val totalItemsCount: Int,
)

internal fun shouldConsumeNewItemsIndicator(
    viewport: FeedViewport,
    rowCount: Int,
): Boolean {
    if (viewport.totalItemsCount < rowCount) return false

    return !viewport.canScrollBackward ||
        (viewport.isScrollInProgress && viewport.lastScrolledBackward)
}

@Composable
internal fun NewItemsChip(
    count: Int,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingChip(
        label = pluralStringResource(MR.plurals.browse_feed_new_items, count, count),
        icon = MaterialSymbols.Rounded.KeyboardArrowUp,
        onClick = onClick,
        modifier = modifier,
        loading = isLoading,
    )
}
