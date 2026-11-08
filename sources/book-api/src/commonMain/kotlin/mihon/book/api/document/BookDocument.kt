package mihon.book.api.document

import kotlinx.serialization.Serializable

/**
 * Canonical semantic representation of one structured BOOK resource.
 *
 * @property resourceId publication-scoped identity of the represented resource.
 * @property revision optional resource revision used for location reconciliation.
 * @property content canonical text, blocks, anchors, and subordinate resources.
 */
@Serializable
data class BookDocument(
    val resourceId: String,
    val revision: String?,
    val content: BookDocumentContent,
) {
    init {
        require(resourceId.isNotBlank()) { "document resource id must not be blank" }
        require(revision == null || revision.isNotBlank()) { "document revision must not be blank" }
    }

    /** Top-level semantic blocks in canonical reading order. */
    val blocks: List<BookDocumentBlock>
        get() = content.blocks

    /** Named anchors mapped into top-level block coordinates. */
    val anchors: Map<String, BookDocumentPosition>
        get() = content.anchors

    /** Publication-scoped subordinate resources referenced by this document. */
    val resourceIds: Set<String>
        get() = content.resourceIds

    /** UTF-16 length of the canonical logical text. */
    val logicalExtent: Int
        get() = content.text.length

    /**
     * Resolves a normalized progression to the nearest top-level block position.
     *
     * @param progression normalized progression in the document.
     */
    fun positionAtProgression(progression: Float): BookDocumentPosition =
        positionAtLogicalOffset((logicalExtent * progression.coerceIn(0f, 1f)).toInt())

    /**
     * Resolves a canonical UTF-16 offset to a top-level block position.
     *
     * @param offset offset in [BookDocumentContent.text].
     */
    fun positionAtLogicalOffset(offset: Int): BookDocumentPosition {
        val target = offset.coerceIn(0, logicalExtent)
        val block = blocks.firstOrNull { target < it.logicalEndExclusive } ?: blocks.last()
        return BookDocumentPosition(
            blockId = block.id,
            offsetWithinBlock = (target - block.logicalStart).coerceIn(0, block.logicalLength),
        )
    }

    /**
     * Computes normalized progression for a valid top-level block position.
     *
     * Invalid positions resolve to zero.
     */
    fun progressionAt(position: BookDocumentPosition): Float {
        val absolute = logicalOffset(position) ?: return 0f
        return absolute.toFloat().div(logicalExtent.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    /**
     * Converts a valid top-level block position to a canonical UTF-16 offset.
     *
     * @return the canonical offset, or `null` when the block is unknown.
     */
    fun logicalOffset(position: BookDocumentPosition): Int? {
        val block = blocks.firstOrNull { it.id == position.blockId } ?: return null
        return block.logicalStart + position.offsetWithinBlock.coerceIn(0, block.logicalLength)
    }

    /** Returns whether [position] is contained by a top-level block. */
    fun contains(position: BookDocumentPosition): Boolean {
        val block = blocks.firstOrNull { it.id == position.blockId } ?: return false
        return position.offsetWithinBlock in 0..block.logicalLength
    }
}
