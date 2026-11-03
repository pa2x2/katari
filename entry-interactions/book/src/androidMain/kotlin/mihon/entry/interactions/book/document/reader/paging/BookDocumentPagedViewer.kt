package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.BookDocumentChapterEnd
import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.book.document.reader.BookDocumentChapterSelectionContainer
import mihon.entry.interactions.book.document.reader.BookDocumentNavigationRequest
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.reader.BookReaderProgress
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.viewer.chrome.ui.navigation.ReaderTapAction
import mihon.entry.viewer.chrome.ui.navigation.readerTapAction
import mihon.presentation.kit.layout.PageSpreadScope
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Composes page content, selection and gestures using the paged navigation owner. With [spread], pages that are spreads
 * lay their two pages out on either side of it; a page alone in a spread takes the first side, and a chapter transition
 * the middle.
 */
@Composable
internal fun BookDocumentPagedViewer(
    pages: List<BookDocumentPage>,
    spread: PageSpreadScope?,
    mode: BookDocumentReadingMode,
    initialLocation: BookDocumentViewerLocation<EntryChapter>?,
    navigationRequest: BookDocumentNavigationRequest?,
    loadStates: Map<Long, BookDocumentChapterLoadState>,
    tapZones: Int,
    tapInversion: Int,
    animatePages: Boolean,
    volumeKeys: Boolean,
    invertVolumeKeys: Boolean,
    chromeVisible: Boolean,
    chapterEnd: BookDocumentChapterEnd?,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onChapterBoundaryReached: (EntryChapter) -> Unit,
    onTransitionRetry: (EntryChapter) -> Unit,
    onChapterEndObservation: (EntryChapter, Boolean, Boolean) -> Unit,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onScrollStarted: () -> Unit,
    onUserScrollStarted: () -> Unit,
    onReaderTap: () -> Unit,
    onViewportLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit = {},
    onPageProgress: (BookReaderProgress.Page?) -> Unit = {},
) {
    val navigation = rememberBookDocumentPagedNavigation(
        pages, mode, initialLocation, navigationRequest,
        animatePages, volumeKeys, invertVolumeKeys, chromeVisible,
        chapterEnd,
        onLocation, onChapterBoundaryReached, onChapterEndObservation, onScrollStarted, onUserScrollStarted,
        onViewportLocation,
    )
    val pager = navigation.pager
    val vertical = mode == BookDocumentReadingMode.PAGED_VERTICAL
    val rtl = mode == BookDocumentReadingMode.PAGED_RTL
    BookDocumentPageProgressEffect(pages, pager, rtl, onPageProgress)
    val modifier = navigation.modifier
    val move = navigation.move
    val pageContent: @Composable (Int) -> Unit = { index ->
        val page = pages[index]
        val chapterId = page.ownerChapter.id
        BookDocumentChapterSelectionContainer(
            chapterId = chapterId,
            ownerIdentity = "book-page:${page.key}",
            modifier = Modifier.fillMaxSize(),
        ) { selection ->
            LaunchedEffect(pager.currentPage) {
                if (pager.currentPage != index) selection.clearSelection()
            }
            val content: @Composable (BookDocumentPage) -> Unit = {
                BookDocumentPageContent(it, loadStates, onAnchorClick, onExternalLinkClick, onTransitionRetry)
            }
            Box(
                modifier = Modifier.fillMaxSize().pointerInput(
                    page.key,
                    tapZones,
                    tapInversion,
                    vertical,
                    rtl,
                    animatePages,
                ) {
                    detectTapGestures { offset ->
                        if (!selection.consumeSelectionTap()) {
                            when (
                                readerTapAction(
                                    tapZones,
                                    vertical,
                                    tapInversion,
                                    offset.x / size.width,
                                    offset.y / size.height,
                                )
                            ) {
                                ReaderTapAction.MENU -> onReaderTap()
                                ReaderTapAction.NEXT -> move(1)
                                ReaderTapAction.PREV -> move(-1)
                                ReaderTapAction.LEFT -> move(if (rtl) 1 else -1)
                                ReaderTapAction.RIGHT -> move(if (rtl) -1 else 1)
                            }
                        }
                    }
                },
            ) {
                val sides = page.singlePages
                val isContent = page.fragments.first().item is BookDocumentViewerItem.Block
                if (spread == null || !isContent) {
                    content(page)
                } else {
                    val first: @Composable BoxScope.() -> Unit = { content(sides.first()) }
                    val second: @Composable BoxScope.() -> Unit = { sides.getOrNull(1)?.let { content(it) } }
                    spread.Pages(
                        left = if (rtl) second else first,
                        right = if (rtl) first else second,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
    if (vertical) {
        VerticalPager(state = pager, modifier = modifier, key = { pages[it].key }) { pageContent(it) }
    } else {
        HorizontalPager(state = pager, modifier = modifier, reverseLayout = rtl, key = {
            pages[it].key
        }) { pageContent(it) }
    }
}
