package app.katari.entry.interactions.manga.reader.image.background

import kotlin.math.roundToInt

/**
 * What the rules for the space beside a page ([chooseSideBackdrop]) read of each of its rows, top to bottom, for a page
 * [width] pixels across: [rowColors] holds a row's colour where the whole row is close to one colour, else null, and
 * [edgeLuma] how bright the row is at both its sides. It is all the rules need, so the profiles of consecutive pages
 * can be [joined] and read as one, as the pages of a long strip are one picture cut into pieces.
 */
internal class RowProfile(val width: Int, val rowColors: List<Int?>, val edgeLuma: FloatArray) {
    val height: Int get() = rowColors.size
}

/** How many rows the profile has once stretched [width] pixels across, as [joined] stretches it. */
internal fun RowProfile.heightAt(width: Int): Int = (height.toLong() * width / this.width).toInt()

/** The profile of these pixels: each row's colour where it has one, and the brightness of a band at each side. */
internal fun PagePixels.rowProfile(): RowProfile {
    val band = maxOf(MIN_EDGE_BAND, (width * EDGE_BAND_SHARE).roundToInt())
    val edge = (0..<band) + (width - band..<width)
    return RowProfile(
        width = width,
        rowColors = List(height) { y -> rowColor(y) },
        edgeLuma = FloatArray(height) { y -> edge.map { x -> this[x, y].luma() }.average().toFloat() },
    )
}

/**
 * The profiles one under the other as a single page [width] pixels across: each is stretched to that width with its
 * rows in proportion, as pages shown at one width are.
 */
internal fun List<RowProfile>.joined(width: Int): RowProfile {
    val rowColors = mutableListOf<Int?>()
    val edgeLuma = mutableListOf<Float>()
    for (profile in this) {
        val rows = profile.heightAt(width)
        for (row in 0..<rows) {
            val source = (row.toLong() * profile.height / rows).toInt()
            rowColors += profile.rowColors[source]
            edgeLuma += profile.edgeLuma[source]
        }
    }
    return RowProfile(width, rowColors, edgeLuma.toFloatArray())
}

/** The colour of row [y] when the whole row is close to it, else null. */
private fun PagePixels.rowColor(y: Int): Int? {
    val color = this[width / 2, y]
    return color.takeIf { (0..<width).all { x -> this[x, y].isCloseColor(color) } }
}

// How far in from each side the artwork's edge is read
private const val EDGE_BAND_SHARE = 0.02f
private const val MIN_EDGE_BAND = 2
