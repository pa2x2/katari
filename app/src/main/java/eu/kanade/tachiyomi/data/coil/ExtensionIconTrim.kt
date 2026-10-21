package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Extension and source icons are drawn in the app's own rounded-square mask. Icons ship with whatever margin their
 * extension drew around them, and inside the mask that margin shows as a second frame, so it is cut away here: an
 * adaptive icon is drawn without the launcher's mask, and a bitmap icon is cropped to what it actually paints.
 */
class ExtensionIconTrim : Transformation() {

    override val cacheKey: String = "ExtensionIconTrim"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap = input.trimTransparentEdges()
}

/**
 * Draws an adaptive icon's layers unmasked, cropped to the part a launcher shows: the inner 72 of their 108 dp, so the
 * outer sixth on every side is left out.
 */
internal fun Drawable.toUnmaskedIconBitmap(): Bitmap {
    if (this !is AdaptiveIconDrawable) return toBitmap()
    val size = intrinsicWidth.coerceAtLeast(1)
    val bleed = size / 4
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    listOfNotNull(background, foreground).forEach { layer ->
        layer.setBounds(-bleed, -bleed, size + bleed, size + bleed)
        layer.draw(canvas)
    }
    return bitmap
}

/**
 * Crops to the smallest square around the pixels that aren't (nearly) transparent. Faint pixels, such as the drop
 * shadow older extension icons carry, don't count. Returns the bitmap as it is when nothing would be cut.
 */
internal fun Bitmap.trimTransparentEdges(): Bitmap {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0..<height) {
        for (x in 0..<width) {
            if (pixels[y * width + x] ushr 24 > OPAQUE_ENOUGH) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < left || bottom < top) return this
    // A square keeps the icon's proportions in the square mask; it grows around the content's centre and stays inside
    // the bitmap.
    val side = maxOf(right - left + 1, bottom - top + 1).coerceAtMost(minOf(width, height))
    val x = ((left + right + 1 - side) / 2).coerceIn(0, width - side)
    val y = ((top + bottom + 1 - side) / 2).coerceIn(0, height - side)
    if (side == width && side == height) return this
    return Bitmap.createBitmap(this, x, y, side, side)
}

/** Alpha above which a pixel counts as part of the icon rather than its margin or shadow. */
private const val OPAQUE_ENOUGH = 40
