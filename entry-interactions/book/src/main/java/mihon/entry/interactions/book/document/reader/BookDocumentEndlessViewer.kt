package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.layout.onPlaced
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.position.BookDocumentViewportGeometry
import mihon.entry.interactions.book.document.reader.position.LocalBookDocumentViewportGeometry
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
    loadedSections: Map<Long, BookDocumentPublicationSections<EntryChapter>>,
    loadStates: Map<Long, BookDocumentChapterLoadState>,
    navigationRequest: BookDocumentNavigationRequest?,
    textSizePercent: Int,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onAnchorMissing: (String) -> Unit,
    onInternalLinkClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onScrollStarted: () -> Unit,
    onUserScrollStarted: () -> Unit,
    onReaderTap: () -> Unit,
    initialLocation: BookDocumentViewerLocation<EntryChapter>? = null,
    onViewportLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val proposedItems = remember(window, loadedSections) {
        buildBookDocumentPublicationViewerItems(window, loadedSections, EntryChapter::id)
    }
    var items by remember { mutableStateOf(proposedItems) }
    val viewportGeometry = remember { BookDocumentViewportGeometry() }
    val currentOnViewportLocation by rememberUpdatedState(onViewportLocation)
    val currentSection = initialLocation?.section ?: loadedSections[currentChapterId]?.initialSection
    val initialIndex = currentSection?.let { section ->
        items.indexOfPosition(section.key, initialLocation?.position ?: section.initialPosition).coerceAtLeast(0)
    } ?: 0
    val chapterPrefetchStrategy = remember { BookDocumentChapterPrefetchStrategy() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        prefetchStrategy = chapterPrefetchStrategy,
    )
    val currentItems by rememberUpdatedState(items)
    val currentLoadedSections by rememberUpdatedState(loadedSections)
    val currentOnScrollStarted by rememberUpdatedState(onScrollStarted)
    val currentOnUserScrollStarted by rememberUpdatedState(onUserScrollStarted)
    val currentOnAnchorMissing by rememberUpdatedState(onAnchorMissing)
    val currentOnInternalLinkClick by rememberUpdatedState(onInternalLinkClick)
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
        val nextSectionKey = window.next?.id?.let(loadedSections::get)?.sections?.firstOrNull()?.key
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
        { section: BookDocumentSection<EntryChapter>, link: BookDocumentLinkTarget ->
            when (link) {
                is BookDocumentLinkTarget.Anchor,
                is BookDocumentLinkTarget.Resource,
                is BookDocumentLinkTarget.Reference,
                -> currentOnInternalLinkClick(section, link)
                is BookDocumentLinkTarget.External -> currentOnAnchorMissing(link.url)
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
        listState.scrollToBookDocumentPosition(section.document, position, index)
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
            if (initialLocation != null) {
                val index = items.indexOfPosition(initialLocation.section.key, initialLocation.position)
                if (index >= 0) {
                    listState.scrollToItem(index)
                    if ((items[index] as? BookDocumentViewerItem.Block)?.content?.content is
                            mihon.book.api.document.BookDocumentBlockContent.Text
                    ) {
                        val top = snapshotFlow {
                            viewportGeometry.lineTop(initialLocation.section, initialLocation.position)
                        }.filterNotNull().first()
                        listState.scrollBy(top)
                    }
                }
            } else {
                currentSection?.let { scrollToSectionPosition(it) }
            }
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
        val section = loadedSections[currentChapterId]?.initialSection ?: return@LaunchedEffect
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
        val section = loadedSections[request.chapterId]
            ?.sections
            ?.firstOrNull { it.key == request.sectionKey }
            ?: return@LaunchedEffect
        scrollToSectionPosition(section, request.position)
    }

    LaunchedEffect(listState, items) {
        snapshotFlow {
            // Observe scrolling as well as text layout: coordinates alone retain their identity.
            listState.firstVisibleItemIndex
            listState.firstVisibleItemScrollOffset
            if (initialPositionRestored) {
                viewportGeometry.firstLocation(
                    listState.layoutInfo.visibleItemsInfo.mapNotNull { currentItems.resolve(it.index, it.key) },
                )
            } else {
                null
            }
        }.filterNotNull().distinctUntilChanged().collect { currentOnViewportLocation(it) }
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

    CompositionLocalProvider(LocalBookDocumentViewportGeometry provides viewportGeometry) {
        BookDocumentChapterSelectionContainer(
            chapterId = currentChapterId,
            modifier = modifier.onPlaced { viewportGeometry.viewport = it },
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
