package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentTextRange
import mihon.book.api.document.toBookDocumentLinkTarget
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.BOOK_RESOURCE_FRAGMENT_ATTRIBUTE
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.BOOK_RESOURCE_ID_ATTRIBUTE
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.BOOK_RESOURCE_REFERENCE_ATTRIBUTE
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal data class HtmlProseInlineFragment(
    val text: String,
    val links: List<BookDocumentLink>,
    val inlineStyles: List<BookDocumentInlineStyleRange>,
    val anchors: Map<String, Int>,
) {
    fun toRichText(start: Int = 0): BookDocumentRichText = BookDocumentRichText(
        text = text,
        range = BookDocumentTextRange(start, start + text.length),
        links = links,
        inlineStyles = inlineStyles,
    )

    fun withParagraphTerminator(): HtmlProseInlineFragment {
        val trimmed = trimEnd()
        return trimmed.copy(text = trimmed.text + "\n\n")
    }

    fun trim(): HtmlProseInlineFragment {
        val start = text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: text.length
        val end = text.indexOfLast { !it.isWhitespace() }.let { if (it < start) start else it + 1 }
        return slice(start, end)
    }

    private fun trimEnd(): HtmlProseInlineFragment {
        val end = text.indexOfLast { !it.isWhitespace() }.let { if (it < 0) 0 else it + 1 }
        return slice(0, end)
    }

    private fun slice(start: Int, end: Int): HtmlProseInlineFragment {
        val boundedStart = start.coerceIn(0, text.length)
        val boundedEnd = end.coerceIn(boundedStart, text.length)
        return HtmlProseInlineFragment(
            text = text.substring(boundedStart, boundedEnd),
            links = links.mapNotNull { link ->
                val clippedStart = maxOf(link.start, boundedStart)
                val clippedEnd = minOf(link.endExclusive, boundedEnd)
                if (clippedEnd <= clippedStart) {
                    null
                } else {
                    BookDocumentLink(
                        start = clippedStart - boundedStart,
                        endExclusive = clippedEnd - boundedStart,
                        target = link.target,
                    )
                }
            },
            inlineStyles = inlineStyles.mapNotNull { style ->
                val clippedStart = maxOf(style.start, boundedStart)
                val clippedEnd = minOf(style.endExclusive, boundedEnd)
                if (clippedEnd <= clippedStart) {
                    null
                } else {
                    BookDocumentInlineStyleRange(
                        start = clippedStart - boundedStart,
                        endExclusive = clippedEnd - boundedStart,
                        style = style.style,
                    )
                }
            },
            anchors = anchors.mapValues { (_, offset) ->
                (offset - boundedStart).coerceIn(0, boundedEnd - boundedStart)
            },
        )
    }
}

internal class HtmlProseInlineParser(
    private val preserveWhitespace: Boolean,
) {
    private val text = StringBuilder()
    private val links = mutableListOf<BookDocumentLink>()
    private val styles = mutableListOf<BookDocumentInlineStyleRange>()
    private val anchors = linkedMapOf<String, Int>()
    private var whitespacePending = false

    fun parse(nodes: List<Node>): HtmlProseInlineFragment {
        nodes.forEach(::appendNode)
        return HtmlProseInlineFragment(text.toString(), links, styles, anchors).trim()
    }

    private fun appendNode(node: Node) {
        when (node) {
            is TextNode -> appendText(node.wholeText)
            is Element -> appendElement(node)
            else -> node.childNodes().forEach(::appendNode)
        }
    }

    private fun appendElement(element: Element) {
        element.fragments().forEach { fragment -> anchors.putIfAbsent(fragment, text.length) }
        if (element.normalName() == "br") {
            whitespacePending = false
            if (text.isNotEmpty() && text.last() != '\n') text.append('\n')
            return
        }
        if (element.normalName() == "img") return

        if (!preserveWhitespace && whitespacePending && text.isNotEmpty() && element.text().isNotBlank()) {
            text.append(' ')
            whitespacePending = false
        }
        val start = text.length
        element.childNodes().forEach(::appendNode)
        val end = text.length
        if (end <= start) return
        element.bookDocumentLinkTarget()?.let { target ->
            links += BookDocumentLink(start, end, target)
        }
        element.documentInlineStyle()?.let { style ->
            styles += BookDocumentInlineStyleRange(start, end, style)
        }
    }

    private fun appendText(value: String) {
        if (preserveWhitespace) {
            text.append(value)
            return
        }
        value.forEach { character ->
            if (character.isWhitespace()) {
                whitespacePending = true
            } else {
                if (whitespacePending && text.isNotEmpty() && text.last() != '\n') text.append(' ')
                whitespacePending = false
                text.append(character)
            }
        }
    }
}

private fun Element.bookDocumentLinkTarget(): BookDocumentLinkTarget? {
    val resourceId = attr(BOOK_RESOURCE_ID_ATTRIBUTE).trim()
    val fragment = attr(BOOK_RESOURCE_FRAGMENT_ATTRIBUTE).trim().takeIf(String::isNotEmpty)
    if (attr(BOOK_RESOURCE_REFERENCE_ATTRIBUTE) == "true" && fragment != null) {
        return BookDocumentLinkTarget.Reference(resourceId.takeIf(String::isNotEmpty), fragment)
    }
    if (resourceId.isNotEmpty()) {
        return BookDocumentLinkTarget.Resource(
            resourceId = resourceId,
            fragment = fragment,
        )
    }
    return attr("href").toBookDocumentLinkTarget()
}

internal fun Element.fragments(): List<String> = buildList {
    attr("id").trim().takeIf(String::isNotEmpty)?.let { add(it.take(256)) }
    if (normalName() == "a") {
        attr("name").trim().takeIf(String::isNotEmpty)?.let { name ->
            name.take(256).takeIf { it !in this }?.let(::add)
        }
    }
}
