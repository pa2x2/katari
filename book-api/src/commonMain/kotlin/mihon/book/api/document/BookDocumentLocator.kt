package mihon.book.api.document

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import mihon.book.api.BookLocator
import mihon.book.api.BookTextContext

/**
 * Resolves a persisted locator against this canonical document.
 *
 * Resolution order is precise block/offset, fragment or anchor, bounded text context, then
 * progression.
 *
 * @return a valid top-level block position, or `null` when the locator targets another resource or
 * cannot be reconciled.
 */
fun BookDocument.resolvePosition(locator: BookLocator): BookDocumentPosition? {
    if (locator.resourceId != resourceId) return null

    val precise = locator.extensions[DOCUMENT_LOCATION_EXTENSION] as? JsonObject
    val preciseVersion = (precise?.get(VERSION_KEY) as? JsonPrimitive)?.intOrNull
    if (preciseVersion == DOCUMENT_LOCATION_VERSION) {
        val blockId = (precise[BLOCK_ID_KEY] as? JsonPrimitive)
            ?.contentOrNull
            ?.takeIf(String::isNotBlank)
            ?.let(::BookDocumentBlockId)
        val offset = (precise[OFFSET_KEY] as? JsonPrimitive)?.intOrNull
        if (blockId != null && offset != null) {
            BookDocumentPosition(blockId, offset).takeIf(::contains)?.let { return it }
        }
    }

    locator.fragments.firstNotNullOfOrNull { fragment ->
        anchors[fragment] ?: blocks.firstOrNull { block ->
            block.id.value == fragment || fragment in block.sourceFragments
        }?.let { block -> BookDocumentPosition(block.id, 0) }
    }?.let { return it }

    locator.textContext?.findOffsetIn(content.text)?.let(::positionAtLogicalOffset)?.let { return it }

    return locator.progression?.toFloat()?.let(::positionAtProgression)
}

/**
 * Creates a persisted locator for a valid canonical document position.
 *
 * The `app.katari.document.location` version-1 extension remains compatible with existing Katari
 * prose locations.
 */
fun BookDocument.locatorAt(position: BookDocumentPosition): BookLocator {
    val block = checkNotNull(blocks.firstOrNull { it.id == position.blockId }) {
        "document position must target an existing block"
    }
    val safeOffset = position.offsetWithinBlock.coerceIn(0, block.logicalLength)
    val absolute = (block.logicalStart + safeOffset).coerceIn(0, content.text.length)
    val contextStart = (absolute - TEXT_CONTEXT_LENGTH).coerceAtLeast(0)
    val contextEnd = (absolute + TEXT_CONTEXT_LENGTH).coerceAtMost(content.text.length)
    val before = content.text.substring(contextStart, absolute).takeIf(String::isNotBlank)
    val after = content.text.substring(absolute, contextEnd).takeIf(String::isNotBlank)

    return BookLocator(
        resourceId = resourceId,
        progression = progressionAt(BookDocumentPosition(block.id, safeOffset)).toDouble(),
        fragments = block.sourceFragments.ifEmpty { listOf(block.id.value) },
        textContext = BookTextContext(before = before, after = after)
            .takeUnless { it == BookTextContext() },
        extensions = mapOf(
            DOCUMENT_LOCATION_EXTENSION to JsonObject(
                mapOf(
                    VERSION_KEY to JsonPrimitive(DOCUMENT_LOCATION_VERSION),
                    BLOCK_ID_KEY to JsonPrimitive(block.id.value),
                    OFFSET_KEY to JsonPrimitive(safeOffset),
                ),
            ),
        ),
    )
}

private fun BookTextContext.findOffsetIn(text: String): Int? {
    val highlightValue = highlight?.takeIf(String::isNotEmpty)
    if (highlightValue != null) {
        return text.matchOffsets(highlightValue)
            .maxByOrNull { start ->
                contextScore(text, start, start + highlightValue.length)
            }
    }

    val beforeValue = before?.takeIf(String::isNotEmpty)
    val afterValue = after?.takeIf(String::isNotEmpty)
    val candidates = buildSet {
        beforeValue?.let { value ->
            text.matchOffsets(value).forEach { add(it + value.length) }
        }
        afterValue?.let { value ->
            text.matchOffsets(value).forEach(::add)
        }
    }
    return candidates.maxByOrNull { offset -> contextScore(text, offset, offset) }
}

private fun BookTextContext.contextScore(
    text: String,
    start: Int,
    endExclusive: Int,
): Int {
    val beforeScore = before?.matchingSuffixLength(text, start) ?: 0
    val afterScore = after?.matchingPrefixLength(text, endExclusive) ?: 0
    return beforeScore + afterScore
}

private fun String.matchingSuffixLength(
    text: String,
    offset: Int,
): Int {
    val available = minOf(length, offset)
    var matched = 0
    while (matched < available && this[length - 1 - matched] == text[offset - 1 - matched]) {
        matched++
    }
    return matched
}

private fun String.matchingPrefixLength(
    text: String,
    offset: Int,
): Int {
    val available = minOf(length, text.length - offset)
    var matched = 0
    while (matched < available && this[matched] == text[offset + matched]) {
        matched++
    }
    return matched
}

private fun String.matchOffsets(value: String): Sequence<Int> = sequence {
    var start = 0
    while (start <= length - value.length) {
        val match = indexOf(value, start)
        if (match < 0) return@sequence
        yield(match)
        start = match + 1
    }
}

private const val DOCUMENT_LOCATION_EXTENSION = "app.katari.document.location"
private const val DOCUMENT_LOCATION_VERSION = 1
private const val VERSION_KEY = "version"
private const val BLOCK_ID_KEY = "blockId"
private const val OFFSET_KEY = "offset"
private const val TEXT_CONTEXT_LENGTH = 32
