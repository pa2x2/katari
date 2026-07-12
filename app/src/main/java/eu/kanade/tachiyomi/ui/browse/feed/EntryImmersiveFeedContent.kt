package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.domain.source.model.FeedItemRef
import eu.kanade.presentation.entry.entryTypePresentation
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryImmersiveFeedHandle
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun EntryImmersiveFeedContent(
    timelineModel: CatalogChronologicalFeedScreenModel,
    immersiveModel: EntryImmersiveFeedScreenModel,
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
    val context = LocalContext.current
    val timelineState by timelineModel.state.collectAsState()
    val immersiveState by immersiveModel.state.collectAsState()
    val retryLabel = stringResource(MR.strings.action_retry)
    val unknownError = stringResource(MR.strings.unknown_error)
    val pagerState = rememberPagerState { timelineState.itemRefs.size }
    val scope = rememberCoroutineScope()
    val entryOpenInteraction = remember { Injekt.get<mihon.entry.interactions.EntryOpenInteraction>() }
    // Keep the entry anchor stable after the initial restore. Immersive scrolling updates
    // the model's saved anchor, but that must not invalidate the pagination collector.
    val initialSavedItem = remember(timelineModel) { timelineModel.savedAnchorSnapshot().resolvedItem() }
    val anchorRestoreKey = initialSavedItem?.let { "${it.id}:${it.type}" }.orEmpty()
    var restoredAnchorKey by rememberSaveable { mutableStateOf<String?>(null) }
    var isZoomed by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(false) }

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

    LaunchedEffect(timelineState.itemRefs, anchorRestoreKey) {
        if (restoredAnchorKey == anchorRestoreKey || timelineState.itemRefs.isEmpty()) return@LaunchedEffect
        val index = initialSavedItem?.let(timelineState.itemRefs::indexOf)?.takeIf { it >= 0 } ?: 0
        pagerState.scrollToPage(index)
        restoredAnchorKey = anchorRestoreKey
    }

    LaunchedEffect(timelineState.itemRefs, pagerState.currentPage, pagerState.settledPage) {
        val retainedPages = buildSet {
            add(pagerState.settledPage)
            for (page in pagerState.currentPage - PRELOAD_RADIUS..pagerState.currentPage + PRELOAD_RADIUS) add(page)
        }
        immersiveModel.retain(retainedPages.mapNotNull(timelineState.itemRefs::getOrNull).toSet())
    }

    LaunchedEffect(jumpToNewestRequest) {
        if (jumpToNewestRequest > 0 && timelineState.itemRefs.isNotEmpty()) {
            pagerState.animateScrollToPage(0)
        }
    }

    LaunchedEffect(
        timelineState.isRefreshing,
        timelineState.isAppending,
        timelineState.nextPageKey,
        timelineState.itemRefs.size,
    ) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                if (restoredAnchorKey != anchorRestoreKey) return@collectLatest
                timelineModel.saveAnchor(timelineState.itemRefs.getOrNull(page), scrollOffset = 0)
                if (page >= timelineState.itemRefs.lastIndex - LOAD_MORE_PAGE_THRESHOLD) {
                    timelineModel.loadMore()
                }
            }
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

    Box(modifier = modifier) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            key = { immersiveFeedItemKey(timelineState.itemRefs[it]) },
            userScrollEnabled = !isZoomed,
            beyondViewportPageCount = PRELOAD_RADIUS,
        ) { page ->
            val itemRef = timelineState.itemRefs[page]
            val entry = rememberFeedEntry(itemRef, timelineModel)
            val itemState = immersiveState.items[itemRef]
            val isActive = page == pagerState.settledPage
            val preloadRange = (pagerState.currentPage - PRELOAD_RADIUS)..(pagerState.currentPage + PRELOAD_RADIUS)
            val shouldLoad = page in preloadRange || isActive

            if (entry != null && shouldLoad) {
                LaunchedEffect(itemRef) { immersiveModel.load(context, entry) }
            }

            EntryImmersiveFeedPage(
                entry = entry,
                itemState = itemState,
                isActive = isActive,
                immersiveModel = immersiveModel,
                activeSource = activeSource,
                feedLabel = feedLabel,
                controlsVisible = controlsVisible,
                onToggleControls = { controlsVisible = !controlsVisible },
                onShowFeedPicker = onShowFeedPicker,
                onExitImmersive = onExitImmersive,
                onEntryClick = onEntryClick,
                onLibraryAction = onLibraryAction,
                showBackToTop = pagerState.currentPage > 0,
                onBackToTop = { scope.launch { pagerState.animateScrollToPage(0) } },
                onOpenChapter = if (
                    entry != null &&
                    itemState is EntryImmersiveFeedScreenModel.ItemState.Ready
                ) {
                    {
                        entryOpenInteraction.open(
                            context = context,
                            entry = entry,
                            chapter = itemState.chapter,
                        )
                    }
                } else {
                    null
                },
                onZoomStateChange = { zoomed ->
                    isZoomed = zoomed
                    onZoomStateChange(zoomed)
                },
                onRetry = { entry?.let { immersiveModel.retry(context, it) } },
            )
        }

        if (timelineState.newItemsAvailableCount > 0 && !timelineState.isRefreshing) {
            NewItemsChip(
                count = timelineState.newItemsAvailableCount,
                countIsLowerBound = timelineState.newItemsCountIsLowerBound,
                onClick = {
                    timelineModel.showNewItems()
                    scope.launch { pagerState.scrollToPage(0) }
                },
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
}

@Composable
private fun EntryImmersiveFeedPage(
    entry: Entry?,
    itemState: EntryImmersiveFeedScreenModel.ItemState?,
    isActive: Boolean,
    immersiveModel: EntryImmersiveFeedScreenModel,
    activeSource: Source,
    feedLabel: String,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onShowFeedPicker: () -> Unit,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    showBackToTop: Boolean,
    onBackToTop: () -> Unit,
    onOpenChapter: (() -> Unit)?,
    onZoomStateChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    var bottomOverlayHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val controlsBottomInset = with(density) { bottomOverlayHeightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val readyHandle = (itemState as? EntryImmersiveFeedScreenModel.ItemState.Ready)?.handle
        if (entry != null && shouldShowImmersiveFeedPoster(readyHandle)) {
            AsyncImage(
                model = entry.asEntryCover(),
                contentDescription = entry.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }

        when (itemState) {
            is EntryImmersiveFeedScreenModel.ItemState.Ready -> {
                val renderer = remember(itemState.handle, immersiveModel) {
                    immersiveModel.renderer(itemState.handle)
                }
                renderer.Content(
                    modifier = Modifier.fillMaxSize(),
                    active = isActive,
                    controlsVisible = controlsVisible,
                    controlsBottomInset = controlsBottomInset,
                    onToggleControls = onToggleControls,
                    onZoomStateChange = onZoomStateChange,
                    onProgress = { immersiveModel.persistProgress(itemState.handle, it) },
                )
            }
            is EntryImmersiveFeedScreenModel.ItemState.Error -> FeedError(
                message = itemState.throwable.message ?: stringResource(MR.strings.unknown_error),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
            is EntryImmersiveFeedScreenModel.ItemState.Loading, null -> if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleControls,
                        ),
                )
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            }
        }

        if (entry != null) {
            ImmersiveFeedOverlay(
                visible = controlsVisible,
                entry = entry,
                chapterName = (itemState as? EntryImmersiveFeedScreenModel.ItemState.Ready)?.chapter?.name,
                source = activeSource,
                feedLabel = feedLabel,
                onShowFeedPicker = onShowFeedPicker,
                onExitImmersive = onExitImmersive,
                onEntryClick = { onEntryClick(entry) },
                onLibraryAction = { onLibraryAction(entry) },
                showBackToTop = showBackToTop,
                onBackToTop = onBackToTop,
                onOpenChapter = onOpenChapter,
                onBottomOverlaySize = { bottomOverlayHeightPx = it },
            )
        }
    }
}

@Composable
private fun ImmersiveFeedOverlay(
    visible: Boolean,
    entry: Entry,
    chapterName: String?,
    source: Source,
    feedLabel: String,
    onShowFeedPicker: () -> Unit,
    onExitImmersive: () -> Unit,
    onEntryClick: () -> Unit,
    onLibraryAction: () -> Unit,
    showBackToTop: Boolean,
    onBackToTop: () -> Unit,
    onOpenChapter: (() -> Unit)?,
    onBottomOverlaySize: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it / 5 },
            exit = fadeOut() + slideOutVertically { -it / 5 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent),
                        ),
                    )
                    .padding(
                        start = 12.dp,
                        top = WindowInsets.statusBarsIgnoringVisibility
                            .asPaddingValues()
                            .calculateTopPadding() + 12.dp,
                        end = 12.dp,
                        bottom = 36.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedImmersivePill(
                    source = source,
                    label = feedLabel,
                    onClick = onShowFeedPicker,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 5 },
            exit = fadeOut() + slideOutVertically { it / 5 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { onBottomOverlaySize(it.height) }
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                        ),
                    )
                    .padding(
                        start = 16.dp,
                        top = 40.dp,
                        end = 16.dp,
                        bottom = WindowInsets.navigationBarsIgnoringVisibility
                            .asPaddingValues()
                            .calculateBottomPadding() + 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = entry.displayTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (chapterName != null) {
                        Text(
                            text = chapterName,
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showBackToTop) {
                        ImmersiveActionButton(
                            label = stringResource(MR.strings.action_move_to_top),
                            icon = Icons.Outlined.KeyboardArrowUp,
                            onClick = onBackToTop,
                        )
                    }
                    ImmersiveActionButton(
                        label = stringResource(MR.strings.browse_feed_exit_immersive),
                        icon = Icons.Outlined.FullscreenExit,
                        onClick = onExitImmersive,
                    )
                    ImmersiveActionButton(
                        label = stringResource(
                            if (entry.favorite) MR.strings.remove_from_library else MR.strings.add_to_library,
                        ),
                        icon = if (entry.favorite) Icons.Outlined.Delete else Icons.Outlined.FavoriteBorder,
                        onClick = onLibraryAction,
                    )
                    ImmersiveActionButton(
                        label = stringResource(MR.strings.action_details),
                        icon = Icons.Outlined.Info,
                        onClick = onEntryClick,
                    )
                    val presentation = entry.type.entryTypePresentation()
                    ImmersiveActionButton(
                        label = stringResource(presentation.immersiveOpenLabel),
                        icon = presentation.immersiveOpenIcon,
                        onClick = { onOpenChapter?.invoke() },
                        enabled = onOpenChapter != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImmersiveActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = if (enabled) 0.12f else 0.05f),
        contentColor = Color.White.copy(alpha = if (enabled) 1f else 0.38f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

internal fun shouldShowImmersiveFeedPoster(handle: EntryImmersiveFeedHandle?): Boolean {
    return true
}

@Composable
private fun FeedError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry) { Text(text = stringResource(MR.strings.action_retry)) }
        }
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

internal fun immersiveFeedItemKey(itemRef: FeedItemRef): String {
    return "${itemRef.type.name}:${itemRef.id}"
}

private const val PRELOAD_RADIUS = 1
private const val LOAD_MORE_PAGE_THRESHOLD = 3
