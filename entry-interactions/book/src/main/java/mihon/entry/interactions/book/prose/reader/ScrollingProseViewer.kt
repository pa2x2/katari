package mihon.entry.interactions.book.prose

import android.text.Layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.BookDocumentVisibleItemLayout
import mihon.entry.interactions.book.document.reader.LocalBookDocumentSectionKey
import mihon.entry.interactions.book.document.reader.bookDocumentScrollOffset
import mihon.entry.interactions.book.document.reader.bookDocumentViewerDatasetAnchor
import mihon.entry.interactions.book.document.reader.bookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.bookDocumentViewerTransitionAtAnchor
import mihon.entry.interactions.book.document.reader.buildBookDocumentViewerItems
import mihon.entry.interactions.book.document.reader.indexOfPosition
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.interactions.viewer.EntryChildDirection
import tachiyomi.domain.entry.model.EntryChapter

@Composable
internal fun ScrollingProseViewer(
    state: HtmlProseReaderUiState,
    initialProgression: Float,
    initialDocumentPosition: BookDocumentPosition?,
    palette: ProsePalette,
    fontFamily: String,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    pageMarginsPercent: Int,
    textAlignment: String,
    preparationToken: Any,
    onPosition: (ProseViewerPosition) -> Unit,
    onChapterEntered: (EntryChapter) -> Unit,
    onTransitionChapterRequested: (EntryChapter) -> Unit,
    onTransitionChapterRetry: (EntryChapter) -> Unit,
    onMenuToggle: () -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onActions: (ProseViewerActions) -> Unit,
    onPrepared: () -> Unit,
) {
    val items = remember(state.window, state.loadedChapters) {
        buildBookDocumentViewerItems(
            window = state.window,
            loaded = state.loadedChapters,
            keyOf = EntryChapter::id,
        )
    }
    val currentSection = state.loadedChapters[state.currentChapterId]
    val initialPosition = initialDocumentPosition
        ?.takeIf { currentSection?.document?.document?.contains(it) == true }
        ?: currentSection?.document?.document?.positionAtProgression(initialProgression)
        ?: currentSection?.initialPosition
    val initialIndex = initialPosition?.let {
        items.indexOfPosition(state.currentChapterId.toString(), it)
    }?.coerceAtLeast(0) ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    var initialPositionRestored by remember(listState) { mutableStateOf(false) }
    var pendingWindowAnchor by remember(listState) {
        mutableStateOf<ProseViewerWindowAnchor?>(null)
    }
    val itemKeys = remember(items) { items.map(BookDocumentViewerItem<EntryChapter>::key) }
    var observedItemKeys by remember(listState) { mutableStateOf(itemKeys) }
    val scope = rememberCoroutineScope()
    var pendingAnchor by remember(listState) {
        mutableStateOf(
            initialPosition
                ?.takeIf {
                    currentSection
                        ?.document
                        ?.block(it.blockId)
                        ?.block
                        ?.content is BookDocumentBlockContent.Disclosure
                }
                ?.let {
                    PendingBookDocumentAnchor(
                        chapterId = state.currentChapterId,
                        position = it,
                    )
                },
        )
    }
    var listCoordinates by remember(listState) {
        mutableStateOf<LayoutCoordinates?>(null)
    }
    val windowAnchor = pendingWindowAnchor?.takeIf {
        it.destinationChapterId == state.currentChapterId
    }
    val windowAnchorIndex = windowAnchor?.let { anchor ->
        items.indexOfFirst { it.key == anchor.itemKey }
    } ?: -1
    SideEffect {
        val itemSetChanged = itemKeys != observedItemKeys
        when {
            // Entering an adjacent chapter rotates the previous/current/next window. If the
            // outgoing previous chapter has a different block count, retaining its numeric index
            // can clamp the list into the following chapter.
            windowAnchor != null -> {
                if (windowAnchorIndex >= 0 && !listState.isScrollInProgress) {
                    listState.requestScrollToItem(windowAnchorIndex, windowAnchor.scrollOffset)
                }
                pendingWindowAnchor = null
            }
            // Loading a reached transition inserts a document on either side of a surviving
            // stable transition key. Preserve that key's pixel offset without cancelling motion.
            itemSetChanged && !listState.isScrollInProgress -> {
                val layoutInfo = listState.layoutInfo
                val anchor = bookDocumentViewerDatasetAnchor(
                    items = items,
                    visibleItems = layoutInfo.visibleItemsInfo.map {
                        BookDocumentVisibleItemLayout(
                            index = it.index,
                            key = it.key,
                            offset = it.offset,
                            size = it.size,
                        )
                    },
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                )
                if (anchor != null) {
                    listState.requestScrollToItem(anchor.index, anchor.scrollOffset)
                }
            }
        }
        observedItemKeys = itemKeys
    }

    suspend fun scrollToPosition(
        sectionKey: String,
        position: BookDocumentPosition,
    ) {
        val index = items.indexOfPosition(sectionKey, position)
        if (index < 0) return
        val item = items[index] as BookDocumentViewerItem.Block
        listState.scrollToItem(index)
        val layout = snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            item?.let {
                ProseViewerRestorationLayout(
                    itemSize = it.size,
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                )
            }
        }.filter { it != null }.first() ?: return
        listState.scrollToItem(
            index,
            bookDocumentScrollOffset(
                document = item.section.document,
                position = position,
                itemSize = layout.itemSize,
                viewportStartOffset = layout.viewportStartOffset,
                viewportEndOffset = layout.viewportEndOffset,
            ),
        )
    }

    LaunchedEffect(listState, items, state.currentChapterId, initialPosition, preparationToken) {
        if (!initialPositionRestored) {
            initialPosition?.let {
                scrollToPosition(state.currentChapterId.toString(), it)
            }
            initialPositionRestored = true
        }
        withFrameNanos {}
        onPrepared()
    }
    val currentOnMenuToggle = rememberUpdatedState(onMenuToggle)
    LaunchedEffect(listState, items, state.currentChapterId) {
        onActions(
            ProseViewerActions(
                seekProgress = seekProgress@{ progression ->
                    val section = state.loadedChapters[state.currentChapterId] ?: return@seekProgress
                    scope.launch {
                        scrollToPosition(
                            section.key,
                            section.document.document.positionAtProgression(progression),
                        )
                    }
                },
                previousSection = {
                    items.indexOfFirst { item ->
                        item is BookDocumentViewerItem.Transition &&
                            item.transition.direction == EntryChildDirection.PREVIOUS &&
                            item.transition.from.id == state.currentChapterId
                    }.takeIf { it >= 0 }?.let { scope.launch { listState.animateScrollToItem(it) } }
                },
                nextSection = {
                    items.indexOfFirst { item ->
                        item is BookDocumentViewerItem.Transition &&
                            item.transition.direction == EntryChildDirection.NEXT &&
                            item.transition.from.id == state.currentChapterId
                    }.takeIf { it >= 0 }?.let { scope.launch { listState.animateScrollToItem(it) } }
                },
                onTapFraction = { currentOnMenuToggle.value() },
            ),
        )
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { listCoordinates = it },
    ) {
        items(items, key = { it.key }) { item ->
            when (item) {
                is BookDocumentViewerItem.Block -> {
                    val block = item.content.block
                    val sectionBlocks = item.section.document.document.blocks
                    val topPadding = if (block.id == sectionBlocks.first().id) 16.dp else 0.dp
                    val bottomPadding = if (block.id == sectionBlocks.last().id) 16.dp else 0.dp
                    val requestedAnchor = pendingAnchor?.takeIf {
                        it.chapterId == item.section.owner.id &&
                            it.position.blockId == block.id
                    }
                    CompositionLocalProvider(
                        LocalBookDocumentSectionKey provides item.section.key,
                    ) {
                        ProseDocumentBlock(
                            content = item.content,
                            resourceLoader = item.section.resourceLoader,
                            readerForeground = palette.foreground,
                            readerBackground = palette.background,
                            readerTextSizeSp = 16f * fontSizePercent / 100f,
                            readerTypeface = proseTypeface(fontFamily),
                            lineSpacingMultiplier = lineHeightPercent / 100f,
                            readerTextAlignment = textAlignment.toTextViewAlignment(),
                            justificationMode = if (
                                textAlignment == HtmlProseSettingsProvider.ALIGN_JUSTIFY
                            ) {
                                Layout.JUSTIFICATION_MODE_INTER_WORD
                            } else {
                                Layout.JUSTIFICATION_MODE_NONE
                            },
                            trimTerminalLine = block.id != sectionBlocks.last().id,
                            onAnchorClick = { anchorId, _ ->
                                val target = item.section.document.document.anchors[anchorId]
                                    ?: return@ProseDocumentBlock
                                val targetIndex = items.indexOfPosition(item.section.key, target)
                                if (targetIndex < 0) return@ProseDocumentBlock
                                pendingAnchor = PendingBookDocumentAnchor(
                                    chapterId = item.section.owner.id,
                                    position = target,
                                )
                                scope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            },
                            onExternalLinkClick = onExternalLinkClick,
                            onViewChanged = {},
                            anchorOffsetWithinBlock = requestedAnchor?.position?.offsetWithinBlock,
                            onAnchorTargetPositioned = { coordinates, offsetPx ->
                                val list = listCoordinates?.takeIf { it.isAttached }
                                    ?: return@ProseDocumentBlock
                                if (!coordinates.isAttached) return@ProseDocumentBlock
                                val delta = (
                                    coordinates.positionInWindow().y -
                                        list.positionInWindow().y +
                                        offsetPx
                                    )
                                pendingAnchor = null
                                scope.launch { listState.animateScrollBy(delta) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    onClick = onMenuToggle,
                                )
                                .padding(
                                    start = 20.dp * pageMarginsPercent / 100,
                                    top = topPadding * pageMarginsPercent / 100,
                                    end = 20.dp * pageMarginsPercent / 100,
                                    bottom = bottomPadding * pageMarginsPercent / 100,
                                ),
                        )
                    }
                }
                is BookDocumentViewerItem.Transition -> ProseTransition(
                    transition = item.transition,
                    loadState = item.transition.to
                        ?.let { state.transitionLoadStates[it.id] }
                        .toSharedLoadState(),
                    onRetry = item.transition.to?.let { chapter ->
                        { onTransitionChapterRetry(chapter) }
                    },
                    palette = palette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onMenuToggle,
                        )
                        .padding(28.dp),
                )
            }
        }
    }
    LaunchedEffect(listState, items, initialPositionRestored) {
        if (!initialPositionRestored) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo }
            .map { info ->
                bookDocumentViewerTransitionAtAnchor(
                    items = items,
                    visibleItems = info.visibleItemsInfo.map {
                        BookDocumentVisibleItemLayout(
                            index = it.index,
                            key = it.key,
                            offset = it.offset,
                            size = it.size,
                        )
                    },
                    viewportStartOffset = info.viewportStartOffset,
                    viewportEndOffset = info.viewportEndOffset,
                    canScrollBackward = listState.canScrollBackward,
                    canScrollForward = listState.canScrollForward,
                )?.to
            }
            .filter { it != null }
            .distinctUntilChanged()
            .collect { chapter ->
                chapter ?: return@collect
                onTransitionChapterRequested(chapter)
            }
    }
    LaunchedEffect(listState, items, initialPositionRestored, windowAnchor) {
        if (!initialPositionRestored || windowAnchor != null) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo }
            .map { info ->
                // LazyListItemInfo offsets are mutable and the instances are reused between scroll frames.
                // Snapshot their values before distinctUntilChanged so live offset changes are not suppressed.
                bookDocumentViewerLocation(
                    items = items,
                    visibleItems = info.visibleItemsInfo.map {
                        BookDocumentVisibleItemLayout(
                            index = it.index,
                            key = it.key,
                            offset = it.offset,
                            size = it.size,
                        )
                    },
                    viewportStartOffset = info.viewportStartOffset,
                    viewportEndOffset = info.viewportEndOffset,
                )
            }
            .filter { it != null }
            .distinctUntilChanged()
            .collect { location ->
                location ?: return@collect
                onPosition(
                    ProseViewerPosition(
                        chapterId = location.section.owner.id,
                        progression = location.progression,
                        currentPage = 1,
                        totalPages = 1,
                        documentPosition = location.position,
                    ),
                )
                if (location.section.owner.id != state.currentChapterId) {
                    val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                    val itemKey = firstVisible?.key as? String
                    if (itemKey != null) {
                        pendingWindowAnchor = ProseViewerWindowAnchor(
                            destinationChapterId = location.section.owner.id,
                            itemKey = itemKey,
                            scrollOffset = listState.firstVisibleItemScrollOffset,
                        )
                    }
                    onChapterEntered(location.section.owner)
                }
            }
    }
}
