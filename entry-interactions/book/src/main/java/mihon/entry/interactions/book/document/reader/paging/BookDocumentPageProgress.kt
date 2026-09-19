package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import mihon.entry.interactions.book.reader.BookReaderProgress

/**
 * Counts content pages in the section the settled page reports progress for. Page ownership comes
 * from [BookDocumentPage.ownerChapter], so a transition page inherits the position it leads from
 * without the progress reporter inspecting the transition itself.
 */
internal fun List<BookDocumentPage>.pageProgress(index: Int): BookReaderProgress.Page? {
    val page = getOrNull(index) ?: return null
    val owner = page.ownerChapter
    val contentIndex = page.fragments.first().section?.let { index }
        ?: nearestContentPageIndex(index, owner.id)
        ?: return null
    val section = get(contentIndex).fragments.first().section ?: return null
    val indices = indices.filter { get(it).fragments.first().section?.key == section.key }
    return BookReaderProgress.Page(indices.indexOf(contentIndex) + 1, indices.size)
}

private fun List<BookDocumentPage>.nearestContentPageIndex(index: Int, ownerId: Long): Int? {
    val maxDistance = maxOf(index, lastIndex - index)
    for (distance in 1..maxDistance) {
        (index - distance).takeIf { it >= 0 }?.let { previous ->
            if (get(previous).fragments.first().section?.owner?.id == ownerId) return previous
        }
        (index + distance).takeIf { it <= lastIndex }?.let { next ->
            if (get(next).fragments.first().section?.owner?.id == ownerId) return next
        }
    }
    return null
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
