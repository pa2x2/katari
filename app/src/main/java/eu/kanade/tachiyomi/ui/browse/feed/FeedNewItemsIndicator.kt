package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource

@Composable
internal fun FeedNewItemsIndicator(
    state: FeedViewModel.State,
    viewModel: FeedViewModel<*>,
    viewportKey: Any?,
    viewport: () -> FeedViewport,
    onScrollToNewest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(viewportKey, state.newItemsAvailableCount, state.pendingRefresh, state.itemRefs.size) {
        if (state.newItemsAvailableCount == 0) return@LaunchedEffect
        if (state.pendingRefresh != null) return@LaunchedEffect

        snapshotFlow(viewport)
            .distinctUntilChanged()
            .collectLatest { currentViewport ->
                if (shouldConsumeNewItemsIndicator(currentViewport, state.itemRefs.size)) {
                    viewModel.consumeNewItemsIndicator()
                }
            }
    }

    // Reading the viewport directly would recompose on every scrolled frame through layout info.
    val currentViewport by rememberUpdatedState(viewport)
    val canScrollBackward by remember { derivedStateOf { currentViewport().canScrollBackward } }
    if (!shouldShowNewItemsChip(state, canScrollBackward)) return

    NewItemsChip(
        count = state.newItemsAvailableCount,
        countIsLowerBound = state.newItemsCountIsLowerBound,
        isBridging = state.isBridgingRefresh,
        onClick = {
            viewModel.showNewItems()
            onScrollToNewest()
        },
        modifier = modifier,
    )
}

internal fun shouldShowNewItemsChip(
    state: FeedViewModel.State,
    canScrollBackward: Boolean,
): Boolean {
    return state.newItemsAvailableCount > 0 &&
        !state.isRefreshing &&
        (state.pendingRefresh != null || canScrollBackward)
}

internal data class FeedViewport(
    val canScrollBackward: Boolean,
    val isScrollInProgress: Boolean,
    val lastScrolledBackward: Boolean,
    val totalItemsCount: Int,
)

internal fun shouldConsumeNewItemsIndicator(
    viewport: FeedViewport,
    itemCount: Int,
): Boolean {
    if (viewport.totalItemsCount < itemCount) return false

    return !viewport.canScrollBackward ||
        (viewport.isScrollInProgress && viewport.lastScrolledBackward)
}

@Composable
internal fun NewItemsChip(
    count: Int,
    countIsLowerBound: Boolean,
    isBridging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        container = SurfaceContainer.SecondaryContainer,
        shape = KatariTheme.shapes.extraLarge,
        shadow = true,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isBridging) {
                LoadingIndicator(
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                )
            }
            Text(
                role = TextRole.Label,
                text = pluralStringResource(
                    if (countIsLowerBound) {
                        MR.plurals.browse_feed_new_items_at_least
                    } else {
                        MR.plurals.browse_feed_new_items
                    },
                    count,
                    count,
                ),
            )
        }
    }
}
