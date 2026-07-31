package mihon.book.api.document

import kotlinx.serialization.Serializable

/**
 * Recursive canonical content owned by a document or disclosure body.
 *
 * Ranges use UTF-16 offsets in [text]. Block ranges are ordered and non-overlapping. Textual leaves
 * inside each block use block-relative ranges and must reproduce the corresponding canonical
 * substring exactly.
 *
 * @property text exact logical text used for locations, selection, and progress.
 * @property blocks semantic blocks in reading order.
 * @property anchors named anchors mapped to positions in [blocks].
 * @property resourceIds exact publication-scoped image and font resources referenced by this
 * content.
 */
@Serializable
data class BookDocumentContent(
    val text: String,
    val blocks: List<BookDocumentBlock>,
    val anchors: Map<String, BookDocumentPosition>,
    val resourceIds: Set<String> = emptySet(),
) {
    init {
        require(text.isNotEmpty()) { "document content text must not be empty" }
        require(blocks.isNotEmpty()) { "document content must contain at least one block" }
        require(blocks.map(BookDocumentBlock::id).distinct().size == blocks.size) {
            "document block ids must be unique within their content"
        }
        val sourceFragments = blocks.flatMap(BookDocumentBlock::sourceFragments)
        require(sourceFragments.distinct().size == sourceFragments.size) {
            "document source fragments must be unique within their content"
        }
        require(
            blocks.zipWithNext().all { (first, second) ->
                first.logicalEndExclusive <= second.logicalStart
            },
        ) {
            "document block ranges must be ordered and non-overlapping"
        }
        require(blocks.all { it.logicalEndExclusive <= text.length }) {
            "document block ranges must fit inside canonical text"
        }
        require(anchors.keys.none(String::isBlank)) { "document anchor names must not be blank" }
        require(anchors.values.all(::contains)) {
            "document anchors must target an existing block position"
        }
        require(resourceIds.none(String::isBlank)) { "document resource ids must not be blank" }
        require(resourceIds == blocks.flatMapTo(linkedSetOf()) { it.referencedResourceIds() }) {
            "document resource ids must exactly match referenced images and fonts"
        }
        blocks.forEach { block ->
            block.content.validateCanonicalText(
                text.substring(block.logicalStart, block.logicalEndExclusive),
            )
        }
    }

    /** Returns whether [position] is contained by a block in this content. */
    fun contains(position: BookDocumentPosition): Boolean {
        val block = blocks.firstOrNull { it.id == position.blockId } ?: return false
        return position.offsetWithinBlock in 0..block.logicalLength
    }
}
