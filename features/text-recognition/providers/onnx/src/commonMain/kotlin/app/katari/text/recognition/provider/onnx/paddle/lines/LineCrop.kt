package app.katari.text.recognition.provider.onnx.paddle.lines

import app.katari.text.recognition.api.image.ArgbPixels
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Cuts [line] out of [source] as an upright image, as PaddleOCR's perspective crop does. Parts of the line outside
 * [source] repeat its edge pixels.
 *
 * With [turnCounterClockwise], the result is additionally turned a quarter counter-clockwise, which lays a vertical
 * column out the way PaddleOCR presents vertical text to its recognizer.
 *
 * Each result pixel is a bilinear sample of the source at the point its centre maps to.
 */
internal fun cropLine(
    source: ArgbPixels,
    line: RotatedRectangle.Quadrilateral,
    turnCounterClockwise: Boolean,
): ArgbPixels? {
    val width = line.width.roundToInt()
    val height = line.height.roundToInt()
    if (width < 1 || height < 1) return null
    val corners = DoubleArray(8).also { array ->
        line.corners.forEachIndexed { index, corner ->
            array[2 * index] = corner.x
            array[2 * index + 1] = corner.y
        }
    }
    val w = width.toDouble()
    val h = height.toDouble()
    // A quarter turn counter-clockwise sends the top edge to the left and the right edge to the top.
    val upright = if (turnCounterClockwise) {
        doubleArrayOf(0.0, w, 0.0, 0.0, h, 0.0, h, w)
    } else {
        doubleArrayOf(0.0, 0.0, w, 0.0, w, h, 0.0, h)
    }
    val toSource = PerspectiveTransform.between(upright, corners) ?: return null
    val resultWidth = if (turnCounterClockwise) height else width
    val resultHeight = if (turnCounterClockwise) width else height
    val values = IntArray(resultWidth * resultHeight)
    val point = DoubleArray(2)
    for (y in 0 until resultHeight) {
        for (x in 0 until resultWidth) {
            toSource.map(x + 0.5, y + 0.5, point)
            values[y * resultWidth + x] = source.sample(point[0] - 0.5, point[1] - 0.5)
        }
    }
    return ArgbPixels(resultWidth, resultHeight, values)
}

/** Bilinear sample at ([x], [y]) in pixel-centre coordinates, repeating the edge pixels outside the image. */
private fun ArgbPixels.sample(x: Double, y: Double): Int {
    val left = floor(x)
    val top = floor(y)
    val fx = x - left
    val fy = y - top
    val x0 = left.toInt().coerceIn(0, width - 1)
    val x1 = (left.toInt() + 1).coerceIn(0, width - 1)
    val y0 = top.toInt().coerceIn(0, height - 1)
    val y1 = (top.toInt() + 1).coerceIn(0, height - 1)
    val topLeft = this[x0, y0]
    val topRight = this[x1, y0]
    val bottomLeft = this[x0, y1]
    val bottomRight = this[x1, y1]
    var result = 0
    for (shift in CHANNEL_SHIFTS) {
        val upper = channel(topLeft, shift) * (1 - fx) + channel(topRight, shift) * fx
        val lower = channel(bottomLeft, shift) * (1 - fx) + channel(bottomRight, shift) * fx
        result = result or ((upper * (1 - fy) + lower * fy).roundToInt().coerceIn(0, 255) shl shift)
    }
    return result
}

private fun channel(pixel: Int, shift: Int): Double = ((pixel ushr shift) and 0xFF).toDouble()

private val CHANNEL_SHIFTS = intArrayOf(24, 16, 8, 0)
