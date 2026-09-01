package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentContent
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentPosition
import mihon.book.api.document.BookDocumentStyle
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import org.jsoup.nodes.Element
import java.security.MessageDigest

internal class HtmlProseDocumentParser {
    fun parse(resourceId: String, revision: String?, body: Element): BookDocument {
        val context = HtmlProseParsingContext()
        val parsed = mutableListOf<HtmlProseParsedBlock>()
        HtmlProseBlockParser(context).collectChildren(body, body.documentBlockStyle(), false, parsed)
        val content = assembleContent(parsed)
        if (content.text.length > HtmlProseChapterContract.MAX_CANONICAL_UTF16) {
            throw HtmlProseLimitExceededException("Canonical chapter text exceeds its UTF-16 limit")
        }
        return BookDocument(resourceId, revision, content)
    }
}

internal class HtmlProseParsingContext {
    private var semanticUnits = 0
    private var canonicalUtf16 = 0

    fun claimSemanticUnit() {
        semanticUnits += 1
        if (semanticUnits > HtmlProseChapterContract.MAX_BLOCKS) {
            throw HtmlProseLimitExceededException("HTML contains too many semantic blocks")
        }
    }

    fun claimCanonicalText(length: Int) {
        canonicalUtf16 += length
        if (canonicalUtf16 > HtmlProseChapterContract.MAX_CANONICAL_UTF16) {
            throw HtmlProseLimitExceededException("Canonical chapter text exceeds its UTF-16 limit")
        }
    }
}

internal data class HtmlProseParsedBlock(
    val text: String,
    val plainText: String,
    val role: BookDocumentBlockRole,
    val content: BookDocumentBlockContent,
    val style: BookDocumentStyle,
    val explicitId: String?,
    val fragments: List<String>,
    val anchors: Map<String, Int>,
)

internal fun assembleContent(parsedBlocks: List<HtmlProseParsedBlock>): BookDocumentContent {
    require(parsedBlocks.isNotEmpty()) { "The prose chapter contains no readable document blocks" }
    val text = StringBuilder()
    val blocks = mutableListOf<BookDocumentBlock>()
    val anchors = linkedMapOf<String, BookDocumentPosition>()
    val usedIds = mutableMapOf<String, Int>()
    val usedFragments = mutableSetOf<String>()

    parsedBlocks.forEach { parsed ->
        if (parsed.text.isEmpty()) return@forEach
        val start = text.length
        text.append(parsed.text)
        val blockId = uniqueBlockId(parsed.explicitId, parsed.role, parsed.plainText, usedIds)
        val sourceFragments = parsed.fragments.filter(usedFragments::add)
        blocks += BookDocumentBlock(
            id = blockId,
            role = parsed.role,
            content = parsed.content,
            plainText = parsed.plainText.trim(),
            sourceFragments = sourceFragments,
            style = parsed.style,
            logicalStart = start,
            logicalEndExclusive = text.length,
        )
        sourceFragments.forEach { fragment ->
            anchors.putIfAbsent(
                fragment,
                BookDocumentPosition(blockId, parsed.anchors[fragment]?.coerceIn(0, parsed.text.length) ?: 0),
            )
        }
    }
    require(blocks.isNotEmpty()) { "The prose chapter contains no readable document blocks" }
    return BookDocumentContent(
        text = text.toString(),
        blocks = blocks,
        anchors = anchors,
        resourceIds = blocks.flatMapTo(linkedSetOf(), BookDocumentBlock::referencedResources),
    )
}

private fun uniqueBlockId(
    explicitId: String?,
    role: BookDocumentBlockRole,
    text: String,
    usedIds: MutableMap<String, Int>,
): BookDocumentBlockId {
    val base = explicitId?.trim()?.takeIf(String::isNotEmpty)?.take(256) ?: run {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("${role.kind}:${role.level}:${role.depth}:$text".toByteArray())
            .take(8)
            .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
        "${role.kind.name.lowercase()}-$digest"
    }
    val occurrence = usedIds.getOrDefault(base, 0)
    usedIds[base] = occurrence + 1
    return BookDocumentBlockId(if (occurrence == 0) base else "$base-${occurrence + 1}")
}

private fun BookDocumentBlock.referencedResources(): Set<String> = buildSet {
    (style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId?.let(::add)
    inlineStyles.mapNotNullTo(this) { range ->
        (range.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId
    }
    when (val value = content) {
        is BookDocumentBlockContent.Figure -> add(value.image.resourceId)
        is BookDocumentBlockContent.Disclosure -> addAll(value.body.resourceIds)
        else -> Unit
    }
}
