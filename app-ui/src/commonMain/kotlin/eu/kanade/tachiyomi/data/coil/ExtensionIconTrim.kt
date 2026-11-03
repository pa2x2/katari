package eu.kanade.tachiyomi.data.coil

import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Extension and source icons are drawn in the app's own rounded-square mask. Icons ship with whatever margin their
 * extension drew around them, and inside the mask that margin shows as a second frame, so it is cut away here: a
 * bitmap icon is cropped to what it actually paints. An adaptive icon reaches here already drawn without the
 * launcher's mask ([ExtensionIconSource]).
 */
class ExtensionIconTrim : Transformation() {

    override val cacheKey: String = "ExtensionIconTrim"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val square = input.iconPixels().trimmedSquare() ?: return input
        return input.croppedTo(square)
    }
}

/** A square part of an icon, in pixels from its top left. */
internal data class IconSquare(val x: Int, val y: Int, val side: Int)

/**
 * The smallest square around the pixels that aren't (nearly) transparent. Faint pixels, such as the drop shadow older
 * extension icons carry, don't count. Null when nothing would be cut.
 */
internal fun IconPixels.trimmedSquare(): IconSquare? {
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0..<height) {
        for (x in 0..<width) {
            if (argb[y * width + x] ushr 24 > OPAQUE_ENOUGH) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < left || bottom < top) return null
    // A square keeps the icon's proportions in the square mask; it grows around the content's centre and stays inside
    // the bitmap.
    val side = maxOf(right - left + 1, bottom - top + 1).coerceAtMost(minOf(width, height))
    val x = ((left + right + 1 - side) / 2).coerceIn(0, width - side)
    val y = ((top + bottom + 1 - side) / 2).coerceIn(0, height - side)
    if (side == width && side == height) return null
    return IconSquare(x, y, side)
}

/** Alpha above which a pixel counts as part of the icon rather than its margin or shadow. */
private const val OPAQUE_ENOUGH = 40
