package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter

/** Stable-key, adjacent-session vertical document stream. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookDocumentEndlessViewer(
    currentChapter: EntryChapter,
    currentChapterId: Long,
    window: EntryChildWindow<EntryChapter>,
    loadedSections: Map<Long, BookDocumentSection<EntryChapter>>,
    loadStates: Map<Long, BookDocumentChapterLoadState>,
    navigationRequest: BookDocumentNavigationRequest?,
    textSizePercent: Int,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onAnchorMissing: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onScrollStarted: () -> Unit,
    onUserScrollStarted: () -> Unit,
    onReaderTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val proposedItems = remember(window, loadedSections) {
        buildBookDocumentViewerItems(window, loadedSections, EntryChapter::id)
    }
    var items by remember { mutableStateOf(proposedItems) }
    val currentSection = loadedSections[currentChapterId]
    val initialIndex = currentSection?.let { section ->
        items.indexOfPosition(section.key, section.initialPosition).coerceAtLeast(0)
    } ?: 0
    val chapterPrefetchStrategy = remember { BookDocumentChapterPrefetchStrategy() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        prefetchStrategy = chapterPrefetchStrategy,
    )
    val currentItems by rememberUpdatedState(items)
    val currentOnScrollStarted by rememberUpdatedState(onScrollStarted)
    val currentOnUserScrollStarted by rememberUpdatedState(onUserScrollStarted)
    val currentOnAnchorMissing by rememberUpdatedState(onAnchorMissing)
    val currentOnExternalLinkClick by rememberUpdatedState(onExternalLinkClick)
    val currentOnReaderTap by rememberUpdatedState(onReaderTap)
    val currentOnTransitionReached by rememberUpdatedState(onTransitionReached)
    val currentLoadStates by rememberUpdatedState(loadStates)
    val currentTextSizePercent by rememberUpdatedState(textSizePercent)
    var observedDatasetIdentity by remember(listState) { mutableStateOf(items.identity) }
    var initialPositionRestored by remember(listState) { mutableStateOf(false) }
    var observedTextSizePercent by remember(listState) { mutableIntStateOf(textSizePercent) }
    var lastObservedLocation by remember(listState) {
        mutableStateOf<BookDocumentViewerLocation<EntryChapter>?>(null)
    }
    var textSizeReflowAnchor by remember(listState) {
        mutableStateOf<BookDocumentViewerLocation<EntryChapter>?>(null)
    }
    val prefetchTarget = remember(window.next?.id, loadedSections, items) {
        val nextSectionKey = window.next?.id?.let(loadedSections::get)?.key
        val nextSectionIndex = nextSectionKey?.let { sectionKey ->
            items.indexOfSection(sectionKey)
        } ?: -1
        nextSectionKey to nextSectionIndex
    }

    LaunchedEffect(proposedItems, listState) {
        if (proposedItems.identity == items.identity) return@LaunchedEffect
        val directionChangingTransitions = items.transitionKeysChangingDirection(proposedItems)
        val stableTailExpansion = items.isStablePrefixOf(proposedItems)
        val stableForwardAdvance = items.advancesToLoadedNext(proposedItems)
        val stableBackwardRetreat = items.retreatsToLoadedPrevious(proposedItems)
        snapshotFlow {
            (
                stableTailExpansion ||
                    stableForwardAdvance ||
                    stableBackwardRetreat ||
                    !listState.isScrollInProgress
                ) &&
                listState.layoutInfo.visibleItemsInfo.none { it.key in directionChangingTransitions }
        }
            .filter { ready -> ready }
            .first()
        items = proposedItems
    }

    val anchorClick = remember(listState) {
        { section: BookDocumentSection<EntryChapter>, fragment: String ->
            val target = section.document.document.anchors[fragment]
            if (target == null) {
                currentOnAnchorMissing(fragment)
            } else {
                val index = currentItems.indexOfPosition(section.key, target)
                if (index >= 0) listState.requestScrollToItem(index)
            }
        }
    }
    val externalLinkClick = remember { { url: String -> currentOnExternalLinkClick(url) } }
    val readerTap = remember { { currentOnReaderTap() } }
    val transitionRetry = remember { { chapter: EntryChapter -> currentOnTransitionReached(chapter) } }
    val chapterLoadState = remember { { chapterId: Long -> currentLoadStates[chapterId] } }

    suspend fun scrollToSectionPosition(
        section: BookDocumentSection<EntryChapter>,
        position: mihon.book.api.document.BookDocumentPosition = section.initialPosition,
    ) {
        val index = items.indexOfPosition(section.key, position)
        if (index < 0) return
        listState.scrollToItem(index)
        val layout = snapshotFlow {
            val info = listState.layoutInfo
            info.visibleItemsInfo.firstOrNull { it.index == index }?.let { item ->
                Triple(item.size, info.viewportStartOffset, info.viewportEndOffset)
            }
        }.filterNotNull().first()
        listState.scrollToItem(
            index,
            bookDocumentScrollOffset(
                document = section.document,
                position = position,
                itemSize = layout.first,
                viewportStartOffset = layout.second,
                viewportEndOffset = layout.third,
            ),
        )
    }

    SideEffect {
        chapterPrefetchStrategy.updateTarget(prefetchTarget.first, prefetchTarget.second)

        if (
            items.identity != observedDatasetIdentity &&
            !listState.isScrollInProgress &&
            navigationRequest == null
        ) {
            val info = listState.layoutInfo
            bookDocumentViewerDatasetAnchor(
                items = items,
                visibleItems = info.visibleBookDocumentLayouts(),
                viewportStartOffset = info.viewportStartOffset,
            )?.let { anchor -> listState.requestScrollToItem(anchor.index, anchor.scrollOffset) }
        }
        observedDatasetIdentity = items.identity
    }

    LaunchedEffect(items) {
        if (!initialPositionRestored) {
            currentSection?.let { scrollToSectionPosition(it) }
            initialPositionRestored = true
        }
    }

    LaunchedEffect(textSizePercent, initialPositionRestored) {
        if (!initialPositionRestored || textSizePercent == observedTextSizePercent) return@LaunchedEffect
        val anchor = textSizeReflowAnchor ?: lastObservedLocation
        observedTextSizePercent = textSizePercent
        if (anchor == null) return@LaunchedEffect
        textSizeReflowAnchor = anchor
        scrollToSectionPosition(anchor.section, anchor.position)
        textSizeReflowAnchor = null
    }

    LaunchedEffect(currentChapterId, items, initialPositionRestored) {
        if (!initialPositionRestored) return@LaunchedEffect
        if (navigationRequest != null) return@LaunchedEffect
        val section = loadedSections[currentChapterId] ?: return@LaunchedEffect
        val visibleChapterIds = listState.layoutInfo.visibleItemsInfo.mapNotNull { layout ->
            (items.resolve(layout.index, layout.key) as? BookDocumentViewerItem.Block)?.section?.owner?.id
        }
        if (currentChapterId !in visibleChapterIds) {
            val index = items.indexOfPosition(section.key, section.initialPosition)
            if (index >= 0) scrollToSectionPosition(section)
        }
    }

    LaunchedEffect(navigationRequest) {
        val request = navigationRequest ?: return@LaunchedEffect
        val section = loadedSections[request.chapterId] ?: return@LaunchedEffect
        scrollToSectionPosition(section, request.position)
    }

    LaunchedEffect(listState, items) {
        var observedLocation: BookDocumentViewerLocation<EntryChapter>? = null
        var observedTransition: EntryChapter? = null
        snapshotFlow {
            val info = listState.layoutInfo
            val visibleItems = info.visibleBookDocumentLayouts()
            BookDocumentReadingLayoutObservation(
                location = bookDocumentViewerLocation(
                    currentItems,
                    visibleItems,
                    info.viewportStartOffset,
                    info.viewportEndOffset,
                ),
                transition = bookDocumentViewerTransitionAtAnchor(
                    currentItems,
                    visibleItems,
                    info.viewportStartOffset,
                    info.viewportEndOffset,
                    listState.canScrollBackward,
                    listState.canScrollForward,
                )?.to,
            )
        }.collect { observation ->
            observation.location?.takeIf { it != observedLocation }?.let { location ->
                observedLocation = location
                if (
                    textSizeReflowAnchor == null &&
                    currentTextSizePercent == observedTextSizePercent
                ) {
                    lastObservedLocation = location
                }
                onLocation(location)
            }
            observation.transition?.takeIf { it != observedTransition }?.let { transition ->
                observedTransition = transition
                currentOnTransitionReached(transition)
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (textSizeReflowAnchor == null) currentOnScrollStarted()
            }
    }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions
            .filterIsInstance<DragInteraction.Start>()
            .collect { currentOnUserScrollStarted() }
    }
    LaunchedEffect(listState, items, currentChapterId) {
        snapshotFlow {
            val info = listState.layoutInfo
            val terminalVisible = info.visibleItemsInfo.any { layout ->
                val transition = (currentItems.resolve(layout.index, layout.key) as? BookDocumentViewerItem.Transition)
                    ?.transition
                transition?.direction == EntryChildDirection.NEXT &&
                    transition.to == null &&
                    layout.offset < info.viewportEndOffset &&
                    layout.offset + layout.size > info.viewportStartOffset
            }
            TerminalLayoutObservation(
                currentChapterId,
                terminalVisible,
                listState.canScrollForward,
                listState.isScrollInProgress,
            )
        }.distinctUntilChanged().collect { observation ->
            if (currentChapter.id != observation.chapterId) return@collect
            onTerminalObservation(
                currentChapter,
                observation.terminalVisible,
                observation.canScrollForward,
                observation.scrollInProgress,
            )
            if (observation.terminalVisible && !observation.canScrollForward && !observation.scrollInProgress) {
                withFrameNanos { }
                val info = listState.layoutInfo
                val stillVisible = info.visibleItemsInfo.any { layout ->
                    val transition = (
                        currentItems.resolve(layout.index, layout.key)
                            as? BookDocumentViewerItem.Transition
                        )?.transition
                    transition?.direction == EntryChildDirection.NEXT && transition.to == null &&
                        layout.offset < info.viewportEndOffset &&
                        layout.offset + layout.size > info.viewportStartOffset
                }
                if (stillVisible && !listState.canScrollForward && !listState.isScrollInProgress) {
                    onTerminalObservation(currentChapter, true, false, false)
                }
            }
        }
    }

    BookDocumentChapterSelectionContainer(
        chapterId = currentChapterId,
        modifier = modifier,
    ) { selection ->
        BookDocumentViewerList(
            items = items,
            state = listState,
            selection = selection,
            chapterLoadState = chapterLoadState,
            onAnchorClick = anchorClick,
            onExternalLinkClick = externalLinkClick,
            onReaderTap = readerTap,
            onTransitionRetry = transitionRetry,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListLayoutInfo.visibleBookDocumentLayouts() =
    visibleItemsInfo.map {
        BookDocumentVisibleItemLayout(index = it.index, key = it.key, offset = it.offset, size = it.size)
    }

private fun <T> List<BookDocumentViewerItem<T>>.resolve(index: Int, key: Any): BookDocumentViewerItem<T>? =
    getOrNull(index)?.takeIf { it.key == key } ?: firstOrNull { it.key == key }

private data class TerminalLayoutObservation(
    val chapterId: Long,
    val terminalVisible: Boolean,
    val canScrollForward: Boolean,
    val scrollInProgress: Boolean,
)

private data class BookDocumentReadingLayoutObservation(
    val location: BookDocumentViewerLocation<EntryChapter>?,
    val transition: EntryChapter?,
)
