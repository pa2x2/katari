package mihon.text.recognition.provider.onnx.tensor

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Resizes opaque ARGB [pixels] of a [width]×[height] image to [targetWidth]×[targetHeight] with a bilinear filter
 * whose support widens with the reduction, as Pillow's `BILINEAR` resize does.
 *
 * The models were trained on images reduced that way. A fixed 2×2 bilinear sample, which Android's filtered bitmap
 * scaling uses, skips source pixels once an image shrinks by more than half and loses thin strokes.
 */
internal fun resamplePixels(
    pixels: IntArray,
    width: Int,
    height: Int,
    targetWidth: Int,
    targetHeight: Int,
): IntArray {
    require(pixels.size == width * height)
    require(targetWidth > 0 && targetHeight > 0)
    if (width == targetWidth && height == targetHeight) return pixels.copyOf()

    val horizontal = FilterWeights(width, targetWidth)
    val vertical = FilterWeights(height, targetHeight)
    // Like Pillow, the horizontal pass produces an 8-bit image that the vertical pass reads.
    val rows = IntArray(targetWidth * height)
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until targetWidth) {
            var red = 0f
            var green = 0f
            var blue = 0f
            horizontal.forEach(x) { source, weight ->
                val pixel = pixels[row + source]
                red += weight * pixel.red()
                green += weight * pixel.green()
                blue += weight * pixel.blue()
            }
            rows[y * targetWidth + x] = (red.channel() shl 16) or (green.channel() shl 8) or blue.channel()
        }
    }
    val result = IntArray(targetWidth * targetHeight)
    for (y in 0 until targetHeight) {
        for (x in 0 until targetWidth) {
            var red = 0f
            var green = 0f
            var blue = 0f
            vertical.forEach(y) { source, weight ->
                val pixel = rows[source * targetWidth + x]
                red += weight * pixel.red()
                green += weight * pixel.green()
                blue += weight * pixel.blue()
            }
            result[y * targetWidth + x] = OPAQUE or (red.channel() shl 16) or (green.channel() shl 8) or blue.channel()
        }
    }
    return result
}

/** Source pixels and normalized weights contributing to every target pixel along one axis. */
private class FilterWeights(sourceSize: Int, targetSize: Int) {
    private val first = IntArray(targetSize)
    private val count = IntArray(targetSize)
    private val weights: Array<FloatArray>

    init {
        val scale = sourceSize.toDouble() / targetSize
        val filterScale = max(scale, 1.0)
        val support = filterScale
        weights = Array(targetSize) { target ->
            val center = (target + 0.5) * scale
            val start = max((center - support + 0.5).toInt(), 0)
            val end = min((center + support + 0.5).toInt(), sourceSize)
            val taps = FloatArray(end - start) { offset ->
                max(0.0, 1.0 - abs((start + offset - center + 0.5) / filterScale)).toFloat()
            }
            val sum = taps.sum()
            if (sum > 0f) taps.indices.forEach { taps[it] /= sum }
            first[target] = start
            count[target] = taps.size
            taps
        }
    }

    inline fun forEach(target: Int, action: (source: Int, weight: Float) -> Unit) {
        val start = first[target]
        val taps = weights[target]
        for (offset in 0 until count[target]) action(start + offset, taps[offset])
    }
}

private fun Float.channel(): Int = roundToInt().coerceIn(0, 255)

private const val OPAQUE = 0xFF shl 24
