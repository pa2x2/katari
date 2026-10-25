package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentWhiteSpace
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.style.clearsFloats
import mihon.entry.interactions.book.format.html.prosechapter.style.documentBoxStyle
import mihon.entry.interactions.book.format.html.prosechapter.style.documentFloat
import mihon.entry.interactions.book.format.html.prosechapter.style.resolvedFor
import mihon.entry.interactions.book.format.html.prosechapter.style.sideBySideLayout
import mihon.entry.interactions.book.format.html.prosechapter.style.withoutSpacing
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
        // The parent's margins belong to its box, not to the blocks of its loose text.
        val style = inheritedStyle.resolvedFor(parent).withoutSpacing()
        val isNote = noteContext || parent.attr("role") in setOf("doc-endnote", "doc-endnotes", "note")
        val inline = mutableListOf<Node>()
        var parentFragmentsAssigned = false
        val pendingFragments = linkedSetOf<String>()
        val initialSize = destination.size

        fun flushInline() {
            if (inline.none(Node::hasReadableText)) {
                inline.filterIsInstance<Element>().forEach { element ->
                    element.getAllElements().forEach { pendingFragments += it.fragments() }
                }
                inline.clear()
                return
            }
            val fragments = pendingFragments.toList() + if (parentFragmentsAssigned) emptyList() else parent.fragments()
            addTextBlock(
                nodes = inline.toList(),
                element = parent,
                role = BookDocumentBlockRole(
                    if (isNote) BookDocumentBlockKind.NOTE else BookDocumentBlockKind.PARAGRAPH,
                ),
                style = style,
                inheritedFragments = fragments,
                destination = destination,
            )
            if (fragments.isNotEmpty()) parentFragmentsAssigned = true
            pendingFragments.clear()
            inline.clear()
        }

        parent.childNodes().forEach { node ->
            if (node in excludedChildren) return@forEach
            when {
                node is TextNode -> inline.add(node)
                node is Element && !node.isBlockElement() -> inline.add(node)
                node is Element -> {
                    flushInline()
                    val fragments =
                        pendingFragments.toList() + if (parentFragmentsAssigned) emptyList() else parent.fragments()
                    val added = addBlockElement(node, style, isNote, fragments, destination)
                    if (added) {
                        if (fragments.isNotEmpty()) parentFragmentsAssigned = true
                        pendingFragments.clear()
                    } else {
                        node.getAllElements().forEach { pendingFragments += it.fragments() }
                    }
                }
                else -> inline.add(node)
            }
        }
        flushInline()
        if (pendingFragments.isNotEmpty() && destination.size > initialSize) {
            val last = destination.last()
            destination[destination.lastIndex] = last.copy(
                fragments = (last.fragments + pendingFragments).distinct(),
                anchors = pendingFragments.associateWith { last.text.trimEnd().length } + last.anchors,
            )
        }
    }

    private fun addBlockElement(
        element: Element,
        inheritedStyle: BookDocumentStyle,
        noteContext: Boolean,
        inheritedFragments: List<String>,
        destination: MutableList<HtmlProseParsedBlock>,
    ): Boolean {
        val box = element.documentBoxStyle(container = element.isContainer())
        val elementStyle = inheritedStyle.resolvedFor(element)
        val style = if (box != null) elementStyle.withoutSpacing() else elementStyle
        val size = destination.size
        val added = addBlockElementContent(element, style, noteContext, inheritedFragments, destination)
        if (box != null) destination.enclose(size, HtmlProseParsedBox(box))
        destination.applyFloat(size, element)
        return added
    }

    /** Adds [element] as the content of one item of a side-by-side row. */
    internal fun addItemElement(
        element: Element,
        inheritedStyle: BookDocumentStyle,
        noteContext: Boolean,
        destination: MutableList<HtmlProseParsedBlock>,
    ) {
        addBlockElement(element, inheritedStyle, noteContext, emptyList(), destination)
    }

    private fun addBlockElementContent(
        element: Element,
        style: BookDocumentStyle,
        noteContext: Boolean,
        inheritedFragments: List<String>,
        destination: MutableList<HtmlProseParsedBlock>,
    ): Boolean {
        return when (element.normalName()) {
            "h1", "h2", "h3", "h4", "h5", "h6" -> addTextAndImageBlocks(
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.HEADING, level = element.normalName().drop(1).toInt()),
                style,
                inheritedFragments,
                destination,
            )
            "p" -> addTextAndImageBlocks(
                element = element,
                role = BookDocumentBlockRole(
                    if (noteContext) BookDocumentBlockKind.NOTE else BookDocumentBlockKind.PARAGRAPH,
                ),
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
                        BookDocumentBlockKind.PARAGRAPH
                    },
                ),
                style = style,
                inheritedFragments = inheritedFragments,
                destination = destination,
            )
            "blockquote" -> addQuoteBlocks(element, style, noteContext, inheritedFragments, destination)
            "pre" -> addTextBlock(
                element.childNodes(),
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.PREFORMATTED),
                style.copy(whiteSpace = BookDocumentWhiteSpace.PRE),
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
                val unsupported = element.attr("data-katari-unsupported").takeIf(String::isNotBlank)
                val sideBySide = element.sideBySideLayout()
                if (unsupported != null) {
                    addUnsupportedBlock(unsupported, element, style, inheritedFragments, destination)
                } else if (sideBySide != null) {
                    addSideBySideBlock(element, sideBySide, style, noteContext, inheritedFragments, destination)
                } else {
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

// Elements whose blocks come from their children, so their own margins only exist as a box around them.
private fun Element.isContainer(): Boolean = normalName() !in LEAF_BLOCK_ELEMENTS && !hasAttr("data-katari-unsupported")

private val LEAF_BLOCK_ELEMENTS = setOf(
    "h1", "h2", "h3", "h4", "h5", "h6", "p", "address", "dt", "dd", "figcaption", "caption", "pre", "ol", "ul",
    "table", "figure", "img", "details", "hr",
)

/**
 * Marks a single block added since [start] as floating when its element floats; a float made of several blocks
 * stays in normal flow. A block of an element that clears floats starts below them.
 */
private fun MutableList<HtmlProseParsedBlock>.applyFloat(start: Int, element: Element) {
    if (start == size) return
    val float = element.documentFloat()
    if (float != null && size - start == 1) {
        val block = this[start]
        this[start] = block.copy(style = block.style.copy(flow = block.style.flow.copy(float = float)))
    }
    if (element.clearsFloats()) {
        val block = this[start]
        this[start] = block.copy(style = block.style.copy(flow = block.style.flow.copy(clearsFloats = true)))
    }
}

/**
 * Puts the blocks added since [start] inside [box], and makes plain paragraphs in a box that draws something
 * callouts. A box is dropped when it would nest deeper than the document model allows.
 */
private fun MutableList<HtmlProseParsedBlock>.enclose(start: Int, box: HtmlProseParsedBox) {
    if (start == size) return
    if (subList(start, size).any { it.boxes.size >= HtmlProseChapterContract.MAX_BOX_DEPTH }) return
    for (index in start until size) {
        val block = this[index]
        this[index] = block.copy(
            boxes = listOf(box) + block.boxes,
            role = if (box.style.decorated && block.role.kind == BookDocumentBlockKind.PARAGRAPH) {
                BookDocumentBlockRole(BookDocumentBlockKind.CALLOUT)
            } else {
                block.role
            },
        )
    }
}
