package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.document.reader.BookDocumentChapterEnd
import mihon.entry.interactions.book.document.reader.BookDocumentNavigationRequest
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import tachiyomi.domain.entry.model.EntryChapter

/** Coordinates page turns and semantic restoration independently of page rendering and selection. */
@Composable
internal fun rememberBookDocumentPagedNavigation(
    pages: List<BookDocumentPage>,
    mode: BookDocumentReadingMode,
    initialLocation: BookDocumentViewerLocation<EntryChapter>?,
    navigationRequest: BookDocumentNavigationRequest?,
    animatePages: Boolean,
    volumeKeys: Boolean,
    invertVolumeKeys: Boolean,
    chromeVisible: Boolean,
    chapterEnd: BookDocumentChapterEnd?,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onChapterBoundaryReached: (EntryChapter) -> Unit,
    onChapterEndObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onScrollStarted: () -> Unit,
    onUserScrollStarted: () -> Unit,
    onViewportLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
): BookDocumentPagedNavigation {
    val anchor = remember { PageAnchor(initialLocation) }
    val initialIndex = initialLocation?.let { location ->
        pages.indexOfFirst { it.contains(location.section.key, location.position) }.coerceAtLeast(0)
    } ?: 0
    val pager = rememberPagerState(initialPage = initialIndex) { pages.size }
    val scope = rememberCoroutineScope()
    val rtl = mode == BookDocumentReadingMode.PAGED_RTL
    val currentOnLocation by rememberUpdatedState(onLocation)
    val currentOnViewportLocation by rememberUpdatedState(onViewportLocation)
    val currentOnChapterBoundaryReached by rememberUpdatedState(onChapterBoundaryReached)
    val currentOnChapterEndObservation by rememberUpdatedState(onChapterEndObservation)
    val currentChapterEnd by rememberUpdatedState(chapterEnd)
    val currentOnUserScrollStarted by rememberUpdatedState(onUserScrollStarted)
    val currentOnScrollStarted by rememberUpdatedState(onScrollStarted)
    val currentAnimatePages by rememberUpdatedState(animatePages)
    val focus = remember { FocusRequester() }
    LaunchedEffect(chromeVisible, mode, pager.currentPage) {
        // Selection takes focus inside a page. Return it to the stable pager when that page
        // changes, so subsequent keys still have a target after its selection container is disposed.
        if (!chromeVisible) focus.requestFocus()
    }

    // Chapter completion evidence is stream-relative: the settled page renders the tail of the
    // terminal chapter's final content block. The chapter-transition row and the transient
    // pagination window play no part in it. Pages away from the end still report their owning
    // chapter so a stale candidate resets.
    suspend fun observeChapterEnd(index: Int, page: BookDocumentPage) {
        val chapterEndReached = currentChapterEnd?.let(page::endsChapterContent) == true
        currentOnChapterEndObservation(
            page.ownerChapter,
            chapterEndReached,
            !chapterEndReached && index < pages.lastIndex,
            false,
        )
        if (chapterEndReached) {
            withFrameNanos { }
            currentOnChapterEndObservation(page.ownerChapter, true, false, false)
        }
    }

    fun move(delta: Int) {
        // A keyed page can retain its tap handler while adjacent chapters change the page window.
        if (pager.pageCount == 0) return
        val destination = (pager.currentPage + delta).coerceIn(0, pager.pageCount - 1)
        if (destination == pager.currentPage) return
        currentOnUserScrollStarted()
        currentOnScrollStarted()
        scope.launch {
            if (currentAnimatePages) pager.animateScrollToPage(destination) else pager.scrollToPage(destination)
        }
    }

    LaunchedEffect(pages, navigationRequest) {
        val request = navigationRequest
        val target = if (request != null) {
            pages.indexOfFirst { it.contains(request.sectionKey, request.position) }
        } else {
            val sameTransition = pages.indexOfFirst {
                it.key == anchor.pageKey && it.fragments.first().item is BookDocumentViewerItem.Transition
            }
            when {
                sameTransition >= 0 -> sameTransition
                // A resolved boundary page disappears once its destination resolves. Keep the
                // direction of travel by settling on the destination content the boundary led to,
                // instead of bouncing back to the page the reader came from.
                else -> pages.resolvedBoundaryTargetIndex(anchor.settledTransition)
                    ?: anchor.location?.let { location ->
                        pages.indexOfFirst { it.contains(location.section.key, location.position) }
                    }
                    ?: -1
            }
        }
        val currentPageKey = pager.layoutInfo.visiblePagesInfo.firstOrNull { it.index == pager.currentPage }?.key
        val visiblePage = pages.firstOrNull { it.key == currentPageKey }
        val visiblePageRetained = request == null && visiblePage != null &&
            (
                pager.isScrollInProgress ||
                    anchor.location?.let { visiblePage.contains(it.section.key, it.position) } != false
                )
        // Pager preserves keyed pages when neighbours load. A competing scrollToPage during
        // a user drag is cancelled by the gesture and would also cancel our location observer.
        if (target >= 0 && !visiblePageRetained) pager.scrollToPage(target)
        snapshotFlow {
            val visibleKey = pager.layoutInfo.visiblePagesInfo.firstOrNull { it.index == pager.settledPage }?.key
            visibleKey to pager.isScrollInProgress
        }.collect { (visibleKey, moving) ->
            if (moving) return@collect
            val index = pages.indexOfFirst { it.key == visibleKey }
            val page = pages.getOrNull(index) ?: return@collect
            anchor.pageKey = page.key
            val transition = (page.fragments.first().item as? BookDocumentViewerItem.Transition)?.transition
            anchor.settledTransition = transition
            if (transition != null) {
                transition.to?.let(currentOnChapterBoundaryReached)
            } else {
                val first = page.fragments.first()
                val section = requireNotNull(first.section)
                val requestedPosition = request?.takeIf { page.contains(it.sectionKey, it.position) }?.position
                // Keep the semantic passage through repeated font/viewport changes. Publishing the
                // new page's start on each reflow would gradually walk backwards through the book.
                val retainedPosition = anchor.location?.takeIf {
                    page.contains(it.section.key, it.position)
                }?.position
                val position = requestedPosition ?: retainedPosition ?: requireNotNull(first.position)
                val document = section.document.document
                val last = page.fragments.last()
                val lastBlock = last.item as BookDocumentViewerItem.Block
                val endPosition = mihon.book.api.document.BookDocumentPosition(lastBlock.content.id, last.end)
                val location = BookDocumentViewerLocation(
                    section,
                    position,
                    document.progressionAt(position),
                    document.progressionAt(endPosition),
                    restoredNavigationId = request?.id,
                )
                anchor.location = location
                currentOnLocation(location)
                currentOnViewportLocation(location)
            }
            observeChapterEnd(index, page)
        }
    }
    LaunchedEffect(pager) {
        pager.interactionSource.interactions.filterIsInstance<DragInteraction.Start>().collect {
            currentOnUserScrollStarted()
            currentOnScrollStarted()
        }
    }
    val modifier = Modifier.fillMaxSize().focusRequester(focus).onPreviewKeyEvent { event ->
        val delta = when (event.key) {
            Key.DirectionRight -> if (rtl) -1 else 1
            Key.DirectionLeft -> if (rtl) 1 else -1
            Key.DirectionDown, Key.PageDown -> 1
            Key.DirectionUp, Key.PageUp -> -1
            Key.VolumeDown -> if (volumeKeys) (if (invertVolumeKeys) -1 else 1) else 0
            Key.VolumeUp -> if (volumeKeys) (if (invertVolumeKeys) 1 else -1) else 0
            else -> 0
        }
        if (chromeVisible || delta == 0) {
            false
        } else {
            if (event.type == KeyEventType.KeyUp) move(delta)
            true
        }
    }.focusable()
    return BookDocumentPagedNavigation(pager, modifier, ::move)
}

internal class BookDocumentPagedNavigation(
    val pager: PagerState,
    val modifier: Modifier,
    val move: (Int) -> Unit,
)

/**
 * The page a vanished chapter boundary resolves into: the destination content it was leading to,
 * taken from the reader's direction of travel. The first page of the chapter a NEXT boundary led
 * to, or the last page of the chapter a PREVIOUS boundary led back to. Null when the destination
 * content is not part of the pages, so the caller falls back to the last content location.
 */
internal fun List<BookDocumentPage>.resolvedBoundaryTargetIndex(
    transition: EntryChildTransition<EntryChapter>?,
): Int? {
    val destinationId = transition?.to?.id ?: return null
    return when (transition.direction) {
        EntryChildDirection.NEXT -> indexOfFirst { it.fragments.first().section?.owner?.id == destinationId }
        EntryChildDirection.PREVIOUS -> indexOfLast { it.fragments.first().section?.owner?.id == destinationId }
    }.takeIf { it >= 0 }
}

private class PageAnchor(
    var location: BookDocumentViewerLocation<EntryChapter>? = null,
    var pageKey: String? = null,
    var settledTransition: EntryChildTransition<EntryChapter>? = null,
)
