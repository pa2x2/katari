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
    val resourceIds: Set<String> = emptySet(),
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
        require(resourceIds.none(String::isBlank)) { "document resource ids must not be blank" }
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
    val content: BookDocumentBlockContent = BookDocumentBlockContent.Text(),
    val plainText: String,
    val sourceFragments: List<String>,
    val links: List<BookDocumentLink> = emptyList(),
    val inlineStyles: List<BookDocumentInlineStyleRange> = emptyList(),
    val style: BookDocumentStyle = BookDocumentStyle(),
    val logicalStart: Int,
    val logicalEndExclusive: Int,
) {
    init {
        require(sourceFragments.none(String::isBlank)) { "block source fragments must not be blank" }
        require(logicalStart >= 0) { "block logical start must not be negative" }
        require(logicalEndExclusive > logicalStart) { "block logical range must not be empty" }
        require(links.all { it.start in 0 until logicalLength && it.endExclusive in 1..logicalLength }) {
            "block links must fit inside the logical range"
        }
        require(inlineStyles.all { it.start in 0 until logicalLength && it.endExclusive in 1..logicalLength }) {
            "block inline styles must fit inside the logical range"
        }
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
            require(columnCount in 1..MAX_TABLE_COLUMNS) {
                "document table must contain between 1 and $MAX_TABLE_COLUMNS columns"
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
        const val MAX_TABLE_COLUMNS = MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN
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

internal data class BookDocumentTableRow(
    val cells: List<BookDocumentTableCell>,
) {
    init {
        require(cells.isNotEmpty()) { "document table row must contain at least one cell" }
    }
}

internal data class BookDocumentTableCell(
    val text: String,
    val header: Boolean,
    val scope: BookDocumentTableCellScope?,
    val columnSpan: Int,
    val rowSpan: Int,
    val links: List<BookDocumentLink>,
) {
    init {
        require(columnSpan in 1..MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) {
            "document table column span is outside the supported range"
        }
        require(rowSpan in 1..MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) {
            "document table row span is outside the supported range"
        }
        require(links.fitInside(text)) { "document table cell links must fit inside the cell text" }
    }
}

internal data class BookDocumentTableLayout(
    val columnCount: Int,
    val rows: List<BookDocumentTableLayoutRow>,
)

internal data class BookDocumentTableLayoutRow(
    val carriedColumns: Set<Int>,
    val placements: List<BookDocumentTableCellPlacement>,
)

internal data class BookDocumentTableCellPlacement(
    val column: Int,
    val cell: BookDocumentTableCell,
)

internal fun List<BookDocumentTableRow>.layoutBookDocumentTable(): BookDocumentTableLayout? {
    if (isEmpty()) return null
    var carried = IntArray(MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN)
    val laidOutRows = mutableListOf<BookDocumentTableLayoutRow>()
    var columnCount = 0
    for (row in this) {
        val carriedColumns = carried.indices.filterTo(linkedSetOf()) { carried[it] > 0 }
        val occupied = BooleanArray(MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) { it in carriedColumns }
        val nextCarried = IntArray(MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) { column ->
            (carried[column] - 1).coerceAtLeast(0)
        }
        val placements = mutableListOf<BookDocumentTableCellPlacement>()
        for (cell in row.cells) {
            val column = occupied.firstAvailableRange(cell.columnSpan) ?: return null
            placements += BookDocumentTableCellPlacement(column, cell)
            repeat(cell.columnSpan) { offset ->
                val occupiedColumn = column + offset
                occupied[occupiedColumn] = true
                if (cell.rowSpan > 1) {
                    nextCarried[occupiedColumn] = maxOf(nextCarried[occupiedColumn], cell.rowSpan - 1)
                }
            }
            columnCount = maxOf(columnCount, column + cell.columnSpan)
        }
        laidOutRows += BookDocumentTableLayoutRow(carriedColumns, placements)
        carried = nextCarried
    }
    return BookDocumentTableLayout(columnCount, laidOutRows)
}

private fun BooleanArray.firstAvailableRange(width: Int): Int? {
    for (start in 0..size - width) {
        if ((start until start + width).none { this[it] }) return start
    }
    return null
}

internal const val MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN = 24

internal enum class BookDocumentTableCellScope {
    ROW,
    COLUMN,
    ROW_GROUP,
    COLUMN_GROUP,
}

internal data class BookDocumentLink(
    val start: Int,
    val endExclusive: Int,
    val target: BookDocumentLinkTarget,
) {
    init {
        require(start >= 0) { "document link start must not be negative" }
        require(endExclusive > start) { "document link range must not be empty" }
    }
}

private fun List<BookDocumentLink>.fitInside(text: String): Boolean =
    all { it.start in text.indices && it.endExclusive in 1..text.length }

internal sealed interface BookDocumentLinkTarget {
    data class Anchor(val fragment: String) : BookDocumentLinkTarget {
        init {
            require(fragment.isNotBlank()) { "document link anchor must not be blank" }
        }
    }

    data class External(val url: String) : BookDocumentLinkTarget {
        init {
            require(url.isNotBlank()) { "document external link must not be blank" }
        }
    }
}

internal fun String.toBookDocumentLinkTarget(): BookDocumentLinkTarget? = when {
    startsWith("#") && length > 1 -> BookDocumentLinkTarget.Anchor(removePrefix("#"))
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true) ->
        BookDocumentLinkTarget.External(this)
    else -> null
}

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
    DISCLOSURE,
    CALLOUT,
    NOTE,
    UNSUPPORTED,
    OTHER,
}
