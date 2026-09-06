package mihon.entry.interactions.book.document.reader.paging

import mihon.book.api.document.BookDocumentBlock
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem

/** Collapses paragraph margins and source blank lines at the physical page edges only. */
internal fun BookDocumentPageFragment.renderedTextBlock(): BookDocumentBlock {
    val block = item as BookDocumentViewerItem.Block
    val source = block.section.document.document.content.text
    var visibleStart = start
    var visibleEnd = end
    if (firstOnPage) {
        while (visibleStart < visibleEnd - 1 && source[block.content.logicalStart + visibleStart].isWhitespace()) {
            visibleStart++
        }
    }
    if (lastOnPage) {
        while (visibleEnd > visibleStart + 1 && source[block.content.logicalStart + visibleEnd - 1].isWhitespace()) {
            visibleEnd--
        }
    }
    val sliced = block.content.pageTextSlice(source, visibleStart, visibleEnd)
    return sliced.copy(
        style = sliced.style.withFlow(
            sliced.style.flow.copy(
                spacingBeforeEm = if (firstOnPage) 0f else sliced.style.spacingBeforeEm,
                spacingAfterEm = if (lastOnPage) 0f else sliced.style.spacingAfterEm,
            ),
        ),
    )
}
