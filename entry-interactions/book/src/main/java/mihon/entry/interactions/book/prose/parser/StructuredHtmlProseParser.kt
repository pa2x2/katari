package mihon.entry.interactions.book.prose

import android.text.SpannableString
import android.text.SpannableStringBuilder
import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentPosition
import mihon.book.api.document.BookDocumentStyle
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import mihon.entry.interactions.book.document.render.toBookDocumentSpanned
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
        val usedFragments = mutableSetOf<String>()

        parsedBlocks.forEach { parsed ->
            val start = combined.length
            combined.append(parsed.renderedText)
            val end = combined.length
            if (end <= start) return@forEach
            val plainText = parsed.logicalPlainText
            val blockId = uniqueBlockId(parsed.explicitId, parsed.role, plainText, usedIds)
            val sourceFragments = parsed.fragments.filter(usedFragments::add)
            val block = BookDocumentBlock(
                id = blockId,
                role = parsed.role,
                content = parsed.content,
                plainText = plainText,
                sourceFragments = sourceFragments,
                style = parsed.style,
                logicalStart = start,
                logicalEndExclusive = end,
            )
            semanticBlocks += block
            preparedBlocks += PreparedBookDocumentBlock(
                block = block,
                renderedText = parsed.renderedText.toString().toBookDocumentSpanned(block),
                disclosureBody = parsed.disclosureBody,
            )
            sourceFragments.forEach { fragment ->
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
            block.inlineStyles.mapNotNullTo(referencedResources) { inline ->
                (inline.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId
            }
        }
        require(semanticBlocks.isNotEmpty()) { "The prose chapter contains no readable document blocks" }

        val document = BookDocument(
            resourceId = resourceId,
            revision = revision,
            content = BookDocumentContent(
                text = combined.toString(),
                blocks = semanticBlocks,
                anchors = anchors,
                resourceIds = referencedResources,
            ),
        )
        val projectedText = SpannableStringBuilder().apply {
            preparedBlocks.forEach { append(it.renderedText) }
        }
        return PreparedBookDocument(document, preparedBlocks, SpannableString(projectedText))
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
