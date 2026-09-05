package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.book.document.reader.BookDocumentChapterSelectionContainer
import mihon.entry.interactions.book.document.reader.BookDocumentNavigationRequest
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.presentation.core.components.reader.navigation.ReaderTapAction
import tachiyomi.presentation.core.components.reader.navigation.readerTapAction

/** Composes page content, selection and gestures using the paged navigation owner. */
@Composable
internal fun BookDocumentPagedViewer(
    pages: List<BookDocumentPage>,
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
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onAnchorClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onScrollStarted: () -> Unit,
    onUserScrollStarted: () -> Unit,
    onReaderTap: () -> Unit,
    onViewportLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit = {},
) {
    val navigation = rememberBookDocumentPagedNavigation(
        pages, mode, initialLocation, navigationRequest,
        animatePages, volumeKeys, invertVolumeKeys, chromeVisible,
        onLocation, onTransitionReached, onTerminalObservation, onScrollStarted, onUserScrollStarted,
        onViewportLocation,
    )
    val pager = navigation.pager
    val vertical = mode == BookDocumentReadingMode.PAGED_VERTICAL
    val rtl = mode == BookDocumentReadingMode.PAGED_RTL
    val modifier = navigation.modifier
    val move = navigation.move
    val pageContent: @Composable (Int) -> Unit = { index ->
        val page = pages[index]
        val chapterId = page.fragments.first().section?.owner?.id
            ?: (page.fragments.first().item as BookDocumentViewerItem.Transition).transition.from.id
        BookDocumentChapterSelectionContainer(
            chapterId = chapterId,
            ownerIdentity = "book-page:${page.key}",
            modifier = Modifier.fillMaxSize(),
        ) { selection ->
            LaunchedEffect(pager.currentPage) {
                if (pager.currentPage != index) selection.clearSelection()
            }
            BookDocumentPageContent(
                page,
                loadStates,
                onAnchorClick,
                onExternalLinkClick,
                onReaderTap,
                onTransitionReached,
                modifier = Modifier.pointerInput(page.key, tapZones, tapInversion, vertical, rtl, animatePages) {
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
            )
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
