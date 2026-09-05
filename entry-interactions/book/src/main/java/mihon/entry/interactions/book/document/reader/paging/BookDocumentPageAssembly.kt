package mihon.entry.interactions.book.document.reader.paging

import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import tachiyomi.domain.entry.model.EntryChapter

/** Packs measured fragments into pages, preserving source line boundaries and explicit transitions. */
internal fun assembleBookDocumentPages(
    items: List<BookDocumentViewerItem<EntryChapter>>,
    pageHeight: Int,
    measure: (BookDocumentPageFragment) -> Pair<Int, List<Int>>,
    pageBreak: BookDocumentViewerLocation<EntryChapter>? = null,
): List<BookDocumentPage> {
    val pages = mutableListOf<BookDocumentPage>()
    val fragments = mutableListOf<BookDocumentPageFragment>()
    var used = 0
    var sectionKey: String? = null
    fun flush() {
        if (fragments.isNotEmpty()) pages += BookDocumentPage(fragments.toList())
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
            val fragment = BookDocumentPageFragment(item)
            pages += BookDocumentPage(listOf(fragment), scrollable = true)
        } else if (item is BookDocumentViewerItem.Block) {
            if (item.content.content is BookDocumentBlockContent.Disclosure ||
                item.content.content is BookDocumentBlockContent.Figure ||
                item.content.content is BookDocumentBlockContent.Table
            ) {
                // Expansion, resource loading and table adaptation can change height after measurement.
                // A dedicated scrollable page keeps the complete rich block and its actions accessible.
                flush()
                pages += BookDocumentPage(listOf(BookDocumentPageFragment(item)), scrollable = true)
                return@forEach
            }
            var start = 0
            val end = item.content.logicalLength
            val breakOffset = pageBreak?.takeIf {
                it.section.key == item.section.key && it.position.blockId == item.content.id
            }?.position?.offsetWithinBlock?.coerceIn(0, end)
            while (start < end) {
                if (start == breakOffset) flush()
                val segmentEnd = if (breakOffset != null && start < breakOffset) breakOffset else end
                val whole = BookDocumentPageFragment(item, start, segmentEnd)
                val (height, lineEnds) = measure(whole)
                if (height <= pageHeight - used) {
                    fragments += whole
                    used += height
                    start = segmentEnd
                    continue
                }
                val candidates = if (item.content.content is BookDocumentBlockContent.Text) {
                    lineEnds.map { start + it }.filter { it > start && it < segmentEnd }
                } else {
                    emptyList()
                }
                var low = 0
                var high = candidates.lastIndex
                var fitting: BookDocumentPageFragment? = null
                while (low <= high) {
                    val middle = (low + high) / 2
                    val candidate = BookDocumentPageFragment(item, start, candidates[middle])
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
                    start = segmentEnd
                }
            }
        }
    }
    flush()
    return pages
}
