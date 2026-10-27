package mihon.entry.interactions.book.document.reader.paging

import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem

/**
 * Pairs pages into two-page spreads. Pairing restarts with every section, so a spread keeps its two pages while
 * neighbouring sections load and unload around it. A page that scrolls, being taller than the window, and a chapter
 * transition each take a spread of their own.
 */
internal fun List<BookDocumentPage>.pairedIntoSpreads(): List<BookDocumentPage> {
    val spreads = ArrayList<BookDocumentPage>(size / 2 + 1)
    var index = 0
    while (index < size) {
        val page = get(index)
        val next = getOrNull(index + 1)
        if (next != null && page.pairsWith(next)) {
            spreads += BookDocumentPage(page.fragments + next.fragments, facingStart = page.fragments.size)
            index += 2
        } else {
            spreads += page
            index++
        }
    }
    return spreads
}

private fun BookDocumentPage.pairsWith(next: BookDocumentPage): Boolean =
    !scrollable && !next.scrollable && isContent && next.isContent &&
        fragments.first().item.paginationGroup() == next.fragments.first().item.paginationGroup()

private val BookDocumentPage.isContent: Boolean get() = fragments.first().item is BookDocumentViewerItem.Block
