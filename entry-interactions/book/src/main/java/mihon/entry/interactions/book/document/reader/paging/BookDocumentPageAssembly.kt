package mihon.entry.interactions.book.document.reader.paging

import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import tachiyomi.domain.entry.model.EntryChapter

/** Packs measured fragments into pages, preserving source line boundaries and explicit transitions. */
internal fun assembleBookDocumentPages(
    items: List<BookDocumentViewerItem<EntryChapter>>,
    pageHeight: Int,
    measure: (BookDocumentPageFragment) -> Pair<Int, List<Int>>,
): List<BookDocumentPage> {
    val pages = mutableListOf<BookDocumentPage>()
    val fragments = mutableListOf<BookDocumentPageFragment>()
    var used = 0
    var sectionKey: String? = null
    fun flush() {
        if (fragments.isNotEmpty()) {
            fragments[fragments.lastIndex] = fragments.last().copy(lastOnPage = true)
            pages += BookDocumentPage(fragments.toList())
        }
        fragments.clear()
        used = 0
    }
    items.forEach { item ->
        // Publication sections begin on a new page, independently of the retained chapter window.
        val group = item.paginationGroup()
        if (sectionKey != group) flush()
        sectionKey = group
        if (item is BookDocumentViewerItem.Transition) {
            flush()
            val fragment = BookDocumentPageFragment(item, firstOnPage = true, lastOnPage = true)
            pages += BookDocumentPage(listOf(fragment), scrollable = true)
        } else if (item is BookDocumentViewerItem.Block) {
            if (item.content.content is BookDocumentBlockContent.Disclosure ||
                item.content.content is BookDocumentBlockContent.Figure ||
                item.content.content is BookDocumentBlockContent.Table
            ) {
                // Expansion, resource loading and table adaptation can change height after measurement.
                // A dedicated scrollable page keeps the complete rich block and its actions accessible.
                flush()
                pages +=
                    BookDocumentPage(
                        listOf(BookDocumentPageFragment(item, firstOnPage = true, lastOnPage = true)),
                        scrollable = true,
                    )
                return@forEach
            }
            var start = 0
            val end = item.content.logicalLength
            while (start < end) {
                val whole =
                    BookDocumentPageFragment(item, start, end, firstOnPage = fragments.isEmpty(), lastOnPage = true)
                val (height, lineEnds) = measure(whole)
                if (height <= pageHeight - used) {
                    fragments += whole.copy(lastOnPage = false)
                    used += measure(whole.copy(lastOnPage = false)).first
                    start = end
                    continue
                }
                val candidates = if (item.content.content is BookDocumentBlockContent.Text) {
                    lineEnds.map { start + it }.filter { it > start && it < end }
                } else {
                    emptyList()
                }
                var low = 0
                var high = candidates.lastIndex
                var fitting: BookDocumentPageFragment? = null
                while (low <= high) {
                    val middle = (low + high) / 2
                    // Keep source whitespace with the preceding line so a continuation cannot
                    // start with empty paragraph lines or become a whitespace-only page.
                    var candidateEnd = candidates[middle]
                    val text = item.section.document.document.content.text
                    while (candidateEnd < end && text[item.content.logicalStart + candidateEnd].isWhitespace()) {
                        candidateEnd++
                    }
                    val candidate = whole.copy(end = candidateEnd)
                    if (measure(candidate).first <= pageHeight - used) {
                        fitting = candidate
                        low = middle + 1
                    } else {
                        high = middle - 1
                    }
                }
                if (fitting != null) {
                    fragments += fitting
                    start = fitting.end
                    flush()
                } else if (fragments.isNotEmpty()) {
                    flush()
                } else {
                    // A single line or rich block can exceed even an empty page at large text sizes.
                    pages += BookDocumentPage(listOf(whole), scrollable = true)
                    start = end
                }
            }
        }
    }
    flush()
    return pages
}
