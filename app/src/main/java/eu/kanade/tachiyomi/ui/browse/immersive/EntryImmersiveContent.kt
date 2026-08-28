package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import eu.kanade.presentation.browse.immersive.EntryImmersivePositionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mihon.entry.interactions.media.EntryImmersiveFeature
import mihon.entry.interactions.media.EntryImmersiveOpenTargetResult
import mihon.entry.interactions.media.EntryImmersivePreloadRadiusResult
import mihon.entry.interactions.media.EntryImmersiveRendererResult
import mihon.entry.interactions.media.EntryImmersiveUnavailableReason
import mihon.entry.interactions.navigation.EntryOpenFeature
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadOverlay
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadState
import tachiyomi.presentation.core.components.reader.ReaderMediaLoadingBackground
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun EntryImmersiveContent(
    itemCount: Int,
    itemIdentity: (Int) -> EntryImmersiveItemKey?,
    itemContent: @Composable (Int) -> Entry?,
    immersiveModel: EntryImmersiveScreenModel,
    contextLabel: String,
    modifier: Modifier = Modifier,
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
    overlayContent: @Composable BoxScope.(PagerState) -> Unit = {},
) {
    val context = LocalContext.current
    val immersiveState by immersiveModel.state.collectAsState()
    val initialPageInBounds = positionState.itemIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPageInBounds) { itemCount }
    val scope = rememberCoroutineScope()
    val entryOpenFeature = remember { Injekt.get<EntryOpenFeature>() }
    val immersiveFeature = remember { Injekt.get<EntryImmersiveFeature>() }
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
            .takeIf { it in 0 until itemCount }
            ?.let(itemIdentity)
            ?.let(::add)
        for (page in pagerState.currentPage - preloadRadius..pagerState.currentPage + preloadRadius) {
            if (page in 0 until itemCount) itemIdentity(page)?.let(::add)
        }
    }
    LaunchedEffect(retainedItemKeys) {
        immersiveModel.retain(retainedItemKeys)
    }

    LaunchedEffect(pageRequest) {
        if (pageRequest > 0 && itemCount > 0) pagerState.animateScrollToPage(0)
    }

    LaunchedEffect(itemCount) {
        if (itemCount > 0 && pagerState.currentPage >= itemCount) {
            pagerState.scrollToPage(itemCount - 1)
        }
    }

    LaunchedEffect(pagerState, itemCount) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { page ->
                positionState.updateItemIndex(page)
                currentOnPageSettled(page)
                if (page >= itemCount - LOAD_MORE_PAGE_THRESHOLD - 1) currentOnNearEnd(page)
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
                    .background(Color.Black),
                key = { page ->
                    itemIdentity(page)?.let(::entryImmersiveItemKey) ?: "immersive:$page"
                },
                userScrollEnabled = !pagingBlocked,
                beyondViewportPageCount = preloadRadius,
            ) { page ->
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
                    contextLeadingContent = contextLeadingContent,
                    controlsVisible = controlsVisible,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onContextClick = onContextClick,
                    onExitImmersive = onExitImmersive,
                    onEntryClick = onEntryClick,
                    onLibraryAction = onLibraryAction,
                    showBackToTop = pagerState.currentPage > 0,
                    onBackToTop = { scope.launch { pagerState.animateScrollToPage(0) } },
                    onOpenChapter = if (entry != null && itemState is EntryImmersiveScreenModel.ItemState.Ready) {
                        val openTarget = immersiveFeature.openTarget(itemState.handle)
                        val chapter = itemState.chapter
                        if (openTarget is EntryImmersiveOpenTargetResult.Available &&
                            chapter?.id == openTarget.childId
                        ) {
                            {
                                entryOpenFeature.open(
                                    context = context,
                                    entry = entry,
                                    chapter = chapter,
                                )
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
}

internal fun shouldEnableImmersivePullRefresh(settledPage: Int, pagingBlocked: Boolean): Boolean {
    return settledPage == 0 && !pagingBlocked
}

@Composable
private fun EntryImmersivePage(
    entry: Entry?,
    itemState: EntryImmersiveScreenModel.ItemState?,
    isActive: Boolean,
    immersiveModel: EntryImmersiveScreenModel,
    contextLabel: String,
    contextLeadingContent: (@Composable () -> Unit)?,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onContextClick: (() -> Unit)?,
    onExitImmersive: () -> Unit,
    onEntryClick: (Entry) -> Unit,
    onLibraryAction: (Entry) -> Unit,
    showBackToTop: Boolean,
    onBackToTop: () -> Unit,
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
            .background(Color.Black),
    ) {
        ReaderMediaLoadingBackground(modifier = Modifier.fillMaxSize())

        when (itemState) {
            is EntryImmersiveScreenModel.ItemState.Ready -> {
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
            is EntryImmersiveScreenModel.ItemState.Error -> ReaderMediaLoadOverlay(
                state = ReaderMediaLoadState.Failed(
                    itemState.throwable.message ?: stringResource(MR.strings.unknown_error),
                ),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            is EntryImmersiveScreenModel.ItemState.Inapplicable -> ReaderMediaLoadOverlay(
                state = ReaderMediaLoadState.Failed(stringResource(MR.strings.source_unsupported)),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            is EntryImmersiveScreenModel.ItemState.Unavailable -> ReaderMediaLoadOverlay(
                state = ReaderMediaLoadState.Failed(immersiveUnavailableMessage(itemState.reason)),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            is EntryImmersiveScreenModel.ItemState.Loading, null -> if (isActive) {
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
                chapterName = (itemState as? EntryImmersiveScreenModel.ItemState.Ready)?.chapter?.name,
                contextLabel = contextLabel,
                contextLeadingContent = contextLeadingContent,
                onContextClick = onContextClick,
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
private fun immersiveUnavailableMessage(reason: EntryImmersiveUnavailableReason): String {
    return when (reason) {
        EntryImmersiveUnavailableReason.SourceUnavailable ->
            stringResource(MR.strings.browse_video_feed_source_not_found)
        EntryImmersiveUnavailableReason.SourceOptedOut -> stringResource(MR.strings.source_unsupported)
        EntryImmersiveUnavailableReason.NoReadingChild -> stringResource(MR.strings.no_chapters_error)
    }
}

private const val LOAD_MORE_PAGE_THRESHOLD = 3
