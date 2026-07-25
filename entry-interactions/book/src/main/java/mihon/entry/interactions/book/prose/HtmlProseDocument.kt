package mihon.entry.interactions.book.prose

import android.text.Layout
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.text.HtmlCompat
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockId
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import tachiyomi.domain.entry.model.EntryChapter
import java.security.MessageDigest

internal typealias HtmlProseLoadedChapter = BookDocumentSection<EntryChapter>

internal data class HtmlProsePage(
    val chapter: EntryChapter,
    val index: Int,
    val total: Int,
    val text: Spanned,
    val progression: Float,
    val sourceStart: Int = 0,
    val sourceEndExclusive: Int = sourceStart + text.length,
)

/**
 * Adapts HTML prose into the internal processor-neutral structured BOOK document.
 *
 * HTML and Jsoup stay on this producer side of the boundary; the resulting semantic model is intentionally suitable
 * for eventual promotion to `book-api` after its contract has been validated by another structured BOOK processor.
 */
internal fun prepareHtmlBookDocument(
    resourceId: String,
    revision: String?,
    bodyHtml: String,
): PreparedBookDocument = prepareHtmlBookDocument(
    resourceId = resourceId,
    revision = revision,
    body = Jsoup.parseBodyFragment(bodyHtml).body(),
)

internal fun prepareHtmlBookDocument(
    resourceId: String,
    revision: String?,
    body: Element,
): PreparedBookDocument {
    val markedBody = body.clone()
    val sources = buildList { collectBlockSources(markedBody) }
    val anchorSources = markAnchors(markedBody)
    val parsed = SpannableStringBuilder(
        HtmlCompat.fromHtml(markedBody.html(), HtmlCompat.FROM_HTML_MODE_LEGACY),
    )
    val blockMarkers = sources.map { source -> parsed.trackAndRemove(source.marker) }
    val anchorMarkers = anchorSources.map { source -> source to parsed.trackAndRemove(source.marker) }
    normalizeParagraphBreaks(parsed)
    require(parsed.any { !it.isWhitespace() }) { "The prose chapter contains no readable document blocks" }
    val blockStarts = blockMarkers.map(parsed::getSpanStart)
    val anchorPositions = anchorMarkers.map { (source, marker) -> source to parsed.getSpanStart(marker) }
    require(blockStarts.none { it < 0 } && anchorPositions.none { (_, position) -> position < 0 }) {
        "Unable to preserve an HTML document boundary"
    }
    blockMarkers.forEach(parsed::removeSpan)
    anchorMarkers.forEach { (_, marker) -> parsed.removeSpan(marker) }

    val semanticBlocks = mutableListOf<BookDocumentBlock>()
    val preparedBlocks = mutableListOf<PreparedBookDocumentBlock>()
    val usedIds = mutableMapOf<String, Int>()
    val sourceBlocks = mutableMapOf<Int, BookDocumentBlock>()

    sources.forEachIndexed { index, source ->
        val logicalStart = if (index == 0) 0 else blockStarts[index]
        val logicalEnd = blockStarts.getOrNull(index + 1) ?: parsed.length
        if (logicalEnd <= logicalStart) return@forEachIndexed
        val renderedText = SpannableString(parsed.subSequence(logicalStart, logicalEnd))
        val plainText = parsed.subSequence(logicalStart, logicalEnd).toString().trim()
        val id = uniqueBlockId(source.explicitId, source.role, plainText, usedIds)
        val block = BookDocumentBlock(
            id = id,
            role = source.role,
            plainText = plainText,
            sourceFragments = source.sourceFragments,
            logicalStart = logicalStart,
            logicalEndExclusive = logicalEnd,
        )
        semanticBlocks += block
        preparedBlocks += PreparedBookDocumentBlock(block, renderedText)
        sourceBlocks[index] = block
    }
    require(semanticBlocks.isNotEmpty()) { "The prose chapter contains no readable document blocks" }

    val anchors = linkedMapOf<String, BookDocumentPosition>()
    anchorPositions.forEach { (source, markerPosition) ->
        val absolute = markerPosition.coerceIn(0, parsed.length)
        val sourceBlock = sources.indexOfFirst { source.ids.any(it.sourceFragments::contains) }
            .takeIf { it >= 0 }
            ?.let(sourceBlocks::get)
        val block = sourceBlock ?: semanticBlocks.lastOrNull { absolute >= it.logicalStart } ?: semanticBlocks.first()
        source.ids.forEach { anchor ->
            anchors.putIfAbsent(
                anchor,
                BookDocumentPosition(
                    blockId = block.id,
                    offsetWithinBlock = (absolute - block.logicalStart).coerceIn(0, block.logicalLength),
                ),
            )
        }
    }
    val anchorFragmentsByBlock = anchors.entries.groupBy(
        keySelector = { (_, position) -> position.blockId },
        valueTransform = Map.Entry<String, BookDocumentPosition>::key,
    )
    val semanticBlocksWithFragments = semanticBlocks.map { block ->
        block.copy(
            sourceFragments = (block.sourceFragments + anchorFragmentsByBlock[block.id].orEmpty()).distinct(),
        )
    }
    val blocksById = semanticBlocksWithFragments.associateBy(BookDocumentBlock::id)
    preparedBlocks.replaceAll { prepared ->
        PreparedBookDocumentBlock(
            block = checkNotNull(blocksById[prepared.block.id]),
            renderedText = prepared.renderedText,
        )
    }

    val document = BookDocument(
        resourceId = resourceId,
        revision = revision,
        blocks = semanticBlocksWithFragments,
        anchors = anchors,
        logicalExtent = parsed.length,
    )
    return PreparedBookDocument(document, preparedBlocks, SpannableString(parsed))
}

private fun MutableList<HtmlBlockSource>.collectBlockSources(parent: Element) {
    val firstAddedIndex = size
    val inline = mutableListOf<Node>()

    fun flushInline() {
        if (inline.none(Node::hasReadableText)) {
            inline.clear()
            return
        }
        val marker = blockMarker(size)
        inline.first().prependMarker(marker)
        add(
            HtmlBlockSource(
                marker = marker,
                role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
            ),
        )
        inline.clear()
    }

    parent.childNodes().toList().forEach { node ->
        when (node) {
            is TextNode -> inline.add(node)
            is Element -> when {
                node.tagName() in ATOMIC_BLOCK_TAGS -> {
                    flushInline()
                    if (node.hasReadableText()) {
                        addMarkedElement(node)
                    }
                }
                node.tagName() in BLOCK_CONTAINER_TAGS -> {
                    flushInline()
                    collectBlockSources(node)
                }
                else -> inline.add(node)
            }
            else -> inline.add(node)
        }
    }
    flushInline()

    val containerFragments = listOf(parent.id(), parent.attr("name"))
        .filter(String::isNotBlank)
        .distinct()
    if (containerFragments.isNotEmpty() && firstAddedIndex < size) {
        val first = this[firstAddedIndex]
        this[firstAddedIndex] = first.copy(
            explicitId = first.explicitId ?: containerFragments.first(),
            sourceFragments = (containerFragments + first.sourceFragments).distinct(),
        )
    }
}

private fun MutableList<HtmlBlockSource>.addMarkedElement(element: Element) {
    val marker = blockMarker(size)
    if (element.tagName() == "ol" || element.tagName() == "ul") {
        element.children().firstOrNull { it.tagName() == "li" }?.prependText(marker)
            ?: element.prependText(marker)
    } else {
        element.prependText(marker)
    }
    val explicitId = listOf(element.id(), element.attr("name")).firstOrNull(String::isNotBlank)
    add(
        HtmlBlockSource(
            marker = marker,
            role = element.blockRole(),
            explicitId = explicitId,
            sourceFragments = listOfNotNull(explicitId),
        ),
    )
}

private fun Node.hasReadableText(): Boolean = when (this) {
    is TextNode -> text().any(Char::isReadableDocumentCharacter)
    is Element -> text().any(Char::isReadableDocumentCharacter)
    else -> false
}

private fun Char.isReadableDocumentCharacter(): Boolean =
    !isWhitespace() && this != '\u00A0' && this != '\u200B'

private fun Node.prependMarker(marker: String) {
    when (this) {
        is TextNode -> text(marker + text())
        is Element -> prependText(marker)
        else -> before(TextNode(marker))
    }
}

private fun Element.blockRole(): BookDocumentBlockRole {
    val tag = tagName()
    return when (tag) {
        "p" -> BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH)
        in HEADING_TAGS -> BookDocumentBlockRole(
            kind = BookDocumentBlockKind.HEADING,
            level = tag.removePrefix("h").toInt(),
        )
        "ol", "ul" -> BookDocumentBlockRole(
            kind = BookDocumentBlockKind.LIST,
            ordered = tag == "ol",
        )
        "blockquote" -> BookDocumentBlockRole(BookDocumentBlockKind.QUOTE)
        "pre" -> BookDocumentBlockRole(BookDocumentBlockKind.PREFORMATTED)
        "table" -> BookDocumentBlockRole(BookDocumentBlockKind.TABLE)
        "figure" -> BookDocumentBlockRole(BookDocumentBlockKind.FIGURE)
        "figcaption", "caption" -> BookDocumentBlockRole(BookDocumentBlockKind.CAPTION)
        "hr" -> BookDocumentBlockRole(BookDocumentBlockKind.THEMATIC_BREAK)
        else -> BookDocumentBlockRole(BookDocumentBlockKind.OTHER)
    }
}

private fun markAnchors(body: Element): List<HtmlAnchorSource> = buildList {
    body.select("[id], a[name]").forEach { element ->
        val ids = listOf(element.id(), element.attr("name")).filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return@forEach
        val marker = anchorMarker(size)
        element.prependText(marker)
        add(HtmlAnchorSource(marker, ids))
    }
}

private fun SpannableStringBuilder.trackAndRemove(token: String): Any {
    val index = indexOf(token)
    require(index >= 0) { "Unable to preserve an HTML document boundary" }
    val marker = Any()
    setSpan(marker, index, index, Spanned.SPAN_MARK_MARK)
    delete(index, index + token.length)
    return marker
}

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

internal fun paginateProse(
    chapter: HtmlProseLoadedChapter,
    text: Spanned,
    paint: TextPaint,
    availableWidthPx: Int,
    availableHeightPx: Int,
    alignment: Layout.Alignment,
    lineSpacingMultiplier: Float,
    justificationMode: Int = Layout.JUSTIFICATION_MODE_NONE,
): List<HtmlProsePage> {
    if (text.isEmpty() || availableWidthPx <= 0 || availableHeightPx <= 0) return emptyList()
    val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidthPx)
        .setAlignment(alignment)
        .setIncludePad(false)
        .setLineSpacing(0f, lineSpacingMultiplier)
    if (justificationMode != Layout.JUSTIFICATION_MODE_NONE) builder.setJustificationMode(justificationMode)
    val layout = builder.build()
    if (layout.lineCount == 0) return emptyList()

    val ranges = buildList {
        var firstLine = 0
        while (firstLine < layout.lineCount) {
            val pageBottom = layout.getLineTop(firstLine) + availableHeightPx
            var lastLine = layout.getLineForVertical(pageBottom - 1)
                .coerceIn(firstLine, layout.lineCount - 1)
            while (lastLine > firstLine && layout.getLineBottom(lastLine) > pageBottom) {
                lastLine--
            }
            val start = layout.getLineStart(firstLine)
            val end = layout.getLineEnd(lastLine).coerceIn(start, text.length)
            if (end > start) add(start until end)
            firstLine = lastLine + 1
        }
    }
    return ranges.mapIndexed { index, range ->
        HtmlProsePage(
            chapter = chapter.owner,
            index = index,
            total = ranges.size,
            text = SpannableString(text.subSequence(range.first, range.last + 1)),
            progression = if (index == ranges.lastIndex) {
                1f
            } else {
                range.first.toFloat().div(text.length.coerceAtLeast(1))
            },
            sourceStart = range.first,
            sourceEndExclusive = range.last + 1,
        )
    }
}

internal fun pageIndexForAnchor(pages: List<HtmlProsePage>, anchorOffset: Int): Int? {
    return pages.indexOfFirst { page ->
        anchorOffset >= page.sourceStart &&
            (
                anchorOffset < page.sourceEndExclusive ||
                    (page.index == pages.lastIndex && anchorOffset == page.sourceEndExclusive)
                )
    }.takeIf { it >= 0 }
}

private data class HtmlBlockSource(
    val marker: String,
    val role: BookDocumentBlockRole,
    val explicitId: String? = null,
    val sourceFragments: List<String> = emptyList(),
)

private data class HtmlAnchorSource(
    val marker: String,
    val ids: List<String>,
)

private val HEADING_TAGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
private val ATOMIC_BLOCK_TAGS = HEADING_TAGS + setOf(
    "p",
    "ol",
    "ul",
    "blockquote",
    "pre",
    "table",
    "figure",
    "figcaption",
    "caption",
    "dt",
    "dd",
    "hr",
)
private val BLOCK_CONTAINER_TAGS = setOf("body", "article", "aside", "div", "dl", "section")
private const val BLOCK_HASH_LENGTH = 16
private const val ANCHOR_MARKER_START = '\uE000'
private const val ANCHOR_MARKER_END = '\uE001'

private fun blockMarker(index: Int): String = "$ANCHOR_MARKER_START-b${index.toString(36)}-$ANCHOR_MARKER_END"

private fun anchorMarker(index: Int): String = "$ANCHOR_MARKER_START-a${index.toString(36)}-$ANCHOR_MARKER_END"
