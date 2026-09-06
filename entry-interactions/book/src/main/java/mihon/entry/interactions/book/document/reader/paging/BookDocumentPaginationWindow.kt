package mihon.entry.interactions.book.document.reader.paging

import mihon.entry.interactions.book.document.reader.BookDocumentViewerDataset
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
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
