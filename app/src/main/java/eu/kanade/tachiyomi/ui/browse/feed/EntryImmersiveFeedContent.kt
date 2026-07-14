package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.browse.immersive.rememberEntryImmersivePositionState
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveContent
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveItemKey
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveScreenModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
internal fun EntryImmersiveFeedContent(
    timelineModel: CatalogChronologicalFeedScreenModel,
    immersiveModel: EntryImmersiveScreenModel,
    snackbarHostState: SnackbarHostState,
    activeSource: Source,
    feedLabel: String,
    onShowFeedPicker: () -> Unit,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    onZoomStateChange: (Boolean) -> Unit,
    jumpToNewestRequest: Int,
    modifier: Modifier = Modifier,
) {
    val timelineState by timelineModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val retryLabel = stringResource(MR.strings.action_retry)
    val unknownError = stringResource(MR.strings.unknown_error)

    LaunchedEffect(timelineState.error) {
        val error = timelineState.error ?: return@LaunchedEffect
        if (timelineState.itemRefs.isEmpty()) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = error.message ?: unknownError,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Indefinite,
        )
        if (result == SnackbarResult.ActionPerformed) timelineModel.refresh(manual = true)
    }

    if (!timelineState.hasLoaded && timelineState.isRefreshing && timelineState.itemRefs.isEmpty()) {
        LoadingScreen(modifier)
        return
    }

    if (timelineState.itemRefs.isEmpty()) {
        EmptyScreen(
            modifier = modifier,
            message = timelineState.error?.message ?: stringResource(MR.strings.no_results_found),
            actions = persistentListOf(
                EmptyScreenAction(
                    stringRes = MR.strings.action_retry,
                    icon = Icons.Outlined.Refresh,
                    onClick = { timelineModel.refresh(manual = true) },
                ),
            ),
        )
        return
    }

    val initialSavedItem = remember(timelineModel) { timelineModel.savedAnchorSnapshot().resolvedItem() }
    val initialPage = initialSavedItem?.let(timelineState.itemRefs::indexOf)?.takeIf { it >= 0 } ?: 0
    val positionState = rememberEntryImmersivePositionState(
        resetKey = timelineModel,
        initialItemIndex = initialPage,
    )

    EntryImmersiveContent(
        itemCount = timelineState.itemRefs.size,
        itemIdentity = { page -> timelineState.itemRefs.getOrNull(page)?.toImmersiveItemKey() },
        itemContent = { page ->
            timelineState.itemRefs.getOrNull(page)?.let { rememberFeedEntry(it, timelineModel) }
        },
        immersiveModel = immersiveModel,
        contextLabel = feedLabel,
        contextLeadingContent = {
            SourceIcon(
                source = activeSource,
                modifier = Modifier
                    .size(20.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        },
        onContextClick = onShowFeedPicker,
        onExitImmersive = onExitImmersive,
        onEntryClick = onEntryClick,
        onLibraryAction = onLibraryAction,
        onZoomStateChange = onZoomStateChange,
        positionState = positionState,
        refreshing = timelineState.isRefreshing,
        onRefresh = { timelineModel.refresh(manual = true) },
        pageRequest = jumpToNewestRequest,
        onPageSettled = { page ->
            timelineModel.saveAnchor(timelineState.itemRefs.getOrNull(page), scrollOffset = 0)
        },
        onNearEnd = { timelineModel.loadMore() },
        modifier = modifier,
    ) { pagerState ->
        FeedNewItemsIndicator(
            state = timelineState,
            screenModel = timelineModel,
            viewportKey = pagerState,
            viewport = {
                FeedViewport(
                    canScrollBackward = pagerState.canScrollBackward,
                    isScrollInProgress = pagerState.isScrollInProgress,
                    lastScrolledBackward = pagerState.lastScrolledBackward,
                    totalItemsCount = pagerState.pageCount,
                )
            },
            onScrollToNewest = { coroutineScope.launch { pagerState.scrollToPage(0) } },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = WindowInsets.statusBarsIgnoringVisibility
                        .asPaddingValues()
                        .calculateTopPadding() + 16.dp,
                ),
        )
    }
}

@Composable
private fun rememberFeedEntry(
    itemRef: FeedItemRef,
    timelineModel: CatalogChronologicalFeedScreenModel,
): Entry? {
    var entry by remember(itemRef) { mutableStateOf<Entry?>(null) }
    LaunchedEffect(itemRef, timelineModel) {
        timelineModel.subscribeItem(itemRef).collectLatest { item ->
            entry = (item as CatalogListItem.EntryItem).entry
        }
    }
    return entry
}

private fun FeedItemRef.toImmersiveItemKey(): EntryImmersiveItemKey {
    return EntryImmersiveItemKey(id = id, type = type)
}
