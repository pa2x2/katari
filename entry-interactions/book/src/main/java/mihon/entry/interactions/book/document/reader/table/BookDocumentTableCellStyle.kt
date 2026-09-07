package mihon.entry.interactions.book.document.reader.table

import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentTableCell

/** Keeps authored text flow when applying header emphasis in both measurement and rendering. */
internal fun BookDocumentBlock.forTableCell(cell: BookDocumentTableCell): BookDocumentBlock = if (cell.header) {
    copy(style = style.copy(bold = true).withFlow(style.flow))
} else {
    this
}
