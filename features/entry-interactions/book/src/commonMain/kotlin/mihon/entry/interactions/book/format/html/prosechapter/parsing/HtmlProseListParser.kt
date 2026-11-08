package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentListItem
import mihon.book.api.document.BookDocumentListMarkerStyle
import mihon.book.api.document.BookDocumentStyle
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import org.jsoup.nodes.Element

internal fun HtmlProseBlockParser.addListBlock(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
): Boolean {
    val parsedItems = mutableListOf<ParsedListItem>()
    collectListItems(element, depth = 0, parsedItems)
    if (parsedItems.isEmpty()) return false

    claimSemanticUnit()
    val canonical = StringBuilder()
    val items = mutableListOf<BookDocumentListItem>()
    val anchors = linkedMapOf<String, Int>()
    val fragments = linkedSetOf<String>().apply {
        addAll(inheritedFragments)
        addAll(element.fragments())
    }
    parsedItems.forEach { item ->
        val start = canonical.length
        canonical.append(item.inline.text)
        items += BookDocumentListItem(item.inline.toRichText(start), item.depth, item.marker)
        item.inline.anchors.forEach { (fragment, offset) ->
            fragments += fragment
            anchors.putIfAbsent(fragment, start + offset)
        }
        fragments += item.fragments
        item.fragments.forEach { fragment -> anchors.putIfAbsent(fragment, start) }
        canonical.append('\n')
    }
    canonical.append('\n')
    claimCanonicalText(canonical.length)
    val ordered = element.normalName() == "ol"
    destination += HtmlProseParsedBlock(
        text = canonical.toString(),
        plainText = parsedItems.joinToString("\n") { it.inline.text }.trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.LIST, ordered = ordered),
        content = BookDocumentBlockContent.ListBlock(
            ordered = ordered,
            start = element.attr("start").toIntOrNull()?.coerceAtLeast(1) ?: 1,
            markerStyle = element.listMarkerStyle(),
            items = items,
        ),
        style = style,
        explicitId = element.fragments().firstOrNull() ?: inheritedFragments.firstOrNull(),
        fragments = fragments.toList(),
        anchors = anchors,
    )
    return true
}

private data class ParsedListItem(
    val depth: Int,
    val marker: String?,
    val inline: HtmlProseInlineFragment,
    val fragments: List<String>,
)

private fun HtmlProseBlockParser.collectListItems(
    list: Element,
    depth: Int,
    destination: MutableList<ParsedListItem>,
) {
    if (depth > HtmlProseChapterContract.MAX_LIST_DEPTH) {
        throw HtmlProseLimitExceededException("HTML list nesting exceeds the supported depth")
    }
    val ordered = list.normalName() == "ol"
    var ordinal = list.attr("start").toIntOrNull()?.coerceAtLeast(1) ?: 1
    list.children().filter { it.normalName() == "li" }.forEach { item ->
        val itemOrdinal = item.attr("value").toIntOrNull()?.takeIf { it >= 1 } ?: ordinal
        val inlineNodes = item.childNodes().filterNot { node ->
            node is Element && node.normalName() in setOf("ol", "ul")
        }
        val inline = parseInline(inlineNodes)
        if (inline.text.isNotBlank()) {
            claimSemanticUnit()
            destination += ParsedListItem(
                depth = depth,
                marker = if (ordered) list.orderedMarker(itemOrdinal) else "•",
                inline = inline,
                fragments = item.fragments(),
            )
        }
        item.children().filter { it.normalName() in setOf("ol", "ul") }.forEach { nested ->
            collectListItems(nested, depth + 1, destination)
        }
        ordinal = itemOrdinal + 1
    }
}

private fun Element.listMarkerStyle(): BookDocumentListMarkerStyle = when {
    normalName() == "ul" -> BookDocumentListMarkerStyle.BULLET
    attr("type") == "a" -> BookDocumentListMarkerStyle.LOWER_ALPHA
    attr("type") == "A" -> BookDocumentListMarkerStyle.UPPER_ALPHA
    attr("type") == "i" -> BookDocumentListMarkerStyle.LOWER_ROMAN
    attr("type") == "I" -> BookDocumentListMarkerStyle.UPPER_ROMAN
    else -> BookDocumentListMarkerStyle.DECIMAL
}

private fun Element.orderedMarker(value: Int): String = when (listMarkerStyle()) {
    BookDocumentListMarkerStyle.LOWER_ALPHA -> "${value.toAlpha().lowercase()}."
    BookDocumentListMarkerStyle.UPPER_ALPHA -> "${value.toAlpha()}."
    BookDocumentListMarkerStyle.LOWER_ROMAN -> "${value.toRoman().lowercase()}."
    BookDocumentListMarkerStyle.UPPER_ROMAN -> "${value.toRoman()}."
    else -> "$value."
}

private fun Int.toAlpha(): String {
    var value = coerceAtLeast(1)
    return buildString {
        while (value > 0) {
            value -= 1
            insert(0, ('A'.code + value % 26).toChar())
            value /= 26
        }
    }
}

private fun Int.toRoman(): String {
    var value = coerceIn(1, 3_999)
    val symbols = listOf(
        1_000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
        50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
    )
    return buildString {
        symbols.forEach { (amount, symbol) ->
            while (value >= amount) {
                append(symbol)
                value -= amount
            }
        }
    }
}
