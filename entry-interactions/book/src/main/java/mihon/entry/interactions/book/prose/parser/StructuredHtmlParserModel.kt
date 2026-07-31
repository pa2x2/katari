package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentTableCellScope
import mihon.book.api.document.BookDocumentTableRow
import mihon.book.api.document.BookDocumentTextRange
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

internal data class ParsedBlock(
    val renderedText: Spanned,
    val logicalPlainText: String,
    val role: BookDocumentBlockRole,
    val content: BookDocumentBlockContent,
    val style: BookDocumentStyle,
    val explicitId: String?,
    val fragments: List<String>,
    val localAnchorOffsets: Map<String, Int> = emptyMap(),
    val disclosureBody: List<PreparedBookDocumentBlock> = emptyList(),
    val referencedResources: Set<String> = emptySet(),
) {
    fun anchorOffset(fragment: String): Int =
        localAnchorOffsets[fragment]?.coerceIn(0, renderedText.length) ?: 0
}

internal data class ParsedListItem(
    val depth: Int,
    val marker: String?,
    val renderedText: Spanned,
    val anchorOffsets: Map<String, Int>,
    val inlineStyles: List<BookDocumentInlineStyleRange>,
)

internal data class ParsedTableRow(
    val model: BookDocumentTableRow,
    val cells: List<ParsedTableCell>,
    val fragments: List<String>,
)

internal data class ParsedTableCell(
    val header: Boolean,
    val scope: BookDocumentTableCellScope?,
    val columnSpan: Int,
    val rowSpan: Int,
    val renderedText: Spanned,
    val anchorOffsets: Map<String, Int>,
    val inlineStyles: List<BookDocumentInlineStyleRange>,
)

internal sealed interface ParagraphPiece {
    data class Inline(val element: Element) : ParagraphPiece
    data class Image(val element: Element) : ParagraphPiece
}

internal fun Element.splitAroundImages(): List<ParagraphPiece> {
    return splitNodeAroundImages().map { piece ->
        when (piece) {
            is SplitNodePiece.Inline -> ParagraphPiece.Inline(piece.node as Element)
            is SplitNodePiece.Image -> ParagraphPiece.Image(piece.element)
        }
    }
}

internal sealed interface SplitNodePiece {
    data class Inline(val node: Node) : SplitNodePiece
    data class Image(val element: Element) : SplitNodePiece
}

internal fun Node.splitNodeAroundImages(): List<SplitNodePiece> = when {
    this is Element && tagName() == "img" -> listOf(SplitNodePiece.Image(clone()))
    this !is Element -> listOf(SplitNodePiece.Inline(clone()))
    childNodeSize() == 0 -> listOf(SplitNodePiece.Inline(clone()))
    else -> {
        val pieces = mutableListOf<SplitNodePiece>()
        var inlineElement: Element? = null

        fun flushInline() {
            inlineElement?.let { pieces += SplitNodePiece.Inline(it) }
            inlineElement = null
        }

        childNodes().forEach { child ->
            child.splitNodeAroundImages().forEach { piece ->
                when (piece) {
                    is SplitNodePiece.Inline -> {
                        val destination = inlineElement ?: clone().also(Element::empty).also {
                            inlineElement = it
                        }
                        destination.appendChild(piece.node)
                    }
                    is SplitNodePiece.Image -> {
                        flushInline()
                        pieces += piece
                    }
                }
            }
        }
        flushInline()
        pieces
    }
}

internal data class RenderedFragment(
    val text: Spanned,
    val anchorOffsets: Map<String, Int>,
    val inlineStyles: List<BookDocumentInlineStyleRange> = emptyList(),
) {
    fun toRichText(rangeStart: Int): BookDocumentRichText = BookDocumentRichText(
        text = text.toString(),
        range = BookDocumentTextRange(rangeStart, rangeStart + text.length),
        links = text.documentLinks(),
        inlineStyles = inlineStyles,
    )

    fun trim(): RenderedFragment {
        val value = text.toString()
        val start = value.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: value.length
        val end = value.indexOfLast { !it.isWhitespace() }.let { if (it < start) start else it + 1 }
        return slice(start, end)
    }

    fun trimEnd(): RenderedFragment {
        val value = text.toString()
        val end = value.indexOfLast { !it.isWhitespace() }.let { if (it < 0) 0 else it + 1 }
        return slice(0, end)
    }

    fun withParagraphTerminator(): RenderedFragment {
        val trimmed = trimEnd()
        return trimmed.copy(text = SpannableStringBuilder(trimmed.text).append("\n\n"))
    }

    private fun slice(start: Int, end: Int): RenderedFragment {
        val boundedStart = start.coerceIn(0, text.length)
        val boundedEnd = end.coerceIn(boundedStart, text.length)
        return RenderedFragment(
            text = SpannableString(text.subSequence(boundedStart, boundedEnd)),
            anchorOffsets = anchorOffsets.mapValues { (_, offset) ->
                (offset - boundedStart).coerceIn(0, boundedEnd - boundedStart)
            },
            inlineStyles = inlineStyles.mapNotNull { inline ->
                inline.clippedAndShifted(boundedStart, boundedEnd)
            },
        )
    }
}
