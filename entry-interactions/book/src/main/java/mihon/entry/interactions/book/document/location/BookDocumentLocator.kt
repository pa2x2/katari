package mihon.entry.interactions.book.document.location

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import mihon.book.api.BookLocator
import mihon.book.api.BookTextContext
import mihon.entry.interactions.book.document.model.BookDocumentBlockId
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.render.PreparedBookDocument

internal fun PreparedBookDocument.resolvePosition(locator: BookLocator): BookDocumentPosition? {
    if (locator.resourceId != document.resourceId) return null

    val precise = locator.extensions[DOCUMENT_LOCATION_EXTENSION] as? JsonObject
    val preciseVersion = (precise?.get(VERSION_KEY) as? JsonPrimitive)?.intOrNull
    if (preciseVersion == DOCUMENT_LOCATION_VERSION) {
        val blockId = (precise[BLOCK_ID_KEY] as? JsonPrimitive)
            ?.contentOrNull
            ?.takeIf(String::isNotBlank)
            ?.let(::BookDocumentBlockId)
        val offset = (precise[OFFSET_KEY] as? JsonPrimitive)?.intOrNull
        if (blockId != null && offset != null) {
            BookDocumentPosition(blockId, offset).takeIf(document::contains)?.let { return it }
        }
    }

    locator.fragments.firstNotNullOfOrNull { fragment ->
        document.blocks.firstOrNull { block ->
            block.id.value == fragment || fragment in block.sourceFragments
        }?.let { block -> BookDocumentPosition(block.id, 0) }
    }?.let { return it }

    return locator.progression?.toFloat()?.let(document::positionAtProgression)
}

internal fun PreparedBookDocument.locatorAt(position: BookDocumentPosition): BookLocator {
    val block = checkNotNull(document.blocks.firstOrNull { it.id == position.blockId }) {
        "document position must target an existing block"
    }
    val safeOffset = position.offsetWithinBlock.coerceIn(0, block.logicalLength)
    val absolute = (block.logicalStart + safeOffset).coerceIn(0, combinedText.length)
    val contextStart = (absolute - TEXT_CONTEXT_LENGTH).coerceAtLeast(0)
    val contextEnd = (absolute + TEXT_CONTEXT_LENGTH).coerceAtMost(combinedText.length)
    val before = combinedText.subSequence(contextStart, absolute).toString().takeIf(String::isNotBlank)
    val after = combinedText.subSequence(absolute, contextEnd).toString().takeIf(String::isNotBlank)

    return BookLocator(
        resourceId = document.resourceId,
        progression = document.progressionAt(BookDocumentPosition(block.id, safeOffset)).toDouble(),
        fragments = block.sourceFragments.ifEmpty { listOf(block.id.value) },
        textContext = BookTextContext(before = before, after = after).takeUnless { it == BookTextContext() },
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

private const val DOCUMENT_LOCATION_EXTENSION = "app.katari.document.location"
private const val DOCUMENT_LOCATION_VERSION = 1
private const val VERSION_KEY = "version"
private const val BLOCK_ID_KEY = "blockId"
private const val OFFSET_KEY = "offset"
private const val TEXT_CONTEXT_LENGTH = 32
