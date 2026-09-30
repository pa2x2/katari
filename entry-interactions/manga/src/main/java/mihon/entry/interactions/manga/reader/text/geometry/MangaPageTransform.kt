package mihon.entry.interactions.manga.reader.text.geometry

import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import tachiyomi.core.common.util.system.ImageUtil

/** How a viewer derived the image it displays from the raw page of [rawSize]. */
internal sealed interface MangaPageTransform {

    /**
     * The piece of the raw page that shows a region with raw [bounds], or `null` when less than half of the region is
     * shown at all.
     */
    fun pieceShowing(bounds: ImageRect): MangaPagePiece?

    /** The raw page is displayed as it is. */
    data object None : MangaPageTransform {
        override fun pieceShowing(bounds: ImageRect) = MangaPagePiece.Whole
    }

    /** One half of a double-page spread, displayed on its own (dual-page split in the paged viewers). */
    data class Half(val rawSize: ImageSize, val side: ImageUtil.Side) : MangaPageTransform {
        override fun pieceShowing(bounds: ImageRect): MangaPagePiece? {
            val half = rawSize.half(side)
            return MangaPagePiece.Moved(half, dx = -half.left, dy = 0).takeIf { it.holdsMostOf(bounds) }
        }
    }

    /** Both halves of a spread, [upper] above the other (dual-page split in the webtoon viewer). */
    data class Stacked(val rawSize: ImageSize, val upper: ImageUtil.Side) : MangaPageTransform {
        override fun pieceShowing(bounds: ImageRect): MangaPagePiece? {
            val top = rawSize.half(upper)
            val bottom = rawSize.half(upper.opposite)
            return listOf(
                MangaPagePiece.Moved(top, dx = -top.left, dy = 0),
                MangaPagePiece.Moved(bottom, dx = -bottom.left, dy = rawSize.height),
            )
                .filter { it.holdsMostOf(bounds) }
                .maxByOrNull { it.overlap(bounds) }
        }
    }

    /** The spread turned a quarter to fit the screen (rotate to fit). */
    data class Rotated(val rawSize: ImageSize, val clockwise: Boolean) : MangaPageTransform {
        override fun pieceShowing(bounds: ImageRect) = MangaPagePiece.Turned(rawSize, clockwise)
    }
}

/** A part of the raw page and where the viewer's image shows it. */
internal sealed interface MangaPagePiece {
    /** Where the part of [rect] inside this piece appears in the viewer's image, or `null` if none of it is inside. */
    fun toTransformed(rect: ImageRect): ImageRect?

    data object Whole : MangaPagePiece {
        override fun toTransformed(rect: ImageRect) = rect
    }

    data class Moved(val area: ImageRect, val dx: Int, val dy: Int) : MangaPagePiece {
        override fun toTransformed(rect: ImageRect): ImageRect? =
            rect.intersect(area)?.let { ImageRect(it.left + dx, it.top + dy, it.right + dx, it.bottom + dy) }

        fun overlap(rect: ImageRect): Long = rect.intersect(area)?.area ?: 0

        fun holdsMostOf(rect: ImageRect): Boolean = overlap(rect) * 2 >= rect.area
    }

    /** The whole page turned a quarter; a clockwise turn moves the raw left edge to the top. */
    data class Turned(val rawSize: ImageSize, val clockwise: Boolean) : MangaPagePiece {
        override fun toTransformed(rect: ImageRect): ImageRect? {
            val r = rect.intersect(rawSize.bounds) ?: return null
            return if (clockwise) {
                ImageRect(rawSize.height - r.bottom, r.left, rawSize.height - r.top, r.right)
            } else {
                ImageRect(r.top, rawSize.width - r.right, r.bottom, rawSize.width - r.left)
            }
        }
    }
}

/** The half [ImageUtil.splitInHalf] and [ImageUtil.splitAndMerge] take for [side]. */
private fun ImageSize.half(side: ImageUtil.Side): ImageRect = when (side) {
    ImageUtil.Side.LEFT -> ImageRect(0, 0, width / 2, height)
    ImageUtil.Side.RIGHT -> ImageRect(width - width / 2, 0, width, height)
}

private val ImageUtil.Side.opposite: ImageUtil.Side
    get() = when (this) {
        ImageUtil.Side.LEFT -> ImageUtil.Side.RIGHT
        ImageUtil.Side.RIGHT -> ImageUtil.Side.LEFT
    }
