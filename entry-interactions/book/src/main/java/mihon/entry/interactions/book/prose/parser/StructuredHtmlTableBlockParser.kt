package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyleRange
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import mihon.entry.interactions.book.document.model.BookDocumentTableCell
import mihon.entry.interactions.book.document.model.BookDocumentTableRow
import mihon.entry.interactions.book.document.model.MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN
import mihon.entry.interactions.book.document.model.layoutBookDocumentTable
import org.jsoup.nodes.Element

internal fun StructuredHtmlProseParser.addTable(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    val parsedRows = element.select("tr").take(MAX_TABLE_ROWS).mapNotNull { row ->
        val cells = row.children()
            .filter { it.tagName() == "th" || it.tagName() == "td" }
            .take(MAX_TABLE_CELLS_PER_ROW)
            .map { cell ->
                val rendered = renderHtml(cell).trim()
                val text = rendered.text.toString()
                ParsedTableCell(
                    model = BookDocumentTableCell(
                        text = text,
                        header = cell.tagName() == "th",
                        scope = cell.attr("scope").toTableScope(),
                        columnSpan = cell.attr("colspan").toIntOrNull()
                            ?.coerceIn(1, MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN)
                            ?: 1,
                        rowSpan = cell.attr("rowspan").toIntOrNull()
                            ?.coerceIn(1, MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN)
                            ?: 1,
                        links = rendered.text.documentLinks(),
                    ),
                    renderedText = rendered.text,
                    anchorOffsets = rendered.anchorOffsets,
                    inlineStyles = rendered.inlineStyles,
                )
            }
        cells.takeIf(List<*>::isNotEmpty)?.let {
            ParsedTableRow(
                model = BookDocumentTableRow(cells.map(ParsedTableCell::model)),
                cells = cells,
                fragments = row.ownFragments(),
            )
        }
    }
    val rows = parsedRows.map(ParsedTableRow::model)
    val tableLayout = rows.layoutBookDocumentTable()
    val columnCount = tableLayout?.columnCount ?: 0
    if (
        rows.isEmpty() ||
        columnCount !in 1..MAX_TABLE_COLUMNS ||
        element.select("tr").size > MAX_TABLE_ROWS
    ) {
        addTextBlock(
            element,
            BookDocumentBlockRole(BookDocumentBlockKind.OTHER),
            style,
            inheritedFragments,
        )
        return
    }
    val captionRendered = element.selectFirst("caption")?.let(::renderHtml)?.trim()
        ?.takeIf { it.text.any(Char::isReadableDocumentCharacter) }
    val caption = captionRendered?.text?.toString()
    val anchorOffsets = linkedMapOf<String, Int>().apply {
        element.ownFragments().forEach { put(it, 0) }
    }
    val inlineStyles = mutableListOf<BookDocumentInlineStyleRange>()
    val text = SpannableStringBuilder().apply {
        captionRendered?.let { rendered ->
            val captionStart = length
            append(rendered.text)
            rendered.anchorOffsets.forEach { (fragment, offset) ->
                anchorOffsets.putIfAbsent(fragment, captionStart + offset)
            }
            rendered.inlineStyles.forEach { inline ->
                inlineStyles += inline.shifted(captionStart)
            }
            append('\n')
        }
        parsedRows.forEach { row ->
            val rowStart = length
            row.fragments.forEach { fragment -> anchorOffsets.putIfAbsent(fragment, rowStart) }
            row.cells.forEachIndexed { index, cell ->
                if (index > 0) append('\t')
                val cellStart = length
                append(cell.renderedText)
                cell.anchorOffsets.forEach { (fragment, offset) ->
                    anchorOffsets.putIfAbsent(fragment, cellStart + offset)
                }
                cell.inlineStyles.forEach { inline ->
                    inlineStyles += inline.shifted(cellStart)
                }
            }
            append('\n')
        }
    }
    val rendered = RenderedFragment(SpannableString(text), anchorOffsets)
        .trimEnd()
        .withParagraphTerminator()
    parsedBlocks += ParsedBlock(
        renderedText = rendered.text,
        logicalPlainText = rendered.text.toString().trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.TABLE),
        content = BookDocumentBlockContent.Table(
            caption = caption,
            captionLinks = captionRendered?.text?.documentLinks().orEmpty(),
            rows = rows,
            columnCount = columnCount,
        ),
        style = style,
        explicitId = element.id().ifBlank { null },
        fragments = (inheritedFragments + element.fragments()).distinct(),
        localAnchorOffsets = rendered.anchorOffsets,
        inlineStyles = inlineStyles,
    )
}
