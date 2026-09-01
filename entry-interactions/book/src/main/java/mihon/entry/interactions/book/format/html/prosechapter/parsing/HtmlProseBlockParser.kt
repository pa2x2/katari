package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentWhiteSpace
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal class HtmlProseBlockParser(
    private val context: HtmlProseParsingContext,
) {
    fun collectChildren(
        parent: Element,
        inheritedStyle: BookDocumentStyle,
        noteContext: Boolean,
        destination: MutableList<HtmlProseParsedBlock>,
        excludedChildren: Set<Element> = emptySet(),
    ) {
        val style = inheritedStyle.mergedWith(parent.documentBlockStyle())
        val isNote = noteContext || parent.attr("role") in setOf("doc-endnote", "doc-endnotes", "note")
        val inline = mutableListOf<Node>()
        var parentFragmentsAssigned = false

        fun flushInline() {
            if (inline.none(Node::hasReadableText)) {
                inline.clear()
                return
            }
            val fragments = if (parentFragmentsAssigned) emptyList() else parent.fragments()
            addTextBlock(
                nodes = inline.toList(),
                element = parent,
                role = BookDocumentBlockRole(if (isNote) BookDocumentBlockKind.NOTE else style.panelKind()),
                style = style,
                inheritedFragments = fragments,
                destination = destination,
            )
            if (fragments.isNotEmpty()) parentFragmentsAssigned = true
            inline.clear()
        }

        parent.childNodes().forEach { node ->
            if (node in excludedChildren) return@forEach
            when {
                node is TextNode -> inline.add(node)
                node is Element && !node.isBlockElement() -> inline.add(node)
                node is Element -> {
                    flushInline()
                    val fragments = if (parentFragmentsAssigned) emptyList() else parent.fragments()
                    val added = addBlockElement(node, style, isNote, fragments, destination)
                    if (added && fragments.isNotEmpty()) parentFragmentsAssigned = true
                }
                else -> inline.add(node)
            }
        }
        flushInline()
    }

    private fun addBlockElement(
        element: Element,
        inheritedStyle: BookDocumentStyle,
        noteContext: Boolean,
        inheritedFragments: List<String>,
        destination: MutableList<HtmlProseParsedBlock>,
    ): Boolean {
        val style = inheritedStyle.mergedWith(element.documentBlockStyle())
        return when (element.normalName()) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> addTextBlock(
                element.childNodes(),
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.HEADING, level = element.normalName().drop(1).toInt()),
                style,
                inheritedFragments,
                destination,
            )
            "p" -> addParagraphBlocks(
                element = element,
                role = BookDocumentBlockRole(if (noteContext) BookDocumentBlockKind.NOTE else style.panelKind()),
                style = style,
                inheritedFragments = inheritedFragments,
                destination = destination,
            )
            "address", "dt", "dd", "figcaption", "caption" -> addTextBlock(
                nodes = element.childNodes(),
                element = element,
                role = BookDocumentBlockRole(
                    if (element.normalName() in setOf("figcaption", "caption")) {
                        BookDocumentBlockKind.CAPTION
                    } else if (noteContext) {
                        BookDocumentBlockKind.NOTE
                    } else {
                        style.panelKind()
                    },
                ),
                style = style,
                inheritedFragments = inheritedFragments,
                destination = destination,
            )
            "blockquote" -> addTextBlock(
                element.childNodes(),
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.QUOTE),
                style,
                inheritedFragments,
                destination,
            )
            "pre" -> addTextBlock(
                element.childNodes(),
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.PREFORMATTED),
                style.copy(whiteSpace = BookDocumentWhiteSpace.PRE).withFlow(style.flow),
                inheritedFragments,
                destination,
                preserveWhitespace = true,
            )
            "ol", "ul" -> addListBlock(element, style, inheritedFragments, destination)
            "table" -> addTableBlock(element, style, inheritedFragments, destination)
            "figure", "img" -> addFigureBlock(element, style, inheritedFragments, destination)
            "details" -> addDisclosureBlock(element, style, noteContext, inheritedFragments, destination)
            "hr" -> addThematicBreak(element, style, inheritedFragments, destination)
            else -> {
                val size = destination.size
                element.attr("data-katari-unsupported").takeIf(String::isNotBlank)?.let { type ->
                    addUnsupportedBlock(type, element, style, inheritedFragments, destination)
                } ?: run {
                    collectChildren(element, style, noteContext, destination)
                    if (destination.size > size && inheritedFragments.isNotEmpty()) {
                        val first = destination[size]
                        destination[size] = first.copy(
                            fragments = (inheritedFragments + first.fragments).distinct(),
                            explicitId = first.explicitId ?: inheritedFragments.firstOrNull(),
                        )
                    }
                }
                destination.size > size
            }
        }
    }

    internal fun parseInline(nodes: List<Node>, preserveWhitespace: Boolean = false) =
        HtmlProseInlineParser(preserveWhitespace).parse(nodes)

    internal fun claimSemanticUnit() = context.claimSemanticUnit()

    internal fun claimCanonicalText(length: Int) = context.claimCanonicalText(length)

    internal fun collectNested(
        parent: Element,
        style: BookDocumentStyle,
        noteContext: Boolean,
        excludedChildren: Set<Element> = emptySet(),
    ): List<HtmlProseParsedBlock> = buildList {
        collectChildren(parent, style, noteContext, this, excludedChildren)
    }
}

private fun Node.hasReadableText(): Boolean = when (this) {
    is TextNode -> wholeText.any { !it.isWhitespace() }
    is Element -> normalName() == "img" || text().isNotBlank()
    else -> childNodes().any(Node::hasReadableText)
}

private fun Element.isBlockElement(): Boolean = normalName() in setOf(
    "address", "article", "aside", "blockquote", "caption", "dd", "details", "div", "dl", "dt",
    "figcaption", "figure", "footer", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "img",
    "li", "main", "nav", "ol", "p", "pre", "section", "table", "tbody", "td", "tfoot", "th",
    "thead", "tr", "ul",
)

private fun BookDocumentStyle.panelKind(): BookDocumentBlockKind =
    if (backgroundArgb != null || border != null || paddingEm > 0f) {
        BookDocumentBlockKind.CALLOUT
    } else {
        BookDocumentBlockKind.PARAGRAPH
    }
