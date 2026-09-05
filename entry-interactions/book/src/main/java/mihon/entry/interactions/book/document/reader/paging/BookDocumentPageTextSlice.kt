package mihon.entry.interactions.book.document.reader.paging

import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentTextRange

/** Retains source ranges and inline semantics when a paragraph continues on another page. */
internal fun BookDocumentBlock.pageTextSlice(sourceText: String, start: Int, end: Int): BookDocumentBlock {
    val textContent = content as BookDocumentBlockContent.Text
    require(start in 0 until end && end <= logicalLength)
    if (start == 0 && end == logicalLength) return this
    val text = sourceText.substring(logicalStart + start, logicalStart + end)
    val value = BookDocumentRichText(
        text = text,
        range = BookDocumentTextRange(0, text.length),
        links = links.mapNotNull { link ->
            val from = maxOf(start, link.start)
            val to = minOf(end, link.endExclusive)
            if (from < to) link.copy(start = from - start, endExclusive = to - start) else null
        },
        inlineStyles = inlineStyles.mapNotNull { span ->
            val from = maxOf(start, span.start)
            val to = minOf(end, span.endExclusive)
            if (from < to) span.copy(start = from - start, endExclusive = to - start) else null
        },
    )
    return copy(
        content = textContent.copy(value = value),
        plainText = text.trim(),
        logicalStart = logicalStart + start,
        logicalEndExclusive = logicalStart + end,
        style = style.withFlow(
            style.flow.copy(
                firstLineIndentEm = if (start == 0) style.firstLineIndentEm else 0f,
                spacingBeforeEm = if (start == 0) style.spacingBeforeEm else 0f,
                spacingAfterEm = if (end == logicalLength) style.spacingAfterEm else 0f,
            ),
        ),
    )
}
