package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentItemSizing
import mihon.book.api.document.BookDocumentSideBySideItem
import mihon.book.api.document.BookDocumentSideBySideLayout
import mihon.book.api.document.BookDocumentStyle
import mihon.entry.interactions.book.format.html.prosechapter.style.itemSizing
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Turns a flex row or grid into one side-by-side block whose items keep their own blocks, as CSS makes every child
 * element an item. A container left with fewer than two readable items stacks its blocks in normal flow instead.
 */
internal fun HtmlProseBlockParser.addSideBySideBlock(
    element: Element,
    layout: BookDocumentSideBySideLayout,
    style: BookDocumentStyle,
    noteContext: Boolean,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
): Boolean {
    val items = mutableListOf<Pair<List<HtmlProseParsedBlock>, BookDocumentItemSizing>>()
    element.childNodes().forEach { node ->
        val blocks = mutableListOf<HtmlProseParsedBlock>()
        when (node) {
            is Element -> addItemElement(node, style, noteContext, blocks)
            // Loose text is an anonymous item of its own.
            is TextNode -> if (!node.isBlank) {
                addTextBlock(
                    listOf(node),
                    element,
                    BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
                    style,
                    emptyList(),
                    blocks,
                )
            }
        }
        if (blocks.any { it.text.isNotEmpty() }) {
            items += blocks to ((node as? Element)?.itemSizing() ?: BookDocumentItemSizing())
        }
    }
    if (items.size < 2) {
        val start = destination.size
        items.forEach { (blocks, _) -> destination += blocks }
        if (destination.size > start && inheritedFragments.isNotEmpty()) {
            val first = destination[start]
            destination[start] = first.copy(fragments = (inheritedFragments + first.fragments).distinct())
        }
        return destination.size > start
    }

    val text = StringBuilder()
    val assembled = items.map { (blocks, sizing) ->
        val body = assembleContent(blocks)
        BookDocumentSideBySideItem(body, text.length, sizing).also { text.append(body.text) }
    }
    claimSemanticUnit()
    val fragments = linkedSetOf<String>().apply {
        addAll(inheritedFragments)
        addAll(element.fragments())
        items.forEach { (blocks, _) -> blocks.forEach { addAll(it.fragments) } }
    }
    val anchors = linkedMapOf<String, Int>().apply {
        fragments.forEach { putIfAbsent(it, 0) }
        assembled.forEach { item ->
            item.body.anchors.forEach { (fragment, position) ->
                val block = item.body.blocks.first { it.id == position.blockId }
                put(fragment, item.bodyStartWithinBlock + block.logicalStart + position.offsetWithinBlock)
            }
        }
    }
    destination += HtmlProseParsedBlock(
        text = text.toString(),
        plainText = assembled.flatMap { it.body.blocks }.joinToString("\n") { it.plainText }.trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.SIDE_BY_SIDE),
        content = BookDocumentBlockContent.SideBySide(layout, assembled),
        style = style,
        explicitId = element.fragments().firstOrNull() ?: inheritedFragments.firstOrNull(),
        fragments = fragments.toList(),
        anchors = anchors,
    )
    return true
}
