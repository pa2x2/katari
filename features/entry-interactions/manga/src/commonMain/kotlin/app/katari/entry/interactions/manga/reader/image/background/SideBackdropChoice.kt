package app.katari.entry.interactions.manga.reader.image.background

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What continues a page into the space beside it, down its height: [left] and [right] each cover the page from top to
 * bottom, and are empty when the page shows no background to continue.
 */
internal class SideBackdrop(val left: List<BackdropSpan>, val right: List<BackdropSpan>)

/**
 * A stretch of a side from [top] to [bottom], as fractions of the page's height, running from [topArgb] to
 * [bottomArgb]; the two are the same where the side holds one colour. ARGB without premultiplied alpha.
 */
internal data class BackdropSpan(val top: Float, val bottom: Float, val topArgb: Int, val bottomArgb: Int)

/**
 * Picks the colours that continue [page] beside it, down its height, for pages far taller than the space beside them,
 * such as a long strip's.
 *
 * The page's background is taken only from gaps: stretches of rows that are one colour across the whole width, as the
 * space between panels is. A gap holds its colour exactly. A short stretch must also end at artwork or at a sharp edge:
 * rows of one colour each that blend on into the next are a gradient, such as a banner's, which can hold still for a
 * while at its ends; a long one is a gap even where a gradient fades into it. Artwork reaching the page's
 * edge never lends the sides its colours: beside it they keep the gap colour above and below, and where the two
 * differ, each reaches as far as the page's edge still shows it and the rest fades from one to the other.
 */
internal fun chooseSideBackdrop(page: PagePixels): SideBackdrop {
    val gaps = page.gaps()
    if (gaps.isEmpty()) return SideBackdrop(emptyList(), emptyList())
    val band = maxOf(MIN_EDGE_BAND, (page.width * EDGE_BAND_SHARE).roundToInt())
    return SideBackdrop(
        left = page.sideSpans(gaps, 0..<band),
        right = page.sideSpans(gaps, page.width - band..<page.width),
    )
}

/** Rows [top] until [bottom] that are all [argb] across the page. */
private class Gap(val top: Int, val bottom: Int, val argb: Int)

private fun PagePixels.gaps(): List<Gap> {
    val rowColors = List(height) { y -> uniformColor(0..<width, y) }
    val minRows = maxOf(1, (width * MIN_GAP_SHARE).roundToInt())
    val longRows = maxOf(1, (width * LONG_GAP_SHARE).roundToInt())
    fun blendsInto(row: Int, argb: Int) = rowColors.getOrNull(row)?.isCloseTo(argb) == true
    val gaps = mutableListOf<Gap>()
    var start = -1
    for (y in 0..height) {
        val color = rowColors.getOrNull(y)
        if (start >= 0 && (color == null || !color.isSameAs(rowColors[start]!!))) {
            val argb = rowColors[start]!!
            val rows = y - start
            if (rows >= longRows || (rows >= minRows && !blendsInto(start - 1, argb) && !blendsInto(y, argb))) {
                gaps += Gap(start, y, argb)
            }
            start = -1
        }
        if (color != null && start < 0) start = y
    }
    return gaps
}

/** The colour of row [y] across [xs] when they are all close to it, else null. */
private fun PagePixels.uniformColor(xs: IntRange, y: Int): Int? {
    val color = this[(xs.first + xs.last) / 2, y]
    return color.takeIf { xs.all { x -> this[x, y].isCloseTo(color) } }
}

private fun PagePixels.sideSpans(gaps: List<Gap>, edge: IntRange): List<BackdropSpan> {
    val spans = mutableListOf<BackdropSpan>()
    fun solid(top: Int, bottom: Int, argb: Int) {
        if (bottom > top) spans += BackdropSpan(top.toFloat() / height, bottom.toFloat() / height, argb, argb)
    }
    solid(0, gaps.first().bottom, gaps.first().argb)
    for ((upper, lower) in gaps.zipWithNext()) {
        if (upper.argb.isCloseTo(lower.argb)) {
            solid(upper.bottom, lower.bottom, upper.argb)
            continue
        }
        var fadeTop = upper.bottom
        while (fadeTop < lower.top && uniformColor(edge, fadeTop)?.isCloseTo(upper.argb) == true) fadeTop++
        var fadeBottom = lower.top
        while (fadeBottom > fadeTop && uniformColor(edge, fadeBottom - 1)?.isCloseTo(lower.argb) == true) fadeBottom--
        solid(upper.bottom, fadeTop, upper.argb)
        if (fadeBottom > fadeTop) {
            spans += BackdropSpan(fadeTop.toFloat() / height, fadeBottom.toFloat() / height, upper.argb, lower.argb)
        }
        solid(fadeBottom, lower.bottom, lower.argb)
    }
    solid(gaps.last().bottom, height, gaps.last().argb)
    return spans
}

private fun Int.channelDistance(other: Int): Int = maxOf(
    abs(((this shr 16) and 0xFF) - ((other shr 16) and 0xFF)),
    abs(((this shr 8) and 0xFF) - ((other shr 8) and 0xFF)),
    abs((this and 0xFF) - (other and 0xFF)),
)

// Close enough to pass for one colour in a picture, as JPEG noise and antialiasing leave it
private fun Int.isCloseTo(other: Int): Boolean = channelDistance(other) < CLOSE_DISTANCE

// One colour held exactly, as a gap's is and a gradient's isn't
private fun Int.isSameAs(other: Int): Boolean = channelDistance(other) <= SAME_DISTANCE

private const val CLOSE_DISTANCE = 30
private const val SAME_DISTANCE = 6

// How tall a gap must be, as a share of the page's width: thinner lines of one colour are part of the artwork
private const val MIN_GAP_SHARE = 0.05f

// How tall a gap must be, as a share of the page's width, to count where a gradient fades into it
private const val LONG_GAP_SHARE = 0.25f

// How far in from each side the page's edge is read for where a colour still reaches
private const val EDGE_BAND_SHARE = 0.02f
private const val MIN_EDGE_BAND = 2
