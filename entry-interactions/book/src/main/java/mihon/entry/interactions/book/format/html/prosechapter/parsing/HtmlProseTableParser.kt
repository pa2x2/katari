package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentTableCell
import mihon.book.api.document.BookDocumentTableCellScope
import mihon.book.api.document.BookDocumentTableRow
import mihon.book.api.document.layoutBookDocumentTable
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import org.jsoup.nodes.Element

internal fun HtmlProseBlockParser.addTableBlock(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
): Boolean {
    val rowElements = element.select("tr").filter { it.nearestAncestor("table") === element }
    if (rowElements.isEmpty()) return false
    val captionElement = element.children().firstOrNull { it.normalName() == "caption" }
    val caption = captionElement?.let { parseInline(it.childNodes()) }?.takeIf { it.text.isNotBlank() }
    val parsedRows = rowElements.mapNotNull { row ->
        val cells = row.children().filter { it.normalName() in setOf("td", "th") }.map { cell ->
            val columnSpan = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
            val rowSpan = cell.attr("rowspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
            if (
                columnSpan > HtmlProseChapterContract.MAX_TABLE_COLUMNS ||
                rowSpan > HtmlProseChapterContract.MAX_TABLE_COLUMNS
            ) {
                throw HtmlProseLimitExceededException("HTML table span exceeds the supported limit")
            }
            claimSemanticUnit()
            ParsedTableCell(
                inline = parseInline(cell.childNodes()),
                header = cell.normalName() == "th",
                scope = cell.attr("scope").toTableScope(),
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                fragments = cell.fragments(),
            )
        }
        cells.takeIf(List<ParsedTableCell>::isNotEmpty)?.let { ParsedTableRow(it, row.fragments()) }
    }
    if (parsedRows.isEmpty()) return false

    claimSemanticUnit()
    val canonical = StringBuilder()
    val anchors = linkedMapOf<String, Int>()
    val fragments = linkedSetOf<String>().apply {
        addAll(inheritedFragments)
        addAll(element.fragments())
    }
    val captionRich = caption?.let { value ->
        val start = canonical.length
        canonical.append(value.text)
        value.anchors.forEach { (fragment, offset) -> anchors.putIfAbsent(fragment, start + offset) }
        fragments += value.anchors.keys
        captionElement.fragments().forEach { fragment ->
            fragments += fragment
            anchors.putIfAbsent(fragment, start)
        }
        canonical.append('\n')
        value.toRichText(start)
    }
    val rows = parsedRows.map { row ->
        fragments += row.fragments
        row.fragments.forEach { fragment -> anchors.putIfAbsent(fragment, canonical.length) }
        val cells = row.cells.mapIndexed { index, cell ->
            val start = canonical.length
            canonical.append(cell.inline.text)
            cell.inline.anchors.forEach { (fragment, offset) -> anchors.putIfAbsent(fragment, start + offset) }
            fragments += cell.inline.anchors.keys
            fragments += cell.fragments
            cell.fragments.forEach { fragment -> anchors.putIfAbsent(fragment, start) }
            val model = BookDocumentTableCell(
                content = cell.inline.toRichText(start),
                header = cell.header,
                scope = cell.scope,
                columnSpan = cell.columnSpan,
                rowSpan = cell.rowSpan,
            )
            canonical.append(if (index == row.cells.lastIndex) '\n' else '\t')
            model
        }
        BookDocumentTableRow(cells)
    }
    canonical.append('\n')
    claimCanonicalText(canonical.length)
    val layout = rows.layoutBookDocumentTable()
        ?: throw HtmlProseLimitExceededException("HTML table cannot be represented within the supported grid")
    if (layout.columnCount > HtmlProseChapterContract.MAX_TABLE_COLUMNS) {
        throw HtmlProseLimitExceededException("HTML table contains too many columns")
    }
    destination += HtmlProseParsedBlock(
        text = canonical.toString(),
        plainText = buildList {
            caption?.text?.let(::add)
            parsedRows.forEach { row -> add(row.cells.joinToString("\t") { it.inline.text }) }
        }.joinToString("\n").trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.TABLE),
        content = BookDocumentBlockContent.Table(captionRich, rows, layout.columnCount),
        style = style,
        explicitId = element.fragments().firstOrNull() ?: inheritedFragments.firstOrNull(),
        fragments = fragments.toList(),
        anchors = anchors,
    )
    return true
}

private data class ParsedTableRow(
    val cells: List<ParsedTableCell>,
    val fragments: List<String>,
)

private data class ParsedTableCell(
    val inline: HtmlProseInlineFragment,
    val header: Boolean,
    val scope: BookDocumentTableCellScope?,
    val columnSpan: Int,
    val rowSpan: Int,
    val fragments: List<String>,
)

private fun Element.nearestAncestor(tag: String): Element? {
    var current = parent()
    while (current != null) {
        if (current.normalName() == tag) return current
        current = current.parent()
    }
    return null
}

private fun String.toTableScope(): BookDocumentTableCellScope? = when (trim().lowercase()) {
    "row" -> BookDocumentTableCellScope.ROW
    "col" -> BookDocumentTableCellScope.COLUMN
    "rowgroup" -> BookDocumentTableCellScope.ROW_GROUP
    "colgroup" -> BookDocumentTableCellScope.COLUMN_GROUP
    else -> null
}
