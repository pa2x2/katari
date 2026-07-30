package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal fun prepareStructuredHtmlBookDocument(
    resourceId: String,
    revision: String?,
    body: Element,
): PreparedBookDocument = StructuredHtmlProseParser(resourceId, revision, body).parse()

internal class StructuredHtmlProseParser(
    internal val resourceId: String,
    internal val revision: String?,
    private val body: Element,
) {
    internal val parsedBlocks = mutableListOf<ParsedBlock>()
    private val usedIds = mutableMapOf<String, Int>()

    fun parse(): PreparedBookDocument {
        collectChildren(body, BookDocumentStyle(), noteContext = false)
        require(parsedBlocks.isNotEmpty()) { "The prose chapter contains no readable document blocks" }

        val combined = SpannableStringBuilder()
        val semanticBlocks = mutableListOf<BookDocumentBlock>()
        val preparedBlocks = mutableListOf<PreparedBookDocumentBlock>()
        val anchors = linkedMapOf<String, BookDocumentPosition>()
        val referencedResources = linkedSetOf<String>()

        parsedBlocks.forEach { parsed ->
            val start = combined.length
            combined.append(parsed.renderedText)
            val end = combined.length
            if (end <= start) return@forEach
            val plainText = parsed.logicalPlainText
            val blockId = uniqueBlockId(parsed.explicitId, parsed.role, plainText, usedIds)
            val links = parsed.renderedText.documentLinks()
            val block = BookDocumentBlock(
                id = blockId,
                role = parsed.role,
                content = parsed.content,
                plainText = plainText,
                sourceFragments = parsed.fragments,
                links = links,
                inlineStyles = parsed.inlineStyles,
                style = parsed.style,
                logicalStart = start,
                logicalEndExclusive = end,
            )
            semanticBlocks += block
            preparedBlocks += PreparedBookDocumentBlock(
                block = block,
                renderedText = parsed.renderedText,
                disclosureBody = parsed.disclosureBody,
            )
            parsed.fragments.forEach { fragment ->
                anchors.putIfAbsent(fragment, BookDocumentPosition(blockId, parsed.anchorOffset(fragment)))
            }
            when (val content = parsed.content) {
                is BookDocumentBlockContent.Figure -> referencedResources += content.image.resourceId
                else -> Unit
            }
            referencedResources += parsed.referencedResources
            (parsed.style.fontFamily as? BookDocumentFontFamily.Resource)
                ?.resourceId
                ?.let(referencedResources::add)
            parsed.inlineStyles.mapNotNullTo(referencedResources) { inline ->
                (inline.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId
            }
        }
        require(semanticBlocks.isNotEmpty()) { "The prose chapter contains no readable document blocks" }

        val document = BookDocument(
            resourceId = resourceId,
            revision = revision,
            blocks = semanticBlocks,
            anchors = anchors,
            resourceIds = referencedResources,
            logicalExtent = combined.length,
        )
        return PreparedBookDocument(document, preparedBlocks, SpannableString(combined))
    }

    internal fun collectChildren(
        parent: Element,
        inheritedStyle: BookDocumentStyle,
        noteContext: Boolean,
    ) {
        val parentStyle = inheritedStyle.merge(parent.documentStyle())
        val parentNoteContext = noteContext || parent.attr("role") == "doc-endnotes"
        val inlineNodes = mutableListOf<Node>()
        var parentFragmentsAssigned = false

        fun flushInline() {
            val readable = inlineNodes.any(Node::hasReadableText)
            if (!readable) {
                inlineNodes.clear()
                return
            }
            val wrapper = Element("p")
            inlineNodes.forEach { wrapper.appendChild(it.clone()) }
            val parentFragments = if (parentFragmentsAssigned) emptyList() else parent.ownFragments()
            addTextBlock(
                element = wrapper,
                role = BookDocumentBlockRole(
                    when {
                        parentNoteContext -> BookDocumentBlockKind.NOTE
                        parentStyle.isMeaningBearingPanel() -> BookDocumentBlockKind.CALLOUT
                        else -> BookDocumentBlockKind.PARAGRAPH
                    },
                ),
                style = parentStyle,
                inheritedFragments = parentFragments,
            )
            parentFragmentsAssigned = parentFragmentsAssigned || parentFragments.isNotEmpty()
            inlineNodes.clear()
        }

        parent.childNodes().toList().forEach { node ->
            when (node) {
                is TextNode -> inlineNodes.add(node)
                is Element -> {
                    if (!node.isBlockElement()) {
                        inlineNodes.add(node)
                        return@forEach
                    }
                    flushInline()
                    val inheritedFragments = if (parentFragmentsAssigned) emptyList() else parent.ownFragments()
                    val added = addBlockElement(
                        element = node,
                        inheritedStyle = parentStyle,
                        noteContext = parentNoteContext,
                        inheritedFragments = inheritedFragments,
                    )
                    if (added && inheritedFragments.isNotEmpty()) parentFragmentsAssigned = true
                }
                else -> inlineNodes.add(node)
            }
        }
        flushInline()
    }
}
