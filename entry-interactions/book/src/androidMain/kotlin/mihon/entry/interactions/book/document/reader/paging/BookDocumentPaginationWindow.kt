package mihon.entry.interactions.book.document.reader.paging

import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.book.document.reader.BookDocumentViewerDataset
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.transition.boundaryChapterGap
import mihon.entry.interactions.book.document.reader.transition.isSeamlessWhenNeededBoundary
import mihon.entry.interactions.book.document.reader.transition.rendersCompactHiddenBoundary
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import tachiyomi.domain.entry.model.EntryChapter

/** Natural document sections bound measurement without inventing page breaks between arbitrary blocks. */
internal fun BookDocumentViewerItem<EntryChapter>.paginationGroup(): String = when (this) {
    is BookDocumentViewerItem.Block -> section.key
    is BookDocumentViewerItem.Transition -> key
}

/** Include the active section and both adjacent sections, retaining intervening chapter transitions. */
internal fun BookDocumentViewerDataset<EntryChapter>.paginationWindow(
    center: Int,
): List<BookDocumentViewerItem<EntryChapter>> {
    if (isEmpty()) return emptyList()
    var start = center.coerceIn(indices)
    var end = start
    val group = get(start).paginationGroup()
    while (start > 0 && get(start - 1).paginationGroup() == group) start--
    while (end < lastIndex && get(end + 1).paginationGroup() == group) end++
    if (start > 0) {
        start--
        if (get(start) is BookDocumentViewerItem.Transition && start > 0) start--
        val previousGroup = get(start).paginationGroup()
        while (start > 0 && get(start - 1).paginationGroup() == previousGroup) start--
    }
    if (end < lastIndex) {
        end++
        if (get(end) is BookDocumentViewerItem.Transition && end < lastIndex) end++
        val nextGroup = get(end).paginationGroup()
        while (end < lastIndex && get(end + 1).paginationGroup() == nextGroup) end++
    }
    return (start..end).map(::get)
}

/**
 * Pagination input for the paged viewer. A resolved boundary must not spend a page between the
 * two chapters: hidden mode renders pure spacing once a contiguous destination is prepared, and
 * when-needed mode joins contiguous chapters seamlessly. Boundaries that still need the reader's
 * attention - terminal, failed, preparing, unrequested or gapped destinations - keep their page,
 * because settling on it is what requests the destination chapter.
 */
internal fun List<BookDocumentViewerItem<EntryChapter>>.withoutResolvedBoundaries(
    displayMode: ChapterTransitionMode,
    preparedChapterIds: Set<Long>,
    loadStateOf: (Long) -> BookDocumentChapterLoadState?,
): List<BookDocumentViewerItem<EntryChapter>> = filterNot { item ->
    if (item !is BookDocumentViewerItem.Transition) return@filterNot false
    val destination = item.transition.to ?: return@filterNot false
    if (destination.id !in preparedChapterIds) return@filterNot false
    val loadState = loadStateOf(destination.id)
    val chapterGap = boundaryChapterGap(item.transition)
    rendersCompactHiddenBoundary(displayMode, item.transition, loadState, chapterGap) ||
        isSeamlessWhenNeededBoundary(displayMode, item.transition, true, loadState)
}
