package app.katari.entry.interactions.manga.reader.image.background

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Picks the colours that continue [page] into the space at both its sides, down its height, for pages far taller than
 * that space, such as a long strip's: spans covering the page from top to bottom, or none when the page shows no
 * background to continue.
 *
 * The page's background is taken only from gaps: stretches of rows that are one neutral colour across the whole width,
 * as the space between panels is; a saturated band across the page is artwork. A gap holds its colour exactly. A short
 * stretch must also end at artwork or at a sharp edge: rows of one colour each that blend on into the next are a
 * gradient, such as a banner's, which can hold still for a while at its ends; a long one is a gap even where a
 * gradient fades into it.
 *
 * Artwork never lends the sides its colours. Beside artwork between two gaps of different colours, the sides keep the
 * colour the artwork's edges are nearer to in brightness, and switch once, with a short fade, where the edges turn from
 * one to the other: dark artwork between a black gap and a white one stays black beside it and turns white where the
 * white gap begins.
 */
internal fun chooseSideBackdrop(page: PagePixels): List<BackdropSpan> {
    val gaps = page.gaps()
    if (gaps.isEmpty()) return emptyList()
    val spans = SpanList(page.height)
    spans.solid(0, gaps.first().bottom, gaps.first().argb)
    for ((upper, lower) in gaps.zipWithNext()) {
        if (upper.argb.isCloseTo(lower.argb)) {
            spans.solid(upper.bottom, lower.bottom, upper.argb)
            continue
        }
        // The fade centres on the switch, within the artwork between the gaps
        val fadeRows = minOf(lower.top - upper.bottom, maxOf(1, (page.width * FADE_SHARE).roundToInt()))
        val fadeTop = (page.switchRow(upper, lower) - fadeRows / 2).coerceIn(upper.bottom, lower.top - fadeRows)
        spans.solid(upper.bottom, fadeTop, upper.argb)
        spans.fade(fadeTop, fadeTop + fadeRows, upper.argb, lower.argb)
        spans.solid(fadeTop + fadeRows, lower.bottom, lower.argb)
    }
    spans.solid(gaps.last().bottom, page.height, gaps.last().argb)
    return spans.spans
}

/** Rows [top] until [bottom] that are all [argb] across the page. */
private class Gap(val top: Int, val bottom: Int, val argb: Int)

private fun PagePixels.gaps(): List<Gap> {
    val rowColors = List(height) { y -> rowColor(y) }
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
            val gradientEnd = blendsInto(start - 1, argb) || blendsInto(y, argb)
            if (argb.isNeutral() && (rows >= longRows || (rows >= minRows && !gradientEnd))) {
                gaps += Gap(start, y, argb)
            }
            start = -1
        }
        if (color != null && start < 0) start = y
    }
    return gaps
}

/** The colour of row [y] when the whole row is close to it, else null. */
private fun PagePixels.rowColor(y: Int): Int? {
    val color = this[width / 2, y]
    return color.takeIf { (0..<width).all { x -> this[x, y].isCloseTo(color) } }
}

/**
 * Where the sides switch from [upper]'s colour to [lower]'s in the artwork between them: the row that leaves the rows
 * whose edges are nearer [upper]'s colour in brightness above it, and those nearer [lower]'s below, as far as one
 * switch can. The edges are both sides' bands, read together, so the two sides switch at the same height.
 */
private fun PagePixels.switchRow(upper: Gap, lower: Gap): Int {
    val band = maxOf(MIN_EDGE_BAND, (width * EDGE_BAND_SHARE).roundToInt())
    val edge = (0..<band) + (width - band..<width)
    val upperLuma = upper.argb.luma()
    val lowerLuma = lower.argb.luma()
    // How much nearer lower's colour than upper's the rows from upper's end down to each candidate are, summed: the
    // switch is where that sum is lowest, so rows above it lean to upper's colour and rows below to lower's
    var best = upper.bottom
    var bestSum = 0f
    var sum = 0f
    for (y in upper.bottom..<lower.top) {
        val luma = edge.map { x -> this[x, y].luma() }.average().toFloat()
        sum += abs(luma - upperLuma) - abs(luma - lowerLuma)
        if (sum < bestSum) {
            bestSum = sum
            best = y + 1
        }
    }
    return best
}

/** Spans built top to bottom over a page [height] rows tall, a solid one running on into the next of its colour. */
private class SpanList(private val height: Int) {
    val spans = mutableListOf<BackdropSpan>()

    fun solid(top: Int, bottom: Int, argb: Int) {
        if (bottom <= top) return
        val last = spans.lastOrNull()
        if (last != null && last.topArgb == argb && last.bottomArgb == argb) {
            spans[spans.lastIndex] = last.copy(bottom = bottom.toFloat() / height)
        } else {
            spans += BackdropSpan(top.toFloat() / height, bottom.toFloat() / height, argb, argb)
        }
    }

    fun fade(top: Int, bottom: Int, topArgb: Int, bottomArgb: Int) {
        if (bottom > top) spans += BackdropSpan(top.toFloat() / height, bottom.toFloat() / height, topArgb, bottomArgb)
    }
}

private val Int.red: Int get() = (this shr 16) and 0xFF
private val Int.green: Int get() = (this shr 8) and 0xFF
private val Int.blue: Int get() = this and 0xFF

private fun Int.luma(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

private fun Int.channelDistance(other: Int): Int =
    maxOf(abs(red - other.red), abs(green - other.green), abs(blue - other.blue))

// Close enough to pass for one colour in a picture, as JPEG noise and antialiasing leave it
private fun Int.isCloseTo(other: Int): Boolean = channelDistance(other) < CLOSE_DISTANCE

// One colour held exactly, as a gap's is and a gradient's isn't
private fun Int.isSameAs(other: Int): Boolean = channelDistance(other) <= SAME_DISTANCE

// White, black, grey or a slight tint, as the space between panels is
private fun Int.isNeutral(): Boolean = maxOf(red, green, blue) - minOf(red, green, blue) < NEUTRAL_SPREAD

private const val CLOSE_DISTANCE = 30
private const val SAME_DISTANCE = 6
private const val NEUTRAL_SPREAD = 48

// How tall a gap must be, as a share of the page's width: thinner lines of one colour are part of the artwork
private const val MIN_GAP_SHARE = 0.05f

// How tall a gap must be, as a share of the page's width, to count where a gradient fades into it
private const val LONG_GAP_SHARE = 0.25f

// How tall the fade between two gap colours is, as a share of the page's width
private const val FADE_SHARE = 0.1f

// How far in from each side the artwork's edge is read
private const val EDGE_BAND_SHARE = 0.02f
private const val MIN_EDGE_BAND = 2
