package mihon.entry.interactions.book.prose

import android.text.Layout
import android.text.TextPaint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.interactions.viewer.EntryChildDirection
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.math.roundToInt

@Composable
internal fun PaginatedProseViewer(
    state: HtmlProseReaderUiState,
    initialProgression: Float,
    initialDocumentPosition: BookDocumentPosition?,
    palette: ProsePalette,
    fontFamily: String,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    pageMarginsPercent: Int,
    textAlignment: String,
    tapNavigation: Boolean,
    chromeVisible: Boolean,
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val horizontalMargin = 20.dp * pageMarginsPercent / 100
        val verticalMargin = 16.dp * pageMarginsPercent / 100
        val widthPx = paginatedContentExtentPx(maxWidth, horizontalMargin, density)
        val heightPx = paginatedContentExtentPx(maxHeight, verticalMargin, density)
        val paint = remember(fontFamily, fontSizePercent, palette.foreground, density) {
            TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                textSize = with(density) { (16.sp * fontSizePercent / 100).toPx() }
                color = palette.foreground.toReaderArgb()
                typeface = proseTypeface(fontFamily)
            }
        }
        val alignment = textAlignment.toLayoutAlignment()
        val documents = remember(state.loadedChapters) {
            state.loadedChapters.mapValues { (_, chapter) -> chapter.document }
        }
        val paginatedTypefaces by rememberPaginatedProseTypefaces(state.loadedChapters)
        val pages = remember(
            documents,
            paginatedTypefaces,
            widthPx,
            heightPx,
            paint.textSize,
            paint.typeface,
            alignment,
            lineHeightPercent,
        ) {
            state.loadedChapters.mapValues { (_, chapter) ->
                paginateStructuredProse(
                    chapter = chapter,
                    paint = paint,
                    availableWidthPx = widthPx,
                    availableHeightPx = heightPx,
                    alignment = alignment,
                    lineSpacingMultiplier = lineHeightPercent / 100f,
                    justificationMode = if (textAlignment == HtmlProseSettingsProvider.ALIGN_JUSTIFY) {
                        Layout.JUSTIFICATION_MODE_INTER_WORD
                    } else {
                        Layout.JUSTIFICATION_MODE_NONE
                    },
                    resourceTypefaces = paginatedTypefaces[chapter.owner.id].orEmpty(),
                )
            }
        }
        val items = remember(state.window, pages) {
            buildPaginatedItems(state.window, pages)
        }
        if (items.isEmpty()) return@BoxWithConstraints
        val currentPageRanges = pages[state.currentChapterId].orEmpty().map { page ->
            page.index to Triple(
                page.sourceStart,
                page.sourceEndExclusive,
                page.structuredBlock?.block?.id?.value,
            )
        }
        val paginationKey = listOf(
            widthPx,
            heightPx,
            fontFamily,
            fontSizePercent,
            lineHeightPercent,
            pageMarginsPercent,
            textAlignment,
            currentPageRanges,
        )
        val initialSourceOffset = initialDocumentPosition
            ?.takeIf { documents[state.currentChapterId]?.document?.contains(it) == true }
            ?.let { documents[state.currentChapterId]?.document?.logicalOffset(it) }
        val initialPage = initialPaginatedItemIndex(
            items = items,
            chapterId = state.currentChapterId,
            progression = initialProgression,
            sourceOffset = initialSourceOffset,
        )
        val pagerState = key(paginationKey) {
            rememberPagerState(initialPage = initialPage) { items.size }
        }
        val itemKeys = remember(items) { items.map(ProsePagerItem::key) }
        var observedItemKeys by remember(pagerState) { mutableStateOf(itemKeys) }
        var initialPositionPending by remember(pagerState) { mutableStateOf(true) }
        val laidOutPages = remember(pagerState) { mutableStateMapOf<String, Boolean>() }
        val scope = rememberCoroutineScope()
        val handleTapFraction: (Float) -> Unit = { fraction ->
            when {
                chromeVisible || !tapNavigation || fraction in 0.33f..0.66f -> onMenuToggle()
                fraction < 0.33f && pagerState.currentPage > 0 -> scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
                fraction > 0.66f && pagerState.currentPage < items.lastIndex -> scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
        val currentHandleTapFraction = rememberUpdatedState(handleTapFraction)
        val pendingAnchor = remember(pagerState) {
            mutableStateOf(
                initialDocumentPosition
                    ?.takeIf {
                        documents[state.currentChapterId]
                            ?.block(it.blockId)
                            ?.block
                            ?.content
                            ?.let { content -> content is BookDocumentBlockContent.Disclosure } == true
                    }
                    ?.let {
                        PendingBookDocumentAnchor(
                            chapterId = state.currentChapterId,
                            position = it,
                        )
                    },
            )
        }
        LaunchedEffect(pagerState, items, state.currentChapterId) {
            onActions(
                ProseViewerActions(
                    seekPage = { pageIndex ->
                        items.indexOfFirst {
                            it is ProsePagerItem.Page &&
                                it.page.chapter.id == state.currentChapterId &&
                                it.page.index == pageIndex
                        }.takeIf { it >= 0 }?.let { scope.launch { pagerState.scrollToPage(it) } }
                    },
                    seekProgress = { progression ->
                        val chapterPages = items.filterIsInstance<ProsePagerItem.Page>()
                            .filter { it.page.chapter.id == state.currentChapterId }
                        val page = ((chapterPages.size - 1) * progression).roundToInt().coerceAtLeast(0)
                        chapterPages.getOrNull(page)?.let { target ->
                            items.indexOf(target).takeIf { it >= 0 }?.let {
                                scope.launch { pagerState.scrollToPage(it) }
                            }
                        }
                    },
                    previousSection = {
                        items.indexOfFirst {
                            it is ProsePagerItem.Transition &&
                                it.transition.direction == EntryChildDirection.PREVIOUS &&
                                it.transition.from.id == state.currentChapterId
                        }.takeIf { it >= 0 }?.let { scope.launch { pagerState.animateScrollToPage(it) } }
                    },
                    nextSection = {
                        items.indexOfFirst {
                            it is ProsePagerItem.Transition &&
                                it.transition.direction == EntryChildDirection.NEXT &&
                                it.transition.from.id == state.currentChapterId
                        }.takeIf { it >= 0 }?.let { scope.launch { pagerState.animateScrollToPage(it) } }
                    },
                    onTapFraction = { fraction -> currentHandleTapFraction.value(fraction) },
                ),
            )
        }
        SideEffect {
            if (itemKeys != observedItemKeys) {
                if (!pagerState.isScrollInProgress) {
                    prosePagerDatasetAnchor(
                        previousItemKeys = observedItemKeys,
                        items = items,
                        settledPage = pagerState.settledPage,
                    )?.let { newIndex ->
                        pagerState.requestScrollToPage(newIndex)
                    }
                    observedItemKeys = itemKeys
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tapNavigation, chromeVisible, pagerState.currentPage) {
                    detectTapGestures { offset ->
                        handleTapFraction(offset.x / size.width.coerceAtLeast(1))
                    }
                },
            key = { items[it].key },
            beyondViewportPageCount = 1,
        ) { index ->
            PaginatedProseItem(
                index = index,
                item = items[index],
                documents = documents,
                pages = pages,
                items = items,
                state = state,
                palette = palette,
                fontFamily = fontFamily,
                fontSizePercent = fontSizePercent,
                lineHeightPercent = lineHeightPercent,
                textAlignment = textAlignment,
                horizontalMargin = horizontalMargin,
                verticalMargin = verticalMargin,
                initialDocumentPosition = initialDocumentPosition,
                initialPage = initialPage,
                laidOutPages = laidOutPages,
                pagerState = pagerState,
                pendingAnchor = pendingAnchor,
                scope = scope,
                onPosition = onPosition,
                onExternalLinkClick = onExternalLinkClick,
                onTransitionChapterRetry = onTransitionChapterRetry,
            )
        }
        LaunchedEffect(pagerState, items) {
            snapshotFlow {
                if (pagerState.isScrollInProgress) {
                    null
                } else {
                    (items.getOrNull(pagerState.settledPage) as? ProsePagerItem.Transition)
                        ?.transition
                        ?.to
                }
            }
                .filter { it != null }
                .distinctUntilChanged()
                .collect { chapter ->
                    chapter ?: return@collect
                    onTransitionChapterRequested(chapter)
                }
        }
        LaunchedEffect(pagerState, items) {
            snapshotFlow { pagerState.settledPage }
                .map { items.getOrNull(it) }
                .filter { it is ProsePagerItem.Page }
                .distinctUntilChanged()
                .collect { item ->
                    val page = (item as ProsePagerItem.Page).page
                    if (initialPositionPending) {
                        initialPositionPending = false
                        if (
                            pagerState.settledPage == initialPage &&
                            page.chapter.id == state.currentChapterId
                        ) {
                            onPosition(
                                ProseViewerPosition(
                                    chapterId = page.chapter.id,
                                    progression = initialProgression,
                                    currentPage = page.index + 1,
                                    totalPages = page.total,
                                    documentPosition = initialDocumentPosition
                                        ?: documents[page.chapter.id]
                                            ?.document
                                            ?.positionAtProgression(initialProgression),
                                ),
                            )
                            return@collect
                        }
                    }
                    if (page.structuredBlock != null) {
                        if (page.chapter.id != state.currentChapterId) onChapterEntered(page.chapter)
                        return@collect
                    }
                    val documentPosition = documents[page.chapter.id]
                        ?.document
                        ?.positionAtLogicalOffset(
                            if (page.index == page.total - 1) {
                                page.sourceEndExclusive
                            } else {
                                page.sourceStart
                            },
                        )
                    onPosition(
                        ProseViewerPosition(
                            page.chapter.id,
                            page.progression,
                            page.index + 1,
                            page.total,
                            documentPosition,
                        ),
                    )
                    if (page.chapter.id != state.currentChapterId) onChapterEntered(page.chapter)
                }
        }
        LaunchedEffect(
            pagerState,
            items,
            initialPage,
            preparationToken,
            fontFamily,
            fontSizePercent,
            lineHeightPercent,
            pageMarginsPercent,
            textAlignment,
        ) {
            val settledItem = snapshotFlow {
                if (initialPositionPending || pagerState.isScrollInProgress) {
                    return@snapshotFlow null
                }
                val settledPage = pagerState.settledPage
                (items.getOrNull(settledPage) as? ProsePagerItem.Page)
                    ?.takeIf {
                        settledPage == initialPage &&
                            it.page.chapter.id == state.currentChapterId &&
                            laidOutPages[it.key] == true
                    }
            }.filter { it != null }.first() ?: return@LaunchedEffect
            withFrameNanos {}
            if (
                pagerState.settledPage == initialPage &&
                !pagerState.isScrollInProgress &&
                laidOutPages[settledItem.key] == true
            ) {
                onPrepared()
            }
        }
    }
}

internal fun paginatedContentExtentPx(
    containerExtent: Dp,
    edgeMargin: Dp,
    density: Density,
): Int = with(density) {
    (containerExtent.roundToPx() - edgeMargin.roundToPx() * 2).coerceAtLeast(1)
}
