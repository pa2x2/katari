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
 * @property spacingBeforeEm bounded spacing before the block in em units.
 * @property spacingAfterEm bounded spacing after the block in em units.
 * @property lineHeightScale bounded line-height multiplier.
 * @property firstLineIndentEm bounded first-line indentation in em units.
 * @property direction optional logical text direction.
 * @property languageTag optional declared language context for accessibility.
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
    /** Compatibility constructor for serializers compiled against SDK 2.5. */
    @Suppress("UNUSED_PARAMETER", "DEPRECATION", "DEPRECATION_ERROR")
    constructor(
        seen: Int,
        alignment: BookDocumentAlignment?,
        whiteSpace: BookDocumentWhiteSpace,
        foregroundArgb: Long?,
        backgroundArgb: Long?,
        border: BookDocumentBorder?,
        paddingEm: Float,
        fontFamily: BookDocumentFontFamily?,
        fontSizeScale: Float,
        bold: Boolean,
        marker: kotlinx.serialization.internal.SerializationConstructorMarker?,
    ) : this(
        alignment = alignment.takeIf { seen and 0x001 != 0 },
        whiteSpace = whiteSpace.takeIf { seen and 0x002 != 0 } ?: BookDocumentWhiteSpace.NORMAL,
        foregroundArgb = foregroundArgb.takeIf { seen and 0x004 != 0 },
        backgroundArgb = backgroundArgb.takeIf { seen and 0x008 != 0 },
        border = border.takeIf { seen and 0x010 != 0 },
        paddingEm = paddingEm.takeIf { seen and 0x020 != 0 } ?: 0f,
        fontFamily = fontFamily.takeIf { seen and 0x040 != 0 },
        fontSizeScale = fontSizeScale.takeIf { seen and 0x080 != 0 } ?: 1f,
        bold = bold.takeIf { seen and 0x100 != 0 } ?: false,
    )

    var flow: BookDocumentFlowStyle = BookDocumentFlowStyle()
        private set

    val spacingBeforeEm: Float get() = flow.spacingBeforeEm
    val spacingAfterEm: Float get() = flow.spacingAfterEm
    val lineHeightScale: Float get() = flow.lineHeightScale
    val firstLineIndentEm: Float get() = flow.firstLineIndentEm
    val direction: BookDocumentTextDirection? get() = flow.direction
    val languageTag: String? get() = flow.languageTag

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

    /** Returns a copy carrying [flow] without changing the SDK 2.5 constructor ABI. */
    fun withFlow(flow: BookDocumentFlowStyle): BookDocumentStyle = withFlow(this, flow)

    override fun equals(other: Any?): Boolean =
        other is BookDocumentStyle &&
            alignment == other.alignment &&
            whiteSpace == other.whiteSpace &&
            foregroundArgb == other.foregroundArgb &&
            backgroundArgb == other.backgroundArgb &&
            border == other.border &&
            paddingEm == other.paddingEm &&
            flow == other.flow &&
            fontFamily == other.fontFamily &&
            fontSizeScale == other.fontSizeScale &&
            bold == other.bold

    override fun hashCode(): Int = listOf(
        alignment,
        whiteSpace,
        foregroundArgb,
        backgroundArgb,
        border,
        paddingEm,
        flow,
        fontFamily,
        fontSizeScale,
        bold,
    ).hashCode()

    companion object {
        /** Creates a style with the domain-neutral document-flow properties added in SDK 2.6. */
        fun withFlow(base: BookDocumentStyle, flow: BookDocumentFlowStyle): BookDocumentStyle =
            base.copy().apply { this.flow = flow }
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
        require(style.hasEffect()) { "document inline style must change at least one property" }
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
 * @property languageTag optional declared language for this range.
 * @property direction optional declared direction for this range.
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
    /** Compatibility constructor for serializers compiled against SDK 2.5. */
    @Suppress("UNUSED_PARAMETER", "DEPRECATION", "DEPRECATION_ERROR")
    constructor(
        seen: Int,
        foregroundArgb: Long?,
        backgroundArgb: Long?,
        fontFamily: BookDocumentFontFamily?,
        fontSizeScale: Float?,
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        strikethrough: Boolean,
        subscript: Boolean,
        superscript: Boolean,
        code: Boolean,
        small: Boolean,
        marker: kotlinx.serialization.internal.SerializationConstructorMarker?,
    ) : this(
        foregroundArgb = foregroundArgb.takeIf { seen and 0x001 != 0 },
        backgroundArgb = backgroundArgb.takeIf { seen and 0x002 != 0 },
        fontFamily = fontFamily.takeIf { seen and 0x004 != 0 },
        fontSizeScale = fontSizeScale.takeIf { seen and 0x008 != 0 },
        bold = bold.takeIf { seen and 0x010 != 0 } ?: false,
        italic = italic.takeIf { seen and 0x020 != 0 } ?: false,
        underline = underline.takeIf { seen and 0x040 != 0 } ?: false,
        strikethrough = strikethrough.takeIf { seen and 0x080 != 0 } ?: false,
        subscript = subscript.takeIf { seen and 0x100 != 0 } ?: false,
        superscript = superscript.takeIf { seen and 0x200 != 0 } ?: false,
        code = code.takeIf { seen and 0x400 != 0 } ?: false,
        small = small.takeIf { seen and 0x800 != 0 } ?: false,
    )

    var textContext: BookDocumentTextContext = BookDocumentTextContext()
        private set

    val languageTag: String? get() = textContext.languageTag
    val direction: BookDocumentTextDirection? get() = textContext.direction

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
    }

    internal fun hasEffect(): Boolean =
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
            small ||
            !textContext.isEmpty

    override fun equals(other: Any?): Boolean =
        other is BookDocumentInlineStyle &&
            foregroundArgb == other.foregroundArgb &&
            backgroundArgb == other.backgroundArgb &&
            fontFamily == other.fontFamily &&
            fontSizeScale == other.fontSizeScale &&
            bold == other.bold &&
            italic == other.italic &&
            underline == other.underline &&
            strikethrough == other.strikethrough &&
            subscript == other.subscript &&
            superscript == other.superscript &&
            code == other.code &&
            small == other.small &&
            textContext == other.textContext

    override fun hashCode(): Int = listOf(
        foregroundArgb,
        backgroundArgb,
        fontFamily,
        fontSizeScale,
        bold,
        italic,
        underline,
        strikethrough,
        subscript,
        superscript,
        code,
        small,
        textContext,
    ).hashCode()

    companion object {
        /** Creates an inline style with the domain-neutral language context added in SDK 2.6. */
        fun withTextContext(
            base: BookDocumentInlineStyle,
            textContext: BookDocumentTextContext,
        ): BookDocumentInlineStyle = base.copy().apply {
            this.textContext = textContext
            require(hasEffect()) { "document inline style must change at least one property" }
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

/** Explicit logical direction for semantic document text. */
@Serializable
enum class BookDocumentTextDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
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
