package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.core.text.HtmlCompat
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentAlignment
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockId
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentBorder
import mihon.entry.interactions.book.document.model.BookDocumentBorderStyle
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.model.BookDocumentImage
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyle
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyleRange
import mihon.entry.interactions.book.document.model.BookDocumentLink
import mihon.entry.interactions.book.document.model.BookDocumentListItem
import mihon.entry.interactions.book.document.model.BookDocumentListMarkerStyle
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import mihon.entry.interactions.book.document.model.BookDocumentTableCell
import mihon.entry.interactions.book.document.model.BookDocumentTableCellScope
import mihon.entry.interactions.book.document.model.BookDocumentTableRow
import mihon.entry.interactions.book.document.model.BookDocumentWhiteSpace
import mihon.entry.interactions.book.document.model.MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN
import mihon.entry.interactions.book.document.model.layoutBookDocumentTable
import mihon.entry.interactions.book.document.model.toBookDocumentLinkTarget
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.security.MessageDigest

internal fun prepareStructuredHtmlBookDocument(
    resourceId: String,
    revision: String?,
    body: Element,
): PreparedBookDocument = StructuredHtmlProseParser(resourceId, revision, body).parse()

private class StructuredHtmlProseParser(
    private val resourceId: String,
    private val revision: String?,
    private val body: Element,
) {
    private val parsedBlocks = mutableListOf<ParsedBlock>()
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

    private fun collectChildren(
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

    private fun addBlockElement(
        element: Element,
        inheritedStyle: BookDocumentStyle,
        noteContext: Boolean,
        inheritedFragments: List<String>,
    ): Boolean {
        val before = parsedBlocks.size
        val style = inheritedStyle.merge(element.documentStyle())
        val tag = element.tagName()
        when {
            element.hasAttr("data-katari-unsupported") -> addUnsupportedBlock(element, style, inheritedFragments)
            tag == "hr" -> addThematicBreak(element, style, inheritedFragments)
            tag == "figure" -> addFigure(element, style, inheritedFragments)
            tag == "img" -> addImage(element, style, inheritedFragments)
            tag == "table" -> addTable(element, style, inheritedFragments)
            tag == "details" -> addDisclosure(element, style, inheritedFragments)
            tag == "ol" || tag == "ul" -> addList(element, style, inheritedFragments)
            tag == "pre" -> addPreformatted(element, style, inheritedFragments)
            tag in HEADING_TAGS -> addTextBlock(
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.HEADING, level = tag.drop(1).toInt()),
                style,
                inheritedFragments,
            )
            tag == "blockquote" -> addTextBlock(
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.QUOTE),
                style,
                inheritedFragments,
            )
            tag == "figcaption" || tag == "caption" -> addTextBlock(
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.CAPTION),
                style,
                inheritedFragments,
            )
            tag == "p" -> addParagraph(element, style, noteContext, inheritedFragments)
            tag in CONTAINER_TAGS -> collectChildren(element, style, noteContext)
            else -> addTextBlock(
                element,
                BookDocumentBlockRole(
                    if (noteContext) BookDocumentBlockKind.NOTE else BookDocumentBlockKind.OTHER,
                ),
                style,
                inheritedFragments,
            )
        }
        if (parsedBlocks.size > before && inheritedFragments.isNotEmpty()) {
            val index = before
            parsedBlocks[index] = parsedBlocks[index].copy(
                explicitId = parsedBlocks[index].explicitId ?: inheritedFragments.first(),
                fragments = (inheritedFragments + parsedBlocks[index].fragments).distinct(),
            )
        }
        return parsedBlocks.size > before
    }

    private fun addParagraph(
        element: Element,
        style: BookDocumentStyle,
        noteContext: Boolean,
        inheritedFragments: List<String>,
    ) {
        val pieces = element.splitAroundImages()
        if (pieces.none { it is ParagraphPiece.Image }) {
            addTextBlock(
                element,
                BookDocumentBlockRole(
                    if (noteContext) BookDocumentBlockKind.NOTE else BookDocumentBlockKind.PARAGRAPH,
                ),
                style,
                inheritedFragments,
            )
            return
        }
        val paragraphFragments = element.ownFragments()
        var leadingIdentityAssigned = false
        pieces.forEach { piece ->
            val before = parsedBlocks.size
            val fragments = (inheritedFragments + paragraphFragments)
                .takeUnless { leadingIdentityAssigned }
                .orEmpty()
            when (piece) {
                is ParagraphPiece.Inline -> {
                    piece.element.removeAttr("id")
                    piece.element.removeAttr("name")
                    addTextBlock(
                        piece.element,
                        BookDocumentBlockRole(
                            if (noteContext) BookDocumentBlockKind.NOTE else BookDocumentBlockKind.PARAGRAPH,
                        ),
                        style,
                        fragments,
                    )
                }
                is ParagraphPiece.Image -> addImage(piece.element, style, fragments)
            }
            if (parsedBlocks.size > before && !leadingIdentityAssigned) {
                paragraphFragments.firstOrNull()?.let { explicitId ->
                    parsedBlocks[before] = parsedBlocks[before].copy(explicitId = explicitId)
                }
                leadingIdentityAssigned = true
            }
        }
    }

    private fun addTextBlock(
        element: Element,
        role: BookDocumentBlockRole,
        style: BookDocumentStyle,
        inheritedFragments: List<String>,
    ) {
        if (!element.hasReadableText()) return
        val rendered = when (style.whiteSpace) {
            BookDocumentWhiteSpace.PRE,
            BookDocumentWhiteSpace.PRE_WRAP,
            -> renderPreservedText(element)
            BookDocumentWhiteSpace.NORMAL -> renderHtml(element)
        }
        if (rendered.text.none(Char::isReadableDocumentCharacter)) return
        val styled = rendered.text.withSemanticStyle(style)
        parsedBlocks += ParsedBlock(
            renderedText = styled,
            logicalPlainText = styled.toString().trim(),
            role = role,
            content = BookDocumentBlockContent.Text(
                preformatted = style.whiteSpace != BookDocumentWhiteSpace.NORMAL,
            ),
            style = style,
            explicitId = element.id().ifBlank { null },
            fragments = (inheritedFragments + element.fragments()).distinct(),
            localAnchorOffsets = rendered.anchorOffsets,
            inlineStyles = rendered.inlineStyles,
        )
    }

    private fun addPreformatted(
        element: Element,
        style: BookDocumentStyle,
        inheritedFragments: List<String>,
    ) {
        val rendered = renderPreservedText(element).trimEnd().withParagraphTerminator()
        if (rendered.text.none(Char::isReadableDocumentCharacter)) return
        val preStyle = style.copy(
            whiteSpace = BookDocumentWhiteSpace.PRE,
            fontFamily = BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.MONOSPACE),
        )
        parsedBlocks += ParsedBlock(
            renderedText = rendered.text,
            logicalPlainText = rendered.text.toString().trim(),
            role = BookDocumentBlockRole(BookDocumentBlockKind.PREFORMATTED),
            content = BookDocumentBlockContent.Text(preformatted = true),
            style = preStyle,
            explicitId = element.id().ifBlank { null },
            fragments = (inheritedFragments + element.fragments()).distinct(),
            localAnchorOffsets = rendered.anchorOffsets,
        )
    }

    private fun addThematicBreak(
        element: Element,
        style: BookDocumentStyle,
        inheritedFragments: List<String>,
    ) {
        parsedBlocks += ParsedBlock(
            renderedText = SpannableString(OBJECT_REPLACEMENT_TEXT),
            logicalPlainText = "",
            role = BookDocumentBlockRole(BookDocumentBlockKind.THEMATIC_BREAK),
            content = BookDocumentBlockContent.ThematicBreak,
            style = style,
            explicitId = element.id().ifBlank { null },
            fragments = (inheritedFragments + element.fragments()).distinct(),
        )
    }

    private fun addUnsupportedBlock(
        element: Element,
        style: BookDocumentStyle,
        inheritedFragments: List<String>,
    ) {
        val text = UNSUPPORTED_CONTENT_BLOCK_TEXT.withParagraphTerminator()
        parsedBlocks += ParsedBlock(
            renderedText = SpannableString(text),
            logicalPlainText = UNSUPPORTED_CONTENT_BLOCK_TEXT,
            role = BookDocumentBlockRole(BookDocumentBlockKind.UNSUPPORTED),
            content = BookDocumentBlockContent.Unsupported(
                element.attr("data-katari-unsupported").take(MAX_DIAGNOSTIC_LENGTH),
            ),
            style = style,
            explicitId = element.id().ifBlank { null },
            fragments = (inheritedFragments + element.fragments()).distinct(),
        )
    }

    private fun addImage(
        element: Element,
        style: BookDocumentStyle,
        inheritedFragments: List<String>,
        caption: String? = null,
        figureElement: Element = element,
    ) {
        val resource = element.attr("src").trim()
        val alt = element.attr("alt").trim().ifBlank {
            element.attr("title").trim().ifBlank { null }
        }
        if (resource.isBlank()) {
            val fallback = alt ?: IMAGE_UNAVAILABLE_TEXT
            val rendered = SpannableString(fallback.withParagraphTerminator())
            parsedBlocks += ParsedBlock(
                renderedText = rendered,
                logicalPlainText = fallback,
                role = BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
                content = BookDocumentBlockContent.Text(),
                style = style,
                explicitId = figureElement.id().ifBlank { null },
                fragments = (inheritedFragments + figureElement.fragments()).distinct(),
            )
            return
        }
        val image = BookDocumentImage(
            resourceId = resource,
            alternativeText = alt,
            width = element.positiveDimension("width"),
            height = element.positiveDimension("height"),
        )
        val logicalText = listOfNotNull(alt ?: IMAGE_UNAVAILABLE_TEXT, caption)
            .joinToString("\n")
            .withParagraphTerminator()
        parsedBlocks += ParsedBlock(
            renderedText = SpannableString(logicalText),
            logicalPlainText = logicalText.trim(),
            role = BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
            content = BookDocumentBlockContent.Figure(image, caption),
            style = style,
            explicitId = figureElement.id().ifBlank { null },
            fragments = (inheritedFragments + figureElement.fragments()).distinct(),
        )
    }

    private fun addFigure(
        element: Element,
        style: BookDocumentStyle,
        inheritedFragments: List<String>,
    ) {
        val image = element.selectFirst("img")
        val caption = element.selectFirst("figcaption")?.text()?.trim()?.ifBlank { null }
        if (image == null) {
            addTextBlock(
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.FIGURE),
                style,
                inheritedFragments,
            )
            return
        }
        addImage(image, style, inheritedFragments, caption, element)
    }

    private fun addList(
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

    private fun collectListItems(
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

    private fun addTable(
        element: Element,
        style: BookDocumentStyle,
        inheritedFragments: List<String>,
    ) {
        val parsedRows = element.select("tr").take(MAX_TABLE_ROWS).mapNotNull { row ->
            val cells = row.children()
                .filter { it.tagName() == "th" || it.tagName() == "td" }
                .take(MAX_TABLE_CELLS_PER_ROW)
                .map { cell ->
                    val rendered = renderHtml(cell).trim()
                    val text = rendered.text.toString()
                    ParsedTableCell(
                        model = BookDocumentTableCell(
                            text = text,
                            header = cell.tagName() == "th",
                            scope = cell.attr("scope").toTableScope(),
                            columnSpan = cell.attr("colspan").toIntOrNull()
                                ?.coerceIn(1, MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN)
                                ?: 1,
                            rowSpan = cell.attr("rowspan").toIntOrNull()
                                ?.coerceIn(1, MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN)
                                ?: 1,
                            links = rendered.text.documentLinks(),
                        ),
                        renderedText = rendered.text,
                        anchorOffsets = rendered.anchorOffsets,
                        inlineStyles = rendered.inlineStyles,
                    )
                }
            cells.takeIf(List<*>::isNotEmpty)?.let {
                ParsedTableRow(
                    model = BookDocumentTableRow(cells.map(ParsedTableCell::model)),
                    cells = cells,
                    fragments = row.ownFragments(),
                )
            }
        }
        val rows = parsedRows.map(ParsedTableRow::model)
        val tableLayout = rows.layoutBookDocumentTable()
        val columnCount = tableLayout?.columnCount ?: 0
        if (
            rows.isEmpty() ||
            columnCount !in 1..MAX_TABLE_COLUMNS ||
            element.select("tr").size > MAX_TABLE_ROWS
        ) {
            addTextBlock(
                element,
                BookDocumentBlockRole(BookDocumentBlockKind.OTHER),
                style,
                inheritedFragments,
            )
            return
        }
        val captionRendered = element.selectFirst("caption")?.let(::renderHtml)?.trim()
            ?.takeIf { it.text.any(Char::isReadableDocumentCharacter) }
        val caption = captionRendered?.text?.toString()
        val anchorOffsets = linkedMapOf<String, Int>().apply {
            element.ownFragments().forEach { put(it, 0) }
        }
        val inlineStyles = mutableListOf<BookDocumentInlineStyleRange>()
        val text = SpannableStringBuilder().apply {
            captionRendered?.let { rendered ->
                val captionStart = length
                append(rendered.text)
                rendered.anchorOffsets.forEach { (fragment, offset) ->
                    anchorOffsets.putIfAbsent(fragment, captionStart + offset)
                }
                rendered.inlineStyles.forEach { inline ->
                    inlineStyles += inline.shifted(captionStart)
                }
                append('\n')
            }
            parsedRows.forEach { row ->
                val rowStart = length
                row.fragments.forEach { fragment -> anchorOffsets.putIfAbsent(fragment, rowStart) }
                row.cells.forEachIndexed { index, cell ->
                    if (index > 0) append('\t')
                    val cellStart = length
                    append(cell.renderedText)
                    cell.anchorOffsets.forEach { (fragment, offset) ->
                        anchorOffsets.putIfAbsent(fragment, cellStart + offset)
                    }
                    cell.inlineStyles.forEach { inline ->
                        inlineStyles += inline.shifted(cellStart)
                    }
                }
                append('\n')
            }
        }
        val rendered = RenderedFragment(SpannableString(text), anchorOffsets)
            .trimEnd()
            .withParagraphTerminator()
        parsedBlocks += ParsedBlock(
            renderedText = rendered.text,
            logicalPlainText = rendered.text.toString().trim(),
            role = BookDocumentBlockRole(BookDocumentBlockKind.TABLE),
            content = BookDocumentBlockContent.Table(
                caption = caption,
                captionLinks = captionRendered?.text?.documentLinks().orEmpty(),
                rows = rows,
                columnCount = columnCount,
            ),
            style = style,
            explicitId = element.id().ifBlank { null },
            fragments = (inheritedFragments + element.fragments()).distinct(),
            localAnchorOffsets = rendered.anchorOffsets,
            inlineStyles = inlineStyles,
        )
    }

    private fun addDisclosure(
        element: Element,
        style: BookDocumentStyle,
        inheritedFragments: List<String>,
    ) {
        val summaryElement = element.children().firstOrNull { it.tagName() == "summary" }
        val summary = summaryElement?.text()?.trim()?.ifBlank { null } ?: DISCLOSURE_SUMMARY_FALLBACK
        val bodyElement = element.clone().also { clone ->
            clone.children().firstOrNull { it.tagName() == "summary" }?.remove()
            clone.removeDocumentStyleAttributes()
        }
        val body = runCatching {
            StructuredHtmlProseParser(
                resourceId = "$resourceId#disclosure",
                revision = revision,
                body = bodyElement,
            ).parse()
        }.getOrNull()
        if (body == null) {
            addTextBlock(
                summaryElement ?: element,
                BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
                style,
                inheritedFragments,
            )
            return
        }
        val summaryPrefix = "$summary\n"
        val rendered = SpannableStringBuilder(summaryPrefix).append(body.combinedText)
        if (!rendered.endsWith("\n\n")) rendered.append("\n\n")
        val localAnchors = buildMap {
            element.ownFragments().forEach { put(it, 0) }
            body.document.anchors.forEach { (fragment, position) ->
                val bodyOffset = body.document.logicalOffset(position) ?: return@forEach
                putIfAbsent(fragment, summaryPrefix.length + bodyOffset)
            }
        }
        parsedBlocks += ParsedBlock(
            renderedText = SpannableString(rendered),
            logicalPlainText = rendered.toString().trim(),
            role = BookDocumentBlockRole(BookDocumentBlockKind.DISCLOSURE),
            content = BookDocumentBlockContent.Disclosure(
                summary = summary,
                body = body.document.blocks,
                initiallyExpanded = element.hasAttr("open"),
            ),
            style = style,
            explicitId = element.id().ifBlank { null },
            fragments = (inheritedFragments + element.fragments()).distinct(),
            localAnchorOffsets = localAnchors,
            disclosureBody = body.blocks,
            referencedResources = body.document.resourceIds,
        )
    }
}

private data class ParsedBlock(
    val renderedText: Spanned,
    val logicalPlainText: String,
    val role: BookDocumentBlockRole,
    val content: BookDocumentBlockContent,
    val style: BookDocumentStyle,
    val explicitId: String?,
    val fragments: List<String>,
    val localAnchorOffsets: Map<String, Int> = emptyMap(),
    val inlineStyles: List<BookDocumentInlineStyleRange> = emptyList(),
    val disclosureBody: List<PreparedBookDocumentBlock> = emptyList(),
    val referencedResources: Set<String> = emptySet(),
) {
    fun anchorOffset(fragment: String): Int =
        localAnchorOffsets[fragment]?.coerceIn(0, renderedText.length) ?: 0
}

private data class ParsedListItem(
    val model: BookDocumentListItem,
    val renderedText: Spanned,
    val anchorOffsets: Map<String, Int>,
    val inlineStyles: List<BookDocumentInlineStyleRange>,
)

private data class ParsedTableRow(
    val model: BookDocumentTableRow,
    val cells: List<ParsedTableCell>,
    val fragments: List<String>,
)

private data class ParsedTableCell(
    val model: BookDocumentTableCell,
    val renderedText: Spanned,
    val anchorOffsets: Map<String, Int>,
    val inlineStyles: List<BookDocumentInlineStyleRange>,
)

private sealed interface ParagraphPiece {
    data class Inline(val element: Element) : ParagraphPiece
    data class Image(val element: Element) : ParagraphPiece
}

private fun Element.splitAroundImages(): List<ParagraphPiece> {
    return splitNodeAroundImages().map { piece ->
        when (piece) {
            is SplitNodePiece.Inline -> ParagraphPiece.Inline(piece.node as Element)
            is SplitNodePiece.Image -> ParagraphPiece.Image(piece.element)
        }
    }
}

private sealed interface SplitNodePiece {
    data class Inline(val node: Node) : SplitNodePiece
    data class Image(val element: Element) : SplitNodePiece
}

private fun Node.splitNodeAroundImages(): List<SplitNodePiece> = when {
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

private data class RenderedFragment(
    val text: Spanned,
    val anchorOffsets: Map<String, Int>,
    val inlineStyles: List<BookDocumentInlineStyleRange> = emptyList(),
) {
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

private data class AnchorMarker(
    val token: String,
    val fragments: List<String>,
)

private data class InlineStyleMarker(
    val startToken: String,
    val endToken: String,
    val style: BookDocumentInlineStyle,
)

private fun renderHtml(element: Element): RenderedFragment {
    val (anchored, anchorMarkers) = element.withAnchorMarkers()
    val (marked, styleMarkers) = anchored.withInlineStyleMarkers()
    val rendered = SpannableStringBuilder(
        HtmlCompat.fromHtml(marked.outerHtml(), HtmlCompat.FROM_HTML_MODE_LEGACY),
    )
    normalizeParagraphBreaks(rendered)
    val (anchorOffsets, inlineStyles) = rendered.removeDocumentMarkers(anchorMarkers, styleMarkers)
    if (!rendered.endsWith("\n\n")) rendered.append("\n\n")
    return RenderedFragment(SpannableString(rendered), anchorOffsets, inlineStyles)
}

private fun renderPreservedText(element: Element): RenderedFragment {
    val (marked, markers) = element.withAnchorMarkers()
    val rendered = SpannableStringBuilder(marked.wholeText())
    val (anchorOffsets, _) = rendered.removeDocumentMarkers(markers, emptyList())
    return RenderedFragment(SpannableString(rendered), anchorOffsets).withParagraphTerminator()
}

private fun Element.withAnchorMarkers(): Pair<Element, List<AnchorMarker>> {
    val marked = clone()
    val markers = buildList {
        val anchors = buildList {
            add(marked)
            marked.select("[id], a[name]").filterTo(this) { it !== marked }
        }
        anchors.forEach { anchor ->
            val fragments = anchor.ownFragments()
            if (fragments.isEmpty()) return@forEach
            val token = "$ANCHOR_MARKER_START${size.toString(36)}$ANCHOR_MARKER_END"
            anchor.prependText(token)
            add(AnchorMarker(token, fragments))
        }
    }
    return marked to markers
}

private fun Element.withInlineStyleMarkers(): Pair<Element, List<InlineStyleMarker>> {
    val marked = clone()
    val markers = buildList {
        marked.select("*")
            .filterNot(Element::isBlockElement)
            .forEach { inline ->
                val style = inline.documentInlineStyle() ?: return@forEach
                val index = size.toString(36)
                val startToken = "$INLINE_STYLE_MARKER_START$index+$INLINE_STYLE_MARKER_END"
                val endToken = "$INLINE_STYLE_MARKER_START$index-$INLINE_STYLE_MARKER_END"
                inline.prependText(startToken)
                inline.appendText(endToken)
                add(InlineStyleMarker(startToken, endToken, style))
            }
    }
    return marked to markers
}

private fun SpannableStringBuilder.removeDocumentMarkers(
    anchorMarkers: List<AnchorMarker>,
    styleMarkers: List<InlineStyleMarker>,
): Pair<Map<String, Int>, List<BookDocumentInlineStyleRange>> {
    val anchorPositions = anchorMarkers.mapNotNull { marker ->
        indexOf(marker.token).takeIf { it >= 0 }?.let { marker to it }
    }
    val stylePositions = styleMarkers.mapNotNull { marker ->
        val start = indexOf(marker.startToken)
        val end = indexOf(marker.endToken)
        if (start < 0 || end < start) null else Triple(marker, start, end)
    }
    val tokenRanges = buildList {
        anchorPositions.forEach { (marker, position) ->
            add(position until position + marker.token.length)
        }
        stylePositions.forEach { (marker, start, end) ->
            add(start until start + marker.startToken.length)
            add(end until end + marker.endToken.length)
        }
    }
    fun cleanedOffset(rawOffset: Int): Int =
        rawOffset - tokenRanges.filter { it.first < rawOffset }.sumOf(IntRange::count)

    val anchors = buildMap {
        anchorPositions.forEach { (marker, position) ->
            val offset = cleanedOffset(position)
            marker.fragments.forEach { putIfAbsent(it, offset) }
        }
    }
    val styles = stylePositions.mapNotNull { (marker, start, end) ->
        val cleanedStart = cleanedOffset(start)
        val cleanedEnd = cleanedOffset(end)
        if (cleanedEnd <= cleanedStart) {
            null
        } else {
            BookDocumentInlineStyleRange(cleanedStart, cleanedEnd, marker.style)
        }
    }
    tokenRanges.sortedByDescending(IntRange::first).forEach { range ->
        delete(range.first, range.last + 1)
    }
    return anchors to styles
}

private fun Spanned.documentLinks(): List<BookDocumentLink> =
    getSpans(0, length, URLSpan::class.java).mapNotNull { span ->
        val target = span.url.toBookDocumentLinkTarget() ?: return@mapNotNull null
        BookDocumentLink(
            start = getSpanStart(span),
            endExclusive = getSpanEnd(span),
            target = target,
        )
    }

private fun Spanned.withSemanticStyle(style: BookDocumentStyle): Spanned {
    if (!style.bold || isEmpty()) return this
    return SpannableString(this).apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

private fun Element.documentStyle(): BookDocumentStyle {
    val border = attr("data-katari-border")
        .split('|')
        .takeIf { it.size >= 2 }
        ?.let { parts ->
            val width = parts[0].toFloatOrNull() ?: return@let null
            val borderStyle = when (parts[1]) {
                "dashed" -> BookDocumentBorderStyle.DASHED
                "dotted" -> BookDocumentBorderStyle.DOTTED
                else -> BookDocumentBorderStyle.SOLID
            }
            BookDocumentBorder(
                widthDp = width.coerceIn(0.5f, 8f),
                colorArgb = parts.getOrNull(2)?.toArgbLong(),
                style = borderStyle,
            )
        }
    val fontFamily = when {
        hasAttr("data-katari-font-resource") ->
            BookDocumentFontFamily.Resource(attr("data-katari-font-resource"))
        attr("data-katari-font-generic") == "sans-serif" ->
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.SANS_SERIF)
        attr("data-katari-font-generic") == "monospace" ->
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.MONOSPACE)
        attr("data-katari-font-generic") == "serif" ->
            BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.SERIF)
        else -> null
    }
    return BookDocumentStyle(
        alignment = when (attr("data-katari-align")) {
            "center" -> BookDocumentAlignment.CENTER
            "right", "end" -> BookDocumentAlignment.END
            "left", "start" -> BookDocumentAlignment.START
            else -> null
        },
        whiteSpace = when (attr("data-katari-white-space")) {
            "pre" -> BookDocumentWhiteSpace.PRE
            "pre-wrap" -> BookDocumentWhiteSpace.PRE_WRAP
            else -> BookDocumentWhiteSpace.NORMAL
        },
        foregroundArgb = attr("data-katari-color").toArgbLong(),
        backgroundArgb = attr("data-katari-background").toArgbLong(),
        border = border,
        paddingEm = attr("data-katari-padding-em").toFloatOrNull()?.coerceIn(0f, 4f) ?: 0f,
        fontFamily = fontFamily,
        fontSizeScale = attr("data-katari-font-scale").toFloatOrNull()?.coerceIn(0.75f, 1.5f) ?: 1f,
        bold = attr("data-katari-bold") == "true",
    )
}

private fun Element.documentInlineStyle(): BookDocumentInlineStyle? {
    val style = documentStyle()
    val foreground = style.foregroundArgb.takeIf { hasAttr("data-katari-color") }
    val background = style.backgroundArgb.takeIf { hasAttr("data-katari-background") }
    val fontFamily = style.fontFamily.takeIf {
        hasAttr("data-katari-font-resource") || hasAttr("data-katari-font-generic")
    }
    val fontSizeScale = style.fontSizeScale.takeIf { hasAttr("data-katari-font-scale") }
    val bold = hasAttr("data-katari-bold") && style.bold
    if (foreground == null && background == null && fontFamily == null && fontSizeScale == null && !bold) {
        return null
    }
    return BookDocumentInlineStyle(
        foregroundArgb = foreground,
        backgroundArgb = background,
        fontFamily = fontFamily,
        fontSizeScale = fontSizeScale,
        bold = bold,
    )
}

private fun BookDocumentInlineStyleRange.shifted(offset: Int) =
    copy(start = start + offset, endExclusive = endExclusive + offset)

private fun BookDocumentInlineStyleRange.clippedAndShifted(
    sliceStart: Int,
    sliceEndExclusive: Int,
): BookDocumentInlineStyleRange? {
    val clippedStart = maxOf(start, sliceStart)
    val clippedEnd = minOf(endExclusive, sliceEndExclusive)
    if (clippedEnd <= clippedStart) return null
    return copy(
        start = clippedStart - sliceStart,
        endExclusive = clippedEnd - sliceStart,
    )
}

private fun Element.removeDocumentStyleAttributes() {
    DOCUMENT_STYLE_ATTRIBUTES.forEach(::removeAttr)
}

private fun BookDocumentStyle.merge(child: BookDocumentStyle): BookDocumentStyle = BookDocumentStyle(
    alignment = child.alignment ?: alignment,
    whiteSpace = child.whiteSpace.takeUnless { it == BookDocumentWhiteSpace.NORMAL } ?: whiteSpace,
    foregroundArgb = child.foregroundArgb ?: foregroundArgb,
    backgroundArgb = child.backgroundArgb ?: backgroundArgb,
    border = child.border ?: border,
    paddingEm = child.paddingEm.takeUnless { it == 0f } ?: paddingEm,
    fontFamily = child.fontFamily ?: fontFamily,
    fontSizeScale = child.fontSizeScale.takeUnless { it == 1f } ?: fontSizeScale,
    bold = child.bold || bold,
)

private fun BookDocumentStyle.isMeaningBearingPanel(): Boolean =
    backgroundArgb != null || border != null

private fun String.toArgbLong(): Long? {
    if (!matches(Regex("""#[0-9a-fA-F]{8}"""))) return null
    return drop(1).toLongOrNull(16)
}

private fun Element.fragments(): List<String> =
    (listOf(this) + select("[id], a[name]"))
        .flatMap { element -> listOf(element.id(), element.attr("name")) }
        .filter(String::isNotBlank)
        .distinct()

private fun Element.ownFragments(): List<String> =
    listOf(id(), attr("name")).filter(String::isNotBlank).distinct()

private fun Element.positiveDimension(attribute: String): Int? =
    attr(attribute).toIntOrNull()?.takeIf { it in 1..MAX_IMAGE_DIMENSION }

private fun Element.listMarkerStyle(): BookDocumentListMarkerStyle {
    if (tagName() != "ol") return BookDocumentListMarkerStyle.BULLET
    return when (attr("type")) {
        "a" -> BookDocumentListMarkerStyle.LOWER_ALPHA
        "A" -> BookDocumentListMarkerStyle.UPPER_ALPHA
        "i" -> BookDocumentListMarkerStyle.LOWER_ROMAN
        "I" -> BookDocumentListMarkerStyle.UPPER_ROMAN
        else -> BookDocumentListMarkerStyle.DECIMAL
    }
}

private fun BookDocumentListMarkerStyle.marker(value: Int): String = when (this) {
    BookDocumentListMarkerStyle.DECIMAL -> "$value."
    BookDocumentListMarkerStyle.LOWER_ALPHA -> "${value.toAlphabetic().lowercase()}."
    BookDocumentListMarkerStyle.UPPER_ALPHA -> "${value.toAlphabetic()}."
    BookDocumentListMarkerStyle.LOWER_ROMAN -> "${value.toRoman().lowercase()}."
    BookDocumentListMarkerStyle.UPPER_ROMAN -> "${value.toRoman()}."
    BookDocumentListMarkerStyle.BULLET -> "•"
}

private fun Int.toAlphabetic(): String {
    if (this <= 0) return toString()
    var remaining = this
    return buildString {
        while (remaining > 0) {
            remaining--
            insert(0, ('A'.code + remaining % 26).toChar())
            remaining /= 26
        }
    }
}

private fun Int.toRoman(): String {
    if (this !in 1..3_999) return toString()
    var remaining = this
    return buildString {
        ROMAN_NUMERALS.forEach { (number, numeral) ->
            while (remaining >= number) {
                append(numeral)
                remaining -= number
            }
        }
    }
}

private fun String.toTableScope(): BookDocumentTableCellScope? = when (lowercase()) {
    "row" -> BookDocumentTableCellScope.ROW
    "col" -> BookDocumentTableCellScope.COLUMN
    "rowgroup" -> BookDocumentTableCellScope.ROW_GROUP
    "colgroup" -> BookDocumentTableCellScope.COLUMN_GROUP
    else -> null
}

private fun Node.hasReadableText(): Boolean = when (this) {
    is TextNode -> text().any(Char::isReadableDocumentCharacter)
    is Element -> text().any(Char::isReadableDocumentCharacter)
    else -> false
}

private fun Char.isReadableDocumentCharacter(): Boolean =
    !isWhitespace() && this != '\u00A0' && this != '\u200B' && this != '\uFFFC'

private fun Element.isBlockElement(): Boolean =
    tagName() in BLOCK_TAGS || hasAttr("data-katari-unsupported")

private fun String.withParagraphTerminator(): String = trimEnd() + "\n\n"

private fun normalizeParagraphBreaks(parsed: SpannableStringBuilder) {
    var index = parsed.length - 1
    while (index >= 0) {
        if (parsed[index] == '\n') {
            val end = index + 1
            while (index >= 0 && parsed[index] == '\n') index--
            val start = index + 1
            if (end - start >= 2) parsed.replace(start, end, "\n\n")
        } else {
            index--
        }
    }
}

private fun uniqueBlockId(
    explicitId: String?,
    role: BookDocumentBlockRole,
    plainText: String,
    usedIds: MutableMap<String, Int>,
): BookDocumentBlockId {
    val base = explicitId ?: buildString {
        append("auto:")
        append(role.kind.name.lowercase())
        append(':')
        append(
            sha256(
                "${role.kind.name}:${role.level?.toString().orEmpty()}:${role.depth}:" +
                    "${role.ordered?.toString().orEmpty()}\u0000$plainText",
            ).take(BLOCK_HASH_LENGTH),
        )
    }
    val occurrence = usedIds.getOrDefault(base, 0)
    usedIds[base] = occurrence + 1
    return BookDocumentBlockId(if (occurrence == 0) base else "$base:$occurrence")
}

private fun sha256(value: String): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(value.encodeToByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
private val DOCUMENT_STYLE_ATTRIBUTES = setOf(
    "data-katari-align",
    "data-katari-background",
    "data-katari-bold",
    "data-katari-border",
    "data-katari-color",
    "data-katari-font-generic",
    "data-katari-font-resource",
    "data-katari-font-scale",
    "data-katari-padding-em",
    "data-katari-white-space",
)
private val CONTAINER_TAGS = setOf("article", "aside", "body", "div", "dl", "section")
private val BLOCK_TAGS = HEADING_TAGS + CONTAINER_TAGS + setOf(
    "blockquote",
    "caption",
    "dd",
    "details",
    "dt",
    "figcaption",
    "figure",
    "hr",
    "img",
    "ol",
    "p",
    "pre",
    "table",
    "ul",
)
private val ROMAN_NUMERALS = listOf(
    1_000 to "M",
    900 to "CM",
    500 to "D",
    400 to "CD",
    100 to "C",
    90 to "XC",
    50 to "L",
    40 to "XL",
    10 to "X",
    9 to "IX",
    5 to "V",
    4 to "IV",
    1 to "I",
)
private const val OBJECT_REPLACEMENT_TEXT = "\uFFFC\n\n"
private const val IMAGE_UNAVAILABLE_TEXT = "Image unavailable"
private const val DISCLOSURE_SUMMARY_FALLBACK = "Additional content"
private const val MAX_DIAGNOSTIC_LENGTH = 64
private const val MAX_LIST_DEPTH = 8
private const val MAX_TABLE_ROWS = 200
private const val MAX_TABLE_COLUMNS = 24
private const val MAX_TABLE_CELLS_PER_ROW = 24
private const val MAX_IMAGE_DIMENSION = 32_768
private const val BLOCK_HASH_LENGTH = 16
private const val ANCHOR_MARKER_START = '\uE000'
private const val ANCHOR_MARKER_END = '\uE001'
private const val INLINE_STYLE_MARKER_START = '\uE002'
private const val INLINE_STYLE_MARKER_END = '\uE003'
