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
import mihon.entry.interactions.book.document.reader.BookDocumentNavigationRequest
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildDirection
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
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
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
    val currentOnTransitionReached by rememberUpdatedState(onTransitionReached)
    val currentOnTerminalObservation by rememberUpdatedState(onTerminalObservation)
    val currentOnUserScrollStarted by rememberUpdatedState(onUserScrollStarted)
    val currentOnScrollStarted by rememberUpdatedState(onScrollStarted)
    val focus = remember { FocusRequester() }
    LaunchedEffect(chromeVisible, mode) { if (!chromeVisible) focus.requestFocus() }

    fun move(delta: Int) {
        val destination = (pager.currentPage + delta).coerceIn(0, pages.lastIndex)
        if (destination == pager.currentPage) return
        onUserScrollStarted()
        onScrollStarted()
        scope.launch {
            if (animatePages) pager.animateScrollToPage(destination) else pager.scrollToPage(destination)
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
            if (sameTransition >= 0) {
                sameTransition
            } else {
                anchor.location?.let { location ->
                    pages.indexOfFirst { it.contains(location.section.key, location.position) }
                } ?: -1
            }
        }
        if (target >= 0) pager.scrollToPage(target)
        snapshotFlow { pager.settledPage to pager.isScrollInProgress }.collect { (index, moving) ->
            if (moving) return@collect
            val page = pages.getOrNull(index) ?: return@collect
            anchor.pageKey = page.key
            val transition = (page.fragments.first().item as? BookDocumentViewerItem.Transition)?.transition
            if (transition != null) {
                transition.to?.let(currentOnTransitionReached)
                val terminal = transition.direction == EntryChildDirection.NEXT && transition.to == null
                currentOnTerminalObservation(transition.from, terminal, index < pages.lastIndex, false)
                if (terminal) {
                    withFrameNanos { }
                    currentOnTerminalObservation(transition.from, true, false, false)
                }
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
                )
                anchor.location = location
                currentOnLocation(location)
                val topPosition = requireNotNull(first.position)
                currentOnViewportLocation(
                    BookDocumentViewerLocation(section, topPosition, document.progressionAt(topPosition)),
                )
                currentOnTerminalObservation(section.owner, false, index < pages.lastIndex, false)
            }
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

private class PageAnchor(var location: BookDocumentViewerLocation<EntryChapter>?, var pageKey: String? = null)
