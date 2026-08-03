package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import mihon.entry.interactions.viewer.EntryChildDirection
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.presentation.core.util.clickableNoIndication

/** Stable-key, adjacent-session vertical document stream. */
@Composable
internal fun BookDocumentEndlessViewer(
    state: BookDocumentReaderState,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onAnchorMissing: (String) -> Unit = {},
    onExternalLinkClick: (String) -> Unit,
    onScrollStarted: () -> Unit,
    onReaderTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(state.window, state.loadedSections) {
        buildBookDocumentViewerItems(state.window, state.loadedSections, EntryChapter::id)
    }
    val currentSection = state.loadedSections[state.currentChapterId]
    val initialIndex = currentSection?.let { section ->
        items.indexOfPosition(section.key, section.initialPosition).coerceAtLeast(0)
    } ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val currentItems by rememberUpdatedState(items)
    val currentOnScrollStarted by rememberUpdatedState(onScrollStarted)
    var observedKeys by remember(listState) { mutableStateOf(items.map { it.key }) }
    var initialPositionRestored by remember(listState) { mutableStateOf(false) }

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
        val keys = items.map { it.key }
        if (keys != observedKeys && !listState.isScrollInProgress && state.navigationRequest == null) {
            val info = listState.layoutInfo
            bookDocumentViewerDatasetAnchor(
                items = items,
                visibleItems = info.visibleBookDocumentLayouts(),
                viewportStartOffset = info.viewportStartOffset,
            )?.let { anchor -> listState.requestScrollToItem(anchor.index, anchor.scrollOffset) }
        }
        observedKeys = keys
    }

    LaunchedEffect(items) {
        if (!initialPositionRestored) {
            currentSection?.let { scrollToSectionPosition(it) }
            initialPositionRestored = true
        }
    }

    LaunchedEffect(state.currentChapterId, items, initialPositionRestored) {
        if (!initialPositionRestored) return@LaunchedEffect
        if (state.navigationRequest != null) return@LaunchedEffect
        val section = state.loadedSections[state.currentChapterId] ?: return@LaunchedEffect
        val visibleChapterIds = listState.layoutInfo.visibleItemsInfo.mapNotNull { layout ->
            (items.resolve(layout.index, layout.key) as? BookDocumentViewerItem.Block)?.section?.owner?.id
        }
        if (state.currentChapterId !in visibleChapterIds) {
            val index = items.indexOfPosition(section.key, section.initialPosition)
            if (index >= 0) scrollToSectionPosition(section)
        }
    }

    LaunchedEffect(state.navigationRequest) {
        val request = state.navigationRequest ?: return@LaunchedEffect
        val section = state.loadedSections[request.chapterId] ?: return@LaunchedEffect
        scrollToSectionPosition(section, request.position)
    }

    LaunchedEffect(listState, items) {
        snapshotFlow {
            val info = listState.layoutInfo
            bookDocumentViewerLocation(
                currentItems,
                info.visibleBookDocumentLayouts(),
                info.viewportStartOffset,
                info.viewportEndOffset,
            )
        }.filterNotNull().distinctUntilChanged().collect(onLocation)
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnScrollStarted() }
    }
    LaunchedEffect(listState, items) {
        snapshotFlow {
            val info = listState.layoutInfo
            bookDocumentViewerTransitionAtAnchor(
                currentItems,
                info.visibleBookDocumentLayouts(),
                info.viewportStartOffset,
                info.viewportEndOffset,
                listState.canScrollBackward,
                listState.canScrollForward,
            )?.to
        }.filterNotNull().distinctUntilChanged().collect(onTransitionReached)
    }
    LaunchedEffect(listState, items, state.currentChapterId) {
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
                state.currentChapterId,
                terminalVisible,
                listState.canScrollForward,
                listState.isScrollInProgress,
            )
        }.distinctUntilChanged().collect { observation ->
            val chapter = state.chapters.firstOrNull { it.id == observation.chapterId } ?: return@collect
            onTerminalObservation(
                chapter,
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
                    onTerminalObservation(chapter, true, false, false)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.clickableNoIndication(onClick = onReaderTap),
    ) {
        items(items, key = { it.key }) { item ->
            when (item) {
                is BookDocumentViewerItem.Block -> CompositionLocalProvider(
                    LocalBookDocumentSectionKey provides item.section.key,
                ) {
                    BookDocumentBlockRenderer(
                        block = item.content,
                        owningContent = item.section.document.document.content,
                        sectionKey = item.section.key,
                        resourceLoader = item.section.resourceLoader,
                        onAnchorClick = { fragment ->
                            val target = item.section.document.document.anchors[fragment]
                            if (target == null) {
                                onAnchorMissing(fragment)
                            } else {
                                val index = currentItems.indexOfPosition(item.section.key, target)
                                if (index >= 0) listState.requestScrollToItem(index)
                            }
                        },
                        onExternalLinkClick = onExternalLinkClick,
                        onReaderTap = onReaderTap,
                        preserveTerminalSpacing = item.content.id != item.section.document.blocks.last().id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
                is BookDocumentViewerItem.Transition -> BookDocumentChapterTransition(
                    transition = item.transition,
                    loadState = item.transition.to?.let { state.loadStates[it.id] },
                    onRetry = item.transition.to?.let { chapter -> { onTransitionReached(chapter) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                )
            }
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
