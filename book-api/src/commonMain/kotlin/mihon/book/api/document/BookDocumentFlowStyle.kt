package mihon.book.api.document

import kotlinx.serialization.Serializable

/** Domain-neutral flow and language hints layered onto a semantic document block. */
@Serializable
data class BookDocumentFlowStyle(
    val spacingBeforeEm: Float = 0f,
    val spacingAfterEm: Float = 0f,
    val lineHeightScale: Float = DEFAULT_LINE_HEIGHT_SCALE,
    val firstLineIndentEm: Float = 0f,
    val direction: BookDocumentTextDirection? = null,
    val languageTag: String? = null,
) {
    init {
        require(spacingBeforeEm in 0f..MAX_SPACING_EM && spacingAfterEm in 0f..MAX_SPACING_EM) {
            "document spacing is outside the supported range"
        }
        require(lineHeightScale in MIN_LINE_HEIGHT_SCALE..MAX_LINE_HEIGHT_SCALE) {
            "document line height is outside the supported range"
        }
        require(firstLineIndentEm in -MAX_INDENT_EM..MAX_INDENT_EM) {
            "document first-line indentation is outside the supported range"
        }
        require(languageTag == null || languageTag.isNotBlank()) {
            "document language tag must not be blank"
        }
    }
}

private const val MAX_SPACING_EM = 8f
private const val MAX_INDENT_EM = 8f
private const val MIN_LINE_HEIGHT_SCALE = 0.8f
private const val MAX_LINE_HEIGHT_SCALE = 3f
private const val DEFAULT_LINE_HEIGHT_SCALE = 1.25f
