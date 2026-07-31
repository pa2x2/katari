package mihon.book.api.document

import kotlinx.serialization.Serializable

/**
 * Stable publication-local identity of one semantic block.
 *
 * @property value non-blank serialized identity.
 */
@Serializable
@JvmInline
value class BookDocumentBlockId(val value: String) {
    init {
        require(value.isNotBlank()) { "document block id must not be blank" }
    }
}

/**
 * Precise position relative to a semantic block.
 *
 * @property blockId target block identity.
 * @property offsetWithinBlock UTF-16 offset relative to the block's canonical text.
 */
@Serializable
data class BookDocumentPosition(
    val blockId: BookDocumentBlockId,
    val offsetWithinBlock: Int,
) {
    init {
        require(offsetWithinBlock >= 0) { "document block offset must not be negative" }
    }
}

/**
 * Half-open UTF-16 range relative to an owning semantic text.
 *
 * @property start inclusive start offset.
 * @property endExclusive exclusive end offset.
 */
@Serializable
data class BookDocumentTextRange(
    val start: Int,
    val endExclusive: Int,
) {
    init {
        require(start >= 0) { "document text range start must not be negative" }
        require(endExclusive >= start) { "document text range end must not precede its start" }
    }

    /** Number of UTF-16 code units in this range. */
    val length: Int
        get() = endExclusive - start
}
