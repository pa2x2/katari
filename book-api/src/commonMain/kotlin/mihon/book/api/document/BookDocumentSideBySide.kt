package mihon.book.api.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One item of a [BookDocumentBlockContent.SideBySide] row.
 *
 * @property body the item's content. A box around all of its blocks is the item's own box, which a reader may
 * stretch to the height of the row.
 * @property bodyStartWithinBlock offset at which [body] begins in the owning block.
 * @property sizing how wide the item wants to be.
 */
@Serializable
data class BookDocumentSideBySideItem(
    val body: BookDocumentContent,
    val bodyStartWithinBlock: Int,
    val sizing: BookDocumentItemSizing = BookDocumentItemSizing(),
) {
    init {
        require(bodyStartWithinBlock >= 0) { "document side-by-side item start must not be negative" }
    }

    val bodyEndWithinBlock: Int get() = bodyStartWithinBlock + body.text.length
}

/**
 * Width rules of a side-by-side item, as flex and grid items state them.
 *
 * @property grow share of free row space the item takes, as `flex-grow`.
 * @property shrink share of missing row space the item gives up, as `flex-shrink`.
 * @property basisEm preferred width in em, or null for [basisFraction] or the content's width.
 * @property basisFraction preferred width as a fraction of the row.
 * @property column optional zero-based grid column the item starts in.
 * @property columnSpan grid columns the item covers.
 */
@Serializable
data class BookDocumentItemSizing(
    val grow: Float = 0f,
    val shrink: Float = 1f,
    val basisEm: Float? = null,
    val basisFraction: Float? = null,
    val column: Int? = null,
    val columnSpan: Int = 1,
) {
    init {
        require(grow in 0f..MAX_FLEX_FACTOR && shrink in 0f..MAX_FLEX_FACTOR) {
            "document flex factors are outside the supported range"
        }
        require(basisEm == null || basisEm in 0f..MAX_LENGTH_EM) {
            "document item basis is outside the supported range"
        }
        require(basisFraction == null || basisFraction in 0f..1f) { "document item basis fraction must be a fraction" }
        require(column == null || column in 0 until MAX_GRID_TRACKS) { "document grid column is out of range" }
        require(columnSpan in 1..MAX_GRID_TRACKS) { "document grid span is out of range" }
    }
}

/** How the items of a side-by-side row share it. */
@Serializable
sealed interface BookDocumentSideBySideLayout {
    val columnGapEm: Float
    val rowGapEm: Float
    val alignItems: BookDocumentItemAlignment

    /**
     * A flex row: items keep their preferred widths, then grow into free space or shrink, and [wrap] onto further
     * lines when they don't fit.
     *
     * @property reversed whether items run from the end, as `row-reverse`.
     */
    @Serializable
    @SerialName("flex")
    data class Flex(
        val wrap: Boolean = false,
        val reversed: Boolean = false,
        val justify: BookDocumentJustification = BookDocumentJustification.START,
        override val alignItems: BookDocumentItemAlignment = BookDocumentItemAlignment.STRETCH,
        override val columnGapEm: Float = 0f,
        override val rowGapEm: Float = 0f,
    ) : BookDocumentSideBySideLayout {
        init {
            requireGaps(columnGapEm, rowGapEm)
        }
    }

    /**
     * A grid: items fill [columns] row by row, or as many columns of at least [autoFillMinEm] as fit when the
     * template repeats to fill the row.
     */
    @Serializable
    @SerialName("grid")
    data class Grid(
        val columns: List<BookDocumentGridTrack>,
        val autoFillMinEm: Float? = null,
        override val alignItems: BookDocumentItemAlignment = BookDocumentItemAlignment.STRETCH,
        override val columnGapEm: Float = 0f,
        override val rowGapEm: Float = 0f,
    ) : BookDocumentSideBySideLayout {
        init {
            require(columns.size in 1..MAX_GRID_TRACKS || autoFillMinEm != null) {
                "document grid must have between 1 and $MAX_GRID_TRACKS columns"
            }
            require(autoFillMinEm == null || autoFillMinEm in 1f..MAX_LENGTH_EM) {
                "document grid fill width is outside the supported range"
            }
            requireGaps(columnGapEm, rowGapEm)
        }
    }
}

/** A grid column's width rule. */
@Serializable
sealed interface BookDocumentGridTrack {
    @Serializable
    @SerialName("fixed")
    data class Fixed(val em: Float) : BookDocumentGridTrack {
        init {
            require(em in 0f..MAX_LENGTH_EM) { "document grid track is outside the supported range" }
        }
    }

    @Serializable
    @SerialName("fraction")
    data class OfRow(val fraction: Float) : BookDocumentGridTrack {
        init {
            require(fraction in 0f..1f) { "document grid track fraction must be a fraction" }
        }
    }

    /** A share of the space left after the other columns, as `fr`. */
    @Serializable
    @SerialName("flexible")
    data class Flexible(val share: Float) : BookDocumentGridTrack {
        init {
            require(share in 0f..MAX_FLEX_FACTOR) { "document grid share is outside the supported range" }
        }
    }

    /** As wide as the widest item in the column needs, as `auto`. */
    @Serializable
    @SerialName("auto")
    data object Auto : BookDocumentGridTrack
}

/** Distribution of free space along a flex row, as `justify-content`. */
@Serializable
enum class BookDocumentJustification {
    START,
    CENTER,
    END,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY,
}

/** Placement of items across a line, as `align-items`. */
@Serializable
enum class BookDocumentItemAlignment {
    STRETCH,
    START,
    CENTER,
    END,
}

/**
 * An element the text beside it wraps around, as CSS `float`.
 *
 * @property widthEm optional width in em; with neither width set, the reader picks one.
 * @property widthFraction optional width as a fraction of the page.
 */
@Serializable
data class BookDocumentFloat(
    val side: Side,
    val widthEm: Float? = null,
    val widthFraction: Float? = null,
) {
    init {
        require(widthEm == null || widthEm in 1f..MAX_LENGTH_EM) {
            "document float width is outside the supported range"
        }
        require(widthFraction == null || widthFraction in MIN_FLOAT_FRACTION..1f) {
            "document float width fraction is outside the supported range"
        }
    }

    @Serializable
    enum class Side {
        LEFT,
        RIGHT,
    }
}

private fun requireGaps(columnGapEm: Float, rowGapEm: Float) {
    require(columnGapEm in 0f..MAX_GAP_EM && rowGapEm in 0f..MAX_GAP_EM) {
        "document gaps are outside the supported range"
    }
}

internal const val MAX_GRID_TRACKS = 24
private const val MAX_FLEX_FACTOR = 100f
private const val MAX_LENGTH_EM = 200f
private const val MAX_GAP_EM = 8f
private const val MIN_FLOAT_FRACTION = 0.05f
