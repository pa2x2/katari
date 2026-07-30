package mihon.entry.interactions.book.document.model

internal sealed interface BookDocumentBlockContent {
    data class Text(
        val preformatted: Boolean = false,
    ) : BookDocumentBlockContent

    data class ListBlock(
        val ordered: Boolean,
        val start: Int,
        val markerStyle: BookDocumentListMarkerStyle,
        val items: List<BookDocumentListItem>,
    ) : BookDocumentBlockContent {
        init {
            require(items.isNotEmpty()) { "document list must contain at least one item" }
        }
    }

    data class Figure(
        val image: BookDocumentImage,
        val caption: String?,
    ) : BookDocumentBlockContent

    data class Table(
        val caption: String?,
        val captionLinks: List<BookDocumentLink>,
        val rows: List<BookDocumentTableRow>,
        val columnCount: Int,
    ) : BookDocumentBlockContent {
        init {
            require(rows.isNotEmpty()) { "document table must contain at least one row" }
            require(columnCount in 1..MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) {
                "document table must contain between 1 and $MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN columns"
            }
            require(rows.layoutBookDocumentTable()?.columnCount == columnCount) {
                "document table column count must match its rowspan-aware grid"
            }
            require(captionLinks.fitInside(caption.orEmpty())) {
                "document table caption links must fit inside the caption"
            }
        }
    }

    data class Disclosure(
        val summary: String,
        val body: List<BookDocumentBlock>,
        val initiallyExpanded: Boolean,
    ) : BookDocumentBlockContent {
        init {
            require(summary.isNotBlank()) { "document disclosure summary must not be blank" }
            require(body.isNotEmpty()) { "document disclosure body must not be empty" }
        }
    }

    data object ThematicBreak : BookDocumentBlockContent

    data class Unsupported(
        val elementType: String,
    ) : BookDocumentBlockContent {
        init {
            require(elementType.isNotBlank()) { "unsupported element type must not be blank" }
            require(elementType.length <= MAX_DIAGNOSTIC_LENGTH) { "unsupported element type is too long" }
        }
    }

    private companion object {
        const val MAX_DIAGNOSTIC_LENGTH = 64
    }
}

internal data class BookDocumentListItem(
    val text: String,
    val depth: Int,
    val marker: String?,
) {
    init {
        require(text.isNotBlank()) { "document list item text must not be blank" }
        require(depth in 0..MAX_LIST_DEPTH) { "document list item depth is outside the supported range" }
        require(marker == null || marker.isNotBlank()) { "document list marker must not be blank" }
    }

    private companion object {
        const val MAX_LIST_DEPTH = 8
    }
}

internal enum class BookDocumentListMarkerStyle {
    DECIMAL,
    LOWER_ALPHA,
    UPPER_ALPHA,
    LOWER_ROMAN,
    UPPER_ROMAN,
    BULLET,
}

internal data class BookDocumentImage(
    val resourceId: String,
    val alternativeText: String?,
    val width: Int?,
    val height: Int?,
) {
    init {
        require(resourceId.isNotBlank()) { "document image resource id must not be blank" }
        require(alternativeText == null || alternativeText.isNotBlank()) {
            "document image alternative text must not be blank"
        }
        require(width == null || width in 1..MAX_INTRINSIC_DIMENSION) {
            "document image width is outside the supported range"
        }
        require(height == null || height in 1..MAX_INTRINSIC_DIMENSION) {
            "document image height is outside the supported range"
        }
    }

    private companion object {
        const val MAX_INTRINSIC_DIMENSION = 32_768
    }
}
