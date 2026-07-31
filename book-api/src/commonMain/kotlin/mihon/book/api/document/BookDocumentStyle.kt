package mihon.book.api.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Validated presentation hints for a semantic block.
 *
 * @property alignment optional logical text alignment.
 * @property whiteSpace whitespace handling.
 * @property foregroundArgb optional ARGB foreground color.
 * @property backgroundArgb optional ARGB background color.
 * @property border optional border decoration.
 * @property paddingEm bounded logical padding in em units.
 * @property fontFamily optional generic or publication-resource font family.
 * @property fontSizeScale bounded relative font size.
 * @property bold whether the complete block is semantically bold.
 */
@Serializable
data class BookDocumentStyle(
    val alignment: BookDocumentAlignment? = null,
    val whiteSpace: BookDocumentWhiteSpace = BookDocumentWhiteSpace.NORMAL,
    val foregroundArgb: Long? = null,
    val backgroundArgb: Long? = null,
    val border: BookDocumentBorder? = null,
    val paddingEm: Float = 0f,
    val fontFamily: BookDocumentFontFamily? = null,
    val fontSizeScale: Float = 1f,
    val bold: Boolean = false,
) {
    init {
        require(foregroundArgb == null || foregroundArgb in 0..MAX_ARGB) {
            "document foreground color must be ARGB"
        }
        require(backgroundArgb == null || backgroundArgb in 0..MAX_ARGB) {
            "document background color must be ARGB"
        }
        require(paddingEm in 0f..MAX_PADDING_EM) {
            "document padding is outside the supported range"
        }
        require(fontSizeScale in MIN_FONT_SCALE..MAX_FONT_SCALE) {
            "document font scale is outside the supported range"
        }
    }
}

/**
 * One inline semantic style range relative to a rich-text leaf.
 *
 * @property start inclusive UTF-16 offset.
 * @property endExclusive exclusive UTF-16 offset.
 * @property style semantic style applied to the range.
 */
@Serializable
data class BookDocumentInlineStyleRange(
    val start: Int,
    val endExclusive: Int,
    val style: BookDocumentInlineStyle,
) {
    init {
        require(start >= 0) { "document inline style start must not be negative" }
        require(endExclusive > start) { "document inline style range must not be empty" }
    }

    internal fun shifted(offset: Int): BookDocumentInlineStyleRange =
        copy(start = start + offset, endExclusive = endExclusive + offset)
}

/**
 * Complete supported inline semantic style.
 *
 * @property foregroundArgb optional ARGB foreground color.
 * @property backgroundArgb optional ARGB background color.
 * @property fontFamily optional generic or publication-resource font family.
 * @property fontSizeScale optional bounded relative font size.
 * @property bold strong or bold emphasis.
 * @property italic italic or semantic emphasis.
 * @property underline underlined text.
 * @property strikethrough struck text.
 * @property subscript subscript text.
 * @property superscript superscript text.
 * @property code inline code text.
 * @property small semantically small text.
 */
@Serializable
data class BookDocumentInlineStyle(
    val foregroundArgb: Long? = null,
    val backgroundArgb: Long? = null,
    val fontFamily: BookDocumentFontFamily? = null,
    val fontSizeScale: Float? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val subscript: Boolean = false,
    val superscript: Boolean = false,
    val code: Boolean = false,
    val small: Boolean = false,
) {
    init {
        require(foregroundArgb == null || foregroundArgb in 0..MAX_ARGB) {
            "document inline foreground color must be ARGB"
        }
        require(backgroundArgb == null || backgroundArgb in 0..MAX_ARGB) {
            "document inline background color must be ARGB"
        }
        require(fontSizeScale == null || fontSizeScale in MIN_FONT_SCALE..MAX_FONT_SCALE) {
            "document inline font scale is outside the supported range"
        }
        require(!subscript || !superscript) {
            "document inline style cannot be both subscript and superscript"
        }
        require(
            foregroundArgb != null ||
                backgroundArgb != null ||
                fontFamily != null ||
                fontSizeScale != null ||
                bold ||
                italic ||
                underline ||
                strikethrough ||
                subscript ||
                superscript ||
                code ||
                small,
        ) {
            "document inline style must change at least one property"
        }
    }
}

/** Logical text alignment. */
@Serializable
enum class BookDocumentAlignment {
    START,
    CENTER,
    END,
}

/** Canonical whitespace handling. */
@Serializable
enum class BookDocumentWhiteSpace {
    NORMAL,
    PRE_WRAP,
    PRE,
}

/**
 * Validated border decoration.
 *
 * @property widthDp bounded width in density-independent pixels.
 * @property colorArgb optional ARGB color.
 * @property style supported border style.
 */
@Serializable
data class BookDocumentBorder(
    val widthDp: Float,
    val colorArgb: Long?,
    val style: BookDocumentBorderStyle,
) {
    init {
        require(widthDp in 0.5f..8f) { "document border width is outside the supported range" }
        require(colorArgb == null || colorArgb in 0..MAX_ARGB) {
            "document border color must be ARGB"
        }
    }
}

/** Supported border styles. */
@Serializable
enum class BookDocumentBorderStyle {
    SOLID,
    DASHED,
    DOTTED,
}

/** Validated font family reference. */
@Serializable
sealed interface BookDocumentFontFamily {

    /**
     * Platform-independent generic family.
     *
     * @property family generic family category.
     */
    @Serializable
    @SerialName("generic")
    data class Generic(val family: GenericFamily) : BookDocumentFontFamily

    /**
     * Publication-scoped validated font resource.
     *
     * @property resourceId non-blank resource identity.
     */
    @Serializable
    @SerialName("resource")
    data class Resource(val resourceId: String) : BookDocumentFontFamily {
        init {
            require(resourceId.isNotBlank()) { "document font resource id must not be blank" }
        }
    }

    /** Generic font family categories. */
    @Serializable
    enum class GenericFamily {
        SERIF,
        SANS_SERIF,
        MONOSPACE,
    }
}

private const val MAX_ARGB = 0xFFFF_FFFFL
private const val MAX_PADDING_EM = 4f
private const val MIN_FONT_SCALE = 0.75f
private const val MAX_FONT_SCALE = 1.5f
