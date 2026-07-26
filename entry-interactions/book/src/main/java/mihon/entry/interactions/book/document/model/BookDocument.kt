package mihon.entry.interactions.book.document.model

/**
 * Incubating processor-neutral model for structured BOOK resources.
 *
 * This contract intentionally contains no Android, renderer, source-child, or format-specific types. It is kept
 * internal while its identity and location semantics are exercised by built-in processors, but it is explicitly
 * intended to move to `book-api` once another processor validates the model or the public processor boundary needs it.
 */
internal data class BookDocument(
    val resourceId: String,
    val revision: String?,
    val blocks: List<BookDocumentBlock>,
    val anchors: Map<String, BookDocumentPosition>,
    val logicalExtent: Int,
) {
    init {
        require(resourceId.isNotBlank()) { "document resource id must not be blank" }
        require(revision == null || revision.isNotBlank()) { "document revision must not be blank" }
        require(blocks.isNotEmpty()) { "document must contain at least one block" }
        require(blocks.map(BookDocumentBlock::id).distinct().size == blocks.size) {
            "document block ids must be unique"
        }
        require(
            blocks.zipWithNext().all { (first, second) ->
                first.logicalEndExclusive <= second.logicalStart
            },
        ) {
            "document block ranges must be ordered and non-overlapping"
        }
        require(logicalExtent >= blocks.last().logicalEndExclusive) {
            "document extent must contain every block"
        }
        require(anchors.values.all(::contains)) { "document anchors must target an existing block position" }
    }

    fun positionAtProgression(progression: Float): BookDocumentPosition {
        return positionAtLogicalOffset((logicalExtent * progression.coerceIn(0f, 1f)).toInt())
    }

    fun positionAtLogicalOffset(offset: Int): BookDocumentPosition {
        val target = offset.coerceIn(0, logicalExtent)
        val block = blocks.firstOrNull { target < it.logicalEndExclusive } ?: blocks.last()
        return BookDocumentPosition(
            blockId = block.id,
            offsetWithinBlock = (target - block.logicalStart).coerceIn(0, block.logicalLength),
        )
    }

    fun progressionAt(position: BookDocumentPosition): Float {
        val absolute = logicalOffset(position) ?: return 0f
        return absolute.toFloat().div(logicalExtent.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    fun logicalOffset(position: BookDocumentPosition): Int? {
        val block = blocks.firstOrNull { it.id == position.blockId } ?: return null
        return block.logicalStart + position.offsetWithinBlock.coerceIn(0, block.logicalLength)
    }

    fun contains(position: BookDocumentPosition): Boolean {
        val block = blocks.firstOrNull { it.id == position.blockId } ?: return false
        return position.offsetWithinBlock in 0..block.logicalLength
    }
}

@JvmInline
internal value class BookDocumentBlockId(val value: String) {
    init {
        require(value.isNotBlank()) { "document block id must not be blank" }
    }
}

internal data class BookDocumentBlock(
    val id: BookDocumentBlockId,
    val role: BookDocumentBlockRole,
    val plainText: String,
    val sourceFragments: List<String>,
    val logicalStart: Int,
    val logicalEndExclusive: Int,
) {
    init {
        require(sourceFragments.none(String::isBlank)) { "block source fragments must not be blank" }
        require(logicalStart >= 0) { "block logical start must not be negative" }
        require(logicalEndExclusive > logicalStart) { "block logical range must not be empty" }
    }

    val logicalLength: Int
        get() = logicalEndExclusive - logicalStart
}

internal data class BookDocumentPosition(
    val blockId: BookDocumentBlockId,
    val offsetWithinBlock: Int,
) {
    init {
        require(offsetWithinBlock >= 0) { "document block offset must not be negative" }
    }
}

internal data class BookDocumentBlockRole(
    val kind: BookDocumentBlockKind,
    val level: Int? = null,
    val depth: Int = 0,
    val ordered: Boolean? = null,
) {
    init {
        require(level == null || level > 0) { "document block level must be positive" }
        require(depth >= 0) { "document block depth must not be negative" }
    }
}

internal enum class BookDocumentBlockKind {
    PARAGRAPH,
    HEADING,
    LIST,
    LIST_ITEM,
    QUOTE,
    PREFORMATTED,
    TABLE,
    FIGURE,
    CAPTION,
    THEMATIC_BREAK,
    OTHER,
}
