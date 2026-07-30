package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyleRange
import mihon.entry.interactions.book.document.model.BookDocumentListItem
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import org.jsoup.nodes.Element

internal fun StructuredHtmlProseParser.addList(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    val items = mutableListOf<ParsedListItem>()
    collectListItems(element, depth = 0, items)
    if (items.isEmpty()) return
    val ordered = element.tagName() == "ol"
    val markerStyle = element.listMarkerStyle()
    val start = if (ordered) element.attr("start").toIntOrNull()?.coerceIn(-100_000, 100_000) ?: 1 else 1
    val anchorOffsets = linkedMapOf<String, Int>().apply {
        element.ownFragments().forEach { put(it, 0) }
    }
    val inlineStyles = mutableListOf<BookDocumentInlineStyleRange>()
    val text = SpannableStringBuilder().apply {
        items.forEach { item ->
            repeat(item.model.depth) { append("  ") }
            append(item.model.marker ?: "•")
            append(' ')
            val itemStart = length
            append(item.renderedText)
            item.anchorOffsets.forEach { (fragment, offset) ->
                anchorOffsets.putIfAbsent(fragment, itemStart + offset)
            }
            item.inlineStyles.forEach { inline ->
                inlineStyles += inline.shifted(itemStart)
            }
            append('\n')
        }
        append('\n')
    }
    parsedBlocks += ParsedBlock(
        renderedText = SpannableString(text),
        logicalPlainText = text.toString().trim(),
        role = BookDocumentBlockRole(
            kind = BookDocumentBlockKind.LIST,
            ordered = ordered,
        ),
        content = BookDocumentBlockContent.ListBlock(ordered, start, markerStyle, items.map(ParsedListItem::model)),
        style = style,
        explicitId = element.id().ifBlank { null },
        fragments = (inheritedFragments + element.fragments()).distinct(),
        localAnchorOffsets = anchorOffsets,
        inlineStyles = inlineStyles,
    )
}

internal fun StructuredHtmlProseParser.collectListItems(
    list: Element,
    depth: Int,
    destination: MutableList<ParsedListItem>,
) {
    if (depth > MAX_LIST_DEPTH) return
    val ordered = list.tagName() == "ol"
    val markerStyle = list.listMarkerStyle()
    var index = if (ordered) list.attr("start").toIntOrNull()?.coerceIn(-100_000, 100_000) ?: 1 else 1
    list.children().filter { it.tagName() == "li" }.forEach { item ->
        val nested = item.children().filter { it.tagName() == "ol" || it.tagName() == "ul" }
        val own = item.clone()
        own.children().filter { it.tagName() == "ol" || it.tagName() == "ul" }.forEach(Element::remove)
        val rendered = renderHtml(own).trim()
        if (rendered.text.any(Char::isReadableDocumentCharacter)) {
            destination += ParsedListItem(
                model = BookDocumentListItem(
                    text = rendered.text.toString(),
                    depth = depth,
                    marker = if (ordered) markerStyle.marker(index) else "•",
                ),
                renderedText = rendered.text,
                anchorOffsets = rendered.anchorOffsets,
                inlineStyles = rendered.inlineStyles,
            )
        }
        nested.forEach { collectListItems(it, depth + 1, destination) }
        index++
    }
}
