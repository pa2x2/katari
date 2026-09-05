package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.paging.BookDocumentPagedViewer
import mihon.entry.interactions.book.document.reader.paging.BookDocumentPaginationLayout
import mihon.entry.interactions.book.document.reader.paging.paginationGroup
import mihon.entry.interactions.book.document.reader.paging.paginationWindow
import mihon.entry.interactions.book.document.reader.table.BookDocumentTablePreparation
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter

/** Switches rendering modes around the first visible line, independently of reading-progress observations. */
@Composable
internal fun BookDocumentModeViewport(
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
    mode: BookDocumentReadingMode,
    tapZones: Int,
    inversion: Int,
    animation: Boolean,
    volume: Boolean,
    invertVolume: Boolean,
    chromeVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val anchor = rememberSaveable(saver = BookDocumentViewportAnchor.Saver) { BookDocumentViewportAnchor() }
    anchor.resolve(loadedSections)
    if (mode == BookDocumentReadingMode.SCROLL) {
        BookDocumentEndlessViewer(
            currentChapter, currentChapterId, window, loadedSections, loadStates, navigationRequest, textSizePercent,
            onLocation, onTransitionReached, onTerminalObservation, onAnchorMissing, onInternalLinkClick,
            onExternalLinkClick, onScrollStarted, onUserScrollStarted, onReaderTap,
            initialLocation = anchor.location,
            onViewportLocation = { anchor.location = it },
            modifier = modifier,
        )
    } else {
        val items = remember(window, loadedSections) {
            buildBookDocumentPublicationViewerItems(window, loadedSections, EntryChapter::id)
        }
        val initialLocation = anchor.location ?: loadedSections[currentChapterId]?.initialSection?.let { section ->
            BookDocumentViewerLocation(
                section,
                section.initialPosition,
                section.document.document.progressionAt(section.initialPosition),
            )
        }
        // Keep a page boundary at the handoff line for this paged session. Chapter-aligned pages
        // alone can otherwise move the top of the viewport by almost a full screen on each switch.
        val pageBreak = remember { initialLocation }
        var centerGroup by remember { mutableStateOf<String?>(null) }
        val center = anchor.location ?: initialLocation
        val centerIndex = center?.let { items.indexOfPosition(it.section.key, it.position) }?.coerceAtLeast(0) ?: 0
        val visibleItems = remember(items, centerGroup) { items.paginationWindow(centerIndex) }
        val pagedLocationChanged = { location: BookDocumentViewerLocation<EntryChapter> ->
            onLocation(location)
            val index = items.indexOfPosition(location.section.key, location.position)
            if (index >= 0) centerGroup = items[index].paginationGroup()
        }
        val requestedIndex = navigationRequest?.let { items.indexOfPosition(it.sectionKey, it.position) }
        val pageItems = if (requestedIndex != null && requestedIndex >= 0) {
            remember(items, requestedIndex) { items.paginationWindow(requestedIndex) }
        } else {
            visibleItems
        }
        BookDocumentTablePreparation(
            pageItems.mapNotNull {
                (it as? BookDocumentViewerItem.Block)?.section
            }.distinct(),
            modifier,
        ) {
            BookDocumentPaginationLayout(pageItems, pageBreak = pageBreak) { pages ->
                BookDocumentPagedViewer(
                    pages, mode, initialLocation, navigationRequest, loadStates,
                    tapZones, inversion, animation,
                    volume, invertVolume, chromeVisible,
                    pagedLocationChanged, onTransitionReached, onTerminalObservation, onInternalLinkClick,
                    onExternalLinkClick, onScrollStarted, onUserScrollStarted, onReaderTap,
                    onViewportLocation = { anchor.location = it },
                )
            }
        }
    }
}
