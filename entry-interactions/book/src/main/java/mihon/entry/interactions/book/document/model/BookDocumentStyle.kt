package mihon.entry.interactions.book.document.model

internal data class BookDocumentStyle(
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
        require(paddingEm in 0f..MAX_PADDING_EM) { "document padding is outside the supported range" }
        require(fontSizeScale in MIN_FONT_SCALE..MAX_FONT_SCALE) {
            "document font scale is outside the supported range"
        }
    }

    private companion object {
        const val MAX_ARGB = 0xFFFF_FFFFL
        const val MAX_PADDING_EM = 4f
        const val MIN_FONT_SCALE = 0.75f
        const val MAX_FONT_SCALE = 1.5f
    }
}

internal data class BookDocumentInlineStyleRange(
    val start: Int,
    val endExclusive: Int,
    val style: BookDocumentInlineStyle,
) {
    init {
        require(start >= 0) { "document inline style start must not be negative" }
        require(endExclusive > start) { "document inline style range must not be empty" }
    }
}

internal data class BookDocumentInlineStyle(
    val foregroundArgb: Long? = null,
    val backgroundArgb: Long? = null,
    val fontFamily: BookDocumentFontFamily? = null,
    val fontSizeScale: Float? = null,
    val bold: Boolean = false,
) {
    init {
        require(foregroundArgb == null || foregroundArgb in 0..0xFFFF_FFFFL) {
            "document inline foreground color must be ARGB"
        }
        require(backgroundArgb == null || backgroundArgb in 0..0xFFFF_FFFFL) {
            "document inline background color must be ARGB"
        }
        require(fontSizeScale == null || fontSizeScale in 0.75f..1.5f) {
            "document inline font scale is outside the supported range"
        }
        require(
            foregroundArgb != null ||
                backgroundArgb != null ||
                fontFamily != null ||
                fontSizeScale != null ||
                bold,
        ) {
            "document inline style must change at least one property"
        }
    }
}

internal enum class BookDocumentAlignment {
    START,
    CENTER,
    END,
}

internal enum class BookDocumentWhiteSpace {
    NORMAL,
    PRE_WRAP,
    PRE,
}

internal data class BookDocumentBorder(
    val widthDp: Float,
    val colorArgb: Long?,
    val style: BookDocumentBorderStyle,
) {
    init {
        require(widthDp in 0.5f..8f) { "document border width is outside the supported range" }
        require(colorArgb == null || colorArgb in 0..0xFFFF_FFFFL) {
            "document border color must be ARGB"
        }
    }
}

internal enum class BookDocumentBorderStyle {
    SOLID,
    DASHED,
    DOTTED,
}

internal sealed interface BookDocumentFontFamily {
    data class Generic(val family: GenericFamily) : BookDocumentFontFamily
    data class Resource(val resourceId: String) : BookDocumentFontFamily {
        init {
            require(resourceId.isNotBlank()) { "document font resource id must not be blank" }
        }
    }

    enum class GenericFamily {
        SERIF,
        SANS_SERIF,
        MONOSPACE,
    }
}
