package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentTableCell
import mihon.book.api.document.BookDocumentTableRow
import mihon.book.api.document.MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN
import mihon.book.api.document.layoutBookDocumentTable
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
                ParsedTableCell(
                    header = cell.tagName() == "th",
                    scope = cell.attr("scope").toTableScope(),
                    columnSpan = cell.attr("colspan").toIntOrNull()
                        ?.coerceIn(1, MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN)
                        ?: 1,
                    rowSpan = cell.attr("rowspan").toIntOrNull()
                        ?.coerceIn(1, MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN)
                        ?: 1,
                    renderedText = rendered.text,
                    anchorOffsets = rendered.anchorOffsets,
                    inlineStyles = rendered.inlineStyles,
                )
            }
        cells.takeIf(List<*>::isNotEmpty)?.let {
            ParsedTableRow(
                model = BookDocumentTableRow(
                    cells.map { cell ->
                        BookDocumentTableCell(
                            content = RenderedFragment(
                                text = cell.renderedText,
                                anchorOffsets = cell.anchorOffsets,
                                inlineStyles = cell.inlineStyles,
                            ).toRichText(rangeStart = 0),
                            header = cell.header,
                            scope = cell.scope,
                            columnSpan = cell.columnSpan,
                            rowSpan = cell.rowSpan,
                        )
                    },
                ),
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
    val anchorOffsets = linkedMapOf<String, Int>().apply {
        element.ownFragments().forEach { put(it, 0) }
    }
    var semanticCaption: BookDocumentRichText? = null
    val semanticRows = mutableListOf<BookDocumentTableRow>()
    val text = SpannableStringBuilder()
    captionRendered?.let { rendered ->
        val captionStart = text.length
        text.append(rendered.text)
        semanticCaption = rendered.toRichText(captionStart)
        rendered.anchorOffsets.forEach { (fragment, offset) ->
            anchorOffsets.putIfAbsent(fragment, captionStart + offset)
        }
        text.append('\n')
    }
    parsedRows.forEach { row ->
        val rowStart = text.length
        row.fragments.forEach { fragment -> anchorOffsets.putIfAbsent(fragment, rowStart) }
        val semanticCells = mutableListOf<BookDocumentTableCell>()
        row.cells.forEachIndexed { index, cell ->
            if (index > 0) text.append('\t')
            val cellStart = text.length
            text.append(cell.renderedText)
            semanticCells += BookDocumentTableCell(
                content = RenderedFragment(
                    text = cell.renderedText,
                    anchorOffsets = cell.anchorOffsets,
                    inlineStyles = cell.inlineStyles,
                ).toRichText(cellStart),
                header = cell.header,
                scope = cell.scope,
                columnSpan = cell.columnSpan,
                rowSpan = cell.rowSpan,
            )
            cell.anchorOffsets.forEach { (fragment, offset) ->
                anchorOffsets.putIfAbsent(fragment, cellStart + offset)
            }
        }
        semanticRows += BookDocumentTableRow(semanticCells)
        text.append('\n')
    }
    val rendered = RenderedFragment(SpannableString(text), anchorOffsets)
        .trimEnd()
        .withParagraphTerminator()
    parsedBlocks += ParsedBlock(
        renderedText = rendered.text,
        logicalPlainText = rendered.text.toString().trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.TABLE),
        content = BookDocumentBlockContent.Table(
            caption = semanticCaption,
            rows = semanticRows,
            columnCount = columnCount,
        ),
        style = style,
        explicitId = element.id().ifBlank { null },
        fragments = (inheritedFragments + element.fragments()).distinct(),
        localAnchorOffsets = rendered.anchorOffsets,
    )
}
