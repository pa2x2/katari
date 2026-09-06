package mihon.book.api.document

import kotlinx.serialization.Serializable

/**
 * One textual leaf with semantic links and inline styles.
 *
 * [range] is relative to the owning block's canonical text. Link and style ranges are relative to
 * [text].
 *
 * @property text exact leaf text.
 * @property range location of [text] in the owning block.
 * @property links typed link ranges relative to [text].
 * @property inlineStyles semantic style ranges relative to [text].
 */
@Serializable
data class BookDocumentRichText(
    val text: String,
    val range: BookDocumentTextRange,
    val links: List<BookDocumentLink> = emptyList(),
    val inlineStyles: List<BookDocumentInlineStyleRange> = emptyList(),
) {
    init {
        require(range.length == text.length) {
            "document rich-text range length must equal its text length"
        }
        require(links.all { it.endExclusive <= text.length }) {
            "document rich-text links must fit inside their text"
        }
        require(inlineStyles.all { it.endExclusive <= text.length }) {
            "document rich-text styles must fit inside their text"
        }
    }

    internal fun validateCanonicalText(blockText: String) {
        require(range.endExclusive <= blockText.length) {
            "document rich text must fit inside canonical block text"
        }
        require(
            blockText.regionMatches(
                thisOffset = range.start,
                other = text,
                otherOffset = 0,
                length = text.length,
            ),
        ) {
            "document rich text must match canonical block text"
        }
    }
}

/**
 * One flattened semantic list item.
 *
 * @property content rich item text mapped into the owning list block.
 * @property depth zero-based nested-list depth.
 * @property marker explicit rendered marker metadata.
 */
@Serializable
data class BookDocumentListItem(
    val content: BookDocumentRichText,
    val depth: Int,
    val marker: String?,
) {
    init {
        require(content.text.isNotBlank()) { "document list item text must not be blank" }
        require(depth in 0..MAX_LIST_DEPTH) {
            "document list item depth is outside the supported range"
        }
        require(marker == null || marker.isNotBlank()) { "document list marker must not be blank" }
    }

    /** Exact item text without its marker. */
    val text: String
        get() = content.text
}

/** Supported semantic list marker styles. */
@Serializable
enum class BookDocumentListMarkerStyle {
    DECIMAL,
    LOWER_ALPHA,
    UPPER_ALPHA,
    LOWER_ROMAN,
    UPPER_ROMAN,
    BULLET,
}

/**
 * Validated image reference and its canonical alternative text.
 *
 * @property resourceId publication-scoped image resource identity.
 * @property alternativeText optional alternative text mapped into the owning figure block.
 * @property decorative whether the image is explicitly presentation-only and must stay silent.
 * @property width optional validated intrinsic width in pixels.
 * @property height optional validated intrinsic height in pixels.
 */
@Serializable
data class BookDocumentImage(
    val resourceId: String,
    val alternativeText: BookDocumentRichText?,
    val width: Int?,
    val height: Int?,
) {
    /** Compatibility constructor for serializers compiled against SDK 2.5. */
    @Suppress("UNUSED_PARAMETER", "DEPRECATION", "DEPRECATION_ERROR")
    constructor(
        seen: Int,
        resourceId: String,
        alternativeText: BookDocumentRichText?,
        width: Int?,
        height: Int?,
        marker: kotlinx.serialization.internal.SerializationConstructorMarker?,
    ) : this(
        resourceId = resourceId,
        alternativeText = alternativeText,
        width = width,
        height = height,
    ) {
        require(seen and 0xF == 0xF) { "serialized document image is missing required fields" }
    }

    var decorative: Boolean = false
        private set(value) {
            require(!value || alternativeText == null) {
                "a decorative document image must not expose alternative text"
            }
            field = value
        }

    init {
        require(resourceId.isNotBlank()) { "document image resource id must not be blank" }
        require(alternativeText == null || alternativeText.text.isNotBlank()) {
            "document image alternative text must not be blank"
        }
        require(width == null || width in 1..MAX_INTRINSIC_DIMENSION) {
            "document image width is outside the supported range"
        }
        require(height == null || height in 1..MAX_INTRINSIC_DIMENSION) {
            "document image height is outside the supported range"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is BookDocumentImage &&
            resourceId == other.resourceId &&
            alternativeText == other.alternativeText &&
            width == other.width &&
            height == other.height &&
            decorative == other.decorative

    override fun hashCode(): Int = listOf(resourceId, alternativeText, width, height, decorative).hashCode()

    companion object {
        /** Creates an image with the domain-neutral accessibility semantics added in SDK 2.6. */
        fun withAccessibility(
            resourceId: String,
            alternativeText: BookDocumentRichText?,
            width: Int?,
            height: Int?,
            decorative: Boolean,
        ): BookDocumentImage = BookDocumentImage(
            resourceId = resourceId,
            alternativeText = alternativeText,
            width = width,
            height = height,
        ).apply {
            this.decorative = decorative
        }
    }
}

private const val MAX_LIST_DEPTH = 8
private const val MAX_INTRINSIC_DIMENSION = 32_768
