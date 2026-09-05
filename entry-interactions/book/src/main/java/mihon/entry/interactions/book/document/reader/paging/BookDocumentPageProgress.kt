package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.reader.BookReaderProgress
import mihon.entry.interactions.viewer.EntryChildDirection

/** Counts content pages in the current document section, independently of prefetched neighbours. */
internal fun List<BookDocumentPage>.pageProgress(index: Int): BookReaderProgress.Page? {
    val page = getOrNull(index) ?: return null
    val transition = (page.fragments.first().item as? BookDocumentViewerItem.Transition)?.transition
    val contentIndex = if (transition == null) {
        index
    } else if (transition.direction == EntryChildDirection.NEXT) {
        (index - 1 downTo 0).firstOrNull { get(it).fragments.first().section?.owner?.id == transition.from.id }
    } else {
        (index + 1..lastIndex).firstOrNull { get(it).fragments.first().section?.owner?.id == transition.from.id }
    } ?: return null
    val section = get(contentIndex).fragments.first().section ?: return null
    val indices = indices.filter { get(it).fragments.first().section?.key == section.key }
    return BookReaderProgress.Page(indices.indexOf(contentIndex) + 1, indices.size)
}

/** Publishes the live displayed page while logical resume observations wait for settlement. */
@Composable
internal fun BookDocumentPageProgressEffect(
    pages: List<BookDocumentPage>,
    pager: PagerState,
    onProgress: (BookReaderProgress.Page?) -> Unit,
) {
    val currentOnPageProgress by rememberUpdatedState(onProgress)
    LaunchedEffect(pages, pager) {
        snapshotFlow {
            // The current page follows the gesture before settlement. Use the measured key:
            // chapter-window rebasing can change indices while old layout data is still visible.
            pager.layoutInfo.visiblePagesInfo.firstOrNull { it.index == pager.currentPage }?.key
        }.collect { visibleKey ->
            val index = pages.indexOfFirst { it.key == visibleKey }
            if (index >= 0) currentOnPageProgress(pages.pageProgress(index))
        }
    }
}
