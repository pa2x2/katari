package mihon.book.api.document

import kotlinx.serialization.Serializable

/**
 * A box drawn around a run of consecutive blocks of one [BookDocumentContent]: a translator's note, a game's status
 * window, a letter set apart from the narration.
 *
 * Blocks stay a flat list, so reading positions, pagination and selection keep working per block; a box only
 * decorates the blocks its range covers. Boxes nest: a block belongs to every box whose range covers it, and two
 * boxes either nest or don't overlap.
 *
 * @property firstBlockIndex index in [BookDocumentContent.blocks] of the first block inside the box.
 * @property lastBlockIndex index of the last block inside the box, inclusive.
 * @property style what the box draws and the room it takes.
 */
@Serializable
data class BookDocumentBox(
    val firstBlockIndex: Int,
    val lastBlockIndex: Int,
    val style: BookDocumentBoxStyle,
) {
    init {
        require(firstBlockIndex >= 0) { "document box start must not be negative" }
        require(lastBlockIndex >= firstBlockIndex) { "document box must contain at least one block" }
    }

    fun contains(other: BookDocumentBox): Boolean =
        firstBlockIndex <= other.firstBlockIndex && other.lastBlockIndex <= lastBlockIndex
}

/**
 * Validated decoration and spacing of a [BookDocumentBox]. Colours are the document's own, as ARGB; the reader
 * adapts them to its reading theme. Lengths in `Em` scale with the reader's text size, lengths in `Dp` don't.
 *
 * @property backgroundArgb optional background colour, drawn under [backgroundGradient].
 * @property backgroundGradient optional gradient filling the box.
 * @property borders border of each side.
 * @property cornerRadiiDp rounding of each corner.
 * @property paddingEm space between the borders and the blocks inside.
 * @property marginEm space around the borders.
 * @property centered whether the box sits in the middle of the page when narrower than it.
 * @property maxWidthEm optional largest width of the box including its borders.
 * @property widthFraction optional width as a fraction of the space the box sits in.
 * @property fitContent whether the box is only as wide as its widest line.
 * @property shadows shadows cast by the box, outermost first.
 */
@Serializable
data class BookDocumentBoxStyle(
    val backgroundArgb: Long? = null,
    val backgroundGradient: BookDocumentGradient? = null,
    val borders: BookDocumentBoxBorders = BookDocumentBoxBorders(),
    val cornerRadiiDp: BookDocumentCorners = BookDocumentCorners(),
    val paddingEm: BookDocumentInsets = BookDocumentInsets(),
    val marginEm: BookDocumentInsets = BookDocumentInsets(),
    val centered: Boolean = false,
    val maxWidthEm: Float? = null,
    val widthFraction: Float? = null,
    val fitContent: Boolean = false,
    val shadows: List<BookDocumentShadow> = emptyList(),
) {
    init {
        require(backgroundArgb == null || backgroundArgb in 0..MAX_BOX_ARGB) { "document box background must be ARGB" }
        require(
            paddingEm.all {
                it in 0f..MAX_BOX_SPACING_EM
            },
        ) { "document box padding is outside the supported range" }
        require(marginEm.all { it in 0f..MAX_BOX_SPACING_EM }) { "document box margin is outside the supported range" }
        require(maxWidthEm == null || maxWidthEm in MIN_BOX_WIDTH_EM..MAX_BOX_WIDTH_EM) {
            "document box width is outside the supported range"
        }
        require(widthFraction == null || widthFraction in MIN_BOX_FRACTION..1f) {
            "document box width fraction is outside the supported range"
        }
        require(shadows.size <= MAX_BOX_SHADOWS) { "document box has too many shadows" }
    }

    /** Whether the box draws anything, as opposed to only taking room. */
    val decorated: Boolean
        get() = backgroundArgb != null || backgroundGradient != null || borders.any || shadows.isNotEmpty()
}

/** Lengths for the four sides of a box. */
@Serializable
data class BookDocumentInsets(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f,
) {
    fun all(predicate: (Float) -> Boolean): Boolean =
        predicate(top) && predicate(right) && predicate(bottom) && predicate(left)
}

/** The border of each side of a box; a missing side draws nothing. */
@Serializable
data class BookDocumentBoxBorders(
    val top: BookDocumentBorder? = null,
    val right: BookDocumentBorder? = null,
    val bottom: BookDocumentBorder? = null,
    val left: BookDocumentBorder? = null,
) {
    val any: Boolean get() = top != null || right != null || bottom != null || left != null
}

/** Corner radii of a box in density-independent pixels. */
@Serializable
data class BookDocumentCorners(
    val topLeft: Float = 0f,
    val topRight: Float = 0f,
    val bottomRight: Float = 0f,
    val bottomLeft: Float = 0f,
) {
    init {
        require(listOf(topLeft, topRight, bottomRight, bottomLeft).all { it in 0f..MAX_CORNER_RADIUS_DP }) {
            "document box corner radius is outside the supported range"
        }
    }
}

/**
 * A shadow cast by a box or by text, in density-independent pixels.
 *
 * @property colorArgb shadow colour, or null for the colour of the text.
 * @property inset whether a box shadow falls inside the box; text shadows are never inset.
 */
@Serializable
data class BookDocumentShadow(
    val offsetXDp: Float,
    val offsetYDp: Float,
    val blurDp: Float,
    val spreadDp: Float = 0f,
    val colorArgb: Long? = null,
    val inset: Boolean = false,
) {
    init {
        require(offsetXDp in -MAX_SHADOW_DP..MAX_SHADOW_DP && offsetYDp in -MAX_SHADOW_DP..MAX_SHADOW_DP) {
            "document shadow offset is outside the supported range"
        }
        require(blurDp in 0f..MAX_SHADOW_DP && spreadDp in -MAX_SHADOW_DP..MAX_SHADOW_DP) {
            "document shadow size is outside the supported range"
        }
        require(colorArgb == null || colorArgb in 0..MAX_BOX_ARGB) { "document shadow colour must be ARGB" }
    }
}

/**
 * A linear or radial gradient.
 *
 * @property angleDegrees direction of a linear gradient, clockwise from bottom-to-top as in CSS (180 = downwards).
 * @property stops colours in order; a stop without a position sits evenly between its neighbours.
 */
@Serializable
data class BookDocumentGradient(
    val kind: Kind,
    val angleDegrees: Float = DEFAULT_GRADIENT_ANGLE,
    val stops: List<Stop>,
    val repeating: Boolean = false,
) {
    init {
        require(stops.size in 2..MAX_GRADIENT_STOPS) {
            "document gradient must have between 2 and $MAX_GRADIENT_STOPS stops"
        }
        require(angleDegrees.isFinite()) { "document gradient angle must be finite" }
    }

    @Serializable
    enum class Kind {
        LINEAR,
        RADIAL,
    }

    /** @property position fraction of the gradient line, or null to space evenly. */
    @Serializable
    data class Stop(val colorArgb: Long, val position: Float? = null) {
        init {
            require(colorArgb in 0..MAX_BOX_ARGB) { "document gradient colour must be ARGB" }
            require(position == null || position.isFinite()) { "document gradient stop must be finite" }
        }
    }
}

private const val MAX_BOX_ARGB = 0xFFFF_FFFFL
private const val MAX_BOX_SPACING_EM = 8f
private const val MIN_BOX_WIDTH_EM = 1f
private const val MAX_BOX_WIDTH_EM = 200f
private const val MIN_BOX_FRACTION = 0.05f
private const val MAX_BOX_SHADOWS = 4
private const val MAX_CORNER_RADIUS_DP = 200f
private const val MAX_SHADOW_DP = 64f
private const val MAX_GRADIENT_STOPS = 16
private const val DEFAULT_GRADIENT_ANGLE = 180f
