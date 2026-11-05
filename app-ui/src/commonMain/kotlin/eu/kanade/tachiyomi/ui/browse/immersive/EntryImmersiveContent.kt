package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.presentation.browse.immersive.EntryImmersivePositionState
import eu.kanade.tachiyomi.ui.entry.open.rememberEntryOpenMessages
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.currentPlatformContext
import mihon.entry.interactions.media.EntryImmersiveOpenTargetResult
import mihon.entry.interactions.media.EntryImmersivePreloadRadiusResult
import mihon.entry.interactions.media.EntryImmersiveRendererResult
import mihon.entry.interactions.media.EntryImmersiveUnavailableReason
import mihon.entry.viewer.chrome.ui.ReaderMediaLoadOverlay
import mihon.entry.viewer.chrome.ui.ReaderMediaLoadState
import mihon.entry.viewer.chrome.ui.ReaderMediaLoadingBackground
import mihon.presentation.kit.refresh.PullRefresh
import mihon.presentation.kit.shortcut.KeyboardShortcutHandler
import mihon.presentation.kit.shortcut.ScreenShortcuts
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * [itemCount] is read where it is needed rather than passed as a number: [itemIdentity] may read a live list, such as
 * paging items, and a count captured at the last composition would let the pager ask that list for pages past its end
 * the moment a refresh shrinks it.
 */
@Composable
internal fun EntryImmersiveContent(
    itemCount: () -> Int,
    itemIdentity: (Int) -> EntryImmersiveItemKey?,
    itemContent: @Composable (Int) -> Entry?,
    immersiveModel: EntryImmersiveViewModel,
    contextLabel: String,
    modifier: Modifier = Modifier,
    contextSupportingLabel: String? = null,
    contextLeadingContent: (@Composable () -> Unit)? = null,
    onContextClick: (() -> Unit)?,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    onPagingBlockedChange: (Boolean) -> Unit,
    positionState: EntryImmersivePositionState,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pageRequest: Int = 0,
    onPageSettled: (Int) -> Unit = {},
    onNearEnd: (Int) -> Unit = {},
    /** Content for a page that holds no entry, or null for entry pages. */
    nonEntryPage: (Int) -> (@Composable () -> Unit)? = { null },
    overlayContent: @Composable BoxScope.(PagerState) -> Unit = {},
) {
    val context = currentPlatformContext()
    val graph = LocalAppUiGraph.current
    val immersiveState by immersiveModel.state.collectAsState()
    val currentItemCount = itemCount()
    val initialPageInBounds = positionState.itemIndex.coerceIn(0, (currentItemCount - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPageInBounds, pageCount = itemCount)
    val scope = rememberCoroutineScope()
    val entryOpenFeature = remember { graph.entryOpenFeature }
    val openMessages = rememberEntryOpenMessages()
    val immersiveFeature = remember { graph.entryImmersiveFeature }
    val currentOnPageSettled by rememberUpdatedState(onPageSettled)
    val currentOnNearEnd by rememberUpdatedState(onNearEnd)
    var pagingBlocked by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(false) }

    val preloadRadius = when (
        val result = itemIdentity(pagerState.currentPage)?.type?.let(immersiveFeature::preloadRadius)
    ) {
        is EntryImmersivePreloadRadiusResult.Available -> result.radius
        is EntryImmersivePreloadRadiusResult.Inapplicable, null -> 0
    }
    val retainedItemKeys = buildSet {
        pagerState.settledPage
            .takeIf { it in 0 until currentItemCount }
            ?.let(itemIdentity)
            ?.let(::add)
        for (page in pagerState.currentPage - preloadRadius..pagerState.currentPage + preloadRadius) {
            if (page in 0 until currentItemCount) itemIdentity(page)?.let(::add)
        }
    }
    LaunchedEffect(retainedItemKeys) {
        immersiveModel.retain(retainedItemKeys)
    }

    val refreshEnabled = !pagingBlocked && !refreshing
    val refreshFromTop: () -> Unit = {
        scope.launch {
            pagerState.scrollToPage(0)
            onRefresh()
        }
    }
    // The refresh button hides with the controls, so F5 is declared here as well to keep working while they are hidden.
    KeyboardShortcutHandler(ScreenShortcuts.Refresh, enabled = refreshEnabled, onTrigger = refreshFromTop)

    LaunchedEffect(pageRequest) {
        if (pageRequest > 0 && currentItemCount > 0) pagerState.animateScrollToPage(0)
    }

    LaunchedEffect(currentItemCount) {
        if (currentItemCount > 0 && pagerState.currentPage >= currentItemCount) {
            pagerState.scrollToPage(currentItemCount - 1)
        }
    }

    LaunchedEffect(pagerState, currentItemCount) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                positionState.updateItemIndex(page)
                currentOnPageSettled(page)
                if (page >= currentItemCount - LOAD_MORE_PAGE_THRESHOLD - 1) currentOnNearEnd(page)
            }
    }

    PullRefresh(
        refreshing = refreshing,
        enabled = shouldEnableImmersivePullRefresh(pagerState.settledPage, pagingBlocked),
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceContainer.Media),
                key = { page ->
                    itemIdentity(page)?.let(::entryImmersiveItemKey) ?: "immersive:$page"
                },
                userScrollEnabled = !pagingBlocked,
                beyondViewportPageCount = preloadRadius,
            ) { page ->
                nonEntryPage(page)?.let { content ->
                    content()
                    return@VerticalPager
                }

                val entry = itemContent(page)
                val itemKey = entry?.immersiveItemKey() ?: itemIdentity(page)
                val itemState = itemKey?.let(immersiveState.items::get)
                val isActive = page == pagerState.settledPage
                val preloadRange = (pagerState.currentPage - preloadRadius)..(pagerState.currentPage + preloadRadius)

                if (entry != null && (page in preloadRange || isActive)) {
                    LaunchedEffect(itemKey) { immersiveModel.load(context, entry) }
                }

                EntryImmersivePage(
                    entry = entry,
                    itemState = itemState,
                    isActive = isActive,
                    immersiveModel = immersiveModel,
                    contextLabel = contextLabel,
                    contextSupportingLabel = contextSupportingLabel,
                    contextLeadingContent = contextLeadingContent,
                    controlsVisible = controlsVisible,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onContextClick = onContextClick,
                    onExitImmersive = onExitImmersive,
                    onEntryClick = onEntryClick,
                    onLibraryAction = onLibraryAction,
                    showBackToTop = pagerState.currentPage > 0,
                    onBackToTop = { scope.launch { pagerState.animateScrollToPage(0) } },
                    onRefresh = refreshFromTop,
                    refreshEnabled = refreshEnabled,
                    onOpenChapter = if (entry != null && itemState is EntryImmersiveViewModel.ItemState.Ready) {
                        val openTarget = immersiveFeature.openTarget(itemState.handle)
                        val chapter = itemState.chapter
                        if (openTarget is EntryImmersiveOpenTargetResult.Available &&
                            chapter?.id == openTarget.childId
                        ) {
                            {
                                val result = entryOpenFeature.open(context = context, entry = entry, chapter = chapter)
                                openMessages.report(entry.type, result)
                            }
                        } else {
                            null
                        }
                    } else {
                        null
                    },
                    onPagingBlockedChange = { blocked ->
                        pagingBlocked = blocked
                        onPagingBlockedChange(blocked)
                    },
                    onRetry = { entry?.let { immersiveModel.retry(context, it) } },
                )
            }

            overlayContent(pagerState)
        }
    }
    EntryImmersivePageKeys(pagerState = pagerState, enabled = !pagingBlocked)
}

internal fun shouldEnableImmersivePullRefresh(settledPage: Int, pagingBlocked: Boolean): Boolean {
    return settledPage == 0 && !pagingBlocked
}

@Composable
private fun EntryImmersivePage(
    entry: Entry?,
    itemState: EntryImmersiveViewModel.ItemState?,
    isActive: Boolean,
    immersiveModel: EntryImmersiveViewModel,
    contextLabel: String,
    contextSupportingLabel: String?,
    contextLeadingContent: (@Composable () -> Unit)?,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onContextClick: (() -> Unit)?,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    showBackToTop: Boolean,
    onBackToTop: () -> Unit,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
    onOpenChapter: (() -> Unit)?,
    onPagingBlockedChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    var bottomOverlayHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val controlsBottomInset = with(density) { bottomOverlayHeightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // The pages beside the shown one are composed too, controls and all: Tab moving into them would scroll
            // the pager to an entry the viewer didn't pick.
            .focusProperties { onEnter = { if (!isActive) cancelFocusChange() } }
            .focusGroup()
            .background(SurfaceContainer.Media),
    ) {
        ReaderMediaLoadingBackground(modifier = Modifier.fillMaxSize())

        when (itemState) {
            is EntryImmersiveViewModel.ItemState.Ready -> {
                when (
                    val renderer = remember(itemState.handle, immersiveModel) {
                        immersiveModel.renderer(itemState.handle)
                    }
                ) {
                    is EntryImmersiveRendererResult.Available -> renderer.renderer.Content(
                        modifier = Modifier.fillMaxSize(),
                        active = isActive,
                        controlsVisible = controlsVisible,
                        controlsBottomInset = controlsBottomInset,
                        onToggleControls = onToggleControls,
                        onPagingBlockedChange = onPagingBlockedChange,
                        onProgress = { immersiveModel.persistProgress(itemState.handle, it) },
                    )
                    is EntryImmersiveRendererResult.Failed -> ReaderMediaLoadOverlay(
                        state = ReaderMediaLoadState.Failed(
                            renderer.error.message ?: stringResource(MR.strings.unknown_error),
                        ),
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            is EntryImmersiveViewModel.ItemState.Error -> ReaderMediaLoadOverlay(
                state = ReaderMediaLoadState.Failed(
                    itemState.throwable.message ?: stringResource(MR.strings.unknown_error),
                ),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            is EntryImmersiveViewModel.ItemState.Inapplicable -> ReaderMediaLoadOverlay(
                state = ReaderMediaLoadState.Failed(stringResource(MR.strings.source_unsupported)),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            is EntryImmersiveViewModel.ItemState.Unavailable -> ReaderMediaLoadOverlay(
                state = ReaderMediaLoadState.Failed(immersiveUnavailableMessage(itemState.reason)),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            is EntryImmersiveViewModel.ItemState.Loading, null -> if (isActive) {
                ReaderMediaLoadOverlay(
                    state = ReaderMediaLoadState.Loading(),
                    onBackgroundClick = onToggleControls,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (entry != null) {
            ImmersiveOverlay(
                visible = controlsVisible,
                entry = entry,
                chapterName = (itemState as? EntryImmersiveViewModel.ItemState.Ready)?.chapter?.name,
                contextLabel = contextLabel,
                contextSupportingLabel = contextSupportingLabel,
                contextLeadingContent = contextLeadingContent,
                onContextClick = onContextClick,
                onExitImmersive = onExitImmersive,
                onEntryClick = { onEntryClick(entry) },
                onLibraryAction = { onLibraryAction(entry) },
                showBackToTop = showBackToTop,
                onBackToTop = onBackToTop,
                onRefresh = onRefresh,
                refreshEnabled = refreshEnabled,
                onOpenChapter = onOpenChapter,
                onBottomOverlaySize = { bottomOverlayHeightPx = it },
            )
        }
    }
}

@Composable
private fun immersiveUnavailableMessage(reason: EntryImmersiveUnavailableReason): String {
    return when (reason) {
        EntryImmersiveUnavailableReason.SourceUnavailable ->
            stringResource(MR.strings.browse_video_feed_source_not_found)
        EntryImmersiveUnavailableReason.NoReadingChild -> stringResource(MR.strings.no_chapters_error)
    }
}

private const val LOAD_MORE_PAGE_THRESHOLD = 3
